# SSAFIT PlayList 프로젝트 전수 점검 보고서

## 1. 점검 범위

본 문서는 현재 저장소의 `develop` 브랜치 기준 구현 상태를 최종 보고서 작성용 사실 근거로 정리한다. 코드는 수정하지 않았고, 저장소 구조, 설정, 백엔드/프론트엔드 구현, Mapper/XML, 테스트, Docker, Git 이력, 기존 문서를 확인했다.

주요 확인 대상은 다음과 같다.

- 루트 구조: `README.md`, `compose.yaml`, `.env.example`, `docker/`, `docs/`, `frontend/`, `ssafit/`
- 백엔드: `ssafit/pom.xml`, `ssafit/Dockerfile`, `src/main/java`, `src/main/resources/application*.yml`, `src/main/resources/mappers`, `src/main/resources/sql`
- 프론트엔드: `frontend/package.json`, `frontend/vite.config.js`, `frontend/nginx.conf`, `frontend/src/router/index.js`, `frontend/src/api`, `frontend/src/views`, `frontend/src/components`
- 테스트: `ssafit/src/test`, `frontend/e2e`, `docs/verification`
- 문서: `docs/`, `docs/project-artifacts`, `docs/integration`, `docs/verification`
- Git 이력: 최근 30개 커밋 및 병합 이력

확인 시점의 주요 Git 이력은 다음과 같다.

- `0fe340b docs: add project artifacts and final traceability package`
- `dff5044 Merge branch 'feature/b-ai-feature-update' into develop`
- `f44589f feat: 추천 히스토리 관리 및 데모 스냅샷 갱신`
- `0f3eed9 merge: integrate Spotify auth and playlist features`
- `001aeae Merge branch 'feature/cadence-intensity-spotify-live' into develop`
- `bba86bb build: enforce Java 25 and add MySQL-backed E2E tests`

## 2. 프로젝트 기술 스택

| 영역 | 확인된 기술 |
| --- | --- |
| Backend | Java 25, Spring Boot 4.0.6, Maven Wrapper, WAR 패키징 |
| Web | Spring MVC REST API, JSP view, Session 기반 인증 |
| Persistence | MyBatis 4.0.1, MySQL Connector/J, MySQL 8.x |
| Frontend | Vue 3.5, Vite 6, Vue Router 4, Axios |
| E2E | Playwright |
| Runtime | Docker Compose, backend Java 25 container, frontend Nginx container, MySQL container |
| External API | Spotify Web API, OpenAI-compatible Responses API |

- 근거 파일: `ssafit/pom.xml`
- 근거 클래스 또는 메서드: `pom.xml`
- 판단: 구현 완료
- 판단 사유: Spring Boot 4.0.6, Java 25, MyBatis, MySQL, validation, JSP 의존성이 선언되어 있다.

- 근거 파일: `frontend/package.json`
- 근거 클래스 또는 메서드: `scripts`, `dependencies`
- 판단: 구현 완료
- 판단 사유: Vue/Vite/Axios/Vue Router 및 Playwright 스크립트가 선언되어 있다.

## 3. 시스템 구조 요약

시스템은 Spring Boot 백엔드와 Vue 프론트엔드가 분리된 구조이며, 회원 가입/로그인/마이페이지 일부는 JSP로 유지된다. Docker 환경에서는 Nginx가 Vue 정적 파일을 제공하고 `/api`, `/login`, `/join`, `/members/*`, 정적 JSP 리소스 경로를 백엔드로 프록시해 브라우저 origin을 `http://127.0.0.1:5173`으로 맞춘다.

```text
Browser
  -> frontend Nginx/Vite
      -> Vue routes: music, playlists, recommendations, running records/sessions
      -> proxy /api/* to Spring Boot
      -> proxy /login, /join, /members/* to JSP backend
  -> Spring Boot
      -> Controller -> Service -> MyBatis Mapper -> MySQL
      -> Spotify API / OpenAI-compatible API
```

- 근거 파일: `frontend/nginx.conf`
- 근거 클래스 또는 메서드: `location /api/`, `location = /login`, `location ^~ /members/`
- 판단: 구현 완료
- 판단 사유: Docker 프론트엔드 Nginx가 Vue와 JSP/REST API 경로를 분기 프록시한다.

