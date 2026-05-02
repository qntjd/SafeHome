package com.safehome.safehome_api.domain.trip.controller;

import com.safehome.safehome_api.domain.trip.dto.FavoritePlaceDto;
import com.safehome.safehome_api.domain.trip.service.FavoritePlaceService;
import com.safehome.safehome_api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "FavoritePlace", description = "즐겨찾기 장소 API")
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class FavoritePlaceController {

    private final FavoritePlaceService placeService;

    @Operation(summary = "즐겨찾기 목록 조회")
    @GetMapping
    public ApiResponse<List<FavoritePlaceDto.PlaceResponse>> getPlaces(
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(placeService.getPlaces(user.getUsername()));
    }

    @Operation(summary = "즐겨찾기 추가")
    @PostMapping
    public ApiResponse<FavoritePlaceDto.PlaceResponse> addPlace(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody FavoritePlaceDto.CreateRequest req
    ) {
        return ApiResponse.success(placeService.addPlace(user.getUsername(), req));
    }

    @Operation(summary = "즐겨찾기 삭제")
    @DeleteMapping("/{placeId}")
    public ApiResponse<Void> deletePlace(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID placeId
    ) {
        placeService.deletePlace(user.getUsername(), placeId);
        return ApiResponse.success(null);
    }
}