package com.safehome.safehome_api.batch;

import com.safehome.safehome_api.domain.safety.service.SafetyScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SafetyScoreScheduler implements ApplicationRunner {

    private final SafetyScoreService safetyScoreService;

    private static final java.util.Map<String, String> DAEGU_DISTRICTS = java.util.Map.of(
        "2771010100", "대구 중구",
        "2771010200", "대구 동구",
        "2771010300", "대구 서구",
        "2771010400", "대구 남구",
        "2771010500", "대구 북구",
        "2771010600", "대구 수성구",
        "2771010700", "대구 달서구",
        "2771010800", "대구 달성군"
    );

    // 앱 시작 시 자동 계산
    @Override
    public void run(ApplicationArguments args) {
        log.info("[SafetyScore] 시작 시 안전점수 계산");
        calculateAll();
    }

    // 매일 새벽 4시 재계산
    @Scheduled(cron = "0 0 4 * * *")
    public void scheduledCalculate() {
        log.info("[SafetyScore] 정기 안전점수 재계산");
        calculateAll();
    }

    private void calculateAll() {
        DAEGU_DISTRICTS.forEach((code, name) -> {
            try {
                safetyScoreService.calculateAndSave(code, name);
            } catch (Exception e) {
                log.error("[SafetyScore] {} 계산 실패: {}", name, e.getMessage());
            }
        });
        log.info("[SafetyScore] 전체 계산 완료");
    }
}