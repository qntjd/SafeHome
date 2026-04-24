package com.safehome.safehome_api.domain.safety.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "district_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class DistrictScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String districtCode;

    private String districtName;

    // 각 항목별 점수 (0.0 ~ 100.0)
    @Builder.Default
    private Double cctvScore = 0.0;

    @Builder.Default
    private Double crimeScore = 0.0;   // 범죄 점수 (낮을수록 안전)

    @Builder.Default
    private Double lightScore = 0.0;

    @Builder.Default
    private Double bellScore = 0.0;

    @Builder.Default
    private Double totalScore = 0.0;   // 가중 합산 최종 점수

    @UpdateTimestamp
    private LocalDateTime calculatedAt;

    public void updateScores(double cctv, double crime, double light, double bell) {
        this.cctvScore  = cctv;
        this.crimeScore = crime;
        this.lightScore = light;
        this.bellScore  = bell;
        // 가중치: CCTV 30% + 범죄역점수 40% + 가로등 20% + 비상벨 10%
        this.totalScore = (cctv * 0.3) + (crime * 0.4) + (light * 0.2) + (bell * 0.1);
    }
}