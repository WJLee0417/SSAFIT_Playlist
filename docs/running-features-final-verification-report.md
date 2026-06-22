# 1. 구현 범위 요약

- 대상 브랜치: `feature/b-running-features-ui`
- 검증 대상: `runningrecord`, `runningsession`, 참여 기능의 Backend API와 Vue 화면 연동
- Backend Controller/DTO 컴파일, Validation, 인증/권한, 공통 예외 응답 계약을 확인했다.
- Frontend Router, API Client URL/HTTP Method, 목록·상세·등록·수정 화면, 로딩·빈 상태·오류 상태, 비로그인 처리, 권한별 버튼, 중복 클릭 방지를 확인했다.
- 전체 Backend 테스트와 Frontend 프로덕션 빌드를 실행했다.
- Swagger/OpenAPI는 프로젝트에 의존성과 설정이 없어 노출되지 않는다. 대신 `docs/integration/ssafit-api-tester.http`에 러닝 API 수동 검증 요청이 정의되어 있다.
- 기능 결함으로 인한 코드 수정은 수행하지 않았으며, 이 완료 보고서만 추가했다.

# 2. runningrecord API 목록

공통 성공 응답은 `{ "success", "message", "data" }`, 실패 응답은 `{ "code", "message" }` 형식이다. 모든 API는 Session 로그인이 필요하다.

| HTTP Method | 경로 | 기능 | 주요 검증 |
| --- | --- | --- | --- |
| GET | `/api/running-records/me` | 내 러닝 기록 목록 | Session 사용자 기준 조회, 내부 `memberId` 미노출 |
| POST | `/api/running-records` | 러닝 기록 등록 | `recordDate`, 양수 거리·시간, 메모 길이 Validation |
| GET | `/api/running-records/{runningRecordId}` | 내 러닝 기록 상세 | 존재 여부와 소유자 권한 검증 |
| PUT | `/api/running-records/{runningRecordId}` | 내 러닝 기록 수정 | Path ID 사용, 소유자 권한, Validation |
| DELETE | `/api/running-records/{runningRecordId}` | 내 러닝 기록 삭제 | 소유자 권한, soft delete 결과 검증 |

Response DTO 노출 필드는 `id`, `recordDate`, `distanceKm`, `durationSeconds`, `paceSecondsPerKm`, `memo`, `createdAt`, `updatedAt`이다. `memberId`, `deletedAt`은 노출하지 않는다.

# 3. runningsession API 목록

모든 API는 Session 로그인이 필요하다. 모임 수정·상태 변경·취소는 작성자만 가능하다.

| HTTP Method | 경로 | 기능 | 주요 검증 |
| --- | --- | --- | --- |
| GET | `/api/running-sessions` | 러닝 모임 목록 | 목록 DTO 반환 |
| POST | `/api/running-sessions` | 러닝 모임 생성 | 제목·장소·일시·정원 Validation, Session 사용자를 작성자로 지정 |
| GET | `/api/running-sessions/{runningSessionId}` | 모임 상세 | 모임, 참여 인원 수, 안전한 참여자 DTO 결합 |
| PUT | `/api/running-sessions/{runningSessionId}` | 작성자 모임 수정 | 작성자 권한, 취소 상태 제한, Validation |
| PATCH | `/api/running-sessions/{runningSessionId}/status` | 모집 상태 변경 | `OPEN`/`CLOSED`만 허용 |
| POST | `/api/running-sessions/{runningSessionId}/cancel` | 작성자 모임 취소 | 별도 DELETE를 가정하지 않고 Service 계약 사용 |
| GET | `/api/running-sessions/{runningSessionId}/participants` | 참여자 목록 | 회원 개인정보 대신 참여 DTO만 노출 |
| POST | `/api/running-sessions/{runningSessionId}/participants/me` | 현재 사용자 참여 | 중복 참여, 정원 초과, 모집 종료, 존재하지 않는 모임 검증 |
| DELETE | `/api/running-sessions/{runningSessionId}/participants/me` | 현재 사용자 참여 취소 | 현재 로그인 사용자의 참여만 취소 |

