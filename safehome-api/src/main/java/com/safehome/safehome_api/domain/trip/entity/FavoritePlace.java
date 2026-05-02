package com.safehome.safehome_api.domain.trip.entity;

import com.safehome.safehome_api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "favorite_places", indexes = {
        @Index(name = "idx_favorite_user", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class FavoritePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;       // 집, 직장, 학교 등

    @Column(nullable = false)
    private String address;    // 도로명 주소

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlaceType placeType = PlaceType.CUSTOM;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum PlaceType {
        HOME, WORK, SCHOOL, CUSTOM
    }
}