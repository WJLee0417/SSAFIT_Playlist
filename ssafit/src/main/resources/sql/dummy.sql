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
    target_bpm_min,
    target_bpm_max,
    target_cadence_min,
    target_cadence_max,
    preferred_genre
) VALUES
    (
        (SELECT id FROM members WHERE login_id = 'runner_low'),
        120,
        140,
        165,
        180,
        'POP'
    ),
    (
        (SELECT id FROM members WHERE login_id = 'runner_high'),
        170,
        200,
        NULL,
        NULL,
        'ROCK'
    ),
    (
        (SELECT id FROM members WHERE login_id = 'admin_candidate'),
        140,
        170,
        NULL,
        NULL,
        'ELECTRONIC'
    ),
    (
        (SELECT id FROM members WHERE login_id = 'runner_interval'),
        140,
        200,
        NULL,
        NULL,
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

-- 러닝 기록
-- duration_seconds / distance_km로 계산한 pace_seconds_per_km를 저장한다.
INSERT INTO running_records (
    member_id,
    record_date,
    distance_km,
    duration_seconds,
    pace_seconds_per_km,
    average_cadence,
    average_heart_rate_bpm,
    memo
)
SELECT
    (SELECT id FROM members WHERE login_id = 'runner_low'),
    '2026-06-10',
    5.00,
    1800,
    360,
    172,
    154,
    '5km 아침 러닝'
WHERE NOT EXISTS (
    SELECT 1
    FROM running_records
    WHERE member_id = (SELECT id FROM members WHERE login_id = 'runner_low')
      AND record_date = '2026-06-10'
      AND distance_km = 5.00
      AND duration_seconds = 1800
);

INSERT INTO running_records (
    member_id,
    record_date,
    distance_km,
    duration_seconds,
    pace_seconds_per_km,
    memo
)
SELECT
    (SELECT id FROM members WHERE login_id = 'runner_low'),
    '2026-06-15',
    10.00,
    3300,
    330,
    '10km 지속주'
WHERE NOT EXISTS (
    SELECT 1
    FROM running_records
    WHERE member_id = (SELECT id FROM members WHERE login_id = 'runner_low')
      AND record_date = '2026-06-15'
      AND distance_km = 10.00
      AND duration_seconds = 3300
);

INSERT INTO running_records (
    member_id,
    record_date,
    distance_km,
    duration_seconds,
    pace_seconds_per_km,
    memo
)
SELECT
    (SELECT id FROM members WHERE login_id = 'runner_high'),
    '2026-06-12',
    3.00,
    900,
    300,
    '고강도 3km 템포런'
WHERE NOT EXISTS (
    SELECT 1
    FROM running_records
    WHERE member_id = (SELECT id FROM members WHERE login_id = 'runner_high')
      AND record_date = '2026-06-12'
      AND distance_km = 3.00
      AND duration_seconds = 900
);

INSERT INTO running_records (
    member_id,
    record_date,
    distance_km,
    duration_seconds,
    pace_seconds_per_km,
    memo
)
SELECT
    (SELECT id FROM members WHERE login_id = 'runner_interval'),
    '2026-06-18',
    8.00,
    2400,
    300,
    '인터벌 포함 8km'
WHERE NOT EXISTS (
    SELECT 1
    FROM running_records
    WHERE member_id = (
        SELECT id FROM members WHERE login_id = 'runner_interval'
    )
      AND record_date = '2026-06-18'
      AND distance_km = 8.00
      AND duration_seconds = 2400
);

-- 러닝 모임
INSERT INTO running_sessions (
    host_member_id,
    title,
    description,
    meeting_place,
    scheduled_at,
    capacity,
    status
)
SELECT
    (SELECT id FROM members WHERE login_id = 'runner_low'),
    '한강 초보 러닝',
    '천천히 함께 달리는 OPEN 모임',
    '여의나루역 2번 출구',
    '2026-07-04 09:00:00',
    5,
    'OPEN'
WHERE NOT EXISTS (
    SELECT 1
    FROM running_sessions
    WHERE host_member_id = (
        SELECT id FROM members WHERE login_id = 'runner_low'
    )
      AND title = '한강 초보 러닝'
      AND scheduled_at = '2026-07-04 09:00:00'
);

INSERT INTO running_sessions (
    host_member_id,
    title,
    description,
    meeting_place,
    scheduled_at,
    capacity,
    status
)
SELECT
    (SELECT id FROM members WHERE login_id = 'runner_high'),
    '정원 1명 템포런',
    '정원 초과 검증용 OPEN 모임',
    '반포한강공원',
    '2026-07-05 07:00:00',
    1,
    'OPEN'
WHERE NOT EXISTS (
    SELECT 1
    FROM running_sessions
    WHERE host_member_id = (
        SELECT id FROM members WHERE login_id = 'runner_high'
    )
      AND title = '정원 1명 템포런'
      AND scheduled_at = '2026-07-05 07:00:00'
);

INSERT INTO running_sessions (
    host_member_id,
    title,
    description,
    meeting_place,
    scheduled_at,
    capacity,
    status
)
SELECT
    (SELECT id FROM members WHERE login_id = 'runner_interval'),
    '참여자 없는 인터벌',
    '빈 참여자 목록 확인용 OPEN 모임',
    '잠실종합운동장',
    '2026-07-06 19:30:00',
    4,
    'OPEN'
WHERE NOT EXISTS (
    SELECT 1
    FROM running_sessions
    WHERE host_member_id = (
        SELECT id FROM members WHERE login_id = 'runner_interval'
    )
      AND title = '참여자 없는 인터벌'
      AND scheduled_at = '2026-07-06 19:30:00'
);

INSERT INTO running_sessions (
    host_member_id,
    title,
    description,
    meeting_place,
    scheduled_at,
    capacity,
    status
)
SELECT
    (SELECT id FROM members WHERE login_id = 'runner_high'),
    '모집 마감 롱런',
    '생성자가 모집을 닫은 CLOSED 모임',
    '서울숲',
    '2026-07-12 08:00:00',
    8,
    'CLOSED'
WHERE NOT EXISTS (
    SELECT 1
    FROM running_sessions
    WHERE host_member_id = (
        SELECT id FROM members WHERE login_id = 'runner_high'
    )
      AND title = '모집 마감 롱런'
      AND scheduled_at = '2026-07-12 08:00:00'
);

INSERT INTO running_sessions (
    host_member_id,
    title,
    description,
    meeting_place,
    scheduled_at,
    capacity,
    status,
    cancelled_at
)
SELECT
    (SELECT id FROM members WHERE login_id = 'runner_low'),
    '취소된 우천 러닝',
    'CANCELLED 상태 확인용 모임',
    '뚝섬유원지역',
    '2026-07-13 09:00:00',
    6,
    'CANCELLED',
    '2026-06-20 12:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM running_sessions
    WHERE host_member_id = (
        SELECT id FROM members WHERE login_id = 'runner_low'
    )
      AND title = '취소된 우천 러닝'
      AND scheduled_at = '2026-07-13 09:00:00'
);

-- 러닝 모임 참여 관계
-- 정원 1명 모임은 runner_low 한 명으로 가득 찬 상태다.
INSERT IGNORE INTO running_session_participants (session_id, member_id)
SELECT
    (
        SELECT id
        FROM running_sessions
        WHERE title = '정원 1명 템포런'
          AND host_member_id = (
              SELECT id FROM members WHERE login_id = 'runner_high'
          )
        LIMIT 1
    ),
    (SELECT id FROM members WHERE login_id = 'runner_low');

-- 여러 명 정원 OPEN 모임에는 서로 다른 두 회원이 참여한다.
INSERT IGNORE INTO running_session_participants (session_id, member_id)
SELECT
    (
        SELECT id
        FROM running_sessions
        WHERE title = '한강 초보 러닝'
          AND host_member_id = (
              SELECT id FROM members WHERE login_id = 'runner_low'
          )
        LIMIT 1
    ),
    participant.member_id
FROM (
    SELECT id AS member_id
    FROM members
    WHERE login_id IN ('runner_high', 'runner_interval')
) participant;
