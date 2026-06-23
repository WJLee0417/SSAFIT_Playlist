# SSAFIT PlayList

## 개발 환경

- STS 5.0.1
- Java 25
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

## Java 25 및 Maven 실행 환경

Backend는 Java 25 문법과 Java 25 컴파일 대상을 사용합니다. JDK 설치 여부와 별개로,
Maven Wrapper를 실행하는 JVM도 반드시 Java 25여야 합니다. Java 25가 아닌 JVM에서는
Maven `validate` 단계에서 소스 컴파일 전에 빌드가 중단됩니다.

Windows PowerShell에서 현재 터미널 세션에 JDK 25를 지정하는 예시:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

java -version
Set-Location .\ssafit
.\mvnw.cmd -version
```

두 버전 확인 결과 모두 `25`를 표시해야 합니다. 특히 `.\mvnw.cmd -version`의
`Java version`, `runtime` 항목이 Maven이 실제로 사용하는 JVM입니다.

- `JAVA_HOME`은 JDK 25 설치 디렉터리를 가리켜야 합니다.
- `PATH`에서는 `%JAVA_HOME%\bin`이 낮은 Java 버전보다 먼저 와야 합니다.
- STS에서는 Installed JRE, Project JRE와 Maven 실행 JRE를 Java 25로 지정합니다.
- IntelliJ IDEA에서는 Project SDK뿐 아니라 Maven Runner와 Maven Importer JDK도
  Java 25로 지정합니다.
- IDE 터미널의 Java와 IDE가 내부적으로 Maven을 실행하는 Java는 다를 수 있으므로,
  IDE Maven 실행 로그 또는 Maven 설정 화면에서도 Java 25를 확인합니다.

Backend와 Frontend 실행 환경은 서로 독립적입니다.

```powershell
# Backend: JDK 25 필요
Set-Location .\ssafit
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run

# Frontend: 별도 PowerShell에서 Node.js/pnpm 사용
Set-Location .\frontend
pnpm install --frozen-lockfile
pnpm dev
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

## 구현 범위와 협업 경계

`feature/a-spotify-service` 브랜치에는 다음 내부 영역이 구현되어 있습니다.

- DB DDL과 제약조건
- Domain과 Enum
- MyBatis Mapper Interface/XML
- Service 비즈니스 로직
- Session 기반 인증/인가와 권한 검증
- 규칙 기반 자동 큐레이션 내부 로직
- Spotify 곡 검색과 선택 곡 저장
- Spotify OAuth 연결과 Token 갱신
- Spotify 플레이리스트 Export와 상태 저장

다음 항목은 팀원 B 담당 영역이므로 이 브랜치에서 구현하거나 임의 수정하지 않습니다.

- Controller
- Request/Response DTO
- JSP와 Vue3 화면
- Vue API Module
- Spring Security와 JWT

팀원 B는 [Spotify Service 연동 계약](docs/spotify-service-contract.md)을 기준으로
Controller, DTO, Vue 화면을 연결합니다.

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

### 1. 환경 변수 준비

저장소 루트의 `.env.example`을 참고해 STS Run Configurations 또는 로컬 실행
환경에 다음 변수를 등록합니다.

```text
SPOTIFY_CLIENT_ID=<Spotify Developer Dashboard에서 발급>
SPOTIFY_CLIENT_SECRET=<Spotify Developer Dashboard에서 발급>
SPOTIFY_REDIRECT_URI=http://127.0.0.1:8080/api/spotify/oauth/callback
```

- 실제 값은 IDE Run Configuration, OS 환경 변수 또는 별도 비밀 저장소에만 둡니다.
- 실제 Client ID, Secret, Token을 `.env.example`, `application.yml`, README,
  테스트 코드에 넣지 않습니다.
- 프로젝트는 `.env` 파일을 자동으로 읽지 않으므로 `.env`를 사용할 경우 IDE나
  별도 로더를 통해 환경 변수로 주입해야 합니다.

### 2. Redirect URI 등록

Spotify Developer Dashboard에는 `SPOTIFY_REDIRECT_URI`와 완전히 동일한 Redirect
URI를 등록합니다.

```text
http://127.0.0.1:8080/api/spotify/oauth/callback
```

프로토콜, 호스트, 포트, 경로와 마지막 슬래시까지 모두 일치해야 합니다.

### 3. 필요한 Spotify scope

```text
user-read-private
playlist-modify-private
playlist-modify-public
```

### 4. 보안 기준

- Access Token, Refresh Token, Authorization Code, Client Secret은 API 응답과
  로그에 출력하지 않습니다.
- Spotify 연결 상태 응답에 `SpotifyConnection` Domain을 그대로 노출하지 않습니다.
- Vue에서 Access Token을 저장하거나 Spotify API를 직접 호출하지 않습니다.

상세 입력·출력 및 ErrorCode 계약은
[Spotify Service 연동 계약](docs/spotify-service-contract.md)을 확인합니다.

## Spotify Export 상태 정책

- Export는 `NOT_EXPORTED → PENDING → SUCCESS` 순서로 진행합니다.
- 외부 API 실패 시 `PENDING → FAILED`로 변경하고
  `spotify_export_error_code`에는 민감정보가 없는 ErrorCode 이름만 저장합니다.
- Spotify Playlist 생성 후 수록곡 추가가 실패하면 생성된 Playlist ID와 URL을
  보존합니다.
- `FAILED` 상태에서 Spotify Playlist ID가 없을 때만 자동 재시도할 수 있습니다.
- Spotify Playlist ID가 남은 부분 실패는 중복 플레이리스트와 중복 수록곡 생성을
  막기 위해 자동 재시도하지 않습니다.
- `SUCCESS` 상태를 다시 Export하면 `SPOTIFY_ALREADY_EXPORTED`가 발생합니다.
- URI가 없는 수록곡이 하나라도 있으면 일부 곡을 조용히 제외하지 않고 Export를
  중단합니다.

기존 로컬 DB에는 다음 컬럼을 추가해야 합니다.

```sql
ALTER TABLE playlists
ADD COLUMN spotify_export_error_code VARCHAR(50) NULL
AFTER spotify_export_status;
```

## Spotify 문서

- [팀원 B 작업 인수인계서](docs/team-b-handover.md)
- [팀원 B용 Service 연동 계약](docs/spotify-service-contract.md)
- [실제 Spotify 계정 수동 통합 체크리스트](docs/spotify-manual-integration-checklist.md)

## 실행 확인

Windows:

```bash
mvnw.cmd clean test
mvnw.cmd -o clean test
mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw clean test
./mvnw -o clean test
./mvnw spring-boot:run
```

오프라인 테스트는 필요한 Maven 의존성이 로컬 캐시에 한 번 이상 내려받아진 이후
실행할 수 있습니다. Spotify 관련 JUnit 테스트는 Mock 기반이며 실제 Spotify API를
호출하지 않습니다.

애플리케이션 실행 후 다음 주소에서 `OK` 응답을 확인합니다.

```text
http://localhost:8080/health
```
