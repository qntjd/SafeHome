package com.safehome.safehome_api.domain.trip.service;

import com.safehome.safehome_api.domain.trip.dto.FavoritePlaceDto;
import com.safehome.safehome_api.domain.trip.entity.FavoritePlace;
import com.safehome.safehome_api.domain.trip.repository.FavoritePlaceRepository;
import com.safehome.safehome_api.domain.user.entity.User;
import com.safehome.safehome_api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoritePlaceService {

    private final FavoritePlaceRepository placeRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FavoritePlaceDto.PlaceResponse> getPlaces(String email) {
        User user = findUser(email);
        return placeRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId())
                .stream()
                .map(FavoritePlaceDto.PlaceResponse::from)
                .toList();
    }

    @Transactional
    public FavoritePlaceDto.PlaceResponse addPlace(String email, FavoritePlaceDto.CreateRequest req) {
        User user = findUser(email);

        if (placeRepository.countByUserId(user.getId()) >= 10) {
            throw new IllegalStateException("즐겨찾기는 최대 10개까지 저장할 수 있습니다.");
        }

        FavoritePlace place = FavoritePlace.builder()
                .user(user)
                .name(req.name())
                .address(req.address())
                .lat(req.lat())
                .lng(req.lng())
                .placeType(req.placeType() != null ? req.placeType() : FavoritePlace.PlaceType.CUSTOM)
                .build();

        return FavoritePlaceDto.PlaceResponse.from(placeRepository.save(place));
    }

    @Transactional
    public void deletePlace(String email, UUID placeId) {
        User user = findUser(email);
        FavoritePlace place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다."));

        if (!place.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        placeRepository.delete(place);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}