- 근거 파일: `frontend/src/router/index.js`
- 근거 클래스 또는 메서드: `routes`, `router.beforeEach`
- 판단: 구현 완료
- 판단 사유: Vue Router가 음악, 플레이리스트, 추천, 러닝 기록, 러닝 세션, Spotify OAuth 완료 화면을 정의하고 인증 필요 라우트에서 세션 확인을 수행한다.

## 4. 도메인 및 주요 기능 목록

- 회원/인증: 회원 가입, 로그인, 로그아웃, 세션 조회, 마이페이지, Spotify 로그인 계정 온보딩
- 음악: 목록, 검색, 정렬, 상세, 내부 음악 등록/수정, Spotify 트랙 선택 저장
- 리뷰: 음악별 리뷰 등록/수정/삭제/조회, 평균 점수 조회
- 플레이리스트: CRUD, 공개/비공개, 수록곡 추가/삭제/순서 변경, 저장 목록, 추천 결과 저장
- 추천: 러닝 강도, 케이던스, 심박, 선호 장르, 음악 BPM/에너지 기반 추천, 추천 로그 조회/삭제/저장
- 러닝 프로필: 성별, 나이, 최대 심박 추정, 목표 심박/케이던스 자동 갱신, 선호 장르
- 러닝 기록: CRUD, 거리/시간 기반 페이스 계산, 케이던스/심박 검증
- 러닝 세션: 모임 CRUD, 상태 변경, 취소, 참여/참여 취소, 정원 검증
- Spotify: 트랙 검색, OAuth 연결/로그인, 토큰 갱신, 내 플레이리스트 조회/가져오기, SSAFIT 플레이리스트 내보내기
- 생성형 AI: AI 러닝 리포트, 목표 거리 기반 운동 계획 생성, fallback 생성

## 5. 기능별 구현 현황 표

| 기능 | 상태 | 핵심 근거 |
| --- | --- | --- |
| 회원 가입 | 구현 완료 | `MemberApiController.join`, `MemberViewController.join`, `MemberService.register`, `MemberMapper.insert` |
| 로그인/로그아웃 | 구현 완료 | `AuthApiController.login/logout`, `MemberViewController.login/logout`, `AuthService.login/logout` |
| 세션 기반 인증 | 구현 완료 | `SessionConst.LOGIN_MEMBER_ID`, `SessionMemberResolver`, `AuthService.getLoginMemberId` |
| 음악 목록/검색/정렬/상세 | 구현 완료 | `MusicApiController.getMusicList/getMusicDetail`, `MusicService.getMusicList`, `MusicMapper.selectMusicList` |
| 내부 음악 등록/수정 | 구현 완료 | `MusicApiController.createInternalMusic/updateInternalMusic`, `MusicService.createInternalMusic/updateInternalMusic` |
| 음악 삭제 | 미구현 | 삭제 API/서비스 메서드가 확인되지 않음 |
| Spotify 트랙 검색/저장 | 부분 구현 | `SpotifyService.searchTracks`, `MusicService.saveSelectedSpotifyTrack`; BPM/장르 보강은 API 제한과 실패 fallback 존재 |
| 음악 리뷰 CRUD | 구현 완료 | `MusicReviewApiController`, `MusicReviewService`, `MusicReviewMapper.xml` |
| 플레이리스트 CRUD | 구현 완료 | `PlaylistApiController`, `PlaylistService`, `PlaylistMapper.xml` |
| 플레이리스트 수록곡 관리 | 구현 완료 | `PlaylistItemApiController`, `PlaylistItemService`, `PlaylistItemMapper.xml` |
| 플레이리스트 저장/취소 | 구현 완료 | `PlaylistSaveApiController`, `PlaylistSaveService`, `PlaylistSaveMapper.xml` |
| 러닝 기반 추천 | 구현 완료 | `RecommendationApiController`, `RecommendationService.recommend`, `RecommendationLogMapper.xml` |
| 추천 로그 조회/삭제/저장 | 구현 완료 | `RecommendationService.getMyRecommendationLogs/deleteMyRecommendationLog/saveRecommendation` |
| 러닝 프로필 | 구현 완료 | `RunningProfileApiController`, `RunningProfileViewController`, `RunningProfileService`, `RunningProfileMapper.xml` |
| 러닝 기록 CRUD | 구현 완료 | `RunningRecordApiController`, `RunningRecordService`, `RunningRecordMapper.xml` |
| 러닝 세션 CRUD/상태/취소 | 구현 완료 | `RunningSessionApiController`, `RunningSessionService`, `RunningSessionMapper.xml` |
| 러닝 세션 참여/취소 | 구현 완료 | `RunningSessionParticipantApiController`, `RunningSessionParticipantService`, `selectByIdForUpdate` |
| Spotify OAuth 연결/로그인 | 구현 완료 | `SpotifyApiController`, `SpotifyLoginViewController`, `SpotifyOAuthService` |
| Spotify 플레이리스트 가져오기 | 구현 완료 | `SpotifyApiController.getMyPlaylists/importPlaylist`, `SpotifyPlaylistImportService` |
| Spotify 내보내기 | 구현 완료 | `PlaylistExportApiController`, `PlaylistExportService.exportWithResult` |
| AI 러닝 리포트 | 구현 완료 | `AiRunningReportApiController`, `AiRunningReportGenerationService`, `OpenAiRunningReportProvider` |
| 목표 거리 기반 AI 운동 계획 | 부분 구현 | `RecommendationService.generateWorkoutPlan`; provider 없거나 실패 시 서버 계산 fallback |
| 실 API 기반 LLM 테스트 | 테스트 필요 | 기본 테스트는 fake/mock provider 및 `AI_RUNNING_REPORT_ENABLED=false` 중심 |

