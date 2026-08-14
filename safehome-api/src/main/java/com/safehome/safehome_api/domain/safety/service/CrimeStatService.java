package com.safehome.safehome_api.domain.safety.service;

import com.safehome.safehome_api.domain.safety.dto.CrimeStatDto;
import com.safehome.safehome_api.domain.safety.entity.CrimeStat;
import com.safehome.safehome_api.domain.safety.repository.CrimeStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrimeStatService {

    private final CrimeStatRepository crimeStatRepository;

    private static final Map<String, String> DISTRICT_NAMES = Map.ofEntries(
        Map.entry("11", "서울"),
        Map.entry("21", "부산"),
        Map.entry("22", "대구"),
        Map.entry("23", "인천"),
        Map.entry("24", "광주"),
        Map.entry("25", "대전"),
        Map.entry("26", "울산"),
        Map.entry("36", "세종"),
        Map.entry("31", "경기"),
        Map.entry("32", "강원"),
        Map.entry("33", "충북"),
        Map.entry("34", "충남"),
        Map.entry("35", "전북"),
        Map.entry("46", "전남"),
        Map.entry("47", "경북"),
        Map.entry("48", "경남"),
        Map.entry("50", "제주")
    );

    private static final Map<String, String> CRIME_TYPE_LABELS = Map.of(
        "VIOLENT", "강력범죄",
        "ASSAULT", "폭행",
        "THEFT",   "절도",
        "FRAUD",   "사기·지능",
        "VICE",    "풍속·마약",
        "OTHER",   "기타"
    );

    @Cacheable(value = "crimes", key = "#year")
    @Transactional(readOnly = true)
    public CrimeStatDto.AllDistrictCrimeResponse getAllDistrictCrimes(Integer year) {
        List<CrimeStat> stats = crimeStatRepository.findAllByYear(year);

        Map<String, List<CrimeStat>> grouped = stats.stream()
                .collect(Collectors.groupingBy(CrimeStat::getDistrictCode));

        List<CrimeStatDto.DistrictCrimeResponse> districts = grouped.entrySet().stream()
                .map(entry -> buildDistrictResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(CrimeStatDto.DistrictCrimeResponse::totalCount).reversed())
                .toList();

        return new CrimeStatDto.AllDistrictCrimeResponse(districts);
    }

    @Transactional(readOnly = true)
    public CrimeStatDto.DistrictCrimeResponse getDistrictCrimes(String districtCode, Integer year) {
        List<CrimeStat> stats = crimeStatRepository.findAllByDistrictCodeAndYear(districtCode, year);
        return buildDistrictResponse(districtCode, stats);
    }

    @Transactional(readOnly = true)
    public CrimeStatDto.CrimeTrendResponse getCrimeTrend(String districtCode) {
        List<CrimeStat> stats = crimeStatRepository.findAllByDistrictCode(districtCode);

        Map<Integer, Integer> byYear = stats.stream()
                .collect(Collectors.groupingBy(
                        CrimeStat::getYear,
                        Collectors.summingInt(CrimeStat::getCount)
                ));

        List<CrimeStatDto.YearlyCrimeCount> yearlyTrend = byYear.entrySet().stream()
                .map(e -> new CrimeStatDto.YearlyCrimeCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(CrimeStatDto.YearlyCrimeCount::year))
                .toList();

        return new CrimeStatDto.CrimeTrendResponse(
                districtCode,
                DISTRICT_NAMES.getOrDefault(districtCode, districtCode),
                yearlyTrend
        );
    }

    private CrimeStatDto.DistrictCrimeResponse buildDistrictResponse(String code, List<CrimeStat> stats) {
        Map<String, Integer> crimeByType = stats.stream()
                .collect(Collectors.toMap(
                        s -> CRIME_TYPE_LABELS.getOrDefault(s.getCrimeType().name(), s.getCrimeType().name()),
                        CrimeStat::getCount,
                        Integer::sum
                ));

        int totalCount = crimeByType.values().stream().mapToInt(Integer::intValue).sum();

        return new CrimeStatDto.DistrictCrimeResponse(
                code,
                DISTRICT_NAMES.getOrDefault(code, code),
                crimeByType,
                totalCount
        );
    }
}