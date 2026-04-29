package com.safehome.safehome_api.domain.alert.entity;

import com.safehome.safehome_api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert_subscriptions", indexes = {
        @Index(name = "idx_alert_sub_user", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class AlertSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;

    // 시도명 (예: 대구광역시, 서울특별시)
    @Column(nullable = false)
    private String sidoName;

    // 시군구명 (예: 달서구, 강남구) — null이면 시도 전체
    private String sigunguName;

    // 구독 라벨 (예: 집, 직장, 부모님댁)
    private String label;

    // 내 지역 여부 (현재 위치 기반 자동 등록)
    @Builder.Default
    private Boolean isMyLocation = false;

    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void deactivate() {
        this.isActive = false;
    }

    public void updateLabel(String label) {
        this.label = label;
    }

    
    public String getDisplayName() {
        if (sigunguName != null && !sigunguName.isBlank()) {
            return sidoName + " " + sigunguName;
        }
        return sidoName;
    }

    public enum AlertType {
        DISASTER,
        CRIME,
        ALL
    }
}