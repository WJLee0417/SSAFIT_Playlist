# SSAFIT Playlist 프로젝트 산출물 패키지

문서 생성 기준일: 2026-06-25  
근거 기준: 현재 저장소의 실제 코드, SQL, Mapper, Vue/JSP 화면, Git 이력, `docs/project-artifacts/00-evidence/`

## 1. 산출물 전체 목차

| 경로 | 목적 |
|---|---|
| `00-evidence/` | 코드와 Git 기반 근거 데이터, 구현 상태 판정의 원본 |
| `01-requirements/` | 목표 요구사항과 현재 구현 상태를 분리한 요구사항 정의 |
| `02-usecase/` | Actor와 Use Case 관계, PlantUML 다이어그램, 상세 명세 |
| `03-erd/` | 실제 DDL/Mapper 기준 ERD, DBML, Mermaid, 데이터 사전 |
| `04-wbs/` | 완료/잔여 작업을 구분한 WBS와 리스크 |
| `05-gantt/` | Git 이력과 WBS 기반 일정표, 발표 전 잔여 작업 |
| `06-screen-design/` | Vue/JSP/Controller/API 기준 화면 설계서와 화면 흐름 |
| `traceability-master.md` | Requirement, Use Case, Screen, API, DB, WBS, Gantt 통합 추적표 |
| `quality-checklist.md` | 산출물 정합성 검증 체크리스트 |
| `document-inconsistency-report.md` | 충돌, 근거 부족, 발표 전 확인 항목 보고서 |

## 2. 열람 권장 순서

1. `00-evidence/project-inventory.md`
2. `01-requirements/requirements-definition.md`
3. `traceability-master.md`
4. `02-usecase/use-case-specification.md`
5. `03-erd/erd-data-dictionary.md`
6. `06-screen-design/screen-specification.md`
7. `04-wbs/wbs.md`
8. `05-gantt/gantt.mmd`
9. `quality-checklist.md`
10. `document-inconsistency-report.md`

## 3. 다이어그램 활용 방법

| 형식 | 파일 | 활용 도구 |
|---|---|---|
| PlantUML | `02-usecase/*.puml` | PlantUML, IntelliJ/VS Code PlantUML extension, draw.io PlantUML import |
| Mermaid | `03-erd/erd.mmd`, `05-gantt/gantt.mmd`, `06-screen-design/screen-flow.mmd` | Mermaid Live Editor, Markdown preview, Notion/Obsidian 일부 플러그인 |
| DBML | `03-erd/erd.dbml` | dbdiagram.io, DBML viewer |

## 4. draw.io로 옮길 때 사용할 파일

- Use Case: `02-usecase/use-case-overview.puml`, `use-case-member.puml`, `use-case-music-playlist.puml`, `use-case-running-recommendation.puml`, `use-case-spotify.puml`
- ERD: `03-erd/erd.dbml`, `03-erd/erd.mmd`, `03-erd/drawio-import-guide.md`
- Gantt: `05-gantt/gantt.mmd`, `05-gantt/gantt.csv`
- 화면 흐름: `06-screen-design/screen-flow.mmd`
- 화면 와이어프레임 참고: `06-screen-design/wireframe-spec.md`

## 5. 구현 상태 표기 규칙

| 상태 | 의미 |
|---|---|
| `IMPLEMENTED` | 코드, 화면/API/DB 근거가 확인된 상태 |
| `PARTIAL` | 일부 구현은 있으나 화면, 외부 검증, 권한 정책 등이 불완전한 상태 |
| `UNVERIFIED` | 코드 근거는 있으나 실제 외부 연동, 실기동, 수동 검증이 필요한 상태 |
| `NOT_IMPLEMENTED` | 근거 코드가 확인되지 않는 상태 |
| `NOT_IMPLEMENTED_SCREEN` | API/요구사항은 있으나 화면 route/component가 없는 상태 |
| `DONE` | WBS/Gantt에서 산출물 또는 구현 완료가 확인된 작업 |
| `BLOCKED` | 외부 계정, Java/Docker 환경, API Key 등으로 검증이 막힌 작업 |

## 6. 근거 데이터 위치

- 전체 inventory: `00-evidence/project-inventory.md`
- 구현 근거 CSV: `00-evidence/implementation-evidence.csv`
- 현재 아키텍처: `00-evidence/architecture-as-is.md`
- 미해결 이슈: `00-evidence/open-issues.md`
- 재사용 JSON: `00-evidence/project-inventory.json`

## 7. 최종 검증 메모

- `FR-019`는 Spotify 플레이리스트 내보내기, `FR-020`은 Spotify 플레이리스트 가져오기 기준으로 통일했다.
- Gantt Chart와 화면 설계서는 현재 패키지 기준 생성 완료로 WBS/Gantt 상태를 갱신했다.
- `FR-005` 러닝 프로필은 발표 기준 JSP `/members/me/running-profile` 화면을 실제 구현 화면으로 사용한다. Vue 전용 러닝 프로필 화면은 발표 전 신규 구현 대상이 아니라 후속 SPA 통합 과제로 분리했다.
- Spotify OAuth/import/export, AI provider, Docker 전체 기동, Java 25 전체 테스트는 발표 전 수동 확인이 필요하다.
