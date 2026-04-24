package com.safehome.safehome_api.domain.alert.repository;

import com.safehome.safehome_api.domain.alert.entity.AlertSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AlertSubscriptionRepository extends JpaRepository<AlertSubscription, UUID> {

    List<AlertSubscription> findAllByUserIdAndIsActiveTrue(UUID userId);

    // 특정 좌표가 구독 반경 내에 있는 활성 구독 조회
    @Query("""
        SELECT s FROM AlertSubscription s
        WHERE s.isActive = true
          AND (6371 * acos(
                cos(radians(:lat)) * cos(radians(s.centerLat)) *
                cos(radians(s.centerLng) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(s.centerLat))
              )) <= s.radiusKm
    """)
    List<AlertSubscription> findActiveSubscriptionsNear(
            @Param("lat") double lat,
            @Param("lng") double lng
    );
}