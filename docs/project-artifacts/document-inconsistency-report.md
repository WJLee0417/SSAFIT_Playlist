# 문서 정합성 검증 보고서

## 1. 검증 범위

- `00-evidence/`
- `01-requirements/`
- `02-usecase/`
- `03-erd/`
- `04-wbs/`
- `05-gantt/`
- `06-screen-design/`

## 2. 발견 및 수정한 문서 간 충돌

| 항목 | 충돌 내용 | 수정 결과 |
|---|---|---|
| Spotify 요구사항 ID | 요구사항 정의서 기준 `FR-019=내보내기`, `FR-020=가져오기`이나 화면 설계 일부에서 반대로 표기됨 | `screen-catalog.csv`, `screen-specification.md`, `screen-design-gap.md`를 FR-019 export, FR-020 import로 통일 |
| WBS 산출물 상태 | `wbs.md`, `wbs.csv`에서 Gantt Chart와 화면 설계서가 후속 예정/NOT_STARTED로 남아 있었음 | `9.4`, `12.6`, `12.7`을 DONE으로 갱신하고 근거 경로 기록 |
| Gantt 산출물 상태 | `gantt.csv`, `gantt.mmd`에서 Gantt Chart 작성이 IN_PROGRESS, 화면 설계서가 NOT_STARTED로 남아 있었음 | `G-803`, `G-605`를 DONE/PROJECT_DOCUMENT로 갱신 |
| 일정 가정 문서 | 화면 설계서를 이후 수행할 `ASSUMPTION` 작업으로 설명함 | `schedule-assumptions.md`에 Gantt/화면 설계서는 생성 완료 산출물로 갱신 |
| 러닝 프로필 화면 정책 | Vue 전용 러닝 프로필 화면이 미구현이라 결함처럼 보일 수 있었음 | 발표 기준은 JSP `/members/me/running-profile` 화면으로 확정하고, Vue 화면은 후속 SPA 통합 과제로 문서화 |

## 3. 구현 근거 부족 항목

| 항목 | 현재 상태 | 부족한 근거 | 발표 전 조치 |
|---|---|---|---|
| FR-018 Spotify OAuth | UNVERIFIED | 실제 Spotify Dashboard redirect URI, scope, 사용자 동의 성공 기록 | 실제 계정으로 OAuth 시작→callback→연결 상태 확인 |
| FR-019 Spotify 내보내기 | UNVERIFIED | 실제 Spotify playlist 생성 및 수록곡 추가 성공 기록 | export 대상 playlist와 Spotify URI 보유 곡으로 시연 |
| FR-020 Spotify 가져오기 | UNVERIFIED / 화면 미구현 | 전용 Vue 화면 route/component 없음, 실제 import 성공 기록 없음 | 발표에서는 API 구현/화면 미구현으로 설명 |
| FR-021 AI 러닝 리포트 | UNVERIFIED | 실제 AI Provider API Key/호출 성공 기록 | provider 활성 또는 fallback 시나리오 준비 |
| FR-024 내부 음악 관리 권한 | UNVERIFIED | 관리자 역할/권한 모델 없음 | 관리자 기능으로 발표하지 않음 |

## 4. DB 관계 불명확 항목

| 항목 | 설명 | 위험 |
|---|---|---|
| 추천 결과 JSON | `recommendation_logs.result_summary` 내부 음악 ID는 물리 FK가 아니다. | 음악 삭제/변경 후 과거 추천 결과와 실제 음악 정합성 차이 가능 |
| AI 리포트 스냅샷 JSON | `ai_running_reports.input_snapshot_json`은 러닝 기록/추천 로그의 스냅샷이다. | 원본 변경 후 리포트 재현성은 JSON 스키마에 의존 |
| Spotify 외부 식별자 | `music.spotify_*`, `playlists.spotify_*`, `spotify_connections.spotify_user_id`는 외부 시스템 ID다. | Spotify에서 삭제/권한 변경 시 내부 DB가 자동 감지하지 못함 |
| 실제 DB 접속 | ERD는 `schema.sql`과 Mapper 기준이다. | 운영/로컬 DB 드리프트 가능성은 별도 접속 검증 필요 |

## 5. 화면 경로 불명확 항목

| 항목 | 설명 | 처리 |
|---|---|---|
| Vue 러닝 프로필 전용 화면 | `runningProfileApi.js`는 있으나 Vue route/page 없음 | JSP `/members/me/running-profile`을 발표 기준 실제 화면으로 문서화하고 Vue 화면은 후속 SPA 통합 과제로 분리 |
| Spotify 가져오기 화면 | API client와 Controller는 있으나 route/component 없음 | `SCR-032 NOT_IMPLEMENTED_SCREEN`으로 추적 |
| 로그아웃 완료 화면 | 전용 화면 없이 JSP nav POST와 redirect 흐름 | SCR-004 처리 화면으로 문서화 |
| `/login/spotify` | 화면이 아니라 redirect Controller/route | SCR-028 partial redirect flow로 문서화 |

## 6. 일정 근거 부족 항목

| 항목 | Gantt Task | 상태 | 이유 |
|---|---|---|---|
| 실제 Spotify OAuth 검증 | G-504 | BLOCKED | 외부 앱 설정과 사용자 계정 필요 |
| 실제 Spotify Import/Export 검증 | G-505 | BLOCKED | OAuth scope와 외부 API 성공 필요 |
| 실제 AI Provider 호출 검증 | G-405 | BLOCKED | API Key와 provider endpoint 필요 |
| Java 25 Backend 전체 테스트 | G-705 | BLOCKED | 현재 로컬 Java 8 enforcer 실패 이력 |
| Docker 전체 기동 검증 | G-706 | UNVERIFIED | compose config와 실제 up/build는 다름 |
| 전체 브라우저 수동 검증 | G-707 | UNVERIFIED | Running 외 화면 E2E/수동 검증 부족 |

## 7. 발표 전에 확인해야 할 최종 항목

1. Spotify OAuth redirect URI와 scope가 발표 URL과 일치하는지 확인한다.
2. Spotify export 대상 플레이리스트의 모든 수록곡에 `spotify_uri`가 있는지 확인한다.
3. Spotify 가져오기는 화면 미구현으로 설명할지, API 데모로 대체할지 결정한다.
4. Java 25 환경에서 backend test 또는 최소 smoke test를 재확인한다.
5. Docker 전체 기동과 `http://127.0.0.1:5173/` 접속, JSP/Vue redirect를 확인한다.
6. 데모 계정, 러닝 프로필, 러닝 기록, 음악 seed, 플레이리스트 seed가 준비되어 있는지 확인한다.
7. AI Provider가 실패할 경우 fallback 또는 실패 상태를 발표에서 어떻게 설명할지 정한다.
