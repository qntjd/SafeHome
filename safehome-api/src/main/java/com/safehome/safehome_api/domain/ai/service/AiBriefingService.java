package com.safehome.safehome_api.domain.ai.service;

import com.safehome.safehome_api.domain.ai.client.ClaudeApiClient;
import com.safehome.safehome_api.domain.ai.entity.AiBriefing;
import com.safehome.safehome_api.domain.ai.repository.AiBriefingRepository;
import com.safehome.safehome_api.domain.news.entity.NewsArticle;
import com.safehome.safehome_api.domain.news.repository.NewsRepository;
import com.safehome.safehome_api.domain.safety.service.CrimeStatService;
import com.safehome.safehome_api.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiBriefingService {

    private final AiBriefingRepository briefingRepository;
    private final NewsRepository newsRepository;
    private final ClaudeApiClient claudeApiClient;

    @Transactional
    public String getTodayBriefing(User user, String districtName, int safetyScore, String grade) {
        LocalDate today = LocalDate.now();

        return briefingRepository.findByUserIdAndBriefingDate(user.getId(), today)
                .map(AiBriefing::getContent)
                .orElseGet(() -> generateAndSave(user, districtName, safetyScore, grade, today));
    }

    private String generateAndSave(User user, String districtName, int safetyScore, String grade, LocalDate today) {
        List<NewsArticle> recentNews = newsRepository.findTop2ByOrderByPublishedAtDesc();
        boolean isNight = LocalTime.now().getHour() >= 22 || LocalTime.now().getHour() < 6;

        String prompt = buildPrompt(districtName, safetyScore, grade, isNight, recentNews);
        String briefing = claudeApiClient.generate(prompt);

        briefingRepository.save(AiBriefing.builder()
                .user(user)
                .briefingDate(today)
                .content(briefing)
                .build());

        return briefing;
    }

    private String buildPrompt(String districtName, int score, String grade, boolean isNight, List<NewsArticle> news) {
        String newsSummary = news.isEmpty()
                ? "특별한 이슈 없음"
                : news.get(0).getTitle();

        return """
            당신은 안전 앱 SafeHome의 AI 브리핑 작성자입니다.
            아래 정보를 바탕으로 2~3문장의 친근하고 담백한 안전 브리핑을 한국어로 작성하세요.
            과장하거나 위협적인 표현은 쓰지 마세요.

            - 지역: %s
            - 안전점수: %d점 (%s등급)
            - 현재 시간대: %s
            - 최근 안전뉴스: %s

            브리핑 본문만 출력하고 다른 설명은 하지 마세요.
            """.formatted(
                districtName, score, grade,
                isNight ? "야간(오후 10시~오전 6시)" : "주간",
                newsSummary
        );
    }
}