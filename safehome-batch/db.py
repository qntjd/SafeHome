import psycopg2
from config import DB_CONFIG

def get_connection():
    return psycopg2.connect(**DB_CONFIG)

def upsert_facility(conn, facility_type: str, lat: float, lng: float,
                    district_code: str, district_name: str):
    with conn.cursor() as cur:
        cur.execute("""
            INSERT INTO safety_facilities (id, type, lat, lng, district_code, district_name, is_active, synced_at)
            VALUES (gen_random_uuid(), %s, %s, %s, %s, %s, true, NOW())
            ON CONFLICT DO NOTHING
        """, (facility_type, lat, lng, district_code, district_name))
    conn.commit()

def upsert_crime_stat(conn, district_code: str, year: int, month: int,
                      crime_type: str, count: int):
    with conn.cursor() as cur:
        cur.execute("""
            INSERT INTO crime_stats (id, district_code, year, month, crime_type, count)
            VALUES (gen_random_uuid(), %s, %s, %s, %s, %s)
            ON CONFLICT (district_code, year, month, crime_type)
            DO UPDATE SET count = EXCLUDED.count
        """, (district_code, year, month, crime_type, count))
    conn.commit()