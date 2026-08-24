package com.safehome.safehome_api.global.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 알리고(Aligo) SMS 발송 클라이언트.
 * https://smartsms.aligo.in — 가입 후 API Key 발급 + 발신번호 사전 등록이 필요하다.
 * 키가 설정돼 있지 않으면(로컬 개발 등) 실제 전송 없이 로그만 남기고 조용히 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AligoSmsClient {

    private static final String SEND_URL = "https://apis.aligo.in/send/";

    private final RestTemplate restTemplate;

    @Value("${aligo.api-key:}")
    private String apiKey;

    @Value("${aligo.user-id:}")
    private String userId;

    @Value("${aligo.sender:}")
    private String sender;

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(userId) && StringUtils.hasText(sender);
    }

    /** @param receiverPhone 숫자만 남기고 자동 정리됨 (010-1234-5678 형태로 넘겨도 됨) */
    @SuppressWarnings("unchecked")
    public void send(String receiverPhone, String message) {
        if (!isConfigured()) {
            log.warn("[Aligo] 미설정 상태 — SMS 발송을 건너뜁니다. 수신자: {} | 메시지: {}", receiverPhone, message);
            return;
        }

        String receiver = receiverPhone.replaceAll("[^0-9]", "");
        if (receiver.isBlank()) {
            log.warn("[Aligo] 수신자 번호가 비어있어 발송을 건너뜁니다.");
            return;
        }

        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("key", apiKey);
            body.add("user_id", userId);
            body.add("sender", sender);
            body.add("receiver", receiver);
            body.add("msg", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            Map<String, Object> res = restTemplate.postForObject(SEND_URL, request, Map.class);
            Object resultCode = res != null ? res.get("result_code") : null;

            if (resultCode == null || ((Number) resultCode).intValue() < 0) {
                log.error("[Aligo] SMS 발송 실패 | 수신자: {} | 응답: {}", receiver, res);
            } else {
                log.info("[Aligo] SMS 발송 완료 | 수신자: {} | 응답: {}", receiver, res);
            }
        } catch (Exception e) {
            log.error("[Aligo] SMS 발송 중 오류 | 수신자: {} | {}", receiver, e.getMessage());
        }
    }
}
