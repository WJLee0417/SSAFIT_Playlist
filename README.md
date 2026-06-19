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