핵심 판단 근거:

- 근거 파일: `ssafit/src/main/java/com/ssafy/ssafit/music/controller/api/MusicApiController.java`
- 근거 클래스 또는 메서드: `MusicApiController.getMusicList`, `MusicApiController.createInternalMusic`, `MusicApiController.updateInternalMusic`
- 판단: 부분 구현
- 판단 사유: 음악 조회/등록/수정과 Spotify 저장은 구현되어 있으나 음악 삭제 API는 확인되지 않는다.

- 근거 파일: `ssafit/src/main/java/com/ssafy/ssafit/playlist/controller/api/PlaylistItemApiController.java`
- 근거 클래스 또는 메서드: `getItems`, `add`, `updateOrder`, `reorder`, `delete`
- 판단: 구현 완료
- 판단 사유: 수록곡 조회, 추가, 단건 순서 변경, 일괄 순서 변경, 삭제 API가 모두 존재한다.

- 근거 파일: `ssafit/src/main/java/com/ssafy/ssafit/runningsession/service/RunningSessionParticipantService.java`
- 근거 클래스 또는 메서드: `joinSession`
- 판단: 구현 완료
- 판단 사유: 세션 row lock, 로그인 사용자 재확인, OPEN 상태, 중복 참여, 정원 초과, unique key 충돌을 검증한다.

## 6. 외부 API 연동 현황

### Spotify

Spotify 연동은 실제 API 호출 코드가 구현되어 있다.

- Client Credentials 토큰 발급: `SpotifyRestApiClient.issueClientAccessToken`
- 트랙 검색: `SpotifyRestApiClient.searchTracks`
- Audio Features tempo 조회: `SpotifyRestApiClient.getTrackTempoBpm`
- Artist genre 조회: `SpotifyRestApiClient.getArtistGenres`
- OAuth code 교환/refresh: `exchangeAuthorizationCode`, `refreshAccessToken`
- 사용자 프로필 조회: `getCurrentUser`
- 내 플레이리스트 조회/트랙 조회: `getCurrentUserPlaylists`, `getPlaylistTracks`
- 플레이리스트 생성/수록곡 추가: `createPlaylist`, `addTracks`

다만 Spotify 정책/API 제한 대응으로 BPM, 장르, 오디오 특성 기반 흐름은 제한적으로 표기해야 한다.

