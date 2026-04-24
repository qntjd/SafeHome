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

if __name__ == "__main__":
    run_all()

    # 뉴스는 1시간마다 수집
    schedule.every().hour.do(collect_news)
    # 안전시설은 매일 새벽 3시
    schedule.every().day.at("03:00").do(run_all)

    print("스케줄러 실행 중")
    while True:
        schedule.run_pending()
        time.sleep(60)