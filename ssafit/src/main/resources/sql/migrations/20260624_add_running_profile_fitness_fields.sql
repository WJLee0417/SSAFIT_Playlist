ALTER TABLE running_profiles
    ADD COLUMN gender VARCHAR(20) NULL AFTER member_id,
    ADD COLUMN age TINYINT UNSIGNED NULL AFTER gender,
    ADD COLUMN estimated_max_heart_rate_bpm SMALLINT UNSIGNED NULL AFTER age,
    ADD CONSTRAINT ck_running_profiles_gender
        CHECK (
            gender IS NULL
            OR gender IN ('FEMALE', 'MALE', 'OTHER', 'UNSPECIFIED')
        ),
    ADD CONSTRAINT ck_running_profiles_age
        CHECK (age IS NULL OR age BETWEEN 10 AND 100),
    ADD CONSTRAINT ck_running_profiles_estimated_max_hr
        CHECK (
            estimated_max_heart_rate_bpm IS NULL
            OR estimated_max_heart_rate_bpm BETWEEN 120 AND 210
        );
