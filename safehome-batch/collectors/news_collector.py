import requests
import html
import re
from datetime import datetime
from config import (
    NAVER_CLIENT_ID,
    NAVER_CLIENT_SECRET,
    NAVER_NEWS_API_URL,
)
from db import get_connection

# 전국 단위 키워드
NEWS_KEYWORDS = [
    # 범죄
    "살인 검거",
    "강도 검거",
    "성범죄 체포",
    "절도 검거",
    "폭행 체포",
    "사기 검거",
    # 재난
    "화재 진화",
    "산불 진화",
    "홍수 대피",
    "태풍 피해",
    "지진 발생",
    "폭설 피해",
    # 안전사고
    "교통사고 사망",
    "교통사고 부상",
    "추락사고",
    "익사사고",
    # 재난문자
    "긴급재난문자",
    "안전안내문자",
]

# 반드시 포함되어야 할 단어 (OR 조건)
REQUIRED_WORDS = [
    '경찰', '소방', '검거', '체포', '사망', '부상', '피해',
    '화재', '사고', '재난', '긴급', '대피', '진화', '발생',
]

# 제외할 단어
EXCLUDE_WORDS = [
    '드라마', '영화', '소설', '웹툰', '게임', '예능',
    '축구', '야구', '농구', '배구', '골프',
    '주식', '코인', '투자', '펀드',
    '맛집', '여행', '쇼핑', '패션',
    '연예', '아이돌', '콘서트',
]

# 신뢰 언론사
TRUSTED_SOURCES = [
    '연합뉴스', '뉴시스', 'KBS', 'MBC', 'SBS', 'YTN',
    '조선일보', '중앙일보', '동아일보', '한겨레', '경향신문',
    '매일신문', '영남일보', '부산일보', '광주일보',
    '한국일보', '국민일보', '세계일보', 'JTBC', 'MBN',
]

def clean_html(text: str) -> str:
    text = html.unescape(text)
    text = re.sub(r'<[^>]+>', '', text)
    return text.strip()

def parse_date(date_str: str):
    try:
        return datetime.strptime(date_str, '%a, %d %b %Y %H:%M:%S +0900')
    except Exception:
        return datetime.now()

def is_valid_article(title: str, description: str, source: str) -> bool:
    full_text = title + ' ' + description

    # 1. 제외 단어 필터링
    if any(word in full_text for word in EXCLUDE_WORDS):
        return False

    # 2. 필수 단어 포함 여부
    if not any(word in full_text for word in REQUIRED_WORDS):
        return False

    # 3. 신뢰 언론사 필터링 (없으면 통과 — 소규모 지역 언론 포함)
    # 신뢰 언론사면 가점, 아니어도 통과
    return True

def collect_news():
    print("[뉴스] 전국 수집 시작")
    conn = get_connection()
    total = 0

    for keyword in NEWS_KEYWORDS:
        try:
            headers = {
                "X-Naver-Client-Id":     NAVER_CLIENT_ID,
                "X-Naver-Client-Secret": NAVER_CLIENT_SECRET,
            }
            params = {
                "query":   keyword,
                "display": 20,
                "start":   1,
                "sort":    "date",
            }

            res = requests.get(NAVER_NEWS_API_URL, headers=headers, params=params, timeout=10)
            res.raise_for_status()
            data = res.json()

            items = data.get("items", [])
            count = 0

            for item in items:
                title       = clean_html(item.get("title", ""))
                description = clean_html(item.get("description", ""))
                url         = item.get("originallink") or item.get("link", "")
                source      = item.get("source", "")
                published_at = parse_date(item.get("pubDate", ""))

                if not title or not url:
                    continue

                # 품질 필터링
                if not is_valid_article(title, description, source):
                    continue

                try:
                    with conn.cursor() as cur:
                        cur.execute("""
                            INSERT INTO news_articles
                                (title, description, url, source, keyword, published_at)
                            VALUES (%s, %s, %s, %s, %s, %s)
                            ON CONFLICT (url) DO NOTHING
                        """, (title, description, url, source or '뉴스', keyword, published_at))
                    conn.commit()
                    count += 1
                except Exception as e:
                    conn.rollback()
                    continue

            total += count
            print(f"[뉴스] '{keyword}' → {count}건 저장")

        except Exception as e:
            print(f"[뉴스] '{keyword}' 수집 실패: {e}")

    conn.close()
    print(f"[뉴스] 전국 수집 완료 → 총 {total}건")