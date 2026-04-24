package com.safehome.safehome_api.global.oauth;

import com.safehome.safehome_api.domain.user.entity.User;
import com.safehome.safehome_api.domain.user.repository.UserRepository;
import com.safehome.safehome_api.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken  = jwtProvider.createAccessToken(email, user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(email);

        // 닉네임 URL 인코딩 추가
        String encodedNickname = URLEncoder.encode(user.getNickname(), StandardCharsets.UTF_8);
        String encodedEmail    = URLEncoder.encode(email, StandardCharsets.UTF_8);

        String redirectUrl = String.format(
                "http://localhost:5173/oauth/callback?accessToken=%s&refreshToken=%s&nickname=%s&email=%s",
                accessToken, refreshToken, encodedNickname, encodedEmail
        );

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}