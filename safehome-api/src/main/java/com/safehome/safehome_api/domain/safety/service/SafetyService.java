package com.safehome.safehome_api.domain.safety.service;

import com.safehome.safehome_api.domain.safety.dto.SafetyDto;
import com.safehome.safehome_api.domain.safety.entity.DistrictScore;
import com.safehome.safehome_api.domain.safety.entity.SafetyFacility;
import com.safehome.safehome_api.domain.safety.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyService {

    private final SafetyFacilityRepository facilityRepository;
    private final DistrictScoreRepository districtScoreRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Qualifier("osrmRestTemplate")
    private final RestTemplate osrmRestTemplate;

    private static final String OSRM_FOOT_ROUTE_URL = "https://router.project-osrm.org/route/v1/foot/";

    // 반경(미터) 기준으로 좌표 검색을 수행하는 공통 헬퍼
    private List<SafetyFacility> findWithinRadius(double lat, double lng, double radiusMeters) {
        double latDelta = radiusMeters / 111000.0;
        double lngDelta = radiusMeters / (111000.0 * Math.cos(Math.toRadians(lat)));

        return facilityRepository.findWithinRadius(
                lat, lng, radiusMeters,
                lat - latDelta, lat + latDelta,
                lng - lngDelta, lng + lngDelta
        );
    }

    @Transactional(readOnly = true)
    public SafetyDto.HeatmapResponse getHeatmap() {
        String cacheKey = "heatmap:all";

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, SafetyDto.HeatmapResponse.class);
            } catch (Exception ignored) {}
        }

        List<SafetyDto.ScoreResponse> list = districtScoreRepository.findAllOrderByScore()
                .stream()
                .map(SafetyDto.ScoreResponse::from)
                .toList();
        SafetyDto.HeatmapResponse response = new SafetyDto.HeatmapResponse(list);

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofHours(1));
        } catch (Exception ignored) {}

        return response;
    }

    @Transactional(readOnly = true)
    public List<SafetyDto.FacilityResponse> getNearbyFacilities(double lat, double lng, double radius) {
        String cacheKey = "facilities:" + lat + "," + lng + "," + radius;

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, SafetyDto.FacilityResponse.class));
            } catch (Exception ignored) {}
        }

        List<SafetyDto.FacilityResponse> response = findWithinRadius(lat, lng, radius)
                .stream()
                .map(SafetyDto.FacilityResponse::from)
                .toList();

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofHours(1));
        } catch (Exception ignored) {}

        return response;
    }

    @Transactional(readOnly = true)
    public SafetyDto.ScoreResponse getDistrictScore(String districtCode) {
        DistrictScore score = districtScoreRepository.findByDistrictCode(districtCode)
                .orElseThrow(() -> new IllegalArgumentException("해당 지역 점수 데이터가 없습니다: " + districtCode));
        return SafetyDto.ScoreResponse.from(score);
    }

    @Transactional(readOnly = true)
    public SafetyDto.SafeRouteResponse getSafeRoute(
            double startLat, double startLng,
            double endLat,   double endLng
    ) {
        List<double[]> checkPoints = legCheckPoints(
                new double[]{ startLat, startLng }, new double[]{ endLat, endLng }
        );
        RouteEval eval = evaluatePath(checkPoints);
        return new SafetyDto.SafeRouteResponse(
                eval.safePoints(), eval.totalCctv(), eval.totalBell(), eval.totalPolice(), eval.safetyScore()
        );
    }

    // 최단 경로 vs 안전 경로(안전시설이 밀집한 지점을 경유하는 경로) 비교.
    // 실제 보행로를 따라가는 경로는 OSRM(무료 공개 도보 길찾기)에서 받아오고,
    // OSRM이 응답하지 않으면 직선 근사로 자연스럽게 대체된다.
    @Transactional(readOnly = true)
    public SafetyDto.RouteCompareResponse getRouteCompare(
            double startLat, double startLng,
            double endLat,   double endLng
    ) {
        double[] start = { startLat, startLng };
        double[] end   = { endLat, endLng };
        double directDistance = haversineMeters(startLat, startLng, endLat, endLng);

        OsrmRoute directOsrm = fetchOsrmRoute(List.of(start, end));
        List<double[]> directPath = directOsrm != null ? directOsrm.path() : List.of(start, end);
        double directRealDistance = directOsrm != null ? directOsrm.distanceMeters() : directDistance;
        List<double[]> directCheckpoints = directOsrm != null
                ? sampleAlongPath(directOsrm.path(), 4)
                : legCheckPoints(start, end);

        RouteEval directEval = evaluatePath(directCheckpoints);
        SafetyDto.RouteOption directOption = new SafetyDto.RouteOption(
                "DIRECT", "최단 경로",
                toRoutePointList(directPath),
                directEval.safePoints(), directEval.totalCctv(), directEval.totalBell(), directEval.totalPolice(),
                directEval.safetyScore(), 0.0
        );

        // 출발~도착 중간 지점 주변에서 경유지 후보(실제 안전시설 위치)를 찾는다.
        // 후보 하나하나마다 DB를 다시 조회하면 최악의 경우 수백 번 왕복이 생겨 타임아웃이 나므로,
        // 코리도 전체를 한 번만 조회한 뒤 밀도 계산은 메모리 안에서 처리한다.
        double midLat = (startLat + endLat) / 2;
        double midLng = (startLng + endLng) / 2;
        double corridorRadius = Math.max(directDistance / 2 * 1.3, 300);
        List<SafetyFacility> candidates = findWithinRadius(midLat, midLng, corridorRadius);

        SafetyFacility bestWaypoint = null;
        double bestScore = -1;
        for (SafetyFacility c : candidates) {
            double detour = haversineMeters(startLat, startLng, c.getLat(), c.getLng())
                           + haversineMeters(c.getLat(), c.getLng(), endLat, endLng);
            if (directDistance > 0 && detour > directDistance * 1.4) continue; // 40% 넘게 돌아가면 제외

            List<SafetyFacility> nearby = candidates.stream()
                    .filter(f -> haversineMeters(c.getLat(), c.getLng(), f.getLat(), f.getLng()) <= 300)
                    .toList();
            double score = weightedFacilityScore(nearby);
            if (score > bestScore) {
                bestScore = score;
                bestWaypoint = c;
            }
        }

        SafetyDto.RouteOption safeOption;
        if (bestWaypoint != null) {
            double[] way = { bestWaypoint.getLat(), bestWaypoint.getLng() };

            OsrmRoute safeOsrm = fetchOsrmRoute(List.of(start, way, end));
            List<double[]> safePath = safeOsrm != null ? safeOsrm.path() : List.of(start, way, end);
            double safeRealDistance = safeOsrm != null
                    ? safeOsrm.distanceMeters()
                    : haversineMeters(start[0], start[1], way[0], way[1]) + haversineMeters(way[0], way[1], end[0], end[1]);
            List<double[]> safeCheckpoints;
            if (safeOsrm != null) {
                safeCheckpoints = sampleAlongPath(safeOsrm.path(), 6);
            } else {
                List<double[]> merged = new java.util.ArrayList<>(legCheckPoints(start, way));
                merged.addAll(legCheckPoints(way, end));
                safeCheckpoints = merged;
            }

            RouteEval safeEval = evaluatePath(safeCheckpoints);
            double extraRatio = directRealDistance > 0 ? (safeRealDistance - directRealDistance) / directRealDistance : 0.0;

            safeOption = new SafetyDto.RouteOption(
                    "SAFE", "안전 경로",
                    toRoutePointList(safePath),
                    safeEval.safePoints(), safeEval.totalCctv(), safeEval.totalBell(), safeEval.totalPolice(),
                    safeEval.safetyScore(), Math.max(extraRatio, 0.0)
            );
        } else {
            // 주변에 안전시설이 없어 더 나은 경유지를 못 찾은 경우 — 최단 경로와 동일하게 반환
            safeOption = new SafetyDto.RouteOption(
                    "SAFE", "안전 경로",
                    toRoutePointList(directPath),
                    directEval.safePoints(), directEval.totalCctv(), directEval.totalBell(), directEval.totalPolice(),
                    directEval.safetyScore(), 0.0
            );
        }

        return new SafetyDto.RouteCompareResponse(directOption, safeOption);
    }

    private record OsrmRoute(List<double[]> path, double distanceMeters) {}

    // OSRM 공개 도보 길찾기 서버에서 실제 보행 경로를 받아온다. 실패하면 null(호출부에서 직선으로 대체).
    @SuppressWarnings("unchecked")
    private OsrmRoute fetchOsrmRoute(List<double[]> waypoints) {
        try {
            String coords = waypoints.stream()
                    .map(p -> p[1] + "," + p[0]) // OSRM은 lng,lat 순서
                    .collect(Collectors.joining(";"));
            String url = OSRM_FOOT_ROUTE_URL + coords + "?overview=full&geometries=geojson";

            Map<String, Object> res = osrmRestTemplate.getForObject(url, Map.class);
            if (res == null || !"Ok".equals(res.get("code"))) return null;

            List<Map<String, Object>> routes = (List<Map<String, Object>>) res.get("routes");
            if (routes == null || routes.isEmpty()) return null;

            Map<String, Object> route = routes.get(0);
            Map<String, Object> geometry = (Map<String, Object>) route.get("geometry");
            List<List<Number>> coordinates = (List<List<Number>>) geometry.get("coordinates");
            if (coordinates == null || coordinates.isEmpty()) return null;

            List<double[]> path = coordinates.stream()
                    .map(c -> new double[]{ c.get(1).doubleValue(), c.get(0).doubleValue() }) // lat, lng
                    .toList();
            double distance = ((Number) route.get("distance")).doubleValue();

            return new OsrmRoute(path, distance);
        } catch (Exception e) {
            log.warn("[경로 비교] OSRM 도보 경로 조회 실패, 직선으로 대체: {}", e.getMessage());
            return null;
        }
    }

    // 경로 위 점이 많을 때(OSRM 결과) 안전시설 조회 횟수를 늘리지 않도록 균등하게 일부만 뽑는다
    private List<double[]> sampleAlongPath(List<double[]> path, int sampleCount) {
        if (path.size() <= sampleCount) return path;
        List<double[]> result = new java.util.ArrayList<>();
        for (int i = 1; i <= sampleCount; i++) {
            int idx = (int) Math.round((double) i / (sampleCount + 1) * (path.size() - 1));
            result.add(path.get(idx));
        }
        return result;
    }

    private List<SafetyDto.RoutePoint> toRoutePointList(List<double[]> path) {
        List<SafetyDto.RoutePoint> points = new java.util.ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            String role = i == 0 ? "START" : (i == path.size() - 1 ? "END" : "PATH");
            points.add(toPoint(path.get(i), role));
        }
        return points;
    }

    private record RouteEval(
            List<SafetyDto.RoutePoint> safePoints,
            int totalCctv, int totalBell, int totalPolice, double safetyScore
    ) {}

    // 경로 위 지점들(체크포인트)을 훑으며 주변 300m 안전시설을 집계한다
    private RouteEval evaluatePath(List<double[]> checkPoints) {
        List<SafetyDto.RoutePoint> safePoints = new java.util.ArrayList<>();
        int totalCctv = 0, totalBell = 0, totalPolice = 0;

        for (double[] point : checkPoints) {
            List<SafetyFacility> facilities = findWithinRadius(point[0], point[1], 300);

            int cctv   = (int) facilities.stream().filter(f -> f.getType().name().equals("CCTV")).count();
            int bell   = (int) facilities.stream().filter(f -> f.getType().name().equals("EMERGENCY_BELL")).count();
            int police = (int) facilities.stream().filter(f -> f.getType().name().equals("POLICE")).count();

            totalCctv   += cctv;
            totalBell   += bell;
            totalPolice += police;

            if (cctv + bell + police > 0) {
                facilities.stream().findFirst().ifPresent(f ->
                        safePoints.add(new SafetyDto.RoutePoint(
                                point[0], point[1],
                                f.getType().name(),
                                f.getDistrictName()
                        ))
                );
            }
        }

        double safetyScore = Math.min(100, (totalCctv * 2.0) + (totalBell * 3.0) + (totalPolice * 5.0));
        return new RouteEval(safePoints, totalCctv, totalBell, totalPolice, safetyScore);
    }

    // 두 지점을 잇는 구간 위의 25/50/75% 지점 (기존 직선 경로 샘플링과 동일한 방식)
    private List<double[]> legCheckPoints(double[] from, double[] to) {
        return List.of(
                new double[]{ from[0] * 0.75 + to[0] * 0.25, from[1] * 0.75 + to[1] * 0.25 },
                new double[]{ from[0] * 0.5  + to[0] * 0.5,  from[1] * 0.5  + to[1] * 0.5  },
                new double[]{ from[0] * 0.25 + to[0] * 0.75, from[1] * 0.25 + to[1] * 0.75 }
        );
    }

    private double weightedFacilityScore(List<SafetyFacility> facilities) {
        double cctv   = facilities.stream().filter(f -> f.getType().name().equals("CCTV")).count();
        double bell   = facilities.stream().filter(f -> f.getType().name().equals("EMERGENCY_BELL")).count();
        double police = facilities.stream().filter(f -> f.getType().name().equals("POLICE")).count();
        return (cctv * 2.0) + (bell * 3.0) + (police * 5.0);
    }

    private SafetyDto.RoutePoint toPoint(double[] latLng, String role) {
        return new SafetyDto.RoutePoint(latLng[0], latLng[1], role, null);
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    @Transactional(readOnly = true)
    public SafetyDto.NearbyDangerResponse getNearbyDanger(double lat, double lng) {
        List<SafetyFacility> facilities = findWithinRadius(lat, lng, 500);

        int cctv   = (int) facilities.stream().filter(f -> f.getType().name().equals("CCTV")).count();
        int bell   = (int) facilities.stream().filter(f -> f.getType().name().equals("EMERGENCY_BELL")).count();
        int police = (int) facilities.stream().filter(f -> f.getType().name().equals("POLICE")).count();

        int total = cctv + bell + police;

        String dangerLevel;
        String message;

        if (total >= 10) {
            dangerLevel = "SAFE";
            message     = "주변에 안전시설이 충분해요. 안심하고 이동하세요.";
        } else if (total >= 3) {
            dangerLevel = "CAUTION";
            message     = "주변 안전시설이 부족해요. 밝은 곳으로 이동하세요.";
        } else {
            dangerLevel = "DANGER";
            message     = "주변에 안전시설이 없어요! 빠르게 안전한 곳으로 이동하세요.";
        }

        return new SafetyDto.NearbyDangerResponse(cctv, bell, police, dangerLevel, message);
    }

    @Transactional(readOnly = true)
    public SafetyDto.FacilityCountResponse getNearbyFacilityCounts(double lat, double lng, double radius) {
        double latDelta = radius / 111000.0;
        double lngDelta = radius / (111000.0 * Math.cos(Math.toRadians(lat)));

        List<Object[]> rows = facilityRepository.countWithinRadiusByType(
                lat, lng, radius,
                lat - latDelta, lat + latDelta,
                lng - lngDelta, lng + lngDelta
        );

        int cctv = 0, bell = 0, police = 0;
        for (Object[] row : rows) {
            String type = (String) row[0];
            long count = ((Number) row[1]).longValue();
            switch (type) {
                case "CCTV" -> cctv = (int) count;
                case "EMERGENCY_BELL" -> bell = (int) count;
                case "POLICE" -> police = (int) count;
            }
        }

        return new SafetyDto.FacilityCountResponse(cctv, bell, police);
    }
}