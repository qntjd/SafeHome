package com.safehome.safehome_api.domain.trip.controller;

import com.safehome.safehome_api.domain.trip.dto.TripDto;
import com.safehome.safehome_api.domain.trip.service.TripService;
import com.safehome.safehome_api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Trip", description = "안심 귀가 API")
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @Operation(summary = "귀가 시작")
    @PostMapping
    public ApiResponse<TripDto.TripResponse> start(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody TripDto.StartRequest req
    ) {
        return ApiResponse.success(tripService.startTrip(user.getUsername(), req));
    }

    @Operation(summary = "도착 확인")
    @PatchMapping("/{tripId}/arrive")
    public ApiResponse<TripDto.TripResponse> arrive(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID tripId
    ) {
        return ApiResponse.success(tripService.arrive(user.getUsername(), tripId));
    }

    @Operation(summary = "SOS 발동")
    @PostMapping("/{tripId}/sos")
    public ApiResponse<TripDto.TripResponse> sos(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID tripId
    ) {
        return ApiResponse.success(tripService.triggerSos(user.getUsername(), tripId));
    }

    @Operation(summary = "귀가 취소")
    @DeleteMapping("/{tripId}")
    public ApiResponse<Void> cancel(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID tripId
    ) {
        tripService.cancelTrip(user.getUsername(), tripId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "공유 링크로 위치 조회")
    @GetMapping("/share/{shareToken}")
    public ApiResponse<TripDto.ShareLocationResponse> getSharedLocation(
            @PathVariable String shareToken
    ) {
        return ApiResponse.success(tripService.getSharedLocation(shareToken));
    }
}