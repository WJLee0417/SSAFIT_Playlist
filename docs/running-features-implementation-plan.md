# P12.0 러닝 추가 기능 구현 계획

## 1. 문서 목적

현재 `feature/a-running-features-service` 브랜치의 프로젝트 구조를 분석하고, 기존 MVP 및 Spotify 기능을 침범하지 않으면서 아래 세 가지 추가 기능을 구현하기 위한 기준과 순서를 정리한다.

- 러닝 기록 CRUD
- 러닝 모임 CRUD 및 생성자 권한 검증
- 러닝 모임 참여/취소 및 정원·중복·상태 검증

이번 단계는 사전 점검과 구현 계획 문서 작성만 수행한다. DDL, Java, Mapper XML 등 실제 구현 파일은 수정하거나 생성하지 않는다.

## 2. 현재 구조 분석

### 2.1 프로젝트 루트

저장소 루트 아래 `ssafit/` 디렉터리가 실제 Maven 애플리케이션 루트다.

```text
SSAFIT_Playlist
├─ docs
├─ README.md
└─ ssafit
   ├─ pom.xml
   ├─ mvnw.cmd
   └─ src
```

확인된 기술 구성:

- Java 21
- Spring Boot 4.0.6
- MyBatis 4.0.1
- MySQL 8
- Session 기반 로그인 인증
- JUnit 5, Mockito, AssertJ

### 2.2 Domain 패키지 위치

기능별 Domain은 아래 위치에 작성되어 있다.

```text
ssafit/src/main/java/com/ssafy/ssafit/{feature}/domain
```

현재 주요 Domain 패키지:

- `member/domain`
- `runningprofile/domain`
- `music/domain`
- `musicreview/domain`
- `playlist/domain`
- `recommendation/domain`
- `spotify/domain`

Domain은 일반적으로 Lombok 기반 mutable POJO 형태다.

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Domain {
}
```

DB PK와 FK는 `Long`, 날짜·시간은 `LocalDateTime`, 상태값은 Domain 패키지의 Enum을 사용한다. DB 컬럼은 `snake_case`, Java 필드는 `camelCase`다.

러닝 추가 기능용 패키지는 이미 생성되어 있으나 현재 `.gitkeep`만 존재한다.

```text
com.ssafy.ssafit.runningrecord.domain
com.ssafy.ssafit.runningsession.domain
```

### 2.3 Mapper Interface/XML 위치

Mapper Interface 위치:

```text
ssafit/src/main/java/com/ssafy/ssafit/{feature}/mapper
```

Mapper XML 위치:

```text
ssafit/src/main/resources/mappers/{feature}
```

요청서에 언급된 `src/main/resources/mapper`가 아니라 실제 프로젝트 경로는 복수형인 `mappers`다.

러닝 기능용 경로는 이미 존재하지만 현재 `.gitkeep`만 있다.

```text
ssafit/src/main/java/com/ssafy/ssafit/runningrecord/mapper
ssafit/src/main/java/com/ssafy/ssafit/runningsession/mapper
ssafit/src/main/resources/mappers/runningrecord
ssafit/src/main/resources/mappers/runningsession
```

기존 Mapper 작성 방식:

- Interface에 `@Mapper` 사용
- 다중 인자에 `@Param` 사용
- 단건 조회는 `Optional<T>`
- 목록 조회는 `List<T>`
- insert, update, delete 결과는 `int`
- XML `namespace`는 Mapper Interface FQCN과 일치
- Interface 메서드명과 XML statement `id` 일치
- 생성 PK는 `useGeneratedKeys`, `keyProperty`, `keyColumn` 사용
- 명시적 `resultMap`으로 DB 컬럼과 Java 필드 매핑
- 반복 컬럼은 `<sql id="...Columns">`로 분리

MyBatis 설정:

```yaml
mybatis:
  mapper-locations: classpath:/mappers/**/*.xml
  type-aliases-package: com.ssafy.ssafit
  configuration:
    map-underscore-to-camel-case: true