- `SpotifyService.toMetadata`는 검색 결과 트랙마다 tempo와 genre 조회를 시도하지만 실패 시 null로 저장한다.
- `MusicTempoProvider`는 별도 tempo provider 추상화이나 기본 구현은 `NoOpMusicTempoProvider`이고 `SPOTIFY_AUDIO_FEATURE_ENRICHMENT_ENABLED=false`가 기본값이다.
- Spotify Search API 자체는 BPM/장르/오디오 특성 조건 검색 API가 아니므로, 추천 후보 수집은 검색어 기반 후보 확보 후 로컬 DB/서버 점수화로 대체된다.

- 근거 파일: `ssafit/src/main/java/com/ssafy/ssafit/spotify/client/SpotifyRestApiClient.java`
- 근거 클래스 또는 메서드: `searchTracks`, `getTrackTempoBpm`, `getArtistGenres`, `createPlaylist`, `addTracks`
- 판단: 구현 완료
- 판단 사유: Spotify Web API 엔드포인트를 `RestClient`로 실제 호출한다.

- 근거 파일: `ssafit/src/main/java/com/ssafy/ssafit/music/tempo/NoOpMusicTempoProvider.java`
- 근거 클래스 또는 메서드: `resolveMusicBpm`
- 판단: 외부 정책 또는 API 제한으로 대체 구현
- 판단 사유: 별도 음악 tempo 보강 provider는 기본 no-op이며, 오디오 특성 보강은 기본 비활성이다.

## 7. 생성형 AI 활용 현황

생성형 AI 기능은 실제 API 호출 구현과 fallback 구현이 함께 존재한다.

| 기능 | 실제 API 호출 | Mock/더미 | fallback | 판단 |
| --- | --- | --- | --- | --- |
| AI 러닝 리포트 | 있음. `OpenAiRunningReportProvider`가 `/responses` 호출 | 테스트는 mock/fake provider 사용 | 있음. 데이터 부족/provider 없음/API 실패 시 `FALLBACK` 리포트 저장 | 구현 완료 |
| 목표 거리 기반 운동 계획 추천 | 있음. `RecommendationService.generateWorkoutPlan`에서 `AiRunningReportLlmProvider` 사용 | 테스트는 provider mock 가능 | 있음. `fallbackWorkoutPlan` | 부분 구현 |
| 단순 프롬프트 설계 | 있음 | 해당 없음 | 해당 없음 | 구현 완료 |

운영 기본값은 `AI_RUNNING_REPORT_ENABLED=false`, `AI_RUNNING_REPORT_PROVIDER=none`이므로 실제 LLM 호출은 명시적으로 환경 변수를 설정해야 발생한다. 최종 보고서에서는 “항상 AI API를 호출한다”라고 쓰면 안 된다.

- 근거 파일: `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/provider/OpenAiRunningReportProvider.java`
- 근거 클래스 또는 메서드: `OpenAiRunningReportProvider.generate`
- 판단: 구현 완료
- 판단 사유: OpenAI 호환 Responses API `/responses`로 `model`, `input`, `max_output_tokens`를 포함한 HTTP POST 요청을 보낸다.

- 근거 파일: `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/service/AiRunningReportGenerationService.java`
- 근거 클래스 또는 메서드: `generateLocked`, `saveFallback`
- 판단: 구현 완료
- 판단 사유: 데이터 부족, provider 미설정, API 실패, JSON 파싱 실패 시 서버 계산 기반 fallback 리포트를 저장한다.

- 근거 파일: `compose.yaml`, `.env.example`, `ssafit/src/main/resources/application.yml`
- 근거 클래스 또는 메서드: `AI_RUNNING_REPORT_ENABLED`, `ai.running-report`
- 판단: 부분 구현
- 판단 사유: 실제 LLM 호출 기능은 있으나 기본 실행 환경에서는 비활성화된다.

## 8. Docker 및 실행 환경 현황

Docker Compose는 MySQL, backend, frontend 3개 서비스를 구성한다.

- MySQL: `mysql:8.4.4`, `schema.sql`, `demo-snapshot.sql`을 최초 볼륨 생성 시 entrypoint로 실행
- Backend: `eclipse-temurin:25-jdk-noble` 빌드, `eclipse-temurin:25-jre-noble` 런타임, Spring `docker` profile
- Frontend: `node:22.12.0-alpine` 빌드, `nginx:1.27.4-alpine` 런타임
- Docker 브라우저 접근 기본값: `http://127.0.0.1:5173`
- Docker 백엔드 DB 연결: `mysql:3306`
- 로컬 기본 백엔드 DB 연결: `localhost:3306/ssafit_playlist`

