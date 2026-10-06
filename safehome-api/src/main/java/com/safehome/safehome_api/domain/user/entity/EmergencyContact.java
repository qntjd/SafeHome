package com.safehome.safehome_api.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "emergency_contacts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_contact_user_phone",
                columnNames = {"user_id", "phone"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Builder.Default
    private Integer notifyAfterMin = 10; 
}