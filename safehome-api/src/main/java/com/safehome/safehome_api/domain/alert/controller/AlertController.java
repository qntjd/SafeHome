package com.safehome.safehome_api.domain.alert.controller;

import com.safehome.safehome_api.domain.alert.dto.AlertDto;
import com.safehome.safehome_api.domain.alert.service.AlertService;
import com.safehome.safehome_api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@Tag(name = "Alert", description = "재난·범죄 알림 API")
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "SSE 실시간 알림 연결")
    @GetMapping(value = "/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectLive(@AuthenticationPrincipal UserDetails user) {
        return alertService.connectSse(user.getUsername());
    }

    @Operation(summary = "알림 구독 등록")
    @PostMapping("/subscribe")
    public ApiResponse<AlertDto.SubscriptionResponse> subscribe(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody AlertDto.SubscribeRequest req
    ) {
        return ApiResponse.success(alertService.subscribe(user.getUsername(), req));
    }

    @Operation(summary = "알림 구독 해제")
    @DeleteMapping("/subscribe/{subscriptionId}")
    public ApiResponse<Void> unsubscribe(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID subscriptionId
    ) {
        alertService.unsubscribe(user.getUsername(), subscriptionId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "내 구독 목록 조회")
    @GetMapping("/subscriptions")
    public ApiResponse<List<AlertDto.SubscriptionResponse>> getMySubscriptions(
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(alertService.getMySubscriptions(user.getUsername()));
    }

    @Operation(summary = "전체 알림 이력 조회")
    @GetMapping("/history")
    public ApiResponse<List<AlertDto.AlertHistoryResponse>> getHistory() {
        return ApiResponse.success(alertService.getAlertHistory());
    }

    @Operation(summary = "내 지역 알림 이력 조회")
    @GetMapping("/history/my")
    public ApiResponse<List<AlertDto.AlertHistoryResponse>> getMyHistory(
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(alertService.getMyLocationAlerts(user.getUsername()));
    }
}