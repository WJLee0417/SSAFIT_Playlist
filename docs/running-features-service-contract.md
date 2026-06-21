# 러닝 추가 기능 Service 연동 계약

## 1. 문서 목적

이 문서는 `feature/a-running-features-service` 브랜치에 구현된 러닝 기록, 러닝 모임, 모임 참여 기능을 팀원 B가 Controller, DTO, Vue 화면과 연결할 수 있도록 실제 Service 시그니처와 정책을 정리한다.

기준일은 2026-06-21이며, 문서에 적힌 내용은 현재 브랜치의 구현을 기준으로 한다.

팀원 B는 이 문서를 바탕으로 API 계층을 구현하되 Domain 객체를 그대로 응답으로 노출하기보다 Request/Response DTO로 변환해야 한다.

## 2. 공통 연동 원칙

### 2.1 인증

모든 러닝 기능 Service 공개 메서드는 `HttpSession`을 받으며 로그인 인증이 필요하다.

Service 내부 인증 흐름:

```java
Member loginMember = authService.getLoginMember(session);
Long loginMemberId = loginMember.getId();
```

Controller는 클라이언트가 전달한 `memberId`, `hostMemberId`를 로그인 사용자 식별값으로 신뢰하면 안 된다. Service가 Session 로그인 회원 ID로 소유자와 생성자를 결정한다.

### 2.2 예외 응답

Service는 `CustomException(ErrorCode)`을 발생시킨다. 기존 `GlobalExceptionHandler`가 다음 정보로 HTTP 응답을 만든다.

```json
{
  "code": "ERROR_CODE_NAME",
  "message": "오류 메시지"
}
```

Controller에서 동일 예외를 임의의 다른 HTTP 상태나 코드로 다시 변환하지 않는 것을 권장한다.

### 2.3 실제 입력값 오류 코드

요구 문서에 언급된 `INVALID_INPUT`은 현재 프로젝트의 `ErrorCode`에 존재하지 않는다.

현재 구현은 입력값 오류에 아래 코드를 사용한다.

```text
INVALID_REQUEST
```

팀원 B는 Controller와 DTO에서 `INVALID_INPUT`을 참조하지 말고 `INVALID_REQUEST`를 기준으로 연동해야 한다.

## 3. 러닝 기록 기능

### 3.1 Domain 핵심 필드

`RunningRecord`

| 필드 | 타입 | 의미 |
|---|---|---|
| `id` | `Long` | 기록 ID |
| `memberId` | `Long` | 기록 소유 회원 ID |
| `recordDate` | `LocalDate` | 러닝 날짜 |
| `distanceKm` | `BigDecimal` | 거리(km) |
| `durationSeconds` | `Integer` | 러닝 시간(초) |
| `paceSecondsPerKm` | `Integer` | 1km당 페이스(초) |
| `memo` | `String` | 메모 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |
| `deletedAt` | `LocalDateTime` | soft delete 시각 |

### 3.2 페이스 정책

`paceSecondsPerKm`는 외부 입력값을 사용하지 않는다.

Service 계산식:

```text
durationSeconds / distanceKm
```

계산 결과는 초 단위 정수로 반올림(`RoundingMode.HALF_UP`)하여 저장한다.

Request DTO에는 `paceSecondsPerKm`를 입력 필드로 두지 않는 것을 권장한다. Response DTO에서는 Service가 계산한 값을 조회 결과로 제공한다.

### 3.3 기록 생성

Service:

```java
RunningRecord createRecord(
        HttpSession session,
        RunningRecord runningRecord)
```

필수 입력:

- `recordDate`
- `distanceKm`
- `durationSeconds`

선택 입력:

- `memo`

Service가 무시하거나 재설정하는 필드:

- `id`
- `memberId`
- `paceSecondsPerKm`
- `createdAt`
- `updatedAt`
- `deletedAt`

검증:

- `recordDate != null`
- `distanceKm > 0`
- `durationSeconds > 0`
- `memo` 최대 1,000자
- 미래 날짜 금지 정책은 없음

반환 핵심 데이터:

