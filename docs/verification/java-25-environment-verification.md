# Java 25 실행 환경 검증 보고서

## 1. 목적

Backend가 요구하는 Java 25를 Maven 실행 초기에 검증하고, Java 8 등 낮은 버전에서
record, 패턴 매칭, switch expression 관련 컴파일 오류가 먼저 노출되는 문제를
방지한다.

검증일: 2026-06-23  
대상 브랜치: `feature/b-running-features-ui`

## 2. 원인 분석

- `ssafit/pom.xml`의 컴파일 대상은 이미 `<java.version>25</java.version>`이었다.
- Maven Wrapper는 Maven 3.9.16을 사용하지만, Wrapper 자체는 실행 JVM을 Java 25로
  제한하지 않았다.
- 로컬 `PATH`가 Java 8을 가리키는 경우에도 Maven이 컴파일 단계까지 진행할 수 있어,
  개발자는 환경 문제 대신 Java 문법 또는 `release 25` 관련 오류를 먼저 보게 됐다.
- 기존 Maven Enforcer 설정은 없었다.
- `.mvn`에는 Wrapper 설정만 있고 `jvm.config`, `maven.config`는 없었다.
- 저장소에 GitHub Actions, GitLab CI, Jenkins, Azure Pipelines 등 CI 설정 파일은
  존재하지 않았다.
- README와 일부 개발 환경 문서에는 Java 21 안내가 남아 있어 현재 `pom.xml`과
  일치하지 않았다.

## 3. 변경 내용

### Maven 사전 검증

`maven-enforcer-plugin` 3.5.0의 `requireJavaVersion` 규칙을 `validate` 단계에
추가했다.

```xml
<version>[25,26)</version>
```

따라서 Maven 실행 JVM은 Java 25.x만 허용한다. 검증 메시지는 다음 정보를 포함한다.

- 감지된 Java specification/runtime 버전
- Maven이 사용하는 `java.home`
- 요구 버전 Java 25
- `JAVA_HOME`, `PATH`, IDE Maven Runner/Importer JDK 확인 안내
- `java -version`, `.\mvnw.cmd -version` 재확인 명령

Java 버전을 변경하거나 설치하는 스크립트는 추가하지 않았다.

### 문서

- Windows PowerShell의 `JAVA_HOME`, `PATH` 설정 예시 추가
- 셸 Java와 Maven 실행 Java를 각각 확인하는 방법 추가
- STS와 IntelliJ의 Maven 실행 JDK를 Java 25로 지정하는 안내 추가
- Backend(Java 25/Maven)와 Frontend(Node.js/npm) 실행 환경 분리
- 현재 환경과 맞지 않던 Java 21 안내를 Java 25로 수정

### CI

현재 저장소에는 CI 설정이 없어 수정할 파일이 없었다. 향후 CI를 추가할 때는 기본
Runner Java에 의존하지 말고 Java 25 배포판을 명시적으로 설정한 뒤
`ssafit` 디렉터리에서 Maven Wrapper를 실행해야 한다.

## 4. Java 25 미만 환경 검증

실행 환경:

```text
Apache Maven 3.9.16
Java version: 1.8.0_192
Runtime: C:\Program Files\Zulu\zulu-8\jre
```

실행 명령:

```powershell
Set-Location .\ssafit
.\mvnw.cmd validate
```

결과:

```text
[INFO] --- enforcer:3.5.0:enforce (enforce-java-25-runtime) @ ssafit ---
[ERROR] Java 25 is required to build the SSAFIT Backend. Detected Maven Java:
specification version 1.8, runtime 1.8.0_192-b01,
java.home C:\Program Files\Zulu\zulu-8\jre. Required version: Java 25 (25.x).
Check JAVA_HOME, PATH, and the IDE Maven runner/importer JDK, then verify with
"java -version" and ".\mvnw.cmd -version".
```

- 결과: 의도한 `BUILD FAILURE`
- 실패 단계: `validate`
- 소스 컴파일 실행 여부: 실행되지 않음
- record, switch expression 등 Java 문법 오류 노출 여부: 없음

## 5. Java 25 환경 테스트

STS 5.1.1은 다음 JustJ Java 25 런타임을 내장하고 있었다.

```text
C:\Users\SSAFY\BackUp\sts-5.1.1.RELEASE\plugins\
org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.2.v20260203-1141\jre
```

폴더명은 `jre`지만 `javac 25.0.2`, `java.compiler`, `jdk.compiler` 모듈을 포함한
full JustJ 배포본이다. STS의 `SpringToolsForEclipse.ini`와 워크스페이스 기본 VM도
이 경로를 사용하고 있었다.

검증 명령:

```powershell
$env:JAVA_HOME = 'C:\Users\SSAFY\BackUp\sts-5.1.1.RELEASE\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.2.v20260203-1141\jre'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

java -version
Set-Location .\ssafit
.\mvnw.cmd -version
.\mvnw.cmd test
```

검증 결과:

```text
Java version: 25.0.2
Vendor: Eclipse Adoptium
Maven: 3.9.16
Maven Enforcer requireJavaVersion: passed
Tests run: 257
Failures: 0
Errors: 0
Skipped: 13
BUILD SUCCESS
Total time: 16.939 s
```

Spring Boot 테스트 로그에서도 `using Java 25.0.2`를 확인했다. Mapper 통합 테스트
13개는 테스트 DB 환경변수가 없어 기존 정책대로 조건부 스킵됐다.

## 6. 변경 파일

- `ssafit/pom.xml`
- `README.md`
- `docs/java25-sts511-development-environment-guide.md`
- `docs/integration/local-environment-handoff-guide.md`
- `docs/mysql-integration-test-guide.md`
- `docs/team-b-handover.md`
- `docs/verification/java-25-environment-verification.md`

runningrecord, runningsession, 참여, Spotify, 회원, 음악, 리뷰, 플레이리스트의
비즈니스 로직은 변경하지 않았다.

## 7. 남은 제약 사항

- MySQL 테스트 환경변수가 없는 경우 Mapper 통합 테스트 13개는 기존 정책대로
  조건부 스킵된다.
- CI 설정은 현재 저장소에 존재하지 않는다. 향후 CI 도입 시 Java 25를 명시해야 한다.
- Maven Wrapper를 처음 실행하는 환경은 부모 POM과 Enforcer 플러그인을 받기 위한
  Maven Central 접근이 필요하다.
- STS 설치 경로는 PC마다 다르므로 다른 개발자가 내장 JustJ를 재사용하려면 해당
  PC의 `SpringToolsForEclipse.ini`에 기록된 `-vm` 경로를 기준으로 설정해야 한다.