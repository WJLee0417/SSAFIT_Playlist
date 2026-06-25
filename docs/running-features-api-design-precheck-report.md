# 러닝 기록·러닝 모임 API/UI 구현 사전점검 보고서

- 점검일: 2026-06-22
- 점검 브랜치: `feature/b-running-features-ui`
- 작업 범위: `runningrecord`, `runningsession`의 현재 구현 분석과 Controller/DTO/Vue/API 설계
- 코드 변경: 없음
- 산출물 변경: 본 보고서 1개 추가

# 1. 현재 구조 분석

## 1.1 Git 및 구현 상태

- 현재 브랜치는 요구사항과 동일한 `feature/b-running-features-ui`이다.
- 점검 시작 시 작업 트리는 깨끗했다.
- `runningrecord`, `runningsession`의 Domain, Mapper, Service와 단위 테스트는 구현되어 있다.
- 두 기능의 Controller, API DTO, Vue API 모듈, Vue 화면, Router 등록은 아직 없다.
- 이번 단계에서는 기존 Domain, Mapper, Service를 수정하지 않았다.

## 1.2 Backend 구조

```text
com.ssafy.ssafit
├─ common
│  ├─ auth
│  ├─ config
│  ├─ exception
│  └─ response
├─ runningrecord
│  ├─ domain
│  ├─ mapper
│  └─ service
└─ runningsession
   ├─ domain
   ├─ mapper
   └─ service
```

기존 REST 계층은 `<기능>/controller/api`, DTO는 `<기능>/dto`에 둔다. 성공 응답은 `ApiResponse<T>`, 오류 응답은 `ErrorResponse(code, message)`를 사용한다.

인증은 `JSESSIONID` 기반 `HttpSession` 방식이다. 일반 Controller는 `SessionMemberResolver`로 회원 ID를 얻지만, 러닝 기능 Service는 `HttpSession`을 직접 받고 내부에서 인증과 권한을 검증한다. 따라서 러닝 Controller는 Service 계약을 바꾸거나 회원 ID를 별도 주입하지 말고 현재 세션을 그대로 전달해야 한다.

`GlobalExceptionHandler`가 다음 계약을 제공한다.

```json
{
  "code": "ERROR_CODE",
  "message": "오류 메시지"
}
```

성공 응답은 다음 형식이다.

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

Swagger/OpenAPI 의존성과 설정은 없다. 현재 API 문서화·수동 검증 수단은 `docs/integration/ssafit-api-tester.http`이다.

## 1.3 Vue 구조

- Vue 3 + Vite + Vue Router + Axios 구성이다.
- Pinia는 설치·사용하지 않는다.
- 공통 API Client는 `frontend/src/api/http.js`이며 `withCredentials: true`로 세션 쿠키를 전송한다.
- 성공 응답의 `data`는 Axios interceptor가 자동으로 꺼내 반환한다.
- 오류는 `ApiError`로 변환하고 HTTP 상태와 `code`를 함께 사용한다.
- 인증 필요 Route는 `meta.requiresAuth`와 Router guard를 사용한다.
- 화면은 `AppLayout`, `PageStatePanel`, 공통 버튼·카드·폼 CSS를 재사용한다.

# 2. runningrecord Service 분석

## 2.1 Domain과 Mapper

`RunningRecord` 필드는 다음과 같다.

| 필드 | 타입 | 비고 |
| --- | --- | --- |
| `id` | `Long` | 기록 ID |
| `memberId` | `Long` | 소유 회원, Service가 세션으로 결정 |
| `recordDate` | `LocalDate` | 러닝 날짜 |
| `distanceKm` | `BigDecimal` | 0보다 커야 함 |
| `durationSeconds` | `Integer` | 0보다 커야 함 |
| `paceSecondsPerKm` | `Integer` | Service 자동 계산 |
| `memo` | `String` | 최대 1,000자 |
| `createdAt`, `updatedAt` | `LocalDateTime` | DB 기본값 |
| `deletedAt` | `LocalDateTime` | soft delete |

목록은 로그인 회원의 미삭제 기록만 `record_date DESC, id DESC`로 조회한다. 상세 조회도 `deleted_at IS NULL` 조건을 적용한다.

## 2.2 공개 메서드

