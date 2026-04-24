package com.safehome.safehome_api.domain.alert.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "disaster_alerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class DisasterAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private String districtCode;

    private String districtName;

    private Double lat;
    private Double lng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertLevel level;

    // 외부 API 원본 ID (중복 방지)
    @Column(unique = true)
    private String externalId;

    @CreationTimestamp
    private LocalDateTime issuedAt;

    public enum AlertLevel {
        INFO,     // 일반
        WARNING,  // 주의
        DANGER    // 위험
    }
}