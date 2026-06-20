# P12.0 러닝 추가 기능 구현 전 사전 점검 보고서

## 1. 점검 개요

- 점검 브랜치: `feature/a-running-features-service`
- 애플리케이션 루트: 저장소 루트의 `ssafit/`
- 점검일: 2026-06-20
- 점검 범위:
  - `ssafit/src/main/java`
  - `ssafit/src/main/resources`
  - `ssafit/src/main/resources/sql`
  - `ssafit/src/main/resources/mappers`
  - `ssafit/src/test/java`
  - `ssafit/src/test/resources`
  - 루트 `README.md`
  - 기존 인증, 예외, Mapper, 테스트 코드

이번 단계에서는 기존 코드를 수정하지 않고 현재 구조와 구현 관례만 분석했다.

## 2. 결론 요약

러닝 기록과 러닝 모임 기능은 아직 구현되지 않았다. 다만 아래 패키지와 Mapper 디렉터리는 `.gitkeep`만 포함한 빈 골격으로 이미 준비되어 있다.

```text
com.ssafy.ssafit.runningrecord
├─ domain
├─ dto
├─ mapper
└─ service

com.ssafy.ssafit.runningsession
├─ domain
├─ dto
├─ mapper
└─ service

src/main/resources/mappers/runningrecord
src/main/resources/mappers/runningsession
```

현재 `schema.sql`에는 러닝 기록, 러닝 모임, 모임 참여 테이블이 없다. 따라서 구현 시 기존 9개 테이블 뒤에 신규 테이블을 추가해야 한다.

기존 프로젝트는 다음 관례가 확립되어 있다.

- 기능별 최상위 패키지 아래 `domain`, `dto`, `mapper`, `service` 분리
- Domain은 Lombok 기반 mutable POJO
- Enum은 Domain 패키지 안의 단순 Java enum
- Mapper Interface는 `@Mapper`, 다중 인자는 `@Param` 사용
- Mapper XML은 `src/main/resources/mappers/{feature}` 아래 배치
- XML `namespace`는 Mapper Interface FQCN과 일치
- 복합 조회와 Enum·시간 필드는 명시적 `resultMap` 사용
- Service는 클래스 단위 `@Transactional(readOnly = true)`, 쓰기 메서드에 `@Transactional`
- 비즈니스 예외는 `CustomException(ErrorCode)`로 통일
- Service 단위 테스트는 JUnit 5 + Mockito + AssertJ
- 실제 MySQL Mapper 통합 테스트는 환경 변수가 있을 때만 실행
- 회원과 플레이리스트에 soft delete가 적용되어 있으나 모든 테이블의 공통 정책은 아님

이번 러닝 기능은 공통 지시사항에 따라 로그인 회원 ID를 메서드 입력값으로 신뢰하지 않아야 한다. 신규 Service의 인증 필요 메서드는 `HttpSession`과 기존 `AuthService`를 사용해 로그인 회원을 내부에서 조회하는 방식이 적합하다.

## 3. 기술 구성 확인

`ssafit/pom.xml` 기준 구성은 다음과 같다.

| 항목 | 현재 상태 |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.6 |
| MyBatis Starter | 4.0.1 |
| DB Driver | MySQL Connector/J |
| Validation | Spring Validation |
| 테스트 | Spring Boot Test, JUnit 5, Mockito, AssertJ |
| 인증 | HttpSession 기반 |
| SQL 자동 초기화 | 운영 기본값 `never` |

`application.yml`의 주요 MyBatis 설정은 다음과 같다.

```yaml
mybatis:
  mapper-locations: classpath:/mappers/**/*.xml
  type-aliases-package: com.ssafy.ssafit
  configuration:
    map-underscore-to-camel-case: true
```

DB 컬럼은 `snake_case`, Java 필드는 `camelCase`를 사용한다.

## 4. 현재 schema.sql 구조

현재 `schema.sql`에는 아래 9개 테이블이 순서대로 정의되어 있다.