| 메서드 | 입력 | 반환 | 인증·권한 | 주요 예외 |
| --- | --- | --- | --- | --- |
| `createRecord` | `HttpSession`, `RunningRecord` | 생성 기록 | 로그인 필수, `memberId`는 세션으로 덮어씀 | `AUTH_REQUIRED`, `MEMBER_NOT_FOUND`, `INVALID_LOGIN`, `INVALID_REQUEST` |
| `getMyRecordList` | `HttpSession` | `List<RunningRecord>` | 본인 기록만 조회 | 인증 계열 |
| `getMyRecordDetail` | `HttpSession`, 기록 ID | `RunningRecord` | 소유자만 조회 | `RUNNING_RECORD_NOT_FOUND`, `ACCESS_DENIED` |
| `updateMyRecord` | `HttpSession`, 변경 객체 | 수정 기록 | 소유자만 수정 | `RUNNING_RECORD_NOT_FOUND`, `ACCESS_DENIED`, `INVALID_REQUEST` |
| `deleteMyRecord` | `HttpSession`, 기록 ID | 없음 | 소유자만 soft delete | `RUNNING_RECORD_NOT_FOUND`, `ACCESS_DENIED` |

페이스는 `durationSeconds / distanceKm`를 초/km 단위로 반올림하여 계산한다. Request DTO에는 `memberId`, `paceSecondsPerKm`, 생성·수정·삭제 시각을 받지 않아야 한다.

미래 날짜 기록은 현재 Service에서 허용한다. Controller가 별도 정책을 임의로 추가하면 Service 계약과 달라지므로 금지 여부는 별도 합의가 필요하다.

# 3. runningsession Service 분석

## 3.1 모임 Domain과 상태

`RunningSession`은 주최자, 제목, 설명, 장소, 일정, 정원, 상태와 취소·생성·수정 시각을 가진다.

상태는 `OPEN`, `CLOSED`, `CANCELLED`이다.

- `OPEN`: 신규 참여 가능
- `CLOSED`: 신규 참여 불가
- `CANCELLED`: 모임 취소, 수정·상태 변경 불가

정원이 차도 상태가 자동으로 `CLOSED`가 되지 않는다. 주최자가 상태를 직접 변경해야 한다.

## 3.2 RunningSessionService 공개 메서드

| 메서드 | 입력 | 반환 | 인증·권한 | 주요 예외 |
| --- | --- | --- | --- | --- |
| `createSession` | `HttpSession`, `RunningSession` | 생성 모임 | 로그인 필수, 주최자와 `OPEN` 상태를 Service가 설정 | `AUTH_REQUIRED`, `INVALID_REQUEST` |
| `getSessionList` | `HttpSession` | 전체 모임 목록 | 로그인 필수 | 인증 계열 |
| `getSessionDetail` | `HttpSession`, 모임 ID | 모임 | 로그인 회원이면 조회 가능 | `SESSION_NOT_FOUND` |
| `updateMySession` | `HttpSession`, 변경 객체 | 수정 모임 | 주최자만, 취소 모임 불가 | `ACCESS_DENIED`, `SESSION_CLOSED`, `INVALID_REQUEST` |
| `changeMySessionStatus` | `HttpSession`, 모임 ID, 상태 | 상태 변경 모임 | 주최자만, `OPEN`·`CLOSED`만 허용 | `ACCESS_DENIED`, `SESSION_CLOSED`, `INVALID_REQUEST` |
| `cancelMySession` | `HttpSession`, 모임 ID | 취소 모임 | 주최자만, 재취소 불가 | `ACCESS_DENIED`, `SESSION_CLOSED` |

제목은 공백 불가·최대 200자, 장소는 공백 불가·최대 255자, 설명은 최대 2,000자, 정원은 1 이상이다. 일정이 과거인지 여부는 검증하지 않는다.

## 3.3 RunningSessionParticipantService 공개 메서드

| 메서드 | 입력 | 반환 | 정책 | 주요 예외 |
| --- | --- | --- | --- | --- |
| `joinSession` | `HttpSession`, 모임 ID | 참여 관계 | `OPEN`만 가능, 중복·정원 초과 방지, 행 잠금 사용 | `SESSION_NOT_FOUND`, `SESSION_CLOSED`, `SESSION_FULL`, `DUPLICATE_SESSION_JOIN` |
| `cancelMyParticipation` | `HttpSession`, 모임 ID | 없음 | 본인 참여 관계만 물리 삭제, CLOSED/CANCELLED에서도 취소 가능 | `SESSION_NOT_FOUND`, `ACCESS_DENIED` |
| `getSessionParticipants` | `HttpSession`, 모임 ID | 참여자 목록 | 로그인 필수 | `SESSION_NOT_FOUND` |
| `getParticipantCount` | `HttpSession`, 모임 ID | 현재 참여 인원 | 로그인 필수 | `SESSION_NOT_FOUND` |

