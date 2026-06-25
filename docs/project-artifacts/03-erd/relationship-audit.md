# 관계 감사 보고서

## 1. 기준과 한계

- 기준 DDL: `ssafit/src/main/resources/sql/schema.sql`
- 보조 근거: `ssafit/src/main/resources/sql/migrations/*.sql`, `ssafit/src/main/resources/mappers/**/*.xml`, 도메인/DTO 코드, 요구사항 정의서
- 실제 DB 접속 쿼리는 수행하지 않았다. `application.yml`의 `spring.sql.init.mode=never`와 기존 inventory의 `DB runtime drift` 항목 때문에 운영/로컬 DB가 DDL과 다를 가능성은 남아 있다.

## 2. 실제 FK 관계

| ID | From | To | ON UPDATE | ON DELETE | 의미 |
|---|---|---|---|---|---|
| FK-01 | `running_profiles.member_id` | `members.id` | RESTRICT | RESTRICT | 회원별 러닝 프로필 |
| FK-02 | `music_reviews.member_id` | `members.id` | RESTRICT | RESTRICT | 리뷰 작성 회원 |
| FK-03 | `music_reviews.music_id` | `music.id` | RESTRICT | RESTRICT | 리뷰 대상 음악 |
| FK-04 | `playlists.owner_id` | `members.id` | RESTRICT | RESTRICT | 사용자 플레이리스트 소유자 |
| FK-05 | `playlist_items.playlist_id` | `playlists.id` | RESTRICT | CASCADE | 플레이리스트 삭제 시 수록곡 제거 |
| FK-06 | `playlist_items.music_id` | `music.id` | RESTRICT | RESTRICT | 수록 음악 |
| FK-07 | `playlist_saves.member_id` | `members.id` | RESTRICT | CASCADE | 회원 삭제 시 저장 관계 제거 |
| FK-08 | `playlist_saves.playlist_id` | `playlists.id` | RESTRICT | CASCADE | 플레이리스트 삭제 시 저장 관계 제거 |
| FK-09 | `recommendation_logs.member_id` | `members.id` | RESTRICT | RESTRICT | 추천 요청 회원 |
| FK-10 | `recommendation_logs.saved_playlist_id` | `playlists.id` | RESTRICT | SET NULL | 저장된 큐레이션 플레이리스트 |
| FK-11 | `ai_running_reports.member_id` | `members.id` | RESTRICT | RESTRICT | AI 리포트 대상 회원 |
| FK-12 | `spotify_connections.member_id` | `members.id` | RESTRICT | CASCADE | 회원별 Spotify 연결 |
| FK-13 | `running_records.member_id` | `members.id` | RESTRICT | RESTRICT | 러닝 기록 소유 회원 |
| FK-14 | `running_sessions.host_member_id` | `members.id` | RESTRICT | RESTRICT | 러닝 세션 주최 회원 |
| FK-15 | `running_session_participants.session_id` | `running_sessions.id` | RESTRICT | CASCADE | 세션 삭제 시 참여 관계 제거 |
| FK-16 | `running_session_participants.member_id` | `members.id` | RESTRICT | CASCADE | 회원 삭제 시 참여 관계 제거 |

## 3. 코드 기반 논리적 관계

| ID | 관계 | 근거 | 물리 FK 없음의 의미 |
|---|---|---|---|
| LR-01 | `running_profiles.preferred_genre` -> `music.genre` | `RunningProfileMapper`, `RecommendationService`, 음악 검색 조건 | 장르 문자열 불일치가 추천 품질 저하로 이어질 수 있다. |
| LR-02 | `recommendation_logs.result_summary` JSON -> `music.id` 목록 | `RecommendationLogMapper`, `RecommendationResult`, 추천 결과 저장 흐름 | JSON 내부 음악 ID는 FK로 보호되지 않아 음악 삭제/변경 시 과거 추천 결과와 불일치할 수 있다. |
| LR-03 | `recommendation_logs.request_conditions` JSON -> 프로필/기록 기반 요청 스냅샷 | 추천 로그 저장 로직 | 요청 당시 상태 보존용으로 보이며 원본 테이블 변경과 동기화되지 않는다. |
| LR-04 | `ai_running_reports.input_snapshot_json` -> `running_records`, `recommendation_logs` | `AiRunningReportAggregationService`, `AiRunningReportMapper` | 리포트 입력은 스냅샷 JSON이라 원본 기록/추천 로그 삭제와 독립적이다. |
| LR-05 | `music.spotify_track_id`, `music.spotify_uri` -> Spotify Track | `MusicMapper`, `SpotifyService`, `SpotifyRestApiClient` | 외부 Spotify 데이터와 내부 DB 정합성은 DB FK로 검증할 수 없다. |
| LR-06 | `playlists.spotify_playlist_id`, `playlists.spotify_playlist_url` -> Spotify Playlist | `PlaylistExportService`, `PlaylistMapper` | 외부 플레이리스트 삭제/권한 변경을 DB가 감지하지 못한다. |
| LR-07 | `spotify_connections.spotify_user_id` -> Spotify User | `SpotifyConnectionMapper`, OAuth callback | Spotify 계정 변경/연결 해제는 애플리케이션 로직과 외부 API 상태에 의존한다. |

