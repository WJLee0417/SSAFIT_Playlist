# ERD 데이터 사전

기준 파일은 `ssafit/src/main/resources/sql/schema.sql`이다. migration 파일은 기존 DB 보정 근거로 함께 확인했으며, `schema.sql`에는 최신 migration 결과가 대부분 반영되어 있다. 실제 운영 DB에는 직접 접속하지 않았으므로 런타임 드리프트 가능성은 `relationship-audit.md`에 별도 기록한다.

## 도메인별 테이블 분류

| 도메인 | 테이블 | 연결 요구사항 |
|---|---|---|
| Member | `members` | FR-001, FR-002, FR-003, FR-004 |
| Running Profile | `running_profiles` | FR-005, FR-015 |
| Music | `music` | FR-008, FR-009, FR-017, FR-024 |
| Music Review | `music_reviews` | FR-009, FR-010 |
| Playlist | `playlists` | FR-011, FR-016, FR-019, FR-020 |
| Playlist Item | `playlist_items` | FR-012, FR-016, FR-019, FR-020 |
| Playlist Save | `playlist_saves` | FR-013 |
| Recommendation | `recommendation_logs` | FR-014, FR-015, FR-016 |
| Spotify 연동 정보 | `spotify_connections`, `music.spotify_*`, `playlists.spotify_*` | FR-017, FR-018, FR-019, FR-020 |
| Running Record | `running_records` | FR-006, FR-015, FR-021 |
| Running Session | `running_sessions`, `running_session_participants` | FR-007 |
| 기타 실제 존재 테이블 | `ai_running_reports` | FR-021 |

## members

목적: 회원 계정, 로그인 식별자, 세션 인증 대상, soft delete 상태를 저장한다.

| 컬럼명 | 타입 | Null | PK/FK | 기본값 | 설명 | 요구사항 |
|---|---|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | N | PK | AUTO_INCREMENT | 회원 식별자 | FR-001~FR-004 |
| `login_id` | `VARCHAR(50)` | N | UNIQUE | - | 로그인 ID | FR-001, FR-002 |
| `password_hash` | `VARCHAR(255)` | N | - | - | PBKDF2 비밀번호 해시 | FR-001, FR-002 |
| `email` | `VARCHAR(255)` | N | UNIQUE | - | 회원 이메일 | FR-001, FR-004 |
| `nickname` | `VARCHAR(100)` | N | - | - | 표시 이름 | FR-004 |
| `status` | `VARCHAR(20)` | N | CHECK | `'ACTIVE'` | `ACTIVE`, `INACTIVE`, `SUSPENDED`, `WITHDRAWN` | FR-004 |
| `created_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6)` | 생성 시각 | 공통 |
| `updated_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6) ON UPDATE` | 수정 시각 | 공통 |
| `deleted_at` | `DATETIME(6)` | Y | CHECK | `NULL` | 탈퇴 상태일 때만 값 허용 | FR-004 |

## running_profiles

목적: 회원별 1개의 러닝 프로필, 목표 BPM/케이던스, 선호 장르, 심박 추정값을 저장한다.

| 컬럼명 | 타입 | Null | PK/FK | 기본값 | 설명 | 요구사항 |
|---|---|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | N | PK | AUTO_INCREMENT | 프로필 식별자 | FR-005 |
| `member_id` | `BIGINT UNSIGNED` | N | FK, UNIQUE | - | `members.id`, 회원당 1개 | FR-005 |
| `gender` | `VARCHAR(20)` | Y | CHECK | `NULL` | `FEMALE`, `MALE`, `OTHER`, `UNSPECIFIED` | FR-005 |
| `age` | `TINYINT UNSIGNED` | Y | CHECK | `NULL` | 10~100 | FR-005 |
| `estimated_max_heart_rate_bpm` | `SMALLINT UNSIGNED` | Y | CHECK | `NULL` | 120~210 | FR-015 |
| `target_bpm_min` | `SMALLINT UNSIGNED` | Y | CHECK | `NULL` | 목표 음악 BPM 하한, 60~220 | FR-005, FR-015 |
| `target_bpm_max` | `SMALLINT UNSIGNED` | Y | CHECK | `NULL` | 목표 음악 BPM 상한, 60~220 | FR-005, FR-015 |
| `target_cadence_min` | `SMALLINT UNSIGNED` | Y | CHECK | `NULL` | 목표 케이던스 하한, 60~240 | FR-005, FR-015 |
| `target_cadence_max` | `SMALLINT UNSIGNED` | Y | CHECK | `NULL` | 목표 케이던스 상한, 60~240 | FR-005, FR-015 |
| `preferred_genre` | `VARCHAR(100)` | Y | 논리 관계 | `NULL` | `music.genre`와 논리적으로 매칭 | FR-015 |
| `created_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6)` | 생성 시각 | 공통 |
| `updated_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6) ON UPDATE` | 수정 시각 | 공통 |