모임 Response DTO는 `hostMemberId`를 작성자 판별용으로 제공하지만 회원 이메일·로그인 ID 등 개인정보는 노출하지 않는다.

# 4. Vue 화면 및 Router 목록

| Router 경로 | 화면 | 인증 | 주요 동작 |
| --- | --- | --- | --- |
| `/running-records` | `RunningRecordListView.vue` | 필요 | 목록, 로딩, 빈 상태, 오류 상태 |
| `/running-records/new` | `RunningRecordFormView.vue` | 필요 | 등록, Client/Server Validation, 저장 중 비활성화 |
| `/running-records/:runningRecordId` | `RunningRecordDetailView.vue` | 필요 | 상세, 수정 이동, 삭제 |
| `/running-records/:runningRecordId/edit` | `RunningRecordFormView.vue` | 필요 | 기존 값 조회, 수정 |
| `/running-sessions` | `RunningSessionListView.vue` | 필요 | 목록, 로딩, 빈 상태, 오류 상태 |
| `/running-sessions/new` | `RunningSessionFormView.vue` | 필요 | 모임 생성, 저장 중 비활성화 |
| `/running-sessions/:runningSessionId` | `RunningSessionDetailView.vue` | 필요 | 상세, 참여/취소, 작성자 제어, 참여자 목록 |
| `/running-sessions/:runningSessionId/edit` | `RunningSessionFormView.vue` | 필요 | 작성자 확인 후 수정 |

- Router guard는 인증 API 성공 전 보호 화면 진입을 허용하지 않는다.
- `401 AUTH_REQUIRED`는 `/login-required?returnTo=...`로 이동한다.
- 인증 확인 API 자체가 실패하면 `/access-check-failed`로 이동해 로그인 상태를 임의로 추정하지 않는다.
- 상세 화면은 현재 회원 ID와 `hostMemberId`를 비교해 작성자 수정·취소·모집 상태 버튼을 노출한다.
- 참여 버튼은 이미 참여 중이거나 모집 상태가 `OPEN`이 아니면 숨긴다.
- 참여·취소·상태 변경·모임 취소는 공통 `actionBusy` 상태로 중복 클릭을 막는다.
- 등록·수정 버튼은 `saving` 상태로 중복 제출을 막는다.
- 정원 초과와 동시 참여 경합은 Backend 응답을 최종 기준으로 처리한다.

# 5. 테스트 결과

| 구분 | 검증 항목 | 결과 | 비고 |
| --- | --- | --- | --- |
| 브랜치 | 현재 브랜치 | 통과 | `feature/b-running-features-ui` |
| Backend | Java 25 컴파일 | 통과 | Temurin `25.0.3`으로 102개 main source 컴파일 |
| Backend | 전체 Maven 테스트 | 통과 | 257개 실행, 실패 0, 오류 0, 스킵 13 |
| Backend | 러닝 기록 Controller 테스트 | 통과 | 7개 |
| Backend | 러닝 기록 Service 테스트 | 통과 | 21개 |
| Backend | 러닝 모임 Controller 테스트 | 통과 | 9개 |
| Backend | 러닝 참여 Controller 테스트 | 통과 | 9개 |
| Backend | 러닝 모임 Service 테스트 | 통과 | 20개 |
| Backend | 러닝 참여 Service 테스트 | 통과 | 14개 |
| Backend | Request DTO Validation | 통과 | `@Valid`, `@NotNull`, `@NotBlank`, `@Positive`, 길이·숫자 제약 확인 |
| Backend | Response DTO 내부 필드 차단 | 통과 | 러닝 기록 `memberId/deletedAt` 미노출, 참여자 개인정보 미노출 테스트 존재 |
| Backend | 인증/권한/예외 계약 | 통과 | `AUTH_REQUIRED`, `ACCESS_DENIED`, NOT_FOUND, CONFLICT 시나리오 테스트 통과 |
| Backend | 기존 기능 회귀 | 통과 | 회원·음악·리뷰·플레이리스트·추천·Spotify 테스트 포함 전체 통과 |
| Database | 실제 Mapper 통합 테스트 | 미실행 | 테스트 DB 환경변수 미설정으로 13개 스킵 |
| Frontend | `npm run build` | 통과 | Vite 6.4.3, 127 modules transformed |
| Frontend | Router/API Client 계약 | 통과 | Controller 경로와 URL/HTTP Method 일치 |
| Frontend | 상태 화면 및 권한별 UI | 통과 | 코드 검증 및 빌드 산출물 렌더링 확인 |
| Frontend | Backend 미연결 인증 확인 실패 화면 | 통과 | 빌드 산출물에서 `UNKNOWN_ERROR`와 재시도 UI 렌더링 확인 |
| E2E | 로그인 후 실제 DB CRUD 브라우저 시나리오 | 미실행 | 실행 가능한 테스트 DB·로그인 fixture가 제공되지 않음 |
| API 문서 | Swagger/OpenAPI | 미구현 | 의존성·설정 없음 |
| API 문서 | HTTP API Tester | 통과 | 러닝 기록 5개, 러닝 모임/참여 9개 요청 정의 확인 |