## 4. 누락 가능성이 있는 FK와 정합성 위험

| 대상 | 현재 상태 | 위험 | 권장 개선안 |
|---|---|---|---|
| `recommendation_logs.result_summary` 내부 음악 ID | JSON 저장, FK 없음 | 음악 삭제 또는 Spotify metadata 갱신 후 과거 추천 결과가 실제 음악과 달라질 수 있다. | 추천 결과 상세 테이블을 별도 구성하거나 JSON 스키마 버전과 음악 스냅샷을 명확히 유지한다. |
| `ai_running_reports.input_snapshot_json` 내부 기록/추천 참조 | JSON 저장, FK 없음 | 러닝 기록 soft delete 또는 추천 로그 삭제 정책 변화 시 리포트 입력 근거 추적이 어려울 수 있다. | 스냅샷 보존 정책을 문서화하고 원본 ID를 함께 저장한다. |
| `running_profiles.preferred_genre` | 문자열 | 장르 오탈자, 대소문자 차이, 음악 장르 표준화 부재 | 장르 코드 테이블 또는 enum-like 검증을 도입한다. |
| `music.genre` | 문자열 | 검색/추천 조건이 자유 문자열에 의존 | 장르 normalization 정책을 추가한다. |
| `playlists.spotify_playlist_id` | 외부 ID UNIQUE, FK 없음 | Spotify에서 삭제된 playlist를 성공 상태로 계속 보유할 수 있다. | export 상태 갱신/검증 API를 별도 운영한다. |

## 5. 삭제 시 고아 데이터 위험

| 삭제 대상 | 현재 FK/Cascade | 위험 평가 |
|---|---|---|
| `members` | 다수 테이블이 `RESTRICT`, 일부 관계 테이블은 `CASCADE` | 실제 회원 삭제보다 `members.status/deleted_at` soft delete 정책이 필요하다. `music_reviews`, `playlists`, `running_records`, `running_sessions`, `recommendation_logs`, `ai_running_reports`가 RESTRICT로 물리 삭제를 막는다. |
| `music` | `music_reviews`, `playlist_items`가 RESTRICT | 음악이 리뷰/플레이리스트에 사용되면 물리 삭제가 막힌다. 과거 추천 JSON 안의 음악 ID는 물리 FK가 아니므로 별도 위험이 남는다. |
| `playlists` | items/saves는 CASCADE, recommendation log는 SET NULL | 플레이리스트 삭제 시 수록곡/저장 관계는 제거되지만 추천 로그의 저장 연결은 사라진다. soft delete와 물리 delete 정책이 혼재될 수 있다. |
| `running_sessions` | participants CASCADE | 세션 삭제 시 참여 관계는 제거된다. 단, 코드상 세션 취소는 상태 변경이므로 물리 삭제 사용 여부를 분리해야 한다. |
| `running_records` | 회원 FK만 존재, 추천/AI는 JSON 논리 참조 | 기록 soft delete 후 추천/AI 스냅샷은 남을 수 있다. 이는 이력 보존으로 볼 수 있지만 화면 표시 정책이 필요하다. |

## 6. Cascade 정책 부재 여부

- `members`를 기준으로 핵심 이력 테이블은 대부분 `ON DELETE RESTRICT`다. 이는 soft delete 정책과 일치하지만, 실제 물리 삭제 운영 절차가 없으면 개인정보 삭제 요구 대응이 어려울 수 있다.
- `playlist_items`, `playlist_saves`, `running_session_participants`, `spotify_connections`는 관계/연결성 테이블 성격이라 `ON DELETE CASCADE`가 적용되어 있다.
- `recommendation_logs.saved_playlist_id`는 `ON DELETE SET NULL`이라 저장된 플레이리스트가 삭제되어도 추천 로그 자체는 보존된다.

## 7. Spotify 관련 식별자 저장 구조

