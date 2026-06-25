# MySQL 통합 테스트 실행 가이드

## 1. 목적

`MapperIntegrationTest`를 실제 MySQL 8 서버에서 실행하여 MyBatis SQL, DDL 제약조건, 트랜잭션 롤백 및 러닝 모임 참여 동시성을 검증한다.

H2, SQLite 및 Mock DB는 사용하지 않는다. 운영 DB, 공용 개발 DB 또는 develop DB에도 연결하지 않는다.

## 2. 사전 요구사항

- Java 25
- MySQL 8
- Maven Wrapper
- 별도의 테스트 DB와 테스트 전용 계정
- 테스트 DB 이름에 `test` 또는 `local_test` 포함

현재 프로젝트에는 Flyway와 Liquibase가 없다. `mapper-test` 프로필에서 Spring SQL 초기화가 `classpath:sql/schema.sql`을 실행한다.

관련 설정:

- `ssafit/src/test/resources/application-mapper-test.yml`
- `ssafit/src/main/resources/sql/schema.sql`
- `ssafit/src/test/java/com/ssafy/ssafit/mapper/MapperIntegrationTest.java`

## 3. 테스트 DB 생성

관리자 계정으로 MySQL에 접속한 뒤 테스트 전용 DB와 계정을 생성한다.

```sql
CREATE DATABASE ssafit_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'ssafit_test'@'localhost'
    IDENTIFIED BY '<TEST_ONLY_PASSWORD>';

GRANT ALL PRIVILEGES ON ssafit_test.*
    TO 'ssafit_test'@'localhost';

FLUSH PRIVILEGES;
```

테스트 계정에는 `ssafit_test` 외 다른 DB 권한을 부여하지 않는다.

## 4. 환경변수

필수 또는 사용 가능한 환경변수는 다음과 같다.

| 변수 | 필수 여부 | 설명 |
| --- | --- | --- |
| `SSAFIT_TEST_DB_URL` | 필수 | 실제 MySQL 테스트 DB JDBC URL |
| `SSAFIT_TEST_DB_USERNAME` | 필수 | 테스트 전용 DB 계정 |
| `SSAFIT_TEST_DB_PASSWORD` | 필수 | 테스트 전용 DB 비밀번호 |

세 환경변수 중 하나라도 없으면 Mapper 통합 테스트는 스킵되지 않고 명확한
오류로 실패한다. 접속 정보의 기본값과 하드코딩된 비밀번호는 사용하지 않는다.

권장 URL:

```text
jdbc:mysql://127.0.0.1:3306/ssafit_test?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
```

## 5. Windows CMD 실행

```bat
set SSAFIT_TEST_DB_URL=jdbc:mysql://127.0.0.1:3306/ssafit_test?useSSL=false^&serverTimezone=Asia/Seoul^&characterEncoding=UTF-8^&allowPublicKeyRetrieval=true
set SSAFIT_TEST_DB_USERNAME=ssafit_test
set SSAFIT_TEST_DB_PASSWORD=<TEST_ONLY_PASSWORD>

cd ssafit
mvnw.cmd clean test
```

## 6. PowerShell 실행

```powershell
$env:SSAFIT_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3306/ssafit_test?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true'
$env:SSAFIT_TEST_DB_USERNAME = 'ssafit_test'
$env:SSAFIT_TEST_DB_PASSWORD = '<TEST_ONLY_PASSWORD>'

Set-Location ssafit
.\mvnw.cmd clean test
```

## 7. Docker를 사용한 격리 MySQL 예시

호스트 MySQL과 충돌하지 않는 포트를 사용한다.

```powershell
docker run -d `
  --name ssafit-mysql-integration-test `
  -p 33306:3306 `
  -e MYSQL_ROOT_PASSWORD='<ROOT_TEST_PASSWORD>' `
  -e MYSQL_DATABASE='ssafit_test' `
  -e MYSQL_USER='ssafit_test' `
  -e MYSQL_PASSWORD='<TEST_ONLY_PASSWORD>' `
  mysql:8.0.32
