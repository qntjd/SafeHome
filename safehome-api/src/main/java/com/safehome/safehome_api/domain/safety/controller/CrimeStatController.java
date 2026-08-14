package com.safehome.safehome_api.domain.safety.controller;

import com.safehome.safehome_api.domain.safety.dto.CrimeStatDto;
import com.safehome.safehome_api.domain.safety.service.CrimeStatService;
import com.safehome.safehome_api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Crime", description = "범죄 통계 API")
@RestController
@RequestMapping("/api/crime")
@RequiredArgsConstructor
public class CrimeStatController {

    private final CrimeStatService crimeStatService;

    @Operation(summary = "전체 지역 범죄 통계 조회")
    @GetMapping
    public ApiResponse<CrimeStatDto.AllDistrictCrimeResponse> getAllCrimes(
            @RequestParam(defaultValue = "2024") Integer year
    ) {
        return ApiResponse.success(crimeStatService.getAllDistrictCrimes(year));
    }

    @Operation(summary = "특정 지역 범죄 통계 조회")
    @GetMapping("/{districtCode}")
    public ApiResponse<CrimeStatDto.DistrictCrimeResponse> getDistrictCrimes(
            @PathVariable String districtCode,
            @RequestParam(defaultValue = "2024") Integer year
    ) {
        return ApiResponse.success(crimeStatService.getDistrictCrimes(districtCode, year));
    }

    @Operation(summary = "특정 지역 연도별 범죄 추이 조회")
    @GetMapping("/{districtCode}/trend")
    public ApiResponse<CrimeStatDto.CrimeTrendResponse> getCrimeTrend(
            @PathVariable String districtCode
    ) {
        return ApiResponse.success(crimeStatService.getCrimeTrend(districtCode));
    }
}