주최자는 모임 생성만으로 자동 참여하지 않는다. 원하면 일반 회원과 동일하게 참여 API를 호출해야 한다.

# 4. Controller/DTO 구현 계획

## 4.1 Controller

다음 세 Controller가 현재 패키지 규칙에 맞다.

```text
runningrecord/controller/api/RunningRecordApiController
runningsession/controller/api/RunningSessionApiController
runningsession/controller/api/RunningSessionParticipantApiController
```

Controller 책임은 `@Valid` Request binding, 경로 ID와 DTO 조합, `HttpSession` 전달, Response DTO 변환, 성공 HTTP 상태 지정으로 제한한다. Service가 수행하는 인증·소유권·상태 검증을 Controller에서 복제하지 않는다.

## 4.2 DTO

### RunningRecord DTO

- `CreateRequest(recordDate, distanceKm, durationSeconds, memo)`
- `UpdateRequest(recordDate, distanceKm, durationSeconds, memo)`
- `Response(id, recordDate, distanceKm, durationSeconds, paceSecondsPerKm, memo, createdAt, updatedAt)`

본인 전용 기능이므로 응답의 `memberId`는 필수 정보가 아니다. Request에는 `memberId`, `paceSecondsPerKm`, 타임스탬프를 포함하지 않는다.

### RunningSession DTO

- `CreateRequest(title, description, meetingPlace, scheduledAt, capacity)`
- `UpdateRequest(title, description, meetingPlace, scheduledAt, capacity)`
- `StatusUpdateRequest(status)` — `OPEN`, `CLOSED`만 허용
- `SummaryResponse(id, hostMemberId, title, meetingPlace, scheduledAt, capacity, status, cancelledAt)`
- `DetailResponse(session, participantCount, participants, currentMemberParticipating)`
- `ParticipantResponse(id, sessionId, memberId, joinedAt)`

`hostMemberId`, `status`, `cancelledAt`은 생성·수정 DTO에서 받지 않는다. 참여 요청도 `memberId`를 받지 않는다.

## 4.3 응답 조합 원칙

- 모임 상세에서는 `getSessionDetail`, `getParticipantCount`, `getSessionParticipants`를 조합할 수 있다.
- `currentMemberParticipating`은 상세 참여자 목록에 현재 세션 회원 ID가 있는지 비교하여 계산할 수 있다.
- 모임 목록마다 참여 인원과 내 참여 여부를 개별 조회하면 N+1 Service/SQL 호출이 생긴다. 초기 구현에서는 목록 응답을 기본 모임 정보로 제한하고, 상세에서만 참여 정보를 조합하는 방식을 권장한다.
- 참여자 닉네임을 직접 제공하는 러닝 Service 계약은 없다. 첫 구현은 `memberId`만 표시하고, 닉네임이 필수라면 회원 조회 계약을 안전하게 확정한 뒤 추가해야 한다.

# 5. Vue 구현 계획

## 5.1 API 모듈

```text
frontend/src/api/runningRecordApi.js
frontend/src/api/runningSessionApi.js
```

기존 `apiClient`를 사용하며 별도 Axios 인스턴스나 인증 방식을 만들지 않는다.

## 5.2 화면과 Route

| 화면 | 제안 Route | 인증 | 주요 기능 |
| --- | --- | --- | --- |
| 러닝 기록 목록 | `/running-records` | 필수 | 본인 기록 목록, 등록 진입 |
| 러닝 기록 등록 | `/running-records/new` | 필수 | 날짜·거리·시간·메모 입력 |
| 러닝 기록 상세 | `/running-records/:recordId` | 필수 | 상세, 수정·삭제 |
| 러닝 기록 수정 | `/running-records/:recordId/edit` | 필수 | 기존 값 수정 |
| 러닝 모임 목록 | `/running-sessions` | 필수 | 전체 상태의 모임 목록, 생성 진입 |
| 러닝 모임 생성 | `/running-sessions/new` | 필수 | 모임 정보 입력 |
| 러닝 모임 상세 | `/running-sessions/:sessionId` | 필수 | 참여자·인원, 참여/취소, 주최자 작업 |
| 러닝 모임 수정 | `/running-sessions/:sessionId/edit` | 필수 | 주최자만 수정 |

