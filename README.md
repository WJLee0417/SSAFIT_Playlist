# SSAFIT PlayList

## 다른 PC에서 Docker로 실행하기

이 프로젝트는 Docker Compose로 `mysql`, `backend`, `frontend` 서비스를 한 번에 실행합니다.
다른 PC에서는 Java, Node.js, MySQL을 따로 설치하지 않아도 됩니다. Docker Desktop과 Git만 있으면 됩니다.

### 1. 준비물

- Docker Desktop 설치 및 실행
- Git 설치
- Spotify 기능을 테스트할 경우 Spotify 개발자 대시보드 접근 권한

### 2. 프로젝트 받기

Windows PowerShell:

```powershell
git clone <repository-url>
Set-Location SSAFIT_Playlist
Copy-Item .env.example .env
```

macOS/Linux:

```bash
git clone <repository-url>
cd SSAFIT_Playlist
cp .env.example .env
```

`.env`는 개인 실행 환경 파일입니다. 실제 비밀번호와 Spotify 비밀 키를 넣어도 되지만 Git에 커밋하면 안 됩니다.

### 3. `.env` 수정

새 PC에서 최소로 바꿔야 하는 값은 아래 값들입니다.

```text
MYSQL_PASSWORD=<로컬 DB 비밀번호>
MYSQL_ROOT_PASSWORD=<로컬 MySQL root 비밀번호>
SSAFIT_DB_PASSWORD=<MYSQL_PASSWORD와 같은 값>
SPOTIFY_CLIENT_ID=<Spotify 클라이언트 ID>
SPOTIFY_CLIENT_SECRET=<Spotify 클라이언트 비밀 키>
```

Spotify를 사용하지 않을 때는 `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET`을 비워 둘 수 있습니다.
AI 러닝 리포트와 AI 운동 계획 추천을 실제 LLM 호출로 시연하려면 아래 값도 설정합니다.
비워 두면 서버 계산 기반 fallback 결과가 표시됩니다.

```text
AI_RUNNING_REPORT_ENABLED=true
AI_RUNNING_REPORT_PROVIDER=openai
AI_RUNNING_REPORT_API_KEY=<OpenAI 또는 호환 API 키>
```

Docker 브라우저 주소는 기본적으로 아래 값으로 맞춰져 있습니다.

```text
FRONTEND_HOST_PORT=5173
FRONTEND_PUBLIC_ORIGIN=http://127.0.0.1:5173
SSAFIT_FRONTEND_ORIGIN=http://127.0.0.1:5173
VITE_API_BASE_URL=/api
VITE_JSP_BASE_URL=http://127.0.0.1:5173
```

Spotify OAuth까지 확인하려면 Spotify 개발자 대시보드의 리다이렉트 URI에도 다음 값을 등록해야 합니다.

```text
http://127.0.0.1:5173/api/spotify/oauth/callback
```

### 4. 실행

Docker Compose 설정이 정상인지 먼저 확인합니다.

```bash
docker compose config --quiet
```

처음 실행하거나 코드가 바뀐 뒤에는 빌드와 실행을 같이 합니다.

```bash
docker compose up -d --build
```

상태를 확인합니다.

```bash
docker compose ps
```

정상 상태 예시는 아래와 같습니다.

```text
mysql      healthy
backend    healthy
frontend   healthy
```

브라우저에서 접속합니다.

```text
http://127.0.0.1:5173/
```

### 4-1. 시연용 DB 스냅샷 사전 작업

시연용 데이터는 아래 파일 하나로 통합되어 있습니다.

```text
ssafit/src/main/resources/sql/demo-snapshot.sql
```

이 스냅샷에는 공식 데모 플레이리스트, `Zone 2 러닝`, `WORKOUT K-POP 데모`,
`16반의 플레이리스트`, 데모 회원, 러닝 프로필, 러닝 기록, 러닝 세션,
참여 관계, 저장 관계, 음악 리뷰가 포함됩니다.

시연 로그인용 데모 계정은 아래와 같습니다. 비밀번호는 모두 같습니다.

```text
runner_low / e2e-password
runner_high / e2e-password
runner_interval / e2e-password
admin_candidate / e2e-password
demo_spotify / e2e-password
tester / e2e-password
demo_runner / e2e-password
```

