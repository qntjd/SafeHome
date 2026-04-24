import requests
from config import API_KEY, CCTV_API_URL, TARGET_DISTRICTS
from db import get_connection, upsert_facility

def collect_cctv():
    print("[CCTV] 수집 시작")
    conn = get_connection()
    total = 0
    page = 1

    while True:
        try:
            params = {
                "serviceKey": API_KEY,
                "type":       "json",
                "numOfRows":  1000,
                "pageNo":     page,
            }

            res = requests.get(CCTV_API_URL, params=params, timeout=10)
            res.raise_for_status()
            data = res.json()

            items = (
                data.get("response", {})
                    .get("body", {})
                    .get("items", {})
                    .get("item", [])
            )
            if isinstance(items, dict):
                items = [items]
            if not items:
                break

            count = 0
            for item in items:
                addr = item.get("LCTN_ROAD_NM_ADDR", "") or item.get("LCTN_LOTNO_ADDR", "")

                # 대구 주소만 필터링
                if "대구" not in addr:
                    continue

                lat = item.get("WGS84_LAT")
                lng = item.get("WGS84_LOT")
                if not lat or not lng:
                    continue

                # 대구 구/군 매핑
                district_code, district_name = get_daegu_district(addr)

                try:
                    upsert_facility(
                        conn=conn,
                        facility_type="CCTV",
                        lat=float(lat),
                        lng=float(lng),
                        district_code=district_code,
                        district_name=district_name,
                    )
                    count += 1
                except ValueError:
                    continue

            total += count
            print(f"[CCTV] 페이지 {page} → 대구 {count}건 저장")

            total_count = int(
                data.get("response", {})
                    .get("body", {})
                    .get("totalCount", 0)
            )
            if page * 1000 >= total_count:
                break
            page += 1

        except Exception as e:
            print(f"[CCTV] 수집 실패 (페이지 {page}): {e}")
            break

    conn.close()
    print(f"[CCTV] 수집 완료 → 총 {total}건")

def get_daegu_district(addr: str) -> tuple[str, str]:
    districts = {
        "중구":   ("2771010100", "대구 중구"),
        "동구":   ("2771010200", "대구 동구"),
        "서구":   ("2771010300", "대구 서구"),
        "남구":   ("2771010400", "대구 남구"),
        "북구":   ("2771010500", "대구 북구"),
        "수성구": ("2771010600", "대구 수성구"),
        "달서구": ("2771010700", "대구 달서구"),
        "달성군": ("2771010800", "대구 달성군"),
    }
    for name, (code, full_name) in districts.items():
        if name in addr:
            return code, full_name
    return ("2771000000", "대구광역시")