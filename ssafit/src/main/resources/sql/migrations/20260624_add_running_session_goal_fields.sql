ALTER TABLE running_sessions
    ADD COLUMN target_distance_km DECIMAL(10, 2) NULL AFTER capacity,
    ADD COLUMN target_pace_seconds_per_km INT UNSIGNED NULL AFTER target_distance_km,
    ADD CONSTRAINT ck_running_sessions_target_distance
        CHECK (target_distance_km IS NULL OR target_distance_km > 0),
    ADD CONSTRAINT ck_running_sessions_target_pace
        CHECK (
            target_pace_seconds_per_km IS NULL
            OR target_pace_seconds_per_km > 0
        );
