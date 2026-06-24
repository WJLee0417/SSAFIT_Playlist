ALTER TABLE running_profiles
    DROP CHECK ck_running_profiles_intensity,
    DROP CHECK ck_running_profiles_bpm_min,
    DROP CHECK ck_running_profiles_bpm_max,
    DROP CHECK ck_running_profiles_bpm_range,
    DROP COLUMN running_intensity,
    MODIFY COLUMN target_bpm_min SMALLINT UNSIGNED NULL,
    MODIFY COLUMN target_bpm_max SMALLINT UNSIGNED NULL,
    ADD CONSTRAINT ck_running_profiles_bpm_min
        CHECK (target_bpm_min IS NULL OR target_bpm_min BETWEEN 60 AND 220),
    ADD CONSTRAINT ck_running_profiles_bpm_max
        CHECK (target_bpm_max IS NULL OR target_bpm_max BETWEEN 60 AND 220),
    ADD CONSTRAINT ck_running_profiles_bpm_range
        CHECK (
            (target_bpm_min IS NULL AND target_bpm_max IS NULL)
            OR (
                target_bpm_min IS NOT NULL
                AND target_bpm_max IS NOT NULL
                AND target_bpm_min <= target_bpm_max
            )
        );
