# MySQL Mapper 통합 테스트

`MapperIntegrationTest`는 실제 MySQL 8에서 검색, 정렬, JOIN, 제약조건,
트랜잭션 롤백과 러닝 모임 참여 동시성을 검증한다.

통합 테스트는 더 이상 환경변수 미설정을 이유로 조용히 스킵되지 않는다. 전체
Backend 테스트를 실행하려면 다음 세 환경변수를 모두 설정해야 한다.

```powershell
$env:SSAFIT_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:33306/ssafit_mapper_test?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true'
$env:SSAFIT_TEST_DB_USERNAME = 'ssafit_test'
$env:SSAFIT_TEST_DB_PASSWORD = '<TEST_ONLY_PASSWORD>'

.\mvnw.cmd -Dtest=MapperIntegrationTest test
.\mvnw.cmd clean test
```

안전 조건:

- JDBC URL은 `jdbc:mysql://`로 시작해야 한다.
- URL의 DB 이름에 `test`가 포함되어야 한다.
- username과 password를 명시해야 한다.
- 실제 연결 DB가 URL에서 검증한 DB명과 일치해야 한다.
- root 계정으로는 실행할 수 없다.

`mapper-test` 프로필은 테스트 시작 시 전용 DB의 알려진 테이블만 FK 역순으로
삭제하고 `sql/schema.sql`로 다시 생성한다. 각 테스트 전후에도 모든 테스트 행을
삭제한다. 따라서 이 프로필에는 개발 DB나 운영 DB URL을 절대 지정하지 않는다.
