package com.safehome.safehome_api.domain.trip.dto;

import com.safehome.safehome_api.domain.trip.entity.SafeTrip;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

public class TripDto {

    public record ShareResponse(
        String shareUrl,
        String shareToken
    ) {}

    public record ShareLocationResponse(
        Double currentLat,
        Double currentLng,
        Double endLat,
        Double endLng,
        String status,
        String nickname,
        String expectedArrivalAt
    ) {}

    public record StartRequest(
            @NotNull Double startLat,
            @NotNull Double startLng,
            @NotNull Double endLat,
            @NotNull Double endLng,
            @NotNull @Positive Integer estimatedMinutes  // 예상 소요 시간(분)
    ) {}

    public record TripResponse(
            UUID id,
            Double startLat,
            Double startLng,
            Double endLat,
            Double endLng,
            LocalDateTime departureAt,
            LocalDateTime expectedArrivalAt,
            LocalDateTime arrivedAt,
            String status,
            String shareToken
    ) {
        public static TripResponse from(SafeTrip t) {
            return new TripResponse(
                    t.getId(),
                    t.getStartLat(), t.getStartLng(),
                    t.getEndLat(),   t.getEndLng(),
                    t.getDepartureAt(),
                    t.getExpectedArrivalAt(),
                    t.getArrivedAt(),
                    t.getStatus().name(),
                    t.getShareToken()
            );
        }
    }

    
}