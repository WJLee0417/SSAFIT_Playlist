# Gantt 일정 산정 기준과 가정

## 1. 실제 일정으로 판정한 기준

실제 일정은 Git log에서 확인 가능한 커밋 날짜를 우선 사용했다.

- `2026-06-08`: `59f818d Initial commit` 기준 저장소 초기화.
- `2026-06-19`: `2730e86`, `e581a90`, `ba83166`, `4f171bb`, `844a980`, `7616bd8`, `b222e41`, `b1fb98b` 등으로 Spring Boot, DB, 회원, 음악, 리뷰, 플레이리스트, 추천, Spotify Service 기반 구현이 시작/완료됨.
- `2026-06-20~2026-06-21`: `feature/a-running-features-service` 커밋으로 러닝 기록/세션/참여 Service와 테스트가 구현되고 develop에 병합됨.
- `2026-06-21~2026-06-22`: `5d1f5e1`, `97cd065`, `3e417e0`, `21c12ed`, `5ed8f6c`, `5d8a0f5`, `f9ce884`로 Controller/DTO/JSP/Vue/API 연결 작업이 진행됨.
- `2026-06-23`: `c105234`, `bba86bb`로 케이던스·운동 강도 추천 통합, Java 25 enforcement, MySQL-backed E2E 테스트 근거가 추가됨.
- `2026-06-24`: `6da7bf3`, `ed2ff52`, `68134dd`, `b36c477`, `af8ee3b` 등으로 AI 러닝 리포트, Spotify OAuth/import/export, Docker/README/프록시 흐름이 보강됨.
- `2026-06-25`: `feature/b-ai-feature-update` 병합과 문서 산출물 작성일 기준으로 추천 히스토리, 데모 스냅샷, project-artifacts 문서화가 진행됨.

Git log는 날짜 단위까지 확인되며, 커밋 시각까지 정확히 사용하지 않았다. 따라서 `gantt.mmd`와 `gantt.csv`의 시각 값은 날짜 내 정렬과 발표 전 잔여 작업 표현을 위한 보조값이다.

## 2. 추정 일정으로 판정한 기준

다음 작업은 Git log만으로 실제 수행 완료 시각을 확인할 수 없어 `ASSUMPTION` 또는 `MANUAL_CONFIRMATION_NEEDED`로 표시했다.

- 발표 전 수동 검증 작업: `2026-06-25 18:00` 이후부터 `2026-06-26 09:00` 전까지 배치.
- Docker 전체 기동 검증: Compose config는 확인됐지만 실제 `docker compose up --build` 완료 근거가 없어 추정 구간으로 배치.
- Spotify OAuth/import/export 실검증: 코드와 테스트는 있으나 실제 Spotify 계정, redirect URI, scope, rate limit 확인이 필요하므로 `MANUAL_CONFIRMATION_NEEDED`.
- AI provider 실호출: provider 코드는 있으나 환경 변수와 API Key 기반 실 호출 확인이 필요하므로 `MANUAL_CONFIRMATION_NEEDED`.
- 발표 리허설: WBS상 후속 작업이며 실제 수행 여부가 Git/문서 근거로 확인되지 않으므로 `ASSUMPTION`.
- Gantt Chart와 화면 설계서: `docs/project-artifacts/05-gantt`, `docs/project-artifacts/06-screen-design` 산출물이 생성되어 최종 패키지 기준 `PROJECT_DOCUMENT` 완료 작업으로 갱신했다.
- Vue 전용 러닝 프로필 화면: 발표 전 신규 구현 대상에서 제외하고 JSP `/members/me/running-profile`을 실제 시연 화면으로 사용하는 것으로 확정했다. Vue Router 화면 구현은 `2.5` 후속 SPA 통합 과제로 남긴다.

## 3. Git 이력만으로 확인할 수 없는 일정

| 항목 | 이유 | Gantt 처리 |
|---|---|---|
| 실제 Spotify OAuth 성공 | 외부 Dashboard 설정과 사용자 동의가 Git에 남지 않는다. | `G-504`, `G-505`, `MANUAL_CONFIRMATION_NEEDED` |
| 실제 AI Provider 호출 성공 | API Key, provider endpoint, 네트워크 결과가 Git에 남지 않는다. | `G-405`, `MANUAL_CONFIRMATION_NEEDED` |
| 로컬 Java 25 backend 전체 테스트 재실행 | 현재 작업 환경은 Java 8 enforcer 실패 이력이 있어 실제 재실행 완료가 아니다. | `G-705`, `MANUAL_CONFIRMATION_NEEDED` |
| Docker 전체 기동 성공 | `docker compose config` 수준의 확인과 실제 up/build/healthcheck는 다르다. | `G-706`, `ASSUMPTION` |
| 사람 기준 브라우저 클릭 검증 | 일부 E2E는 있으나 전체 화면 수동 검증 결과가 Git에 없다. | `G-707`, `ASSUMPTION` |
| 발표 리허설 | 발표 직전 실제 수행 여부는 Git으로 확인할 수 없다. | `G-805`, `ASSUMPTION` |

## 4. 발표 전 마감해야 하는 핵심 작업

최종 발표 시작은 `2026-06-26 09:00`이다. 그 전까지 다음 작업이 우선 마감 대상이다.

1. Spotify 데모 경로 확정: OAuth 성공, 검색, import/export 성공 또는 실패 대체 시나리오.
2. Java 25 Maven 환경 확인: `mvnw -version`과 backend test 재실행 또는 실패 사유 명시.
3. Docker 전체 기동: MySQL seed, backend healthcheck, frontend 접속, JSP/Vue redirect 확인.
4. 전체 브라우저 수동 검증: 로그인, 회원가입, 음악/리뷰, 플레이리스트, 러닝 기록/세션, 추천 저장.
5. 발표 시연 체크리스트와 리허설: 데모 계정, 환경 변수, 외부 API 실패 시 설명 순서.

## 5. 일정상 가장 위험한 병목 구간

| 순위 | 병목 | 관련 Task | 위험 이유 |
|---:|---|---|---|
| 1 | Spotify 실검증 | `G-504`, `G-505` | 외부 계정, Dashboard redirect URI, scope, rate limit에 의존한다. 발표에서 체감 가치가 크지만 Git만으로 완료 판정이 불가능하다. |
| 2 | Java 25와 Docker 검증 | `G-705`, `G-706` | 현재 로컬 기본 Java가 8로 확인되어 backend test가 막힐 수 있고, Docker 전체 기동은 config 확인과 별개다. |
| 3 | JSP/Vue/브라우저 통합 검증 | `G-105`, `G-604`, `G-707` | 세션 쿠키, returnTo, 프록시, 로그인 후 복귀 경로가 발표 동선 전체에 영향을 준다. |

## 6. 발표 마일스톤

- `G-806`: 최종 발표 시작
- 일시: `2026-06-26 09:00`
- 이 시각 이후 작업은 본 Gantt에서 발표 후 후속 작업으로 보지 않고, 발표 전 완료 또는 리스크 설명 대상으로 본다.
