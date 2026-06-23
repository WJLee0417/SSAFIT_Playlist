# 러닝 기능 브라우저 E2E 실행 가이드

## 1. 구성

E2E 도구는 Playwright를 사용한다. 기존 Frontend에는 Playwright, Cypress, Vitest 등
테스트 도구가 없었고, Playwright는 Vite dev server와 Spring Backend를 한 명령에서
기동하고 Chromium의 Session Cookie를 실제 브라우저와 동일하게 유지할 수 있어
최소 범위로 도입했다.

E2E는 다음 실제 구성요소를 사용한다.

```text
Chromium
  → Vue/Vite http://localhost:5173
  → Spring Boot http://localhost:8080
  → MySQL 8 전용 테스트 DB
```

API Mock, Mock Service Worker, 테스트용 Controller는 사용하지 않는다.

## 2. 사전 요구사항

- Java 25
- Maven Wrapper
- Node.js 및 npm
- MySQL 8 테스트 전용 DB
- Chromium 설치를 위한 최초 1회 네트워크 접근

Frontend 의존성 및 브라우저 설치:

```powershell
Set-Location .\frontend
npm install
npx playwright install chromium
```

## 3. 테스트 DB

DB 이름에는 반드시 `test`가 포함되어야 한다. 테스트 계정은 해당 DB에만 권한을
가져야 하며 root 계정을 사용하지 않는다.

```sql
CREATE DATABASE ssafit_e2e_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'ssafit_e2e'@'localhost'
    IDENTIFIED BY '<E2E_TEST_ONLY_PASSWORD>';

GRANT ALL PRIVILEGES ON ssafit_e2e_test.*
    TO 'ssafit_e2e'@'localhost';
```

PowerShell 환경변수:

```powershell
$env:SSAFIT_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:33306/ssafit_e2e_test?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true'
$env:SSAFIT_TEST_DB_USERNAME = 'ssafit_e2e'
$env:SSAFIT_TEST_DB_PASSWORD = '<E2E_TEST_ONLY_PASSWORD>'
```

`playwright.config.js`와 Spring E2E 환경 처리기가 DB 이름을 실행 전에 확인한다.
DB 이름에 `test`가 없으면 Backend나 브라우저 테스트를 시작하지 않는다.

## 4. E2E fixture

`e2e` Spring Profile은 Backend 시작 시 다음 순서로 전용 테스트 DB를 준비한다.

1. 알려진 테스트 테이블만 FK 역순 삭제
2. 운영 스키마 원본 `sql/schema.sql` 실행
3. `sql/e2e-seed.sql` 실행

재현 가능한 전용 계정:

| 역할 | 로그인 ID | 비밀번호 |
| --- | --- | --- |
| 작성자 | `e2e_host` | `e2e-password` |
| 참가자 | `e2e_participant` | `e2e-password` |
| 정원 채움 참가자 | `e2e_full_member` | `e2e-password` |

해시는 PBKDF2 형식으로 seed에 저장되며 실제 운영 계정이나 비밀번호를 사용하지 않는다.

## 5. Backend 단독 실행

```powershell
Set-Location .\ssafit
.\mvnw.cmd spring-boot:test-run "-Dspring-boot.run.profiles=e2e"
```

`spring-boot:test-run`을 사용하는 이유는 E2E Profile, fixture, 안전장치를
`src/test`에 격리하기 위해서다.

## 6. Frontend 단독 실행

별도 PowerShell:

```powershell
Set-Location .\frontend
npm run dev -- --host 127.0.0.1
```

Vue는 `http://localhost:5173`, Backend는 `http://localhost:8080`을 사용한다.
Backend CORS는 해당 Vue origin에 credential 요청을 허용하며 Axios는
`withCredentials: true`로 Session Cookie를 전송한다.

## 7. 자동 E2E 실행

Backend와 Frontend를 수동으로 실행하지 않은 상태에서 다음 명령을 사용한다.
Playwright가 두 서버를 시작하고 테스트 종료 시 정리한다.

```powershell
Set-Location .\frontend
npm run e2e
```

브라우저를 표시하려면:

```powershell
npm run e2e:headed
```

특정 파일만 실행:

```powershell
npx playwright test e2e/running-features.spec.js
```

## 8. 검증 시나리오

- 비로그인 보호 경로와 `/login-required`
- 인증 확인 API 네트워크 실패와 `/access-check-failed`
- JSP 로그인 및 실제 `JSESSIONID`
- 러닝 기록 등록, 상세, 수정, 삭제
- 모임 생성, 상세, 수정, 모집 마감/재개, 취소
- 비작성자의 수정/취소 버튼 비노출
- 참여, 참여 취소, 중복 참여 409
- CLOSED 참여 409 및 UI 참여 버튼 비노출
- 정원 초과 `SESSION_FULL`

## 9. 실패 확인 순서

1. 콘솔에서 실패한 테스트와 단계 확인
2. `frontend/test-results/<test-name>/error-context.md` 확인
3. 실패 시 보존되는 screenshot, video, trace 확인
4. `frontend/playwright-report/index.html` 확인
5. Backend 로그에서 HTTP status와 ErrorCode 확인
6. 테스트 DB의 `members`, `running_records`, `running_sessions`,
   `running_session_participants` 확인
7. `SSAFIT_TEST_DB_URL`의 DB 이름과 실제 `SELECT DATABASE()` 결과 확인

Trace 확인:

```powershell
npx playwright show-trace .\test-results\<test-name>\trace.zip
```
