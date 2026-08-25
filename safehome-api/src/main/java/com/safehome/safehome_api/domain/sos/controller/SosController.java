package com.safehome.safehome_api.domain.sos.controller;

import com.safehome.safehome_api.domain.sos.dto.SosDto;
import com.safehome.safehome_api.domain.sos.service.SosService;
import com.safehome.safehome_api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "SOS", description = "SOS 발동 이력 API")
@RestController
@RequestMapping("/api/sos")
@RequiredArgsConstructor
public class SosController {

    private final SosService sosService;

    @Operation(summary = "SOS 발동 이력 기록")
    @PostMapping("/log")
    public ApiResponse<SosDto.LogResponse> createLog(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody SosDto.CreateLogRequest req
    ) {
        return ApiResponse.success(sosService.createLog(user.getUsername(), req));
    }

    @Operation(summary = "내 SOS 발동 이력 조회")
    @GetMapping("/log")
    public ApiResponse<List<SosDto.LogResponse>> getMyLogs(
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(sosService.getMyLogs(user.getUsername()));
    }
}