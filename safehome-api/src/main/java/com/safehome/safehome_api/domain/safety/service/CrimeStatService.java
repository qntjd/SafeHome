package com.safehome.safehome_api.domain.safety.service;

import com.safehome.safehome_api.domain.safety.dto.CrimeStatDto;
import com.safehome.safehome_api.domain.safety.entity.CrimeStat;
import com.safehome.safehome_api.domain.safety.repository.CrimeStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrimeStatService {

    private final CrimeStatRepository crimeStatRepository;

    private static final Map<String, String> DISTRICT_NAMES = Map.of(
        "2771010100", "대구 중구",
        "2771010200", "대구 동구",
        "2771010300", "대구 서구",
        "2771010400", "대구 남구",
        "2771010500", "대구 북구",
        "2771010600", "대구 수성구",
        "2771010700", "대구 달서구",
        "2771010800", "대구 달성군"
    );

    private static final Map<String, String> CRIME_TYPE_LABELS = Map.of(
        "VIOLENT", "강력범죄",
        "ASSAULT", "폭행",
        "THEFT",   "절도",
        "FRAUD",   "사기·지능",
        "VICE",    "풍속·마약",
        "OTHER",   "기타"
    );

    @Transactional(readOnly = true)
    public CrimeStatDto.AllDistrictCrimeResponse getAllDistrictCrimes() {
        List<CrimeStat> stats = crimeStatRepository.findAllByYear(2024);

        // districtCode별 그룹핑
        Map<String, List<CrimeStat>> grouped = stats.stream()
                .collect(Collectors.groupingBy(CrimeStat::getDistrictCode));

        List<CrimeStatDto.DistrictCrimeResponse> districts = grouped.entrySet().stream()
                .map(entry -> {
                    String code = entry.getKey();
                    List<CrimeStat> districtStats = entry.getValue();

                    Map<String, Integer> crimeByType = districtStats.stream()
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
                })
                .sorted(Comparator.comparingInt(CrimeStatDto.DistrictCrimeResponse::totalCount).reversed())
                .toList();

        return new CrimeStatDto.AllDistrictCrimeResponse(districts);
    }

    @Transactional(readOnly = true)
    public CrimeStatDto.DistrictCrimeResponse getDistrictCrimes(String districtCode) {
        List<CrimeStat> stats = crimeStatRepository.findAllByDistrictCode(districtCode);

        Map<String, Integer> crimeByType = stats.stream()
                .collect(Collectors.toMap(
                        s -> CRIME_TYPE_LABELS.getOrDefault(s.getCrimeType().name(), s.getCrimeType().name()),
                        CrimeStat::getCount,
                        Integer::sum
                ));

        int totalCount = crimeByType.values().stream().mapToInt(Integer::intValue).sum();

        return new CrimeStatDto.DistrictCrimeResponse(
                districtCode,
                DISTRICT_NAMES.getOrDefault(districtCode, districtCode),
                crimeByType,
                totalCount
        );
    }
}