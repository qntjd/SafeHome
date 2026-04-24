package com.safehome.safehome_api.global.oauth;

import com.safehome.safehome_api.domain.user.entity.User;
import com.safehome.safehome_api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User oAuth2User = super.loadUser(request);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuthAttributes oAuthAttributes = OAuthAttributes.ofGoogle(attributes);

        // 신규 유저면 자동 회원가입
        userRepository.findByEmail(oAuthAttributes.getEmail())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(oAuthAttributes.getEmail())
                                .nickname(oAuthAttributes.getNickname())
                                .passwordHash("OAUTH_" + oAuthAttributes.getProvider())
                                .build()
                ));

        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "email"
        );
    }
}