package com.safehome.safehome_api.batch;

import com.safehome.safehome_api.domain.alert.entity.DisasterAlert;
import com.safehome.safehome_api.domain.alert.repository.AlertSubscriptionRepository;
import com.safehome.safehome_api.domain.alert.repository.DisasterAlertRepository;
import com.safehome.safehome_api.domain.alert.service.AlertService;
import com.safehome.safehome_api.domain.alert.service.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisasterAlertBatch {

    private final DisasterAlertRepository alertRepository;
    private final AlertSubscriptionRepository subscriptionRepository;
    private final SseEmitterManager sseEmitterManager;
    private final RestTemplate restTemplate;

    @Value("${public-api.disaster.url:https://www.safetydata.go.kr/V2/api/DSSP-IF-00247}")
    private String disasterApiUrl;
    @Value("${public-api.disaster.key:YOUR_API_KEY}")
    private String apiKey;

    // 3분마다 재난문자 API 폴링
    @Scheduled(fixedDelay = 180_000)
    public void fetchAndBroadcast() {
        log.info("[DisasterBatch] 재난문자 수집 시작. SSE 연결 수={}", sseEmitterManager.getConnectionCount());

        try {
            List<Map<String, Object>> rawAlerts = callDisasterApi();

            for (Map<String, Object> raw : rawAlerts) {
                String externalId = String.valueOf(raw.get("SN"));

                // 중복 방지
                if (alertRepository.existsByExternalId(externalId)) continue;

                DisasterAlert alert = DisasterAlert.builder()
                        .externalId(externalId)
                        .title("재난문자")
                        .message(String.valueOf(raw.get("MSG_CN")))
                        .districtCode(String.valueOf(raw.get("RCPTN_RGN_NM")))
                        .districtName(String.valueOf(raw.get("RCPTN_RGN_NM")))
                        .level(DisasterAlert.AlertLevel.WARNING)
                        .build();

                alertRepository.save(alert);

                // 구독자에게 SSE 전송
                notifySubscribers(alert);
            }

        } catch (Exception e) {
            log.error("[DisasterBatch] 수집 실패: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> callDisasterApi() {
        // 실제 행안부 API 응답 파싱 (공공데이터포털 키 필요)
        // 개발 중에는 Mock 데이터 반환
        return List.of(
                Map.of(
                        "SN", "TEST-" + System.currentTimeMillis(),
                        "MSG_CN", "[테스트] 강풍 주의보가 발령되었습니다.",
                        "RCPTN_RGN_NM", "대구광역시"
                )
        );
    }

    private void notifySubscribers(DisasterAlert alert) {
        if (alert.getLat() == null || alert.getLng() == null) {
            // 좌표 없으면 전체 브로드캐스트
            sseEmitterManager.broadcast(Map.of(
                    "type", "DISASTER",
                    "message", alert.getMessage(),
                    "district", alert.getDistrictName(),
                    "level", alert.getLevel().name(),
                    "issuedAt", alert.getIssuedAt()
            ));
            return;
        }

        // 좌표 있으면 반경 내 구독자에게만 전송
        subscriptionRepository
                .findActiveSubscriptionsNear(alert.getLat(), alert.getLng())
                .forEach(sub -> sseEmitterManager.send(
                        sub.getUser().getId(),
                        Map.of(
                                "type", "DISASTER",
                                "message", alert.getMessage(),
                                "district", alert.getDistrictName(),
                                "level", alert.getLevel().name()
                        )
                ));
    }
}