## music

목적: 내부/seed/Spotify 음악 메타데이터와 추천에 필요한 BPM, 장르, 에너지, Spotify 식별자를 저장한다.

| 컬럼명 | 타입 | Null | PK/FK | 기본값 | 설명 | 요구사항 |
|---|---|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | N | PK | AUTO_INCREMENT | 음악 식별자 | FR-008 |
| `title` | `VARCHAR(255)` | N | FULLTEXT | - | 곡명 | FR-008 |
| `artist` | `VARCHAR(255)` | N | FULLTEXT | - | 아티스트 | FR-008 |
| `album_name` | `VARCHAR(255)` | Y | - | `NULL` | 앨범명 | FR-009 |
| `image_url` | `VARCHAR(500)` | Y | - | `NULL` | 커버 이미지 URL | FR-009 |
| `genre` | `VARCHAR(100)` | Y | INDEX | `NULL` | 장르. 프로필/추천 조건과 논리 매칭 | FR-008, FR-015 |
| `bpm` | `SMALLINT UNSIGNED` | Y | INDEX, CHECK | `NULL` | 음악 템포, 60~220 | FR-015 |
| `duration_ms` | `INT UNSIGNED` | Y | CHECK | `NULL` | 재생 길이, 0 초과 | FR-017 |
| `music_energy_level` | `VARCHAR(20)` | Y | CHECK | `NULL` | `CALM`, `STEADY`, `ENERGETIC`, `PEAK` | FR-015 |
| `source_type` | `VARCHAR(20)` | N | INDEX, CHECK | - | `ADMIN`, `SEED`, `EXTERNAL`, `SPOTIFY` | FR-017, FR-024 |
| `spotify_track_id` | `VARCHAR(100)` | Y | UNIQUE | `NULL` | Spotify Track ID | FR-017 |
| `spotify_uri` | `VARCHAR(255)` | Y | UNIQUE | `NULL` | Spotify URI. Export 시 필요 | FR-017, FR-019 |
| `spotify_external_url` | `VARCHAR(500)` | Y | - | `NULL` | Spotify 외부 URL | FR-017 |
| `created_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6)` | 생성 시각 | 공통 |
| `updated_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6) ON UPDATE` | 수정 시각 | 공통 |

## music_reviews

목적: 회원별 음악 선호도와 러닝 적합도 평가를 저장한다.

| 컬럼명 | 타입 | Null | PK/FK | 기본값 | 설명 | 요구사항 |
|---|---|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | N | PK | AUTO_INCREMENT | 리뷰 식별자 | FR-010 |
| `member_id` | `BIGINT UNSIGNED` | N | FK, UNIQUE 조합 | - | 작성 회원 | FR-010 |
| `music_id` | `BIGINT UNSIGNED` | N | FK, UNIQUE 조합 | - | 대상 음악 | FR-010 |
| `preference_score` | `TINYINT UNSIGNED` | N | CHECK | - | 선호도 1~5 | FR-010 |
| `running_fit_score` | `TINYINT UNSIGNED` | N | INDEX, CHECK | - | 러닝 적합도 1~5 | FR-010, FR-015 |
| `review_content` | `TEXT` | Y | - | `NULL` | 리뷰 내용 | FR-010 |
| `created_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6)` | 생성 시각 | 공통 |
| `updated_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6) ON UPDATE` | 수정 시각 | 공통 |

## playlists

목적: 사용자 수동 플레이리스트, 공식 플레이리스트, 추천 결과 복사본과 Spotify export 상태를 저장한다.

