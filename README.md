# 🏠 SafeHome — 1인 가구 안심 생활 플랫폼

> 공공데이터를 활용해 1인 가구의 안전한 일상을 지원하는 종합 안전 플랫폼

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green?style=flat-square&logo=springboot)
![React](https://img.shields.io/badge/React-19-blue?style=flat-square&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5.0-blue?style=flat-square&logo=typescript)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![Python](https://img.shields.io/badge/Python-3.10-yellow?style=flat-square&logo=python)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?style=flat-square&logo=docker)

---

## 📌 프로젝트 소개

SafeHome은 경찰청·행정안전부 등 공공데이터를 기반으로 1인 가구의 안전한 생활을 지원하는 플랫폼입니다.

- **주변 안전시설(CCTV·비상벨·경찰서)** 지도 시각화
- **동네 안전점수** 자동 산출 및 등급 제공
- **안심 귀가** — 목적지 설정 → 미도착 시 비상연락처 자동 알림 (워치독 패턴)
- **실시간 재난·범죄 알림** (SSE 스트림)
- **지역별 범죄통계** 차트·테이블 시각화
- **안전 뉴스** 자동 수집 (네이버 검색 API)
- **개인정보 보호 자원** 안내 (공공기관 연계)

---

## 🖥 시스템 아키텍처

```
┌─────────────────────────────────────────────────────┐
│                     Client                          │
│         React (Web)  │  Android App                 │
└──────────────────────┼──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│              Spring Boot API Server                 │
│  JWT 인증 │ 안전점수 엔진 │ 워치독 │ SSE │ OAuth2   │
└──────────────────────┼──────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
┌───────▼───────┐           ┌─────────▼───────┐
│  PostgreSQL   │           │      Redis       │
│  (주 데이터)  │           │  (캐시·세션)    │
└───────────────┘           └─────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│           Python 공공데이터 수집 배치               │
│  CCTV │ 비상벨 │ 경찰서 │ 범죄통계 │ 뉴스          │
└─────────────────────────────────────────────────────┘
```

---

## 🚀 주요 기능

### 🗺 안전 지도
- 반경 내 CCTV·비상벨·경찰서·파출소 마커 표시
- 시설 종류별 필터링 (토글 버튼)
- 행정동 단위 안전점수 사이드바

### 📊 동네 안전점수
- CCTV 밀도 (30%) + 범죄 역점수 (40%) + 비상벨 밀도 (10%) + 가로등 (20%) 가중 합산
- A~F 등급 자동 부여
- Spring Scheduler로 매일 새벽 4시 자동 재계산

### 🚶 안심 귀가 (워치독 패턴)
- 카카오맵 주소 검색·지도 클릭으로 목적지 설정
- 예상 소요 시간 슬라이더 설정
- 예상 도착 시각 초과 시 비상연락처에 자동 알림
- SOS 버튼 — 비상연락처 즉시 알림

### 🔔 실시간 알림 (SSE)
- Server-Sent Events 기반 실시간 재난문자 수신
- 구독 반경 내 사용자에게만 지오펜싱 알림
- 행정안전부 재난문자 API 3분 주기 폴링

### 📈 범죄통계 시각화
- 지역별 범죄 건수 스택 바 차트 (Recharts)
- 범죄 유형별 레이더 차트
- 강력범죄·폭행·절도·사기·풍속·기타 6개 분류

### 📰 안전 뉴스
- 네이버 검색 API로 대구 안전·재난·범죄 뉴스 1시간 주기 자동 수집
- 카테고리 필터·키워드 검색·페이지네이션

---

## 🛠 기술 스택

### Backend
| 기술 | 버전 | 용도 |
|---|---|---|
| Java | 17 | 주 언어 |
| Spring Boot | 4.0 | API 서버 |
| Spring Security | 6.x | JWT 인증·OAuth2 |
| Spring Data JPA | 3.x | ORM |
| Spring Scheduler | - | 워치독·점수 재계산 |
| PostgreSQL | 16 | 주 데이터베이스 |
| Redis | 7 | 캐시 |
| Springdoc OpenAPI | 3.x | Swagger 문서화 |

### Frontend
| 기술 | 버전 | 용도 |
|---|---|---|
| React | 19 | UI 프레임워크 |
| TypeScript | 5.0 | 타입 안전성 |
| Vite | 6.x | 빌드 도구 |
| TanStack Query | 5.x | 서버 상태 관리 |
| Zustand | 5.x | 클라이언트 상태 관리 |
| React Kakao Maps SDK | - | 지도 |
| Recharts | - | 차트 시각화 |
| Tailwind CSS | 4.x | 스타일링 |
| React Hook Form | 7.x | 폼 관리 |

### 데이터 수집 (Python)
| 기술 | 용도 |
|---|---|
| Python 3.10 | 배치 수집 언어 |
| requests | HTTP 클라이언트 |
| psycopg2 | PostgreSQL 연결 |
| schedule | 스케줄링 |

### 인프라
| 기술 | 용도 |
|---|---|
| Docker Compose | 로컬 개발 환경 |
| Kakao REST API | 주소→좌표 변환 |

---

## 📂 프로젝트 구조

```
safehome/
├── safehome-api/                # Spring Boot 백엔드
│   └── src/main/java/com/safehome/safehome_api/
│       ├── domain/
│       │   ├── user/            # 회원·인증·비상연락처
│       │   ├── safety/          # 안전시설·안전점수·범죄통계
│       │   ├── trip/            # 안심귀가·워치독
│       │   ├── alert/           # 알림구독·SSE
│       │   └── news/            # 뉴스
│       ├── global/              # JWT·예외처리·공통응답
│       ├── config/              # Security·OAuth2
│       └── batch/               # 안전점수 자동 재계산
│
├── safehome-web/                # React 프론트엔드
│   └── src/
│       ├── api/                 # Axios API 클라이언트
│       ├── components/          # 공통 컴포넌트
│       ├── hooks/               # 커스텀 훅
│       ├── pages/               # 페이지 컴포넌트
│       ├── store/               # Zustand 전역 상태
│       └── types/               # TypeScript 타입
│
└── safehome-batch/              # Python 공공데이터 수집
    └── collectors/
        ├── cctv_collector.py
        ├── emergency_bell_collector.py
        ├── police_collector.py
        ├── crime_stat_collector.py
        └── news_collector.py
```

---

## 🗃 활용 공공데이터

| 데이터 | 제공기관 | 활용 |
|---|---|---|
| CCTV 정보 조회 | 행정안전부 | 안전 지도 마커 |
| 안전비상벨 위치정보 | 행정안전부 | 안전 지도 마커 |
| 전국 지구대·파출소 주소 | 경찰청 | 안전 지도 마커 |
| 범죄 발생 지역별 통계 | 경찰청 | 범죄통계 차트 |
| 범죄통계정보 | 한국형사법무정책연구원 | 안전점수 계산 |
| 재난문자방송 발송이력 | 행정안전부 | 실시간 알림 |
| 뉴스 검색 | 네이버 | 안전 뉴스 |

---

## ⚡ 실행 방법

### 사전 준비
- Docker Desktop
- JDK 17
- Node.js 20+
- Python 3.10+

### 1. 환경변수 설정

`safehome-api/src/main/resources/application.yml`

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET
```

`safehome-batch/.env`

```env
PUBLIC_API_KEY=공공데이터포털_인증키
KAKAO_REST_API_KEY=카카오_REST_API_키
NAVER_CLIENT_ID=네이버_클라이언트_ID
NAVER_CLIENT_SECRET=네이버_클라이언트_시크릿
DB_HOST=localhost
DB_PORT=5432
DB_NAME=safehome
DB_USER=safehome
DB_PASSWORD=safehome1234
```

`safehome-web/index.html`

```html
<script src="//dapi.kakao.com/v2/maps/sdk.js?appkey=카카오_JavaScript_키&libraries=services"></script>
```

### 2. Docker 실행 (DB + Redis)

```bash
docker-compose up -d
```

### 3. 공공데이터 수집

```bash
cd safehome-batch
pip install -r requirements.txt
python main.py
```

### 4. 백엔드 실행

```bash
cd safehome-api
gradlew bootRun
```

### 5. 프론트엔드 실행

```bash
cd safehome-web
npm install
npm run dev
```

### 6. 접속

| 서비스 | URL |
|---|---|
| 웹 앱 | http://localhost:5173 |
| Swagger | http://localhost:8080/swagger-ui.html |

---

## 📡 API 문서

백엔드 실행 후 Swagger UI에서 전체 API 명세를 확인할 수 있어요.

```
http://localhost:8080/swagger-ui.html
```

| 도메인 | 엔드포인트 | 설명 |
|---|---|---|
| Auth | `/api/auth/**` | 회원가입·로그인·토큰 갱신 |
| Safety | `/api/safety/**` | 안전시설·안전점수·히트맵 |
| Trip | `/api/trips/**` | 안심귀가·SOS |
| Alert | `/api/alerts/**` | 알림구독·SSE |
| Crime | `/api/crime/**` | 범죄통계 |
| News | `/api/news/**` | 안전뉴스 |
| Contact | `/api/contacts/**` | 비상연락처 |

---

## 📊 ERD

```
USERS ──────────────────┬── SAFE_TRIPS
  │                     └── EMERGENCY_CONTACTS
  └── ALERT_SUBSCRIPTIONS

SAFETY_FACILITIES ──────── DISTRICT_SCORES
CRIME_STATS ────────────── DISTRICT_SCORES
NEWS_ARTICLES
DISASTER_ALERTS
```

---

## 👤 개발자

| 항목 | 내용 |
|---|---|
| 개발 기간 | 2026년 |
| 개발 인원 | 1인 |
| 개발 환경 | Windows 11 / VSCode / IntelliJ |

---

## 📄 라이선스

본 프로젝트는 포트폴리오 목적으로 제작되었습니다.
공공데이터는 공공데이터포털 이용약관에 따라 활용되었습니다.

---

> 본 시스템의 안전점수 및 범죄통계는 경찰청·행정안전부 공공데이터를 기반으로 산출되며, 실제 치안 상황과 다를 수 있습니다.
