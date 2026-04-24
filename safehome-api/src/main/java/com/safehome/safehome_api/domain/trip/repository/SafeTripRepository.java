package com.safehome.safehome_api.domain.trip.repository;

import com.safehome.safehome_api.domain.trip.entity.SafeTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SafeTripRepository extends JpaRepository<SafeTrip, UUID> {

    // 현재 진행 중인 귀가 조회 (1인 1귀가 원칙)
    Optional<SafeTrip> findByUserIdAndStatus(UUID userId, SafeTrip.TripStatus status);

    // 워치독용 — 예상 도착 시각이 지났는데 아직 IN_PROGRESS 인 것들
    @Query("""
        SELECT t FROM SafeTrip t
        WHERE t.status = 'IN_PROGRESS'
          AND t.expectedArrivalAt < :now
    """)
    List<SafeTrip> findOverdueTrips(@Param("now") LocalDateTime now);
}