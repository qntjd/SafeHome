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

   
    Optional<SafeTrip> findByUserIdAndStatus(UUID userId, SafeTrip.TripStatus status);
    Optional<SafeTrip> findByShareToken(String shareToken);

    
    @Query("""
        SELECT t FROM SafeTrip t
        WHERE t.status = 'IN_PROGRESS'
          AND t.expectedArrivalAt < :now
    """)
    List<SafeTrip> findOverdueTrips(@Param("now") LocalDateTime now);
}