신규 PC 또는 빈 Docker MySQL 볼륨에서 실행하는 경우 별도 SQL 명령은 필요 없습니다.
`docker compose up -d --build`를 실행하면 MySQL 컨테이너가 처음 생성될 때 다음 순서로
자동 실행합니다.

```text
/docker-entrypoint-initdb.d/01-schema.sql
/docker-entrypoint-initdb.d/02-demo-snapshot.sql
```

이미 `ssafit-mysql-data` 볼륨이 만들어진 PC에서는 Docker entrypoint SQL이 다시
실행되지 않습니다. 기존 DB를 시연 스냅샷 상태로 맞추려면 둘 중 하나를 선택합니다.

방법 A: DB를 완전히 초기화하고 시연 스냅샷을 자동 재적용합니다.

```bash
docker compose down -v
docker compose up -d --build
```

`docker compose down -v`는 기존 회원, 플레이리스트, 러닝 기록을 모두 삭제합니다.
시연 직전에 실제로 지워도 되는 DB인지 확인한 뒤 실행하세요.

방법 B: 기존 DB는 유지하고 시연 스냅샷만 수동으로 덮어 넣습니다.

Windows PowerShell:

```powershell
docker compose up -d mysql
Get-Content .\ssafit\src\main\resources\sql\demo-snapshot.sql |
  docker compose exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'
docker compose up -d --build
```

macOS/Linux:

```bash
docker compose up -d mysql
docker compose exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  < ssafit/src/main/resources/sql/demo-snapshot.sql
docker compose up -d --build
```

이제 `data.sql`, `dummy.sql`, `captured-spotify-playlist.sql`,
`zone2-spotify-playlist.sql`은 사용하지 않습니다. 시연 데이터는
`demo-snapshot.sql`만 실행하면 됩니다.

현재 로컬 PC의 실제 DB 상태를 다른 시연 PC에 그대로 옮겨야 한다면, 스냅샷 SQL 대신
덤프/복원을 사용합니다. 이 파일에는 실제 개인 계정, Spotify OAuth token,
refresh token, Spotify user id가 들어갈 수 있으므로 Git에 커밋하지 않습니다.

백업:

```powershell
docker compose exec -T mysql sh -c 'mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' > demo-data.local.sql
```

복원:

```powershell
docker compose up -d mysql
Get-Content .\demo-data.local.sql |
  docker compose exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'
docker compose up -d --build
```

### 5. JSP 로그인 흐름 확인

Docker 환경에서는 JSP 화면도 프론트엔드 Nginx를 통해 접근합니다. 주소는 항상 `:5173` 포트를 유지해야 합니다.

```text
회원가입: http://127.0.0.1:5173/join
로그인:   http://127.0.0.1:5173/login
마이페이지: http://127.0.0.1:5173/members/me
```

회원가입, 로그인, 로그아웃 후 브라우저 주소가 `http://127.0.0.1:5173/...` 형태를 유지하면 정상입니다.

### 6. 로그 보기

```bash
docker compose logs -f mysql
docker compose logs -f backend
docker compose logs -f frontend
```

### 7. 중지와 초기화

컨테이너만 중지하고 DB 데이터는 유지합니다.

```bash
docker compose down
```

DB까지 완전히 지우고 새로 시작할 때만 아래 명령을 사용합니다.

```bash
docker compose down -v
docker compose up -d --build
```

`docker compose down -v`는 Docker MySQL 볼륨을 삭제합니다. 회원 데이터와 테스트 데이터도 같이 사라집니다.

### 8. DB 초기화 방식

새 MySQL 볼륨은 아래 파일로 한 번만 초기화됩니다.

```text
ssafit/src/main/resources/sql/schema.sql
ssafit/src/main/resources/sql/demo-snapshot.sql
```

이미 생성된 볼륨에는 이 SQL이 다시 실행되지 않습니다. 스키마와 시연 데이터를
처음부터 다시 만들려면 `docker compose down -v`가 필요합니다.

### 9. 자주 나는 문제