모임 참여·참여 취소·모집 상태 변경·모임 취소는 별도 화면보다 상세 화면의 작업 버튼으로 제공하는 편이 현재 UI 구조와 기능 규모에 맞다.

## 5.3 UI 상태 처리

- `PageStatePanel`로 loading, empty, loginRequired, unauthorized, error 상태를 처리한다.
- `AUTH_REQUIRED`는 기존 로그인 필요 화면 흐름을 사용한다.
- `ACCESS_DENIED`는 권한 없음 안내를 표시한다.
- `SESSION_CLOSED`, `SESSION_FULL`, `DUPLICATE_SESSION_JOIN`은 상세 화면의 작업 메시지로 표시한 뒤 상세 데이터를 다시 조회한다.
- 삭제·모임 취소·참여 취소는 `window.confirm` 또는 기존 프로젝트와 동일한 확인 UI를 사용한다.
- Pinia는 도입하지 않고 View 내부 상태와 기존 API 모듈 패턴을 따른다.
- `AppLayout` 메뉴에는 러닝 기록과 러닝 모임 진입 링크를 추가하되 다른 기능의 화면과 동작은 변경하지 않는다.

# 6. 예상 API 목록

아래 URI는 현재 코드에 고정된 명세가 없으므로 구현 전 확정이 필요한 제안안이다.

| 목적 | Method | URL | Request DTO | Response DTO | 인증 | Vue 화면 |
| --- | --- | --- | --- | --- | --- | --- |
| 내 기록 목록 | `GET` | `/api/running-records/me` | 없음 | `List<RunningRecord.Response>` | 필수 | 기록 목록 |
| 기록 생성 | `POST` | `/api/running-records` | `CreateRequest` | `RunningRecord.Response` | 필수 | 기록 등록 |
| 내 기록 상세 | `GET` | `/api/running-records/{recordId}` | 없음 | `RunningRecord.Response` | 필수 | 기록 상세·수정 |
| 내 기록 수정 | `PUT` | `/api/running-records/{recordId}` | `UpdateRequest` | `RunningRecord.Response` | 필수 | 기록 수정 |
| 내 기록 삭제 | `DELETE` | `/api/running-records/{recordId}` | 없음 | 없음 | 필수 | 기록 상세 |
| 모임 목록 | `GET` | `/api/running-sessions` | 없음 | `List<SummaryResponse>` | 필수 | 모임 목록 |
| 모임 생성 | `POST` | `/api/running-sessions` | `CreateRequest` | `SummaryResponse` | 필수 | 모임 생성 |
| 모임 상세 | `GET` | `/api/running-sessions/{sessionId}` | 없음 | `DetailResponse` | 필수 | 모임 상세 |
| 내 모임 수정 | `PUT` | `/api/running-sessions/{sessionId}` | `UpdateRequest` | `SummaryResponse` | 필수 | 모임 수정 |
| 모집 상태 변경 | `PATCH` | `/api/running-sessions/{sessionId}/status` | `StatusUpdateRequest` | `SummaryResponse` | 필수 | 모임 상세 |
| 내 모임 취소 | `POST` | `/api/running-sessions/{sessionId}/cancel` | 없음 | `SummaryResponse` | 필수 | 모임 상세 |
| 모임 참여 | `POST` | `/api/running-sessions/{sessionId}/participants/me` | 없음 | `ParticipantResponse` | 필수 | 모임 상세 |
| 내 참여 취소 | `DELETE` | `/api/running-sessions/{sessionId}/participants/me` | 없음 | 없음 | 필수 | 모임 상세 |
| 참여자 목록 | `GET` | `/api/running-sessions/{sessionId}/participants` | 없음 | `List<ParticipantResponse>` | 필수 | 모임 상세 |

생성 API는 기존 Controller 관례대로 HTTP 201을 권장한다. 삭제와 참여 취소는 기존 `ApiResponse<Void>` 성공 메시지 형식을 유지한다.

# 7. 위험 요소 및 확인 필요 사항

1. **생성·수정 직후 DB 시각 누락**
   - insert/update 반환 Domain에는 DB 기본값인 `createdAt`, `updatedAt`이 즉시 채워지지 않을 수 있다.
   - 모임 취소 반환 객체도 DB에서 설정한 `cancelledAt`이 갱신되지 않는다.
   - Controller가 변경 후 상세 Service를 재호출할지, 변경 응답에서 해당 시각을 제외할지 계약 확정이 필요하다.

