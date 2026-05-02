package com.safehome.safehome_api.domain.trip.entity;

import com.safehome.safehome_api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "safe_trips", indexes = {
        @Index(name = "idx_trip_user_status", columnList = "user_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SafeTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double startLat;

    @Column(nullable = false)
    private Double startLng;

    @Column(nullable = false)
    private Double endLat;

    @Column(nullable = false)
    private Double endLng;

    @Column(nullable = false)
    private LocalDateTime departureAt;

    // 예상 도착 시각 (워치독 기준)
    @Column(nullable = false)
    private LocalDateTime expectedArrivalAt;

    private LocalDateTime arrivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TripStatus status = TripStatus.IN_PROGRESS;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(unique = true)
    private String shareToken;

    public void generateShareToken() {
        this.shareToken = java.util.UUID.randomUUID().toString().replace("-", "");
    }

    public void arrive() {
        this.arrivedAt = LocalDateTime.now();
        this.status = TripStatus.ARRIVED;
    }

    public void triggerSos() {
        this.status = TripStatus.SOS;
    }

    public void cancel() {
        this.status = TripStatus.CANCELLED;
    }

    public boolean isOverdue() {
        return status == TripStatus.IN_PROGRESS
                && LocalDateTime.now().isAfter(expectedArrivalAt);
    }

    public enum TripStatus {
        IN_PROGRESS,  
        ARRIVED,      
        SOS,          
        CANCELLED     
    }
}