| 컬럼명 | 타입 | Null | PK/FK | 기본값 | 설명 | 요구사항 |
|---|---|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | N | PK | AUTO_INCREMENT | 플레이리스트 식별자 | FR-011 |
| `owner_id` | `BIGINT UNSIGNED` | Y | FK | `NULL` | 사용자 플레이리스트 소유자. 공식은 NULL | FR-011 |
| `playlist_type` | `VARCHAR(20)` | N | CHECK | - | `USER`, `OFFICIAL` | FR-011 |
| `source_type` | `VARCHAR(20)` | N | CHECK | - | `MANUAL`, `OFFICIAL`, `CURATION_COPY` | FR-011, FR-016 |
| `visibility` | `VARCHAR(20)` | N | INDEX, CHECK | `'PRIVATE'` | `PUBLIC`, `PRIVATE` | FR-011, FR-013 |
| `title` | `VARCHAR(200)` | N | - | - | 제목 | FR-011 |
| `description` | `TEXT` | Y | - | `NULL` | 설명 | FR-011 |
| `spotify_playlist_id` | `VARCHAR(100)` | Y | UNIQUE | `NULL` | Export 결과 Spotify Playlist ID | FR-019 |
| `spotify_playlist_url` | `VARCHAR(500)` | Y | - | `NULL` | Export 결과 Spotify URL | FR-019 |
| `spotify_export_status` | `VARCHAR(20)` | N | CHECK | `'NOT_EXPORTED'` | `NOT_EXPORTED`, `PENDING`, `SUCCESS`, `PARTIAL`, `FAILED` | FR-019 |
| `spotify_export_error_code` | `VARCHAR(50)` | Y | CHECK | `NULL` | 실패/부분 성공 오류 코드 | FR-019 |
| `spotify_exported_at` | `DATETIME(6)` | Y | CHECK | `NULL` | 성공/부분 성공 시각 | FR-019 |
| `created_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6)` | 생성 시각 | 공통 |
| `updated_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6) ON UPDATE` | 수정 시각 | 공통 |
| `deleted_at` | `DATETIME(6)` | Y | INDEX | `NULL` | soft delete 시각 | FR-011 |

## playlist_items

목적: 플레이리스트와 음악의 다대다 관계 및 재생 순서를 저장한다.

| 컬럼명 | 타입 | Null | PK/FK | 기본값 | 설명 | 요구사항 |
|---|---|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | N | PK | AUTO_INCREMENT | 수록곡 식별자 | FR-012 |
| `playlist_id` | `BIGINT UNSIGNED` | N | FK, UNIQUE 조합 | - | 플레이리스트 | FR-012 |
| `music_id` | `BIGINT UNSIGNED` | N | FK, UNIQUE 조합 | - | 음악 | FR-012 |
| `order_no` | `INT UNSIGNED` | N | UNIQUE 조합, CHECK | - | 1 이상 순서 | FR-012 |
| `created_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6)` | 추가 시각 | 공통 |

## playlist_saves

목적: 회원이 공개/공식 플레이리스트를 저장한 관계를 저장한다.

| 컬럼명 | 타입 | Null | PK/FK | 기본값 | 설명 | 요구사항 |
|---|---|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | N | PK | AUTO_INCREMENT | 저장 관계 식별자 | FR-013 |
| `member_id` | `BIGINT UNSIGNED` | N | FK, UNIQUE 조합 | - | 저장한 회원 | FR-013 |
| `playlist_id` | `BIGINT UNSIGNED` | N | FK, UNIQUE 조합 | - | 저장 대상 플레이리스트 | FR-013 |
| `created_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6)` | 저장 시각 | FR-013 |

## recommendation_logs

목적: 추천 요청 조건, 추천 결과 요약, 추천 결과 저장 플레이리스트 연결을 기록한다.