2. **참여 생성 시각 누락**
   - `joinSession` 반환 객체의 `joinedAt`은 DB 기본값이라 즉시 채워지지 않을 수 있다.
   - 정확한 생성 응답이 필요하면 Service에서 참여 관계를 다시 조회하는 최소 변경이 필요할 수 있다.

3. **목록 N+1**
   - 모임 목록에 `participantCount`, 참여자, 내 참여 여부, 주최자 닉네임을 모두 넣으면 모임 수만큼 추가 조회가 발생한다.
   - 현재 Service만 유지하는 1차 구현에서는 목록을 기본 정보로 제한하는 것이 안전하다.

4. **닉네임 계약 부족**
   - 모임·참여 Domain은 회원 ID만 가진다.
   - 닉네임 노출이 필요하면 비밀번호 해시와 이메일 등을 노출하지 않는 전용 응답 조합이 필요하다.

5. **정원 축소 정책**
   - 주최자는 현재 참여 인원보다 작은 값으로 정원을 수정할 수 있다. Service에 방지 로직이 없다.
   - 허용 여부를 제품 정책으로 확정해야 한다.

6. **날짜 정책**
   - 미래의 러닝 기록과 과거의 모임 일정이 현재 Service에서 허용된다.
   - Controller/DTO에서 임의로 금지하지 말고 정책 합의 후 Service와 함께 일치시켜야 한다.

7. **주최자 참여 정책**
   - 주최자는 자동 참여자가 아니다. 주최자도 정원에 포함할지, 별도 참여가 필요한지 UI 문구를 확정해야 한다.

8. **취소 모임의 참여 관계**
   - 모임을 취소해도 기존 참여 관계는 남는다.
   - 취소된 모임에서도 본인 참여 취소는 가능하다.

9. **목록 필터·페이지 기능 없음**
   - 기록과 모임 목록 Service는 전체 `List`만 반환한다.
   - 페이지, 상태 필터, 기간 검색을 UI에 먼저 만들면 새 Mapper/Service 계약이 필요하므로 1차 범위에서 제외한다.

10. **Swagger 미도입**
    - 현재 프로젝트에는 Swagger/OpenAPI가 없다.
    - 새 라이브러리를 임의 도입하지 말고 기존 HTTP Tester에 러닝 API 사례를 추가하는 것이 현재 구조에 맞다.

11. **백엔드 실행 JDK 불일치**
    - `pom.xml`은 Java 25를 요구한다.
    - 현재 명령 실행 환경은 Java 17이며, Java 25로 컴파일된 기존 테스트 클래스를 로드하지 못해 테스트가 시작되지 않았다.

# 8. 다음 구현 단계 권장 순서

1. 제안 URI와 응답 필드, 특히 생성 직후 타임스탬프 처리 방식을 확정한다.
2. `RunningRecordApiDtos`와 `RunningRecordApiController`를 구현하고 Controller 테스트를 추가한다.
3. `runningRecordApi.js`, 기록 목록·등록·상세·수정 화면과 Route를 구현한다.
4. `RunningSessionApiDtos`, 모임 Controller, 참여 Controller를 구현한다.
5. 모임 상세 응답에서 참여 인원·참여자·내 참여 여부를 조합하고 Controller 테스트를 추가한다.
6. `runningSessionApi.js`, 모임 목록·생성·상세·수정 화면과 Route를 구현한다.
7. `AppLayout` 메뉴와 공통 오류 메시지를 연결한다.
8. `docs/integration/ssafit-api-tester.http`에 러닝 API 정상·401·403·404·409 사례를 추가한다.
9. Java 25 환경에서 백엔드 전체 테스트를 실행하고, 프런트 production build와 수동 세션 연동을 검증한다.

## 검증 결과

- Frontend: `npm.cmd run build` 성공, Vite에서 117개 모듈 변환 완료
- Backend: `.\mvnw.cmd test` 실행 실패
  - 테스트 실행 수: 0
  - 원인: Java 25 class file version 69를 Java 17 runtime이 읽지 못함
  - 코드 컴파일·테스트 실패로 판단할 수 없으며 Java 25 환경에서 재검증 필요

## 변경 파일

- `docs/running-features-api-design-precheck-report.md`

## 이번 단계 구현 API

- 없음. 분석·설계 보고서만 작성했다.
