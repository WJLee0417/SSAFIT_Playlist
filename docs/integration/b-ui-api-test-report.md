# B UI/API 통합 테스트 보고서

## 1. 테스트 환경

- 일시: 2026-06-22 KST
- 브랜치: `feature/b-ui-api`
- 기준 HEAD: `5d8a0f5 feat: complete Spotify search OAuth and playlist export UI`
- Java: Temurin 25.0.2
- Spring Boot: 4.0.6
- MySQL: `127.0.0.1:3306`
- 개발 DB: `ssafit_playlist`
- 통합 테스트 DB: `ssafit_test`
- Node: 24.15.0
- npm: 11.12.1
- Frontend: Vue 3 + Vite 6
- 실제 Spotify 환경변수: 미설정

테스트 시작 시 작업 트리는 깨끗했다. 테스트 중 생성한 회원과 데이터에는
`codex_` 접두어를 사용해 기존 데이터와 구분했다.

## 2. 실행한 Backend/Frontend 명령

Backend 전체 테스트:

```powershell
$env:JAVA_HOME = '<STS JustJ JDK 25 경로>'
$env:SSAFIT_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3306/ssafit_test?...'
$env:SSAFIT_TEST_DB_USERNAME = 'ssafit_test'
$env:SSAFIT_TEST_DB_PASSWORD = '<TEST_ONLY_PASSWORD>'
.\mvnw.cmd clean test
```

결과:

```text
Tests run: 232
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Frontend:

```powershell
npm.cmd install
npm.cmd run build
```

결과:

```text
117 modules transformed
BUILD SUCCESS
0 vulnerabilities
```

초기 Frontend build는 `package-lock.json`에 `vue-router`가 누락되어 실패했다.
lock file을 `package.json`과 동기화한 뒤 재빌드에 성공했다.

## 3. JSP 화면 테스트

| 항목 | 결과 | 실제 확인 |
| --- | --- | --- |
| 회원가입 Controller/API | PASS | 201, `members` 행 생성 |
| JSP 로그인 | PASS | `/login` POST → 302, `JSESSIONID` 발급 |
| Vue 복귀 | PASS | 허용된 `http://localhost:5173/...`로 302 |
| 외부 returnTo 차단 | PASS | `https://example.com/...` 입력 시 `/members/me`로 복귀 |
| Session ID URL 노출 방지 | PASS | `Location`에 `;jsessionid` 없음 |
| 러닝 프로필 저장 후 Vue 복귀 | PASS | Controller 단위 테스트 통과 |
| 실제 화면 클릭/시각 확인 | BLOCKED | 인앱 브라우저 초기화 오류 |

## 4. Vue 화면 테스트

| 항목 | 결과 | 근거 |
| --- | --- | --- |
| Vite root 응답 | PASS | HTTP 200, `#app` mount 존재 |
| production build | PASS | 117 modules transformed |
| Router lazy route compile | PASS | 모든 view chunk 생성 |
| `withCredentials` | PASS | 공통 Axios 인스턴스 확인 |
| 401/403/404/409/502 UI 분기 | PASS (코드/응답) | `ApiError`, `PageStatePanel`, 도메인 오류 mapper 확인 |
| 실제 브라우저 렌더링/클릭 | BLOCKED | 브라우저 자동 제어 환경 오류 |
| 새로고침 history fallback | PASS (Vite dev) | Vite history fallback 구조, production 서버 배포 설정은 별도 |

## 5. Session/CORS/Cookie 테스트

실제 경로:

```text
JSP /login
→ Set-Cookie: JSESSIONID; Path=/; HttpOnly
→ Origin: http://localhost:5173
→ GET /api/members/me
→ 200 JSON
```

검증 결과:

- `JSESSIONID` 실제 발급: PASS
- `HttpOnly`: PASS
- 로컬 HTTP이므로 `Secure=false`: 정상
- Axios `withCredentials: true`: PASS
- CORS `Access-Control-Allow-Origin: http://localhost:5173`: PASS
- CORS `Access-Control-Allow-Credentials: true`: PASS
- 비로그인 보호 API: 401 `AUTH_REQUIRED` JSON
- 로그아웃 후 같은 Session API: 401 `AUTH_REQUIRED`
- URL rewriting Session ID 노출: 수정 후 제거
- JWT, Spring Security, localStorage token: 사용하지 않음

## 6. API Tester 또는 Swagger 테스트

Swagger/OpenAPI 의존성은 현재 프로젝트에 없다. 실제 Controller 계약을 기준으로
`docs/integration/ssafit-api-tester.http`를 생성했다.

포함 범위:

- 인증/회원
- 러닝 프로필
- 음악 목록/상세/페이징
- 평가 CRUD
- 플레이리스트 CRUD
- 수록곡 추가/삭제/순서 변경
- 참조 저장/취소/목록
- 자동 큐레이션 조회/로그/저장
- Spotify 검색/내부 저장/OAuth/연결/Export
- 비로그인, 권한 없음, 대상 없음, 중복 요청 예시

Entity 또는 Token Domain을 API 응답으로 직접 노출하지 않는 것을 DTO 코드에서 확인했다.

## 7. Spotify 테스트

| 항목 | 결과 | 실제 결과 |
| --- | --- | --- |
| 검색 Controller 실제 호출 | BLOCKED | 자격증명 없음, 502 `SPOTIFY_SEARCH_FAILED` |
| 검색 결과 내부 music 저장 | PASS | 실제 MySQL에 `source_type=SPOTIFY` 저장 |
| 저장 music을 수록곡 추가 | PASS | 실제 `playlist_items` 생성 |
| OAuth URL 생성 | BLOCKED | 환경변수 없음, 500 `SPOTIFY_CONFIGURATION_ERROR` |
| OAuth 전체 브라우저 이동 코드 | PASS (코드) | JSON URL 검증 후 `window.location.assign` |
| Callback Vue 복귀 | PASS (코드/HTTP) | 고정 Vue 완료 URL로 302, code/state 미포함 |
| 실제 Spotify 승인/Token 교환 | NOT TESTED | 계정·Client ID/Secret 없음 |
| 실제 Playlist 생성/곡 추가 | NOT TESTED | 연결 Token 없음 |
| 미연결 Export | PASS | 401 `SPOTIFY_AUTH_REQUIRED` |
| Access/Refresh Token 비노출 | PASS | DTO/Vue/문서 검색 확인 |

Callback 실패 검증:

```text
/api/spotify/oauth/callback?code=<REDACTED>&state=<REDACTED>
→ 302
→ /oauth/spotify/complete?oauthError=SPOTIFY_OAUTH_STATE_MISMATCH
```

Vue URL에는 Authorization Code, OAuth state, Access/Refresh Token이 전달되지 않는다.

## 8. 자동 큐레이션 조회/저장 분리 테스트

실제 API 및 MySQL 기준 결과:

1. 추천 조회 성공: 200, `recommendation_logs.id=1`
2. 조회 직후 소유 플레이리스트 개수: 1
3. 추천 저장 성공: 201
4. 저장 후 소유 플레이리스트 개수: 2
5. 생성 행: `playlist_type=USER`, `source_type=CURATION_COPY`
6. `recommendation_logs.saved_playlist_id=6`
7. 수록곡이 새 playlist로 복사됨
8. `playlist_saves`는 자동 큐레이션 저장에 사용되지 않음

따라서 조회와 저장이 실제 DB에서 분리되어 있다.

## 9. 오류 코드별 UI 테스트

