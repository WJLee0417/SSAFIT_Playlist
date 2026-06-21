-- SSAFIT PlayList 기준 데이터
-- schema.sql 실행 후 MySQL Workbench 또는 CLI에서 직접 실행한다.
-- 애플리케이션 자동 초기화 대상이 아니며, 반복 실행해도 중복되지 않도록 작성한다.

SET NAMES utf8mb4;

INSERT INTO music (
    title,
    artist,
    album_name,
    genre,
    bpm,
    source_type
)
SELECT
    'SSAFIT Warm Up',
    'SSAFIT',
    'Official Running Seeds',
    'POP',
    125,
    'SEED'
WHERE NOT EXISTS (
    SELECT 1
    FROM music
    WHERE title = 'SSAFIT Warm Up'
      AND artist = 'SSAFIT'
);

INSERT INTO music (
    title,
    artist,
    album_name,
    genre,
    bpm,
    source_type
)
SELECT
    'SSAFIT Steady Run',
    'SSAFIT',
    'Official Running Seeds',
    'ELECTRONIC',
    150,
    'SEED'
WHERE NOT EXISTS (
    SELECT 1
    FROM music
    WHERE title = 'SSAFIT Steady Run'
      AND artist = 'SSAFIT'
);

INSERT INTO music (
    title,
    artist,
    album_name,
    genre,
    bpm,
    source_type
)
SELECT
    'SSAFIT Final Sprint',
    'SSAFIT',
    'Official Running Seeds',
    'ROCK',
    180,
    'SEED'
WHERE NOT EXISTS (
    SELECT 1
    FROM music
    WHERE title = 'SSAFIT Final Sprint'
      AND artist = 'SSAFIT'
);

INSERT INTO playlists (
    owner_id,
    playlist_type,
    source_type,
    visibility,
    title,
    description,
    spotify_export_status
)
SELECT
    NULL,
    'OFFICIAL',
    'OFFICIAL',
    'PUBLIC',
    'SSAFIT 공식 러닝 스타터',
    'MVP 실행에 사용하는 최소 공식 플레이리스트',
    'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1
    FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title = 'SSAFIT 공식 러닝 스타터'
);

INSERT IGNORE INTO playlist_items (playlist_id, music_id, order_no)
SELECT
    p.id,
    m.id,
    seed.order_no
FROM playlists p
INNER JOIN (
    SELECT 'SSAFIT Warm Up' AS title, 1 AS order_no
    UNION ALL
    SELECT 'SSAFIT Steady Run', 2
    UNION ALL
    SELECT 'SSAFIT Final Sprint', 3
) seed
INNER JOIN music m
    ON m.title = seed.title
   AND m.artist = 'SSAFIT'
WHERE p.playlist_type = 'OFFICIAL'
  AND p.title = 'SSAFIT 공식 러닝 스타터';
