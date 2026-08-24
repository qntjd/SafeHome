import requests
import json
from config import API_KEY

STAN_REGION_URL = "https://apis.data.go.kr/1741000/StanReginCd/getStanReginCdList"

SIDO_LIST = [
    "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시",
    "대전광역시", "울산광역시", "세종특별자치시", "경기도", "강원특별자치도",
    "충청북도", "충청남도", "전북특별자치도", "전라남도", "경상북도",
    "경상남도", "제주특별자치도"
]


def fetch_sigungu_list(sido_name: str) -> list[dict]:
    """특정 시/도의 시/군/구 목록만 추출 (읍면동 코드가 000인 것만)"""
    result = []
    page = 1

    while True:
        params = {
            "ServiceKey": API_KEY,
            "type": "json",
            "pageNo": page,
            "numOfRows": 100,
            "locatadd_nm": sido_name,
        }
        res = requests.get(STAN_REGION_URL, params=params, timeout=10)
        res.raise_for_status()
        data = res.json()

        rows = data.get("StanReginCd", [{}, {}])[1].get("row", [])
        if not rows:
            break

        for row in rows:
            # 시/군/구 단위만 (읍면동, 리 코드가 모두 000인 경우)
            if row.get("umd_cd") == "000" and row.get("ri_cd") == "00":
                addr = row.get("locatadd_nm", "")
                # "대구광역시 남구" → "남구", "경기도 고양시 덕양구" → "고양시덕양구"
                # (시+구 합쳐진 지역은 parts[1:]를 공백 없이 이어붙여야 범죄통계 API 컬럼명과 일치함)
                parts = addr.split(" ")
                if len(parts) >= 2:
                    sigungu_name = "".join(parts[1:])
                    result.append({
                        "code": row.get("region_cd"),
                        "sido": sido_name,
                        "sigungu": sigungu_name,
                        "full_name": addr,
                    })

        total_count = data.get("StanReginCd", [{}])[0].get("head", [{}])[0].get("totalCount", 0)
        if page * 100 >= total_count:
            break
        page += 1

    return result


def build_all_regions():
    all_regions = []
    for sido in SIDO_LIST:
        print(f"[매핑] {sido} 수집 중...")
        regions = fetch_sigungu_list(sido)
        all_regions.extend(regions)
        print(f"[매핑] {sido} → {len(regions)}건")

    return all_regions


if __name__ == "__main__":
    regions = build_all_regions()
    print(f"\n총 {len(regions)}개 시/군/구 수집 완료")

    # Python dict 파일로 저장
    with open("region_codes.py", "w", encoding="utf-8") as f:
        f.write("# 자동 생성된 전국 시/군/구 코드 매핑\n")
        f.write("# key: '시도명시군구명' (공백없음), value: 10자리 법정동코드\n\n")
        f.write("REGION_CODE_MAP = {\n")
        for r in regions:
            key = f"{r['sido']}{r['sigungu']}"
            f.write(f'    "{key}": "{r["code"]}",  # {r["full_name"]}\n')
        f.write("}\n")

    print("region_codes.py 파일 생성 완료!")