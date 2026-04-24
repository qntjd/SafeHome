package com.safehome.safehome_api.domain.user.controller;

import com.safehome.safehome_api.domain.user.dto.AuthDto;
import com.safehome.safehome_api.domain.user.service.UserService;
import com.safehome.safehome_api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입")
    @PostMapping("/register")
    public ApiResponse<AuthDto.TokenResponse> register(@Valid @RequestBody AuthDto.RegisterRequest req) {
        return ApiResponse.success(userService.register(req));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ApiResponse<AuthDto.TokenResponse> login(@Valid @RequestBody AuthDto.LoginRequest req) {
        return ApiResponse.success(userService.login(req));
    }

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    public ApiResponse<AuthDto.TokenResponse> refresh(@Valid @RequestBody AuthDto.RefreshRequest req) {
        return ApiResponse.success(userService.refresh(req));
    }

    @Operation(summary = "프로필 수정")
    @PatchMapping("/api/users/profile")
    public ApiResponse<AuthDto.ProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody AuthDto.UpdateProfileRequest req
    ) {
        return ApiResponse.success(userService.updateProfile(user.getUsername(), req));
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/api/users/me")
    public ApiResponse<AuthDto.ProfileResponse> getMe(
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(userService.getMe(user.getUsername()));
    }
}
