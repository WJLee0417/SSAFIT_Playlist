# SSAFIT Playlist WBS

## 1. 작성 기준

- 기준 요구사항: `docs/project-artifacts/01-requirements/requirements-definition.md`
- 추적성: `docs/project-artifacts/01-requirements/requirements-traceability.csv`
- Use Case: `docs/project-artifacts/02-usecase/use-case-specification.md`
- DB 설계: `docs/project-artifacts/03-erd/erd-data-dictionary.md`
- 구현 근거: `docs/project-artifacts/00-evidence/implementation-evidence.csv`
- Git 근거: `develop` HEAD `dff5044`, 주요 병합 브랜치 `feature/a-db-service`, `feature/a-running-features-service`, `feature/a-spotify-service`, `feature/b-ui-api`, `feature/b-running-features-ui`, `feature/cadence-intensity-spotify-live`, `feature/b-ai-feature-update`

## 2. 담당자 기준

저장소 문서상 `docs/team-b-handover.md`와 README에는 팀원 A/B 경계가 명시되어 있다.

- 팀원 A: Domain, Mapper XML, SQL, Service 비즈니스 로직, Session 기반 인증/권한 검증, Spotify Token/state/export 상태 전이 로직
- 팀원 B: Controller, Request/Response DTO, JSP/Vue 화면, Vue Router, Frontend API Module, 브라우저 UI/API 검증, 발표 시연 흐름
- 공동 또는 확인 필요: 문서화, 배포, 외부 API 실검증, 작성자/담당 경계가 문서만으로 확정되지 않는 작업

## 3. 상태 기준

| 상태 | 의미 |
|---|---|
| DONE | 코드, 문서, Git 또는 테스트 근거로 완료가 확인된 작업 |
| IN_PROGRESS | 일부 산출물 또는 구현은 있으나 후속 문서/검증이 진행 중인 작업 |
| NOT_STARTED | 요구사항상 필요하지만 현재 구현 또는 산출물이 확인되지 않은 작업 |
| BLOCKED | 외부 계정, 환경, 정책, 런타임 준비 없이는 완료 확인이 어려운 작업 |
| UNVERIFIED | 코드 또는 산출물은 있으나 실제 동작/외부 연동/수동 검증이 확인되지 않은 작업 |

## 4. WBS

