# Vue 공통 기반 연동 기록

## 1. 적용 범위

- 기존 `frontend/` Vue 3/Vite 프로젝트를 유지하고 Router, API 모듈, 공통 상태 UI를 보완했다.
- Backend Controller, DTO, Service, Mapper, DDL은 수정하지 않았다.
- 인증은 `HttpSession`과 Session Cookie만 사용한다.
- API 기본 주소는 `VITE_API_BASE_URL`이 없을 때만 `http://localhost:8080/api`를 사용한다.

## 2. 실제 계약과 문서 계약의 차이

| 항목 | 문서 또는 목표 표현 | 실제 구현 | Frontend 대응 |
| --- | --- | --- | --- |
| 오류 코드 필드 | 일부 목표 문서에 `errorCode` 표기 | `ErrorResponse`는 `{code, message}` | `errorCode`와 `code`를 모두 읽고 `ApiError.errorCode`로 보존 |
| JSP 로그인 복귀 | Vue의 기존 경로 복원 필요 | `/login`은 `returnTo`를 읽지 않고 성공 후 `/members/me`로 이동 | 로그인 URL에 `returnTo`를 전달하지만 자동 복귀는 JSP 후속 구현 필요 |
| Spotify OAuth callback | OAuth 후 Vue 복귀 경로 필요 | `/api/spotify/oauth/callback`이 완료 화면으로 302 Redirect | Ajax callback을 사용하지 않고 완료 화면에서 연결 상태를 재조회 |
| 인증 상태 확인 API | 별도 `/auth/session` 계약 없음 | 보호 API `GET /api/members/me`가 200 또는 401 반환 | Router Guard가 이 API 결과만 사용 |
| Spotify 미연결 | 정책 확정 필요 항목으로 기록된 문서가 있음 | 200과 `connected=false`로 변환 | 정상 상태로 처리 |
| 예상하지 못한 서버 오류 | 공통 JSON 여부 미확정 | `GlobalExceptionHandler`에 catch-all 500 처리 없음 | 응답 본문이 없더라도 HTTP status와 `UNKNOWN_ERROR`를 보존 |
| 계약 감사 보고서의 구현 현황 | 감사 시점에는 Controller/JSP/Vue 미구현으로 기록 | 현재 브랜치에는 Controller, DTO, JSP, CORS, 최소 Vue가 구현됨 | 최신 소스와 `b-ui-api-contract.md`를 우선 적용 |

고정 API의 URL, HTTP Method, path/query 이름은 실제 Controller와 `b-ui-api-contract.md`가 일치해 변경하지 않았다.

## 3. Session/Cookie 및 Router Guard

- Axios 인스턴스 기본값은 `withCredentials: true`다.
- Spring CORS는 `/api/**`, `http://localhost:5173`, `allowCredentials(true)`로 구성되어 있다.
- Router Guard는 `meta.requiresAuth` 화면 진입 전에 `GET /api/members/me`를 호출한다.
- 200 응답만 로그인으로 인정한다.
- 401 `AUTH_REQUIRED`는 `/login-required?returnTo=...`로 이동한다.
- 인증 확인이 네트워크 오류, 5xx 또는 예상하지 못한 형식으로 실패하면 로그인으로 간주하지 않고 `/access-check-failed`로 이동한다.
- JWT, Access Token, Refresh Token, localStorage 기반 인증 상태는 추가하지 않았다.

## 4. OAuth Redirect 처리

1. Vue는 Ajax로 `GET /api/spotify/oauth/authorize`를 호출해 `authorizationUrl`만 받는다.
2. 이후 `window.location.assign(authorizationUrl)`로 브라우저 전체 이동을 수행한다.
3. Spotify callback은 Axios로 호출하지 않으므로 Redirect/HTML 응답을 공통 API 오류로 오인하지 않는다.
4. Vue에는 `/oauth/spotify/complete?returnTo=/원래경로`를 준비했고, 브라우저 이탈 전에는 인증 정보와 무관한 복귀 경로만 `sessionStorage`에 임시 보존한다.
5. 완료 화면은 Router query를 우선하고 없으면 임시 보존 경로를 사용한 뒤 즉시 제거하며 연결 상태를 다시 조회한다.
6. Backend callback은 code/state/token을 Vue URL에 전달하지 않고 완료 화면으로 Redirect한다.

## 5. 연동 검증 상태

검증 완료:

- `npm.cmd run build`: 성공
- API 호출이 Vue 컴포넌트가 아니라 `src/api/`에만 존재함을 정적 확인
- Axios `withCredentials: true`와 환경변수 우선 base URL 확인
- 실제 Controller의 URL, Method, path/query 이름과 Frontend API 모듈 대조
- 기존 `ApiInfrastructureIntegrationTest`에 credential CORS와 401 `{code:"AUTH_REQUIRED"}` 검증이 존재함을 확인

현재 환경에서 재검증 불가:

- Backend 테스트 실행: 프로젝트는 Java 25를 요구하지만 현재 셸은 Java 17이라 컴파일 전에 실패
- JSP 로그인 후 브라우저의 실제 `JSESSIONID` 전달: 실행 가능한 Backend와 DB가 필요
- 실제 Spotify OAuth/검색/Export: Spotify 환경변수와 외부 계정이 필요
- Vite dev 서버: `--configLoader runner` 사용 시 서버는 `ready`까지 도달했으나 현재 샌드박스가 esbuild의 상위 디렉터리 탐색을 차단해 dependency pre-bundle에서 종료. pnpm junction과 npm 일반 설치 양쪽에서 동일했고 production build는 정상

## 6. Backend 미구현 또는 실제 연동 미검증 API

Controller가 없는 것으로 확인된 고정 API는 없다. 다음 항목은 실제 외부/세션 연동이 미검증이다.

- `POST /api/auth/login`, `GET /api/members/me`: 실제 JSP 로그인 Session Cookie 교차 포트 전달
- `GET /api/spotify/tracks/search`: 실제 Spotify API 응답
- `GET /api/spotify/oauth/authorize`, `GET /api/spotify/oauth/callback`: 실제 OAuth 왕복과 Vue 자동 복귀
- `POST /api/music/from-spotify`: 실제 검색 결과 저장
- `POST /api/playlists/{playlistId}/export/spotify`: 실제 Spotify Playlist 생성/수록곡 추가
- DB가 필요한 음악, 플레이리스트, 리뷰, 추천 전체 API의 실데이터 응답

## 7. 다음 구현 항목

1. JSP `/login`이 `returnTo`를 검증·보존하고 로그인 성공 후 허용된 Vue origin으로 복귀하도록 연결
2. Spotify callback 성공/실패 후 `/oauth/spotify/complete`로 redirect하는 adapter 확정
3. 음악 상세 리뷰 작성/수정/삭제 UI
4. 플레이리스트 수록곡 추가, 순서 변경, 삭제 UI
5. 플레이리스트 참조 저장/취소와 Spotify Export 상태 UI
6. 추천 결과의 저장 폼과 추천 로그 목록 UI
7. Java 25, MySQL, Spotify 환경변수가 준비된 브라우저 통합 검증
