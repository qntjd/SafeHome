package com.safehome.safehome_api.domain.safety.service;

import com.safehome.safehome_api.domain.safety.dto.SafetyDto;
import com.safehome.safehome_api.domain.safety.entity.DistrictScore;
import com.safehome.safehome_api.domain.safety.repository.*;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// @Cacheable 제거하고 수동 캐싱으로 변경
import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SafetyService {

    private final SafetyFacilityRepository facilityRepository;
    private final DistrictScoreRepository districtScoreRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public SafetyDto.HeatmapResponse getHeatmap() {
        String cacheKey = "heatmap:all";

        // 캐시 확인
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, SafetyDto.HeatmapResponse.class);
            } catch (Exception ignored) {}
        }

        // DB 조회
        List<SafetyDto.ScoreResponse> list = districtScoreRepository.findAllOrderByScore()
                .stream()
                .map(SafetyDto.ScoreResponse::from)
                .toList();
        SafetyDto.HeatmapResponse response = new SafetyDto.HeatmapResponse(list);

        // 캐시 저장
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

        List<SafetyDto.FacilityResponse> response = facilityRepository.findWithinRadius(lat, lng, radius)
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
}