# 🏠 SafeHome — 1인 가구 안심 생활 플랫폼

> 공공데이터를 활용해 1인 가구의 안전한 일상을 지원하는 종합 안전 플랫폼

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green?style=flat-square&logo=springboot)
![React](https://img.shields.io/badge/React-19-blue?style=flat-square&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5.0-blue?style=flat-square&logo=typescript)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-red?style=flat-square&logo=redis)
![Python](https://img.shields.io/badge/Python-3.10-yellow?style=flat-square&logo=python)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?style=flat-square&logo=docker)

---

## 📌 프로젝트 소개

SafeHome은 경찰청·행정안전부 등 공공데이터를 기반으로 1인 가구의 안전한 생활을 지원하는 플랫폼입니다.

혼자 사는 사람들이 귀갓길부터 집 안까지 안심할 수 있도록 실시간 안전정보, 안심 귀가, 긴급 SOS 기능을 제공합니다.

---

## 🖥 시스템 아키텍처

```
┌─────────────────────────────────────────────────────┐
│                     Client                          │
│         React Web  │  Android App (예정)            │
└──────────────────────┼──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│              Spring Boot API Server                 │
│  JWT·OAuth2 │ 안전점수 엔진 │ 워치독 │ SSE          │
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
- 전국 CCTV(35,000+건) · 비상벨(8,700+건) · 경찰서·파출소(60건) 마커 표시
- 시설 종류별 필터링 (토글 버튼)
- 현재 위치 기반 내 동네 안전점수 자동 표시
- 안전점수 · 범죄통계 탭 전환

### 📊 동네 안전점수
- CCTV 밀도 (30%) + 범죄 역점수 (40%) + 비상벨 밀도 (10%) + 가로등 (20%) 가중 합산
- A~F 등급 자동 부여
- 대구 구/군 단위 정밀 분석, 전국 시도 단위 확장
- Spring Scheduler 매일 새벽 4시 자동 재계산
- Redis 캐싱으로 응답 성능 최적화

### 🚶 안심 귀가 (전면 개편)
- **즐겨찾기 장소** — 집/직장/학교 등 자주 가는 곳 저장, 원터치 귀가 시작
- **안전 경로 분석** — 출발지~목적지 경로 CCTV/비상벨/경찰서 밀도 분석 및 안전점수 제공
- **주변 위험 알림** — 귀가 중 반경 500m 안전시설 실시간 감지, 안전/주의/위험 구역 표시
- **위치 공유 링크** — 실시간 위치 추적 링크 생성, 비상연락처가 브라우저에서 확인 가능
- **야간 모드** — 오후 10시~오전 6시 자동 전환, 보라색 UI + 음성 감지 자동 활성화
- **워치독 패턴** — 예상 도착 시각 초과 시 비상연락처 자동 알림

### 🚨 긴급 SOS + 음성 감지
- **수동 SOS** — 버튼 클릭으로 즉시 활성화
- **음성 자동 감지** — "살려줘", "도와줘" 등 키워드 감지 시 자동 SOS 발동 (Web Speech API)
- 5초 카운트다운 후 비상연락처 자동 알림
- 112 신고 전화 앱 연결
- 현재 위치 카카오맵 링크 복사

### 🔔 실시간 알림 (SSE)
- Server-Sent Events 기반 실시간 재난문자 수신
- 시도/시군구 단위 알림 구독 (내 지역 / 관심 지역 분리)
- 별명 설정 (집, 직장, 부모님댁 등)
- 행정안전부 재난문자 API 3분 주기 폴링

### 📈 범죄통계 시각화
- 지역별 범죄 건수 스택 바 차트 (Recharts)
- 범죄 유형별 레이더 차트
- 강력범죄·폭행·절도·사기·풍속·기타 6개 분류
- 지역별 상세 테이블

### 📰 안전 뉴스
- 전국 단위 안전·재난·범죄 키워드 뉴스 수집
- 신뢰 언론사 필터링, 관련성 없는 기사 제외
- 카테고리 필터·키워드 검색·페이지네이션

### 🛡 안전 자원
- 명의도용방지, 금융거래 조회, 개인정보 포털 등 공공 안전 서비스 연계
- 여성·1인 가구 특화 서비스 안내

### 👤 계정 및 설정
- 이메일/비밀번호 로그인 + 구글 OAuth2 소셜 로그인
- 비상연락처 CRUD (최대 5명, 미도착 알림 시간 설정)
- 재난·범죄 알림 구독 (시도/시군구 기반)
- 닉네임 변경, 프로필 관리

---

## 🛠 기술 스택

### Backend
| 기술 | 버전 | 용도 |
|---|---|---|
| Java | 17 | 주 언어 |
| Spring Boot | 4.0 | API 서버 |
| Spring Security | 6.x | JWT 인증·OAuth2 |
| Spring Data JPA | 3.x | ORM |
| Spring Scheduler | - | 워치독·점수 재계산·배치 |
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
| Web Speech API | - | 음성 감지 (SOS) |

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
| Kakao Maps API | 지도·주소 검색·역지오코딩 |
| Kakao REST API | 주소→좌표 변환 |
| Naver Search API | 뉴스 수집 |
| Google OAuth2 | 소셜 로그인 |

---

## 📂 프로젝트 구조

```
safehome/
├── safehome-api/                # Spring Boot 백엔드
│   └── src/main/java/com/safehome/safehome_api/
│       ├── domain/
│       │   ├── user/            # 회원·인증·비상연락처
│       │   ├── safety/          # 안전시설·안전점수·범죄통계
│       │   ├── trip/            # 안심귀가·워치독·즐겨찾기
│       │   ├── alert/           # 알림구독·SSE
│       │   └── news/            # 뉴스
│       ├── global/              # JWT·예외처리·공통응답
│       ├── config/              # Security·OAuth2·Redis
│       └── batch/               # 안전점수 자동 재계산
│
├── safehome-web/                # React 프론트엔드
│   └── src/
│       ├── api/                 # Axios API 클라이언트
│       ├── components/          # 공통 컴포넌트 (SOS, Footer 등)
│       ├── hooks/               # 커스텀 훅 (음성감지, 위치 등)
│       ├── pages/               # 페이지 컴포넌트
│       ├── store/               # Zustand 전역 상태
│       ├── utils/               # 공통 유틸 (지도 마커 등)
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

| 데이터 | 제공기관 | 건수 | 활용 |
|---|---|---|---|
| CCTV 정보 조회 | 행정안전부 | 35,000+ | 안전 지도 마커 |
| 안전비상벨 위치정보 | 행정안전부 | 8,700+ | 안전 지도 마커 |
| 전국 지구대·파출소 주소 | 경찰청 | 60+ | 안전 지도 마커 |
| 범죄 발생 지역별 통계 | 경찰청 | 전국 | 범죄통계 차트 |
| 범죄통계정보 | 한국형사법무정책연구원 | 전국 | 안전점수 계산 |
| 재난문자방송 발송이력 | 행정안전부 | 실시간 | 실시간 알림 |
| 뉴스 검색 | 네이버 | 실시간 | 안전 뉴스 |


## 📱 페이지 구성

| 페이지 | 경로 | 설명 |
|---|---|---|
| 홈 (대시보드) | `/` | 안전 현황 요약·빠른 메뉴 |
| 안전 지도 | `/map` | CCTV·비상벨·경찰서 지도·범죄통계 |
| 안심 귀가 | `/trip` | 즐겨찾기·안전경로·위험알림·위치공유 |
| 안전 뉴스 | `/news` | 전국 안전 뉴스 |
| 범죄 통계 | `/crime` | 범죄 차트·테이블 |
| 안전 자원 | `/resources` | 공공 안전 서비스 연계 |
| 설정 | `/settings` | 비상연락처·알림 구독 |
| 위치 공유 | `/share/:token` | 비로그인 실시간 위치 확인 |

---

## 👤 개발 정보

| 항목 | 내용 |
|---|---|
| 개발 기간 | 2026년 |
| 개발 인원 | 1인 |
| 개발 환경 | Windows 11 / VSCode / IntelliJ |
| 대상 사용자 | 1인 가구 (특히 여성·청년) |

---

## 🗺 향후 로드맵

- [ ] Android 앱 — 백그라운드 음성 감지·잠금화면 SOS
- [ ] 전국 시군구 단위 안전점수 확장
- [ ] 사용자 위험 제보 게시판
- [ ] FCM 푸시 알림 실제 연동
- [ ] AWS 배포 — EC2 + RDS + ElastiCache

---

## 📄 라이선스

공공데이터는 공공데이터포털 이용약관에 따라 활용되었습니다.

---

> 본 시스템의 안전점수 및 범죄통계는 경찰청·행정안전부 공공데이터를 기반으로 산출되며, 실제 치안 상황과 다를 수 있습니다.
