# 실제 MySQL 통합 테스트 결과

## 1. 실행 환경

- MySQL 버전: 8.0.32
- Java 버전: 21.0.10
- Spring Boot 버전: 4.0.6
- Maven 버전: 3.9.16
- OS: Windows 11
- 테스트 DB 이름: `ssafit_test`
- 테스트 DB 포트: `33306`
- DB 격리 수준: `REPEATABLE-READ`
- 테이블 엔진: InnoDB
- 실행 일시: 2026-06-21 10:46~10:49 KST

공식 `mysql:8.0.32` Docker 이미지로 호스트 개발 DB와 분리된 테스트 전용 서버를 구성했다. 테스트 계정은 `ssafit_test.*`에만 권한을 가진다.

## 2. 환경변수 확인

- `SSAFIT_TEST_DB_URL`: 설정됨
- DB 계정: 설정됨
- DB 비밀번호: 설정됨
- 민감 정보 로그 및 문서 기록: 없음

JDBC URL의 DB 이름이 `ssafit_test`임을 실행 전에 확인했다. 기존 개발 DB `ssafit_playlist`에는 테스트를 실행하지 않았다.

## 3. 실행 명령

PowerShell 프로세스에 다음 환경변수를 설정한 뒤 실행했다.

```powershell
$env:SSAFIT_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:33306/ssafit_test?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true'
$env:SSAFIT_TEST_DB_USERNAME = 'ssafit_test'
$env:SSAFIT_TEST_DB_PASSWORD = '<REDACTED>'

.\mvnw.cmd clean test
```

동시성 결함 재현 명령:

```powershell
.\mvnw.cmd -Dtest=MapperIntegrationTest#concurrentJoinRequestsDoNotExceedCapacity test
```

## 4. 테스트 결과

- Tests run: 197
- Failures: 1
- Errors: 0
- Skipped: 0
- BUILD FAILURE

`MapperIntegrationTest` 12건은 모두 실제 MySQL에서 실행되었으며 Skip되지 않았다.

- Mapper 통합 테스트: 12건 실행
- 통과: 11건
- 실패: 1건
- Skip: 0건

실패 테스트:

```text
MapperIntegrationTest.concurrentJoinRequestsDoNotExceedCapacity
```

실패 결과:

```text
두 참여 요청 결과: [성공, 성공]
예상 결과: [성공, SESSION_FULL]
최종 참여자 수: 2
예상 참여자 수: 1
```

## 5. 실제 실행된 Mapper 통합 테스트 목록

1. `memberAndProfileMapperMapsGeneratedKeysAndEnums`
2. `runningRecordMapperSupportsCrudAndSoftDelete`
3. `runningSessionMapperSupportsCrudStatusCancelAndLockQuery`
4. `runningSessionParticipantMapperSupportsJoinCancelAndRejoin`
5. `concurrentJoinRequestsDoNotExceedCapacity`
6. `musicMapperAppliesSearchSortAndPaging`
7. `reviewMapperCalculatesAverageScores`
8. `playlistMappersJoinVisibilityItemsSpotifyUrisAndSavedList`
9. `databaseRejectsUniqueAndForeignKeyViolations`
10. `recommendationQueryCreatesOnlyLogAndSaveCopiesItems`
11. `recommendationSaveRollsBackPlaylistWhenBulkInsertFails`
12. `spotifyConnectionMapperInsertsAndRefreshesToken`

검증된 항목:

- Mapper XML namespace와 SQL 실행
- Domain/resultMap enum 및 필드 매핑
- generated key 매핑
- INSERT, SELECT, UPDATE, DELETE
- FK 제약
- UNIQUE 제약
- CHECK 제약
- NOT NULL 제약
- soft delete 조회 제외
- 생성 및 수정 시간 저장
- 추천 저장 트랜잭션 롤백
- 영향 행 수 0/1 반환이 명시된 Mapper 흐름

MySQL CHECK 제약 위반은 오류 코드 `3819`로 정상 거부되었다. Spring JDBC는 해당 오류를 `UncategorizedSQLException`으로 변환하므로 제약 실패 검증은 `DataAccessException` 상위 타입으로 확인했다.

## 6. 러닝 모임 동시성 테스트 결과

- 정원 초과 방지: 실패
- 중복 참여 방지: 통과
- 트랜잭션 롤백: 추천 저장 롤백은 통과, 동시 정원 초과 요청은 예외가 발생하지 않아 참여 INSERT 두 건 모두 커밋됨
- `FOR UPDATE` 락 검증: SQL 실행 및 InnoDB 사용은 확인했으나 현재 처리 순서에서는 정원 초과 방지에 실패

재현 조건:

1. 정원 1명인 `OPEN` 모임 생성
2. 서로 다른 두 회원이 `CountDownLatch`로 동시에 참여 요청
3. 두 요청 모두 `joinSession` 성공
4. 최종 참여자 수 2명 확인

원인 분석:

