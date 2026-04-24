package com.safehome.safehome_api.domain.safety.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "crime_stats", indexes = {
        @Index(name = "idx_crime_district_year_month", columnList = "districtCode, year, month")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class CrimeStat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String districtCode;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrimeType crimeType;

    @Column(nullable = false)
    private Integer count;

    public enum CrimeType {
        VIOLENT,  // 강력범죄
        ASSAULT,  // 폭행
        THEFT,    // 절도
        FRAUD,    // 사기·지능
        VICE,     // 풍속·마약
        OTHER     // 기타
    }
}