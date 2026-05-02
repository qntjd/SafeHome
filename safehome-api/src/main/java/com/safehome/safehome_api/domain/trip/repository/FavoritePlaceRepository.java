package com.safehome.safehome_api.domain.trip.repository;

import com.safehome.safehome_api.domain.trip.entity.FavoritePlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FavoritePlaceRepository extends JpaRepository<FavoritePlace, UUID> {
    List<FavoritePlace> findAllByUserIdOrderByCreatedAtAsc(UUID userId);
    long countByUserId(UUID userId);
}