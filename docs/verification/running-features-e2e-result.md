# 러닝 기능 브라우저 E2E 검증 결과

## 1. 실행 환경

- 실행일: 2026-06-23
- E2E: Playwright 1.61.0, Chromium
- Frontend: Vue 3, Vite 6.4.3
- Backend: Spring Boot 4.0.6
- Java: STS 5.1.1 내장 Eclipse Adoptium 25.0.2
- MySQL: 8.0.45
- DB: `ssafit_mapper_test`, 호스트 포트 `33306`
- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`

## 2. 도구 선정

기존 Frontend에는 E2E 도구가 없었다. Playwright를 선택한 이유:

- 실제 Chromium에서 Vue Router와 JSP 로그인 이동을 함께 검증 가능
- Browser Context별로 서로 다른 `JSESSIONID`를 유지해 작성자/참가자를 동시 재현
- Vite와 Spring Backend를 `webServer` 설정으로 자동 기동
- 실패 시 screenshot, video, trace, HTML report 제공

## 3. 구현 시나리오

### 인증과 권한

- 비로그인 `/running-records` 접근이 `/login-required`로 이동
- 실제 JSP 로그인 폼으로 로그인 후 Vue 보호 화면 복귀
- 인증 확인 API의 연결 실패 시 `/access-check-failed` 유지
- 참가자 계정에는 모임 수정 링크와 모임 취소 버튼이 노출되지 않음

### 러닝 기록

- 등록: 날짜, 거리, 시간, 메모 입력
- 상세 화면과 계산된 거리/메모 확인
- 수정 후 변경된 거리와 메모 확인
- 삭제 확인 대화상자 승인
- 목록 복귀 후 삭제된 기록 미노출
- 실제 DB에서는 `deleted_at`이 설정된 soft delete 행 확인

### 러닝 모임

- 작성자 계정으로 모임 생성
- 상세 진입과 작성자 제어 버튼 확인
- 제목 수정 및 수정 결과 확인
- 모집 마감과 재개
- 모임 취소 및 `CANCELLED` 화면 확인

### 참여

- 참가자 계정으로 OPEN 모임 참여
- 참여 취소
- 참여 후 실제 API 중복 요청이 `409 DUPLICATE_SESSION_JOIN`
- CLOSED 화면에서 참여 버튼 비노출
- CLOSED 모임 실제 API 참여 요청이 `409 SESSION_CLOSED`
- 정원 1명 모임을 다른 계정이 채운 뒤 UI 참여 시 `SESSION_FULL` 메시지 확인

## 4. 실제 실행 명령

```powershell
$env:JAVA_HOME = '<STS 5.1.1 JustJ Java 25 경로>'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:SSAFIT_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:33306/ssafit_mapper_test?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true'
$env:SSAFIT_TEST_DB_USERNAME = 'ssafit_test'
$env:SSAFIT_TEST_DB_PASSWORD = '<REDACTED>'

Set-Location .\frontend
npm run e2e
```

## 5. 실행 결과

```text
Running 3 tests using 1 worker

ok 비로그인 보호 경로와 인증 확인 실패 흐름
ok 러닝 기록 등록, 조회, 수정, 삭제
ok 러닝 모임 작성자 CRUD, 권한, 참여 예외 흐름

3 passed (30.3s)
```

Frontend 프로덕션 빌드:

```text
vite v6.4.3
127 modules transformed
BUILD SUCCESS
```

Backend 전체 회귀 테스트:

```text
Tests run: 259
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

실제 Backend와 Vue를 별도로 기동한 뒤 브라우저에서 JSP 로그인 화면,
`e2e_host` Session 로그인, `/running-records` 빈 목록 화면도 추가 확인했다.

## 6. DB 확인

E2E 종료 후 실제 MySQL에서 다음을 확인했다.

- fixture 회원 3명은 ACTIVE
- 테스트가 생성한 모임 2개는 모두 `CANCELLED`
- 러닝 기록 삭제는 활성 행 0, soft delete 행 1
- 테스트 종료 정리 후 참여 관계 0

DB는 E2E Backend가 시작될 때마다 schema reset과 fixture seed를 다시 수행하므로
반복 실행 간 데이터 의존성이 없다.

## 7. 변경 파일

- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/playwright.config.js`
- `frontend/e2e/running-features.spec.js`
- `.gitignore`
- `ssafit/src/test/java/com/ssafy/ssafit/e2e/E2eDatabaseSafetyEnvironmentPostProcessor.java`
- `ssafit/src/test/resources/META-INF/spring.factories`
- `ssafit/src/test/resources/application-e2e.yml`
- `ssafit/src/test/resources/sql/e2e-seed.sql`
- `docs/verification/running-features-e2e-guide.md`
- `docs/verification/running-features-e2e-result.md`

## 8. 남은 수동 검증

- 모바일·태블릿 반응형 레이아웃의 시각적 품질
- 실제 사용자 체감 기준의 키보드 탐색과 스크린리더 접근성
- 느린 네트워크에서 로딩 애니메이션과 중복 클릭 UX
- Safari/WebKit 및 Firefox 브라우저 호환성

핵심 러닝 CRUD, Session 인증, 작성자 권한, 참여 예외 규칙은 자동 E2E 범위에
포함됐다.
