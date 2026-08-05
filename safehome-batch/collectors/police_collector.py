import requests
from config import API_KEY, KAKAO_REST_API_KEY
from db import get_connection, upsert_facility

POLICE_API_URL = "https://api.odcloud.kr/api/15077036/v1/uddi:6b371c66-09a5-4efd-8445-bfd53672542e"

# 시도청 필드값 → (시도코드, 시도명) — CCTV/비상벨과 동일한 코드 체계
SIDO_POLICE_MAP = [
    ("서울청", "11", "서울"),
    ("부산청", "21", "부산"),
    ("대구청", "22", "대구"),
    ("인천청", "23", "인천"),
    ("광주청", "24", "광주"),
    ("대전청", "25", "대전"),
    ("울산청", "26", "울산"),
    ("세종청", "36", "세종"),
    ("경기남부청", "31", "경기"),
    ("경기북부청", "31", "경기"),
    ("강원청", "32", "강원"),
    ("충북청", "33", "충북"),
    ("충남청", "34", "충남"),
    ("전북청", "35", "전북"),
    ("전남청", "46", "전남"),
    ("경북청", "47", "경북"),
    ("경남청", "48", "경남"),
    ("제주청", "50", "제주"),
]

def get_sido_info(sido_field: str):
    for key, code, name in SIDO_POLICE_MAP:
        if key in sido_field:
            return code, name
    return None

def get_coords(addr: str, name: str):
    """주소로 좌표 검색, 실패 시 관서명으로 재시도"""
    for query in [addr, name]:
        if not query:
            continue
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
                return lat, lng
        except Exception as e:
            print(f"  좌표 검색 오류: {e}")
            continue
    return None

def collect_police():
    print("[경찰서] 전국 수집 시작")
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
                sido_field = item.get("시도청", "")
                sido_info = get_sido_info(sido_field)
                if not sido_info:
                    continue
                district_code, district_name = sido_info

                addr = item.get("주소", "").strip()
                name = item.get("관서명", "").strip()

                if not addr and not name:
                    continue

                result = get_coords(addr, name)
                if not result:
                    print(f"  [경찰서] 좌표 실패: {name}")
                    continue

                lat, lng = result

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
                except Exception as e:
                    print(f"  [경찰서] DB 저장 실패: {name} - {e}")
                    continue

            total += count
            print(f"[경찰서] 페이지 {page} → {count}건 저장 (누적 {total}건)")

            total_count = data.get("totalCount", 0)
            if page * 100 >= total_count:
                break
            page += 1

        except Exception as e:
            print(f"[경찰서] 수집 실패 (페이지 {page}): {e}")
            break

    conn.close()
    print(f"[경찰서] 전국 수집 완료 → 총 {total}건")