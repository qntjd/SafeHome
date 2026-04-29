package com.safehome.safehome_api.batch;

import com.safehome.safehome_api.domain.safety.service.SafetyScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SafetyScoreScheduler implements ApplicationRunner {

    private final SafetyScoreService safetyScoreService;

    private static final Map<String, String> ALL_DISTRICTS = new LinkedHashMap<>() {{
        // 대구
        put("2771010100", "대구 중구");
        put("2771010200", "대구 동구");
        put("2771010300", "대구 서구");
        put("2771010400", "대구 남구");
        put("2771010500", "대구 북구");
        put("2771010600", "대구 수성구");
        put("2771010700", "대구 달서구");
        put("2771010800", "대구 달성군");
        // 서울
        put("11", "서울");
        // 부산
        put("21", "부산");
        // 인천
        put("23", "인천");
        // 광주
        put("24", "광주");
        // 대전
        put("25", "대전");
        // 울산
        put("26", "울산");
        // 세종
        put("36", "세종");
        // 경기
        put("31", "경기");
        // 강원
        put("32", "강원");
        // 충북
        put("33", "충북");
        // 충남
        put("34", "충남");
        // 전북
        put("35", "전북");
        // 전남
        put("46", "전남");
        // 경북
        put("47", "경북");
        // 경남
        put("48", "경남");
        // 제주
        put("50", "제주");
    }};

    @Override
    public void run(ApplicationArguments args) {
        log.info("[SafetyScore] 시작 시 전국 안전점수 계산");
        calculateAll();
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void scheduledCalculate() {
        log.info("[SafetyScore] 정기 전국 안전점수 재계산");
        calculateAll();
    }

    @CacheEvict(value = {"heatmap", "facilities"}, allEntries = true)
    private void calculateAll() {
        ALL_DISTRICTS.forEach((code, name) -> {
            try {
                safetyScoreService.calculateAndSave(code, name);
            } catch (Exception e) {
                log.error("[SafetyScore] {} 계산 실패: {}", name, e.getMessage());
            }
        });
        log.info("[SafetyScore] 전국 계산 완료");
    }
}