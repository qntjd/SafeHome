import requests
from config import API_KEY
from db import get_connection, upsert_crime_stat
from region_codes import REGION_CODE_MAP

# 연도별 API 엔드포인트
CRIME_API_URLS = {
    2020: "https://api.odcloud.kr/api/3074462/v1/uddi:5c067b9b-efe1-414a-8096-89b67ee686bf",
    2021: "https://api.odcloud.kr/api/3074462/v1/uddi:14dc5ecc-3702-4df9-9dae-cb2337bf93cb",
    2022: "https://api.odcloud.kr/api/3074462/v1/uddi:fe3ae686-8f7d-4d82-8c3a-901a02a0aa75",
    2023: "https://api.odcloud.kr/api/3074462/v1/uddi:161740bd-8ec5-4734-9a3d-f7a2cde34942",
    2024: "https://api.odcloud.kr/api/3074462/v1/uddi:ae109087-8690-4cb5-bda9-a7876a92f3b8",
}

# 범죄통계 API 컬럼명의 실제 접두어(대부분 약칭이지만 강원도/경기도/세종시는 접미사가 붙어서 나온다)
# → region_mapper.py가 쓰는 정식 시/도명 (REGION_CODE_MAP이 "정식시도명 + 시군구명" 키라서 매칭에 필요)
PREFIX_TO_FULL_SIDO = {
    "서울": "서울특별시",     "부산": "부산광역시",     "대구": "대구광역시",
    "인천": "인천광역시",     "광주": "광주광역시",     "대전": "대전광역시",
    "울산": "울산광역시",     "세종시": "세종특별자치시", "경기도": "경기도",
    "강원도": "강원특별자치도", "충북": "충청북도",       "충남": "충청남도",
    "전북": "전북특별자치도",  "전남": "전라남도",       "경북": "경상북도",
    "경남": "경상남도",       "제주": "제주특별자치도",
}

# 화면에 보여줄 때 쓰는 짧은 시/도명(접미사 없이 통일)
PREFIX_DISPLAY_NAME = {
    "서울": "서울", "부산": "부산", "대구": "대구", "인천": "인천", "광주": "광주",
    "대전": "대전", "울산": "울산", "세종시": "세종", "경기도": "경기", "강원도": "강원",
    "충북": "충북", "충남": "충남", "전북": "전북", "전남": "전남", "경북": "경북",
    "경남": "경남", "제주": "제주",
}

# 시/군/구 매칭이 안 될 때(세종처럼 하위 구역이 없거나, 일부 지역은 정부 API 조회 실패)
# 시/도 단위로라도 집계할 수 있도록 쓰는 법정동 표준 시/도 코드(2자리) — 실제 시/군/구
# 코드의 앞 2자리와 동일한 체계라 프론트에서 시/도 ↔ 시/군/구를 묶어 보여줄 때 그대로 맞아떨어진다.
SIDO_CODE = {
    "서울": "11", "부산": "26", "대구": "27", "인천": "28", "광주": "29",
    "대전": "30", "울산": "31", "세종시": "36", "경기도": "41", "강원도": "42",
    "충북": "43", "충남": "44", "전북": "45", "전남": "46", "경북": "47",
    "경남": "48", "제주": "50",
}

CRIME_TYPE_MAP = {
    "강력범죄":     "VIOLENT",
    "폭력범죄":     "ASSAULT",
    "절도범죄":     "THEFT",
    "지능범죄":     "FRAUD",
    "특별경제범죄": "FRAUD",
    "마약범죄":     "VICE",
    "풍속범죄":     "VICE",
    "보건범죄":     "VICE",
    "교통범죄":     "TRAFFIC",
    "노동범죄":     "OTHER",
    "병역범죄":     "OTHER",
    "선거범죄":     "OTHER",
    "안보범죄":     "OTHER",
    "환경범죄":     "OTHER",
    "기타범죄":     "OTHER",
}


def match_region(key: str):
    """컬럼명("대구 남구", "경기도 가평군" 등)을 시/군/구 단위 실제 법정동코드로, 안되면 시/도 코드로 매칭"""
    for prefix, full_sido in PREFIX_TO_FULL_SIDO.items():
        if not key.startswith(prefix):
            continue

        display_sido  = PREFIX_DISPLAY_NAME[prefix]
        sigungu_name  = key[len(prefix):].strip()

        if sigungu_name:
            lookup_key = f"{full_sido}{sigungu_name}"
            code = REGION_CODE_MAP.get(lookup_key)
            if code:
                return code, f"{display_sido} {sigungu_name}"

        # 시/군/구 매칭 실패(세종처럼 하위구역이 없거나, 코드 매핑 누락) → 시/도 단위로 집계
        return SIDO_CODE[prefix], display_sido

    return None


def collect_crime_stats():
    print("[범죄통계] 전국 수집 시작 (2020~2024)")
    conn = get_connection()
    total = 0

    for year, api_url in CRIME_API_URLS.items():
        print(f"[범죄통계] {year}년 수집 시작")
        year_total = collect_year(conn, year, api_url)
        total += year_total
        print(f"[범죄통계] {year}년 수집 완료 → {year_total}건")

    conn.close()
    print(f"[범죄통계] 전체 수집 완료 → 총 {total}건 저장")


def collect_year(conn, year: int, api_url: str) -> int:
    total = 0
    page = 1

    # district_code -> (district_name, {crime_type: count})
    district_counts: dict[str, tuple[str, dict[str, int]]] = {}

    while True:
        try:
            params = {
                "serviceKey": API_KEY,
                "page":       page,
                "perPage":    100,
                "returnType": "json",
            }

            res = requests.get(api_url, params=params, timeout=10)
            res.raise_for_status()
            data = res.json()

            items = data.get("data", [])
            if not items:
                break

            for item in items:
                crime_type_raw = item.get("범죄대분류", "")
                crime_type = CRIME_TYPE_MAP.get(crime_type_raw, "OTHER")

                for key, value in item.items():
                    if key in ("범죄대분류", "범죄중분류"):
                        continue
                    matched = match_region(key)
                    if not matched:
                        continue
                    district_code, district_name = matched

                    count = value or 0
                    try:
                        count = int(count)
                    except (ValueError, TypeError):
                        count = 0

                    if district_code not in district_counts:
                        district_counts[district_code] = (district_name, {})
                    _, crime_map = district_counts[district_code]
                    crime_map[crime_type] = crime_map.get(crime_type, 0) + count

            total_count = data.get("totalCount", 0)

            if page * 100 >= total_count:
                break
            page += 1

        except Exception as e:
            print(f"[범죄통계] {year}년 수집 실패 (페이지 {page}): {e}")
            break

    for district_code, (district_name, crime_map) in district_counts.items():
        for crime_type, count in crime_map.items():
            if count == 0:
                continue
            upsert_crime_stat(
                conn=conn,
                district_code=district_code,
                district_name=district_name,
                year=year,
                month=0,
                crime_type=crime_type,
                count=count,
            )
            total += 1

    return total
