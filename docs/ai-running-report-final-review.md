# AI 러닝 리포트 최종 코드 리뷰

## 점검 결과

| 점검 항목 | 결과 | 근거 |
| --- | --- | --- |
| 인증되지 않은 사용자의 API 접근 차단 | 통과 | `AiRunningReportApiController`가 `SessionMemberResolver.requireMemberId(session)`로 현재 로그인 회원을 요구한다. 미인증 테스트는 `AUTH_REQUIRED` 401을 검증한다. |
| 다른 회원 리포트 조회·생성 차단 | 통과 | GET/POST 모두 요청 파라미터로 회원 ID를 받지 않고 세션의 `memberId`만 사용한다. Mapper 조회 조건도 `member_id`, `report_type` 기준이다. |
| API Key 노출 방지 | 통과 | 실제 키는 코드와 `.env.example`에 포함되지 않는다. provider는 `Authorization` 헤더에만 사용하고 예외 메시지에 키를 포함하지 않는다. |
| LLM 호출 실패 전파 방지 | 통과 | timeout, API 오류, JSON 파싱 실패는 `AiRunningReportGenerationService`에서 catch 후 `FALLBACK` 리포트로 저장된다. |
| fallback 리포트 표시 구분 | 통과 | API 응답에 `generationSource`가 포함되고, Vue 카드는 `FALLBACK`일 때 "안전 요약" 배지와 안내 문구를 표시한다. |
| 데이터 부족 시 허위 해석 방지 | 통과 | 러닝 기록이 없으면 주요 통계는 `null`과 `NOT_AVAILABLE`/`INSUFFICIENT`로 표현되고, LLM 호출을 건너뛰어 fallback을 저장한다. |
| 최근 러닝 기록 최대 건수 제한 | 통과 | 기본 8개, 최대 10개로 정규화하며 Mapper SQL도 `LIMIT #{limit}`를 사용한다. |
| Mapper N+1 및 전체 조회 방지 | 통과 | 러닝 기록, 추천 로그, 러닝 프로필, 저장 플레이리스트를 각각 제한된 쿼리로 조회한다. 플레이리스트는 ID 목록 기반 단일 조회다. |
| 최신 리포트 조회 인덱스 | 통과 | `idx_ai_running_reports_member_type_created`, `idx_ai_running_reports_member_snapshot_status`가 최신 조회와 snapshot 재사용 조회를 지원한다. |
| 중복 생성 요청 방어 | 통과 | 회원별 in-memory lock과 snapshot 재사용 조회로 동일 JVM 내 중복 LLM 호출을 줄인다. |
| Vue 로딩·오류·빈 상태 처리 | 통과 | latest loading, no report, API error, login required, fallback, data insufficient 상태를 분기한다. |
| 기존 Spotify·러닝 CRUD·로그인 흐름 영향 | 수정 후 통과 | AI 기능은 별도 API/컴포넌트로 추가되어 기존 기능을 대체하지 않는다. 로그인 링크 하드코딩만 기존 유틸로 수정했다. |
| Docker AI 비활성 기동 | 통과 | `AI_RUNNING_REPORT_ENABLED=false`, `provider=none` 상태에서도 provider bean 없이 fallback 경로로 부팅 가능하다. Compose 컨테이너 healthy 확인 완료. |
| 테스트의 외부 LLM 의존성 | 통과 | 테스트 프로파일은 AI 비활성이고, 생성 서비스 테스트는 Fake Provider를 사용한다. |

## 발견된 문제

### P2. Vue 로그인 이동 URL 하드코딩

- 위치: `frontend/src/components/AiRunningReportCard.vue`
- 증상: 로그인 세션 만료 상태에서 `http://127.0.0.1:8080/login`으로 직접 이동했다.
- 영향: Docker, 포트 변경, 배포 환경에서 기존 JSP 로그인 URL 설정(`VITE_JSP_BASE_URL`)을 따르지 못할 수 있었다.
- 판단: 보안 취약점은 아니지만, 기존 로그인·로그아웃 흐름과 환경 변수 기반 설정을 깨는 일관성 문제다.

## 적용한 최소 수정

### Vue 로그인 링크를 기존 유틸로 통일

