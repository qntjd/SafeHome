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

    private static final Map<String, String> CRIME_TYPE_LABELS = Map.of(
        "VIOLENT", "강력범죄",
        "ASSAULT", "폭행",
        "THEFT",   "절도",
        "FRAUD",   "사기·지능",
        "VICE",    "풍속·마약",
        "TRAFFIC", "교통범죄",
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

    // 시/도 단위로 묶고, 실제 시/군/구 세부 데이터가 있는 시/도는 그 목록도 함께 내려준다.
    // (수집기가 실패해 시/도 단위로만 집계된 지역은 districts가 빈 리스트로 내려간다 — 프론트에서
    //  펼치기 UI를 숨기는 신호로 쓰면 된다)
    @Cacheable(value = "crimesGrouped", key = "#year")
    @Transactional(readOnly = true)
    public CrimeStatDto.AllDistrictCrimeGroupedResponse getAllDistrictCrimesGrouped(Integer year) {
        List<CrimeStat> stats = crimeStatRepository.findAllByYear(year);

        Map<String, List<CrimeStat>> byDistrict = stats.stream()
                .collect(Collectors.groupingBy(CrimeStat::getDistrictCode));

        Map<String, List<Map.Entry<String, List<CrimeStat>>>> bySido = byDistrict.entrySet().stream()
                .collect(Collectors.groupingBy(e -> sidoPrefixOf(e.getKey())));

        List<CrimeStatDto.SidoGroupResponse> groups = bySido.entrySet().stream()
                .map(entry -> buildSidoGroup(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(CrimeStatDto.SidoGroupResponse::totalCount).reversed())
                .toList();

        return new CrimeStatDto.AllDistrictCrimeGroupedResponse(groups);
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

        String districtName = stats.stream()
                .findFirst()
                .map(CrimeStat::getDistrictName)
                .orElse(districtCode);

        return new CrimeStatDto.CrimeTrendResponse(districtCode, districtName, yearlyTrend);
    }

    private String sidoPrefixOf(String districtCode) {
        return districtCode.length() >= 2 ? districtCode.substring(0, 2) : districtCode;
    }

    private CrimeStatDto.SidoGroupResponse buildSidoGroup(
            String sidoCode, List<Map.Entry<String, List<CrimeStat>>> districtEntries
    ) {
        List<CrimeStatDto.DistrictCrimeResponse> districtResponses = districtEntries.stream()
                .map(e -> buildDistrictResponse(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(CrimeStatDto.DistrictCrimeResponse::totalCount).reversed())
                .toList();

        // 시/군/구 매칭 실패로 시/도 코드 자체로만 집계된 경우 = 하위 항목이 자기 자신 하나뿐
        boolean sidoOnly = districtResponses.size() == 1
                && districtResponses.get(0).districtCode().equals(sidoCode);

        String sidoName = sidoOnly
                ? districtResponses.get(0).districtName()
                : districtResponses.get(0).districtName().split(" ")[0];

        // 시/군/구 일부만 매칭 실패해 시/도 코드로 뭉뚱그려진 항목은, 다른 구체적인 시/군/구들과
        // 나란히 놓였을 때 시/도 이름과 헷갈리지 않도록 "OO 기타"로 구분해 보여준다.
        if (!sidoOnly) {
            String finalSidoName = sidoName;
            districtResponses = districtResponses.stream()
                    .map(d -> d.districtCode().equals(sidoCode)
                            ? new CrimeStatDto.DistrictCrimeResponse(
                                    d.districtCode(), finalSidoName + " 기타", d.crimeByType(), d.totalCount())
                            : d)
                    .toList();
        }

        Map<String, Integer> mergedCrimeByType = new HashMap<>();
        int total = 0;
        for (CrimeStatDto.DistrictCrimeResponse d : districtResponses) {
            total += d.totalCount();
            d.crimeByType().forEach((type, count) -> mergedCrimeByType.merge(type, count, Integer::sum));
        }

        return new CrimeStatDto.SidoGroupResponse(
                sidoCode,
                sidoName,
                mergedCrimeByType,
                total,
                sidoOnly ? List.of() : districtResponses
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

        String districtName = stats.stream()
                .findFirst()
                .map(CrimeStat::getDistrictName)
                .orElse(code);

        return new CrimeStatDto.DistrictCrimeResponse(code, districtName, crimeByType, totalCount);
    }
}