1. MySQL 기본 격리 수준은 `REPEATABLE-READ`다.
2. `joinSession`은 모임 행 잠금 전 `AuthService.getLoginMember`를 호출한다.
3. 회원 조회가 트랜잭션의 일관 읽기 스냅샷을 먼저 생성한다.
4. `selectByIdForUpdate`는 동일 모임 행을 잠그지만, 이후 `countParticipants`는 일반 SELECT다.
5. 대기 후 잠금을 획득한 두 번째 트랜잭션도 기존 스냅샷 기준으로 참여자 수 0을 읽는다.
6. 참여 회원이 서로 달라 UNIQUE `(session_id, member_id)` 제약에도 걸리지 않는다.

원인 교차 검증:

- 동일 비즈니스 코드에서 테스트 MySQL 격리 수준만 일시적으로 `READ-COMMITTED`로 변경했다.
- `concurrentJoinRequestsDoNotExceedCapacity` 단일 테스트가 통과했다.
- 테스트 후 격리 수준을 `REPEATABLE-READ`로 복원했다.

따라서 현재 결함은 `FOR UPDATE` SQL 자체의 누락이 아니라, `REPEATABLE-READ`에서 잠금 전 생성된 스냅샷과 이후 일반 참여자 수 조회의 조합으로 판단된다.

권장 최소 수정 방향은 별도 구현 커밋에서 다음 중 하나를 선택하는 것이다.

1. 참여 트랜잭션을 `READ_COMMITTED` 격리 수준으로 명시한다.
2. 참여자 수 확인을 현재 읽기 또는 잠금 읽기로 변경한다.
3. 모임 행에 참여자 수를 원자적으로 관리하는 방식으로 변경한다.

본 작업에서는 비즈니스 로직을 수정하지 않았다.

## 7. 발견된 문제

### P0: 동시 참여 시 정원 초과

- 심각도: P0
- 현재 상태: 실제 MySQL에서 재현
- 재현 절차: 정원 1명 모임에 서로 다른 회원 두 명이 동시에 참여
- 원인: `REPEATABLE-READ` 스냅샷에서 일반 `countParticipants` 조회
- 영향 범위: 모임 정원을 초과한 참여 관계 저장 및 서비스 정책 위반
- 수정 필요 여부: develop 병합 전 필수

### P2: CHECK 제약 예외 타입 기대 차이

- 심각도: P2
- 현재 상태: 테스트 설정 보완 완료
- 원인: MySQL 오류 3819를 Spring JDBC가 `UncategorizedSQLException`으로 변환
- 영향 범위: CHECK 제약은 정상 작동하지만 구체 예외 타입 assertion이 이식성을 저해함
- 조치: `DataAccessException` 상위 타입으로 검증

### P2: 기존 테스트 DB 오연결 방지 부족

- 심각도: P2
- 현재 상태: 테스트 안전장치 보완 완료
- 원인: 기존 `@EnabledIfEnvironmentVariable`이 URL 비어 있지 않음만 검사
- 영향 범위: 잘못된 URL 입력 시 테스트 데이터 삭제 위험
- 조치:
  - 환경변수 활성화 정규식에 테스트 DB 이름 조건 추가
  - 별도 `TestDatabaseSafetyTest` 추가
  - 실제 연결 후 `SELECT DATABASE()` 재검증

### P2: 영향 행 수 미확인 Service

- 심각도: P2
- 현재 상태: 기존 코드에 일부 존재
- 내용: 일부 수정·삭제 Service는 Mapper 반환 영향 행 수를 확인하지 않는다.
- 영향 범위: 동시 삭제 또는 대상 소실 시 실제 변경 없이 성공처럼 종료할 수 있음
- 조치: 이번 작업에서는 수정하지 않고 후속 검토 대상으로 유지

## 8. 데이터 정리 결과

`@AfterEach`와 `@AfterAll` 실행 후 12개 테스트 대상 테이블의 전체 행 수 합계를 직접 조회했다.

```text
전체 잔존 행 수: 0
```

확인 대상:

- members
- running_profiles
- running_records
- running_sessions
- running_session_participants
- music
- music_reviews
- playlists
- playlist_items
- playlist_saves
- recommendation_logs
- spotify_connections

실패한 동시성 테스트가 있었음에도 테스트 데이터는 정상 정리되었다.

## 9. develop 병합 판단

- 판정: 병합 보류

근거:

- 실제 MySQL 연결: 충족
- Mapper 통합 테스트 실행: 충족
- Mapper 테스트 Skip 0: 충족
- 전용 테스트 DB 사용: 충족
- FK, UNIQUE, CHECK, NOT NULL, soft delete 검증: 충족
- 테스트 데이터 정리: 충족
- 전체 Failures 0: 미충족
- 러닝 모임 정원 초과 방지: 미충족
- 전체 BUILD SUCCESS: 미충족

러닝 모임 참여 동시성 결함을 수정한 뒤 `REPEATABLE-READ` 상태의 실제 MySQL에서 전체 테스트를 다시 실행해야 한다. `Failures: 0`, `Errors: 0`, `Skipped: 0`, `BUILD SUCCESS`가 확인되기 전에는 develop 병합을 권장하지 않는다.

