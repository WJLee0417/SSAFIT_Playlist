-- SSAFIT PlayList 로컬 개발용 더미 데이터
-- schema.sql과 data.sql 실행 후 사용한다.
-- 비밀번호 해시는 화면/DB 확인 전용 값이며 실제 로그인 비밀번호로 사용하지 않는다.

SET NAMES utf8mb4;

INSERT IGNORE INTO members (
    login_id,
    password_hash,
    email,
    nickname,
    status
) VALUES
    ('runner_low', 'dummy-password-hash', 'runner.low@example.test', '저강도 러너', 'ACTIVE'),
    ('runner_high', 'dummy-password-hash', 'runner.high@example.test', '고강도 러너', 'ACTIVE'),
    ('runner_interval', 'dummy-password-hash', 'runner.interval@example.test', '인터벌 러너', 'ACTIVE'),
    ('admin_candidate', 'dummy-password-hash', 'admin@example.test', '관리자 후보', 'ACTIVE');

INSERT IGNORE INTO running_profiles (
    member_id,
    running_intensity,
    target_bpm_min,
    target_bpm_max,
    preferred_genre
) VALUES
    (
        (SELECT id FROM members WHERE login_id = 'runner_low'),
        'LOW',
        120,
        140,
        'POP'
    ),
    (
        (SELECT id FROM members WHERE login_id = 'runner_high'),
        'HIGH',
        170,
        200,
        'ROCK'
    ),
    (
        (SELECT id FROM members WHERE login_id = 'admin_candidate'),
        'MODERATE',
        140,
        170,
        'ELECTRONIC'
    ),
    (
        (SELECT id FROM members WHERE login_id = 'runner_interval'),
        'INTERVAL',
        140,
        200,
        'HIPHOP'
    );

INSERT INTO music (title, artist, album_name, genre, bpm, source_type)
SELECT 'Morning Steps', 'Dummy Runner', 'Local Run', 'POP', 120, 'ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM music WHERE title = 'Morning Steps' AND artist = 'Dummy Runner'
);

INSERT INTO music (title, artist, album_name, genre, bpm, source_type)
SELECT 'Easy Breeze', 'Dummy Runner', 'Local Run', 'JAZZ', 130, 'SEED'
WHERE NOT EXISTS (
    SELECT 1 FROM music WHERE title = 'Easy Breeze' AND artist = 'Dummy Runner'
);

INSERT INTO music (title, artist, album_name, genre, bpm, source_type)
SELECT 'City Tempo', 'Dummy Runner', 'Local Run', 'HIPHOP', 140, 'EXTERNAL'
WHERE NOT EXISTS (
    SELECT 1 FROM music WHERE title = 'City Tempo' AND artist = 'Dummy Runner'
);

INSERT INTO music (title, artist, album_name, genre, bpm, source_type)
SELECT 'Neon Pace', 'Dummy Runner', 'Local Run', 'ELECTRONIC', 150, 'ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM music WHERE title = 'Neon Pace' AND artist = 'Dummy Runner'
);

INSERT INTO music (title, artist, album_name, genre, bpm, source_type)
SELECT 'Rock Cadence', 'Dummy Runner', 'Local Run', 'ROCK', 160, 'SEED'
WHERE NOT EXISTS (
    SELECT 1 FROM music WHERE title = 'Rock Cadence' AND artist = 'Dummy Runner'
);

INSERT INTO music (title, artist, album_name, genre, bpm, source_type)
SELECT 'Threshold', 'Dummy Runner', 'Local Run', 'ROCK', 170, 'EXTERNAL'
WHERE NOT EXISTS (
    SELECT 1 FROM music WHERE title = 'Threshold' AND artist = 'Dummy Runner'
);

INSERT INTO music (title, artist, album_name, genre, bpm, source_type)
SELECT 'Final Lap', 'Dummy Runner', 'Local Run', 'ELECTRONIC', 180, 'ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM music WHERE title = 'Final Lap' AND artist = 'Dummy Runner'
);

INSERT INTO music (title, artist, album_name, genre, bpm, source_type)
SELECT 'Sprint Signal', 'Dummy Runner', 'Local Run', 'ROCK', 190, 'SEED'
WHERE NOT EXISTS (
    SELECT 1 FROM music WHERE title = 'Sprint Signal' AND artist = 'Dummy Runner'
);

