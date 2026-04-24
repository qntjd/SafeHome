package com.safehome.safehome_api.domain.safety.service;

import com.safehome.safehome_api.domain.safety.dto.SafetyDto;
import com.safehome.safehome_api.domain.safety.entity.DistrictScore;
import com.safehome.safehome_api.domain.safety.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SafetyService {

    private final SafetyFacilityRepository facilityRepository;
    private final DistrictScoreRepository districtScoreRepository;

    @Transactional(readOnly = true)
    public List<SafetyDto.FacilityResponse> getNearbyFacilities(double lat, double lng, double radius) {
        return facilityRepository.findWithinRadius(lat, lng, radius)
                .stream()
                .map(SafetyDto.FacilityResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SafetyDto.ScoreResponse getDistrictScore(String districtCode) {
        DistrictScore score = districtScoreRepository.findByDistrictCode(districtCode)
                .orElseThrow(() -> new IllegalArgumentException("해당 지역 점수 데이터가 없습니다: " + districtCode));
        return SafetyDto.ScoreResponse.from(score);
    }

    @Transactional(readOnly = true)
    public SafetyDto.HeatmapResponse getHeatmap() {
        List<SafetyDto.ScoreResponse> list = districtScoreRepository.findAllOrderByScore()
                .stream()
                .map(SafetyDto.ScoreResponse::from)
                .toList();
        return new SafetyDto.HeatmapResponse(list);
    }
}