| 순서 | 테이블 | 역할 | 삭제 정책 |
|---:|---|---|---|
| 1 | `members` | 회원 | `status` + `deleted_at` soft delete |
| 2 | `running_profiles` | 회원별 러닝 강도·BPM 프로필 | 별도 삭제 컬럼 없음 |
| 3 | `music` | 음악 메타데이터 | 별도 삭제 컬럼 없음 |
| 4 | `music_reviews` | 회원별 음악 평가 | 별도 삭제 컬럼 없음 |
| 5 | `playlists` | 공식·사용자 플레이리스트 | `deleted_at` soft delete |
| 6 | `playlist_items` | 플레이리스트 수록곡 | 물리 삭제, 부모 삭제 시 cascade |
| 7 | `playlist_saves` | 플레이리스트 저장 관계 | 물리 삭제, FK cascade |
| 8 | `recommendation_logs` | 자동 추천 요청·결과 로그 | 별도 삭제 컬럼 없음 |
| 9 | `spotify_connections` | 회원별 Spotify OAuth 연결 | 물리 삭제, 회원 삭제 시 cascade |

### 4.1 공통 DDL 관례

- PK: `BIGINT UNSIGNED AUTO_INCREMENT`
- 시간 정밀도: `DATETIME(6)`
- 생성 시각: `DEFAULT CURRENT_TIMESTAMP(6)`
- 수정 시각:

```sql
updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
    ON UPDATE CURRENT_TIMESTAMP(6)
```

- PK, UK, FK, CHECK 제약조건에 명시적 이름 사용
  - `pk_{table}`
  - `uk_{table}_{columns}`
  - `fk_{table}_{target}`
  - `ck_{table}_{rule}`
- FK 갱신은 대부분 `ON UPDATE RESTRICT`
- 삭제 동작은 데이터 성격에 따라 `RESTRICT`, `CASCADE`, `SET NULL`을 구분
- 상태 Enum은 `VARCHAR`와 CHECK 제약조건을 함께 사용
- 엔진 및 문자셋:

```sql
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
```

### 4.2 러닝 기능 관련 현재 상태

- `running_profiles`만 존재한다.
- 러닝 기록 테이블은 없다.
- 러닝 모임 테이블은 없다.
- 러닝 모임 참여 관계 테이블은 없다.
- `runningrecord`, `runningsession` Java/Mapper 디렉터리는 빈 골격만 존재한다.

### 4.3 schema.sql 변경 시 주의점

`schema.sql`은 `CREATE TABLE`에 `IF NOT EXISTS`를 사용하지 않는다. SQL README에도 기존 DB에 전체 스키마를 반복 실행하지 말고 별도 마이그레이션 SQL을 작성하라고 명시되어 있다.

따라서 이번 브랜치에서는:

1. 신규 설치용 `schema.sql`에는 신규 테이블 DDL을 추가한다.
2. 기존 팀원 로컬 DB 적용 방법은 Service 계약 문서 또는 별도 안내에 `CREATE TABLE` 구문 실행 순서로 기록한다.
3. 기존 9개 테이블 정의를 불필요하게 재작성하지 않는다.

## 5. Domain 패키지와 작성 방식

기능별 패키지 구조는 다음 형태다.

```text
com.ssafy.ssafit.{feature}
├─ domain
├─ dto
├─ mapper
└─ service
```

