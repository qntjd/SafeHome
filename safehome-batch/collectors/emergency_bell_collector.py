import requests
from config import API_KEY, BELL_API_URL
from db import get_connection, upsert_facility

SIDO_MAP = {
    "서울특별시": ("11", "서울"),
    "부산광역시": ("21", "부산"),
    "대구광역시": ("22", "대구"),
    "인천광역시": ("23", "인천"),
    "광주광역시": ("24", "광주"),
    "대전광역시": ("25", "대전"),
    "울산광역시": ("26", "울산"),
    "세종특별자치시": ("36", "세종"),
    "경기도": ("31", "경기"),
    "강원도": ("32", "강원"),
    "충청북도": ("33", "충북"),
    "충청남도": ("34", "충남"),
    "전라북도": ("35", "전북"),
    "전라남도": ("46", "전남"),
    "경상북도": ("47", "경북"),
    "경상남도": ("48", "경남"),
    "제주특별자치도": ("50", "제주"),
}

def get_district_info(addr: str) -> tuple[str, str]:
    for sido, (code, name) in SIDO_MAP.items():
        if sido in addr:
            return code, name
    return ("00", "기타")

def collect_emergency_bells():
    print("[비상벨] 전국 수집 시작")
    conn = get_connection()
    total = 0
    page  = 1

    while True:
        try:
            params = {
                "serviceKey": API_KEY,
                "type":       "json",
                "numOfRows":  1000,
                "pageNo":     page,
            }

            res = requests.get(BELL_API_URL, params=params, timeout=30)
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
                lat = item.get("WGS84_LAT")
                lng = item.get("WGS84_LOT")
                if not lat or not lng:
                    continue

                addr = item.get("LCTN_ROAD_NM_ADDR", "") or item.get("LCTN_LOTNO_ADDR", "")
                district_code, district_name = get_district_info(addr)

                try:
                    upsert_facility(
                        conn=conn,
                        facility_type="EMERGENCY_BELL",
                        lat=float(lat),
                        lng=float(lng),
                        district_code=district_code,
                        district_name=district_name,
                    )
                    count += 1
                except ValueError:
                    continue

            total += count
            print(f"[비상벨] 페이지 {page} → {count}건 저장 (누적 {total}건)")

            total_count = int(
                data.get("response", {})
                    .get("body", {})
                    .get("totalCount", 0)
            )
            if page * 1000 >= total_count:
                break
            page += 1

        except Exception as e:
            print(f"[비상벨] 수집 실패 (페이지 {page}): {e}")
            break

    conn.close()
    print(f"[비상벨] 전국 수집 완료 → 총 {total}건")