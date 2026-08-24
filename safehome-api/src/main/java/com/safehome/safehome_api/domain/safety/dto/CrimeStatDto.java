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

    // 시/도 단위로 묶고, 그 안에 실제 시/군/구 세부 데이터가 있으면 함께 내려주는 응답
    // (districts가 비어있으면 그 시/도는 세부 데이터가 없다는 뜻 — 프론트에서 펼치기 버튼을 숨기면 됨)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SidoGroupResponse(
            String sidoCode,
            String sidoName,
            Map<String, Integer> crimeByType,
            Integer totalCount,
            List<DistrictCrimeResponse> districts
    ) implements Serializable {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AllDistrictCrimeGroupedResponse(
            List<SidoGroupResponse> sidoGroups
    ) implements Serializable {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YearlyCrimeCount(
                Integer year,
                Integer totalCount
    ) implements Serializable {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CrimeTrendResponse(
                String districtCode,
                String districtName,
                List<YearlyCrimeCount> yearlyTrend
    ) implements Serializable {}
}
