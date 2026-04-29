package com.safehome.safehome_api.domain.safety.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class CrimeStatDto {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CrimeStatResponse(
            String districtCode,
            String districtName,
            String crimeType,
            Integer count
    ) implements Serializable {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DistrictCrimeResponse(
            String districtCode,
            String districtName,
            Map<String, Integer> crimeByType,
            Integer totalCount
    ) implements Serializable {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AllDistrictCrimeResponse(
            List<DistrictCrimeResponse> districts
    ) implements Serializable {}
}