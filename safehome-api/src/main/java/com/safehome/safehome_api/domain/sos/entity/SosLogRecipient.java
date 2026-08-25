package com.safehome.safehome_api.domain.sos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "sos_log_recipients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SosLogRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sos_log_id", nullable = false)
    private SosLog sosLog;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name ="phone_number", nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name ="error_message")
    private String errorMessage;

    @Column(name ="sent_at")
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    public enum Status {
        SUCCESS, FAILED
    }
}