- 생성된 `id`
- Session 회원 ID가 반영된 `memberId`
- 입력한 기록 데이터
- 계산된 `paceSecondsPerKm`

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`
- `INVALID_REQUEST`

### 3.4 내 기록 목록 조회

Service:

```java
List<RunningRecord> getMyRecordList(HttpSession session)
```

입력:

- `HttpSession`

조회 정책:

- Session 로그인 회원의 기록만 조회
- `deleted_at IS NULL`인 기록만 조회
- `recordDate DESC`, `id DESC` 정렬

반환 핵심 데이터:

- 기록 ID
- 기록 날짜
- 거리
- 운동 시간
- 계산된 페이스
- 메모
- 생성·수정 시각

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`

### 3.5 내 기록 상세 조회

Service:

```java
RunningRecord getMyRecordDetail(
        HttpSession session,
        Long runningRecordId)
```

입력:

- `runningRecordId`

권한:

- DB에서 조회한 기록의 `memberId`와 Session 회원 ID가 일치해야 함

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`
- `RUNNING_RECORD_NOT_FOUND`
- `ACCESS_DENIED`

### 3.6 내 기록 수정

Service:

```java
RunningRecord updateMyRecord(
        HttpSession session,
        RunningRecord runningRecord)
```

필수 입력:

- `id`
- `recordDate`
- `distanceKm`
- `durationSeconds`

선택 입력:

- `memo`

Service 처리:

- 기존 기록을 먼저 조회
- DB 원본의 `memberId`로 소유권 검증
- 외부 `memberId` 무시
- 페이스 재계산
- 기존 `createdAt`, `deletedAt` 유지

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`
- `RUNNING_RECORD_NOT_FOUND`
- `ACCESS_DENIED`
- `INVALID_REQUEST`

### 3.7 내 기록 삭제

Service:

```java
void deleteMyRecord(
        HttpSession session,
        Long runningRecordId)
```

삭제 정책:

- 물리 삭제가 아닌 soft delete
- `deleted_at = CURRENT_TIMESTAMP(6)`
- 이후 목록 및 상세 조회에서 제외

권한:

- 기록 소유 회원만 삭제 가능

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`
- `RUNNING_RECORD_NOT_FOUND`
- `ACCESS_DENIED`

## 4. 러닝 모임 기능

### 4.1 Domain 핵심 필드

`RunningSession`

| 필드 | 타입 | 의미 |
|---|---|---|
| `id` | `Long` | 모임 ID |
| `hostMemberId` | `Long` | 생성자 회원 ID |
| `title` | `String` | 모임 제목 |
| `description` | `String` | 설명 |
| `meetingPlace` | `String` | 집결 장소 |
| `scheduledAt` | `LocalDateTime` | 모임 일시 |
| `capacity` | `Integer` | 정원 |
| `status` | `RunningSessionStatus` | 모집 상태 |
| `cancelledAt` | `LocalDateTime` | 취소 시각 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |

상태:

```text
OPEN
CLOSED
CANCELLED
```

상태 의미:

- `OPEN`: 신규 참여 가능
- `CLOSED`: 생성자가 모집을 닫음
- `CANCELLED`: 취소된 모임

정원이 찼다고 자동으로 `CLOSED`가 되지는 않는다.

### 4.2 모임 생성

Service:

```java
RunningSession createSession(
        HttpSession session,
        RunningSession runningSession)
```

필수 입력:

- `title`
- `meetingPlace`
- `scheduledAt`
- `capacity`

선택 입력:

- `description`

길이와 값 검증:

- `title`: 공백 불가, trim 후 최대 200자
- `meetingPlace`: 공백 불가, trim 후 최대 255자
- `description`: 최대 2,000자
- `scheduledAt`: 필수
- `capacity > 0`
- 반드시 미래 일정이어야 한다는 정책은 없음

Service가 재설정하는 필드:

- `id = null`
- `hostMemberId = Session 회원 ID`
- `status = OPEN`
- `cancelledAt = null`
- 생성·수정 시각

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`
- `INVALID_REQUEST`

### 4.3 모임 목록 조회

