package com.safehome.safehome_api.domain.alert.repository;

import com.safehome.safehome_api.domain.alert.entity.AlertSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AlertSubscriptionRepository extends JpaRepository<AlertSubscription, UUID> {

    List<AlertSubscription> findAllByUserIdAndIsActiveTrue(UUID userId);

    // 재난문자 수신 지역과 매칭되는 구독자 조회
    @Query("""
        SELECT s FROM AlertSubscription s
        WHERE s.isActive = true
          AND (
            :districtName LIKE CONCAT('%', s.sidoName, '%')
            OR (s.sigunguName IS NOT NULL AND :districtName LIKE CONCAT('%', s.sigunguName, '%'))
          )
    """)
    List<AlertSubscription> findActiveSubscriptionsByDistrict(
            @Param("districtName") String districtName
    );

    // 중복 구독 체크
    boolean existsByUserIdAndSidoNameAndSigunguNameAndIsActiveTrue(
            UUID userId, String sidoName, String sigunguName
    );
}