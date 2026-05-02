package com.safehome.safehome_api.domain.trip.dto;

import com.safehome.safehome_api.domain.trip.entity.FavoritePlace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class FavoritePlaceDto {

    public record CreateRequest(
            @NotBlank String name,
            @NotBlank String address,
            @NotNull Double lat,
            @NotNull Double lng,
            FavoritePlace.PlaceType placeType
    ) {}

    public record PlaceResponse(
            UUID id,
            String name,
            String address,
            Double lat,
            Double lng,
            String placeType
    ) {
        public static PlaceResponse from(FavoritePlace p) {
            return new PlaceResponse(
                    p.getId(),
                    p.getName(),
                    p.getAddress(),
                    p.getLat(),
                    p.getLng(),
                    p.getPlaceType().name()
            );
        }
    }
}