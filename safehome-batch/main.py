import schedule
import time
from collectors.cctv_collector import collect_cctv
from collectors.emergency_bell_collector import collect_emergency_bells
from collectors.crime_stat_collector import collect_crime_stats
from collectors.news_collector import collect_news
from collectors.police_collector import collect_police


def run_all():
    print("=" * 40)
    print("공공데이터 수집 시작")
    print("=" * 40)
    collect_cctv()
    collect_emergency_bells()
    collect_crime_stats()
    collect_news()
    collect_police()
    print("=" * 40)
    print("모든 수집 완료")
    print("=" * 40)


def initial_run_if_needed():
    """DB에 데이터가 없을 때만 최초 1회 실행"""
    from db import get_connection
    conn = get_connection()
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) FROM safety_facilities")
        count = cur.fetchone()[0]
    conn.close()

    if count == 0:
        print("[초기화] 기존 데이터 없음 → 최초 1회 전체 수집 실행")
        run_all()
    else:
        print(f"[초기화] 기존 데이터 {count}건 존재 → 초기 수집 생략")


if __name__ == "__main__":
    time.sleep(30)  # API 서버가 먼저 DB 커넥션을 안정적으로 확보하도록 잠시 대기
    initial_run_if_needed()

    schedule.every().hour.do(collect_news)
    schedule.every().day.at("03:00").do(run_all)

    print("스케줄러 실행 중")
    while True:
        schedule.run_pending()
        time.sleep(60)