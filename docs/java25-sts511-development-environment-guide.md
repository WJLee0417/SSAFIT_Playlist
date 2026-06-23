# Java 25 및 STS 5.1.1 개발 환경 전환 가이드

## 1. 목적

다른 개발 환경에서 SSAFIT PlayList 프로젝트를 이어서 작업할 때 다음 도구 버전을 기준으로 개발 환경을 구성한다.

```text
Java: 25
IDE: STS 5.1.1
```

현재 프로젝트의 `pom.xml`은 Java 25로 설정되어 있다. Maven을 실행하는 JVM도
Java 25여야 하며, 다른 버전에서는 `validate` 단계에서 컴파일 전에 빌드가 중단된다.

## 2. 사전 준비

다음 항목을 설치하거나 준비한다.

- JDK 25
- STS 5.1.1
- Git
- MySQL 8
- 프로젝트에서 사용하는 Spotify 환경변수
- 실제 MySQL 통합 테스트 실행 시 사용할 별도 테스트 DB

JRE만 설치하지 말고 컴파일러가 포함된 JDK를 설치한다.

## 3. Java 25 설치 확인

터미널에서 다음 명령을 실행한다.

```bash
java -version
javac -version
```

두 명령 모두 버전 25를 표시해야 한다.

Windows PowerShell 예시:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

java -version
javac -version
```

`JAVA_HOME`은 실제 JDK 25 설치 경로에 맞게 설정한다.

영구 환경변수를 변경한 뒤에는 STS와 터미널을 완전히 종료하고 다시 실행한다.

## 4. STS 5.1.1 JDK 설정

STS에서 다음 순서로 JDK 25를 등록한다.

1. `Window`
2. `Preferences`
3. `Java`
4. `Installed JREs`
5. `Add`
6. `Standard VM`
7. JDK 25 설치 디렉터리 선택
8. 등록한 JDK 25를 기본 JRE로 선택

JDK 등록 후 다음 항목도 확인한다.

```text
Preferences
→ Java
→ Compiler
→ Compiler compliance level
```

IDE 전역 JDK와 Maven 프로젝트의 컴파일 대상 버전, Maven 실행 JDK는 서로 다른
개념이다. 세 항목을 모두 Java 25로 맞춘다.

## 5. STS 실행 JDK 확인

STS 자체가 다른 Java 버전으로 실행되는 경우 STS 설치 폴더의 `.ini` 파일에 JDK 경로를 지정한다.

`-vmargs`보다 앞에 다음 형식으로 작성한다.

```text
-vm
C:\Program Files\Java\jdk-25\bin\javaw.exe
-vmargs
```

주의사항:

- `-vm`과 Java 실행 파일 경로는 서로 다른 줄에 작성한다.
- 반드시 `-vmargs`보다 앞에 작성한다.
- 실제 설치 경로에 맞게 수정한다.

STS 재실행 후 다음 메뉴에서 실행 환경을 확인한다.

```text
Help
→ About Spring Tool Suite
→ Installation Details
→ Configuration
```

## 6. Git 저장소 준비

원격 `develop` 브랜치를 기준으로 작업한다.

신규 clone:

```bash
git clone <repository-url>
cd SSAFIT_Playlist
git switch develop
```

이미 clone된 저장소:

```bash
git fetch origin
git switch develop
git pull --ff-only origin develop
```

작업 시작 전 다음 명령으로 상태를 확인한다.

```bash
git branch --show-current
git status
```

## 7. STS 프로젝트 Import

프로젝트 루트의 `ssafit/pom.xml`을 Maven 프로젝트로 가져온다.

```text
File
→ Import
→ Maven
→ Existing Maven Projects
→ ssafit 디렉터리 선택
→ Finish
```

Import 후 다음 작업을 수행한다.

```text
프로젝트 우클릭
→ Maven
→ Update Project
```

필요하면 `Force Update of Snapshots/Releases`를 선택한다.

## 8. 프로젝트 JDK 확인

STS에서 프로젝트 우클릭 후 다음 항목을 확인한다.

```text
Properties
→ Java Build Path
→ Libraries
```

`JRE System Library`가 JDK 25를 가리키도록 설정한다.

다음 항목도 확인한다.

```text
Properties
→ Java Compiler
Properties
→ Project Facets
```

Maven 설정과 IDE 설정이 다르면 Maven 설정을 우선 기준으로 삼고 `Maven → Update Project`를 다시 실행한다.

## 9. 현재 Java 버전 설정

현재 `ssafit/pom.xml`에는 다음 설정이 존재한다.

```xml
<java.version>25</java.version>
```

Maven Enforcer는 Maven 실행 JVM을 Java 25(25.x)로 제한한다. Java 버전 설정을
확인할 때 최소한 다음을 함께 확인한다.

- Spring Boot 및 MyBatis 의존성 호환성
- Maven Compiler Plugin 동작
- Lombok annotation processing
- STS 컴파일러 설정
- 전체 단위 테스트
- 실제 MySQL 통합 테스트

## 10. Lombok 설정

STS에서 Lombok 관련 컴파일 오류가 발생하면 다음을 확인한다.

- Maven 의존성 다운로드 여부
- Annotation Processing 활성화 여부
- STS와 Lombok의 호환 여부
- 프로젝트 Maven Update 여부
- Project Clean 수행 여부

프로젝트에서 Lombok이 정상 인식되지 않으면 Domain 및 Service 클래스에서 생성자나 getter 관련 오류가 연쇄적으로 발생할 수 있다.

## 11. MySQL 8 설정

기본 애플리케이션 연결 정보는 `ssafit/src/main/resources/application.yml`에서 확인한다.

공용 또는 운영 DB 정보를 새 PC에 임의로 복사하지 않는다. 로컬 개발 DB와 테스트 전용 DB를 구분한다.

실제 Mapper 통합 테스트에는 DB 이름에 `test`가 포함된 별도 DB를 사용한다.

권장 테스트 DB 이름:

```text
ssafit_test
```

자세한 실행 절차는 `docs/mysql-integration-test-guide.md`를 따른다.

## 12. Spotify 환경변수

Spotify 기능을 실행하려면 다음 환경변수가 필요하다.

```text
SPOTIFY_CLIENT_ID
SPOTIFY_CLIENT_SECRET
SPOTIFY_REDIRECT_URI
```

Client Secret, Access Token 및 Refresh Token은 Git에 커밋하거나 문서에 기록하지 않는다.

STS의 실행 설정에 환경변수를 추가하려면 다음 메뉴를 사용한다.

```text
Run
→ Run Configurations
→ Spring Boot App
→ Environment
```

## 13. Maven 실행 환경 확인

프로젝트 디렉터리에서 Maven Wrapper가 사용하는 Java 버전을 확인한다.

PowerShell:

```powershell
Set-Location ssafit
.\mvnw.cmd -version
```

출력의 Java version이 25인지 확인한다.

`java -version`은 현재 PowerShell의 `PATH`가 선택한 Java를 보여 주고,
`.\mvnw.cmd -version`은 Maven Wrapper가 실제로 사용하는 Java를 보여 준다.
두 결과가 다르면 `JAVA_HOME`, `PATH` 순서와 IDE Maven JDK 설정을 확인한다.

IntelliJ IDEA에서는 Project SDK만 바꾸는 것으로 충분하지 않다. Maven Runner와
Maven Importer JDK를 모두 Java 25로 지정한다. STS에서도 Installed JRE와 Project
JRE 외에 Maven 실행 JRE가 Java 25인지 확인한다.

기본 테스트:

```powershell
.\mvnw.cmd clean test
```

실제 MySQL 통합 테스트:

```powershell
$env:SSAFIT_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3306/ssafit_test?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true'
$env:SSAFIT_TEST_DB_USERNAME = '<TEST_DB_USERNAME>'
$env:SSAFIT_TEST_DB_PASSWORD = '<TEST_DB_PASSWORD>'

