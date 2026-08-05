import requests
from config import API_KEY
from db import get_connection, upsert_crime_stat

CRIME_API_URL = "https://api.odcloud.kr/api/3074462/v1/uddi:ae109087-8690-4cb5-bda9-a7876a92f3b8"

# 컬럼명 접두어(시도 약칭) → (시도코드, 시도명) — CCTV/비상벨과 동일한 코드 체계
SIDO_MAP = {
    "서울": ("11", "서울"), "부산": ("21", "부산"), "대구": ("22", "대구"),
    "인천": ("23", "인천"), "광주": ("24", "광주"), "대전": ("25", "대전"),
    "울산": ("26", "울산"), "세종": ("36", "세종"), "경기": ("31", "경기"),
    "강원": ("32", "강원"), "충북": ("33", "충북"), "충남": ("34", "충남"),
    "전북": ("35", "전북"), "전남": ("46", "전남"), "경북": ("47", "경북"),
    "경남": ("48", "경남"), "제주": ("50", "제주"),
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
    "교통범죄":     "OTHER",
    "노동범죄":     "OTHER",
    "병역범죄":     "OTHER",
    "선거범죄":     "OTHER",
    "안보범죄":     "OTHER",
    "환경범죄":     "OTHER",
    "기타범죄":     "OTHER",
}

def collect_crime_stats():
    print("[범죄통계] 전국 수집 시작")
    conn = get_connection()
    total = 0
    page = 1

    # { district_code: { crime_type: count } }
    district_counts: dict[str, dict[str, int]] = {
        code: {} for code, _ in SIDO_MAP.values()
    }

    while True:
        try:
            params = {
                "serviceKey": API_KEY,
                "page":       page,
                "perPage":    100,
                "returnType": "json",
            }

            res = requests.get(CRIME_API_URL, params=params, timeout=10)
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
                    # 컬럼명 예: "서울 강남구" → 앞부분("서울")으로 시도 판별
                    sido_short = key.split(" ")[0]
                    if sido_short not in SIDO_MAP:
                        continue
                    district_code, _ = SIDO_MAP[sido_short]

                    count = value or 0
                    try:
                        count = int(count)
                    except (ValueError, TypeError):
                        count = 0

                    if crime_type not in district_counts[district_code]:
                        district_counts[district_code][crime_type] = 0
                    district_counts[district_code][crime_type] += count

            total_count = data.get("totalCount", 0)
            print(f"[범죄통계] 페이지 {page} 처리 완료")

            if page * 100 >= total_count:
                break
            page += 1

        except Exception as e:
            print(f"[범죄통계] 수집 실패 (페이지 {page}): {e}")
            break

    for district_code, crime_map in district_counts.items():
        for crime_type, count in crime_map.items():
            if count == 0:
                continue
            upsert_crime_stat(
                conn=conn,
                district_code=district_code,
                year=2024,
                month=0,
                crime_type=crime_type,
                count=count,
            )
            total += 1

    conn.close()
    print(f"[범죄통계] 전국 수집 완료 → 총 {total}건 저장")