기존 Domain은 일반적으로 아래 Lombok 구성을 사용한다.

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Domain {
    // fields
}
```

주요 타입 관례:

- PK/FK: `Long`
- 작은 정수와 일반 정수: `Integer`
- 날짜·시간: `LocalDateTime`
- 상태값: 해당 Domain 패키지의 Enum
- DB 컬럼과 동일한 의미의 필드만 포함하는 것이 기본
- 목록 전용 계산값이 필요하면 `Playlist.saved`처럼 Domain에 보조 필드를 추가한 사례가 있음

신규 Domain 권장 위치:

```text
runningrecord/domain/RunningRecord.java
runningsession/domain/RunningSession.java
runningsession/domain/RunningSessionParticipant.java
```

`RunningSession` 명칭은 이미 준비된 `runningsession` 패키지와 충돌하지 않고, 기존 `HttpSession`과는 FQCN이 다르므로 Java 사용 자체에는 문제가 없다. 같은 파일에서 두 타입을 동시에 사용할 경우 import 가독성에 주의해야 한다.

## 6. 기존 Enum 작성 방식

현재 비즈니스 Enum은 모두 해당 기능의 `domain` 패키지에 위치하며 별도 필드나 변환 메서드가 없는 단순 enum이다.

```java
public enum RunningIntensity {
    LOW,
    MODERATE,
    HIGH,
    INTERVAL
}
```

현재 Enum 목록:

- `MemberStatus`
- `RunningIntensity`
- `MusicSourceType`
- `MusicSortType`
- `PlaylistType`
- `PlaylistSourceType`
- `PlaylistVisibility`
- `SpotifyExportStatus`

MyBatis는 Enum 이름을 DB의 `VARCHAR` 값으로 매핑하고, DDL CHECK 제약조건이 허용값을 제한한다.

신규 Enum 후보:

- `RunningSessionStatus`
  - 최소 후보: `OPEN`, `CLOSED`, `CANCELLED`, `COMPLETED`
- `RunningSessionParticipantStatus`
  - 최소 후보: `JOINED`, `CANCELLED`

상태 이름과 전이 규칙은 DDL, Java Enum, Service 검증, 테스트에서 정확히 동일해야 한다.

## 7. ErrorCode와 CustomException 구조

### 7.1 ErrorCode

`ErrorCode`는 다음 구조의 enum이다.

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    SOME_ERROR(HttpStatus.BAD_REQUEST, "메시지");

    private final HttpStatus status;
    private final String message;
}
```

기존 공통 인증·권한 코드:

| ErrorCode | HTTP 상태 | 용도 |
|---|---:|---|
| `INVALID_REQUEST` | 400 | 일반 잘못된 요청 |
| `AUTH_REQUIRED` | 401 | 로그인 필요 |
| `ACCESS_DENIED` | 403 | 소유자·본인 권한 없음 |
| `MEMBER_NOT_FOUND` | 404 | 회원 없음 |

기존 구현은 기능별 not found, invalid, duplicate 코드를 추가하고 공통 권한 오류는 `ACCESS_DENIED`를 재사용하는 경향이 있다.

### 7.2 CustomException

`CustomException`은 `RuntimeException`을 상속하고 `ErrorCode` 하나만 보유한다.

```java
throw new CustomException(ErrorCode.SOME_ERROR);
```

사용자별 동적 메시지를 담는 생성자는 없다. 신규 기능도 기존 구조를 유지해야 한다.

### 7.3 전역 처리

`GlobalExceptionHandler`가 `CustomException`을 받아 다음 형태의 `ErrorResponse`로 변환한다.

- HTTP 상태: `ErrorCode.status`
- 응답 코드: `ErrorCode.name()`
- 메시지: `ErrorCode.message`

이번 기능에서 검토할 ErrorCode 후보:

| 후보 | 권장 상태 | 의미 |
|---|---:|---|
| `RUNNING_RECORD_NOT_FOUND` | 404 | 러닝 기록 없음 |
| `INVALID_RUNNING_RECORD` | 400 | 거리·시간·실행 시각 등 값 오류 |
| `RUNNING_SESSION_NOT_FOUND` | 404 | 러닝 모임 없음 또는 삭제됨 |
| `INVALID_RUNNING_SESSION` | 400 | 제목·일시·정원 등 값 오류 |
| `RUNNING_SESSION_NOT_OPEN` | 409 | 참여 가능한 상태가 아님 |
| `RUNNING_SESSION_FULL` | 409 | 정원 초과 |
| `DUPLICATE_RUNNING_SESSION_PARTICIPATION` | 409 | 이미 참여 중 |
| `RUNNING_SESSION_PARTICIPATION_NOT_FOUND` | 404 | 취소할 참여 정보 없음 |