| 저장 위치 | 컬럼 | 제약 | 비고 |
|---|---|---|---|
| `spotify_connections` | `spotify_user_id` | UNIQUE, NOT NULL | 회원별 Spotify 사용자 식별자 |
| `spotify_connections` | `access_token`, `refresh_token` | NOT NULL | DDL 주석상 애플리케이션 계층 암호화 권장. 현재 DB 제약만으로 암호화 여부 확인 불가 |
| `spotify_connections` | `token_expires_at` | NOT NULL, INDEX | 토큰 갱신 대상 조회에 사용 가능 |
| `music` | `spotify_track_id` | UNIQUE, NULL 허용 | Spotify 곡 중복 저장 방지 |
| `music` | `spotify_uri` | UNIQUE, NULL 허용 | Spotify export 수록곡 추가에 필요 |
| `music` | `spotify_external_url` | NULL 허용 | 표시용 외부 URL |
| `playlists` | `spotify_playlist_id` | UNIQUE, NULL 허용 | export 성공/부분 성공 시 필수 |
| `playlists` | `spotify_playlist_url` | NULL 허용 | export 성공/부분 성공 시 필수 |
| `playlists` | `spotify_export_status`, `spotify_export_error_code`, `spotify_exported_at` | CHECK | export 결과 상태 관리 |

## 8. 추천 결과, 러닝 기록, 플레이리스트 수록곡 정합성 위험

| 영역 | 위험 | 현재 완화 장치 | 남은 과제 |
|---|---|---|---|
| 추천 결과 저장 | `recommendation_logs.result_summary`의 음악 목록과 실제 `playlist_items` 저장 결과가 달라질 수 있다. | `recommendation_logs.saved_playlist_id` FK와 서비스 트랜잭션 | 저장 실패/부분 저장 상태를 추천 로그에 더 세밀히 남길지 검토 |
| 러닝 기록 기반 추천 | `running_records`는 soft delete이고 추천/AI는 과거 스냅샷을 보존한다. | Mapper가 `deleted_at IS NULL` 조건으로 최신 계산에서 제외 | 과거 리포트가 삭제 전 데이터를 포함할 수 있음을 개인정보/이력 정책에 명시 |
| 플레이리스트 수록곡 | 같은 playlist 내 동일 music/order 중복은 UNIQUE로 차단된다. | `uk_playlist_items_playlist_music`, `uk_playlist_items_playlist_order` | 순서 변경 중 일시적 UNIQUE 충돌을 서비스에서 안전하게 처리해야 한다. |
| Spotify export | 모든 수록곡에 `spotify_uri`가 없으면 외부 추가가 제한된다. | `music.source_type='SPOTIFY'`일 때 track/uri 필수 CHECK | 내부 등록 음악에는 URI가 없을 수 있어 export 전 검증 필요 |

## 9. DDL, Migration, Mapper 충돌

| 항목 | 관찰 | 판단 |
|---|---|---|
| 최신 schema와 migration | migration에서 추가한 cadence, heart-rate, Spotify URI, AI report, profile fitness fields, session goal fields가 `schema.sql`에 반영되어 있다. | 신규 DB 기준은 `schema.sql`을 우선한다. |
| `20260624_simplify_running_profile_targets.sql` | 과거 `running_intensity` 컬럼과 관련 CHECK를 제거한다. `schema.sql`에는 해당 컬럼이 없다. | 현재 물리 스키마에는 `running_profiles.running_intensity`가 없는 것으로 문서화한다. |
| Mapper 컬럼 | 확인한 Mapper는 `schema.sql`의 최신 컬럼명과 대체로 일치한다. | 명시적 충돌 없음. 단 실제 DB 접속 미확인으로 런타임 드리프트는 남는다. |
| `demo-snapshot.sql` | FK_CHECKS를 끄고 seed 데이터를 적재한다. | DDL 충돌은 아니지만 seed 데이터가 CHECK/FK와 맞지 않으면 초기화 중 실패 가능성이 있다. |

## 10. 권장 개선안

다음은 현재 물리 스키마와 별개의 개선안이며, 본 산출물은 DB 스키마를 수정하지 않았다.

- Spotify token 저장값의 암호화 여부를 코드/운영 설정으로 명확히 검증하고 문서화한다.
- `recommendation_logs.result_summary`의 음악 ID/스냅샷 구조를 JSON Schema 수준으로 문서화한다.
- 장르를 자유 문자열로 둘지 코드 테이블로 분리할지 결정한다.
- 개인정보 삭제 요구를 고려해 `members` soft delete와 연관 이력 보존 정책을 별도 정의한다.
- 실제 운영 DB에 `SHOW CREATE TABLE` 또는 `information_schema` 조회를 수행해 `schema.sql`과 drift를 검증한다.