| 증상 | 해결 |
|---|---|
| `5173` 포트가 이미 사용 중 | `.env`의 `FRONTEND_HOST_PORT`를 다른 포트로 바꾸고 `FRONTEND_PUBLIC_ORIGIN`, `SSAFIT_FRONTEND_ORIGIN`, `VITE_JSP_BASE_URL`도 같은 포트로 맞춥니다. |
| MySQL 포트가 이미 사용 중 | `.env`의 `MYSQL_HOST_PORT`를 `33306` 같은 값으로 바꿉니다. 백엔드 컨테이너는 내부 `mysql:3306`으로 붙으므로 보통 추가 수정이 필요 없습니다. |
| 백엔드가 DB 접속 실패 | 새 볼륨에서는 `MYSQL_PASSWORD`와 `SSAFIT_DB_PASSWORD`가 같아야 합니다. 기존 볼륨이면 처음 만들 때 사용한 비밀번호를 유지해야 합니다. |
| `/login` 이동 후 포트가 빠짐 | `.env`의 `FRONTEND_PUBLIC_ORIGIN`, `SSAFIT_FRONTEND_ORIGIN`, `VITE_JSP_BASE_URL`이 `http://127.0.0.1:5173`인지 확인하고 `docker compose up -d --build`를 다시 실행합니다. |
| 이미지가 예전 코드처럼 동작 | `docker compose build --no-cache backend frontend` 후 `docker compose up -d`를 실행합니다. |
| Spotify 콜백 주소 불일치 | `SPOTIFY_REDIRECT_URI`, `SSAFIT_FRONTEND_ORIGIN`, `VITE_JSP_BASE_URL`, Spotify 개발자 대시보드의 리다이렉트 URI를 같은 origin으로 맞춥니다. |

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

CREATE USER 'ssafit'@'localhost' IDENTIFIED BY 'change-me-local-password';
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
playlist-read-private
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

## AI running report environment variables

AI running reports are disabled by default. When the feature is disabled or no
provider/API key is configured, the backend uses the safe fallback report path
and does not call an external LLM.

Set real values only in local environment variables, Docker `.env`, an IDE run
configuration, or a secret manager. Do not commit real API keys.

```text
AI_RUNNING_REPORT_ENABLED=false
AI_RUNNING_REPORT_PROVIDER=none
AI_RUNNING_REPORT_API_KEY=
AI_RUNNING_REPORT_BASE_URL=https://api.openai.com/v1
AI_RUNNING_REPORT_MODEL=gpt-4.1-mini
AI_RUNNING_REPORT_TIMEOUT=10s
AI_RUNNING_REPORT_PROMPT_VERSION=ai-running-report-v1
AI_RUNNING_REPORT_MAX_OUTPUT_CHARACTERS=1200
```

To enable the OpenAI-compatible provider locally:

```text
AI_RUNNING_REPORT_ENABLED=true
AI_RUNNING_REPORT_PROVIDER=openai
AI_RUNNING_REPORT_API_KEY=<set-in-local-env-only>
```

Default unit and integration tests keep `AI_RUNNING_REPORT_ENABLED=false` and
use fake or mock providers. Tests that require real external LLM network calls
must be added as opt-in tests and excluded from the default test command.

## AI 러닝 리포트 기능

AI 러닝 리포트는 최근 러닝 기록과 음악 추천 로그를 사람이 읽기 쉬운
한국어 리포트로 요약합니다. 숫자 중심 기록 화면의 해석 부담을 줄이고,
다음 러닝과 음악 선택 방향을 제안하는 것이 목적입니다.

활용 전에는 거리, 시간, 페이스, 케이던스, 심박, 추천 BPM, 추천 장르,
추천 로그 목록을 사용자가 직접 비교해야 했습니다. 활용 후에는 같은
데이터를 러닝 패턴 요약, 페이스·강도 해석, 케이던스·심박 참고, 다음
음악 방향, 다음 러닝 제안으로 묶어서 확인할 수 있습니다.

백엔드는 모든 수치 통계를 먼저 계산하고, LLM은 준비된 집계 DTO를
해석해 제한된 JSON 문장으로 바꾸는 역할만 담당합니다. LLM 입력에는
회원 ID, 이메일, 닉네임, API Key 같은 개인정보나 비밀 값을 포함하지
않습니다. 데이터가 부족하거나 provider가 비활성화되어 있거나 LLM 호출이
실패하면 추측하지 않고 `FALLBACK` 리포트를 저장해 반환합니다.

발표 자료, 시연 흐름, 캡처 체크리스트는
[`docs/ai-running-report-feature.md`](docs/ai-running-report-feature.md)에
정리되어 있습니다.
