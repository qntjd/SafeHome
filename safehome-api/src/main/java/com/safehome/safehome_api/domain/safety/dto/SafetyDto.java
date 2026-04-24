package com.safehome.safehome_api.domain.safety.dto;

import com.safehome.safehome_api.domain.safety.entity.DistrictScore;
import com.safehome.safehome_api.domain.safety.entity.SafetyFacility;

import java.time.LocalDateTime;
import java.util.List;

public class SafetyDto {

    public record FacilityResponse(
            String type,
            Double lat,
            Double lng,
            String districtName
    ) {
        public static FacilityResponse from(SafetyFacility f) {
            return new FacilityResponse(
                    f.getType().name(), f.getLat(), f.getLng(), f.getDistrictName());
        }
    }

    public record ScoreResponse(
            String districtCode,
            String districtName,
            Double cctvScore,
            Double crimeScore,
            Double lightScore,
            Double bellScore,
            Double totalScore,
            String grade,           // A/B/C/D/F 등급
            LocalDateTime calculatedAt
    ) {
        public static ScoreResponse from(DistrictScore d) {
            return new ScoreResponse(
                    d.getDistrictCode(),
                    d.getDistrictName(),
                    d.getCctvScore(),
                    d.getCrimeScore(),
                    d.getLightScore(),
                    d.getBellScore(),
                    d.getTotalScore(),
                    gradeOf(d.getTotalScore()),
                    d.getCalculatedAt()
            );
        }

        private static String gradeOf(double score) {
            if (score >= 80) return "A";
            if (score >= 60) return "B";
            if (score >= 40) return "C";
            if (score >= 20) return "D";
            return "F";
        }
    }

    public record NearbyFacilitiesRequest(
            Double lat,
            Double lng,
            Double radiusMeters   // 기본 500m
    ) {}

    public record HeatmapResponse(
            List<ScoreResponse> districts
    ) {}
}