package com.safehome.safehome_api.domain.safety.repository;

import com.safehome.safehome_api.domain.safety.entity.CrimeStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CrimeStatRepository extends JpaRepository<CrimeStat, UUID> {

    @Query("""
        SELECT COALESCE(SUM(c.count), 0) FROM CrimeStat c
        WHERE c.districtCode = :districtCode
          AND c.year = :year
          AND c.month = :month
    """)
    int sumCountByDistrictAndYearMonth(
            @Param("districtCode") String districtCode,
            @Param("year") int year,
            @Param("month") int month
    );

    List<CrimeStat> findAllByDistrictCode(String districtCode);

    List<CrimeStat> findAllByYear(int year);
}