Docker 환경과 로컬 개발 환경 차이는 다음과 같다.

| 구분 | 로컬 | Docker |
| --- | --- | --- |
| Backend DB host | `localhost` | `mysql` service |
| Backend port | `8080` | container `8080`, Nginx proxy로 브라우저는 `5173` 중심 |
| Frontend | Vite dev server `5173` | Nginx `80`, host `5173` |
| JSP 접근 | `8080/login` 가능 | `5173/login`이 Nginx를 통해 backend로 proxy |
| SQL init | `spring.sql.init.mode=never` | MySQL entrypoint가 schema/demo SQL 실행 |
| AI/Spotify secret | OS/IDE env 또는 `.env` | Compose env |

- 근거 파일: `compose.yaml`
- 근거 클래스 또는 메서드: `services.mysql`, `services.backend`, `services.frontend`
- 판단: 구현 완료
- 판단 사유: 각 컨테이너, healthcheck, 환경 변수, SQL volume, 프록시 전제값이 정의되어 있다.

- 근거 파일: `ssafit/src/main/resources/application-docker.yml`
- 근거 클래스 또는 메서드: `spring.datasource.url`, `server.forward-headers-strategy`
- 판단: 구현 완료
- 판단 사유: Docker 내부 DB host와 forward header 전략이 로컬 설정과 분리되어 있다.

환경 테스트는 사용자가 STS 및 Docker 환경에서 여러 차례 수행해 통과한 것으로 전제한다. 기존 검증 문서도 전체 백엔드 테스트, MySQL mapper 통합 테스트, Docker/Java 환경 검증 결과를 통과로 기록하고 있다.

## 9. 테스트 및 검증 현황

테스트 범위는 넓게 구성되어 있다.

- 백엔드 단위 테스트: 회원, 인증, 음악, 리뷰, 플레이리스트, 추천, 러닝 기록/프로필/세션, Spotify, AI 러닝 리포트
- Controller 테스트: 음악, Spotify, AI 러닝 리포트, 러닝 기록, 러닝 세션, 회원 JSP view
- MySQL 통합 테스트: `MapperIntegrationTest`, `AiRunningReportMapperIntegrationTest`
- 테스트 DB 안전장치: `TestDatabaseGuard`, `TestDatabaseSafetyTest`
- E2E: `frontend/e2e/running-features.spec.js`
- Docker/환경 검증 문서: `docs/verification`, `docs/mysql-integration-test-report.md`, `docs/running-features-final-verification-report.md`

환경 테스트는 전부 통과했다고 가정한다.

확인된 기존 검증 문서상 결과:

- `docs/verification/mysql-mapper-integration-test-report.md`: Mapper 통합 테스트 `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`, 전체 Backend 테스트 `Tests run: 259, Failures: 0, Errors: 0, Skipped: 0`
- `docs/verification/running-features-e2e-result.md`: 러닝 기능 E2E 결과 문서 존재
- `docs/verification/java-25-environment-verification.md`: Java 25 환경 검증 문서 존재

- 근거 파일: `ssafit/src/test/resources/application-mapper-test.yml`
- 근거 클래스 또는 메서드: `spring.sql.init.schema-locations`
- 판단: 구현 완료
- 판단 사유: 전용 MySQL 테스트 DB에 schema reset/schema init을 강제한다.

- 근거 파일: `frontend/playwright.config.js`
- 근거 클래스 또는 메서드: `requiredEnvironment`, `webServer`
- 판단: 구현 완료
- 판단 사유: 전용 test DB 이름 검증, backend test-run, frontend dev server를 사용한 E2E 실행 구성이 있다.

- 근거 파일: `ssafit/src/test/java/com/ssafy/ssafit/mapper/TestDatabaseGuard.java`
- 근거 클래스 또는 메서드: `TestDatabaseGuard`
- 판단: 구현 완료
- 판단 사유: 테스트 DB URL/계정/DB명 검증을 통해 개발 DB 오염을 방지한다.

