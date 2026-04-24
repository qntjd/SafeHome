package com.safehome.safehome_api.domain.safety.service;

import com.safehome.safehome_api.domain.safety.entity.*;
import com.safehome.safehome_api.domain.safety.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyScoreService {

    private final SafetyFacilityRepository facilityRepository;
    private final CrimeStatRepository crimeStatRepository;
    private final DistrictScoreRepository districtScoreRepository;

    // 기준 최대값 (정규화용) — 실 데이터 수집 후 조정
    private static final double MAX_CCTV   = 100.0;
    private static final double MAX_LIGHT  = 200.0;
    private static final double MAX_BELL   = 30.0;
    private static final double MAX_CRIME  = 500.0;

    @Transactional
    public void calculateAndSave(String districtCode, String districtName) {
        LocalDate now = LocalDate.now();

        long cctvCount  = facilityRepository.countByDistrictCodeAndType(districtCode, SafetyFacility.FacilityType.CCTV);
        long lightCount = facilityRepository.countByDistrictCodeAndType(districtCode, SafetyFacility.FacilityType.STREETLIGHT);
        long bellCount  = facilityRepository.countByDistrictCodeAndType(districtCode, SafetyFacility.FacilityType.EMERGENCY_BELL);
        int  crimeCount = crimeStatRepository.sumCountByDistrictAndYearMonth(
                districtCode, now.getYear(), now.getMonthValue());

        // 0~100 정규화
        double cctvScore  = normalize(cctvCount,  MAX_CCTV)  * 100;
        double lightScore = normalize(lightCount, MAX_LIGHT) * 100;
        double bellScore  = normalize(bellCount,  MAX_BELL)  * 100;
        // 범죄는 많을수록 위험 → 역점수
        double crimeScore = (1.0 - normalize(crimeCount, MAX_CRIME)) * 100;

        DistrictScore score = districtScoreRepository
                .findByDistrictCode(districtCode)
                .orElseGet(() -> DistrictScore.builder()
                        .districtCode(districtCode)
                        .districtName(districtName)
                        .build());

        score.updateScores(cctvScore, crimeScore, lightScore, bellScore);
        districtScoreRepository.save(score);

        log.info("[SafetyScore] {} → total: {}", districtCode, score.getTotalScore());
    }

    private double normalize(double value, double max) {
        return Math.min(value / max, 1.0);
    }
}