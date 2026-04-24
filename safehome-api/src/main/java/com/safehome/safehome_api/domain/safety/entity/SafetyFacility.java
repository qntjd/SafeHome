package com.safehome.safehome_api.domain.safety.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "safety_facilities", indexes = {
        @Index(name = "idx_facility_district", columnList = "districtCode"),
        @Index(name = "idx_facility_type", columnList = "type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SafetyFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FacilityType type;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(nullable = false)
    private String districtCode; // 행정동 코드

    private String districtName;

    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime syncedAt;

    public enum FacilityType {
        CCTV,           // CCTV
        EMERGENCY_BELL, // 비상벨
        STREETLIGHT,    // 가로등
        POLICE          // 경찰서
    }
}