## 10. 발견된 이슈 및 위험 요소

| 심각도 | 이슈 | 현재 판단 | 근거 |
| --- | --- | --- | --- |
| P0 | 서비스 핵심 흐름 불가 | 발견 없음 | 주요 Controller/Service/Mapper/Vue/E2E가 존재하고 환경 테스트 통과 전제 |
| P1 | 음악 삭제 API 부재 | 부분 구현 | `MusicApiController`에 DeleteMapping 없음 |
| P1 | Spotify BPM/장르/오디오 특성 의존성 | 외부 정책 또는 API 제한으로 대체 구현 | `SpotifyService.resolveTempoBpm/resolveGenre` 실패 시 null, `NoOpMusicTempoProvider` |
| P1 | Spotify Export와 URI 없는 수록곡 | 주요 기능 불완전 가능 | `PlaylistExportService`는 URI 없는 곡을 skipped로 집계하므로 전체 곡 export가 보장되지 않음 |
| P1 | 실제 LLM 호출은 기본 비활성 | 부분 구현 | `AI_RUNNING_REPORT_ENABLED=false`, provider `none` 기본 |
| P2 | README 인코딩 깨짐 | 문서화 보완 필요 | 현재 터미널 출력에서 한글이 깨져 표시됨 |
| P2 | Spotify 실 API 검증은 수동/환경 의존 | 테스트 필요 | 자동 테스트는 mock 중심, 실제 API는 credentials/개발 모드/redirect 등록 필요 |
| P2 | Vue/JSP 혼재 리다이렉트 복잡성 | 사용성 검증 필요 | Router, Nginx, `FrontendReturnUrl`이 origin과 returnTo를 함께 관리 |
| P3 | 토큰 평문 저장 주의 | 개선 권장 | `spotify_connections`에 access/refresh token 컬럼 저장, schema 주석도 암호화 권장 언급 |
| P3 | 문서와 현재 구현 간 시점 차이 | 문서화 개선 | 일부 과거 감사 문서에는 Controller/Vue 미구현 등 현재와 다른 판단이 남아 있음 |

핵심 이슈 근거:

- 근거 파일: `ssafit/src/main/java/com/ssafy/ssafit/music/controller/api/MusicApiController.java`
- 근거 클래스 또는 메서드: 전체 API mapping
- 판단: 부분 구현
- 판단 사유: 음악 생성/수정/조회는 있으나 삭제 API는 확인되지 않는다.

- 근거 파일: `ssafit/src/main/java/com/ssafy/ssafit/spotify/service/PlaylistExportService.java`
- 근거 클래스 또는 메서드: `exportWithResult`
- 판단: 부분 구현
- 판단 사유: Spotify URI가 없는 곡은 `skippedMusicIds`로 분류되어 export 결과에서 제외된다.

- 근거 파일: `ssafit/src/main/java/com/ssafy/ssafit/common/web/FrontendReturnUrl.java`
- 근거 클래스 또는 메서드: `safeRedirect`, `spotifyCompletionRedirect`
- 판단: 구현 완료
- 판단 사유: Vue/JSP 혼재 구조에서 return URL을 제한적으로 허용하는 보호 로직이 있다. 다만 origin 설정이 맞지 않으면 리다이렉트 UX가 깨질 수 있어 환경 검증이 중요하다.

## 11. 최종 보고서 작성 시 주의할 사실

최종 보고서에서 과장하거나 단정하면 안 되는 항목:

- “음악 CRUD 전체 구현”이라고 쓰면 안 된다. 현재 음악은 조회/검색/등록/수정/Spotify 저장은 있으나 삭제 API는 확인되지 않는다.
- “Spotify가 BPM/장르/오디오 특성 기반으로 정밀 검색한다”라고 쓰면 안 된다. Spotify Search는 검색어 기반이며, tempo/genre는 별도 조회를 시도하고 실패 시 null 처리한다.
- “모든 Spotify 곡이 추천과 export에 완전하게 활용된다”라고 쓰면 안 된다. BPM, energy, genre, spotify_uri가 없으면 추천 점수 또는 export에서 제한이 생긴다.
- “항상 OpenAI API를 호출한다”라고 쓰면 안 된다. 기본값은 AI 비활성/provider none이며, provider 설정과 API key가 있어야 실제 호출한다.
- “AI가 모든 수치 분석을 새로 계산한다”라고 쓰면 안 된다. 서버가 집계한 데이터를 LLM에 전달하고, LLM은 문장화/운동 계획 제안에 사용된다.
- “AI 결과만으로 동작한다”라고 쓰면 안 된다. 데이터 부족, provider 미설정, API 실패 시 서버 계산 fallback을 저장/반환한다.
- “Spotify OAuth와 export가 자동 테스트에서 실 API까지 완전 검증된다”라고 쓰면 안 된다. 테스트는 mock 중심이고 실 API 검증은 credentials, redirect URI, 개발 모드 계정 등록에 의존한다.
- “Vue 단일 SPA”라고 쓰면 안 된다. 현재 회원 가입/로그인/마이페이지/러닝 프로필 JSP와 Vue 화면이 혼재한다.
- “Docker와 로컬 실행 경로가 완전히 동일하다”라고 쓰면 안 된다. Docker는 Nginx proxy와 `mysql` hostname, 로컬은 Vite dev server와 `localhost` DB 설정을 사용한다.
- “README를 그대로 최종 사용자 문서로 사용 가능하다”라고 쓰면 안 된다. 현재 확인 환경에서는 README 한글 인코딩이 깨져 표시된다.
- “운영 보안까지 완비”라고 쓰면 안 된다. Spotify token 저장 암호화, secret manager, 운영 로그/보안 정책은 별도 보완 여지가 있다.
- “의료/건강 진단 AI”로 표현하면 안 된다. 러닝 데이터 기반 참고 리포트와 운동/음악 방향 제안이며 건강 진단이 아니다.

## 12. 근거 파일 목록

### 루트 및 실행 환경

- `README.md`
- `.env.example`
- `compose.yaml`
- `ssafit/pom.xml`
- `ssafit/Dockerfile`
- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `ssafit/src/main/resources/application.yml`
- `ssafit/src/main/resources/application-docker.yml`
- `ssafit/src/main/resources/sql/schema.sql`
- `ssafit/src/main/resources/sql/demo-snapshot.sql`

### 백엔드 공통/인증

- `ssafit/src/main/java/com/ssafy/ssafit/SsafitApplication.java`
- `ssafit/src/main/java/com/ssafy/ssafit/common/config/WebConfig.java`
- `ssafit/src/main/java/com/ssafy/ssafit/common/auth/SessionConst.java`
- `ssafit/src/main/java/com/ssafy/ssafit/common/auth/SessionMemberResolver.java`
- `ssafit/src/main/java/com/ssafy/ssafit/common/web/FrontendReturnUrl.java`
- `ssafit/src/main/java/com/ssafy/ssafit/common/exception/ErrorCode.java`
- `ssafit/src/main/java/com/ssafy/ssafit/common/exception/GlobalExceptionHandler.java`
- `ssafit/src/main/java/com/ssafy/ssafit/common/response/ApiResponse.java`

### 회원

- `ssafit/src/main/java/com/ssafy/ssafit/member/controller/api/AuthApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/member/controller/api/MemberApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/member/controller/view/MemberViewController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/member/controller/view/SpotifyLoginViewController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/member/service/AuthService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/member/service/MemberService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/member/mapper/MemberMapper.java`
- `ssafit/src/main/resources/mappers/member/MemberMapper.xml`
- `ssafit/src/main/webapp/WEB-INF/views/member/login.jsp`
- `ssafit/src/main/webapp/WEB-INF/views/member/join.jsp`
- `ssafit/src/main/webapp/WEB-INF/views/member/my-page.jsp`

### 음악/리뷰

- `ssafit/src/main/java/com/ssafy/ssafit/music/controller/api/MusicApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/music/service/MusicService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/music/mapper/MusicMapper.java`
- `ssafit/src/main/resources/mappers/music/MusicMapper.xml`
- `ssafit/src/main/java/com/ssafy/ssafit/musicreview/controller/api/MusicReviewApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/musicreview/service/MusicReviewService.java`
- `ssafit/src/main/resources/mappers/musicreview/MusicReviewMapper.xml`

