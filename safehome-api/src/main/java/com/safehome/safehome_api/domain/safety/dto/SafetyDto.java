package com.safehome.safehome_api.domain.safety.dto;

import com.safehome.safehome_api.domain.safety.entity.DistrictScore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.safehome.safehome_api.domain.safety.entity.SafetyFacility;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class SafetyDto {

    public record SafeRouteResponse(
        List<RoutePoint> safePoints,  
        int totalCctv,
        int totalBell,
        int totalPolice,
        double safetyScore
    ) {}

    public record RoutePoint(
            Double lat,
            Double lng,
            String type,
            String districtName
    ) {}

    // 최단 경로 vs 안전 경로
    public record RouteOption(
            String type,              
            String label,              
            List<RoutePoint> path,    
            List<RoutePoint> safePoints,
            int totalCctv,
            int totalBell,
            int totalPolice,
            double safetyScore,
            double extraDistanceRatio  
    ) {}

    public record RouteCompareResponse(
            RouteOption direct,
            RouteOption safe
    ) {}

    public record NearbyDangerResponse(
        int cctvCount,
        int bellCount,
        int policeCount,
        String dangerLevel,  
        String message
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FacilityResponse(
            String id,
            String type,
            Double lat,
            Double lng,
            String address,
            String districtName,
            Boolean isActive
    ) implements Serializable {
        public static FacilityResponse from(SafetyFacility f) {
            return new FacilityResponse(
                    f.getId().toString(), f.getType().name(), f.getLat(), f.getLng(), f.getAddress(), f.getDistrictName(), f.getIsActive());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScoreResponse(
            String districtCode,
            String districtName,
            Double cctvScore,
            Double crimeScore,
            Double lightScore,
            Double bellScore,
            Double totalScore,
            String grade,         
            LocalDateTime calculatedAt
    ) implements Serializable {
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
            Double radiusMeters   
    ) implements Serializable {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HeatmapResponse(
            List<ScoreResponse> districts
    ) implements Serializable {}

    public record FacilityCountResponse(
            int cctvCount,
            int bellCount,
            int policeCount
    ) implements Serializable {}
}