Service:

```java
List<RunningSession> getSessionList(HttpSession session)
```

조회 정책:

- 로그인 필수
- `OPEN`, `CLOSED`, `CANCELLED`를 모두 반환
- `scheduledAt ASC`, `id ASC` 정렬
- 현재 필터와 페이징 기능 없음

반환 핵심 데이터:

- `id`
- `hostMemberId`
- `title`
- `description`
- `meetingPlace`
- `scheduledAt`
- `capacity`
- `status`
- `cancelledAt`
- 생성·수정 시각

참여자 수는 `RunningSession` 목록 결과에 포함되지 않는다. 화면에서 필요하면 `RunningSessionParticipantService.getParticipantCount` 결과를 별도로 조합해야 한다.

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`

### 4.4 모임 상세 조회

Service:

```java
RunningSession getSessionDetail(
        HttpSession session,
        Long sessionId)
```

정책:

- 로그인 필수
- 생성자가 아닌 회원도 조회 가능
- 취소된 모임도 조회 가능

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`
- `SESSION_NOT_FOUND`

### 4.5 모임 수정

Service:

```java
RunningSession updateMySession(
        HttpSession session,
        RunningSession changes)
```

필수 입력:

- `id`
- `title`
- `meetingPlace`
- `scheduledAt`
- `capacity`

선택 입력:

- `description`

권한과 상태:

- 생성자만 수정 가능
- `OPEN`, `CLOSED` 상태는 수정 가능
- `CANCELLED` 상태는 수정 불가
- 외부 `hostMemberId`, `status`, `cancelledAt`은 반영하지 않음

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`
- `SESSION_NOT_FOUND`
- `ACCESS_DENIED`
- `SESSION_CLOSED`
- `INVALID_REQUEST`

### 4.6 모집 상태 변경

Service:

```java
RunningSession changeMySessionStatus(
        HttpSession session,
        Long sessionId,
        RunningSessionStatus status)
```

허용 상태:

- `OPEN`
- `CLOSED`

정책:

- 생성자만 변경 가능
- `CANCELLED` 전환은 이 메서드에서 허용하지 않음
- 이미 취소된 모임은 상태 변경 불가
- 정원 도달에 따른 자동 상태 변경 없음

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`
- `SESSION_NOT_FOUND`
- `ACCESS_DENIED`
- `SESSION_CLOSED`
- `INVALID_REQUEST`

### 4.7 모임 취소

Service:

```java
RunningSession cancelMySession(
        HttpSession session,
        Long sessionId)
```

정책:

- 생성자만 취소 가능
- `OPEN`, `CLOSED`에서 취소 가능
- 취소 시 DB 상태를 `CANCELLED`로 변경
- DB의 `cancelled_at`을 현재 시각으로 설정
- 재취소 불가
- 참여 관계는 모임 취소 시 자동 삭제하지 않음

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`
- `SESSION_NOT_FOUND`
- `ACCESS_DENIED`
- `SESSION_CLOSED`

## 5. 러닝 모임 참여 기능

### 5.1 Domain 핵심 필드

`RunningSessionParticipant`

| 필드 | 타입 | 의미 |
|---|---|---|
| `id` | `Long` | 참여 관계 ID |
| `sessionId` | `Long` | 모임 ID |
| `memberId` | `Long` | 참여 회원 ID |
| `joinedAt` | `LocalDateTime` | 참여 시각 |

### 5.2 모임 참여

Service:

```java
RunningSessionParticipant joinSession(
        HttpSession session,
        Long sessionId)
