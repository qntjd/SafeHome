package com.safehome.safehome_api.domain.trip.service;

import com.safehome.safehome_api.domain.trip.entity.SafeTrip;
import com.safehome.safehome_api.domain.trip.repository.SafeTripRepository;
import com.safehome.safehome_api.domain.user.entity.EmergencyContact;
import com.safehome.safehome_api.domain.user.repository.EmergencyContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchdogService {

    private final SafeTripRepository tripRepository;
    private final EmergencyContactRepository contactRepository;
    private final NotificationService notificationService;

    // 1분마다 미도착 귀가 체크
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void checkOverdueTrips() {
        List<SafeTrip> overdueTrips = tripRepository.findOverdueTrips(LocalDateTime.now());

        if (overdueTrips.isEmpty()) return;

        log.warn("[Watchdog] 미도착 귀가 {}건 감지", overdueTrips.size());

        for (SafeTrip trip : overdueTrips) {
            List<EmergencyContact> contacts =
                    contactRepository.findAllByUserId(trip.getUser().getId());

            if (contacts.isEmpty()) {
                log.info("[Watchdog] userId={} 비상연락처 없음, 상태만 SOS 전환", trip.getUser().getId());
            } else {
                contacts.forEach(contact ->
                        notificationService.sendOverdueAlert(trip, contact));
            }

            trip.triggerSos();
            log.warn("[Watchdog] tripId={} → SOS 전환", trip.getId());
        }
    }
}