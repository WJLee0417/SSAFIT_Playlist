# WBS 리스크 등록부

## 1. Spotify API 정책 및 권한

| Risk ID | 관련 WBS | 설명 | 영향 | 가능성 | 대응 |
|---|---|---|---|---|---|
| R-SPOTIFY-01 | 8.4, 8.5 | Spotify 앱 모드, 사용자 권한, scope 승인, rate limit에 따라 OAuth/import/export가 실패할 수 있다. | High | High | 발표 전 테스트 계정과 Dashboard 설정을 고정하고, 실패 시 내부 상태/오류 처리 시연 대안을 준비한다. |
| R-SPOTIFY-02 | 8.1, 8.5 | 모든 내부 음악에 Spotify URI가 있는 것은 아니므로 export 수록곡 추가가 제한될 수 있다. | High | Medium | export 대상 플레이리스트는 Spotify 저장 곡만 포함한 데모 데이터로 준비한다. |

## 2. OAuth Redirect URI

| Risk ID | 관련 WBS | 설명 | 영향 | 가능성 | 대응 |
|---|---|---|---|---|---|
| R-OAUTH-01 | 8.4, 11.3 | `SPOTIFY_REDIRECT_URI`와 Spotify Dashboard 등록값이 문자 단위로 불일치하면 callback이 실패한다. | High | High | `.env`와 Dashboard 값을 캡처/기록하고 로컬/도커 URL을 분리 검증한다. |
| R-OAUTH-02 | 2.6, 8.4, 9.2 | JSP 로그인과 Vue OAuth 완료 화면 복귀 사이에서 `returnTo` 또는 state가 누락되면 시연 흐름이 끊긴다. | Medium | Medium | `/login/spotify`, `/oauth/spotify/complete`, `/login-required` 경로를 수동 클릭 체크리스트에 포함한다. |

## 3. JSP/Vue 혼합 전환

| Risk ID | 관련 WBS | 설명 | 영향 | 가능성 | 대응 |
|---|---|---|---|---|---|
| R-MIXED-01 | 2.5, 2.6, 9.2 | 러닝 프로필은 JSP 화면 중심이고 Vue 전용 화면이 없어 화면 설계서에서 기능 위치가 혼동될 수 있다. | Medium | High | 발표 기준 화면을 JSP `/members/me/running-profile`로 확정하고, Vue 전용 화면은 후속 SPA 통합 과제로 분리 표기한다. |
| R-MIXED-02 | 2.6, 10.6 | 세션 쿠키, CORS, 프록시 설정 차이로 JSP 로그인 후 Vue 보호 라우트 복귀가 환경별로 달라질 수 있다. | High | Medium | Docker와 로컬 dev server 각각에서 로그인 복귀 흐름을 확인한다. |

## 4. DB 정합성

| Risk ID | 관련 WBS | 설명 | 영향 | 가능성 | 대응 |
|---|---|---|---|---|---|
| R-DB-01 | 1.3, 7.1, 7.4 | `recommendation_logs.result_summary`, `ai_running_reports.input_snapshot_json`은 JSON 스냅샷이라 내부 ID가 FK로 보호되지 않는다. | Medium | Medium | 과거 추천/리포트는 스냅샷으로 취급한다고 문서화하고 삭제 정책을 분리한다. |
| R-DB-02 | 11.4 | `schema.sql`과 실제 로컬/운영 DB가 다를 수 있다. | High | Medium | 발표 전 `SHOW CREATE TABLE` 또는 mapper integration test로 runtime drift를 확인한다. |
| R-DB-03 | 5.2 | 수록곡 순서 변경 중 UNIQUE `(playlist_id, order_no)` 충돌 가능성이 있다. | Medium | Medium | reorder 서비스 테스트와 실제 UI 순서 변경을 재검증한다. |

## 5. Docker 환경 차이

