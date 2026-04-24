package com.safehome.safehome_api.domain.alert.dto;

import com.safehome.safehome_api.domain.alert.entity.AlertSubscription;
import com.safehome.safehome_api.domain.alert.entity.DisasterAlert;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

public class AlertDto {

    public record SubscribeRequest(
            @NotNull AlertSubscription.AlertType alertType,
            @NotNull Double centerLat,
            @NotNull Double centerLng,
            @Positive Double radiusKm    // 기본 3km
    ) {}

    public record SubscriptionResponse(
            UUID id,
            String alertType,
            Double centerLat,
            Double centerLng,
            Double radiusKm,
            Boolean isActive
    ) {
        public static SubscriptionResponse from(AlertSubscription s) {
            return new SubscriptionResponse(
                    s.getId(),
                    s.getAlertType().name(),
                    s.getCenterLat(), s.getCenterLng(),
                    s.getRadiusKm(), s.getIsActive()
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
                    a.getId(), a.getTitle(), a.getMessage(),
                    a.getDistrictName(), a.getLevel().name(), a.getIssuedAt()
            );
        }
    }
}