생성자 권한 위반에는 기존 `ACCESS_DENIED`를 우선 재사용한다. 실제 구현 전 상태명과 정책을 확정한 뒤 중복 ErrorCode를 만들지 않는다.

## 8. Mapper Interface/XML 위치와 작성 방식

### 8.1 위치

```text
Java:
src/main/java/com/ssafy/ssafit/{feature}/mapper/{Feature}Mapper.java

XML:
src/main/resources/mappers/{feature}/{Feature}Mapper.xml
```

요청서에는 `src/main/resources/mapper`가 확인 대상으로 적혀 있지만 실제 프로젝트 경로는 복수형인 `src/main/resources/mappers`다.

### 8.2 Interface 관례

```java
@Mapper
public interface RunningProfileMapper {

    Optional<RunningProfile> selectByMemberId(@Param("memberId") Long memberId);

    int insert(RunningProfile runningProfile);

    int update(RunningProfile runningProfile);
}
```

- 단건 조회는 `Optional<T>`
- 목록 조회는 `List<T>`
- 변경 건수는 `int`
- 여러 파라미터는 각각 `@Param`
- 생성 PK는 XML의 `useGeneratedKeys`로 Domain에 반영

애플리케이션에는 `@MapperScan("com.ssafy.ssafit")`도 선언되어 있지만 기존 Interface와 동일하게 `@Mapper`를 유지한다.

### 8.3 XML 관례

- MyBatis 3 DTD 사용
- `namespace`는 Interface FQCN
- `resultMap`으로 snake_case와 camelCase를 명시적으로 연결
- 공통 컬럼은 `<sql id="...Columns">`로 추출
- Interface 메서드명과 XML statement `id`를 일치
- Domain 인자는 FQCN `parameterType` 사용
- 동적 조건은 `<if>`, `<choose>` 사용

신규 Mapper 후보:

```text
RunningRecordMapper.java
RunningRecordMapper.xml

RunningSessionMapper.java
RunningSessionMapper.xml

RunningSessionParticipantMapper.java
RunningSessionParticipantMapper.xml
```

### 8.4 참여 동시성 처리 권장안

모임 참여는 단순히 Service에서 현재 인원을 읽고 insert하면 동시 요청에서 정원을 초과할 수 있다.

권장 트랜잭션 흐름:

1. 모임 행을 `SELECT ... FOR UPDATE`로 조회한다.
2. 모임 존재 여부와 `OPEN` 상태를 검증한다.
3. 해당 회원의 현재 참여 상태를 확인한다.
4. `JOINED` 인원을 조회한다.
5. 정원 미만일 때만 참여 insert 또는 재참여 update를 수행한다.
6. `(running_session_id, member_id)` UNIQUE 제약조건으로 중복을 DB에서도 차단한다.
7. `DuplicateKeyException`을 기능 ErrorCode로 변환한다.

Service 검증과 DB 제약조건을 함께 둬야 중복·정원 정책이 안정적이다.

## 9. AuthService와 Session 로그인 회원 조회 방식

### 9.1 Session 키

```java
SessionConst.LOGIN_MEMBER_ID = "LOGIN_MEMBER_ID"
```

로그인 성공 시 `AuthService.login`이 아래 값을 저장한다.

```java
session.setAttribute(SessionConst.LOGIN_MEMBER_ID, member.getId());
```

### 9.2 AuthService 공개 메서드

| 메서드 | 역할 |
|---|---|
| `authenticate(loginId, rawPassword)` | 자격 증명과 활성 회원 검증 |
| `login(session, loginId, rawPassword)` | 인증 후 회원 ID를 Session에 저장 |
| `logout(session)` | Session 무효화 |
| `getLoginMemberId(session)` | Session에서 Long 회원 ID 조회 |
| `getLoginMember(session)` | Session 회원 조회 후 활성 상태 검증 |
| `validateSelf(loginMemberId, targetMemberId)` | 본인 ID 일치 검증 |

중요한 차이:

- `getLoginMemberId`는 Session 값의 존재와 타입만 확인한다.
- `getLoginMember`는 DB에서 회원을 다시 조회하고 `ACTIVE`, `deletedAt == null`까지 검증한다.

### 9.3 신규 Service 적용 원칙

기존 일부 Service는 `Long loginMemberId`를 외부에서 전달받지만, 이번 공통 지시사항은 Service가 로그인 회원 ID를 외부 입력값으로 신뢰하지 않도록 요구한다.

따라서 인증이 필요한 신규 Service 메서드는 아래 원칙을 따른다.

```java
Member loginMember = authService.getLoginMember(session);
Long loginMemberId = loginMember.getId();
```

- 생성·수정·삭제·참여·취소는 `HttpSession`을 받아 내부에서 회원 ID를 얻는다.
- Domain 또는 별도 인자로 전달된 `memberId`, `creatorId`는 신뢰하지 않는다.
- 생성 시 `memberId`/`creatorId`는 로그인 회원 ID로 덮어쓴다.
- 수정·삭제 시 DB에서 원본을 먼저 조회한 뒤 로그인 회원 ID와 소유자/생성자 ID를 비교한다.
- 권한 검증은 Controller가 아니라 Service에서 수행한다.
- 공개 목록·상세 조회에서 인증이 선택 사항인지 여부는 팀원 B와 계약 문서에서 명확히 구분한다.

## 10. 기존 soft delete 사용 여부

soft delete는 프로젝트 전체 공통 규칙이 아니라 선택적으로 적용되어 있다.

### 10.1 적용 대상

- `members`
  - `status = 'WITHDRAWN'`
  - `deleted_at IS NOT NULL`
  - CHECK 제약조건으로 상태와 삭제 시각의 정합성 보장
- `playlists`
  - `deleted_at`만 기록
  - Mapper 조회마다 `deleted_at IS NULL`
  - `softDelete` update 사용

### 10.2 미적용 대상

- `running_profiles`
- `music`
- `music_reviews`
- `playlist_items`
- `playlist_saves`
- `recommendation_logs`
- `spotify_connections`

관계 테이블인 `playlist_saves`는 취소 시 물리 삭제한다.

### 10.3 신규 기능 권장 정책

- 러닝 기록:
  - 사용자 CRUD 이력 보존 요구가 없다면 물리 삭제가 가장 단순하다.
  - 복구·감사 요구가 있으면 `deleted_at` soft delete를 적용한다.
  - 현재 요구만으로는 확정할 수 있으므로 구현 전에 정책 확정이 필요하다.
- 러닝 모임:
  - 생성 후 참여 관계가 생기므로 물리 삭제보다 `status = CANCELLED` 또는 `deleted_at`이 안전하다.
  - 일반 삭제 API와 모임 상태 취소의 의미를 하나로 볼지 구분해야 한다.
- 모임 참여:
  - 참여/취소 상태 검증과 재참여 이력을 다루려면 `JOINED`/`CANCELLED` 상태 보존이 유리하다.
  - 단순 물리 삭제 방식은 구현은 쉽지만 취소 이력과 재참여 정책 표현이 약하다.

권장 기본안은 모임은 상태 기반 취소, 참여는 상태 기반 취소다. `deleted_at`까지 추가할지는 조회·복구 요구가 있을 때 결정한다.

## 11. 기존 테스트 구조

### 11.1 Service 테스트

경로:

```text
src/test/java/com/ssafy/ssafit/{feature}/service
```

작성 방식:

- `@ExtendWith(MockitoExtension.class)`
- Mapper와 협력 Service를 `@Mock`
- `@BeforeEach`에서 Service 직접 생성
- AssertJ의 `assertThat`, `assertThatThrownBy`
- Mapper 호출 여부를 `verify`, `never`로 검증
- `CustomException.errorCode`를 직접 비교

신규 Service 테스트 권장 파일:

```text
runningrecord/service/RunningRecordServiceTest.java
runningsession/service/RunningSessionServiceTest.java
runningsession/service/RunningSessionParticipantServiceTest.java
```

