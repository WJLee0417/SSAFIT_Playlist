# SSAFIT PlayList

## 개발 환경

- STS 5.0.1
- Java 21
- Spring Boot 4.0.6
- Maven Wrapper
- MySQL 8
- MyBatis
- Session 기반 인증
- Spotify OAuth 환경 변수 사용

## 프로젝트 실행 위치

Maven 프로젝트는 저장소의 `ssafit` 디렉터리에 있습니다.

```bash
cd ssafit
```

## 공통 구현 기준

### 인코딩

- Java, XML, SQL, YAML, Properties, Markdown 파일은 UTF-8로 작성합니다.
- Maven 빌드 및 리포트 출력 인코딩은 UTF-8을 사용합니다.
- STS에서 `Workspace`와 프로젝트의 Text file encoding을 UTF-8로 설정합니다.
- Git의 텍스트 파일 줄바꿈은 LF를 기본으로 사용하고, Windows 배치 파일만 CRLF를 사용합니다.

### 명명 규칙

- DB 테이블명은 `snake_case`를 사용합니다.
- DB 컬럼명은 `snake_case`를 사용합니다.
- Java Domain 필드명은 `camelCase`를 사용합니다.
- MyBatis는 `map-underscore-to-camel-case: true`를 기본으로 사용합니다.
- 복잡한 조인이나 별칭이 필요한 조회는 명시적인 `resultMap`을 사용합니다.
- Mapper Interface의 메서드명과 Mapper XML의 `id`를 일치시킵니다.
- Mapper XML의 `namespace`는 Mapper Interface의 FQCN과 일치시킵니다.
- Session 로그인 회원 ID 키는 `SessionConst.LOGIN_MEMBER_ID`를 사용합니다.

## 현재 브랜치 구현 범위

현재 `feature/a-db-service` 브랜치에서는 다음 내부 영역만 구현합니다.

- DB DDL과 제약조건
- Domain과 Enum
- MyBatis Mapper Interface/XML
- Service 비즈니스 로직
- Session 기반 인증/인가와 권한 검증
- 규칙 기반 자동 큐레이션 내부 로직
- Spotify 관련 DB 컬럼과 내부 저장 구조

다음 항목은 현재 브랜치에서 구현하지 않습니다.

- Controller
- Request/Response DTO
- JSP와 Vue3 화면
- Spring Security와 JWT
- 실제 Spotify API 호출
- Spotify OAuth 실제 처리
- Spotify Export 실제 연동
- 실제 AI API 추천

Controller와 Request/Response DTO는 팀원 B 담당입니다. 실제 Spotify API, OAuth,
Export 연동은 이후 `feature/a-spotify-service` 브랜치에서 구현합니다.

## 로컬 DB 생성

MySQL 8에서 다음 SQL을 직접 실행합니다. `application.yml`의 SQL 자동 실행은
비활성화되어 있습니다.

```sql
CREATE DATABASE ssafit_playlist
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'ssafit'@'localhost' IDENTIFIED BY 'ssafit1234!';
GRANT ALL PRIVILEGES ON ssafit_playlist.* TO 'ssafit'@'localhost';
FLUSH PRIVILEGES;
```

collation 오류가 발생하면 다음 설정을 사용합니다.

```sql
CREATE DATABASE ssafit_playlist
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;
```

위 비밀번호는 로컬 개발용 기본값입니다. 실제 운영 비밀번호나 개인 비밀정보는
저장소에 커밋하지 마세요.

## Spotify 환경 변수

STS의 Run Configurations에서 다음 환경 변수를 등록합니다.

```text
SPOTIFY_CLIENT_ID=발급받은_Client_ID
SPOTIFY_CLIENT_SECRET=발급받은_Client_Secret
```

Spotify Developer Dashboard에는 다음 Redirect URI를 등록합니다.

```text
http://localhost:8080/api/spotify/oauth/callback
```

## 실행 확인

Windows:

```bash
mvnw.cmd clean test
mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

애플리케이션 실행 후 다음 주소에서 `OK` 응답을 확인합니다.

```text
http://localhost:8080/health
```
