package com.safehome.safehome_api.domain.sos.entity;

import com.safehome.safehome_api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Entity
@Table(name ="sos_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SosLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private TriggerType triggerType;

    private Double lat;
    private Double lng;
    private String address;

    @Column(name ="police_reported")
    @Builder.Default
    private Boolean policeReported = false;

    @Column(name ="created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "sosLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SosLogRecipient> recipients = new java.util.ArrayList<>();

    public enum TriggerType {
        VOICE, LOCK_SCREEN, MANUAL, WATCHDOG
    }
}
