# MySQL Mapper 통합 테스트

`MapperIntegrationTest`는 실제 MySQL 8의 검색, 정렬, 페이징, JOIN, UNIQUE,
FK 및 트랜잭션 롤백을 검증한다.

운영 데이터 오염을 막기 위해 기본 `mvnw.cmd clean test` 실행에서는 통합 테스트를
건너뛴다. 아래 환경 변수를 전용 테스트 DB로 지정한 경우에만 실행된다.

```powershell
$env:SSAFIT_TEST_DB_URL = 'jdbc:mysql://localhost:3306/ssafit_playlist_test?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true'
$env:SSAFIT_TEST_DB_USERNAME = 'ssafit'
$env:SSAFIT_TEST_DB_PASSWORD = 'test-password'
.\mvnw.cmd clean test
```

테스트 계정에는 `ssafit_playlist_test` 데이터베이스에 대한 권한만 부여한다.
통합 테스트는 각 테스트 전후 모든 행을 삭제하므로 실제 개발/운영 DB URL을
지정하면 안 된다.
