-- 적용 전 spotify_uri 중복 데이터를 반드시 확인하고 정리한다.
-- bpm은 음악 템포이며 average_cadence / average_heart_rate_bpm과 관련 없는 값이다.

ALTER TABLE music
    MODIFY COLUMN bpm SMALLINT UNSIGNED NULL
        COMMENT '음악 템포 BPM. 케이던스 SPM 및 평균 심박수 BPM과 별도 값',
    ADD COLUMN duration_ms INT UNSIGNED NULL AFTER bpm,
    ADD CONSTRAINT uk_music_spotify_uri UNIQUE (spotify_uri),
    ADD CONSTRAINT ck_music_duration
        CHECK (duration_ms IS NULL OR duration_ms > 0);
