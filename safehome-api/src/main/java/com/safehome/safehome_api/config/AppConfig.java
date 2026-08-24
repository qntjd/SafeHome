package com.safehome.safehome_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // 외부 OSRM 도보 경로 API 전용 — 공개 서버가 느리거나 무응답일 때 요청이 무한정 걸리지 않도록
    // 연결/응답 타임아웃을 짧게 둔다.
    @Bean
    public RestTemplate osrmRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(4));
        factory.setReadTimeout(Duration.ofSeconds(4));
        return new RestTemplate(factory);
    }
}