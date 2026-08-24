# 🏠 SafeHome — 1인 가구 안심 생활 플랫폼

> 공공데이터를 활용해 1인 가구의 안전한 일상을 지원하는 종합 안전 플랫폼

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green?style=flat-square&logo=springboot)
![React](https://img.shields.io/badge/React-19-blue?style=flat-square&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5.0-blue?style=flat-square&logo=typescript)
![Kotlin](https://img.shields.io/badge/Kotlin-Android-purple?style=flat-square&logo=kotlin)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-red?style=flat-square&logo=redis)
![Python](https://img.shields.io/badge/Python-3.10-yellow?style=flat-square&logo=python)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?style=flat-square&logo=docker)
![AWS](https://img.shields.io/badge/AWS-EC2-orange?style=flat-square&logo=amazonaws)

🔗 **배포 링크**: [safehome-api.duckdns.org](https://safehome-api.duckdns.org)

---

## 📌 프로젝트 소개

SafeHome은 경찰청·행정안전부 등 공공데이터를 기반으로 1인 가구의 안전한 생활을 지원하는 플랫폼입니다.
혼자 사는 사람들이 귀갓길부터 집 안까지 안심할 수 있도록 실시간 안전정보, 안심 귀가, 긴급 SOS 기능을 웹과 안드로이드 앱으로 제공하며, AWS 클라우드 환경에 실제로 배포·운영하고 있습니다.

---

## 🖥 시스템 아키텍처

```
┌───────────────────────────────────────────────────────┐
│                       Client                           │
│      React Web       │       Android App (Kotlin)      │
└───────────┬───────────────────────┬────────────────────┘
            │                       │
            └──────────┬────────────┘
                        │ HTTPS
┌───────────────────────▼─────────────────────────────────┐
│           Nginx (리버스 프록시 · Let's Encrypt HTTPS)    │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│         Spring Boot API Server (systemd 상시 실행)       │
│   JWT·OAuth2 │ 안전점수 엔진 │ 워치독 │ SSE │ 반경검색   │
└───────────────────────┬─────────────────────────────────┘
                        │
        ┌────────────────┴────────────────┐
        │                                 │
┌───────▼───────┐                ┌────────▼────────┐
│  PostgreSQL   │                │      Redis       │
│  (주 데이터)  │                │  (캐시·세션)    │
└───────────────┘                └─────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│      Python 공공데이터 수집 배치 (Docker, 전국 단위)      │
│    CCTV │ 비상벨 │ 경찰서 │ 범죄통계(5개년) │ 뉴스        │
└───────────────────────────────────────────────────────────┘

배포: AWS EC2 단일 인스턴스, Docker Compose(PostgreSQL·Redis·배치),
     DuckDNS 도메인 + Let's Encrypt HTTPS, Spring Boot systemd 서비스
```

---

## 🚀 주요 기능

### 🗺 안전 지도
- 전국 CCTV(377,000+건) · 비상벨(88,000+건) · 경찰서·파출소(2,000+건) 마커 표시, 대구 단일 지역 → **전국 17개 시/도**로 커버리지 확장
- 시설 종류별 필터링(토글 버튼), 현재 위치 기반 반경 검색
- 반경 검색 쿼리 **인덱스 + Bounding Box** 최적화 (5,101ms → 8.8ms, 약 580배 단축)
- 밀집 지역 마커 과다 렌더링 방지를 위한 **거리순 정렬 + 상위 300건 제한**, 통계 카운트는 별도 API로 분리해 정확도 유지
- 마커 아이콘 **비트맵 캐싱**으로 렌더링 부하 개선 (O(N) → O(1))

### 📊 동네 안전점수
- CCTV 밀도(30%) + 범죄 역점수(40%) + 비상벨 밀도(10%) + 가로등(20%) 가중 합산, A~F 등급 자동 부여
- Spring Scheduler로 매일 새벽 4시 자동 재계산, Redis 캐싱으로 응답 성능 최적화

### 🚶 안심 귀가
- 즐겨찾기 장소 저장, 안전 경로 분석(CCTV·비상벨·경찰서 밀도 기반 안전점수)
- 귀가 중 주변 위험 알림, 위치 공유 링크(비로그인 확인 가능)
- 야간 모드(오후 10시~오전 6시 자동 전환), 워치독 패턴(예상 도착 시각 초과 시 자동 알림)

### 🚨 긴급 SOS (웹 + 안드로이드)
**웹**
- 수동 SOS, 음성 자동 감지(Web Speech API), 5초 카운트다운 후 비상연락처 알림, 112 연결

**안드로이드 (신규)**
- **백그라운드 음성 SOS** — Foreground Service로 화면이 꺼진 상태에서도 상시 음성 감지, 잠금화면 위 SOS 화면 자동 표시
- **잠금화면 SOS + 카운트다운** — 카운트다운 중 취소 가능(오작동 방지), 종료 시 비상연락처 SMS 자동 발송
- **SOS 자동 녹음·녹화** — 음성 녹음 및 Camera2 기반 백그라운드 영상 녹화(화면 프리뷰 없이, 최대 2분, 별도 Foreground Service로 액티비티 종료와 무관하게 지속)
- **112 자동신고 (고급 기능)** — 설정에서 토글, 카운트다운 종료 시 `ACTION_CALL`로 자동 발신(권한 미허용 시 다이얼 화면 대체)
- **SAFE HOME 사용 가이드** — 앱 핵심 기능 안내 + SOS 설정 강조
- **생활 안전 정보** — 휴대폰 자체 긴급 SOS(갤럭시·아이폰) 설정 안내 + 명의도용방지·개인정보 포털 등 공공 안전 서비스 연계

### 🔔 실시간 알림
- SSE 기반 실시간 재난문자 수신, 시도/시군구 단위 알림 구독
- **내 지역**(GPS 자동 감지·구독) / **관심 지역**(부모님댁 등 수동 등록) 탭 분리
- 행정안전부 재난문자 API 주기적 수집

### 📈 범죄통계 시각화
- **2020~2024년 5개년** 전국 17개 시/도 범죄 통계 (강력·폭행·절도·사기지능·풍속마약·**교통**·기타 7개 분류로 세분화)
- 지역별·연도별 조회 API 및 연도별 추이(trend) API 제공
- 웹: 스택 바 차트·레이더 차트 / 안드로이드: 연도 탭 전환 막대그래프

### 📰 안전 뉴스
- 전국 단위 안전·재난·범죄 키워드 뉴스 수집, 신뢰 언론사 필터링

### 🛡 안전 자원
- 명의도용방지, 금융거래 조회, 개인정보 포털 등 공공 안전 서비스 연계

### 👤 계정 및 설정
- 이메일/비밀번호 로그인 + 구글 OAuth2 소셜 로그인(웹·안드로이드 공통)
- 비상연락처 CRUD, 재난·범죄 알림 구독, 닉네임 변경

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

### Frontend (Web)
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
| Web Speech API | - | 음성 감지 (SOS) |

### Android
| 기술 | 용도 |
|---|---|
| Kotlin | 주 언어 |
| Retrofit / OkHttp | API 통신 |
| Kakao Map SDK | 안전지도 |
| Camera2 API / MediaRecorder | SOS 영상·음성 녹화 |
| SpeechRecognizer | 음성 SOS 감지 |
| Foreground Service | 백그라운드 음성 감지·영상 녹화 |
| FusedLocationProviderClient | 위치 기반 내 지역 자동 등록 |

### 데이터 수집 (Python)
| 기술 | 용도 |
|---|---|
| Python 3.10 | 배치 수집 언어 |
| requests | HTTP 클라이언트 |
| psycopg2 | PostgreSQL 연결 |
| schedule | 스케줄링 |

### 인프라 · 배포
| 기술 | 용도 |
|---|---|
| AWS EC2 | 서버 호스팅 |
| Docker Compose | PostgreSQL·Redis·배치 컨테이너 운영 |
| systemd | Spring Boot 상시 실행 |
| Nginx | 리버스 프록시 |
| Let's Encrypt | HTTPS 인증서 |
| DuckDNS | 도메인 연결 |
| Kakao Maps / REST API | 지도·주소 검색·역지오코딩 |
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
│       ├── config/               # Security·OAuth2·Redis
│       └── batch/                # 안전점수 자동 재계산
│
├── safehome-web/                # React 프론트엔드
│   └── src/
│       ├── api/                 # Axios API 클라이언트
│       ├── components/          # 공통 컴포넌트 (SOS, Footer 등)
│       ├── hooks/                # 커스텀 훅 (음성감지, 위치 등)
│       ├── pages/                # 페이지 컴포넌트
│       ├── store/                # Zustand 전역 상태
│       └── types/                # TypeScript 타입
│
├── safehome-batch/               # Python 공공데이터 수집 (Docker)
│   └── collectors/
│       ├── cctv_collector.py
│       ├── emergency_bell_collector.py
│       ├── police_collector.py
│       ├── crime_stat_collector.py     # 2020~2024년 다중 연도 수집
│       └── news_collector.py
│
└── SafeHome (Android)/           # Kotlin 안드로이드 앱
    └── app/src/main/java/com/safehome/app/
        ├── ui/                   # home·map·alert·crime·guide·trip·settings
        ├── service/               # 음성감지·영상녹화·위치추적 Foreground Service
        ├── api/                   # Retrofit API 인터페이스
        └── util/                  # SMS·위치·아이콘 캐싱 등 헬퍼
```

---

## 🗃 활용 공공데이터

| 데이터 | 제공기관 | 커버리지 | 활용 |
|---|---|---|---|
| CCTV 정보 조회 | 행정안전부 | 전국 (377,000+건) | 안전 지도 마커 |
| 안전비상벨 위치정보 | 행정안전부 | 전국 (88,000+건) | 안전 지도 마커 |
| 전국 지구대·파출소 주소 | 경찰청 | 전국 (2,000+건) | 안전 지도 마커 |
| 범죄 발생 지역별 통계 | 경찰청 | 전국 17개 시/도, 2020~2024년 | 범죄통계 차트 |
| 재난문자방송 발송이력 | 행정안전부 | 실시간 | 실시간 알림 |
| 뉴스 검색 | 네이버 | 실시간 | 안전 뉴스 |

---

## 🔥 성능 개선 하이라이트

| 항목 | 개선 전 | 개선 후 |
|---|---|---|
| 안전시설 반경 검색 쿼리 | 5,101ms | 8.8ms (약 580배 단축) |
| 안전시설 조회 API 응답시간 | 7,000~12,000ms | 180~530ms |
| CCTV 수집률 | 37,800 / 377,278건 (10%) | 377,278건 (100%) |
| 비상벨 수집률 | 8,900 / 88,634건 (10%) | 88,634건 (100%) |
| 경찰서·범죄통계 커버리지 | 대구 1개 지역 | 전국 17개 시/도 |
| 마커 아이콘 렌더링 | 마커 개수(N)만큼 비트맵 생성 | 타입별 1회 캐싱, O(N) → O(1) |

> 반경 검색 성능 문제는 배치 재실행 시 `id`가 매번 새로 생성되어 `ON CONFLICT`가 무력화되며 동일 시설 데이터가 중복 저장되던 문제(917만 건, 정상 대비 약 200배)에서 비롯되었습니다. `(type, lat, lng)` UNIQUE 제약으로 근본 원인을 제거하고, 위경도 Bounding Box + 인덱스로 쿼리 구조를 개선해 해결했습니다.

---

## 📱 페이지 구성 (Web)

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
| 개발 기간 | 2026년 3월 ~ 진행 중 |
| 개발 인원 | 1인 (기획·백엔드·프론트엔드·안드로이드·배포 전담) |
| 개발 환경 | Windows 11 / VSCode / IntelliJ / Android Studio |
| 배포 환경 | AWS EC2, Docker Compose, systemd, Nginx |
| 대상 사용자 | 1인 가구 (특히 여성·청년) |

---

## 🗺 향후 로드맵

- [ ] 전국 시군구 단위 안전점수·범죄통계 세분화 (2026년 행정구역 개편 반영 필요)
- [ ] 사용자 위험 제보 게시판
- [ ] FCM 푸시 알림 실제 연동
- [ ] Google Play Store 정식 등록
- [ ] Android 전국 확장 데이터 연동 마무리

---

## 📄 라이선스

공공데이터는 공공데이터포털 이용약관에 따라 활용되었습니다.

---

> 본 시스템의 안전점수 및 범죄통계는 경찰청·행정안전부 공공데이터를 기반으로 산출되며, 실제 치안 상황과 다를 수 있습니다.
