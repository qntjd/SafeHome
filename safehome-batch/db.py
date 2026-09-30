import os

import psycopg2
import time
from config import DB_CONFIG

def get_connection():
    return psycopg2.connect(**DB_CONFIG)

KAKAO_REST_API_KEY = os.getenv("KAKAO_REST_API_KEY")
KAKAO_COORD2ADDRESS_URL = "https://dapi.kakao.com/v2/local/geo/coord2address.json"

def get_address_from_coords(lat: float, lng: float) -> str | None:
    try:
        resp = requests.get(
            KAKAO_COORD2ADDRESS_URL,
            headers={"Authorization": f"KakaoAK {KAKAO_REST_API_KEY}"},
            params={"x": lng, "y": lat},
            timeout=3,
        )
        resp.raise_for_status()
        documents = resp.json().get("documents", [])
        if not documents:
            return None
        road = documents[0].get("road_address")
        if road:
            return road.get("address_name")
        jibun = documents[0].get("address")
        return jibun.get("address_name") if jibun else None
    except Exception as e:
        print(f"[WARN] 주소 변환 실패 (lat={lat}, lng={lng}): {e}")
        return None



def upsert_facility(conn, facility_type: str, lat: float, lng: float,
                    district_code: str, district_name: str):
    address = get_address_from_coords(lat, lng)
    time.sleep(0.05)
    with conn.cursor() as cur:
        cur.execute("""
            INSERT INTO safety_facilities (id, type, lat, lng, district_code, district_name, address,is_active, synced_at)
            VALUES (gen_random_uuid(), %s, %s, %s, %s, %s,%s, true, NOW())
            ON CONFLICT DO NOTHING
        """, (facility_type, lat, lng, district_code, district_name,address))
    conn.commit()

def upsert_crime_stat(conn, district_code: str, district_name: str, year: int, month: int,
                      crime_type: str, count: int):
    with conn.cursor() as cur:
        cur.execute("""
            INSERT INTO crime_stats (id, district_code, district_name, year, month, crime_type, count)
            VALUES (gen_random_uuid(), %s, %s, %s, %s, %s, %s)
            ON CONFLICT (district_code, year, month, crime_type)
            DO UPDATE SET count = EXCLUDED.count, district_name = EXCLUDED.district_name
        """, (district_code, district_name, year, month, crime_type, count))
    conn.commit()