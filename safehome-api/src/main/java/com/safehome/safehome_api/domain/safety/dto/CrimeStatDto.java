package com.safehome.safehome_api.domain.safety.dto;

import com.safehome.safehome_api.domain.safety.entity.CrimeStat;

import java.util.List;
import java.util.Map;

public class CrimeStatDto {

    public record CrimeStatResponse(
            String districtCode,
            String districtName,
            String crimeType,
            Integer count
    ) {
        public static CrimeStatResponse from(CrimeStat c, String districtName) {
            return new CrimeStatResponse(
                    c.getDistrictCode(),
                    districtName,
                    c.getCrimeType().name(),
                    c.getCount()
            );
        }
    }

    public record DistrictCrimeResponse(
            String districtCode,
            String districtName,
            Map<String, Integer> crimeByType,
            Integer totalCount
    ) {}

    public record AllDistrictCrimeResponse(
            List<DistrictCrimeResponse> districts
    ) {}
}