```

Docker 사용 시 JDBC URL의 포트는 `33306`으로 설정한다.

## 8. 테스트 DB 안전장치

현재 테스트에는 다음 안전장치가 적용되어 있다.

1. Spring Context와 DataSource 생성 전에 세 환경변수의 존재를 검증한다.
2. JDBC URL이 MySQL 형식이고 DB 이름에 `test`가 있는지 연결 전에 검증한다.
3. `MapperIntegrationTest`는 실제 연결 후 `SELECT DATABASE()` 결과가 URL에서
   검증한 DB 이름과 정확히 일치하는지 다시 확인한다.
4. root 계정 연결은 거부한다.
5. `SHOW GRANTS` 결과에 전용 테스트 DB 외 권한이 있으면 실행을 거부한다.
6. 테스트 프로필은 전용 DB 내부의 알려진 테이블만 재생성한다.
7. 각 테스트 전후와 전체 클래스 전후에 FK 역순으로 테스트 데이터를 삭제한다.

DB 이름이 `ssafit_playlist`처럼 `test`를 포함하지 않으면 테스트 실행을 중단해야 한다.

예상 오류:

```text
Refusing to run Mapper integration tests against database 'ssafit_playlist'.
The database name in SSAFIT_TEST_DB_URL must contain 'test'.
```

## 9. 테스트 실행 결과 확인

성공적인 MySQL 통합 테스트는 다음 조건을 만족해야 한다.

```text
MapperIntegrationTest
Tests run: 13
Failures: 0
Errors: 0
Skipped: 0
```

통합 테스트만 실행:

```powershell
.\mvnw.cmd -Dtest=MapperIntegrationTest test
```

전체 테스트 실행:

```powershell
.\mvnw.cmd clean test
```

전체 결과도 `Failures: 0`, `Errors: 0`, `Skipped: 0`, `BUILD SUCCESS`여야 한다.

## 10. 실패 시 확인 방법

1. 실패한 테스트 메서드 이름을 확인한다.
2. `ssafit/target/surefire-reports`의 결과를 확인한다.
3. MySQL 오류 코드와 SQL State를 확인한다.
4. 실제 연결 DB를 확인한다.

```sql
SELECT VERSION(), DATABASE(), CURRENT_USER(), @@transaction_isolation;
```

5. 관련 테이블 엔진이 InnoDB인지 확인한다.

```sql
SELECT TABLE_NAME, ENGINE
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE();
```

6. 실패 원인을 Mapper SQL, DDL 제약, 데이터 초기화, 트랜잭션, 잠금 또는 환경 설정으로 분류한다.

테스트를 통과시키기 위해 Skip 조건을 추가하거나 assertion을 제거해서는 안 된다.

## 11. 테스트 데이터 정리

`schema-reset.sql`은 테스트 Application Context 시작 시 알려진 테스트 테이블을
FK 역순으로 삭제한 후 운영 스키마 원본인 `schema.sql`을 다시 실행한다.
`MapperIntegrationTest`는 추가로 `@BeforeAll`, `@BeforeEach`, `@AfterEach`,
`@AfterAll`에서 테스트 테이블의 데이터를 FK 역순으로 삭제한다.

테스트 종료 후 다음 SQL로 잔존 데이터를 확인할 수 있다.

```sql
SELECT COUNT(*) FROM running_session_participants;
SELECT COUNT(*) FROM running_sessions;
SELECT COUNT(*) FROM running_records;
SELECT COUNT(*) FROM spotify_connections;
SELECT COUNT(*) FROM recommendation_logs;
SELECT COUNT(*) FROM playlist_saves;
SELECT COUNT(*) FROM playlist_items;
SELECT COUNT(*) FROM playlists;
SELECT COUNT(*) FROM music_reviews;
SELECT COUNT(*) FROM music;
SELECT COUNT(*) FROM running_profiles;
SELECT COUNT(*) FROM members;
```

Docker 컨테이너가 더 이상 필요하지 않다면 테스트 완료 후 제거한다.

```powershell
docker rm -f ssafit-mysql-integration-test
```
