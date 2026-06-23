# MySQL Mapper 통합 테스트 검증 보고서

## 1. 검증 개요

- 검증일: 2026-06-23
- Java: STS 5.1.1 내장 Eclipse Adoptium 25.0.2
- Maven: Maven Wrapper 3.9.16
- MySQL: 공식 `mysql:8.0.45` Docker 이미지
- 테스트 DB: `ssafit_mapper_test`
- 호스트 포트: `33306`
- 트랜잭션 격리 수준: `REPEATABLE-READ`
- 테이블 엔진: InnoDB

호스트의 개발 MySQL에는 `ssafit_playlist` DB만 존재했고 현재 계정은 해당 개발
DB에만 권한이 있었다. 개발 DB를 사용하지 않고 Docker의 별도 MySQL 서버와 별도
포트, 전용 DB, 전용 계정을 구성했다.

## 2. 기존 Skip 원인

`MapperIntegrationTest`에는 `@EnabledIfEnvironmentVariable` 조건이 있었다.
`SSAFIT_TEST_DB_URL`이 없거나 정규식과 일치하지 않으면 13개 테스트 전체가
JUnit 조건부 스킵으로 처리됐다.

기존 `application-mapper-test.yml`에는 username과 password 기본값이 있어
접속 정보 누락이 명확히 드러나지 않았고, SQL 초기화의
`continue-on-error: true`는 기존 스키마와 현재 `schema.sql`의 차이를 숨길 수
있었다.

## 3. 테스트 DB 안전장치

### 연결 전 검증

`TestDatabaseGuard`가 테스트 클래스 초기화 시점에 다음을 검증한다.

- `SSAFIT_TEST_DB_URL`, `SSAFIT_TEST_DB_USERNAME`,
  `SSAFIT_TEST_DB_PASSWORD` 모두 필수
- JDBC URL은 `jdbc:mysql://` 형식만 허용
- URL의 DB 이름에 `test`가 반드시 포함
- 검증이 끝난 값만 `DynamicPropertySource`로 DataSource에 전달

따라서 개발 DB 이름을 가진 URL은 네트워크 연결과 스키마 초기화 전에 실패한다.

### 연결 후 검증

- `SELECT DATABASE()` 결과와 URL에서 검증한 DB 이름의 정확한 일치 확인
- 실제 DB 이름에 `test` 포함 여부 재확인
- `CURRENT_USER()`가 root 계정이면 거부
- `SHOW GRANTS`에 전용 테스트 DB 외 권한이 있으면 거부

### 계정 격리

실제 사용한 전용 계정 권한:

```text
GRANT USAGE ON *.* TO `ssafit_test`@`%`
GRANT ALL PRIVILEGES ON `ssafit_mapper_test`.* TO `ssafit_test`@`%`
```

### 스키마와 데이터 격리

- 테스트 Context 시작 시 `schema-reset.sql`이 알려진 12개 테이블만 FK 역순으로 제거
- 이어서 운영 스키마 원본 `sql/schema.sql`로 테이블 재생성
- SQL 오류를 무시하지 않도록 `continue-on-error: false`
- 각 테스트 전후 및 클래스 전후에 모든 테스트 데이터를 FK 역순 삭제

## 4. 안전장치 검증

개발 DB URL을 사용한 검증 명령:

```powershell
$env:SSAFIT_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3306/ssafit_playlist?useSSL=false'
$env:SSAFIT_TEST_DB_USERNAME = 'ssafit'
$env:SSAFIT_TEST_DB_PASSWORD = '<REDACTED>'
.\mvnw.cmd -Dtest=MapperIntegrationTest test
```

실제 실패 메시지:

```text
Refusing to run Mapper integration tests against database 'ssafit_playlist'.
The database name in SSAFIT_TEST_DB_URL must contain 'test'.
```

DataSource 연결 및 테이블 초기화 전에 클래스 초기화 단계에서 차단됐다.

환경변수 누락 시 메시지:

```text
Mapper integration tests require environment variable SSAFIT_TEST_DB_URL.
Configure a dedicated MySQL database whose name contains 'test';
integration tests are not skipped.
```

## 5. 실제 실행 명령

```powershell
$env:JAVA_HOME = '<STS 5.1.1 JustJ Java 25 경로>'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:SSAFIT_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:33306/ssafit_mapper_test?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true'
$env:SSAFIT_TEST_DB_USERNAME = 'ssafit_test'
$env:SSAFIT_TEST_DB_PASSWORD = '<REDACTED>'

Set-Location .\ssafit
.\mvnw.cmd -Dtest=MapperIntegrationTest test
.\mvnw.cmd clean test
```

## 6. Mapper 통합 테스트 결과

```text
Tests run: 13
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

실제 MySQL 기준으로 다음을 검증했다.

- 러닝 기록 등록, 목록/상세 조회, 수정, soft delete
- 다른 회원 목록에서 소유자 기록이 조회되지 않는지 확인
- 러닝 모임 생성, 수정, 상태 변경, 취소와 잠금 조회
- 참여 등록, 취소, 재참여와 DB UNIQUE 중복 차단
- Service 기준 중복 참여, 모집 종료, 정원 초과 차단
- 정원 1명/2명 환경의 동시 참여에서 정원 초과 방지
- 참여 Mapper가 회원 이메일, 로그인 ID 등 개인정보를 조회하지 않는 구조 확인
- FK, CHECK, UNIQUE 제약과 트랜잭션 롤백
- 기존 회원, 음악, 리뷰, 플레이리스트, 추천, Spotify Mapper 회귀 검증

## 7. 전체 Backend 테스트 결과

```text
Tests run: 259
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
Total time: 37.046 s
```

## 8. 테스트 종료 후 정리 확인

전체 테스트 종료 후 다음 테이블의 행 수를 실제 MySQL에서 확인했다.

```text
members                       0
running_profiles              0
running_records               0
running_sessions              0
running_session_participants  0
music                         0
music_reviews                 0
playlists                     0
playlist_items                0
playlist_saves                0
recommendation_logs           0
spotify_connections           0
```

테스트 데이터가 남지 않았으며 호스트 개발 DB는 사용하거나 초기화하지 않았다.

## 9. 변경 파일

- `ssafit/src/test/java/com/ssafy/ssafit/mapper/TestDatabaseGuard.java`
- `ssafit/src/test/java/com/ssafy/ssafit/mapper/TestDatabaseSafetyTest.java`
- `ssafit/src/test/java/com/ssafy/ssafit/mapper/MapperIntegrationTest.java`
- `ssafit/src/test/resources/application-mapper-test.yml`
- `ssafit/src/test/resources/sql/schema-reset.sql`
- `ssafit/src/test/resources/README.md`
- `docs/mysql-integration-test-guide.md`
- `docs/verification/mysql-mapper-integration-test-report.md`

운영 API, 임시 Controller, 인증 우회 및 비즈니스 규칙 완화는 추가하지 않았다.
