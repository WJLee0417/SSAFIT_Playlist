# draw.io / PlantUML Import Guide

이 디렉터리의 `.puml` 파일을 Use Case Diagram의 공식 원본으로 사용한다. 저장소와 PATH에서 `drawio`, `plantuml`, `.drawio` 생성용 CLI 또는 관련 스크립트가 확인되지 않았으므로, 이 단계에서는 `.drawio` XML 파일을 임의 생성하지 않았다.

## 파일별 용도

| 파일 | 용도 |
| --- | --- |
| `use-case-overview.puml` | 전체 Actor와 핵심 Use Case만 담은 개요 다이어그램 |
| `use-case-member.puml` | 회원가입, 로그인, 로그아웃, 회원 정보, 러닝 프로필 |
| `use-case-music-playlist.puml` | 음악 조회/상세/리뷰, 내부 플레이리스트, 수록곡, 저장 |
| `use-case-running-recommendation.puml` | 러닝 기록, 러닝 세션, 자동 큐레이션, 운동 데이터 기반 추천, AI 러닝 리포트 |
| `use-case-spotify.puml` | Spotify 검색, OAuth, 플레이리스트 생성, 내보내기, 가져오기 |
| `use-case-specification.md` | Use Case별 선행조건, 기본 흐름, 예외 흐름, 후행조건, 요구사항 연결 |

## PlantUML에서 시각화

1. PlantUML 확장 또는 PlantUML 서버를 사용할 수 있는 도구를 연다.
2. 원하는 `.puml` 파일을 연다.
3. Preview 또는 Export 기능으로 PNG/SVG/PDF를 생성한다.
4. 렌더링 오류가 있으면 `@startuml`부터 `@enduml`까지 전체 문법을 기준으로 확인한다.

예시:

```bash
plantuml docs/project-artifacts/02-usecase/use-case-overview.puml
```

현재 로컬 PATH에는 `plantuml` 명령이 확인되지 않았으므로, 위 명령은 PlantUML을 별도로 설치한 환경에서 실행한다.

## draw.io에서 활용하는 방법

draw.io는 PlantUML 텍스트를 직접 다이어그램으로 변환하거나, PlantUML 렌더링 결과 이미지를 가져오는 방식으로 사용할 수 있다.

권장 흐름:

1. PlantUML 호환 도구에서 `.puml`을 SVG 또는 PNG로 렌더링한다.
2. draw.io에서 `File > Import From > Device` 또는 드래그 앤 드롭으로 렌더링 결과를 가져온다.
3. draw.io에서 편집 가능한 도형이 필요하면, `.puml` 내용을 기준으로 수동 재구성한다.
4. 최종 draw.io 파일을 만들 때도 `.puml`을 원본으로 유지하고, draw.io는 발표/편집용 파생본으로 관리한다.

## 상태 표기 규칙

- `<<partial>>`: 코드 구현은 있으나 화면 일부 부재, 권한 모델 미확정, 또는 외부 API 실검증 필요 상태.
- `<<planned>>`: 목표 요구사항은 있으나 현재 구현 근거가 없는 상태. 이번 Use Case 산출물에는 해당되는 Use Case가 없다.
- stereotype이 없는 Use Case: 현재 근거상 `IMPLEMENTED`.

## 다이어그램 작성 원칙

- 관리자 Actor는 구현 근거가 없으므로 포함하지 않았다.
- Spotify API와 OpenAI 호환 API는 외부 시스템 Actor로 표현했다.
- `include` 관계는 실제 흐름상 필수적인 경우에만 사용했다.
- `Spotify 내보내기`는 `Spotify OAuth 연결`과 `Spotify 플레이리스트 생성`을 `include`한다.
- JSP와 Vue 혼합 구조는 다이어그램보다 `use-case-specification.md`의 선행조건/예외 흐름에서 더 자세히 설명한다.
