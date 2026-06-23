-- 음악 에너지는 음악 BPM, 러닝 케이던스, 평균 심박수와 독립적인 큐레이션 태그다.
-- 기존 데이터에는 임의 값을 채우지 않는다.

ALTER TABLE music
    ADD COLUMN music_energy_level VARCHAR(20) NULL AFTER duration_ms,
    ADD CONSTRAINT ck_music_energy_level
        CHECK (
            music_energy_level IS NULL
            OR music_energy_level IN ('CALM', 'STEADY', 'ENERGETIC', 'PEAK')
        );