```

트랜잭션 처리 순서:

1. Session 로그인 회원 확인
2. `selectByIdForUpdate(sessionId)`로 모임 행 잠금
3. 모임 존재 여부 확인
4. `status == OPEN` 검증
5. 동일 회원의 중복 참여 확인
6. 현재 참여자 수 조회
7. `현재 참여자 수 < capacity` 검증
8. 참여 관계 insert
9. 트랜잭션 종료

정책:

- 생성자도 일반 회원처럼 참여 가능
- `CLOSED`, `CANCELLED` 모임 참여 불가
- 정원 도달 시 상태를 자동으로 `CLOSED`로 변경하지 않음
- Service 사전 검증과 DB UNIQUE 제약조건을 함께 사용
- UNIQUE 경합은 `DUPLICATE_SESSION_JOIN`으로 변환

반환 핵심 데이터:

- 참여 관계 `id`
- `sessionId`
- 로그인 회원의 `memberId`
- `joinedAt`

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`
- `SESSION_NOT_FOUND`
- `SESSION_CLOSED`
- `SESSION_FULL`
- `DUPLICATE_SESSION_JOIN`

### 5.3 내 참여 취소

Service:

```java
void cancelMyParticipation(
        HttpSession session,
        Long sessionId)
```

정책:

- 회원 ID를 외부 입력으로 받지 않음
- `(sessionId, Session 회원 ID)` 관계만 삭제
- 물리 삭제
- 다른 회원의 참여 관계를 삭제할 수 없음
- 자신의 참여 관계가 없으면 `ACCESS_DENIED`
- 참여 취소 후 같은 OPEN 모임에 재참여 가능
- 모임이 `CLOSED` 또는 `CANCELLED`여도 기존 참여 관계 취소는 가능

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`
- `SESSION_NOT_FOUND`
- `ACCESS_DENIED`

### 5.4 참여자 목록

Service:

```java
List<RunningSessionParticipant> getSessionParticipants(
        HttpSession session,
        Long sessionId)
```

정책:

- 로그인 필수
- 모임 존재 여부 검증
- `joinedAt ASC`, `id ASC` 정렬

반환 핵심 데이터:

- 참여 관계 ID
- 모임 ID
- 회원 ID
- 참여 시각

현재 Service는 회원 닉네임을 조인하지 않는다. 참여자 표시명은 기존 회원 조회 기능과 안전한 Response DTO 조합이 필요하며, `Member.passwordHash`를 응답에 포함하면 안 된다.

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`
- `SESSION_NOT_FOUND`

### 5.5 현재 참여자 수

Service:

```java
int getParticipantCount(
        HttpSession session,
        Long sessionId)
```

정책:

- 로그인 필수
- 모임 존재 여부 검증
- 현재 관계 행 개수를 반환

발생 가능한 ErrorCode:

- `AUTH_REQUIRED`
- `MEMBER_NOT_FOUND`
- `INVALID_LOGIN`
- `SESSION_NOT_FOUND`

## 6. ErrorCode 계약

| ErrorCode | HTTP 상태 | 적용 상황 |
|---|---:|---|
| `AUTH_REQUIRED` | 401 | Session 로그인 정보 없음 |
| `INVALID_LOGIN` | 401 | 로그인 회원이 활성 상태가 아님 |
| `ACCESS_DENIED` | 403 | 타인 기록·모임 변경 또는 본인 참여 관계 없음 |
| `MEMBER_NOT_FOUND` | 404 | Session 회원이 DB에 없음 |
| `RUNNING_RECORD_NOT_FOUND` | 404 | 기록 없음 또는 soft delete됨 |
| `SESSION_NOT_FOUND` | 404 | 모임 없음 |
| `INVALID_REQUEST` | 400 | 필수값·길이·양수·허용 상태 검증 실패 |
| `SESSION_CLOSED` | 409 | CLOSED/CANCELLED 참여 또는 취소 상태 변경 시도 |
| `SESSION_FULL` | 409 | 현재 참여자 수가 정원 이상 |
| `DUPLICATE_SESSION_JOIN` | 409 | 이미 참여했거나 UNIQUE 충돌 발생 |

`INVALID_INPUT`은 실제 ErrorCode가 아니다. API 문서와 프론트엔드 분기에서는 `INVALID_REQUEST`를 사용한다.

## 7. 테이블 및 핵심 제약조건

### 7.1 running_records

- `member_id` → `members.id` FK
- `distance_km > 0`
- `duration_seconds > 0`
- `pace_seconds_per_km > 0`
- 회원·날짜 조회 인덱스
- `deleted_at` soft delete

