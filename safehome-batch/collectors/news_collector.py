import requests
import html
from datetime import datetime
from config import (
    NAVER_CLIENT_ID,
    NAVER_CLIENT_SECRET,
    NAVER_NEWS_API_URL,
    NEWS_KEYWORDS,
)
from db import get_connection

def clean_html(text: str) -> str:
    import re
    text = html.unescape(text)
    text = re.sub(r'<[^>]+>', '', text)
    return text.strip()

def parse_date(date_str: str):
    try:
        return datetime.strptime(date_str, '%a, %d %b %Y %H:%M:%S +0900')
    except Exception:
        return datetime.now()

def collect_news():
    print("[뉴스] 수집 시작")
    conn = get_connection()
    total = 0

    for keyword in NEWS_KEYWORDS:
        try:
            headers = {
                "X-Naver-Client-Id":     NAVER_CLIENT_ID,
                "X-Naver-Client-Secret": NAVER_CLIENT_SECRET,
            }
            params = {
                "query":  keyword,
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
                published_at = parse_date(item.get("pubDate", ""))

                if not title or not url:
                    continue

                try:
                    with conn.cursor() as cur:
                        cur.execute("""
                            INSERT INTO news_articles
                                (title, description, url, source, keyword, published_at)
                            VALUES (%s, %s, %s, %s, %s, %s)
                            ON CONFLICT (url) DO NOTHING
                        """, (title, description, url, '네이버뉴스', keyword, published_at))
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
    print(f"[뉴스] 수집 완료 → 총 {total}건")