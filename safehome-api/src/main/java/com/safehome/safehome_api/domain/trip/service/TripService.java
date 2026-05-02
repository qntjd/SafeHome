package com.safehome.safehome_api.domain.trip.service;

import com.safehome.safehome_api.domain.trip.dto.TripDto;
import com.safehome.safehome_api.domain.trip.entity.SafeTrip;
import com.safehome.safehome_api.domain.trip.repository.SafeTripRepository;
import com.safehome.safehome_api.domain.user.entity.EmergencyContact;
import com.safehome.safehome_api.domain.user.entity.User;
import com.safehome.safehome_api.domain.user.repository.EmergencyContactRepository;
import com.safehome.safehome_api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripService {

    private final SafeTripRepository tripRepository;
    private final UserRepository userRepository;
    private final EmergencyContactRepository contactRepository;
    private final NotificationService notificationService;

    @Transactional
    public TripDto.TripResponse startTrip(String email, TripDto.StartRequest req) {
        User user = findUser(email);

        // 이미 진행 중인 귀가가 있으면 거부
        tripRepository.findByUserIdAndStatus(user.getId(), SafeTrip.TripStatus.IN_PROGRESS)
                .ifPresent(t -> { throw new IllegalStateException("이미 진행 중인 귀가가 있습니다."); });

        LocalDateTime now = LocalDateTime.now();
        SafeTrip trip = SafeTrip.builder()
                .user(user)
                .startLat(req.startLat())
                .startLng(req.startLng())
                .endLat(req.endLat())
                .endLng(req.endLng())
                .departureAt(now)
                .expectedArrivalAt(now.plusMinutes(req.estimatedMinutes()))
                .build();

        trip.generateShareToken();
        return TripDto.TripResponse.from(tripRepository.save(trip));
    }

    @Transactional
    public TripDto.TripResponse arrive(String email, UUID tripId) {
        SafeTrip trip = getOwnedTrip(email, tripId);

        if (trip.getStatus() != SafeTrip.TripStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 귀가가 아닙니다.");
        }

        trip.arrive();
        return TripDto.TripResponse.from(trip);
    }

    @Transactional
    public TripDto.TripResponse triggerSos(String email, UUID tripId) {
        SafeTrip trip = getOwnedTrip(email, tripId);
        List<EmergencyContact> contacts =
                contactRepository.findAllByUserId(trip.getUser().getId());

        contacts.forEach(c -> notificationService.sendSosAlert(trip, c));
        trip.triggerSos();

        return TripDto.TripResponse.from(trip);
    }

    @Transactional
    public void cancelTrip(String email, UUID tripId) {
        SafeTrip trip = getOwnedTrip(email, tripId);
        trip.cancel();
    }

    // ── 내부 헬퍼 ──────────────────────────────────────

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private SafeTrip getOwnedTrip(String email, UUID tripId) {
        User user = findUser(email);
        SafeTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("귀가 정보를 찾을 수 없습니다."));

        if (!trip.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return trip;
    }

    @Transactional(readOnly = true)
    public TripDto.ShareLocationResponse getSharedLocation(String shareToken) {
        SafeTrip trip = tripRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 공유 링크입니다."));

        return new TripDto.ShareLocationResponse(
                trip.getStartLat(),
                trip.getStartLng(),
                trip.getEndLat(),
                trip.getEndLng(),
                trip.getStatus().name(),
                trip.getUser().getNickname(),
                trip.getExpectedArrivalAt().toString()
        );
    }
}