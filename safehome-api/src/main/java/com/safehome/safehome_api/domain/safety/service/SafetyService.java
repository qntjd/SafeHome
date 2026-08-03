package com.safehome.safehome_api.domain.safety.service;

import com.safehome.safehome_api.domain.safety.dto.SafetyDto;
import com.safehome.safehome_api.domain.safety.entity.DistrictScore;
import com.safehome.safehome_api.domain.safety.entity.SafetyFacility;
import com.safehome.safehome_api.domain.safety.repository.*;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SafetyService {

    private final SafetyFacilityRepository facilityRepository;
    private final DistrictScoreRepository districtScoreRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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
        List<double[]> checkPoints = List.of(
                new double[]{ startLat * 0.75 + endLat * 0.25, startLng * 0.75 + endLng * 0.25 },
                new double[]{ startLat * 0.5  + endLat * 0.5,  startLng * 0.5  + endLng * 0.5  },
                new double[]{ startLat * 0.25 + endLat * 0.75, startLng * 0.25 + endLng * 0.75 }
        );

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

        double safetyScore = Math.min(100,
                (totalCctv * 2.0) + (totalBell * 3.0) + (totalPolice * 5.0));

        return new SafetyDto.SafeRouteResponse(safePoints, totalCctv, totalBell, totalPolice, safetyScore);
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
}