package com.safehome.safehome_api.domain.user.dto;

import com.safehome.safehome_api.domain.user.entity.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8) String password,
            @NotBlank String nickname,
            Double homeLat,
            Double homeLng
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String email,
            String nickname
    ) {}

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {}

    public record UpdateProfileRequest(
            @NotBlank String nickname
    ) {}

    public record ProfileResponse(
                String email,
                String nickname,
                Double homeLat,
                Double homeLng
    ) {
        public static ProfileResponse from(User user) {
                return new ProfileResponse(
                        user.getEmail(),
                        user.getNickname(),
                        user.getHomeLat(),
                        user.getHomeLng()
                );
        }
    }
}