| Risk ID | 관련 WBS | 설명 | 영향 | 가능성 | 대응 |
|---|---|---|---|---|---|
| R-DOCKER-01 | 11.3, 11.4 | 로컬 실행과 Docker 실행의 API/JSP URL, CORS, port, env 값이 다르면 화면 이동이 실패할 수 있다. | High | Medium | `.env.example`, `frontend/.env.example`, `compose.yaml` 기준으로 데모 환경 값을 고정한다. |
| R-DOCKER-02 | 10.5, 11.4 | backend는 Java 25가 필요하지만 로컬 PATH가 Java 8이면 테스트/빌드가 실패한다. | High | High | Maven 실행 JDK를 Java 25로 고정하고 `mvnw -version` 결과를 발표 전 확인한다. |

## 6. API와 화면 연결 누락

| Risk ID | 관련 WBS | 설명 | 영향 | 가능성 | 대응 |
|---|---|---|---|---|---|
| R-UIAPI-01 | 2.5 | `runningProfileApi.js`는 있으나 Vue Router 전용 프로필 화면이 없다. | Medium | High | JSP 유지 정책을 발표 기준으로 확정했다. Vue 화면 구현은 발표 전 작업이 아니라 후속 통합 과제로 둔다. |
| R-UIAPI-02 | 3.4 | 내부 음악 등록/수정 API와 화면은 있으나 관리자 권한 모델이 없다. | Medium | Medium | 관리자 기능으로 발표하지 않고, 내부 관리 API는 UNVERIFIED로 유지한다. |
| R-UIAPI-03 | 4.4, 10.4 | 리뷰/음악/플레이리스트/추천/Spotify의 E2E가 running 대비 부족하다. | Medium | High | 핵심 시연 흐름 중심 Playwright 또는 수동 체크리스트를 보강한다. |

## 7. 테스트 부족

| Risk ID | 관련 WBS | 설명 | 영향 | 가능성 | 대응 |
|---|---|---|---|---|---|
| R-TEST-01 | 10.4 | Running 외 도메인의 프론트 E2E가 부족해 UI 회귀를 자동 탐지하기 어렵다. | Medium | High | 음악 상세/리뷰, 플레이리스트 CRUD, 추천 저장 시나리오부터 추가한다. |
| R-TEST-02 | 10.5 | 현재 환경에서 Java 25가 준비되지 않으면 전체 backend test를 재실행할 수 없다. | High | High | STS/Maven Runner/JAVA_HOME을 Java 25로 맞춘 뒤 전체 테스트를 재실행한다. |
| R-TEST-03 | 10.6 | 실제 브라우저 클릭 기반 검증이 없으면 쿠키, redirect, 외부 API 화면 상태를 놓칠 수 있다. | High | Medium | 발표 전 사람 기준 체크리스트를 실행하고 스크린샷을 남긴다. |

## 8. 추천 로직의 외부 데이터 의존성

| Risk ID | 관련 WBS | 설명 | 영향 | 가능성 | 대응 |
|---|---|---|---|---|---|
| R-REC-01 | 7.1, 7.3 | Spotify 검색 결과가 BPM/장르/오디오 피처를 항상 제공하지 않아 추천 품질이 데이터 품질에 좌우된다. | Medium | High | 데모 데이터에 BPM/장르가 충분한 음악을 준비하고 nullable 처리 흐름을 설명한다. |
| R-REC-02 | 7.5 | AI provider는 환경 변수와 API Key가 없으면 fallback 또는 실패 상태가 된다. | Medium | High | AI 기능은 코드 구현과 fallback 구조를 설명하고, 실 provider 검증은 별도 체크리스트로 관리한다. |
| R-REC-03 | 7.1, 12.8 | 러닝 기록/프로필 데이터가 부족하면 추천 결과가 빈약하거나 fallback으로 떨어질 수 있다. | Medium | Medium | 발표 계정에 러닝 프로필, 최근 기록, 리뷰/플레이리스트 이력을 사전 적재한다. |
