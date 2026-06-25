# Gantt 렌더링 및 활용 가이드

## 1. Mermaid 렌더링 방법

1. `docs/project-artifacts/05-gantt/gantt.mmd`를 연다.
2. Mermaid Live Editor 또는 Mermaid 지원 Markdown Preview에 내용을 붙여 넣는다.
3. `dateFormat YYYY-MM-DD HH:mm` 형식을 지원하는 렌더러를 사용한다.
4. `done`은 Git/문서/코드 근거로 완료된 작업, `active`는 현재 진행 중이거나 발표 전 확인 중인 작업이다.
5. `[ASSUMPTION]`이 붙은 작업은 Git log로 실제 수행 일정을 확정할 수 없는 계획성 일정이다.
6. `최종 발표 시작`은 `2026-06-26 09:00` milestone이다.

## 2. CSV 활용 방법

`docs/project-artifacts/05-gantt/gantt.csv`는 다음 도구에서 바로 사용할 수 있다.

- Excel: CSV를 열고 `Actual_Start`, `Actual_End`, `Planned_Start`, `Planned_End`를 날짜/시간 형식으로 지정한다.
- Notion: Table database로 import한 뒤 `Status`, `Schedule_Basis`, `Owner`를 select 속성으로 변환한다.
- 프로젝트 관리 도구: `Task_ID`를 고유 키로 사용하고 `Dependency`를 선행 작업 필드로 매핑한다.

## 3. 컬럼 해석

| 컬럼 | 의미 |
|---|---|
| `Task_ID` | Gantt와 CSV 공통 작업 ID |
| `Parent_Task_ID` | 상위 작업 ID. 본 파일은 요약 Gantt라 대부분 비워 두었다. |
| `Task_Name` | 작업명 |
| `Owner` | WBS 기준 담당. 문서 근거가 부족하면 `공동 또는 확인 필요` |
| `Status` | `DONE`, `IN_PROGRESS`, `NOT_STARTED`, `BLOCKED`, `UNVERIFIED` |
| `Actual_Start`, `Actual_End` | Git log 또는 문서 근거로 확인한 실제 수행 구간 |
| `Planned_Start`, `Planned_End` | 발표 전 계획 구간. 근거가 없으면 `ASSUMPTION` |
| `Dependency` | 선행 Task ID |
| `Milestone` | `Y`이면 milestone |
| `Schedule_Basis` | `GIT_LOG`, `PROJECT_DOCUMENT`, `CODE_EVIDENCE`, `ASSUMPTION`, `MANUAL_CONFIRMATION_NEEDED` |

## 4. Draw.io로 옮길 때 참고할 Task ID

| 영역 | Task ID |
|---|---|
| 프로젝트 기반 구축 | `G-001`~`G-005` |
| 회원 및 인증 | `G-101`~`G-106` |
| 음악 및 리뷰 | `G-201`~`G-206` |
| 플레이리스트 | `G-301`~`G-305` |
| 러닝 기록 및 추천 | `G-401`~`G-406` |
| Spotify 연동 | `G-501`~`G-505` |
| 프론트엔드 | `G-601`~`G-605` |
| 테스트 및 Docker | `G-701`~`G-707` |
| 문서화 및 발표 준비 | `G-801`~`G-806` |

## 5. 주요 Dependency

| Dependency | 의미 |
|---|---|
| `G-003 -> G-101/G-201/G-301/G-401` | DB/Mapper/Service 기반 후 도메인 기능 구현 |
| `G-601 -> G-203/G-303/G-402/G-502/G-602` | Vue Router/API 기반 후 화면 연결 |
| `G-501 -> G-502 -> G-504 -> G-505` | Spotify 내부 구현, UI 연결, 실제 OAuth, 실제 import/export 검증 순서 |
| `G-703 -> G-706` | Docker 구성 후 전체 기동 검증 |
| `G-604/G-706 -> G-707 -> G-804 -> G-805 -> G-806` | 시연 UI 점검, 전체 브라우저 검증, 체크리스트, 리허설, 발표 시작 |

## 6. 주의 사항

- 완료 작업의 날짜는 Git log의 커밋 날짜를 기준으로 하며, 실제 투입 시간 또는 작업 시간을 의미하지 않는다.
- `ASSUMPTION` 작업은 발표 전 필요 작업을 시간표에 올린 것이며, 완료로 간주하지 않는다.
- `MANUAL_CONFIRMATION_NEEDED` 작업은 외부 계정, 환경 변수, 실기동, 브라우저 확인이 필요하다.
