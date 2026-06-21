# 로컬 환경 이전 및 작업 시작 가이드

이 문서는 `feature/b-ui-api` 브랜치의 작업을 다른 로컬 환경에서 이어가기 위한 초기 설정과 작업 시작 전 점검 절차를 정리한다.

## 1. 기존 환경에서 작업 종료

현재 작업이 모두 커밋되었는지 확인한다.

```powershell
git branch --show-current
git status
git log -5 --oneline
```

확인 기준:

- 현재 브랜치가 `feature/b-ui-api`다.
- `git status`에 커밋되지 않은 변경이 없다.
- 필요한 커밋이 로컬 이력에 존재한다.

원격 저장소에 반영할 준비가 되었다면 다음 명령을 실행한다.

```powershell
git push origin feature/b-ui-api
```

실제 Secret, Token, 비밀번호가 커밋에 포함되지 않았는지 반드시 확인한다.

## 2. 새 로컬 환경 필수 도구

다음 도구를 설치한다.

- Git
- JDK 21
- MySQL 8
- Node.js
- pnpm
- IntelliJ IDEA, STS 또는 VS Code

버전을 확인한다.

```powershell
git --version
java -version
node --version
pnpm --version
```

Backend 기준 Java 버전은 21이다. IDE의 Project SDK, Maven 실행 JDK, 터미널의 `JAVA_HOME`도 같은 버전을 사용하도록 맞춘다.

## 3. 저장소 복제 및 브랜치 준비

프로젝트 경로에는 가능하면 한글과 공백을 사용하지 않는다. 현재 Windows 환경에서는 공백이 포함된 한글 경로에서 실행 WAR의 JSTL JAR 검색 문제가 확인되었다.

```powershell
git clone https://github.com/WJLee0417/SSAFIT_Playlist.git
cd SSAFIT_Playlist
git fetch origin
git switch feature/b-ui-api
git pull --ff-only origin feature/b-ui-api
```

작업 시작 전 상태를 확인한다.

```powershell
git branch --show-current
git status
git log -5 --oneline
```

확인 기준:

- 현재 브랜치: `feature/b-ui-api`
- 작업 트리: clean
- 원격 브랜치의 최신 커밋이 로컬에 존재

다른 브랜치에서 작업 중이라면 변경 파일을 임의로 버리거나 `git reset --hard`를 실행하지 않는다.

## 4. MySQL 설정

기본 애플리케이션 설정은 다음 값을 사용한다.

```text
Database: ssafit_playlist
Username: ssafit
Password: ssafit1234!
Host: localhost
Port: 3306
```

MySQL에서 로컬 개발용 DB와 계정을 준비한다.

```sql
CREATE DATABASE ssafit_playlist
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'ssafit'@'localhost' IDENTIFIED BY 'ssafit1234!';
GRANT ALL PRIVILEGES ON ssafit_playlist.* TO 'ssafit'@'localhost';
FLUSH PRIVILEGES;
```

테이블이 없다면 다음 파일을 MySQL에서 실행한다.

```text
ssafit/src/main/resources/sql/schema.sql
```

`spring.sql.init.mode`는 `never`이므로 애플리케이션 실행만으로 DDL이 자동 적용되지 않는다.

## 5. Spotify 환경변수

저장소 루트의 `.env.example`을 참고해 실제 실행 환경 또는 IDE Run Configuration에 다음 값을 설정한다.

```text
SPOTIFY_CLIENT_ID=<Spotify Developer Dashboard에서 발급>
SPOTIFY_CLIENT_SECRET=<Spotify Developer Dashboard에서 발급>
SPOTIFY_REDIRECT_URI=http://localhost:8080/api/spotify/oauth/callback
```

Spotify Developer Dashboard에도 동일한 Redirect URI를 등록한다. 실제 값은 Git에 커밋하지 않는다.

Spotify 기능을 사용하지 않는 일반 빌드와 Mock 기반 테스트는 빈 환경변수로도 실행할 수 있다.

## 6. Backend 설정 및 기준선 검증

Windows PowerShell:

```powershell
cd ssafit
.\mvnw.cmd clean test
.\mvnw.cmd package -DskipTests
```

macOS/Linux:

```bash
cd ssafit
./mvnw clean test
./mvnw package -DskipTests
```

2026년 6월 21일 기준선:

- 전체 테스트: 225건
- 실패: 0건
- 오류: 0건
- 조건부 skip: 13건
- WAR 패키징 성공