`AuthService`와 `HttpSession`을 mock하고, 외부에서 전달된 회원 ID가 아니라 Session 회원이 사용되는지 검증해야 한다.

### 11.2 Mapper 통합 테스트

현재 `MapperIntegrationTest` 하나가 실제 MySQL 기반 Mapper·제약조건·트랜잭션을 통합 검증한다.

특징:

- `@SpringBootTest`
- `@ActiveProfiles("mapper-test")`
- `SSAFIT_TEST_DB_URL` 환경 변수가 있을 때만 실행
- `application-mapper-test.yml`에서 `schema.sql` 자동 실행
- 각 테스트 전후 전체 데이터 정리
- UNIQUE/FK 위반과 트랜잭션 rollback 검증

신규 Mapper 검증 항목:

- Enum과 `LocalDateTime` 매핑
- generated key 반환
- 회원별 러닝 기록 목록 정렬
- 다른 회원 기록이 조회·수정·삭제되지 않는 조건
- 모임 생성자 조회와 상태 매핑
- 참여 UNIQUE 제약조건
- 정원 계산 시 `JOINED`만 포함
- 취소 후 재참여 정책
- 모임 잠금 조회 또는 조건부 갱신 동작
- FK 위반과 트랜잭션 rollback

통합 테스트 데이터 정리 순서는 FK 역순으로 신규 참여 → 모임 → 기록을 기존 회원 삭제보다 먼저 처리해야 한다.

### 11.3 기본 테스트 실행 특성

실제 DB 환경 변수가 없으면 Mapper 통합 테스트는 skip된다. 나머지 Service 단위 테스트와 Context 테스트는 기본 `mvnw.cmd clean test`에 포함된다.

## 12. data.sql과 dummy.sql 구조

### 12.1 data.sql

애플리케이션 구동에 필요한 최소 기준 데이터다.

- 공식 seed 음악
- 공식 플레이리스트
- 공식 플레이리스트 수록곡

반복 실행을 고려해 `WHERE NOT EXISTS`, `INSERT IGNORE`를 사용한다.

### 12.2 dummy.sql

로컬 개발과 수동 테스트용 데이터다.

- 활성 회원 4명
- 러닝 프로필
- 여러 BPM·장르의 음악
- 음악 평가
- 공식·공개·비공개 플레이리스트
- 플레이리스트 수록곡과 저장 관계

회원의 `password_hash`는 실제 로그인 가능한 해시가 아니라 DB 확인용 문자열임이 주석에 명시되어 있다.

### 12.3 신규 더미 데이터 권장안

`dummy.sql`에 아래 시나리오를 식별 가능한 자연키 기준으로 추가한다.

- 회원별 러닝 기록 2~3건
- 참여 가능한 `OPEN` 모임
- 정원이 찬 `OPEN` 모임
- `CLOSED`, `CANCELLED`, `COMPLETED` 상태 모임
- 모임 생성자와 일반 참여자
- 취소된 참여 이력

반복 실행 시 중복되지 않도록 `WHERE NOT EXISTS` 또는 적절한 UNIQUE 키와 `INSERT IGNORE`를 사용한다. 자동 초기 데이터가 아닌 기능 테스트용이므로 `data.sql`에는 추가하지 않는 편이 적절하다.

## 13. 신규 DDL 설계 방향

아래는 현재 요구와 기존 구조를 기준으로 한 설계 방향이며, 필드 계약이 확정되기 전 최종 DDL로 간주하지 않는다.

### 13.1 러닝 기록

권장 테이블명:

```text
running_records
```

최소 검토 필드:

```text
id
member_id
started_at 또는 running_date
distance
duration_seconds
average_pace_seconds (필요 시)
calories (필요 시)
memo
created_at
updated_at
deleted_at (정책 확정 시)
```

검증 후보:

- 거리 > 0
- 운동 시간 > 0
- 칼로리 >= 0
- 미래 실행 시각 허용 여부
- 평균 페이스를 입력받을지 거리·시간으로 계산할지