### 7.2 running_sessions

- `host_member_id` → `members.id` FK
- 제목 공백 금지
- `capacity > 0`
- 상태값: `OPEN`, `CLOSED`, `CANCELLED`
- `CANCELLED`일 때만 `cancelled_at` 존재
- 생성자 인덱스
- 상태·일정 인덱스

### 7.3 running_session_participants

- `session_id` → `running_sessions.id` FK
- `member_id` → `members.id` FK
- `UNIQUE (session_id, member_id)`
- 참여 취소는 관계 행 물리 삭제
- 모임별·회원별 조회 인덱스

## 8. 팀원 B 담당 범위

아래 파일과 기능은 현재 브랜치에서 구현하지 않았으며 팀원 B 담당이다.

- `RunningRecordApiController`
- `RunningSessionApiController`
- `RunningSessionParticipantApiController`
- Request DTO
- Response DTO
- Vue API Module
- 러닝 기록 화면
- 러닝 모임 화면
- 러닝 모임 참여 화면
- Vue Router 연결
- Swagger/API Tester 요청·응답 예시

Controller 구현 시 Service의 Session 인증과 권한 검증을 우회하지 않는다.

## 9. Response DTO 권장 구성

### 9.1 러닝 기록

권장 필드:

- `id`
- `recordDate`
- `distanceKm`
- `durationSeconds`
- `paceSecondsPerKm`
- `memo`
- `createdAt`
- `updatedAt`

본인 전용 기능이므로 `memberId` 노출 필요성은 낮다.

### 9.2 러닝 모임

권장 필드:

- `id`
- `hostMemberId`
- 생성자 표시명
- `title`
- `description`
- `meetingPlace`
- `scheduledAt`
- `capacity`
- `status`
- `participantCount`
- `cancelledAt`
- `createdAt`
- `updatedAt`

현재 `RunningSessionService`는 생성자 표시명과 `participantCount`를 함께 반환하지 않는다.

- 생성자 표시명: 안전한 회원 조회 결과와 조합
- 참여자 수: `getParticipantCount(session, sessionId)` 호출 결과와 조합

목록에서 모임마다 참여자 수를 개별 호출하면 N+1 호출이 발생할 수 있다. 초기 규모에서는 Controller 조합이 가능하지만, 목록 규모가 커지면 별도 집계 조회 계약을 추가해야 한다.

### 9.3 참여자

권장 필드:

- `id`
- `sessionId`
- `memberId`
- 참여자 표시명
- `joinedAt`

현재 Service 결과에는 참여자 표시명이 없다.

## 10. 구현 완료 기능

- 러닝 기록 생성, 본인 목록, 본인 상세, 수정, soft delete
- 러닝 기록 페이스 자동 계산
- 러닝 모임 생성, 목록, 상세, 생성자 수정
- 생성자 모집 상태 변경
- 생성자 모임 취소
- OPEN 모임 참여
- 본인 참여 취소와 재참여
- 중복 참여 방지
- 정원 초과 방지
- CLOSED/CANCELLED 참여 차단
- 모임 행 잠금을 이용한 동시성 제어
- 러닝 기능 더미 데이터
- Service 단위 테스트와 MySQL Mapper 통합 테스트

## 11. 자동 테스트 결과

P12.5 완료 시 아래 명령이 성공했다.

```powershell
cd ssafit
.\mvnw.cmd clean test
```

결과:

- 실행 196
- 실패 0
- 오류 0
- skip 12
- `BUILD SUCCESS`

skip된 12개는 `SSAFIT_TEST_DB_URL`이 설정된 전용 MySQL DB에서만 실행되는 `MapperIntegrationTest`다.

Mapper XML은 기본 Spring Context 테스트에서 파싱과 등록이 검증된다. 실제 FK, UNIQUE, soft delete, `FOR UPDATE`, 동시 정원 테스트는 전용 MySQL 테스트 DB에서 별도로 실행해야 한다.

## 12. 최종 코드 검수 결과

### 12.1 코드 구조