| 컬럼명 | 타입 | Null | PK/FK | 기본값 | 설명 | 요구사항 |
|---|---|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | N | PK | AUTO_INCREMENT | 추천 로그 식별자 | FR-014 |
| `member_id` | `BIGINT UNSIGNED` | N | FK | - | 추천 요청 회원 | FR-014 |
| `requested_intensity` | `VARCHAR(20)` | N | CHECK | - | `LOW`, `MODERATE`, `HIGH`, `INTERVAL` | FR-015 |
| `requested_bpm_min` | `SMALLINT UNSIGNED` | N | INDEX, CHECK | - | 요청 BPM 하한, 60~220 | FR-015 |
| `requested_bpm_max` | `SMALLINT UNSIGNED` | N | INDEX, CHECK | - | 요청 BPM 상한, 60~220 | FR-015 |
| `requested_genre` | `VARCHAR(100)` | Y | INDEX | `NULL` | 요청 장르 | FR-015 |
| `request_conditions` | `JSON` | Y | - | `NULL` | 요청 조건 상세 JSON | FR-014 |
| `result_summary` | `JSON` | N | 논리 관계 | - | 추천 결과 JSON. 음악 ID는 물리 FK 없음 | FR-014, FR-016 |
| `recommendation_reason` | `TEXT` | Y | - | `NULL` | 추천 사유 | FR-014 |
| `saved_playlist_id` | `BIGINT UNSIGNED` | Y | FK | `NULL` | 저장된 큐레이션 복사 플레이리스트 | FR-016 |
| `created_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6)` | 생성 시각 | 공통 |

## ai_running_reports

목적: AI 또는 fallback으로 생성한 러닝 리포트와 입력 스냅샷을 저장한다.

| 컬럼명 | 타입 | Null | PK/FK | 기본값 | 설명 | 요구사항 |
|---|---|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | N | PK | AUTO_INCREMENT | 리포트 식별자 | FR-021 |
| `member_id` | `BIGINT UNSIGNED` | N | FK | - | 리포트 대상 회원 | FR-021 |
| `report_type` | `VARCHAR(50)` | N | CHECK | - | `RUNNING_SUMMARY` | FR-021 |
| `analysis_start_date` | `DATE` | Y | - | `NULL` | 분석 시작일 | FR-021 |
| `analysis_end_date` | `DATE` | Y | - | `NULL` | 분석 종료일 | FR-021 |
| `input_snapshot_hash` | `CHAR(64)` | N | INDEX, CHECK | - | 입력 스냅샷 해시 | FR-021 |
| `input_snapshot_json` | `JSON` | N | 논리 관계 | - | 러닝 기록/추천 로그 스냅샷 | FR-021 |
| `status` | `VARCHAR(20)` | N | INDEX, CHECK | - | `PENDING`, `COMPLETED`, `FAILED` | FR-021 |
| `generation_source` | `VARCHAR(20)` | N | CHECK | - | `LLM`, `FALLBACK` | FR-021 |
| `provider` | `VARCHAR(50)` | Y | - | `NULL` | 외부 provider | FR-021 |
| `model_name` | `VARCHAR(100)` | Y | - | `NULL` | 모델명 | FR-021 |
| `prompt_version` | `VARCHAR(50)` | N | - | - | 프롬프트 버전 | FR-021 |
| `report_content_json` | `JSON` | Y | CHECK | `NULL` | 완료 리포트 본문 | FR-021 |
| `error_code` | `VARCHAR(50)` | Y | CHECK | `NULL` | 실패 오류 코드 | FR-021 |
| `created_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6)` | 생성 시각 | 공통 |
| `updated_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6) ON UPDATE` | 수정 시각 | 공통 |

## spotify_connections

목적: 회원별 Spotify OAuth 연결 정보와 토큰을 저장한다.

| 컬럼명 | 타입 | Null | PK/FK | 기본값 | 설명 | 요구사항 |
|---|---|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | N | PK | AUTO_INCREMENT | 연결 식별자 | FR-018 |
| `member_id` | `BIGINT UNSIGNED` | N | FK, UNIQUE | - | 회원당 1개 Spotify 연결 | FR-018 |
| `spotify_user_id` | `VARCHAR(100)` | N | UNIQUE | - | Spotify 사용자 ID | FR-018 |
| `access_token` | `VARCHAR(2048)` | N | - | - | Spotify Access Token | FR-018 |
| `refresh_token` | `VARCHAR(2048)` | N | - | - | Spotify Refresh Token | FR-018 |
| `token_expires_at` | `DATETIME(6)` | N | INDEX | - | Access Token 만료 시각 | FR-018 |
| `created_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6)` | 생성 시각 | 공통 |
| `updated_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6) ON UPDATE` | 수정 시각 | 공통 |

