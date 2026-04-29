package com.safehome.safehome_api.domain.alert.service;

import com.safehome.safehome_api.domain.alert.dto.AlertDto;
import com.safehome.safehome_api.domain.alert.entity.AlertSubscription;
import com.safehome.safehome_api.domain.alert.repository.AlertSubscriptionRepository;
import com.safehome.safehome_api.domain.alert.repository.DisasterAlertRepository;
import com.safehome.safehome_api.domain.user.entity.User;
import com.safehome.safehome_api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertSubscriptionRepository subscriptionRepository;
    private final DisasterAlertRepository alertRepository;
    private final UserRepository userRepository;
    private final SseEmitterManager sseEmitterManager;

    public SseEmitter connectSse(String email) {
        User user = findUser(email);
        return sseEmitterManager.add(user.getId());
    }

    @Transactional
    public AlertDto.SubscriptionResponse subscribe(String email, AlertDto.SubscribeRequest req) {
        User user = findUser(email);

        // 중복 체크
        if (subscriptionRepository.existsByUserIdAndSidoNameAndSigunguNameAndIsActiveTrue(
                user.getId(), req.sidoName(), req.sigunguName())) {
            throw new IllegalArgumentException("이미 구독 중인 지역입니다.");
        }

        AlertSubscription subscription = AlertSubscription.builder()
                .user(user)
                .alertType(req.alertType())
                .sidoName(req.sidoName())
                .sigunguName(req.sigunguName())
                .label(req.label())
                .isMyLocation(req.isMyLocation() != null && req.isMyLocation())
                .build();

        return AlertDto.SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Transactional
    public void unsubscribe(String email, UUID subscriptionId) {
        User user = findUser(email);
        AlertSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));

        if (!sub.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        sub.deactivate();
    }

    @Transactional(readOnly = true)
    public List<AlertDto.SubscriptionResponse> getMySubscriptions(String email) {
        User user = findUser(email);
        return subscriptionRepository.findAllByUserIdAndIsActiveTrue(user.getId())
                .stream()
                .map(AlertDto.SubscriptionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertDto.AlertHistoryResponse> getAlertHistory() {
        return alertRepository.findTop20ByOrderByIssuedAtDesc()
                .stream()
                .map(AlertDto.AlertHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertDto.AlertHistoryResponse> getMyLocationAlerts(String email) {
        User user = findUser(email);
        // 내 지역 구독 목록 가져와서 해당 지역 알림만 필터링
        List<AlertSubscription> myLocSubs = subscriptionRepository
                .findAllByUserIdAndIsActiveTrue(user.getId())
                .stream()
                .filter(AlertSubscription::getIsMyLocation)
                .toList();

        if (myLocSubs.isEmpty()) return List.of();

        String sido = myLocSubs.get(0).getSidoName();
        return alertRepository.findTop20ByOrderByIssuedAtDesc()
                .stream()
                .filter(a -> a.getDistrictName() != null && a.getDistrictName().contains(sido))
                .map(AlertDto.AlertHistoryResponse::from)
                .toList();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}