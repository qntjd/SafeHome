package com.safehome.safehome_api.domain.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.safehome.safehome_api.domain.user.dto.AuthDto;
import com.safehome.safehome_api.domain.user.entity.User;
import com.safehome.safehome_api.domain.user.repository.UserRepository;
import com.safehome.safehome_api.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Transactional
    public AuthDto.TokenResponse register(AuthDto.RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .nickname(req.nickname())
                .homeLat(req.homeLat())
                .homeLng(req.homeLng())
                .build();

        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthDto.TokenResponse login(AuthDto.LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return issueTokens(user);
    }

    public AuthDto.TokenResponse refresh(AuthDto.RefreshRequest req) {
        if (!jwtProvider.validate(req.refreshToken())) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }
        String email = jwtProvider.getEmail(req.refreshToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthDto.ProfileResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return AuthDto.ProfileResponse.from(user);
    }

    @Transactional
    public AuthDto.ProfileResponse updateProfile(String email, AuthDto.UpdateProfileRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        user.updateNickname(req.nickname());
        return AuthDto.ProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public AuthDto.TokenResponse loginWithGoogleToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new IllegalArgumentException("유효하지 않은 구글 토큰입니다.");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email    = payload.getEmail();
            String name     = (String) payload.get("name");
            String nickname = (name != null && !name.isBlank()) ? name : email.split("@")[0];

            User user = userRepository.findByEmail(email).orElseGet(() ->
                    userRepository.save(User.builder()
                            .email(email)
                            .nickname(nickname)
                            .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .role(User.Role.USER)
                            .build())
            );

            return issueTokens(user);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("구글 로그인 처리 중 오류: " + e.getMessage());
        }
    }

    private AuthDto.TokenResponse issueTokens(User user) {
        String access  = jwtProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String refresh = jwtProvider.createRefreshToken(user.getEmail());
        return new AuthDto.TokenResponse(access, refresh, user.getEmail(), user.getNickname());
    }
}