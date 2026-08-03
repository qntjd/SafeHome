package com.safehome.safehome_api.domain.safety.repository;

import com.safehome.safehome_api.domain.safety.entity.SafetyFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface SafetyFacilityRepository extends JpaRepository<SafetyFacility, UUID> {

    List<SafetyFacility> findAllByDistrictCode(String districtCode);

    @Query("""
        SELECT f FROM SafetyFacility f
        WHERE f.isActive = true
          AND f.lat BETWEEN :minLat AND :maxLat
          AND f.lng BETWEEN :minLng AND :maxLng
          AND (6371000 * acos(
                cos(radians(:lat)) * cos(radians(f.lat)) *
                cos(radians(f.lng) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(f.lat))
              )) <= :radiusMeters
    """)
    List<SafetyFacility> findWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );

    long countByDistrictCodeAndType(String districtCode, SafetyFacility.FacilityType type);
}