- `frontend/src/components/AiRunningReportCard.vue`
  - `buildJspLoginUrl(window.location.href)`를 사용하도록 수정했다.
  - 기존 `LoginRequiredView.vue`, `MusicDetailView.vue`, `PlaylistDetailView.vue`와 같은 로그인 이동 방식에 맞췄다.
  - API, DTO, DB, LLM 로직은 변경하지 않았다.

## 테스트 결과

### 통과

```bash
npm.cmd run build
```

- 결과: 성공
- 목적: Vue 카드 수정 후 프론트 빌드 검증

```bash
.\mvnw.cmd "-Dtest=AiRunningReportApiControllerTest,AiRunningReportGenerationServiceTest,AiRunningReportStorageServiceTest,AiRunningReportAggregationServiceTest" test
```

- 결과: 성공
- 결과 요약: `Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`
- 목적: 인증, API 응답, 캐시 재사용, fallback, Fake Provider, 집계, 저장 서비스 회귀 검증

```bash
docker compose ps
```

- 결과: 성공
- 상태: `mysql`, `backend`, `frontend` 모두 `healthy`
- 조건: AI 비활성 상태(`AI_RUNNING_REPORT_ENABLED=false`, `AI_RUNNING_REPORT_PROVIDER=none`)

### 실패 또는 보류

```bash
.\mvnw.cmd test
```

- 결과: 실패
- 결과 요약: `Tests run: 299, Failures: 0, Errors: 5, Skipped: 0`
- 실패 원인: `AiRunningReportMapperIntegrationTest`, `MapperIntegrationTest`가 전용 MySQL 테스트 DB 환경 변수 `SSAFIT_TEST_DB_URL`, `SSAFIT_TEST_DB_USERNAME`, `SSAFIT_TEST_DB_PASSWORD`를 요구한다.
- 재현 메시지: `Mapper integration tests require environment variable SSAFIT_TEST_DB_URL. Configure a dedicated MySQL database whose name contains 'test'; integration tests are not skipped.`
- 판단: AI 기능 로직 실패가 아니라, 기존 통합 테스트 안전장치에 필요한 로컬 테스트 DB 설정 누락이다.

## 병합 가능 여부

조건부 병합 가능.

AI 러닝 리포트 기능의 인증, 권한 격리, LLM fallback, 데이터 부족 처리, 캐시 재사용, Vue 상태 처리, Docker 비활성 기동은 점검 기준을 통과했다. 이번 리뷰에서 발견한 로그인 URL 하드코딩 문제도 최소 수정으로 해소했다.

다만 최종 병합 전에는 전용 MySQL 테스트 DB 환경을 준비한 뒤 기본 `mvn test` 또는 최소 Mapper 통합 테스트를 한 번 통과시켜야 한다. 현재 로컬 실패는 테스트 DB 환경 변수 누락으로 재현된다.

## 남은 운영상 주의사항

- 실제 운영 또는 시연 환경에는 `AI_RUNNING_REPORT_API_KEY`를 Git에 넣지 말고 서버 환경 변수, Docker `.env`, IDE run configuration, secret manager 중 하나로만 주입한다.
- `AI_RUNNING_REPORT_ENABLED=false` 또는 `provider=none`이면 LLM 호출 없이 fallback으로 동작한다. 시연에서 실제 AI 응답을 보여주려면 provider와 API Key를 별도로 설정해야 한다.
- 현재 중복 생성 방어는 단일 애플리케이션 인스턴스의 in-memory lock 기준이다. 다중 서버 운영에서는 DB unique key, distributed lock, job queue 중 하나를 추가 검토해야 한다.
- `input_snapshot_json`은 개인정보를 포함하지 않지만, 집계 통계와 저장된 플레이리스트 요약을 포함한다. 운영 로그에 이 JSON을 그대로 남기지 않는다.
- LLM 응답은 건강 진단이 아니라 운동 기록 참고 설명이다. UI와 프롬프트의 해당 제한 문구를 유지해야 한다.
- Mapper 통합 테스트는 실서비스 DB를 막기 위해 DB 이름에 `test`가 포함된 전용 MySQL만 사용해야 한다.
