-- Existing database migration for cadence and average heart-rate fields.
-- All new columns are nullable so existing rows remain unchanged.

ALTER TABLE running_profiles
    ADD COLUMN target_cadence_min SMALLINT UNSIGNED NULL
        AFTER target_bpm_max,
    ADD COLUMN target_cadence_max SMALLINT UNSIGNED NULL
        AFTER target_cadence_min,
    ADD CONSTRAINT ck_running_profiles_cadence_min
        CHECK (
            target_cadence_min IS NULL
            OR target_cadence_min BETWEEN 60 AND 240
        ),
    ADD CONSTRAINT ck_running_profiles_cadence_max
        CHECK (
            target_cadence_max IS NULL
            OR target_cadence_max BETWEEN 60 AND 240
        ),
    ADD CONSTRAINT ck_running_profiles_cadence_range
        CHECK (
            (target_cadence_min IS NULL AND target_cadence_max IS NULL)
            OR (
                target_cadence_min IS NOT NULL
                AND target_cadence_max IS NOT NULL
                AND target_cadence_min <= target_cadence_max
            )
        );

ALTER TABLE running_records
    ADD COLUMN average_cadence SMALLINT UNSIGNED NULL
        AFTER pace_seconds_per_km,
    ADD COLUMN average_heart_rate_bpm SMALLINT UNSIGNED NULL
        AFTER average_cadence,
    ADD CONSTRAINT ck_running_records_average_cadence
        CHECK (
            average_cadence IS NULL
            OR average_cadence BETWEEN 60 AND 240
        ),
    ADD CONSTRAINT ck_running_records_average_heart_rate
        CHECK (
            average_heart_rate_bpm IS NULL
            OR average_heart_rate_bpm BETWEEN 30 AND 240
        );
