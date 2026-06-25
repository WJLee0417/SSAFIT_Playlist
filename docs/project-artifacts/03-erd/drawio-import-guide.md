# ERD Draw.io 가져오기 가이드

## 1. 산출물 용도

| 파일 | 용도 | 권장 사용 |
|---|---|---|
| `erd.dbml` | 전체 실제 테이블, 컬럼, 인덱스, 물리 FK를 DBML로 표현 | dbdiagram.io, DBML 호환 도구에서 상세 ERD 생성 |
| `erd.mmd` | 핵심 테이블과 관계를 Mermaid ER Diagram으로 간결하게 표현 | Markdown Preview, Mermaid Live Editor, draw.io Mermaid 삽입 |
| `erd-data-dictionary.md` | 테이블/컬럼 상세 사전과 요구사항 연결 | DB 설계서 본문 |
| `relationship-audit.md` | 물리 FK, 논리 관계, 정합성 위험 감사 | 설계 검토 및 개선 과제 도출 |

## 2. DBML 시각화 방법

1. `docs/project-artifacts/03-erd/erd.dbml`을 연다.
2. dbdiagram.io 같은 DBML 호환 도구에 붙여 넣는다.
3. `Ref:`로 표시된 관계는 실제 DDL의 FK이다.
4. 파일 하단의 `논리 관계`, `외부 식별자` 주석은 DBML 관계선이 아니라 감사 문서와 함께 수동 표기한다.

## 3. Mermaid 시각화 방법

1. `docs/project-artifacts/03-erd/erd.mmd`를 연다.
2. Mermaid Live Editor 또는 Markdown Mermaid preview에서 렌더링한다.
3. draw.io에서는 `Arrange > Insert > Advanced > Mermaid` 메뉴를 사용해 내용을 붙여 넣는다.
4. Mermaid 파일의 마지막 `%%` 주석은 논리 관계 설명이며 실제 선으로 그리지 않았다.

## 4. Draw.io에서 직접 재작성할 엔터티 목록

| 도메인 | 엔터티 |
|---|---|
| Member | `members` |
| Running Profile | `running_profiles` |
| Music | `music` |
| Music Review | `music_reviews` |
| Playlist | `playlists` |
| Playlist Item | `playlist_items` |
| Playlist Save | `playlist_saves` |
| Recommendation | `recommendation_logs` |
| Spotify | `spotify_connections`, `music.spotify_*`, `playlists.spotify_*` |
| Running Record | `running_records` |
| Running Session | `running_sessions`, `running_session_participants` |
| AI Report | `ai_running_reports` |

## 5. Draw.io에서 직접 재작성할 실제 관계 목록

| From | To | 관계 |
|---|---|---|
| `running_profiles.member_id` | `members.id` | 1:1 |
| `music_reviews.member_id` | `members.id` | N:1 |
| `music_reviews.music_id` | `music.id` | N:1 |
| `playlists.owner_id` | `members.id` | N:1, nullable |
| `playlist_items.playlist_id` | `playlists.id` | N:1 |
| `playlist_items.music_id` | `music.id` | N:1 |
| `playlist_saves.member_id` | `members.id` | N:1 |
| `playlist_saves.playlist_id` | `playlists.id` | N:1 |
| `recommendation_logs.member_id` | `members.id` | N:1 |
| `recommendation_logs.saved_playlist_id` | `playlists.id` | N:1, nullable |
| `ai_running_reports.member_id` | `members.id` | N:1 |
| `spotify_connections.member_id` | `members.id` | 1:1 |
| `running_records.member_id` | `members.id` | N:1 |
| `running_sessions.host_member_id` | `members.id` | N:1 |
| `running_session_participants.session_id` | `running_sessions.id` | N:1 |
| `running_session_participants.member_id` | `members.id` | N:1 |

## 6. 별도 주석으로 표시할 논리 관계

| 관계 | 표시 권장 |
|---|---|
| `running_profiles.preferred_genre` -> `music.genre` | 점선, `logical` 라벨 |
| `recommendation_logs.result_summary` JSON -> `music.id` 목록 | 점선, `JSON snapshot` 라벨 |
| `ai_running_reports.input_snapshot_json` -> `running_records`, `recommendation_logs` | 점선, `snapshot only` 라벨 |
| `music.spotify_track_id`, `music.spotify_uri` -> Spotify Track | 외부 시스템 박스 연결 |
| `playlists.spotify_playlist_id` -> Spotify Playlist | 외부 시스템 박스 연결 |
| `spotify_connections.spotify_user_id` -> Spotify User | 외부 시스템 박스 연결 |

## 7. `.drawio` 파일 생성 여부

현재 저장소에서 draw.io CLI, PlantUML/DBML 변환 CLI, 또는 `.drawio` 생성 스크립트가 확인되지 않았다. 따라서 임의 XML을 생성하지 않고 `erd.dbml`과 `erd.mmd`를 공식 원본으로 유지한다.
