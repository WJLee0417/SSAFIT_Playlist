-- Align an existing ssafit_playlist database with the current MyBatis model.
-- This migration preserves existing rows and may be executed more than once.
-- MySQL 8.x is required for enforced CHECK constraints.

DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_runtime_music_running_schema$$

CREATE PROCEDURE migrate_runtime_music_running_schema()
BEGIN
    DECLARE duplicate_spotify_uri_count BIGINT DEFAULT 0;
    DECLARE invalid_duration_count BIGINT DEFAULT 0;
    DECLARE invalid_energy_count BIGINT DEFAULT 0;
    DECLARE invalid_source_count BIGINT DEFAULT 0;
    DECLARE invalid_spotify_identity_count BIGINT DEFAULT 0;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'music'
          AND COLUMN_NAME = 'duration_ms'
    ) THEN
        ALTER TABLE music
            ADD COLUMN duration_ms INT UNSIGNED NULL AFTER bpm;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'music'
          AND COLUMN_NAME = 'music_energy_level'
    ) THEN
        ALTER TABLE music
            ADD COLUMN music_energy_level VARCHAR(20) NULL AFTER duration_ms;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'music'
          AND COLUMN_NAME = 'source_type'
    ) THEN
        ALTER TABLE music
            ADD COLUMN source_type VARCHAR(20) NULL AFTER music_energy_level;
        UPDATE music SET source_type = 'SEED' WHERE source_type IS NULL;
        ALTER TABLE music
            MODIFY COLUMN source_type VARCHAR(20) NOT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'music'
          AND COLUMN_NAME = 'spotify_track_id'
    ) THEN
        ALTER TABLE music
            ADD COLUMN spotify_track_id VARCHAR(100) NULL AFTER source_type;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'music'
          AND COLUMN_NAME = 'spotify_uri'
    ) THEN
        ALTER TABLE music
            ADD COLUMN spotify_uri VARCHAR(255) NULL AFTER spotify_track_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'music'
          AND COLUMN_NAME = 'spotify_external_url'
    ) THEN
        ALTER TABLE music
            ADD COLUMN spotify_external_url VARCHAR(500) NULL AFTER spotify_uri;
    END IF;

    SELECT COUNT(*)
    INTO duplicate_spotify_uri_count
    FROM (
        SELECT spotify_uri
        FROM music
        WHERE spotify_uri IS NOT NULL
        GROUP BY spotify_uri
        HAVING COUNT(*) > 1
    ) duplicate_uris;

    IF duplicate_spotify_uri_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Duplicate non-null music.spotify_uri values must be resolved before migration.';
    END IF;

    SELECT COUNT(*) INTO invalid_duration_count
    FROM music
    WHERE duration_ms IS NOT NULL
      AND duration_ms <= 0;

    IF invalid_duration_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Invalid music.duration_ms values must be resolved before migration.';
    END IF;

    SELECT COUNT(*) INTO invalid_energy_count
    FROM music
    WHERE music_energy_level IS NOT NULL
      AND music_energy_level NOT IN ('CALM', 'STEADY', 'ENERGETIC', 'PEAK');

    IF invalid_energy_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Invalid music.music_energy_level values must be resolved before migration.';
    END IF;

    SELECT COUNT(*) INTO invalid_source_count
    FROM music
    WHERE source_type NOT IN ('ADMIN', 'SEED', 'EXTERNAL', 'SPOTIFY');

    IF invalid_source_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Invalid music.source_type values must be resolved before migration.';
    END IF;

    SELECT COUNT(*) INTO invalid_spotify_identity_count
    FROM music
    WHERE source_type = 'SPOTIFY'
      AND (spotify_track_id IS NULL OR spotify_uri IS NULL);

    IF invalid_spotify_identity_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Spotify music rows require both spotify_track_id and spotify_uri.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'music'
          AND CONSTRAINT_NAME = 'uk_music_spotify_track_id'
    ) THEN
        ALTER TABLE music
            ADD CONSTRAINT uk_music_spotify_track_id
                UNIQUE (spotify_track_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'music'
          AND CONSTRAINT_NAME = 'uk_music_spotify_uri'
    ) THEN
        ALTER TABLE music
            ADD CONSTRAINT uk_music_spotify_uri UNIQUE (spotify_uri);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'music'
          AND CONSTRAINT_NAME = 'ck_music_duration'
    ) THEN
        ALTER TABLE music
            ADD CONSTRAINT ck_music_duration
                CHECK (duration_ms IS NULL OR duration_ms > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'music'
          AND CONSTRAINT_NAME = 'ck_music_energy_level'
    ) THEN
        ALTER TABLE music
            ADD CONSTRAINT ck_music_energy_level
                CHECK (
                    music_energy_level IS NULL
                    OR music_energy_level IN (
                        'CALM', 'STEADY', 'ENERGETIC', 'PEAK'
                    )
                );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'music'
          AND CONSTRAINT_NAME = 'ck_music_source_type'
    ) THEN
        ALTER TABLE music
            ADD CONSTRAINT ck_music_source_type
                CHECK (
                    source_type IN ('ADMIN', 'SEED', 'EXTERNAL', 'SPOTIFY')
                );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'music'
          AND CONSTRAINT_NAME = 'ck_music_spotify_identity'
    ) THEN
        ALTER TABLE music
            ADD CONSTRAINT ck_music_spotify_identity
                CHECK (
                    source_type <> 'SPOTIFY'
                    OR (
                        spotify_track_id IS NOT NULL
                        AND spotify_uri IS NOT NULL
                    )
                );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'running_profiles'
          AND COLUMN_NAME = 'target_cadence_min'
    ) THEN
        ALTER TABLE running_profiles
            ADD COLUMN target_cadence_min SMALLINT UNSIGNED NULL
                AFTER target_bpm_max;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'running_profiles'
          AND COLUMN_NAME = 'target_cadence_max'
    ) THEN
        ALTER TABLE running_profiles
            ADD COLUMN target_cadence_max SMALLINT UNSIGNED NULL
                AFTER target_cadence_min;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'running_profiles'
          AND CONSTRAINT_NAME = 'ck_running_profiles_cadence_min'
    ) THEN
        ALTER TABLE running_profiles
            ADD CONSTRAINT ck_running_profiles_cadence_min
                CHECK (
                    target_cadence_min IS NULL
                    OR target_cadence_min BETWEEN 60 AND 240
                );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'running_profiles'
          AND CONSTRAINT_NAME = 'ck_running_profiles_cadence_max'
    ) THEN
        ALTER TABLE running_profiles
            ADD CONSTRAINT ck_running_profiles_cadence_max
                CHECK (
                    target_cadence_max IS NULL
                    OR target_cadence_max BETWEEN 60 AND 240
                );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'running_profiles'
          AND CONSTRAINT_NAME = 'ck_running_profiles_cadence_range'
    ) THEN
        ALTER TABLE running_profiles
            ADD CONSTRAINT ck_running_profiles_cadence_range
                CHECK (
                    (
                        target_cadence_min IS NULL
                        AND target_cadence_max IS NULL
                    )
                    OR (
                        target_cadence_min IS NOT NULL
                        AND target_cadence_max IS NOT NULL
                        AND target_cadence_min <= target_cadence_max
                    )
                );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'running_records'
          AND COLUMN_NAME = 'average_cadence'
    ) THEN
        ALTER TABLE running_records
            ADD COLUMN average_cadence SMALLINT UNSIGNED NULL
                AFTER pace_seconds_per_km;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'running_records'
          AND COLUMN_NAME = 'average_heart_rate_bpm'
    ) THEN
        ALTER TABLE running_records
            ADD COLUMN average_heart_rate_bpm SMALLINT UNSIGNED NULL
                AFTER average_cadence;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'running_records'
          AND CONSTRAINT_NAME = 'ck_running_records_average_cadence'
    ) THEN
        ALTER TABLE running_records
            ADD CONSTRAINT ck_running_records_average_cadence
                CHECK (
                    average_cadence IS NULL
                    OR average_cadence BETWEEN 60 AND 240
                );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'running_records'
          AND CONSTRAINT_NAME = 'ck_running_records_average_heart_rate'
    ) THEN
        ALTER TABLE running_records
            ADD CONSTRAINT ck_running_records_average_heart_rate
                CHECK (
                    average_heart_rate_bpm IS NULL
                    OR average_heart_rate_bpm BETWEEN 30 AND 240
                );
    END IF;
END$$

CALL migrate_runtime_music_running_schema()$$
DROP PROCEDURE migrate_runtime_music_running_schema$$

DELIMITER ;

SHOW COLUMNS FROM music;
SHOW COLUMNS FROM running_profiles;
SHOW COLUMNS FROM running_records;