거리 단위와 DB 타입은 계약 전에 반드시 확정해야 한다. 예를 들어 km를 저장한다면 `DECIMAL`을 사용하고 Java에서는 `BigDecimal`을 사용하는 편이 부동소수점 오차를 피할 수 있다.

### 13.2 러닝 모임

권장 테이블명:

```text
running_sessions
```

최소 검토 필드:

```text
id
creator_id
title
description
meeting_at
location
capacity
status
created_at
updated_at
deleted_at (선택)
```

검증 후보:

- 제목 필수와 최대 길이
- 모임 일시는 생성 시점보다 미래
- 정원 최소 1 이상
- 수정·취소·삭제는 생성자만 가능
- 참여자가 있는 상태에서 정원을 현재 참여 인원보다 작게 줄일 수 없음
- 완료·취소된 모임 수정 제한

### 13.3 모임 참여

권장 테이블명:

```text
running_session_participants
```

최소 검토 필드:

```text
id
running_session_id
member_id
status
joined_at
cancelled_at
created_at
updated_at
```

필수 제약:

```text
UNIQUE (running_session_id, member_id)
```

검증 후보:

- `OPEN` 모임만 참여 가능
- 정원 미만일 때만 참여 가능
- 이미 `JOINED`면 중복 참여 오류
- 참여 내역이 없거나 이미 `CANCELLED`면 취소 오류
- 취소 후 재참여 허용 여부
- 생성자의 자동 참여 여부
- 생성자가 자신의 모임에 별도로 참여 가능한지

## 14. Service 구현 방향

### 14.1 RunningRecordService

예상 책임:

- 로그인 회원의 기록 생성
- 로그인 회원의 기록 목록 및 단건 조회
- 본인 기록만 수정
- 본인 기록만 삭제
- 입력값 검증

모든 소유권 검증은 요청 Domain의 `memberId`가 아니라 DB에서 조회한 원본 `memberId`와 Session 회원 ID를 비교한다.

### 14.2 RunningSessionService

예상 책임:

- 로그인 회원을 생성자로 설정해 모임 생성
- 목록·상세 조회
- 생성자만 수정
- 생성자만 취소 또는 삭제
- 상태 전이와 정원 변경 검증

### 14.3 RunningSessionParticipantService

예상 책임:

- Session 로그인 회원 참여
- 중복 참여 검증
- 모임 상태 검증
- 정원 검증
- 참여 취소
- 동시성 제어

참여 생성과 정원 검증은 하나의 쓰기 트랜잭션 안에서 처리한다.

## 15. 기존 기능 비침범 원칙

이번 구현에서 수정이 예상되는 기존 공통 파일은 아래로 제한한다.

- `src/main/resources/sql/schema.sql`
- `src/main/resources/sql/dummy.sql`
- `common/exception/ErrorCode.java`
- MySQL Mapper 통합 테스트를 확장할 경우 `MapperIntegrationTest.java`

신규 기능 파일은 기존 빈 `runningrecord`, `runningsession` 패키지와 Mapper 디렉터리 안에만 추가한다.

다음 영역은 수정하지 않는다.

- Controller
- Request/Response DTO
- JSP, Vue3, Router, API Module
- Swagger/API Tester 문서
- Spring Security, JWT
- Spotify OAuth/API/Export 구조
- 기존 MVP Service와 Mapper의 공개 계약

DTO 디렉터리는 이미 존재하지만 팀원 B 담당 범위이므로 `.gitkeep` 외 신규 DTO를 만들지 않는다.

## 16. 구현 전 확정이 필요한 계약

아래 항목은 현재 코드만으로 결정할 수 없다.

1. 러닝 기록의 필수 필드와 단위
   - 거리 km/m
   - 시간 초/분
   - 평균 페이스 저장 또는 계산
   - 칼로리 필수 여부
