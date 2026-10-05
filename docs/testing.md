# Testing

## Test Strategy

프로젝트는 기능 계층에 따라 테스트를 분리합니다.

| Layer | Purpose |
| --- | --- |
| Service / Unit | 도메인 규칙과 분기 검증 |
| Controller | 요청·응답, 인증 상태, 오류 응답 검증 |
| Mapper Integration | MyBatis SQL과 MySQL 제약 검증 |
| Frontend Build | Vue/Vite 빌드 가능 여부 확인 |
| E2E | 주요 사용자 흐름을 브라우저 기준으로 검증 |
| Docker | 서비스 구성과 healthcheck 기반 실행 확인 |

## Backend

```bash
cd ssafit
./mvnw test
```

Windows:

```powershell
cd ssafit
.\mvnw.cmd test
```

Backend는 Java 25를 요구합니다. Maven Wrapper가 사용하는 JVM도 Java 25여야 합니다.

## Mapper Integration

Mapper 통합 테스트는 실제 MySQL 테스트 DB가 준비된 환경에서 실행할 수 있도록 분리되어 있습니다. 테스트 DB 접속 정보는 전용 환경변수로 주입하며 개인 개발 DB를 직접 사용하는 방식은 피합니다.

검증 대상에는 다음이 포함됩니다.

- Mapper SQL 실행
- FK / UNIQUE 등 DB 제약
- 상태 변경 결과
- 동시성이나 affected-row 기반 방어 로직이 필요한 경로

## Frontend

```bash
cd frontend
npm ci
npm run build
```

E2E:

```bash
npm run e2e
```

Playwright 테스트는 로그인과 러닝 기능 등 주요 사용자 흐름 검증에 사용합니다.

## External APIs

기본 자동 테스트는 실제 Spotify 또는 외부 LLM 계정에 의존하지 않도록 Mock/Fake 경계를 사용합니다. 실제 계정이 필요한 OAuth, Spotify Playlist 생성, 외부 LLM 호출은 별도 환경 설정이 필요한 통합 검증으로 구분합니다.

## Reproducible Runtime

Docker Compose 실행에서는 MySQL, Backend, Frontend에 healthcheck를 두고 의존 서비스가 준비된 뒤 다음 서비스가 시작되도록 구성합니다.

```bash
docker compose up -d --build
docker compose ps
```
