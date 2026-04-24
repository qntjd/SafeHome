package com.safehome.safehome_api.domain.safety.repository;

import com.safehome.safehome_api.domain.safety.entity.DistrictScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DistrictScoreRepository extends JpaRepository<DistrictScore, UUID> {

    Optional<DistrictScore> findByDistrictCode(String districtCode);

    // 반경 내 행정동 점수 목록 (격자 히트맵용)
    @Query("""
        SELECT d FROM DistrictScore d
        WHERE d.totalScore > 0
        ORDER BY d.totalScore DESC
    """)
    List<DistrictScore> findAllOrderByScore();
}