### 플레이리스트/추천

- `ssafit/src/main/java/com/ssafy/ssafit/playlist/controller/api/PlaylistApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/playlist/controller/api/PlaylistItemApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/playlist/controller/api/PlaylistSaveApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/playlist/service/PlaylistService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/playlist/service/PlaylistItemService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/playlist/service/PlaylistSaveService.java`
- `ssafit/src/main/resources/mappers/playlist/PlaylistMapper.xml`
- `ssafit/src/main/resources/mappers/playlist/PlaylistItemMapper.xml`
- `ssafit/src/main/resources/mappers/playlist/PlaylistSaveMapper.xml`
- `ssafit/src/main/java/com/ssafy/ssafit/recommendation/controller/api/RecommendationApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/recommendation/service/RecommendationService.java`
- `ssafit/src/main/resources/mappers/recommendation/RecommendationLogMapper.xml`

### 러닝

- `ssafit/src/main/java/com/ssafy/ssafit/runningprofile/controller/api/RunningProfileApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/member/controller/view/RunningProfileViewController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/runningprofile/service/RunningProfileService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/runningprofile/service/RunningFitnessMetricService.java`
- `ssafit/src/main/resources/mappers/runningprofile/RunningProfileMapper.xml`
- `ssafit/src/main/java/com/ssafy/ssafit/runningrecord/controller/api/RunningRecordApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/runningrecord/service/RunningRecordService.java`
- `ssafit/src/main/resources/mappers/runningrecord/RunningRecordMapper.xml`
- `ssafit/src/main/java/com/ssafy/ssafit/runningsession/controller/api/RunningSessionApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/runningsession/controller/api/RunningSessionParticipantApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/runningsession/service/RunningSessionService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/runningsession/service/RunningSessionParticipantService.java`
- `ssafit/src/main/resources/mappers/runningsession/RunningSessionMapper.xml`
- `ssafit/src/main/resources/mappers/runningsession/RunningSessionParticipantMapper.xml`

### Spotify/AI

- `ssafit/src/main/java/com/ssafy/ssafit/spotify/controller/api/SpotifyApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/controller/api/PlaylistExportApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/client/SpotifyRestApiClient.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/service/SpotifyService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/service/SpotifyOAuthService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/service/SpotifyPlaylistImportService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/spotify/service/PlaylistExportService.java`
- `ssafit/src/main/resources/mappers/spotify/SpotifyConnectionMapper.xml`
- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/controller/api/AiRunningReportApiController.java`
- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/service/AiRunningReportGenerationService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/service/AiRunningReportAggregationService.java`
- `ssafit/src/main/java/com/ssafy/ssafit/airunningreport/provider/OpenAiRunningReportProvider.java`
- `ssafit/src/main/resources/mappers/airunningreport/AiRunningReportMapper.xml`

### 프론트엔드

- `frontend/package.json`
- `frontend/vite.config.js`
- `frontend/src/router/index.js`
- `frontend/src/api/*.js`
- `frontend/src/views/*.vue`
- `frontend/src/components/*.vue`
- `frontend/e2e/running-features.spec.js`
- `frontend/playwright.config.js`

### 테스트 및 기존 문서

- `ssafit/src/test/java/com/ssafy/ssafit/**/*.java`
- `ssafit/src/test/resources/application-mapper-test.yml`
- `ssafit/src/test/resources/application-e2e.yml`
- `ssafit/src/test/resources/sql/schema-reset.sql`
- `ssafit/src/test/resources/sql/e2e-seed.sql`
- `docs/verification/mysql-mapper-integration-test-report.md`
- `docs/verification/running-features-e2e-result.md`
- `docs/verification/java-25-environment-verification.md`
- `docs/full-service-integration-audit.md`
- `docs/spotify-service-contract.md`
- `docs/ai-running-report-feature.md`
- `docs/project-artifacts/00-evidence/project-inventory.md`
- `docs/project-artifacts/00-evidence/implementation-evidence.csv`
- `docs/project-artifacts/00-evidence/architecture-as-is.md`