2. 러닝 기록 삭제가 물리 삭제인지 soft delete인지
3. 모임 생성자가 정원에 포함되는지
4. 생성자를 참여 테이블에도 자동 등록할지
5. 취소한 회원의 재참여 허용 여부
6. 참여 가능한 모임 상태의 정확한 정의
7. 모임 일시 경과 시 상태를 자동 변경할지 조회 시 판단할지
8. 모임 삭제와 `CANCELLED` 상태 변경을 동일하게 볼지
9. 공개 목록·상세 조회에 로그인 인증이 필요한지
10. 모임 수정 시 허용되는 필드와 상태 전이

이 값이 확정되지 않은 채 구현하면 팀원 B의 DTO/API 계약과 충돌할 가능성이 크다. Service 계약 문서 작성 전에 우선 합의해야 한다.

## 17. 권장 구현 순서

1. 필드·단위·상태·삭제 정책 확정
2. `schema.sql` 신규 3개 테이블 추가
3. Domain과 Enum 추가
4. ErrorCode 추가
5. Mapper Interface/XML 구현
6. Mapper 통합 테스트 확장
7. `AuthService`와 `HttpSession` 기반 Service 구현
8. Service 단위 테스트 구현
9. `dummy.sql` 시나리오 데이터 추가
10. 팀원 B용 Service 계약 문서 작성
11. `mvnw.cmd clean test` 실행

## 18. 구현 체크리스트

- [ ] 기존 9개 테이블과 이름·제약조건 충돌 없음
- [ ] Java 필드 camelCase, DB 컬럼 snake_case
- [ ] Enum 값과 DDL CHECK 값 완전 일치
- [ ] Mapper namespace와 Interface FQCN 일치
- [ ] Interface 메서드명과 XML id 일치
- [ ] `parameterType`, `resultMap`, generated key 설정 확인
- [ ] 인증 필요 Service가 `AuthService.getLoginMember(session)` 사용
- [ ] 외부 `memberId`, `creatorId`를 신뢰하지 않음
- [ ] 수정·삭제 시 DB 원본 기준 소유권 검증
- [ ] 참여 중복을 Service와 UNIQUE 제약조건에서 모두 차단
- [ ] 정원 검증에 동시성 제어 적용
- [ ] 모임 상태별 참여·취소 검증
- [ ] 쓰기 메서드에 `@Transactional`
- [ ] 팀원 B 담당 DTO/Controller 파일 미생성
- [ ] 기존 Spotify/MVP 코드 미수정
- [ ] Service 및 Mapper 테스트 추가
- [ ] `mvnw.cmd clean test` 통과

## 19. 현재 상태 판정

현재 프로젝트는 러닝 추가 기능을 구현하기 위한 패키지 골격과 공통 인프라가 준비되어 있다. 신규 기능이 기존 MVP 및 Spotify 구조와 직접 충돌하는 요소는 발견되지 않았다.

가장 중요한 구현 리스크는 다음 세 가지다.

1. 러닝 기록 필드와 모임 상태 계약이 아직 코드에 정의되어 있지 않다.
2. 기존 Service의 `Long loginMemberId` 인자 관례를 그대로 복제하면 이번 공통 인증 지시사항을 위반할 수 있다.
3. 모임 정원은 단순 조회 후 insert 방식으로 구현하면 동시 요청에서 초과될 수 있다.

따라서 필드 계약을 먼저 확정하고, 신규 Service는 Session 기반 로그인 회원 조회와 DB 잠금/UNIQUE 제약을 결합하는 방식으로 구현하는 것이 적절하다.

## 20. 사전 점검 테스트 결과

2026-06-20 기준 아래 명령을 실행했다.

```powershell
cd ssafit
.\mvnw.cmd clean test
```

결과:

- 전체 140개 테스트 실행
- 실패 0
- 오류 0
- skip 8
- `BUILD SUCCESS`

skip된 8개는 `SSAFIT_TEST_DB_URL` 환경 변수가 있을 때만 실행되는 `MapperIntegrationTest`다. 따라서 현재 단위 테스트와 Spring Context 기준선은 정상이며, 실제 MySQL Mapper 통합 기준선은 테스트 DB 환경에서 별도로 확인해야 한다.
