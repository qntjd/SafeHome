package com.safehome.safehome_api.domain.safety.controller;

import com.safehome.safehome_api.domain.safety.dto.SafetyDto;
import com.safehome.safehome_api.domain.safety.service.SafetyService;
import com.safehome.safehome_api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Safety", description = "안전 점수 & 시설 API")
@RestController
@RequestMapping("/api/safety")
@RequiredArgsConstructor
public class SafetyController {

    private final SafetyService safetyService;

    @Operation(summary = "주변 안전시설 조회", description = "위경도 + 반경(m) 기준 CCTV, 비상벨, 가로등 목록 반환")
    @GetMapping("/facilities")
    public ApiResponse<List<SafetyDto.FacilityResponse>> getFacilities(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "500") Double radius
    ) {
        return ApiResponse.success(safetyService.getNearbyFacilities(lat, lng, radius));
    }

    @Operation(summary = "행정동 안전점수 조회")
    @GetMapping("/score")
    public ApiResponse<SafetyDto.ScoreResponse> getScore(
            @RequestParam String districtCode
    ) {
        return ApiResponse.success(safetyService.getDistrictScore(districtCode));
    }

    @Operation(summary = "전체 히트맵 데이터 조회 (공개 API)")
    @GetMapping("/heatmap")
    public ApiResponse<SafetyDto.HeatmapResponse> getHeatmap() {
        return ApiResponse.success(safetyService.getHeatmap());
    }
}