| HTTP | 실제 응답 | Vue 처리 |
| --- | --- | --- |
| 401 | `AUTH_REQUIRED`, `SPOTIFY_AUTH_REQUIRED` | 로그인 필요/Spotify 재연결 |
| 403 | `ACCESS_DENIED` | 권한 없음 상태 |
| 404 | `MUSIC_NOT_FOUND`, `RESOURCE_NOT_FOUND` | 대상 없음 제목 |
| 409 | `DUPLICATE_PLAYLIST_ITEM` | 충돌 상태 및 도메인 메시지 |
| 500 | `SPOTIFY_CONFIGURATION_ERROR`, `INTERNAL_SERVER_ERROR` | 일반 서버 오류와 ErrorCode 표시 |
| 502 | `SPOTIFY_SEARCH_FAILED` | 외부 서비스 연결 오류 |

잘못된 API 경로와 미지원 HTTP method도 stack trace 없는 공통 JSON으로 정리했다.

## 10. 핵심 시나리오 결과

| 순서 | 시나리오 | 상태 | 비고 |
| --- | --- | --- | --- |
| 1 | 회원가입 | PASS | API + MySQL |
| 2 | JSP 로그인 | PASS | Cookie 발급 및 Vue API 재사용 |
| 3 | 러닝 프로필 설정 | PASS | API + MySQL |
| 4 | Spotify 검색 | BLOCKED | 실 자격증명 없음 |
| 5 | Spotify 곡 DB 저장 | PASS | 외부 검색 결과 형태를 실제 DB 저장 |
| 6 | 음악 목록/상세 | PASS | API 재조회 |
| 7 | 음악 평가 CRUD | PASS | 생성/수정/삭제 및 DB 확인 |
| 8 | 플레이리스트 생성 | PASS | USER + MANUAL |
| 9 | Spotify 곡 수록곡 추가 | PASS | DB 및 상세 재조회 |
| 10 | Spotify OAuth | BLOCKED | 실 계정/환경변수 없음 |
| 11 | Spotify 내보내기 | BLOCKED | 미연결 401까지만 확인 |
| 12 | Spotify URL 표시 | NOT TESTED | 실 Export 결과 없음 |
| 13 | 공개 플레이리스트 참조 저장 | PASS | 다른 회원 Session + DB |
| 14 | 자동 큐레이션 조회 | PASS | 로그 생성 |
| 15 | 조회 직후 playlist 미생성 | PASS | 전후 소유 목록 비교 |
| 16 | 자동 큐레이션 저장 | PASS | 201 |
| 17 | USER + CURATION_COPY | PASS | DB 직접 조회 |
| 18 | 개인 목록/참조 저장 목록 구분 | PASS | 소유 0, 참조 저장 1인 두 번째 회원으로 확인 |

## 11. 실제 DB 반영 확인

직접 조회한 주요 행:

```text
music 13: SPOTIFY, codex-track-...
playlist 5: USER, MANUAL, PUBLIC
playlist 6: USER, CURATION_COPY, PRIVATE
recommendation_log 1: saved_playlist_id=6
playlist_save: member_id=6, playlist_id=5
review 5: 생성/수정 후 소유자 삭제, 최종 count=0
playlist_item 22: music_id=13, order_no=1
```

## 12. 실패 항목 및 원인

- 실 Spotify 검색/OAuth/Export: 환경변수와 실제 Spotify 계정 부재
- 브라우저 화면 클릭 기반 검증: Codex 인앱 브라우저 초기화 오류
- Spotify URL 표시: 실제 Export가 선행되지 않아 미검증

Mock 성공 응답이나 화면 전용 더미 데이터로 위 항목을 PASS 처리하지 않았다.

## 13. 병합 가능 여부

판정: **PASS WITH BLOCKERS**

근거:

- Backend 232건 전체 통과, 실제 MySQL Mapper 13건 포함
- Frontend production build 성공
- 실제 Session/Cookie/CORS 및 내부 MVP DB 흐름 통과
- P0 미해결 결함 없음
- Team B에서 발견한 P1 결함은 수정 및 회귀 테스트 완료
- 실 Spotify 외부 성공 경로와 브라우저 시각 검증은 환경 의존으로 남음
