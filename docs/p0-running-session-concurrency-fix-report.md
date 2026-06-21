# P0 러닝 모임 동시 참여 정원 초과 수정 보고서

## 1. 재현된 문제

### 재현 조건

```text
정원 1명인 OPEN 모임
+ 서로 다른 두 회원의 동시 참여 요청
→ 두 요청 모두 성공
→ 최종 참여자 수 2명
```

### 실제 MySQL 환경

- MySQL: 8.0.32
- DB: `ssafit_test`
- 테이블 엔진: InnoDB
- 트랜잭션 격리 수준: `REPEATABLE-READ`
- 테스트 방식: `ExecutorService`, `CountDownLatch`, 독립 Service 트랜잭션

### 기존 실패 결과

```text
요청 결과: [성공, 성공]
예상 결과: [성공, SESSION_FULL]
최종 참여자 수: 2
예상 참여자 수: 1
BUILD FAILURE
```

## 2. 근본 원인

기존 `joinSession`은 다음 순서로 DB에 접근했다.

```text
1. AuthService.getLoginMember()
   → members 일반 SELECT
2. RunningSessionMapper.selectByIdForUpdate()
   → running_sessions SELECT ... FOR UPDATE
3. 모임 상태 검증
4. 중복 참여 일반 SELECT
5. 현재 참여자 수 일반 SELECT
6. 참여 관계 INSERT
```

MySQL InnoDB의 `REPEATABLE-READ`에서는 첫 일반 SELECT가 트랜잭션의 일관 읽기 스냅샷을 생성한다.

두 번째 요청은 회원 조회 시 참여자가 아직 없는 스냅샷을 먼저 생성한 뒤 모임 행 잠금을 기다렸다. 첫 번째 요청이 참여 관계를 커밋하고 잠금을 해제해도, 두 번째 요청의 `countParticipants`는 기존 스냅샷에서 참여자 수 0을 읽었다.

두 요청의 회원이 서로 다르므로 `(session_id, member_id)` UNIQUE 제약도 정원 초과를 막지 못했다.

## 3. 수정 내용

### 수정 전 DB 조회 순서

```text
회원 일반 SELECT
→ 모임 SELECT ... FOR UPDATE
→ 상태
→ 중복
→ 참여자 수
→ INSERT
```

### 수정 후 DB 조회 순서

```text
Session에서 로그인 회원 ID 추출(DB 접근 없음)
→ 모임 SELECT ... FOR UPDATE
→ 회원 존재 및 활성 상태 일반 SELECT
→ 모임 상태
→ 중복 참여
→ 최신 참여자 수
→ 참여 INSERT
→ 커밋
```

`AuthService.getLoginMemberId`는 Session 속성만 읽기 때문에 DB 스냅샷을 만들지 않는다. 모임 행 잠금이 트랜잭션의 첫 번째 DB 읽기가 되며, 잠금 획득 뒤 `AuthService.getLoginMember`를 호출해 기존 회원 존재·활성 상태 검증도 유지했다.

대기 중인 다음 참여 요청은 이전 참여 트랜잭션의 커밋 후 모임 잠금을 획득한다. 그 후 최초 일반 SELECT가 실행되므로 최신 참여 관계가 포함된 스냅샷에서 인원을 확인한다.

### 변경 파일

- `RunningSessionParticipantService.java`
  - Session 회원 ID 추출과 회원 DB 검증을 분리
  - `selectByIdForUpdate`를 첫 DB 조회로 이동
- `RunningSessionParticipantServiceTest.java`
  - 변경된 호출 순서 검증
  - 인증 검증 유지 확인
- `MapperIntegrationTest.java`
  - 실제 MySQL 환경 검증
  - 정원 1명 동시 요청 반복 테스트
  - 정원 2명 동시 요청 테스트
- `TestDatabaseSafetyTest.java`
  - 테스트 DB 오연결 방지

Mapper Interface/XML과 DDL은 변경하지 않았다. 기존 `SELECT ... FOR UPDATE`와 UNIQUE 제약을 그대로 유지했다.

## 4. 테스트 보강

### 정원 1명 / 동시 요청 2건

- 서로 다른 회원 2명 사용
- `CountDownLatch`로 동시에 시작
- 각 워커가 독립 Service 트랜잭션 사용
- 한 테스트에서 10회 반복
- 반복마다 별도의 회원과 모임 사용

각 반복의 검증:

- 성공 정확히 1건
- `SESSION_FULL` 정확히 1건
- 최종 참여자 수 정확히 1명
- 실패 요청의 참여 관계 없음

### 정원 2명 / 동시 요청 3건

- 성공 정확히 2건
- `SESSION_FULL` 정확히 1건
- 최종 참여자 수 정확히 2명

### REPEATABLE-READ 확인

동시성 테스트 시작 시 실제 DB에서 다음을 직접 조회한다.

```sql
SELECT DATABASE();
SELECT @@transaction_isolation;

SELECT ENGINE
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'running_sessions';
```

테스트는 DB 이름에 `test`가 포함되고, 격리 수준이 `REPEATABLE-READ`이며, 엔진이 InnoDB일 때만 기대 조건을 만족한다.

`Thread.sleep`, JVM lock, `READ_COMMITTED`, 재시도 루프는 사용하지 않았다.

## 5. 실제 MySQL 실행 결과

### 정원 1명 단일 동시성 테스트

```text
Tests run: 1
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

테스트 내부 반복 횟수: 10회

### 정원 2명 추가 동시성 테스트

```text
Tests run: 1
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### 전체 테스트

```text
Tests run: 198
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

- Mapper 통합 테스트: 13건 모두 실제 MySQL에서 실행
- Mapper 통합 테스트 Skip: 0
- 기존 단위 테스트 회귀: 없음
- 테스트 종료 후 잔존 데이터: 0건

## 6. develop 병합 판단

- 판정: 가능

근거:

- MySQL 8.0.32 실제 서버에서 검증
- `REPEATABLE-READ` 유지
- InnoDB 행 잠금 사용
- 정원 1명/2명 동시성 시나리오 모두 통과
- 전체 테스트 198건 통과
- Failures, Errors, Skipped 모두 0
- 테스트 데이터 정리 확인