.\mvnw.cmd clean test
```

테스트 결과는 다음 조건을 만족해야 한다.

```text
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

## 14. 환경 전환 체크리스트

- [ ] `java -version`이 Java 25를 표시한다.
- [ ] `javac -version`이 Java 25를 표시한다.
- [ ] `JAVA_HOME`이 JDK 25를 가리킨다.
- [ ] STS 5.1.1에 JDK 25가 등록되어 있다.
- [ ] STS 자체가 의도한 JDK로 실행된다.
- [ ] `develop` 브랜치를 최신 상태로 받았다.
- [ ] `ssafit`을 Existing Maven Project로 Import했다.
- [ ] Maven Update를 수행했다.
- [ ] Lombok annotation processing이 정상 동작한다.
- [ ] 로컬 MySQL 8 연결을 확인했다.
- [ ] Spotify 환경변수는 필요한 경우에만 안전하게 설정했다.
- [ ] `mvnw.cmd -version`에서 Java 25를 확인했다.
- [ ] 기본 테스트를 실행했다.
- [ ] 실제 MySQL 테스트는 테스트 전용 DB에서 실행했다.

## 15. 주의사항

- JDK 25 설치만으로 Maven 실행 JVM이 자동으로 Java 25가 되지는 않는다.
- `pom.xml`의 Java 25 설정과 Enforcer 검증을 임의로 낮추지 않는다.
- STS의 Project JRE, Maven 실행 JDK, 터미널 `JAVA_HOME`이 서로 다를 수 있으므로 각각 확인한다.
- 테스트 DB 이름에 `test`가 없으면 Mapper 통합 테스트를 실행하지 않는다.
- 민감한 환경변수와 DB 비밀번호를 Git에 커밋하지 않는다.
