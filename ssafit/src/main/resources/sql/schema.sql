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
        CHECK (target_bpm_min <= target_bpm_max)
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
    genre VARCHAR(100) NULL,
    bpm SMALLINT UNSIGNED NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    spotify_track_id VARCHAR(100) NULL,
    spotify_uri VARCHAR(255) NULL,
    spotify_external_url VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_music PRIMARY KEY (id),
    CONSTRAINT uk_music_spotify_track_id UNIQUE (spotify_track_id),
    CONSTRAINT ck_music_bpm
        CHECK (bpm BETWEEN 60 AND 220),
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
                spotify_export_status = 'SUCCESS'
                AND spotify_playlist_id IS NOT NULL
                AND spotify_playlist_url IS NOT NULL
                AND spotify_exported_at IS NOT NULL
            )
            OR (
                spotify_export_status <> 'SUCCESS'
                AND spotify_exported_at IS NULL
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
