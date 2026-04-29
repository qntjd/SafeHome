import os
from dotenv import load_dotenv

load_dotenv()

API_KEY = os.getenv("PUBLIC_API_KEY", "YOUR_API_KEY")
KAKAO_REST_API_KEY = os.getenv("KAKAO_REST_API_KEY", "")

DB_CONFIG = {
    "host":     os.getenv("DB_HOST", "localhost"),
    "port":     int(os.getenv("DB_PORT", 5432)),
    "dbname":   os.getenv("DB_NAME", "safehome"),
    "user":     os.getenv("DB_USER", "safehome"),
    "password": os.getenv("DB_PASSWORD", "qntjd12!@"),
}

TARGET_DISTRICTS = [
    {"code": "2771010100", "name": "대구 중구"},
    {"code": "2771010200", "name": "대구 동구"},
    {"code": "2771010300", "name": "대구 서구"},
    {"code": "2771010400", "name": "대구 남구"},
    {"code": "2771010500", "name": "대구 북구"},
    {"code": "2771010600", "name": "대구 수성구"},
    {"code": "2771010700", "name": "대구 달서구"},
    {"code": "2771010800", "name": "대구 달성군"},
]

NAVER_CLIENT_ID     = os.getenv("NAVER_CLIENT_ID", "")
NAVER_CLIENT_SECRET = os.getenv("NAVER_CLIENT_SECRET", "")
NAVER_NEWS_API_URL  = "https://openapi.naver.com/v1/search/news.json"



# API 엔드포인트
CCTV_API_URL        = "https://apis.data.go.kr/1741000/cctv_info/info"
BELL_API_URL        = "https://apis.data.go.kr/1741000/emergency_call_box_info/info"
CRIME_API_URL       = "https://apis.data.go.kr/B554626/CrimeStatistics"
DISASTER_API_URL    = "https://www.safetydata.go.kr/V2/api/DSSP-IF-00247"