```

### 2.4 Service 패키지 위치

Service는 아래 위치에 작성되어 있다.

```text
ssafit/src/main/java/com/ssafy/ssafit/{feature}/service
```

러닝 기능용 경로:

```text
com.ssafy.ssafit.runningrecord.service
com.ssafy.ssafit.runningsession.service
```

현재 두 패키지는 `.gitkeep`만 포함한다.

기존 Service 작성 방식:

- `@Service`
- Lombok `@RequiredArgsConstructor`
- 클래스에 `@Transactional(readOnly = true)`
- 쓰기 메서드에 `@Transactional`
- 비즈니스 오류는 `CustomException(ErrorCode)`로 변환
- 권한 검증은 Service 계층에서 수행
- Mapper가 조회한 원본 데이터의 소유자 ID를 기준으로 권한 확인

### 2.5 테스트 구조

Service 단위 테스트 위치:

```text
ssafit/src/test/java/com/ssafy/ssafit/{feature}/service
```

기존 Service 테스트 방식:

- JUnit 5
- `@ExtendWith(MockitoExtension.class)`
- Mapper와 협력 Service를 `@Mock`
- `@BeforeEach`에서 Service 생성
- AssertJ의 `assertThat`, `assertThatThrownBy`
- Mockito의 `verify`, `never`
- 발생한 `CustomException`의 `errorCode` 직접 검증

Mapper 통합 테스트:

```text
ssafit/src/test/java/com/ssafy/ssafit/mapper/MapperIntegrationTest.java
```

특징:

- 실제 MySQL 8을 사용하는 통합 테스트
- `@SpringBootTest`
- `@ActiveProfiles("mapper-test")`
- `SSAFIT_TEST_DB_URL` 환경 변수가 있을 때만 실행
- UNIQUE, FK, Enum 매핑, generated key, 트랜잭션 rollback 검증

테스트 설정:

```text
ssafit/src/test/resources/application-mapper-test.yml
ssafit/src/test/resources/README.md
```

2026-06-20 사전 점검 시 `mvnw.cmd clean test` 결과:

- 실행 140
- 실패 0
- 오류 0
- skip 8
- `BUILD SUCCESS`

skip된 8개는 테스트 DB 환경 변수가 필요한 `MapperIntegrationTest`다.

### 2.6 DDL 관리 방식

DDL 위치:

```text
ssafit/src/main/resources/sql/schema.sql
```

현재 `schema.sql`에는 아래 9개 테이블이 있다.

1. `members`
2. `running_profiles`
3. `music`
4. `music_reviews`
5. `playlists`
6. `playlist_items`
7. `playlist_saves`
8. `recommendation_logs`
9. `spotify_connections`

러닝 기록, 러닝 모임, 모임 참여 테이블은 아직 없다.

DDL 관리 원칙:

- `spring.sql.init.mode` 기본값은 `never`
- 운영 애플리케이션 시작 시 SQL이 자동 실행되지 않음
- MySQL Workbench 또는 CLI에서 직접 실행
- 기존 DB에 전체 `schema.sql`을 반복 실행하지 않음
- 기존 DB 변경이 필요하면 별도 마이그레이션 SQL을 준비
- PK, UK, FK, CHECK 제약조건에 명시적 이름 사용
- Enum은 `VARCHAR`와 CHECK 제약조건을 함께 사용
- 시간 컬럼은 `DATETIME(6)`
- 생성 시각은 `CURRENT_TIMESTAMP(6)`
- 수정 시각은 `ON UPDATE CURRENT_TIMESTAMP(6)` 사용

이번 문서 작성 단계에서는 DDL을 수정하지 않는다.

### 2.7 soft delete 적용 방식

soft delete는 모든 테이블의 공통 정책이 아니라 필요한 Domain에 선택적으로 적용되어 있다.

`members`:

- `status = 'WITHDRAWN'`
- `deleted_at IS NOT NULL`
- CHECK 제약조건으로 상태와 삭제 시각 정합성 검증

`playlists`:

- `deleted_at` 기록
- 조회 SQL에서 `deleted_at IS NULL` 조건 사용
- Mapper의 `softDelete` update로 삭제

관계 데이터인 `playlist_saves`는 취소 시 물리 삭제한다.

러닝 기능의 삭제 정책은 구현 전에 확정해야 한다.

- `running_records`: 일반 CRUD 요구만 기준으로 하면 물리 삭제 가능
- `running_sessions`: 참여 관계와 상태 이력을 고려하면 상태 기반 취소가 적합
- `running_session_participants`: 참여/취소 상태와 재참여 정책이 필요하면 상태 보존이 적합

### 2.8 Session 로그인 회원 ID 조회 방식

Session 로그인 키:

```java
SessionConst.LOGIN_MEMBER_ID
```

로그인 성공 시 `AuthService.login`이 Session에 회원 ID를 저장한다.

```java
session.setAttribute(SessionConst.LOGIN_MEMBER_ID, member.getId());
```

기존 조회 메서드:

```java
authService.getLoginMemberId(session);
authService.getLoginMember(session);
```

차이:

- `getLoginMemberId`는 Session에 저장된 값의 존재와 `Long` 타입을 검증한다.
- `getLoginMember`는 회원을 DB에서 다시 조회하고 활성 상태와 탈퇴 여부까지 검증한다.

신규 러닝 Service에서는 외부에서 전달받은 로그인 회원 ID를 신뢰하지 않고 다음 방식으로 처리할 예정이다.

```java
Member loginMember = authService.getLoginMember(session);
Long loginMemberId = loginMember.getId();
```

적용 원칙:

- 생성·수정·삭제·참여·취소 메서드는 Session 인증 흐름 사용
- 입력 Domain의 `memberId`, `creatorId`를 로그인 사용자 정보로 신뢰하지 않음
- 생성 시 로그인 회원 ID를 Service가 직접 설정
- 수정·삭제 시 DB 원본의 소유자 또는 생성자 ID와 로그인 회원 ID를 비교
- 권한 검증은 Controller가 아닌 Service에서 수행

## 3. 추가 기능 구현 대상

### 3.1 running_records

회원별 러닝 기록을 관리하는 테이블이다.

구현 범위:

- 러닝 기록 생성
- 로그인 회원의 기록 목록 조회
- 로그인 회원의 기록 상세 조회
- 본인 기록 수정
- 본인 기록 삭제
- 거리, 운동 시간, 러닝 시각 등 입력값 검증

구현 전 확정 사항:

- 거리 단위와 자료형
- 운동 시간 단위
- 평균 페이스 저장 또는 계산 여부
- 칼로리와 메모 필수 여부
- 물리 삭제 또는 soft delete

### 3.2 running_sessions

러닝 모임과 생성자 정보를 관리하는 테이블이다.

구현 범위:

- 러닝 모임 생성
- 모임 목록 및 상세 조회
- 생성자만 수정
- 생성자만 취소 또는 삭제
- 모임 상태 검증
- 정원 및 모임 일시 검증

생성자 ID는 외부 입력값이 아니라 Session 로그인 회원 ID로 설정한다.

### 3.3 running_session_participants

러닝 모임과 참여 회원의 관계를 관리하는 테이블이다.

구현 범위:

- 모임 참여
- 참여 취소
- 중복 참여 검증
- 정원 초과 검증
- 모임 상태 검증
- 참여하지 않은 회원의 취소 요청 검증

필수 DB 제약 후보:

```text
UNIQUE (running_session_id, member_id)
```

정원 검증과 참여 등록은 하나의 트랜잭션에서 수행해야 한다. 동시 참여 요청으로 정원을 초과하지 않도록 모임 행 잠금 또는 이에 준하는 동시성 제어가 필요하다.

## 4. 구현 예정 Domain 및 Enum

### 4.1 RunningRecord

위치:

```text
com.ssafy.ssafit.runningrecord.domain.RunningRecord
```

역할:

- 회원별 러닝 기록 표현
- `running_records` 컬럼 매핑

### 4.2 RunningSession

위치:

```text
com.ssafy.ssafit.runningsession.domain.RunningSession
```

역할:

- 러닝 모임 정보 표현
- 생성자, 모임 일시, 장소, 정원, 상태 관리

`jakarta.servlet.http.HttpSession`과 이름이 비슷하므로 같은 코드에서 import와 변수명을 명확하게 구분한다.

### 4.3 RunningSessionParticipant

위치:

```text
com.ssafy.ssafit.runningsession.domain.RunningSessionParticipant
```

역할:

- 모임과 참여 회원 관계 표현
- 참여 및 취소 상태 관리

### 4.4 RunningSessionStatus

위치:

```text
com.ssafy.ssafit.runningsession.domain.RunningSessionStatus
```

역할:

- 모임의 참여 가능 여부와 생명주기 표현

상태값 후보:

```text
OPEN
CLOSED
CANCELLED
COMPLETED
```

최종 상태값은 DDL CHECK 제약조건, Java Enum, Service 상태 검증, 테스트에서 동일하게 사용해야 한다.

별도의 참여 상태 Enum이 필요한지는 참여 취소와 재참여 정책을 확정한 후 결정한다.

## 5. 구현 예정 ErrorCode

### 5.1 기존 ErrorCode 확인 결과

현재 `ErrorCode` 전체를 확인한 결과 아래 코드는 존재하지 않는다.

- `RUNNING_RECORD_NOT_FOUND`
- `SESSION_NOT_FOUND`
- `SESSION_CLOSED`
- `SESSION_FULL`
- `DUPLICATE_SESSION_JOIN`

또한 `INVALID_INPUT`도 현재 프로젝트에 존재하지 않는다.

입력값 오류에 사용할 수 있는 기존 유사 ErrorCode:

- `INVALID_REQUEST`
- `INVALID_PROFILE_VALUE`
- `INVALID_BPM_VALUE`
- `INVALID_BPM_RANGE`
- `INVALID_SCORE`
- `INVALID_PLAYLIST_TITLE`
- `INVALID_PLAYLIST_ORDER`

러닝 기능의 일반 입력값 오류는 신규 `INVALID_INPUT`을 만들기보다 기존 공통 코드인 `INVALID_REQUEST`를 우선 재사용한다. 특정 오류를 API에서 명확히 구분해야 할 필요가 확인된 경우에만 러닝 기능 전용 ErrorCode를 검토한다.

### 5.2 추가 필요 ErrorCode

| ErrorCode | 권장 HTTP 상태 | 적용 상황 |
|---|---:|---|
| `RUNNING_RECORD_NOT_FOUND` | 404 | 러닝 기록이 없거나 삭제되어 조회할 수 없음 |
| `SESSION_NOT_FOUND` | 404 | 러닝 모임이 없거나 조회할 수 없음 |
| `SESSION_CLOSED` | 409 | 참여할 수 없는 상태의 모임 |
| `SESSION_FULL` | 409 | 현재 참여 인원이 정원에 도달함 |
| `DUPLICATE_SESSION_JOIN` | 409 | 동일 회원이 이미 모임에 참여 중임 |

권한 검증에는 기존 코드를 재사용한다.

- 로그인 필요: `AUTH_REQUIRED`
- 생성자 또는 소유자 권한 없음: `ACCESS_DENIED`
- 유효한 회원 없음: `MEMBER_NOT_FOUND`

ErrorCode 추가 전 동일 의미의 기존 코드가 새로 생기지 않았는지 다시 확인한다.

## 6. 단계별 구현 계획

### 6.1 DDL, Domain, Enum, ErrorCode

- `running_records` DDL 설계
- `running_sessions` DDL 설계
- `running_session_participants` DDL 설계
- PK, FK, UNIQUE, CHECK, INDEX 정책 반영
- `RunningRecord` 구현
- `RunningSession` 구현
- `RunningSessionParticipant` 구현
- `RunningSessionStatus` 구현
- 기존 ErrorCode 중복 확인 후 필요한 코드만 추가

### 6.2 러닝 기록 Mapper/Service

- `RunningRecordMapper` Interface 작성
- `RunningRecordMapper.xml` 작성
- 생성, 목록, 상세, 수정, 삭제 SQL 구현
- Session 로그인 회원 기준 소유권 검증
- 입력값 검증
- 조회 결과 없음과 권한 오류 구분
- 쓰기 메서드 트랜잭션 적용

### 6.3 러닝 모임 Mapper/Service

- `RunningSessionMapper` Interface 작성
- `RunningSessionMapper.xml` 작성
- 모임 생성, 목록, 상세, 수정, 취소 또는 삭제 구현
- 생성자 ID를 Session 로그인 회원으로 설정
- 수정·삭제 시 DB 원본 기준 생성자 권한 검증
- 모임 상태와 정원 변경 규칙 검증

### 6.4 모임 참여 Mapper/Service

- `RunningSessionParticipantMapper` Interface 작성
- `RunningSessionParticipantMapper.xml` 작성
- 참여, 취소, 참여 여부, 현재 참여 인원 조회 구현
- 중복 참여 검증
- 모임 상태 검증
- 정원 검증
- UNIQUE 제약 위반을 `DUPLICATE_SESSION_JOIN`으로 변환
- 트랜잭션과 DB 잠금을 이용한 정원 동시성 제어

### 6.5 테스트 및 더미 데이터

- `RunningRecordServiceTest`
- `RunningSessionServiceTest`
- `RunningSessionParticipantServiceTest`
- Mapper 통합 테스트 추가
- generated key와 Enum 매핑 검증
- 본인 및 생성자 권한 검증
- 정원, 중복, 상태별 참여 검증
- 동시성 또는 잠금 쿼리 검증
- FK, UNIQUE, rollback 검증
- `dummy.sql`에 러닝 기록과 상태별 모임·참여 시나리오 추가
- `mvnw.cmd clean test` 최종 통과 확인

### 6.6 팀원 B 연동 계약 문서 및 최종 검수

- Controller에서 호출할 Service 메서드 시그니처 정리
- 각 메서드의 Session 필요 여부 기록
- 입력 Domain 필드와 Service가 강제로 설정하는 필드 구분
- 반환 Domain과 조회 정책 기록
- 발생 가능한 ErrorCode 정리
- 모임 상태 전이와 참여 가능 조건 기록
- 수정·삭제·참여·취소 권한 정책 기록
- Mapper Interface/XML namespace, id, parameterType, resultMap 최종 대조
- 기존 MVP 및 Spotify 테스트 회귀 확인

## 7. 구현 전 확정 필요 사항

1. 러닝 기록 거리와 시간의 단위
2. 러닝 기록의 필수 필드
3. 러닝 기록 삭제 정책
4. 모임 생성자를 정원에 포함할지
5. 생성자를 참여 테이블에 자동 등록할지
6. 취소한 회원의 재참여를 허용할지
7. 모임 삭제와 `CANCELLED` 상태를 동일하게 볼지
8. 모임 일시가 지나면 상태를 자동 변경할지
9. 공개 목록과 상세 조회에 로그인이 필요한지
10. 참여 상태 Enum을 별도로 둘지

## 8. 이번 문서 작업의 금지 사항

이번 P12.0 사전 점검 단계에서는 아래 작업을 수행하지 않는다.

- DDL 수정 금지
- Java 코드 생성 금지
- Mapper XML 작성 금지
- Controller 생성 금지
- Request DTO 생성 금지
- Response DTO 생성 금지
- 기존 코드 리팩토링 금지
- JSP, Vue3, Vue Router, Vue API Module 수정 금지
- Swagger/API Tester 문서 작성 금지
- Spring Security, JWT 도입 금지
- 기존 Spotify OAuth/API/Export 구조 수정 금지

이번 단계에서 변경하는 파일은 본 구현 계획 문서 하나로 제한한다.

## 9. 구현 체크리스트

- [ ] 신규 테이블과 기존 테이블 이름 충돌 확인
- [ ] Java 필드 camelCase, DB 컬럼 snake_case 적용
- [ ] Enum과 DDL CHECK 값 일치
- [ ] 기존 ErrorCode 중복 재확인
- [ ] Mapper Interface와 XML namespace·id 일치
- [ ] `parameterType`, `resultMap`, generated key 설정 확인
- [ ] `AuthService.getLoginMember(session)` 기반 인증 적용
- [ ] 외부 `memberId`, `creatorId`를 로그인 정보로 신뢰하지 않음
- [ ] DB 원본 기준 소유자·생성자 권한 검증
- [ ] Service와 DB에서 중복 참여를 함께 차단
- [ ] 정원 검증에 동시성 제어 적용
- [ ] 상태별 참여·취소 검증
- [ ] Controller와 DTO 미생성
- [ ] 기존 MVP 및 Spotify 코드 미침범
- [ ] `mvnw.cmd clean test` 통과

## 10. 커밋 메시지 제안

기존 구현 계획 문서를 요청된 구조와 명칭에 맞게 수정했다는 의미를 담아 다음 메시지를 권장한다.

```text
docs: revise running features implementation plan
```
