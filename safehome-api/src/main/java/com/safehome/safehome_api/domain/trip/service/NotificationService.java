package com.safehome.safehome_api.domain.trip.service;

import com.safehome.safehome_api.domain.trip.entity.SafeTrip;
import com.safehome.safehome_api.domain.user.entity.EmergencyContact;
import com.safehome.safehome_api.global.sms.AligoSmsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AligoSmsClient smsClient;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendOverdueAlert(SafeTrip trip, EmergencyContact contact) {
        String message = String.format(
                "[SafeHome 안심귀가] %s님이 예정 시각(%s)까지 미도착 상태입니다. 현재 위치 확인: %s",
                trip.getUser().getNickname(),
                trip.getExpectedArrivalAt(),
                shareLink(trip)
        );
        smsClient.send(contact.getPhone(), message);
    }

    public void sendSosAlert(SafeTrip trip, EmergencyContact contact) {
        String message = String.format(
                "[SafeHome SOS] %s님이 SOS를 발동했습니다! 현재 위치 확인: %s",
                trip.getUser().getNickname(),
                shareLink(trip)
        );
        smsClient.send(contact.getPhone(), message);
    }

    private String shareLink(SafeTrip trip) {
        return trip.getShareToken() != null
                ? frontendUrl + "/share/" + trip.getShareToken()
                : String.format("(위치: %.4f, %.4f)", trip.getStartLat(), trip.getStartLng());
    }
}