| WBS ID | 상위 ID | 작업명 | 상세 작업 | 관련 요구사항 | 담당 | 상태 | 난이도 | 선행 작업 | 근거 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | - | 프로젝트 관리 및 공통 기반 | 공통 기술 기반과 프로젝트 기준 관리 | FR-022, FR-023 | 공동 또는 확인 필요 | DONE | L | - | `SsafitApplication`, `pom.xml`, Git `2730e86`, `dff5044` |
| 1.1 | 1 | Spring Boot/MyBatis 기반 구성 | Java 25, Spring Boot 4, WAR, MyBatis Mapper scan, MySQL 연결 구조 구성 | FR-022 | 팀원 A | DONE | M | - | EV-002, EV-003, Git `2730e86`, `3875943` |
| 1.2 | 1 | 공통 예외/응답/검증 기반 | `ApiResponse`, `ErrorResponse`, `GlobalExceptionHandler`, Validation 적용 | FR-023 | 공동 또는 확인 필요 | DONE | M | 1.1 | EV-005, EV-006, UC-023 |
| 1.3 | 1 | DB 스키마 및 Mapper 기반 | MVP schema, migration, MyBatis XML 구성 | FR-001~FR-021 | 팀원 A | DONE | L | 1.1 | EV-037, EV-038, EV-039, Git `e581a90`, `656e6e7` |
| 1.4 | 1 | 요구사항/근거 산출물 관리 | Evidence, 요구사항, Use Case, ERD 산출물 작성 | 전체 | 공동 또는 확인 필요 | DONE | M | 1.1 | `docs/project-artifacts/00-evidence`, `01-requirements`, `02-usecase`, `03-erd` |
| 1.5 | 1 | Swagger/OpenAPI 도입 검토 | 자동 API 문서 도구 도입 여부 결정 | FR-023 | 공동 또는 확인 필요 | NOT_STARTED | S | 1.2 | EV-007: Swagger/OpenAPI 미구현 |
| 2 | - | 회원 및 인증 | 세션 기반 회원가입, 로그인, 로그아웃, 마이페이지, 러닝 프로필 | FR-001~FR-005 | 공동 또는 확인 필요 | DONE | L | 1 | 요구사항 FR-001~FR-005 |
| 2.1 | 2 | 회원/인증 Service | 회원가입, 로그인, 로그아웃, 내 정보 조회/수정 Service 구현 | FR-001~FR-004 | 팀원 A | DONE | M | 1.3 | EV-008~EV-011, Git `ba83166` |
| 2.2 | 2 | 회원/인증 API Controller/DTO | `/api/members`, `/api/auth/login`, `/api/auth/logout`, `/api/members/me` 연결 | FR-001~FR-004 | 팀원 B | DONE | M | 2.1 | EV-008, EV-009, Git `5d1f5e1` |
| 2.3 | 2 | JSP 회원 화면 | `/join`, `/login`, `/logout`, `/members/me` JSP 흐름 구현 | FR-001~FR-004 | 팀원 B | DONE | M | 2.1 | EV-010, Git `97cd065` |
| 2.4 | 2 | 러닝 프로필 Service/API/JSP | 러닝 프로필 저장/조회, 자동 목표값, JSP 설정 화면 | FR-005, FR-015 | 공동 또는 확인 필요 | DONE | M | 2.1, 1.3 | EV-019, EV-020, ERD `running_profiles` |
| 2.5 | 2 | Vue 전용 러닝 프로필 화면 후속 통합 | 발표 기준은 JSP 러닝 프로필 화면을 사용하고, Vue Router 기반 프로필 편집 화면은 후속 SPA 통합 과제로 분리 | FR-005 | 팀원 B | NOT_STARTED | M | 2.4, 9.1 | FR-005 PARTIAL, EV-021, `docs/project-artifacts/06-screen-design/screen-design-gap.md` |
| 2.6 | 2 | JSP/Vue 인증 복귀 흐름 검증 | `returnTo`, Cookie session, login-required/access-check-failed 화면 점검 | FR-002, FR-003, FR-023 | 팀원 B | UNVERIFIED | M | 2.2, 2.3, 9.1 | EV-041, `docs/integration/integration-defects.md` |
| 3 | - | 음악 관리 | 음악 조회, 상세, 내부 등록/수정, Spotify 곡 저장 | FR-008, FR-009, FR-017, FR-024 | 공동 또는 확인 필요 | DONE | L | 1, 2 | 요구사항 FR-008, FR-009, FR-017, FR-024 |
| 3.1 | 3 | 음악 DB/Service/Mapper | 음악 검색, 상세, 추천 후보, Spotify metadata 저장 | FR-008, FR-009, FR-017 | 팀원 A | DONE | M | 1.3 | EV-012, ERD `music`, Git `4f171bb` |
| 3.2 | 3 | 음악 API Controller/DTO | `/api/music`, `/api/music/{musicId}`, `/api/music/from-spotify`, `/api/music/internal` | FR-008, FR-009, FR-017, FR-024 | 팀원 B | DONE | M | 3.1 | EV-012, Git `5d1f5e1` |
| 3.3 | 3 | 음악 목록/상세/폼 Vue 화면 | 검색, 정렬, 상세, 내부 음악 폼 화면 연결 | FR-008, FR-009, FR-024 | 팀원 B | DONE | M | 3.2, 9.1 | EV-013, UC-008, UC-009 |
| 3.4 | 3 | 내부 음악 관리 권한 정책 | `/api/music/internal` 관리자/권한 모델 확정 | FR-024 | 공동 또는 확인 필요 | UNVERIFIED | M | 2.1, 3.2 | EV-045, FR-024 UNVERIFIED |
| 4 | - | 음악 리뷰 | 음악 리뷰 CRUD와 평균 점수 조회 | FR-010 | 공동 또는 확인 필요 | DONE | M | 2, 3 | 요구사항 FR-010 |
| 4.1 | 4 | 리뷰 Service/Mapper | 리뷰 생성, 수정, 삭제, 평균 점수 계산 | FR-010 | 팀원 A | DONE | M | 1.3, 3.1 | EV-014, ERD `music_reviews`, Git `844a980` |
| 4.2 | 4 | 리뷰 API Controller/DTO | `/api/music/{musicId}/reviews`, `/api/music-reviews/{reviewId}` 연결 | FR-010 | 팀원 B | DONE | M | 4.1 | EV-014 |
| 4.3 | 4 | 리뷰 Vue 컴포넌트 | 리뷰 작성/수정/삭제 UI, 음악 상세/카드 연동 | FR-010 | 팀원 B | DONE | M | 4.2, 9.1 | `MusicReviewForm.vue`, `reviewApi.js` |
| 4.4 | 4 | 리뷰 E2E/브라우저 검증 | 실제 브라우저에서 리뷰 CRUD 검증 | FR-010 | 팀원 B | UNVERIFIED | S | 4.3, 10.4 | Traceability: 프론트 전체 E2E 미확인 |
| 5 | - | 플레이리스트 | 내부 플레이리스트, 수록곡, 저장, 큐레이션 복사 | FR-011~FR-013, FR-016 | 공동 또는 확인 필요 | DONE | L | 2, 3 | 요구사항 FR-011~FR-013, FR-016 |
| 5.1 | 5 | 플레이리스트 Service/Mapper | CRUD, 공개/비공개, 소유자 검증 | FR-011 | 팀원 A | DONE | M | 1.3, 2.1 | EV-015, Git `7616bd8` |
| 5.2 | 5 | 수록곡 Service/Mapper | 수록곡 추가, 순서 변경, 삭제, 중복 검증 | FR-012 | 팀원 A | DONE | M | 5.1, 3.1 | EV-016, ERD `playlist_items` |
| 5.3 | 5 | 저장 플레이리스트 Service/Mapper | 공개/공식 플레이리스트 참조 저장/취소 | FR-013 | 팀원 A | DONE | M | 5.1 | EV-017, Git `6e3ca07` |
| 5.4 | 5 | 플레이리스트 API Controller/DTO | Playlist, item, save API 연결 | FR-011~FR-013 | 팀원 B | DONE | M | 5.1, 5.2, 5.3 | EV-015~EV-017 |
| 5.5 | 5 | 플레이리스트 Vue 화면 | 목록, 상세, 생성/수정, 저장 목록, 수록곡 관리 | FR-011~FR-013 | 팀원 B | DONE | L | 5.4, 9.1 | EV-018, Git `21c12ed` |
| 5.6 | 5 | 큐레이션 저장 결과 연결 | 추천 결과를 USER+CURATION_COPY 플레이리스트로 저장 | FR-016 | 공동 또는 확인 필요 | DONE | M | 5.1, 5.2, 7.2 | FR-016 IMPLEMENTED, Git `5ed8f6c`, `0ed2d8a` |
| 6 | - | 러닝 데이터 관리 | 러닝 기록, 러닝 세션, 참여 관리 | FR-006, FR-007 | 공동 또는 확인 필요 | DONE | L | 1, 2 | 요구사항 FR-006, FR-007 |
| 6.1 | 6 | 러닝 기록 DB/Service/Mapper | 러닝 기록 CRUD, soft delete, cadence/heart-rate 저장 | FR-006, FR-015 | 팀원 A | DONE | M | 1.3, 2.1 | EV-022, ERD `running_records`, Git `f548b5c` |
| 6.2 | 6 | 러닝 세션 DB/Service/Mapper | 세션 생성/수정/상태/취소/정원 검증 | FR-007 | 팀원 A | DONE | L | 1.3, 2.1 | EV-024, Git `b2b3fad`, `8b9b951` |
| 6.3 | 6 | 러닝 세션 참여 Service/Mapper | 참여, 참여 취소, 중복/정원 검증 | FR-007 | 팀원 A | DONE | M | 6.2 | EV-025, Git `d06ffdd` |
| 6.4 | 6 | 러닝 API Controller/DTO | 러닝 기록/세션/참여 API 연결 | FR-006, FR-007 | 팀원 B | DONE | M | 6.1, 6.2, 6.3 | EV-022~EV-025 |
| 6.5 | 6 | 러닝 Vue 화면 | 기록 목록/상세/폼, 세션 목록/상세/폼 화면 | FR-006, FR-007 | 팀원 B | DONE | L | 6.4, 9.1 | EV-023, EV-026, Git `e791a05`, `d6ed1ca` |
| 6.6 | 6 | 러닝 기능 실제 브라우저 회귀 | 기록/세션 CRUD와 권한 흐름 브라우저 재검증 | FR-006, FR-007 | 팀원 B | UNVERIFIED | M | 6.5, 10.4 | EV-044는 running E2E 존재, 이번 단계 재실행 없음 |
| 7 | - | 러닝 음악 추천 | 케이던스, 심박, 운동 강도 기반 추천과 저장 | FR-014~FR-016, FR-021 | 공동 또는 확인 필요 | DONE | L | 2, 3, 5, 6 | 요구사항 FR-014~FR-016, FR-021 |
| 7.1 | 7 | 추천 DB/Service/정책 | BPM, cadence, intensity, genre 기반 추천 엔진 | FR-014, FR-015 | 팀원 A | DONE | L | 1.3, 3.1, 6.1 | EV-027, EV-028, Git `b222e41`, `c105234` |
| 7.2 | 7 | 추천 API와 저장 API | 추천 조회, 추천 로그, 추천 결과 저장 API | FR-014, FR-016 | 팀원 B | DONE | M | 7.1, 5.1, 5.2 | EV-027, FR-016 |
| 7.3 | 7 | 추천 Vue 화면 | 추천 조건 입력, 결과 표시, 저장 버튼, 로그 복원 | FR-014~FR-016 | 팀원 B | DONE | L | 7.2, 9.1 | `RecommendationView.vue`, Git `5ed8f6c`, `0dee0bd`, `0ed2d8a` |
| 7.4 | 7 | AI 러닝 리포트 저장/조회 | AI 리포트 집계, 생성, 저장, 최신 조회 | FR-021 | 공동 또는 확인 필요 | DONE | L | 6.1, 7.1, 1.3 | EV-035, ERD `ai_running_reports`, Git `6da7bf3` |
| 7.5 | 7 | 실제 AI Provider 호출 검증 | OpenAI 호환 provider 환경 변수/API Key 기반 실 호출 | FR-021 | 공동 또는 확인 필요 | BLOCKED | M | 7.4, 11.3 | EV-036, FR-021 UNVERIFIED |
| 8 | - | Spotify 연동 | 검색, OAuth, 연결, 가져오기, 내보내기 | FR-017~FR-020 | 공동 또는 확인 필요 | UNVERIFIED | L | 2, 3, 5 | 요구사항 FR-017~FR-020 |
| 8.1 | 8 | Spotify Service/Client | 검색, OAuth state/token, 연결, playlist import/export Service | FR-017~FR-020 | 팀원 A | DONE | L | 1.3, 2.1, 3.1, 5.1 | EV-029~EV-033, Git `b1fb98b`, `ed2ff52` |
| 8.2 | 8 | Spotify API Controller/DTO | `/api/spotify/*`, `/api/playlists/{id}/export/spotify` 연결 | FR-017~FR-020 | 팀원 B | DONE | M | 8.1 | EV-029~EV-033 |
| 8.3 | 8 | Spotify Vue UI | 검색, 선택 저장, OAuth 완료, 연결 상태, import/export UI | FR-017~FR-020 | 팀원 B | DONE | L | 8.2, 9.1 | EV-034, Git `5d8a0f5`, `ed2ff52` |
| 8.4 | 8 | Spotify 실제 OAuth 검증 | Dashboard redirect URI, scope, 사용자 동의, callback 검증 | FR-018 | 공동 또는 확인 필요 | BLOCKED | M | 8.2, 8.3, 11.3 | FR-018 UNVERIFIED, integration defects 환경 의존 |
| 8.5 | 8 | Spotify 실제 Export/Import 검증 | 실제 계정의 playlist-read/modify scope로 import/export 성공 확인 | FR-019, FR-020 | 공동 또는 확인 필요 | BLOCKED | L | 8.4 | FR-019, FR-020 UNVERIFIED |
| 9 | - | 프론트엔드 화면 | Vue/JSP 혼합 UI와 화면 흐름 | FR-001~FR-020, FR-023 | 팀원 B | DONE | L | 2, 3, 5, 6, 7, 8 | EV-040, EV-041 |
| 9.1 | 9 | Vue/Vite/Router/API 기반 | Vue 프로젝트, 라우터, Axios API client, 오류 화면 구성 | FR-023 | 팀원 B | DONE | L | 1.2 | EV-040, Git `3e417e0`, `f9ce884` |
| 9.2 | 9 | JSP 회원 화면과 Vue 이동 | JSP 회원/프로필 화면과 Vue 진입 링크, returnTo 처리 | FR-001~FR-005 | 팀원 B | DONE | M | 2.3, 9.1 | EV-010, EV-041 |
| 9.3 | 9 | 주요 도메인 Vue 화면 | 음악, 리뷰, 플레이리스트, 러닝, 추천, Spotify 화면 통합 | FR-008~FR-020 | 팀원 B | DONE | L | 9.1 | EV-013, EV-018, EV-023, EV-026, EV-034 |
| 9.4 | 9 | 화면 설계서 작성 | 실제 JSP/Vue 화면 기준 화면 설계서 작성 | 전체 | 공동 또는 확인 필요 | DONE | L | 1.4, 9.3 | `docs/project-artifacts/06-screen-design` |
| 9.5 | 9 | 발표 시연 UI 점검 | 화면 클릭 경로, 오류 메시지, 반응형/데모 데이터 확인 | 전체 | 공동 또는 확인 필요 | UNVERIFIED | M | 9.3, 10.4, 11.4 | `docs/integration/integration-defects.md` |
| 10 | - | 테스트 및 품질 검증 | 단위, 컨트롤러, Mapper, E2E, 수동 검증 | 전체 | 공동 또는 확인 필요 | IN_PROGRESS | L | 1~9 | EV-043, EV-044 |
| 10.1 | 10 | 백엔드 단위/컨트롤러 테스트 | Service, Controller, 공통 인프라 테스트 | FR-001~FR-023 | 공동 또는 확인 필요 | DONE | L | 1~8 | EV-043, traceability Test_Evidence |
| 10.2 | 10 | Mapper/MySQL 통합 테스트 | 실제 MySQL 기반 Mapper safety/integration test | DB 관련 전체 | 팀원 A | DONE | M | 1.3, 11.1 | Git `51c0bdb`, `bba86bb`, EV-043 |
| 10.3 | 10 | Running Playwright E2E | 러닝 auth guard, 기록/세션 CRUD/참여 E2E | FR-006, FR-007 | 팀원 B | DONE | M | 6.5, 11.2 | EV-044 |
| 10.4 | 10 | 전체 프론트 E2E 확대 | 음악/리뷰/플레이리스트/추천/Spotify 화면 E2E 추가 | FR-008~FR-020 | 팀원 B | NOT_STARTED | L | 9.3 | EV-044: running 외 E2E 부족 |
| 10.5 | 10 | 전체 Maven 테스트 재실행 | Java 25 환경에서 `mvnw clean test` 실행 | 전체 | 공동 또는 확인 필요 | BLOCKED | M | 1.1 | 현재 로컬 Java 8 enforcer 실패 이력 |
| 10.6 | 10 | 실제 브라우저 수동 검증 | JSP/Vue 통합 로그인, 쿠키, OAuth 복귀, 주요 CRUD 클릭 확인 | 전체 | 공동 또는 확인 필요 | UNVERIFIED | L | 9.5, 11.4 | integration defects: 브라우저 클릭 검증 미완료 |
| 11 | - | Docker 및 실행 환경 | Docker Compose, 환경 변수, 배포/시연 실행 | FR-022 | 공동 또는 확인 필요 | IN_PROGRESS | L | 1~10 | EV-042 |
| 11.1 | 11 | Docker Compose 구성 | MySQL, backend, frontend, healthcheck, volumes, env 구성 | FR-022 | 공동 또는 확인 필요 | DONE | M | 1.1, 1.3, 9.1 | EV-042, `compose.yaml` |
| 11.2 | 11 | Frontend build 검증 | `npm install`, `npm run build`, Vite production build 확인 | FR-022 | 팀원 B | DONE | S | 9.1 | cleanup 단계 build 성공, `package.json` |
| 11.3 | 11 | 환경 변수/시크릿 준비 | DB, CORS, Spotify, AI provider 환경 변수 설정 | FR-018~FR-022 | 공동 또는 확인 필요 | IN_PROGRESS | M | 11.1 | `.env.example`, `frontend/.env.example`, Spotify/AI UNVERIFIED |
| 11.4 | 11 | Docker 전체 기동 검증 | `docker compose up --build`로 실제 서비스 기동 확인 | FR-022 | 공동 또는 확인 필요 | UNVERIFIED | M | 11.1, 11.3 | Docker config만 확인, 실제 up 미확인 |
| 12 | - | 문서화 및 발표 산출물 | 산출물, 발표/시연 자료, 최종 문서 정리 | 전체 | 공동 또는 확인 필요 | IN_PROGRESS | L | 1~11 | `docs/project-artifacts` |
| 12.1 | 12 | 근거 데이터 산출물 | 프로젝트 inventory, evidence CSV, architecture, open issues, JSON | 전체 | 공동 또는 확인 필요 | DONE | M | 1.1 | `docs/project-artifacts/00-evidence` |
| 12.2 | 12 | 요구사항 정의서 | 목표 요구사항과 구현 상태 분리, 추적성 CSV | 전체 | 공동 또는 확인 필요 | DONE | M | 12.1 | `docs/project-artifacts/01-requirements` |
| 12.3 | 12 | Use Case Diagram/명세 | PlantUML 다이어그램과 Use Case 상세 명세 | 전체 | 공동 또는 확인 필요 | DONE | M | 12.2 | `docs/project-artifacts/02-usecase` |
| 12.4 | 12 | ERD/DB 설계 문서 | DBML, Mermaid, 데이터 사전, 관계 감사 | DB 관련 전체 | 공동 또는 확인 필요 | DONE | M | 12.2, 1.3 | `docs/project-artifacts/03-erd` |
| 12.5 | 12 | WBS 산출물 | WBS, CSV, risk register 작성 | 전체 | 공동 또는 확인 필요 | DONE | M | 12.2, 12.3, 12.4 | 본 문서 |
| 12.6 | 12 | Gantt Chart 작성 | WBS 기반 일정표 산출물 작성 | 전체 | 공동 또는 확인 필요 | DONE | M | 12.5 | `docs/project-artifacts/05-gantt` |
| 12.7 | 12 | 화면 설계서 작성 | 실제 JSP/Vue 화면 기준 화면 설계서 작성 | 전체 | 공동 또는 확인 필요 | DONE | L | 9.4, 12.5 | `docs/project-artifacts/06-screen-design` |
| 12.8 | 12 | 발표 시연 체크리스트 | 데모 계정, Spotify/AI 환경, 클릭 동선, 실패 대안 정리 | 전체 | 공동 또는 확인 필요 | IN_PROGRESS | M | 10.6, 11.4 | `docs/integration/*`, UNVERIFIED 항목 다수 |

