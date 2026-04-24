import requests
from config import API_KEY, KAKAO_REST_API_KEY
from db import get_connection, upsert_facility

POLICE_API_URL = "https://api.odcloud.kr/api/15077036/v1/uddi:6b371c66-09a5-4efd-8445-bfd53672542e"

DAEGU_DISTRICT_MAP = {
    "중구":   ("2771010100", "대구 중구"),
    "동구":   ("2771010200", "대구 동구"),
    "서구":   ("2771010300", "대구 서구"),
    "남구":   ("2771010400", "대구 남구"),
    "북구":   ("2771010500", "대구 북구"),
    "수성구": ("2771010600", "대구 수성구"),
    "달서구": ("2771010700", "대구 달서구"),
    "달성군": ("2771010800", "대구 달성군"),
}

def get_daegu_district(addr: str) -> tuple[str, str]:
    for name, (code, full_name) in DAEGU_DISTRICT_MAP.items():
        if name in addr:
            return code, full_name
    return ("2771000000", "대구광역시")

def get_coords(addr: str, name: str):
    """주소로 좌표 검색, 실패 시 관서명으로 재시도"""
    for query in [addr, f"대구 {name}"]:
        try:
            res = requests.get(
                "https://dapi.kakao.com/v2/local/search/keyword.json",
                headers={"Authorization": f"KakaoAK {KAKAO_REST_API_KEY}"},
                params={"query": query, "size": 1},
                timeout=5,
            )
            docs = res.json().get("documents", [])
            if docs:
                lat = float(docs[0]["y"])
                lng = float(docs[0]["x"])
                found_addr = docs[0].get("road_address_name") or docs[0].get("address_name", "")
                # 대구 좌표 범위 체크
                if 35.7 <= lat <= 36.0 and 128.4 <= lng <= 128.8:
                    return lat, lng, found_addr
        except Exception as e:
            print(f"  좌표 검색 오류: {e}")
            continue
    return None

def collect_police():
    print("[경찰서] 수집 시작")
    conn = get_connection()
    total = 0
    page = 1

    while True:
        try:
            params = {
                "serviceKey": API_KEY,
                "page":       page,
                "perPage":    100,
                "returnType": "json",
            }

            res = requests.get(POLICE_API_URL, params=params, timeout=10)
            res.raise_for_status()
            data = res.json()

            items = data.get("data", [])
            if not items:
                break

            count = 0
            for item in items:
                sido = item.get("시도청", "")

                # 대구청만 필터링
                if "대구청" not in sido:
                    continue

                addr = item.get("주소", "").strip()
                name = item.get("관서명", "").strip()

                if not addr and not name:
                    continue

                result = get_coords(addr, name)
                if not result:
                    print(f"  [경찰서] 좌표 실패: {name}")
                    continue

                lat, lng, found_addr = result
                district_code, district_name = get_daegu_district(found_addr or addr)

                try:
                    upsert_facility(
                        conn=conn,
                        facility_type="POLICE",
                        lat=lat,
                        lng=lng,
                        district_code=district_code,
                        district_name=district_name,
                    )
                    count += 1
                    print(f"  [경찰서] {name} → {lat:.4f}, {lng:.4f}")
                except Exception as e:
                    print(f"  [경찰서] DB 저장 실패: {name} - {e}")
                    continue

            total += count
            print(f"[경찰서] 페이지 {page} → 대구 {count}건 저장")

            total_count = data.get("totalCount", 0)
            if page * 100 >= total_count:
                break
            page += 1

        except Exception as e:
            print(f"[경찰서] 수집 실패 (페이지 {page}): {e}")
            break

    conn.close()
    print(f"[경찰서] 수집 완료 → 총 {total}건")