Backend 실행 명령:

```powershell
$env:JAVA_HOME = '<JDK 25 경로>'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

Frontend 실행 명령:

```powershell
npm.cmd run build
```

# 6. 발견된 문제

## P0

- 기본 로컬 실행 환경이 Java 8을 가리키면 Backend가 `record`, 패턴 매칭, switch expression을 해석하지 못해 컴파일에 실패한다. 프로젝트는 `pom.xml` 기준 Java 25가 필요하다. Java 25를 지정하면 전체 테스트는 통과한다.

## P1

- 없음.

## P2

- Swagger/OpenAPI가 도입되어 있지 않아 `/v3/api-docs`, Swagger UI 및 스키마 기반 문서 검증을 수행할 수 없다. 현재 대체 수단은 `docs/integration/ssafit-api-tester.http`와 Controller 테스트다.
- 실제 MySQL 테스트 DB 환경변수가 없어 Mapper 통합 테스트 13개와 로그인 후 브라우저 CRUD 시나리오는 실행하지 못했다.

## P3

- Java 25의 향후 제한과 관련된 Mockito 동적 agent 로딩 경고와 Lombok `Unsafe` 경고가 출력된다. 현재 테스트 결과에는 영향이 없지만 의존성 업데이트 시 재확인이 필요하다.
- 모임 상세 화면은 정원 도달 여부를 Client에서 버튼 숨김 조건으로 사용하지 않고 Server의 `SESSION_FULL` 응답을 최종 기준으로 삼는다. 동시성 안전성에는 맞지만, 이미 정원이 찬 사실을 상세 응답으로 명확히 표시하는 UX 개선을 검토할 수 있다.

# 7. 변경 파일 목록

- `docs/running-features-final-verification-report.md`

검증 과정에서 기능 코드는 변경하지 않았다.

# 8. 브랜치 병합 전 확인 사항

- CI와 팀원 개발 환경의 `JAVA_HOME` 및 Maven 실행 Java가 25인지 확인한다.
- 테스트 전용 MySQL DB에서 Mapper 통합 테스트 13개를 실행한다. DB 이름에는 안전장치 요구사항에 따라 `test`가 포함되어야 한다.
- Backend와 Frontend를 동시에 실행하고 실제 로그인 Session으로 기록 CRUD, 모임 생성·수정·취소, 참여·중복 참여·정원 초과·모집 종료 시나리오를 최종 수동 확인한다.
- Swagger/OpenAPI를 병합 조건으로 요구한다면 별도 합의 후 의존성·설정·보안 정책을 추가한다. 현재 브랜치에는 Swagger가 없다.
- `docs/integration/ssafit-api-tester.http`의 ID 변수와 테스트 데이터를 실제 로컬 DB 값에 맞춰 실행한다.
- 회원, 로그인/로그아웃, 음악, 음악 리뷰, 플레이리스트, Spotify 기능은 전체 테스트가 통과했으며 러닝 검증 단계에서 추가 수정하지 않았다.
