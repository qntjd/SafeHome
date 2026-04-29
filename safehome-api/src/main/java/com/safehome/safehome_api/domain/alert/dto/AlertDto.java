package com.safehome.safehome_api.domain.alert.dto;

import com.safehome.safehome_api.domain.alert.entity.AlertSubscription;
import com.safehome.safehome_api.domain.alert.entity.DisasterAlert;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public class AlertDto {

    public record SubscribeRequest(
            @NotNull AlertSubscription.AlertType alertType,
            @NotBlank String sidoName,
            String sigunguName,
            String label,
            Boolean isMyLocation
    ) {}

    public record SubscriptionResponse(
            UUID id,
            String alertType,
            String sidoName,
            String sigunguName,
            String label,
            String displayName,
            Boolean isMyLocation,
            Boolean isActive
    ) {
        public static SubscriptionResponse from(AlertSubscription s) {
            return new SubscriptionResponse(
                    s.getId(),
                    s.getAlertType().name(),
                    s.getSidoName(),
                    s.getSigunguName(),
                    s.getLabel(),
                    s.getDisplayName(),
                    s.getIsMyLocation(),
                    s.getIsActive()
            );
        }
    }

    public record AlertHistoryResponse(
            UUID id,
            String title,
            String message,
            String districtName,
            String level,
            LocalDateTime issuedAt
    ) {
        public static AlertHistoryResponse from(DisasterAlert a) {
            return new AlertHistoryResponse(
                    a.getId(),
                    a.getTitle(),
                    a.getMessage(),
                    a.getDistrictName(),
                    a.getLevel().name(),
                    a.getIssuedAt()
            );
        }
    }
}