## 5. 상태 요약

| 구분 | 개수 |
|---|---:|
| 최상위 작업 | 12 |
| 전체 작업 패키지 | 76 |
| DONE | 56 |
| IN_PROGRESS | 5 |
| NOT_STARTED | 3 |
| BLOCKED | 4 |
| UNVERIFIED | 8 |

## 6. 미완료 핵심 작업

| WBS ID | 작업명 | 상태 | 핵심 이유 |
|---|---|---|---|
| 2.5 | Vue 전용 러닝 프로필 화면 후속 통합 | NOT_STARTED | 발표 기준은 JSP 화면으로 확정했고, Vue Router 화면은 후속 SPA 통합 과제이다. |
| 3.4 | 내부 음악 관리 권한 정책 | UNVERIFIED | 관리자 역할/권한 모델이 확인되지 않았다. |
| 8.4 | Spotify 실제 OAuth 검증 | BLOCKED | Spotify Dashboard redirect URI, scope, 사용자 계정이 필요하다. |
| 8.5 | Spotify 실제 Export/Import 검증 | BLOCKED | 실제 Spotify 계정 권한과 외부 API 성공 확인이 필요하다. |
| 10.4 | 전체 프론트 E2E 확대 | NOT_STARTED | Running 외 도메인의 E2E가 부족하다. |
| 10.5 | 전체 Maven 테스트 재실행 | BLOCKED | 현재 로컬 Maven Java가 8로 확인되어 Java 25 enforcer를 통과하지 못한다. |
| 11.4 | Docker 전체 기동 검증 | UNVERIFIED | Compose config는 확인됐지만 실제 `up --build` 검증은 미수행이다. |

## 7. 발표 전 반드시 검증할 작업

1. `8.4`, `8.5`: 실제 Spotify OAuth, 검색, 가져오기, 내보내기 성공/실패 경로.
2. `10.5`: Java 25 환경에서 전체 backend test 재실행.
3. `10.6`: JSP 로그인/회원가입/마이페이지와 Vue 보호 라우트 복귀 브라우저 클릭 검증.
4. `11.4`: Docker Compose 전체 기동, healthcheck, demo seed 확인.
5. `9.5`, `12.8`: 발표 시연 동선, 오류 메시지, 외부 API 실패 시 대체 시나리오 점검.