- Domain, Mapper, Service 책임 분리 확인
- Mapper Interface와 XML namespace 일치 확인
- Mapper 메서드명과 XML statement id 일치 확인
- Service에서 Session 로그인 회원 기준 권한 검증 확인
- 러닝 브랜치 변경 범위에 Controller/DTO/Vue 구현 없음
- 기준 커밋 `92a05f9` 이후 기존 MVP/Spotify Service 수정 없음
- 작업 트리 불필요 변경 없음

### 12.2 DB 정합성

- `running_records.member_id` FK 확인
- `running_sessions.host_member_id` FK 확인
- 참여 관계의 `(session_id, member_id)` UNIQUE 확인
- Service 중복 검증과 UNIQUE 제약 병행 확인
- 참여 트랜잭션에서 `SELECT ... FOR UPDATE` 사용 확인
- 정원 이상 참여 차단 확인
- CLOSED/CANCELLED 참여 차단 확인
- 참여 취소 물리 삭제와 재참여 정책 확인

### 12.3 테스트 존재 여부

- 러닝 기록 Mapper CRUD/soft delete 테스트 존재
- 러닝 기록 권한·입력값 테스트 존재
- 러닝 모임 Mapper CRUD/상태/취소/잠금 테스트 존재
- 러닝 모임 생성자 권한 테스트 존재
- 참여 Mapper CRUD/UNIQUE 테스트 존재
- 중복 참여 테스트 존재
- 정원 초과 테스트 존재
- CLOSED/CANCELLED 참여 실패 테스트 존재
- 취소 후 재참여 테스트 존재
- 정원 1명 동시 참여 통합 테스트 존재

## 13. 알려진 제한 사항

1. 모임 목록에 페이징과 검색 조건이 없다.
2. 모임 목록 결과에 참여자 수가 포함되지 않는다.
3. 모임과 참여자 Domain에 회원 닉네임이 포함되지 않는다.
4. 생성·수정·취소 Service는 DB 변경 후 행을 다시 조회하지 않는다.
   - 생성 직후 `createdAt`, `updatedAt`, `joinedAt`
   - 취소 직후 `cancelledAt`
   값이 반환 객체에 즉시 채워지지 않을 수 있다.
5. 모임 취소 시 기존 참여 관계는 유지된다.
6. CLOSED/CANCELLED 모임에서도 기존 참여자는 자신의 참여를 취소할 수 있다.
7. 러닝 기록 날짜와 모임 일정에 미래·과거 제한 정책이 없다.
8. Mapper 통합 테스트는 전용 MySQL 테스트 DB 환경 변수가 없으면 skip된다.
9. `schema.sql` 자동 실행이 비활성화되어 있어 기존 로컬 DB에는 신규 DDL을 직접 적용해야 한다.

## 14. develop 병합 전 확인할 사항

- [ ] `develop` 최신 변경을 반영한 뒤 충돌 확인
- [ ] `ErrorCode` 병합 충돌과 중복 이름 확인
- [ ] 최신 `schema.sql`에 러닝 테이블 3개가 유지되는지 확인
- [ ] 기존 로컬·공용 DB에 신규 DDL 적용 계획 확인
- [ ] 전용 MySQL 테스트 DB에서 `MapperIntegrationTest` 실행
- [ ] `mvnw.cmd clean test` 재실행
- [ ] 팀원 B가 `INVALID_REQUEST`를 입력 오류 코드로 사용하는지 확인
- [ ] Controller가 `HttpSession`을 Service에 전달하는지 확인
- [ ] Request DTO에서 `memberId`, `hostMemberId`, `status`, 계산 페이스를 신뢰하지 않는지 확인
- [ ] Response DTO에서 비밀번호 해시 등 회원 민감 필드를 제외하는지 확인
- [ ] 목록 참여자 수 조합 방식과 N+1 허용 범위 확인
- [ ] Controller/DTO/Vue 변경은 별도 브랜치에서 진행

## 15. 권장 커밋 메시지

문서 추가:

```text
docs: add running features service integration contract
```

최종 검수 후 별도 정리 커밋이 필요한 경우:

```text
chore: finalize running features branch verification
```
