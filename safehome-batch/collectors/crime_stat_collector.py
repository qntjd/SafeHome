import requests
from config import API_KEY, TARGET_DISTRICTS
from db import get_connection, upsert_crime_stat

# 2024년 데이터 엔드포인트
CRIME_API_URL = "https://api.odcloud.kr/api/3074462/v1/uddi:ae109087-8690-4cb5-bda9-a7876a92f3b8"

# 대구 지역 컬럼명 매핑 (응답 필드명 → 우리 districtCode)
DAEGU_DISTRICT_MAP = {
    "대구 중구":   "2771010100",
    "대구 동구":   "2771010200",
    "대구 서구":   "2771010300",
    "대구 남구":   "2771010400",
    "대구 북구":   "2771010500",
    "대구 수성구": "2771010600",
    "대구 달서구": "2771010700",
    "대구 달성군": "2771010800",
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
    print("[범죄통계] 수집 시작")
    conn = get_connection()
    total = 0
    page = 1

    # 지역별 범죄 건수 누적 딕셔너리
    # { district_code: { crime_type: count } }
    district_counts: dict[str, dict[str, int]] = {
        code: {} for code in DAEGU_DISTRICT_MAP.values()
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

                # 대구 각 구별 건수 추출
                for district_name, district_code in DAEGU_DISTRICT_MAP.items():
                    count = item.get(district_name, 0) or 0
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

    # DB 저장
    from datetime import datetime
    now = datetime.now()
    for district_code, crime_map in district_counts.items():
        for crime_type, count in crime_map.items():
            if count == 0:
                continue
            upsert_crime_stat(
                conn=conn,
                district_code=district_code,
                year=2024,
                month=0,  # 연간 통계
                crime_type=crime_type,
                count=count,
            )
            total += 1

    conn.close()
    print(f"[범죄통계] 수집 완료 → 총 {total}건 저장")