Mapper 통합 테스트 13건은 별도 테스트 DB 환경이 준비되지 않으면 조건부로 skip될 수 있다. 테스트 수가 이후 커밋에서 달라졌다면 숫자 자체보다 실패와 오류가 없는지, 새 테스트가 정상 포함되었는지를 우선 확인한다.

개발 서버를 실행한다.

```powershell
.\mvnw.cmd spring-boot:run
```

기본 주소는 `http://localhost:8080`이다.

## 7. JSP 화면 점검

서버 실행 후 다음 주소를 확인한다.

| URL | 비로그인 기대 결과 |
| --- | --- |
| `http://localhost:8080/join` | 회원가입 JSP 렌더링 |
| `http://localhost:8080/login` | 로그인 JSP 렌더링 |
| `http://localhost:8080/members/me` | `/login`으로 Redirect |
| `http://localhost:8080/members/me/running-profile` | `/login`으로 Redirect |
| `http://localhost:8080/css/member.css` | CSS 응답 |

추가 수동 점검:

1. 회원가입 성공 후 로그인 화면으로 이동한다.
2. 로그인 ID 또는 이메일 중복 오류가 해당 입력란에 표시된다.
3. 로그인 성공 후 마이페이지로 이동한다.
4. 로그인 상태에서 회원 정보와 러닝 프로필 요약이 표시된다.
5. 러닝 프로필 신규 등록과 수정 후 PRG Redirect가 동작한다.
6. 로그아웃 후 보호 화면에 접근하면 로그인 화면으로 이동한다.
7. 메뉴의 Vue 음악·플레이리스트 링크가 `localhost:5173`으로 이동한다.

실행 WAR를 `java -jar`로 구동할 때 JSTL 검색 오류가 발생한다면 먼저 프로젝트 경로에 한글 또는 공백이 있는지 확인한다. 개발 중에는 `spring-boot:run`으로 JSP 렌더링을 검증할 수 있다.

## 8. Frontend 설정 및 검증

새 터미널에서 저장소 루트의 Frontend 모듈로 이동한다.

```powershell
cd frontend
pnpm install --frozen-lockfile
pnpm build
pnpm dev
```

기본 주소는 `http://localhost:5173`이다.

확인 기준:

- `pnpm build` 성공
- Vite 개발 서버 접속 가능
- API prefix가 `/api/**`
- Axios 요청에 `withCredentials: true` 적용
- Spring Boot의 Session Cookie가 Vue API 요청에 전달됨
- 401, 403, 404, 409, 502 응답을 HTTP status와 오류 `code`로 구분

## 9. 계약 문서 확인

구현 전 다음 문서를 우선 확인한다.

```text
docs/integration/b-ui-api-contract-audit.md
docs/integration/b-ui-api-contract.md
```

핵심 계약:

- 인증은 JWT나 Spring Security가 아닌 `HttpSession` 기반이다.
- 로그인 회원 Session key는 `LOGIN_MEMBER_ID`이고 값 타입은 `Long`이다.
- Controller는 입력 검증, Session 회원 확인, Service 호출, DTO 변환만 담당한다.
- Request DTO를 Domain에 직접 바인딩하지 않는다.
- Access Token, Refresh Token, Client Secret, 비밀번호 해시는 응답이나 로그에 노출하지 않는다.
- DDL, Mapper XML, SQL, Service 비즈니스 로직은 팀원 A 담당 영역이다.
- 실제 Service 계약과 화면 요구가 다르면 Controller 또는 DTO Adapter에서 흡수한다.

## 10. 작업 시작 전 최종 체크리스트

- [ ] `feature/b-ui-api` 브랜치인지 확인했다.
- [ ] 작업 트리가 clean 상태다.
- [ ] 원격 최신 커밋을 `--ff-only`로 반영했다.
- [ ] JDK 21과 Maven 실행 JDK가 일치한다.
- [ ] MySQL 8과 `ssafit_playlist` DB가 준비되었다.
- [ ] 필요한 Spotify 환경변수를 로컬에만 설정했다.
- [ ] Backend 전체 테스트가 통과했다.
- [ ] Backend WAR 패키징이 성공했다.
- [ ] `/join`, `/login` JSP가 정상 렌더링된다.
- [ ] 비로그인 보호 화면의 Redirect가 정상이다.
- [ ] Frontend 의존성 설치와 build가 성공했다.
- [ ] Vue 요청의 Session Cookie 전달 설정을 확인했다.
- [ ] 계약 문서 두 개를 읽고 실제 Service 시그니처를 확인했다.

## 11. 추천 커밋 메시지

이 가이드 문서만 커밋할 때:

```text
docs: add local environment handoff and startup guide
```