## running_records

목적: 회원 개인 러닝 기록과 추천 계산에 필요한 케이던스/심박 데이터를 저장한다.

| 컬럼명 | 타입 | Null | PK/FK | 기본값 | 설명 | 요구사항 |
|---|---|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | N | PK | AUTO_INCREMENT | 기록 식별자 | FR-006 |
| `member_id` | `BIGINT UNSIGNED` | N | FK, INDEX | - | 기록 소유 회원 | FR-006 |
| `record_date` | `DATE` | N | INDEX | - | 러닝 일자 | FR-006 |
| `distance_km` | `DECIMAL(10,2)` | N | CHECK | - | 0 초과 거리 | FR-006 |
| `duration_seconds` | `INT UNSIGNED` | N | CHECK | - | 0 초과 시간 | FR-006 |
| `pace_seconds_per_km` | `INT UNSIGNED` | N | CHECK | - | 0 초과 페이스 | FR-006 |
| `average_cadence` | `SMALLINT UNSIGNED` | Y | CHECK | `NULL` | 평균 케이던스, 60~240 | FR-015 |
| `average_heart_rate_bpm` | `SMALLINT UNSIGNED` | Y | CHECK | `NULL` | 평균 심박, 30~240 | FR-015 |
| `memo` | `TEXT` | Y | - | `NULL` | 메모 | FR-006 |
| `created_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6)` | 생성 시각 | 공통 |
| `updated_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6) ON UPDATE` | 수정 시각 | 공통 |
| `deleted_at` | `DATETIME(6)` | Y | INDEX | `NULL` | soft delete 시각 | FR-006 |

## running_sessions

목적: 회원이 생성한 러닝 모임과 모집 상태, 목표 거리/페이스를 저장한다.

| 컬럼명 | 타입 | Null | PK/FK | 기본값 | 설명 | 요구사항 |
|---|---|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | N | PK | AUTO_INCREMENT | 세션 식별자 | FR-007 |
| `host_member_id` | `BIGINT UNSIGNED` | N | FK, INDEX | - | 주최 회원 | FR-007 |
| `title` | `VARCHAR(200)` | N | CHECK | - | 제목, trim 후 빈 값 불가 | FR-007 |
| `description` | `TEXT` | Y | - | `NULL` | 설명 | FR-007 |
| `meeting_place` | `VARCHAR(255)` | N | - | - | 집결 장소 | FR-007 |
| `scheduled_at` | `DATETIME(6)` | N | INDEX | - | 예정 시각 | FR-007 |
| `capacity` | `INT UNSIGNED` | N | CHECK | - | 정원, 0 초과 | FR-007 |
| `target_distance_km` | `DECIMAL(10,2)` | Y | CHECK | `NULL` | 목표 거리, 0 초과 | FR-007 |
| `target_pace_seconds_per_km` | `INT UNSIGNED` | Y | CHECK | `NULL` | 목표 페이스, 0 초과 | FR-007 |
| `status` | `VARCHAR(20)` | N | INDEX, CHECK | `'OPEN'` | `OPEN`, `CLOSED`, `CANCELLED` | FR-007 |
| `cancelled_at` | `DATETIME(6)` | Y | CHECK | `NULL` | 취소 시각 | FR-007 |
| `created_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6)` | 생성 시각 | 공통 |
| `updated_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6) ON UPDATE` | 수정 시각 | 공통 |

## running_session_participants

목적: 러닝 모임 참여 회원 관계를 저장한다.

| 컬럼명 | 타입 | Null | PK/FK | 기본값 | 설명 | 요구사항 |
|---|---|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | N | PK | AUTO_INCREMENT | 참여 식별자 | FR-007 |
| `session_id` | `BIGINT UNSIGNED` | N | FK, UNIQUE 조합 | - | 러닝 세션 | FR-007 |
| `member_id` | `BIGINT UNSIGNED` | N | FK, UNIQUE 조합 | - | 참여 회원 | FR-007 |
| `joined_at` | `DATETIME(6)` | N | - | `CURRENT_TIMESTAMP(6)` | 참여 시각 | FR-007 |
