-- SSAFIT PlayList MVP schema
-- MySQL 8.0 이상에서 직접 실행한다.
-- application.yml의 spring.sql.init.mode가 never이므로 애플리케이션 시작 시 자동 실행되지 않는다.
-- DB 테이블/컬럼은 snake_case, Java Domain 필드는 camelCase를 사용한다.

SET NAMES utf8mb4;

-- 1. 회원
-- 회원 삭제는 물리 삭제보다 status 변경과 deleted_at 기록을 우선하는 soft delete 정책을 사용한다.
CREATE TABLE members (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    login_id VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_members PRIMARY KEY (id),
    CONSTRAINT uk_members_login_id UNIQUE (login_id),
    CONSTRAINT uk_members_email UNIQUE (email),
    CONSTRAINT ck_members_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'WITHDRAWN')),
    CONSTRAINT ck_members_deleted_state
        CHECK (
            (status = 'WITHDRAWN' AND deleted_at IS NOT NULL)
            OR (status <> 'WITHDRAWN' AND deleted_at IS NULL)
        )
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '회원';

-- 2. 러닝 프로필
-- 회원당 하나의 프로필만 저장한다.
CREATE TABLE running_profiles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    running_intensity VARCHAR(20) NOT NULL,
    target_bpm_min SMALLINT UNSIGNED NOT NULL,
    target_bpm_max SMALLINT UNSIGNED NOT NULL,
    target_cadence_min SMALLINT UNSIGNED NULL,
    target_cadence_max SMALLINT UNSIGNED NULL,
    preferred_genre VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_running_profiles PRIMARY KEY (id),
    CONSTRAINT uk_running_profiles_member_id UNIQUE (member_id),
    CONSTRAINT fk_running_profiles_member
        FOREIGN KEY (member_id) REFERENCES members (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT ck_running_profiles_intensity
        CHECK (running_intensity IN ('LOW', 'MODERATE', 'HIGH', 'INTERVAL')),
    CONSTRAINT ck_running_profiles_bpm_min
        CHECK (target_bpm_min BETWEEN 60 AND 220),
    CONSTRAINT ck_running_profiles_bpm_max
        CHECK (target_bpm_max BETWEEN 60 AND 220),
    CONSTRAINT ck_running_profiles_bpm_range
        CHECK (target_bpm_min <= target_bpm_max),
    CONSTRAINT ck_running_profiles_cadence_min
        CHECK (
            target_cadence_min IS NULL
            OR target_cadence_min BETWEEN 60 AND 240
        ),
    CONSTRAINT ck_running_profiles_cadence_max
        CHECK (
            target_cadence_max IS NULL
            OR target_cadence_max BETWEEN 60 AND 240
        ),
    CONSTRAINT ck_running_profiles_cadence_range
        CHECK (
            (target_cadence_min IS NULL AND target_cadence_max IS NULL)
            OR (
                target_cadence_min IS NOT NULL
                AND target_cadence_max IS NOT NULL
                AND target_cadence_min <= target_cadence_max
            )
        )
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '회원별 러닝 프로필';

-- 3. 음악
-- Spotify에서 선택한 곡은 spotify_track_id를 기준으로 중복 저장을 방지한다.
CREATE TABLE music (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NOT NULL,
    album_name VARCHAR(255) NULL,
    image_url VARCHAR(500) NULL,
    genre VARCHAR(100) NULL,
    bpm SMALLINT UNSIGNED NULL COMMENT '음악 템포 BPM. 케이던스 SPM 및 평균 심박수 BPM과 별도 값',
    duration_ms INT UNSIGNED NULL,
    music_energy_level VARCHAR(20) NULL,
    source_type VARCHAR(20) NOT NULL,
    spotify_track_id VARCHAR(100) NULL,
    spotify_uri VARCHAR(255) NULL,
    spotify_external_url VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_music PRIMARY KEY (id),
    CONSTRAINT uk_music_spotify_track_id UNIQUE (spotify_track_id),
    CONSTRAINT uk_music_spotify_uri UNIQUE (spotify_uri),
    CONSTRAINT ck_music_bpm
        CHECK (bpm IS NULL OR bpm BETWEEN 60 AND 220),
    CONSTRAINT ck_music_duration
        CHECK (duration_ms IS NULL OR duration_ms > 0),
    CONSTRAINT ck_music_energy_level
        CHECK (
            music_energy_level IS NULL
            OR music_energy_level IN ('CALM', 'STEADY', 'ENERGETIC', 'PEAK')
        ),
    CONSTRAINT ck_music_source_type
        CHECK (source_type IN ('ADMIN', 'SEED', 'EXTERNAL', 'SPOTIFY')),
    CONSTRAINT ck_music_spotify_identity
        CHECK (
            source_type <> 'SPOTIFY'
            OR (
                spotify_track_id IS NOT NULL
                AND spotify_uri IS NOT NULL
            )
        ),
    INDEX idx_music_genre (genre),
    INDEX idx_music_bpm (bpm),
    INDEX idx_music_source_bpm (source_type, bpm),
    FULLTEXT INDEX ft_music_keyword (title, artist)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '중앙 관리 음악 메타데이터';

-- 4. 음악 평가
-- 한 회원은 같은 음악에 하나의 평가만 작성할 수 있다.
CREATE TABLE music_reviews (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    music_id BIGINT UNSIGNED NOT NULL,
    preference_score TINYINT UNSIGNED NOT NULL,
    running_fit_score TINYINT UNSIGNED NOT NULL,
    review_content TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_music_reviews PRIMARY KEY (id),
    CONSTRAINT uk_music_reviews_member_music UNIQUE (member_id, music_id),
    CONSTRAINT fk_music_reviews_member
        FOREIGN KEY (member_id) REFERENCES members (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_music_reviews_music
        FOREIGN KEY (music_id) REFERENCES music (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT ck_music_reviews_preference_score
        CHECK (preference_score BETWEEN 1 AND 5),
    CONSTRAINT ck_music_reviews_running_fit_score
        CHECK (running_fit_score BETWEEN 1 AND 5),
    INDEX idx_music_reviews_music_score (music_id, running_fit_score),
    INDEX idx_music_reviews_member (member_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '회원별 음악 평가';

-- 5. 플레이리스트
-- USER 플레이리스트는 owner_id가 필수이고 MANUAL 또는 CURATION_COPY로 생성한다.
-- OFFICIAL 플레이리스트는 시스템이 생성하며 owner_id 없이 PUBLIC + OFFICIAL로 관리한다.
CREATE TABLE playlists (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    owner_id BIGINT UNSIGNED NULL,
    playlist_type VARCHAR(20) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    spotify_playlist_id VARCHAR(100) NULL,
    spotify_playlist_url VARCHAR(500) NULL,
    spotify_export_status VARCHAR(20) NOT NULL DEFAULT 'NOT_EXPORTED',
    spotify_export_error_code VARCHAR(50) NULL,
    spotify_exported_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_playlists PRIMARY KEY (id),
    CONSTRAINT uk_playlists_spotify_playlist_id UNIQUE (spotify_playlist_id),
    CONSTRAINT fk_playlists_owner
        FOREIGN KEY (owner_id) REFERENCES members (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT ck_playlists_type
        CHECK (playlist_type IN ('USER', 'OFFICIAL')),
    CONSTRAINT ck_playlists_source_type
        CHECK (source_type IN ('MANUAL', 'OFFICIAL', 'CURATION_COPY')),
    CONSTRAINT ck_playlists_visibility
        CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT ck_playlists_export_status
        CHECK (
            spotify_export_status IN (
                'NOT_EXPORTED',
                'PENDING',
                'SUCCESS',
                'PARTIAL',
                'FAILED'
            )
        ),
    CONSTRAINT ck_playlists_owner_policy
        CHECK (
            (
                playlist_type = 'USER'
                AND owner_id IS NOT NULL
                AND source_type IN ('MANUAL', 'CURATION_COPY')
            )
            OR (
                playlist_type = 'OFFICIAL'
                AND owner_id IS NULL
                AND source_type = 'OFFICIAL'
                AND visibility = 'PUBLIC'
            )
        ),
    CONSTRAINT ck_playlists_export_result
        CHECK (
            (
                spotify_export_status IN ('SUCCESS', 'PARTIAL')
                AND spotify_playlist_id IS NOT NULL
                AND spotify_playlist_url IS NOT NULL
                AND spotify_exported_at IS NOT NULL
            )
            OR (
                spotify_export_status NOT IN ('SUCCESS', 'PARTIAL')
                AND spotify_exported_at IS NULL
            )
        ),
    CONSTRAINT ck_playlists_export_error
        CHECK (
            (
                spotify_export_status IN ('FAILED', 'PARTIAL')
                AND spotify_export_error_code IS NOT NULL
            )
            OR (
                spotify_export_status NOT IN ('FAILED', 'PARTIAL')
                AND spotify_export_error_code IS NULL
            )
        ),
    INDEX idx_playlists_owner (owner_id),
    INDEX idx_playlists_type_visibility (playlist_type, visibility),
    INDEX idx_playlists_deleted_at (deleted_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '공식 추천, 사용자 생성, 큐레이션 복사 플레이리스트';

-- 6. 플레이리스트 수록곡
-- 같은 플레이리스트에 동일한 음악 또는 동일한 순서를 중복 저장할 수 없다.
CREATE TABLE playlist_items (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    playlist_id BIGINT UNSIGNED NOT NULL,
    music_id BIGINT UNSIGNED NOT NULL,
    order_no INT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_playlist_items PRIMARY KEY (id),
    CONSTRAINT uk_playlist_items_playlist_music UNIQUE (playlist_id, music_id),
    CONSTRAINT uk_playlist_items_playlist_order UNIQUE (playlist_id, order_no),
    CONSTRAINT fk_playlist_items_playlist
        FOREIGN KEY (playlist_id) REFERENCES playlists (id)
        ON UPDATE RESTRICT
        ON DELETE CASCADE,
    CONSTRAINT fk_playlist_items_music
        FOREIGN KEY (music_id) REFERENCES music (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT ck_playlist_items_order_no
        CHECK (order_no >= 1),
    INDEX idx_playlist_items_music (music_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '플레이리스트 수록곡과 재생 순서';

-- 7. 플레이리스트 참조 저장
-- 공개 또는 공식 플레이리스트만 저장할 수 있다는 정책은 PlaylistSaveService에서 검증한다.
-- 자동 큐레이션 결과 저장에는 이 테이블을 사용하지 않고 USER + CURATION_COPY를 생성한다.
CREATE TABLE playlist_saves (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    playlist_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_playlist_saves PRIMARY KEY (id),
    CONSTRAINT uk_playlist_saves_member_playlist UNIQUE (member_id, playlist_id),
    CONSTRAINT fk_playlist_saves_member
        FOREIGN KEY (member_id) REFERENCES members (id)
        ON UPDATE RESTRICT
        ON DELETE CASCADE,
    CONSTRAINT fk_playlist_saves_playlist
        FOREIGN KEY (playlist_id) REFERENCES playlists (id)
        ON UPDATE RESTRICT
        ON DELETE CASCADE,
    INDEX idx_playlist_saves_playlist (playlist_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '공개 또는 공식 플레이리스트 참조 저장 관계';

-- 8. 자동 큐레이션 추천 로그
-- 추천 조회 시 요청 조건과 결과를 기록할 수 있지만 playlists와 playlist_items는 생성하지 않는다.
-- 사용자가 저장을 요청한 경우에만 별도 트랜잭션에서 USER + CURATION_COPY를 만들고
-- saved_playlist_id로 생성 결과를 연결한다.
CREATE TABLE recommendation_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    requested_intensity VARCHAR(20) NOT NULL,
    requested_bpm_min SMALLINT UNSIGNED NOT NULL,
    requested_bpm_max SMALLINT UNSIGNED NOT NULL,
    requested_genre VARCHAR(100) NULL,
    request_conditions JSON NULL,
    result_summary JSON NOT NULL,
    recommendation_reason TEXT NULL,
    saved_playlist_id BIGINT UNSIGNED NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_recommendation_logs PRIMARY KEY (id),
    CONSTRAINT fk_recommendation_logs_member
        FOREIGN KEY (member_id) REFERENCES members (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_recommendation_logs_saved_playlist
        FOREIGN KEY (saved_playlist_id) REFERENCES playlists (id)
        ON UPDATE RESTRICT
        ON DELETE SET NULL,
    CONSTRAINT ck_recommendation_logs_intensity
        CHECK (requested_intensity IN ('LOW', 'MODERATE', 'HIGH', 'INTERVAL')),
    CONSTRAINT ck_recommendation_logs_bpm_min
        CHECK (requested_bpm_min BETWEEN 60 AND 220),
    CONSTRAINT ck_recommendation_logs_bpm_max
        CHECK (requested_bpm_max BETWEEN 60 AND 220),
    CONSTRAINT ck_recommendation_logs_bpm_range
        CHECK (requested_bpm_min <= requested_bpm_max),
    INDEX idx_recommendation_logs_member_created (member_id, created_at),
    INDEX idx_recommendation_logs_conditions (
        requested_intensity,
        requested_bpm_min,
        requested_bpm_max,
        requested_genre
    ),
    INDEX idx_recommendation_logs_saved_playlist (saved_playlist_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '자동 큐레이션 요청 조건, 결과 요약, 저장 연결 로그';

-- 9. Spotify 계정 연결 정보
-- Access Token과 Refresh Token은 평문 저장보다 애플리케이션 계층 암호화 저장을 권장한다.
-- 실제 OAuth 요청과 토큰 갱신 처리는 feature/a-spotify-service 브랜치에서 구현한다.
CREATE TABLE spotify_connections (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    spotify_user_id VARCHAR(100) NOT NULL,
    access_token VARCHAR(2048) NOT NULL,
    refresh_token VARCHAR(2048) NOT NULL,
    token_expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_spotify_connections PRIMARY KEY (id),
    CONSTRAINT uk_spotify_connections_member_id UNIQUE (member_id),
    CONSTRAINT uk_spotify_connections_spotify_user_id UNIQUE (spotify_user_id),
    CONSTRAINT fk_spotify_connections_member
        FOREIGN KEY (member_id) REFERENCES members (id)
        ON UPDATE RESTRICT
        ON DELETE CASCADE,
    INDEX idx_spotify_connections_token_expires_at (token_expires_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '회원별 Spotify OAuth 연결 정보';

-- 10. 러닝 기록
-- 회원이 작성한 러닝 기록은 기존 엔티티 soft delete 정책에 맞춰 deleted_at으로 삭제 처리한다.
CREATE TABLE running_records (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    record_date DATE NOT NULL,
    distance_km DECIMAL(10, 2) NOT NULL,
    duration_seconds INT UNSIGNED NOT NULL,
    pace_seconds_per_km INT UNSIGNED NOT NULL,
    average_cadence SMALLINT UNSIGNED NULL,
    average_heart_rate_bpm SMALLINT UNSIGNED NULL,
    memo TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_running_records PRIMARY KEY (id),
    CONSTRAINT fk_running_records_member
        FOREIGN KEY (member_id) REFERENCES members (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT ck_running_records_distance
        CHECK (distance_km > 0),
    CONSTRAINT ck_running_records_duration
        CHECK (duration_seconds > 0),
    CONSTRAINT ck_running_records_pace
        CHECK (pace_seconds_per_km > 0),
    CONSTRAINT ck_running_records_average_cadence
        CHECK (
            average_cadence IS NULL
            OR average_cadence BETWEEN 60 AND 240
        ),
    CONSTRAINT ck_running_records_average_heart_rate
        CHECK (
            average_heart_rate_bpm IS NULL
            OR average_heart_rate_bpm BETWEEN 30 AND 240
        ),
    INDEX idx_running_records_member_date (member_id, record_date),
    INDEX idx_running_records_record_date (record_date),
    INDEX idx_running_records_deleted_at (deleted_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '회원별 러닝 기록';

-- 11. 러닝 모임
-- 정원 도달은 상태를 자동 변경하지 않으며 참여 Service에서 별도로 검증한다.
CREATE TABLE running_sessions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    host_member_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    meeting_place VARCHAR(255) NOT NULL,
    scheduled_at DATETIME(6) NOT NULL,
    capacity INT UNSIGNED NOT NULL,
    target_distance_km DECIMAL(10, 2) NULL,
    target_pace_seconds_per_km INT UNSIGNED NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    cancelled_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_running_sessions PRIMARY KEY (id),
    CONSTRAINT fk_running_sessions_host_member
        FOREIGN KEY (host_member_id) REFERENCES members (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT ck_running_sessions_title
        CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT ck_running_sessions_capacity
        CHECK (capacity > 0),
    CONSTRAINT ck_running_sessions_target_distance
        CHECK (target_distance_km IS NULL OR target_distance_km > 0),
    CONSTRAINT ck_running_sessions_target_pace
        CHECK (
            target_pace_seconds_per_km IS NULL
            OR target_pace_seconds_per_km > 0
        ),
    CONSTRAINT ck_running_sessions_status
        CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_running_sessions_cancelled_state
        CHECK (
            (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
            OR (status <> 'CANCELLED' AND cancelled_at IS NULL)
        ),
    INDEX idx_running_sessions_host_member (host_member_id),
    INDEX idx_running_sessions_status_scheduled (status, scheduled_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '회원이 생성하는 러닝 모임';

-- 12. 러닝 모임 참여
-- 참여 취소는 관계 행을 물리 삭제하며 같은 모임에 대한 중복 참여는 UNIQUE 제약으로 차단한다.
CREATE TABLE running_session_participants (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_running_session_participants PRIMARY KEY (id),
    CONSTRAINT uk_running_session_participants_session_member
        UNIQUE (session_id, member_id),
    CONSTRAINT fk_running_session_participants_session
        FOREIGN KEY (session_id) REFERENCES running_sessions (id)
        ON UPDATE RESTRICT
        ON DELETE CASCADE,
    CONSTRAINT fk_running_session_participants_member
        FOREIGN KEY (member_id) REFERENCES members (id)
        ON UPDATE RESTRICT
        ON DELETE CASCADE,
    INDEX idx_running_session_participants_session (session_id),
    INDEX idx_running_session_participants_member (member_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '러닝 모임 참여 회원 관계';