INSERT INTO music (
    title,
    artist,
    album_name,
    genre,
    bpm,
    source_type,
    spotify_track_id,
    spotify_uri,
    spotify_external_url
)
SELECT
    'Spotify Test Run',
    'Dummy Spotify Artist',
    'Spotify Test Album',
    'POP',
    200,
    'SPOTIFY',
    'dummy_spotify_track_200',
    'spotify:track:dummy_spotify_track_200',
    'https://open.spotify.com/track/dummy_spotify_track_200'
WHERE NOT EXISTS (
    SELECT 1
    FROM music
    WHERE spotify_track_id = 'dummy_spotify_track_200'
);

INSERT IGNORE INTO music_reviews (
    member_id,
    music_id,
    preference_score,
    running_fit_score,
    review_content
)
SELECT
    member_data.member_id,
    music_data.music_id,
    member_data.preference_score,
    member_data.running_fit_score,
    member_data.review_content
FROM (
    SELECT
        (SELECT id FROM members WHERE login_id = 'runner_low') AS member_id,
        5 AS preference_score,
        4 AS running_fit_score,
        '편안한 러닝에 잘 맞습니다.' AS review_content
    UNION ALL
    SELECT
        (SELECT id FROM members WHERE login_id = 'runner_high'),
        3,
        5,
        '속도를 올릴 때 좋습니다.'
) member_data
CROSS JOIN (
    SELECT id AS music_id
    FROM music
    WHERE title = 'Neon Pace'
      AND artist = 'Dummy Runner'
) music_data;

INSERT IGNORE INTO music_reviews (
    member_id,
    music_id,
    preference_score,
    running_fit_score,
    review_content
)
SELECT
    (SELECT id FROM members WHERE login_id = 'runner_high'),
    id,
    5,
    5,
    '마지막 스퍼트용 곡입니다.'
FROM music
WHERE title = 'Final Lap'
  AND artist = 'Dummy Runner';

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
    '더미 공식 템포런',
    '개발 확인용 공식 플레이리스트',
    'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1 FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title = '더미 공식 템포런'
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
    (SELECT id FROM members WHERE login_id = 'runner_low'),
    'USER',
    'MANUAL',
    'PUBLIC',
    '저강도 아침 러닝',
    'runner_low 사용자의 공개 플레이리스트',
    'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1 FROM playlists
    WHERE owner_id = (SELECT id FROM members WHERE login_id = 'runner_low')
      AND title = '저강도 아침 러닝'
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
    (SELECT id FROM members WHERE login_id = 'runner_high'),
    'USER',
    'MANUAL',
    'PRIVATE',
    '고강도 인터벌',
    'runner_high 사용자의 비공개 플레이리스트',
    'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1 FROM playlists
    WHERE owner_id = (SELECT id FROM members WHERE login_id = 'runner_high')
      AND title = '고강도 인터벌'
);

INSERT IGNORE INTO playlist_items (playlist_id, music_id, order_no)
SELECT
    p.id,
    m.id,
    item_data.order_no
FROM playlists p
INNER JOIN (
    SELECT 'Morning Steps' AS title, 1 AS order_no
    UNION ALL SELECT 'Easy Breeze', 2
    UNION ALL SELECT 'Neon Pace', 3
) item_data
INNER JOIN music m
    ON m.title = item_data.title
   AND m.artist = 'Dummy Runner'
WHERE p.title = '저강도 아침 러닝';

INSERT IGNORE INTO playlist_items (playlist_id, music_id, order_no)
SELECT
    p.id,
    m.id,
    item_data.order_no
FROM playlists p
INNER JOIN (
    SELECT 'Rock Cadence' AS title, 1 AS order_no
    UNION ALL SELECT 'Threshold', 2
    UNION ALL SELECT 'Final Lap', 3
    UNION ALL SELECT 'Sprint Signal', 4
    UNION ALL SELECT 'Spotify Test Run', 5
) item_data
INNER JOIN music m
    ON m.title = item_data.title
WHERE p.title IN ('더미 공식 템포런', '고강도 인터벌');

INSERT IGNORE INTO playlist_saves (member_id, playlist_id)
SELECT
    (SELECT id FROM members WHERE login_id = 'runner_high'),
    id
FROM playlists
WHERE title = '저강도 아침 러닝'
  AND visibility = 'PUBLIC';
