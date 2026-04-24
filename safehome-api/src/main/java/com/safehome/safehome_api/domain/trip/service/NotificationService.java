package com.safehome.safehome_api.domain.trip.service;

import com.safehome.safehome_api.domain.trip.entity.SafeTrip;
import com.safehome.safehome_api.domain.user.entity.EmergencyContact;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    /**
     * 실제 운영: FCM or SMS API 연동
     * 현재: 로그로 대체 (포트폴리오 시연용)
     */
    public void sendOverdueAlert(SafeTrip trip, EmergencyContact contact) {
        String message = String.format(
                "[SafeHome 안심귀가] %s님이 예정 시각(%s)까지 미도착 상태입니다. 연락해보세요.",
                trip.getUser().getNickname(),
                trip.getExpectedArrivalAt()
        );

        log.warn("[FCM] 수신자: {} ({}) | 메시지: {}", contact.getName(), contact.getPhone(), message);
        // TODO: FCM or SMS 전송 구현
        // fcmClient.send(contact.getPhone(), message);
    }

    public void sendSosAlert(SafeTrip trip, EmergencyContact contact) {
        String message = String.format(
                "[SafeHome SOS] %s님이 SOS를 발동했습니다! 위치: lat=%.4f, lng=%.4f",
                trip.getUser().getNickname(),
                trip.getStartLat(),
                trip.getStartLng()
        );

        log.error("[FCM-SOS] 수신자: {} ({}) | 메시지: {}", contact.getName(), contact.getPhone(), message);
        // TODO: FCM or SMS 전송 구현
    }
}