-- SSAFIT PlayList demo snapshot
-- Run after schema.sql or mount as /docker-entrypoint-initdb.d/02-demo-snapshot.sql.
-- Includes official demo playlists, Zone 2 running, WORKOUT K-POP, demo members, running records, sessions, and playlist relationships.
-- Do not store real personal OAuth tokens or production dumps in this file.

SET NAMES utf8mb4;


-- ---- BEGIN official base demo data ----

-- SSAFIT PlayList 기준 데이터
-- schema.sql 실행 후 MySQL Workbench 또는 CLI에서 직접 실행한다.
-- 애플리케이션 자동 초기화 대상이 아니며, 반복 실행해도 중복되지 않도록 작성한다.
--
-- 기본 플레이리스트는 Spotify Search API로 실제 검색 확인한 트랙 ID/URI 기반 샘플이다.
-- Spotify Search API는 BPM을 제공하지 않으므로 bpm/music_energy_level은 러닝 분류용 메타데이터로 관리한다.


-- 이전 기능 확인용 SSAFIT 더미 데이터 제거
DELETE pi
FROM playlist_items pi
INNER JOIN playlists p ON p.id = pi.playlist_id
WHERE p.playlist_type = 'OFFICIAL'
  AND p.title IN (
      'SSAFIT 공식 러닝 스타터',
      'SSAFIT 저강도 대표 더미',
      'SSAFIT 중강도 대표 더미',
      'SSAFIT 고강도 대표 더미',
      'SSAFIT 인터벌 대표 더미'
  );

DELETE FROM playlists
WHERE playlist_type = 'OFFICIAL'
  AND title IN (
      'SSAFIT 공식 러닝 스타터',
      'SSAFIT 저강도 대표 더미',
      'SSAFIT 중강도 대표 더미',
      'SSAFIT 고강도 대표 더미',
      'SSAFIT 인터벌 대표 더미'
  );

DELETE m
FROM music m
LEFT JOIN playlist_items pi ON pi.music_id = m.id
LEFT JOIN music_reviews mr ON mr.music_id = m.id
WHERE m.artist = 'SSAFIT'
  AND m.title IN (
      'SSAFIT Warm Up',
      'SSAFIT Steady Run',
      'SSAFIT Final Sprint',
      'SSAFIT Recovery Steps',
      'SSAFIT Tempo Lift',
      'SSAFIT Interval Surge'
  )
  AND pi.id IS NULL
  AND mr.id IS NULL;

INSERT INTO music (
    title,
    artist,
    album_name,
    image_url,
    genre,
    bpm,
    duration_ms,
    music_energy_level,
    source_type,
    spotify_track_id,
    spotify_uri,
    spotify_external_url
)
SELECT * FROM (
    SELECT
        'BIRDS OF A FEATHER' AS title,
        'Billie Eilish' AS artist,
        'HIT ME HARD AND SOFT' AS album_name,
        'https://i.scdn.co/image/ab67616d0000b27371d62ea7ea8a5be92d3c1f62' AS image_url,
        'POP' AS genre,
        105 AS bpm,
        210373 AS duration_ms,
        'CALM' AS music_energy_level,
        'SPOTIFY' AS source_type,
        '6dOtVTDdiauQNBQEDOtlAB' AS spotify_track_id,
        'spotify:track:6dOtVTDdiauQNBQEDOtlAB' AS spotify_uri,
        'https://open.spotify.com/track/6dOtVTDdiauQNBQEDOtlAB' AS spotify_external_url
    UNION ALL
    SELECT
        'Beautiful Things',
        'Benson Boone',
        'Fireworks & Rollerblades',
        'https://i.scdn.co/image/ab67616d0000b273831949037a1db10b87b005fa',
        'POP ROCK',
        105,
        180304,
        'STEADY',
        'SPOTIFY',
        '3xkHsmpQCBMytMJNiDf3Ii',
        'spotify:track:3xkHsmpQCBMytMJNiDf3Ii',
        'https://open.spotify.com/track/3xkHsmpQCBMytMJNiDf3Ii'
    UNION ALL
    SELECT
        'Espresso',
        'Sabrina Carpenter',
        'Short n'' Sweet',
        'https://i.scdn.co/image/ab67616d0000b273fd8d7a8d96871e791cb1f626',
        'POP',
        104,
        175459,
        'STEADY',
        'SPOTIFY',
        '2HRqTpkrJO5ggZyyK6NPWz',
        'spotify:track:2HRqTpkrJO5ggZyyK6NPWz',
        'https://open.spotify.com/track/2HRqTpkrJO5ggZyyK6NPWz'
    UNION ALL
    SELECT
        'Water',
        'Tyla',
        'Water',
        'https://i.scdn.co/image/ab67616d0000b273d20231861e86a6f74ef2393e',
        'AFROPOP',
        117,
        200255,
        'STEADY',
        'SPOTIFY',
        '5aIVCx5tnk0ntmdiinnYvw',
        'spotify:track:5aIVCx5tnk0ntmdiinnYvw',
        'https://open.spotify.com/track/5aIVCx5tnk0ntmdiinnYvw'
    UNION ALL
    SELECT
        'Flowers',
        'Miley Cyrus',
        'Endless Summer Vacation',
        'https://i.scdn.co/image/ab67616d0000b27366fd0917f38d472c8576754e',
        'POP',
        118,
        200600,
        'STEADY',
        'SPOTIFY',
        '7DSAEUvxU8FajXtRloy8M0',
        'spotify:track:7DSAEUvxU8FajXtRloy8M0',
        'https://open.spotify.com/track/7DSAEUvxU8FajXtRloy8M0'
    UNION ALL
    SELECT
        'Die With A Smile',
        'Lady Gaga, Bruno Mars',
        'Die With A Smile',
        'https://i.scdn.co/image/ab67616d0000b27382ea2e9e1858aa012c57cd45',
        'POP',
        158,
        251667,
        'STEADY',
        'SPOTIFY',
        '2plbrEY59IikOBgBGLjaoe',
        'spotify:track:2plbrEY59IikOBgBGLjaoe',
        'https://open.spotify.com/track/2plbrEY59IikOBgBGLjaoe'
    UNION ALL
    SELECT
        'APT.',
        'ROSÉ, Bruno Mars',
        'APT.',
        'https://i.scdn.co/image/ab67616d0000b27346085aa1855bd888abc51a5a',
        'POP ROCK',
        149,
        169917,
        'ENERGETIC',
        'SPOTIFY',
        '5vNRhkKd0yEAg8suGBpjeY',
        'spotify:track:5vNRhkKd0yEAg8suGBpjeY',
        'https://open.spotify.com/track/5vNRhkKd0yEAg8suGBpjeY'
    UNION ALL
    SELECT
        'Ordinary',
        'Alex Warren',
        'You''ll Be Alright, Kid (Chapter 1)',
        'https://i.scdn.co/image/ab67616d0000b2738d18d73bbfc8985d52865edf',
        'POP',
        148,
        186964,
        'ENERGETIC',
        'SPOTIFY',
        '2RkZ5LkEzeHGRsmDqKwmaJ',
        'spotify:track:2RkZ5LkEzeHGRsmDqKwmaJ',
        'https://open.spotify.com/track/2RkZ5LkEzeHGRsmDqKwmaJ'
    UNION ALL
    SELECT
        'luther (with sza)',
        'Kendrick Lamar, SZA',
        'GNX',
        'https://i.scdn.co/image/ab67616d0000b2737e5e269471fe4a43a42e2f15',
        'HIPHOP',
        139,
        177598,
        'STEADY',
        'SPOTIFY',
        '2CGNAOSuO1MEFCbBRgUzjd',
        'spotify:track:2CGNAOSuO1MEFCbBRgUzjd',
        'https://open.spotify.com/track/2CGNAOSuO1MEFCbBRgUzjd'
    UNION ALL
    SELECT
        'Blinding Lights',
        'The Weeknd',
        'After Hours',
        'https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36',
        'SYNTH POP',
        171,
        200040,
        'PEAK',
        'SPOTIFY',
        '0VjIjW4GlUZAMYd2vXMi3b',
        'spotify:track:0VjIjW4GlUZAMYd2vXMi3b',
        'https://open.spotify.com/track/0VjIjW4GlUZAMYd2vXMi3b'
    UNION ALL
    SELECT
        'As It Was',
        'Harry Styles',
        'Harry''s House',
        'https://i.scdn.co/image/ab67616d0000b27382ce362511fb3d9dda6578ee',
        'POP',
        174,
        167303,
        'ENERGETIC',
        'SPOTIFY',
        '4Dvkj6JhhA12EX05fT7y2e',
        'spotify:track:4Dvkj6JhhA12EX05fT7y2e',
        'https://open.spotify.com/track/4Dvkj6JhhA12EX05fT7y2e'
    UNION ALL
    SELECT
        'good 4 u',
        'Olivia Rodrigo',
        'SOUR',
        'https://i.scdn.co/image/ab67616d0000b273a91c10fe9472d9bd89802e5a',
        'POP PUNK',
        167,
        178146,
        'PEAK',
        'SPOTIFY',
        '4ZtFanR9U6ndgddUvNcjcG',
        'spotify:track:4ZtFanR9U6ndgddUvNcjcG',
        'https://open.spotify.com/track/4ZtFanR9U6ndgddUvNcjcG'
    UNION ALL
    SELECT
        'Starboy',
        'The Weeknd, Daft Punk',
        'Starboy',
        'https://i.scdn.co/image/ab67616d0000b2734718e2b124f79258be7bc452',
        'R&B',
        186,
        230453,
        'PEAK',
        'SPOTIFY',
        '7MXVkk9YMctZqd1Srtv4MB',
        'spotify:track:7MXVkk9YMctZqd1Srtv4MB',
        'https://open.spotify.com/track/7MXVkk9YMctZqd1Srtv4MB'
    UNION ALL
    SELECT
        'Levitating (feat. DaBaby)',
        'Dua Lipa, DaBaby',
        'Future Nostalgia',
        'https://i.scdn.co/image/ab67616d0000b2734aa538a481e67131c214135a',
        'DANCE POP',
        103,
        203064,
        'ENERGETIC',
        'SPOTIFY',
        '5nujrmhLynf4yMoMtj8AQF',
        'spotify:track:5nujrmhLynf4yMoMtj8AQF',
        'https://open.spotify.com/track/5nujrmhLynf4yMoMtj8AQF'
    UNION ALL
    SELECT
        'Unholy (feat. Kim Petras)',
        'Sam Smith, Kim Petras',
        'Unholy (feat. Kim Petras)',
        'https://i.scdn.co/image/ab67616d0000b273a935e4689f15953311772cc4',
        'POP',
        131,
        156943,
        'ENERGETIC',
        'SPOTIFY',
        '3nqQXoyQOWXiESFLlDF1hG',
        'spotify:track:3nqQXoyQOWXiESFLlDF1hG',
        'https://open.spotify.com/track/3nqQXoyQOWXiESFLlDF1hG'
    UNION ALL
    SELECT
        'Not Like Us',
        'Kendrick Lamar',
        'Not Like Us',
        'https://i.scdn.co/image/ab67616d0000b2731ea0c62b2339cbf493a999ad',
        'HIPHOP',
        101,
        274192,
        'PEAK',
        'SPOTIFY',
        '6AI3ezQ4o3HUoP6Dhudph3',
        'spotify:track:6AI3ezQ4o3HUoP6Dhudph3',
        'https://open.spotify.com/track/6AI3ezQ4o3HUoP6Dhudph3'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM music
    WHERE spotify_track_id = seed.spotify_track_id
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
    'Spotify 차트 저강도 리커버리',
    'Spotify Search API로 확인한 차트성 트랙 중 낮은 BPM과 안정적인 에너지 곡을 모은 공식 더미 플레이리스트',
    'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1
    FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title = 'Spotify 차트 저강도 리커버리'
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
    'Spotify 차트 중강도 템포',
    'Spotify Search API로 확인한 글로벌 히트 트랙을 중강도 템포런에 맞게 구성한 공식 더미 플레이리스트',
    'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1
    FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title = 'Spotify 차트 중강도 템포'
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
    'Spotify 차트 고강도 드라이브',
    'Spotify Search API로 확인한 업템포 히트 트랙을 고강도 러닝에 맞게 구성한 공식 더미 플레이리스트',
    'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1
    FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title = 'Spotify 차트 고강도 드라이브'
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
    'Spotify 차트 인터벌 스위치업',
    'Spotify Search API로 확인한 히트 트랙을 저속 구간과 스퍼트 구간이 번갈아 오도록 구성한 공식 더미 플레이리스트',
    'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1
    FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title = 'Spotify 차트 인터벌 스위치업'
);

INSERT IGNORE INTO playlist_items (playlist_id, music_id, order_no)
SELECT
    p.id,
    m.id,
    seed.order_no
FROM playlists p
INNER JOIN (
    SELECT 'Spotify 차트 저강도 리커버리' AS playlist_title, '6dOtVTDdiauQNBQEDOtlAB' AS spotify_track_id, 1 AS order_no
    UNION ALL SELECT 'Spotify 차트 저강도 리커버리', '3xkHsmpQCBMytMJNiDf3Ii', 2
    UNION ALL SELECT 'Spotify 차트 저강도 리커버리', '2HRqTpkrJO5ggZyyK6NPWz', 3
    UNION ALL SELECT 'Spotify 차트 저강도 리커버리', '5aIVCx5tnk0ntmdiinnYvw', 4
    UNION ALL SELECT 'Spotify 차트 저강도 리커버리', '7DSAEUvxU8FajXtRloy8M0', 5
    UNION ALL SELECT 'Spotify 차트 중강도 템포', '2CGNAOSuO1MEFCbBRgUzjd', 1
    UNION ALL SELECT 'Spotify 차트 중강도 템포', '5vNRhkKd0yEAg8suGBpjeY', 2
    UNION ALL SELECT 'Spotify 차트 중강도 템포', '2RkZ5LkEzeHGRsmDqKwmaJ', 3
    UNION ALL SELECT 'Spotify 차트 중강도 템포', '2plbrEY59IikOBgBGLjaoe', 4
    UNION ALL SELECT 'Spotify 차트 고강도 드라이브', '4ZtFanR9U6ndgddUvNcjcG', 1
    UNION ALL SELECT 'Spotify 차트 고강도 드라이브', '0VjIjW4GlUZAMYd2vXMi3b', 2
    UNION ALL SELECT 'Spotify 차트 고강도 드라이브', '4Dvkj6JhhA12EX05fT7y2e', 3
    UNION ALL SELECT 'Spotify 차트 인터벌 스위치업', '6AI3ezQ4o3HUoP6Dhudph3', 1
    UNION ALL SELECT 'Spotify 차트 인터벌 스위치업', '3nqQXoyQOWXiESFLlDF1hG', 2
    UNION ALL SELECT 'Spotify 차트 인터벌 스위치업', '5vNRhkKd0yEAg8suGBpjeY', 3
    UNION ALL SELECT 'Spotify 차트 인터벌 스위치업', '7MXVkk9YMctZqd1Srtv4MB', 4
) seed
    ON p.playlist_type = 'OFFICIAL'
   AND p.title = seed.playlist_title
INNER JOIN music m
    ON m.spotify_track_id = seed.spotify_track_id
WHERE p.deleted_at IS NULL;

-- 사용자 제공 캡처 기반 공식 플레이리스트
-- 원본: https://open.spotify.com/playlist/1w48YJ336lM0fcPeKgcxOi
-- 트랙 메타데이터는 캡처의 곡명/아티스트를 Spotify Search API로 검색한 결과다.
-- bpm/music_energy_level은 Spotify 제공 값이 아니라 트랙 분위기 기반 추정 더미값이다.
INSERT INTO music (
    title,
    artist,
    album_name,
    image_url,
    genre,
    bpm,
    duration_ms,
    music_energy_level,
    source_type,
    spotify_track_id,
    spotify_uri,
    spotify_external_url
) VALUES
    ('Peanut butter Sandwich', 'jisokuryClub', 'pool vol. 2 - Peanut Butter Sandwich', 'https://i.scdn.co/image/ab67616d0000b273ba589c20b88529ee242b589f', 'INDIE POP', 126, 231386, 'STEADY', 'SPOTIFY', '3A76bRRuVaIJrV8mrVeb21', 'spotify:track:3A76bRRuVaIJrV8mrVeb21', 'https://open.spotify.com/track/3A76bRRuVaIJrV8mrVeb21'),
    ('Catch Catch', 'YENA', 'LOVE CATCHER', 'https://i.scdn.co/image/ab67616d0000b2739687ef1de31954cfdeac2f5a', 'K-POP', 128, 180000, 'ENERGETIC', 'SPOTIFY', '2O9bMJticxbQ8FH3NiQ7Xh', 'spotify:track:2O9bMJticxbQ8FH3NiQ7Xh', 'https://open.spotify.com/track/2O9bMJticxbQ8FH3NiQ7Xh'),
    ('Drowning', 'WOODZ', 'OO-LI', 'https://i.scdn.co/image/ab67616d0000b273e87a41550ee1b24be3e6e1fb', 'K-ROCK', 150, 244986, 'ENERGETIC', 'SPOTIFY', '4xeugB5MqWh0jwvXZPxahq', 'spotify:track:4xeugB5MqWh0jwvXZPxahq', 'https://open.spotify.com/track/4xeugB5MqWh0jwvXZPxahq'),
    ('Summer', 'The Volunteers', 'The Volunteers', 'https://i.scdn.co/image/ab67616d0000b2732607acdeab22d0a679e4e62b', 'INDIE ROCK', 122, 251033, 'STEADY', 'SPOTIFY', '4OJ3lHcyZdUaYNyhFQdbax', 'spotify:track:4OJ3lHcyZdUaYNyhFQdbax', 'https://open.spotify.com/track/4OJ3lHcyZdUaYNyhFQdbax'),
    ('Paradise', 'Coldplay', 'Mylo Xyloto', 'https://i.scdn.co/image/ab67616d0000b273de0cd11d7b31c3bd1fd5983d', 'ALTERNATIVE ROCK', 140, 278719, 'STEADY', 'SPOTIFY', '6nek1Nin9q48AVZcWs9e9D', 'spotify:track:6nek1Nin9q48AVZcWs9e9D', 'https://open.spotify.com/track/6nek1Nin9q48AVZcWs9e9D'),
    ('0+0', 'HANRORO', 'JAMONG SALGU CLUB', 'https://i.scdn.co/image/ab67616d0000b273afda752d5db59e06d3f86e3b', 'K-INDIE', 136, 192000, 'STEADY', 'SPOTIFY', '3sOAwRg5esaxDcWnUigWPv', 'spotify:track:3sOAwRg5esaxDcWnUigWPv', 'https://open.spotify.com/track/3sOAwRg5esaxDcWnUigWPv'),
    ('REDRED', 'CORTIS', 'REDRED', 'https://i.scdn.co/image/ab67616d0000b27324ace1518851b27e9983f739', 'K-POP', 150, 163001, 'ENERGETIC', 'SPOTIFY', '2fCwv2ppU5nTRTckomIGsd', 'spotify:track:2fCwv2ppU5nTRTckomIGsd', 'https://open.spotify.com/track/2fCwv2ppU5nTRTckomIGsd'),
    ('1:05', 'The Black Skirts', 'TEAM BABY', 'https://i.scdn.co/image/ab67616d0000b27310b3ebc65d2f65964cf461d8', 'K-INDIE', 96, 240000, 'CALM', 'SPOTIFY', '4bjNOhOMvqFPflwHIaS7Fw', 'spotify:track:4bjNOhOMvqFPflwHIaS7Fw', 'https://open.spotify.com/track/4bjNOhOMvqFPflwHIaS7Fw'),
    ('Love Love Love', 'Epik High', 'Remapping the Human Soul', 'https://i.scdn.co/image/ab67616d0000b2734c71e80a8f580184ba910222', 'HIPHOP', 128, 231093, 'STEADY', 'SPOTIFY', '7hlEhmg5h9XuFtaGfgvqjF', 'spotify:track:7hlEhmg5h9XuFtaGfgvqjF', 'https://open.spotify.com/track/7hlEhmg5h9XuFtaGfgvqjF'),
    ('Endangered Love', 'LEE CHANHYUK', 'EROS', 'https://i.scdn.co/image/ab67616d0000b273c675edee6479e3f798d90716', 'K-POP', 132, 220202, 'STEADY', 'SPOTIFY', '06o2o3TpyR8eGU2ANK21Lp', 'spotify:track:06o2o3TpyR8eGU2ANK21Lp', 'https://open.spotify.com/track/06o2o3TpyR8eGU2ANK21Lp'),
    ('60’s Cardin', 'Glen Check', 'Haute Couture', 'https://i.scdn.co/image/ab67616d0000b2731ab018cd8d7d8ef8cc8cd31b', 'ELECTRONIC', 125, 198000, 'ENERGETIC', 'SPOTIFY', '7fzBV8gzOUbO8doR1utLVA', 'spotify:track:7fzBV8gzOUbO8doR1utLVA', 'https://open.spotify.com/track/7fzBV8gzOUbO8doR1utLVA'),
    ('NO PAIN', 'Silica Gel', 'POWER ANDRE 99', 'https://i.scdn.co/image/ab67616d0000b273c0e82bd1db784c147b8cbe31', 'K-ROCK', 170, 221390, 'PEAK', 'SPOTIFY', '4ceXU11FfeiQ47B4cX28gB', 'spotify:track:4ceXU11FfeiQ47B4cX28gB', 'https://open.spotify.com/track/4ceXU11FfeiQ47B4cX28gB'),
    ('Tokyo Girl', 'Kim Kwangjin', 'Solveig', 'https://i.scdn.co/image/ab67616d0000b27381ca9779ba3162c5a8c9c158', 'BALLAD', 88, 276426, 'CALM', 'SPOTIFY', '2fOXSLl3E1xtSLXq6ItWKc', 'spotify:track:2fOXSLl3E1xtSLXq6ItWKc', 'https://open.spotify.com/track/2fOXSLl3E1xtSLXq6ItWKc'),
    ('비밀의 화원', 'Lee Sang-Eun', '신비체험', 'https://i.scdn.co/image/ab67616d0000b273f88202820fd3e11f3959df44', 'BALLAD', 96, 249652, 'CALM', 'SPOTIFY', '2Oe0zgUBGRzkP4KtpIlo8c', 'spotify:track:2Oe0zgUBGRzkP4KtpIlo8c', 'https://open.spotify.com/track/2Oe0zgUBGRzkP4KtpIlo8c'),
    ('항상 엔진을 켜둘께', 'DELISPICE', 'DRRRR!', 'https://i.scdn.co/image/ab67616d0000b273eff555bb6a3eadc3125a3fcb', 'K-ROCK', 150, 198478, 'ENERGETIC', 'SPOTIFY', '1YUpOr8DUaLoTXCNBDCiGA', 'spotify:track:1YUpOr8DUaLoTXCNBDCiGA', 'https://open.spotify.com/track/1YUpOr8DUaLoTXCNBDCiGA'),
    ('Loveholic', 'Loveholic', 'F.L.O.R.I.S.T', 'https://i.scdn.co/image/ab67616d0000b273e794ad35c701488c9b18bf87', 'K-ROCK', 150, 221021, 'ENERGETIC', 'SPOTIFY', '74qKDPA4s7EdriYosZHaFl', 'spotify:track:74qKDPA4s7EdriYosZHaFl', 'https://open.spotify.com/track/74qKDPA4s7EdriYosZHaFl'),
    ('Cloud Cuckoo Land', 'JUNGWOO', 'Cloud Cuckoo Land', 'https://i.scdn.co/image/ab67616d0000b27315e86c06309cad4f62f9dbdc', 'K-INDIE', 112, 215441, 'CALM', 'SPOTIFY', '4MBxYOetGDZUDJ02zHDMkW', 'spotify:track:4MBxYOetGDZUDJ02zHDMkW', 'https://open.spotify.com/track/4MBxYOetGDZUDJ02zHDMkW'),
    ('Four times around the sun', 'NELL', 'Newton’s Apple (6)', 'https://i.scdn.co/image/ab67616d0000b2734e95a63cb1a2f4e3724bf256', 'K-ROCK', 84, 271114, 'CALM', 'SPOTIFY', '0SFENpaJ71GlfgurLEZIeu', 'spotify:track:0SFENpaJ71GlfgurLEZIeu', 'https://open.spotify.com/track/0SFENpaJ71GlfgurLEZIeu'),
    ('Kyo181', 'Silica Gel', 'Kyo181', 'https://i.scdn.co/image/ab67616d0000b2739a2725f124afea54b892b73a', 'K-ROCK', 181, 253614, 'PEAK', 'SPOTIFY', '3ylVy90sNu8DALe0RbwEbi', 'spotify:track:3ylVy90sNu8DALe0RbwEbi', 'https://open.spotify.com/track/3ylVy90sNu8DALe0RbwEbi'),
    ('Aqua Man', 'Beenzino', '24:26 (5th Anniversary Remaster Edition)', 'https://i.scdn.co/image/ab67616d0000b273fce4d97a91b1d51a64ec8a97', 'HIPHOP', 92, 226612, 'STEADY', 'SPOTIFY', '5tEouf2s1SPwAIkOHnvWtQ', 'spotify:track:5tEouf2s1SPwAIkOHnvWtQ', 'https://open.spotify.com/track/5tEouf2s1SPwAIkOHnvWtQ'),
    ('거꾸로 강을 거슬러 오르는 저 힘찬 연어들처럼', 'Kang San-Eh', '연어', 'https://i.scdn.co/image/ab67616d0000b2735950089334db8f0dfebd3612', 'K-ROCK', 156, 281025, 'ENERGETIC', 'SPOTIFY', '2e9nBjEIKzt4Y1Nvn0J69K', 'spotify:track:2e9nBjEIKzt4Y1Nvn0J69K', 'https://open.spotify.com/track/2e9nBjEIKzt4Y1Nvn0J69K'),
    ('Caravan', 'Sultan of the Disco', 'The Golden Age', 'https://i.scdn.co/image/ab67616d0000b2731ef721dbdf62d982dbf5fc57', 'FUNK', 118, 284761, 'STEADY', 'SPOTIFY', '6aYtpgX7eeMiCbqKEejNIj', 'spotify:track:6aYtpgX7eeMiCbqKEejNIj', 'https://open.spotify.com/track/6aYtpgX7eeMiCbqKEejNIj'),
    ('Happy Day', 'cherryfilter', 'Peace N’ Rock N’ Roll', 'https://i.scdn.co/image/ab67616d0000b273d684385a0dad6af18d789839', 'K-ROCK', 166, 215800, 'PEAK', 'SPOTIFY', '0XY1EvCKCCpkHr9TPnYezs', 'spotify:track:0XY1EvCKCCpkHr9TPnYezs', 'https://open.spotify.com/track/0XY1EvCKCCpkHr9TPnYezs'),
    ('Isn’t That Good?', 'YdBB', 'GREAT SEOUL INVASION Semi Final', 'https://i.scdn.co/image/ab67616d0000b273fdf1cb70ec42c0d6b50bcec5', 'K-ROCK', 148, 212746, 'ENERGETIC', 'SPOTIFY', '10ghh7XuPOiqW24K8HTu9r', 'spotify:track:10ghh7XuPOiqW24K8HTu9r', 'https://open.spotify.com/track/10ghh7XuPOiqW24K8HTu9r'),
    ('Passionate Goodbye (Vocal Lee Ji Hyung)', 'Toy, Lee Ji Hyung', 'Thank You', 'https://i.scdn.co/image/ab67616d0000b273f506b8103782dc2a5d17071e', 'BALLAD', 92, 288040, 'CALM', 'SPOTIFY', '5hRxe2YvPdvrNiX6AX6Q3O', 'spotify:track:5hRxe2YvPdvrNiX6AX6Q3O', 'https://open.spotify.com/track/5hRxe2YvPdvrNiX6AX6Q3O'),
    ('ETA', 'NewJeans', 'NewJeans 2nd EP ''Get Up''', 'https://i.scdn.co/image/ab67616d0000b2730744690248ef3ba7b776ea7b', 'K-POP', 135, 151373, 'ENERGETIC', 'SPOTIFY', '56v8WEnGzLByGsDAXDiv4d', 'spotify:track:56v8WEnGzLByGsDAXDiv4d', 'https://open.spotify.com/track/56v8WEnGzLByGsDAXDiv4d'),
    ('Always Awake - Bonus Track', 'Beenzino', '2 4 : 2 6', 'https://i.scdn.co/image/ab67616d0000b27348651627db43a0ddb8b41f57', 'HIPHOP', 93, 220926, 'STEADY', 'SPOTIFY', '6DcJLFmrrkwqhVOtIKr36u', 'spotify:track:6DcJLFmrrkwqhVOtIKr36u', 'https://open.spotify.com/track/6DcJLFmrrkwqhVOtIKr36u'),
    ('Bounce', 'Cho Yong Pil', 'Hello', 'https://i.scdn.co/image/ab67616d0000b2733cb5d930691d167c774779c2', 'POP ROCK', 128, 183987, 'ENERGETIC', 'SPOTIFY', '4aa6QK7mtuYuVcAeTSSOb7', 'spotify:track:4aa6QK7mtuYuVcAeTSSOb7', 'https://open.spotify.com/track/4aa6QK7mtuYuVcAeTSSOb7'),
    ('Rollin''', 'Brave Girls', 'Rollin''', 'https://i.scdn.co/image/ab67616d0000b27373d59cc449e4592026c3997a', 'K-POP', 125, 197746, 'ENERGETIC', 'SPOTIFY', '6SQUopvuZyrWYjYayxxSXl', 'spotify:track:6SQUopvuZyrWYjYayxxSXl', 'https://open.spotify.com/track/6SQUopvuZyrWYjYayxxSXl'),
    ('running on the sky', 'Lee Juck', 'Lee Juck', 'https://i.scdn.co/image/ab67616d0000b273d507f8be4ae14a9dc055e4db', 'K-ROCK', 176, 262786, 'PEAK', 'SPOTIFY', '6ogV0x5m4yw8rvca4m3NxY', 'spotify:track:6ogV0x5m4yw8rvca4m3NxY', 'https://open.spotify.com/track/6ogV0x5m4yw8rvca4m3NxY'),
    ('Beach Again', 'SSAK3', 'Beach Again', 'https://i.scdn.co/image/ab67616d0000b27397953dea11f055b6518ede4f', 'DANCE POP', 120, 233853, 'STEADY', 'SPOTIFY', '4L1MHK27ifT30Ndicpr7js', 'spotify:track:4L1MHK27ifT30Ndicpr7js', 'https://open.spotify.com/track/4L1MHK27ifT30Ndicpr7js')
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    artist = VALUES(artist),
    album_name = VALUES(album_name),
    image_url = VALUES(image_url),
    genre = VALUES(genre),
    bpm = VALUES(bpm),
    duration_ms = VALUES(duration_ms),
    music_energy_level = VALUES(music_energy_level),
    spotify_uri = VALUES(spotify_uri),
    spotify_external_url = VALUES(spotify_external_url);

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
    '16반의 플레이리스트',
    '사용자 제공 Spotify 플레이리스트 캡처를 바탕으로 Spotify Search API 매칭 결과를 저장한 공식 더미 플레이리스트',
    'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1
    FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title = '16반의 플레이리스트'
);

INSERT IGNORE INTO playlist_items (playlist_id, music_id, order_no)
SELECT
    p.id,
    m.id,
    seed.order_no
FROM playlists p
INNER JOIN (
    SELECT '3A76bRRuVaIJrV8mrVeb21' AS spotify_track_id, 1 AS order_no
    UNION ALL SELECT '2O9bMJticxbQ8FH3NiQ7Xh', 2
    UNION ALL SELECT '4xeugB5MqWh0jwvXZPxahq', 3
    UNION ALL SELECT '4OJ3lHcyZdUaYNyhFQdbax', 4
    UNION ALL SELECT '6nek1Nin9q48AVZcWs9e9D', 5
    UNION ALL SELECT '3sOAwRg5esaxDcWnUigWPv', 6
    UNION ALL SELECT '2fCwv2ppU5nTRTckomIGsd', 7
    UNION ALL SELECT '4bjNOhOMvqFPflwHIaS7Fw', 8
    UNION ALL SELECT '7hlEhmg5h9XuFtaGfgvqjF', 9
    UNION ALL SELECT '06o2o3TpyR8eGU2ANK21Lp', 10
    UNION ALL SELECT '7fzBV8gzOUbO8doR1utLVA', 11
    UNION ALL SELECT '4ceXU11FfeiQ47B4cX28gB', 12
    UNION ALL SELECT '2fOXSLl3E1xtSLXq6ItWKc', 13
    UNION ALL SELECT '2Oe0zgUBGRzkP4KtpIlo8c', 14
    UNION ALL SELECT '1YUpOr8DUaLoTXCNBDCiGA', 15
    UNION ALL SELECT '74qKDPA4s7EdriYosZHaFl', 16
    UNION ALL SELECT '4MBxYOetGDZUDJ02zHDMkW', 17
    UNION ALL SELECT '0SFENpaJ71GlfgurLEZIeu', 18
    UNION ALL SELECT '3ylVy90sNu8DALe0RbwEbi', 19
    UNION ALL SELECT '5tEouf2s1SPwAIkOHnvWtQ', 20
    UNION ALL SELECT '2e9nBjEIKzt4Y1Nvn0J69K', 21
    UNION ALL SELECT '6aYtpgX7eeMiCbqKEejNIj', 22
    UNION ALL SELECT '0XY1EvCKCCpkHr9TPnYezs', 23
    UNION ALL SELECT '10ghh7XuPOiqW24K8HTu9r', 24
    UNION ALL SELECT '5hRxe2YvPdvrNiX6AX6Q3O', 25
    UNION ALL SELECT '56v8WEnGzLByGsDAXDiv4d', 26
    UNION ALL SELECT '6DcJLFmrrkwqhVOtIKr36u', 27
    UNION ALL SELECT '4aa6QK7mtuYuVcAeTSSOb7', 28
    UNION ALL SELECT '6SQUopvuZyrWYjYayxxSXl', 29
    UNION ALL SELECT '6ogV0x5m4yw8rvca4m3NxY', 30
    UNION ALL SELECT '4L1MHK27ifT30Ndicpr7js', 31
) seed
INNER JOIN music m
    ON m.spotify_track_id = seed.spotify_track_id
WHERE p.playlist_type = 'OFFICIAL'
  AND p.title = '16반의 플레이리스트'
  AND p.deleted_at IS NULL;

-- 현재 music.bpm 기준 강도별 기본 플레이리스트
-- LOW: bpm < 125, MODERATE: 125 <= bpm < 150,
-- HIGH: 150 <= bpm < 175, INTERVAL: bpm >= 175
-- 반복 실행 시 최신 BPM 기준으로 수록곡을 다시 구성한다.
DELETE pi
FROM playlist_items pi
INNER JOIN playlists p ON p.id = pi.playlist_id
WHERE p.playlist_type = 'OFFICIAL'
  AND p.title IN (
      'BPM 기본 저강도 러닝',
      'BPM 기본 중강도 템포',
      'BPM 기본 고강도 드라이브',
      'BPM 기본 인터벌 스퍼트'
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
SELECT NULL, 'OFFICIAL', 'OFFICIAL', 'PUBLIC',
       'BPM 기본 저강도 러닝',
       '현재 DB 음악 중 BPM 125 미만 곡을 자동 분류한 기본 공식 플레이리스트',
       'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1 FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title = 'BPM 기본 저강도 러닝'
)
UNION ALL
SELECT NULL, 'OFFICIAL', 'OFFICIAL', 'PUBLIC',
       'BPM 기본 중강도 템포',
       '현재 DB 음악 중 BPM 125 이상 150 미만 곡을 자동 분류한 기본 공식 플레이리스트',
       'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1 FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title = 'BPM 기본 중강도 템포'
)
UNION ALL
SELECT NULL, 'OFFICIAL', 'OFFICIAL', 'PUBLIC',
       'BPM 기본 고강도 드라이브',
       '현재 DB 음악 중 BPM 150 이상 175 미만 곡을 자동 분류한 기본 공식 플레이리스트',
       'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1 FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title = 'BPM 기본 고강도 드라이브'
)
UNION ALL
SELECT NULL, 'OFFICIAL', 'OFFICIAL', 'PUBLIC',
       'BPM 기본 인터벌 스퍼트',
       '현재 DB 음악 중 BPM 175 이상 곡을 자동 분류한 기본 공식 플레이리스트',
       'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1 FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title = 'BPM 기본 인터벌 스퍼트'
);

INSERT IGNORE INTO playlist_items (playlist_id, music_id, order_no)
SELECT
    p.id,
    classified.music_id,
    classified.order_no
FROM playlists p
INNER JOIN (
    SELECT
        base.music_id,
        base.intensity,
        ROW_NUMBER() OVER (
            PARTITION BY base.intensity
            ORDER BY base.bpm ASC, base.title ASC, base.music_id ASC
        ) AS order_no
    FROM (
        SELECT
            id AS music_id,
            title,
            bpm,
            CASE
                WHEN bpm < 125 THEN 'LOW'
                WHEN bpm < 150 THEN 'MODERATE'
                WHEN bpm < 175 THEN 'HIGH'
                ELSE 'INTERVAL'
            END AS intensity
        FROM music
        WHERE bpm IS NOT NULL
    ) base
) classified
    ON p.title = CASE classified.intensity
        WHEN 'LOW' THEN 'BPM 기본 저강도 러닝'
        WHEN 'MODERATE' THEN 'BPM 기본 중강도 템포'
        WHEN 'HIGH' THEN 'BPM 기본 고강도 드라이브'
        WHEN 'INTERVAL' THEN 'BPM 기본 인터벌 스퍼트'
    END
WHERE p.playlist_type = 'OFFICIAL'
  AND p.deleted_at IS NULL;

-- ---- END official base demo data ----


-- ---- BEGIN WORKOUT K-POP demo playlist ----

-- Spotify Search API matched from demo playlist screenshots.
-- BPM, genre, and music_energy_level are inferred dummy metadata because Spotify Search API does not provide them.
-- Low-confidence matches are intentionally skipped.
UPDATE playlists
SET
    title = 'WORKOUT K-POP 데모',
    description = 'Spotify WORKOUT 플레이리스트'
WHERE playlist_type = 'OFFICIAL'
  AND title = '캡처 기반 K-POP 데모 플레이리스트'
  AND deleted_at IS NULL;

UPDATE playlists
SET description = 'Spotify WORKOUT 플레이리스트'
WHERE playlist_type = 'OFFICIAL'
  AND title = 'WORKOUT K-POP 데모'
  AND deleted_at IS NULL;

INSERT INTO music (
    title,
    artist,
    album_name,
    image_url,
    genre,
    bpm,
    duration_ms,
    music_energy_level,
    source_type,
    spotify_track_id,
    spotify_uri,
    spotify_external_url
) VALUES
    ('LEMONADE', 'aespa', 'LEMONADE - The 2nd Album', 'https://i.scdn.co/image/ab67616d0000b2735f0171c63cceb32ccff8b9a9', 'K-POP', 125, 187000, 'ENERGETIC', 'SPOTIFY', '6vjt2smGK75oc9r2OGFfgp', 'spotify:track:6vjt2smGK75oc9r2OGFfgp', 'https://open.spotify.com/track/6vjt2smGK75oc9r2OGFfgp'),
    ('LOVE ATTACK', 'RESCENE', 'SCENEDROME', 'https://i.scdn.co/image/ab67616d0000b273831c0d124fc7c7a3ad0618ab', 'K-POP', 128, 181560, 'ENERGETIC', 'SPOTIFY', '6uosenLUNFZtOIih5T0qfV', 'spotify:track:6uosenLUNFZtOIih5T0qfV', 'https://open.spotify.com/track/6uosenLUNFZtOIih5T0qfV'),
    ('LILAC', 'IU', 'IU 5th Album ''LILAC''', 'https://i.scdn.co/image/ab67616d0000b2734ed058b71650a6ca2c04adff', 'K-POP', 115, 214253, 'STEADY', 'SPOTIFY', '5xrtzzzikpG3BLbo4q1Yul', 'spotify:track:5xrtzzzikpG3BLbo4q1Yul', 'https://open.spotify.com/track/5xrtzzzikpG3BLbo4q1Yul'),
    ('Ditto', 'NewJeans', 'Ditto', 'https://i.scdn.co/image/ab67616d0000b273edf5b257be1d6593e81bb45f', 'K-POP', 134, 185506, 'STEADY', 'SPOTIFY', '3r8RuvgbX9s7ammBn07D3W', 'spotify:track:3r8RuvgbX9s7ammBn07D3W', 'https://open.spotify.com/track/3r8RuvgbX9s7ammBn07D3W'),
    ('Blue Valentine', 'NMIXX', 'Blue Valentine', 'https://i.scdn.co/image/ab67616d0000b273b9f61d52302a78a2972785a2', 'K-POP', 124, 186000, 'STEADY', 'SPOTIFY', '0FKR7mQJnYqdEmHAMhgQjw', 'spotify:track:0FKR7mQJnYqdEmHAMhgQjw', 'https://open.spotify.com/track/0FKR7mQJnYqdEmHAMhgQjw'),
    ('Welcome to the Show', 'DAY6', 'Fourever', 'https://i.scdn.co/image/ab67616d0000b2731e20be5a6bdfd2780516320a', 'K-ROCK', 152, 217067, 'ENERGETIC', 'SPOTIFY', '3BMAWsBp4jc35mCmebC3WT', 'spotify:track:3BMAWsBp4jc35mCmebC3WT', 'https://open.spotify.com/track/3BMAWsBp4jc35mCmebC3WT'),
    ('Magnetic', 'ILLIT', 'SUPER REAL ME', 'https://i.scdn.co/image/ab67616d0000b273f037c5fb9de6c78726cb8e2c', 'K-POP', 131, 160688, 'ENERGETIC', 'SPOTIFY', '1aKvZDoLGkNMxoRYgkckZG', 'spotify:track:1aKvZDoLGkNMxoRYgkckZG', 'https://open.spotify.com/track/1aKvZDoLGkNMxoRYgkckZG'),
    ('Perfect Night', 'LE SSERAFIM', 'Perfect Night', 'https://i.scdn.co/image/ab67616d0000b273c9af8979e5cb3fe7ecc5e230', 'K-POP', 136, 159080, 'STEADY', 'SPOTIFY', '74X2u8JMVooG2QbjRxXwR8', 'spotify:track:74X2u8JMVooG2QbjRxXwR8', 'https://open.spotify.com/track/74X2u8JMVooG2QbjRxXwR8'),
    ('Supernatural', 'NewJeans', 'Supernatural', 'https://i.scdn.co/image/ab67616d0000b2737e1eeb0d7cc374a168369c80', 'K-POP', 104, 191000, 'STEADY', 'SPOTIFY', '5ocSQW5sIUIOFojwXEz9Ki', 'spotify:track:5ocSQW5sIUIOFojwXEz9Ki', 'https://open.spotify.com/track/5ocSQW5sIUIOFojwXEz9Ki'),
    ('Standing Next to You', 'Jung Kook', 'GOLDEN', 'https://i.scdn.co/image/ab67616d0000b273741fd4807f442af3f7359316', 'FUNK POP', 106, 206019, 'STEADY', 'SPOTIFY', '2KslE17cAJNHTsI2MI0jb2', 'spotify:track:2KslE17cAJNHTsI2MI0jb2', 'https://open.spotify.com/track/2KslE17cAJNHTsI2MI0jb2'),
    ('INVU', 'TAEYEON', 'INVU - The 3rd Album', 'https://i.scdn.co/image/ab67616d0000b273034c3a8ba89c6a5ecfda3175', 'K-POP', 106, 204973, 'STEADY', 'SPOTIFY', '7rXcCpIAoOUCydkVDMcoPV', 'spotify:track:7rXcCpIAoOUCydkVDMcoPV', 'https://open.spotify.com/track/7rXcCpIAoOUCydkVDMcoPV'),
    ('NOT CUTE ANYMORE', 'ILLIT', 'NOT CUTE ANYMORE', 'https://i.scdn.co/image/ab67616d0000b2733d09075f6c7fabe9a7234cb3', 'K-POP', 130, 131997, 'ENERGETIC', 'SPOTIFY', '1k0JAiH11gHL9dc5dfQjQr', 'spotify:track:1k0JAiH11gHL9dc5dfQjQr', 'https://open.spotify.com/track/1k0JAiH11gHL9dc5dfQjQr'),
    ('toxic till the end', 'ROSÉ', 'rosie', 'https://i.scdn.co/image/ab67616d0000b273a9fb6e00986e42ad4764b1f3', 'POP', 120, 156685, 'STEADY', 'SPOTIFY', '30HIJzJEUYcL9Qng15UeBo', 'spotify:track:30HIJzJEUYcL9Qng15UeBo', 'https://open.spotify.com/track/30HIJzJEUYcL9Qng15UeBo'),
    ('How Sweet', 'NewJeans', 'How Sweet', 'https://i.scdn.co/image/ab67616d0000b273b657fbb27b17e7bd4691c2b2', 'K-POP', 108, 219026, 'STEADY', 'SPOTIFY', '38tXZcL1gZRfbqfOG0VMTH', 'spotify:track:38tXZcL1gZRfbqfOG0VMTH', 'https://open.spotify.com/track/38tXZcL1gZRfbqfOG0VMTH'),
    ('I DO ME', 'KiiiKiii', 'I DO ME', 'https://i.scdn.co/image/ab67616d0000b2739e9ac9734925e3891401d91d', 'K-POP', 128, 190556, 'ENERGETIC', 'SPOTIFY', '6qmfxmtoNEzqucF1FWeXl8', 'spotify:track:6qmfxmtoNEzqucF1FWeXl8', 'https://open.spotify.com/track/6qmfxmtoNEzqucF1FWeXl8'),
    ('What is Love?', 'TWICE', 'What is Love?', 'https://i.scdn.co/image/ab67616d0000b2734ddb095ce368de91b088f1ce', 'K-POP', 170, 208238, 'PEAK', 'SPOTIFY', '7L81PqO3zKKPsfO7jfyvus', 'spotify:track:7L81PqO3zKKPsfO7jfyvus', 'https://open.spotify.com/track/7L81PqO3zKKPsfO7jfyvus'),
    ('Billyeoon Goyangi (Do the Dance)', 'ILLIT', 'bomb', 'https://i.scdn.co/image/ab67616d0000b273f24755716ee6789fa094112c', 'K-POP', 150, 188641, 'ENERGETIC', 'SPOTIFY', '4p0uVFAXKUISu8yp9gNtWE', 'spotify:track:4p0uVFAXKUISu8yp9gNtWE', 'https://open.spotify.com/track/4p0uVFAXKUISu8yp9gNtWE'),
    ('Whiplash', 'aespa', 'Whiplash - The 5th Mini Album', 'https://i.scdn.co/image/ab67616d0000b273e467a8e8d7b0aa92d354aa75', 'ELECTRONIC K-POP', 126, 183226, 'ENERGETIC', 'SPOTIFY', '6uPnrBgweGOcwjFL4ItAvV', 'spotify:track:6uPnrBgweGOcwjFL4ItAvV', 'https://open.spotify.com/track/6uPnrBgweGOcwjFL4ItAvV'),
    ('Attention', 'NewJeans', 'NewJeans 1st EP ''New Jeans''', 'https://i.scdn.co/image/ab67616d0000b2739d28fd01859073a3ae6ea209', 'K-POP', 105, 180066, 'STEADY', 'SPOTIFY', '2pIUpMhHL6L9Z5lnKxJJr9', 'spotify:track:2pIUpMhHL6L9Z5lnKxJJr9', 'https://open.spotify.com/track/2pIUpMhHL6L9Z5lnKxJJr9'),
    ('I AM', 'IVE', 'I''ve IVE', 'https://i.scdn.co/image/ab67616d0000b2734156626a16155286856ea868', 'K-POP', 123, 183853, 'ENERGETIC', 'SPOTIFY', '7KavHYqoVFNB3IUXfs5gvP', 'spotify:track:7KavHYqoVFNB3IUXfs5gvP', 'https://open.spotify.com/track/7KavHYqoVFNB3IUXfs5gvP'),
    ('HOT', 'LE SSERAFIM', 'HOT', 'https://i.scdn.co/image/ab67616d0000b2731fc0f4faafaa183cc70297e5', 'K-POP', 116, 143797, 'STEADY', 'SPOTIFY', '406IpEtZPvbxApWTGM3twY', 'spotify:track:406IpEtZPvbxApWTGM3twY', 'https://open.spotify.com/track/406IpEtZPvbxApWTGM3twY'),
    ('FAMOUS', 'ALLDAY PROJECT', 'FAMOUS', 'https://i.scdn.co/image/ab67616d0000b27346cfc9a8374ce3c956d2dc71', 'K-POP', 130, 180353, 'ENERGETIC', 'SPOTIFY', '598bDTl82CSTSDnZG0XhGK', 'spotify:track:598bDTl82CSTSDnZG0XhGK', 'https://open.spotify.com/track/598bDTl82CSTSDnZG0XhGK'),
    ('Supersonic', 'fromis_9', 'Supersonic', 'https://i.scdn.co/image/ab67616d0000b2730490e8ab48790ca6e5add267', 'K-POP', 130, 174146, 'ENERGETIC', 'SPOTIFY', '6oNLSQX8bcAdbCElZYju3v', 'spotify:track:6oNLSQX8bcAdbCElZYju3v', 'https://open.spotify.com/track/6oNLSQX8bcAdbCElZYju3v'),
    ('Hype Boy', 'NewJeans', 'NewJeans 1st EP ''New Jeans''', 'https://i.scdn.co/image/ab67616d0000b2739d28fd01859073a3ae6ea209', 'K-POP', 100, 179026, 'STEADY', 'SPOTIFY', '0a4MMyCrzT0En247IhqZbD', 'spotify:track:0a4MMyCrzT0En247IhqZbD', 'https://open.spotify.com/track/0a4MMyCrzT0En247IhqZbD'),
    ('Cosmic', 'Red Velvet', 'Cosmic', 'https://i.scdn.co/image/ab67616d0000b273a54b93bff4827437f56b7896', 'K-POP', 122, 225864, 'STEADY', 'SPOTIFY', '3pDGJRDN3p6kCNZcD97FYY', 'spotify:track:3pDGJRDN3p6kCNZcD97FYY', 'https://open.spotify.com/track/3pDGJRDN3p6kCNZcD97FYY'),
    ('ONE MORE TIME', 'ALLDAY PROJECT', 'ONE MORE TIME', 'https://i.scdn.co/image/ab67616d0000b2732975f0c1acccc15ccaadcb3a', 'K-POP', 128, 195259, 'ENERGETIC', 'SPOTIFY', '4eCan7Jb3q0xQ1FAELOtEz', 'spotify:track:4eCan7Jb3q0xQ1FAELOtEz', 'https://open.spotify.com/track/4eCan7Jb3q0xQ1FAELOtEz'),
    ('body', 'DAYOUNG', 'gonna love me, right?', 'https://i.scdn.co/image/ab67616d0000b273d0e02167a1fc0a06f8bb38fb', 'K-POP', 112, 168000, 'STEADY', 'SPOTIFY', '4LJJxB4a47mreJAfKwbGEM', 'spotify:track:4LJJxB4a47mreJAfKwbGEM', 'https://open.spotify.com/track/4LJJxB4a47mreJAfKwbGEM'),
    ('After LIKE', 'IVE', 'After LIKE', 'https://i.scdn.co/image/ab67616d0000b2731abe22e6883e5a8b3f4726e2', 'K-POP', 125, 176973, 'ENERGETIC', 'SPOTIFY', '6CV6j2xz54thzlrWML3kAW', 'spotify:track:6CV6j2xz54thzlrWML3kAW', 'https://open.spotify.com/track/6CV6j2xz54thzlrWML3kAW'),
    ('Supernova', 'aespa', 'Armageddon - The 1st Album', 'https://i.scdn.co/image/ab67616d0000b2730fc598038040859794c600e2', 'K-POP', 120, 178880, 'ENERGETIC', 'SPOTIFY', '2VdSktBqFfkW7y6q5Ik4Z4', 'spotify:track:2VdSktBqFfkW7y6q5Ik4Z4', 'https://open.spotify.com/track/2VdSktBqFfkW7y6q5Ik4Z4'),
    ('404 (New Era)', 'KiiiKiii', 'Delulu Pack', 'https://i.scdn.co/image/ab67616d0000b2738c77cd2419fcdb44198262a3', 'K-POP', 140, 179586, 'ENERGETIC', 'SPOTIFY', '1rEa59P5yEal5cp1h7kl2e', 'spotify:track:1rEa59P5yEal5cp1h7kl2e', 'https://open.spotify.com/track/1rEa59P5yEal5cp1h7kl2e'),
    ('Heavy Serenade', 'NMIXX', 'Heavy Serenade', 'https://i.scdn.co/image/ab67616d0000b27357437d1095e12513210162c5', 'K-POP', 126, 180000, 'ENERGETIC', 'SPOTIFY', '2adYJ0qJw5Io8nhtQ9KjHq', 'spotify:track:2adYJ0qJw5Io8nhtQ9KjHq', 'https://open.spotify.com/track/2adYJ0qJw5Io8nhtQ9KjHq'),
    ('TOO BAD (feat. Anderson .Paak)', 'G-DRAGON, Anderson .Paak', 'Übermensch', 'https://i.scdn.co/image/ab67616d0000b2730c8549d4dad1c1e95f316736', 'K-HIPHOP', 102, 153664, 'STEADY', 'SPOTIFY', '0Kk5TRkYuWXY89KamtFEFw', 'spotify:track:0Kk5TRkYuWXY89KamtFEFw', 'https://open.spotify.com/track/0Kk5TRkYuWXY89KamtFEFw'),
    ('REBEL HEART', 'IVE', 'REBEL HEART', 'https://i.scdn.co/image/ab67616d0000b273307e7e4ca1b2c02108aec9a0', 'K-POP', 110, 188266, 'STEADY', 'SPOTIFY', '2JoRBbFJyKytt4y3D0HxQu', 'spotify:track:2JoRBbFJyKytt4y3D0HxQu', 'https://open.spotify.com/track/2JoRBbFJyKytt4y3D0HxQu'),
    ('DM', 'fromis_9', 'Midnight Guest', 'https://i.scdn.co/image/ab67616d0000b273cf53b5c466a64daece07edc4', 'K-POP', 125, 204293, 'ENERGETIC', 'SPOTIFY', '7B9W7Qsy5M2kyUNjQYIEG8', 'spotify:track:7B9W7Qsy5M2kyUNjQYIEG8', 'https://open.spotify.com/track/7B9W7Qsy5M2kyUNjQYIEG8'),
    ('Cherish (My Love)', 'ILLIT', 'I''LL LIKE YOU', 'https://i.scdn.co/image/ab67616d0000b273fc2fffae3025fbc540b98f34', 'K-POP', 130, 176854, 'ENERGETIC', 'SPOTIFY', '2MoUuJhpSO4a0czxvsrSC6', 'spotify:track:2MoUuJhpSO4a0czxvsrSC6', 'https://open.spotify.com/track/2MoUuJhpSO4a0czxvsrSC6'),
    ('FANCY', 'TWICE', 'FANCY YOU', 'https://i.scdn.co/image/ab67616d0000b2730a19a3bc8b5350c676d64bcf', 'K-POP', 98, 213885, 'STEADY', 'SPOTIFY', '2IZ6fFOq5d8lT0CxLDcxKz', 'spotify:track:2IZ6fFOq5d8lT0CxLDcxKz', 'https://open.spotify.com/track/2IZ6fFOq5d8lT0CxLDcxKz'),
    ('My Universe', 'Coldplay, BTS', 'My Universe', 'https://i.scdn.co/image/ab67616d0000b273526301de441246800ca739a5', 'POP ROCK', 105, 228000, 'STEADY', 'SPOTIFY', '3FeVmId7tL5YN8B7R3imoM', 'spotify:track:3FeVmId7tL5YN8B7R3imoM', 'https://open.spotify.com/track/3FeVmId7tL5YN8B7R3imoM'),
    ('작은 것들을 위한 시 (Boy With Luv) (feat. Halsey)', 'BTS, Halsey', 'MAP OF THE SOUL : PERSONA', 'https://i.scdn.co/image/ab67616d0000b27346d3e73ccbfde4ee771b7e76', 'K-POP', 120, 229773, 'ENERGETIC', 'SPOTIFY', '2V7Gxq4QW2Fm4zizDF0V6Z', 'spotify:track:2V7Gxq4QW2Fm4zizDF0V6Z', 'https://open.spotify.com/track/2V7Gxq4QW2Fm4zizDF0V6Z'),
    ('Girls Never Die', 'tripleS', '<ASSEMBLE24>', 'https://i.scdn.co/image/ab67616d0000b2734b9eb26603f674ec91dcd545', 'K-POP', 130, 187920, 'ENERGETIC', 'SPOTIFY', '0Ol7uhYjodbXAKKarXdn6r', 'spotify:track:0Ol7uhYjodbXAKKarXdn6r', 'https://open.spotify.com/track/0Ol7uhYjodbXAKKarXdn6r')
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    artist = VALUES(artist),
    album_name = VALUES(album_name),
    image_url = VALUES(image_url),
    genre = VALUES(genre),
    bpm = VALUES(bpm),
    duration_ms = VALUES(duration_ms),
    music_energy_level = VALUES(music_energy_level),
    spotify_uri = VALUES(spotify_uri),
    spotify_external_url = VALUES(spotify_external_url);

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
    'WORKOUT K-POP 데모',
    'Spotify WORKOUT 플레이리스트',
    'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1
    FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title = 'WORKOUT K-POP 데모'
);

INSERT IGNORE INTO playlist_items (playlist_id, music_id, order_no)
SELECT
    p.id,
    m.id,
    seed.order_no
FROM playlists p
INNER JOIN (
    SELECT '6vjt2smGK75oc9r2OGFfgp' AS spotify_track_id, 1 AS order_no
    UNION ALL SELECT '6uosenLUNFZtOIih5T0qfV', 2
    UNION ALL SELECT '5xrtzzzikpG3BLbo4q1Yul', 3
    UNION ALL SELECT '3r8RuvgbX9s7ammBn07D3W', 4
    UNION ALL SELECT '0FKR7mQJnYqdEmHAMhgQjw', 5
    UNION ALL SELECT '3BMAWsBp4jc35mCmebC3WT', 6
    UNION ALL SELECT '1aKvZDoLGkNMxoRYgkckZG', 7
    UNION ALL SELECT '74X2u8JMVooG2QbjRxXwR8', 8
    UNION ALL SELECT '5ocSQW5sIUIOFojwXEz9Ki', 9
    UNION ALL SELECT '2KslE17cAJNHTsI2MI0jb2', 10
    UNION ALL SELECT '7rXcCpIAoOUCydkVDMcoPV', 11
    UNION ALL SELECT '1k0JAiH11gHL9dc5dfQjQr', 12
    UNION ALL SELECT '30HIJzJEUYcL9Qng15UeBo', 13
    UNION ALL SELECT '38tXZcL1gZRfbqfOG0VMTH', 14
    UNION ALL SELECT '6qmfxmtoNEzqucF1FWeXl8', 15
    UNION ALL SELECT '7L81PqO3zKKPsfO7jfyvus', 16
    UNION ALL SELECT '4p0uVFAXKUISu8yp9gNtWE', 17
    UNION ALL SELECT '6uPnrBgweGOcwjFL4ItAvV', 18
    UNION ALL SELECT '2pIUpMhHL6L9Z5lnKxJJr9', 19
    UNION ALL SELECT '7KavHYqoVFNB3IUXfs5gvP', 20
    UNION ALL SELECT '406IpEtZPvbxApWTGM3twY', 21
    UNION ALL SELECT '598bDTl82CSTSDnZG0XhGK', 22
    UNION ALL SELECT '6oNLSQX8bcAdbCElZYju3v', 23
    UNION ALL SELECT '0a4MMyCrzT0En247IhqZbD', 24
    UNION ALL SELECT '3pDGJRDN3p6kCNZcD97FYY', 25
    UNION ALL SELECT '4eCan7Jb3q0xQ1FAELOtEz', 27
    UNION ALL SELECT '4LJJxB4a47mreJAfKwbGEM', 28
    UNION ALL SELECT '6CV6j2xz54thzlrWML3kAW', 29
    UNION ALL SELECT '2VdSktBqFfkW7y6q5Ik4Z4', 30
    UNION ALL SELECT '1rEa59P5yEal5cp1h7kl2e', 31
    UNION ALL SELECT '2adYJ0qJw5Io8nhtQ9KjHq', 32
    UNION ALL SELECT '0Kk5TRkYuWXY89KamtFEFw', 33
    UNION ALL SELECT '2JoRBbFJyKytt4y3D0HxQu', 34
    UNION ALL SELECT '7B9W7Qsy5M2kyUNjQYIEG8', 35
    UNION ALL SELECT '2MoUuJhpSO4a0czxvsrSC6', 36
    UNION ALL SELECT '2IZ6fFOq5d8lT0CxLDcxKz', 37
    UNION ALL SELECT '3FeVmId7tL5YN8B7R3imoM', 39
    UNION ALL SELECT '2V7Gxq4QW2Fm4zizDF0V6Z', 40
    UNION ALL SELECT '0Ol7uhYjodbXAKKarXdn6r', 41
) seed
INNER JOIN music m
    ON m.spotify_track_id = seed.spotify_track_id
WHERE p.playlist_type = 'OFFICIAL'
  AND p.title = 'WORKOUT K-POP 데모'
  AND p.deleted_at IS NULL;

-- ---- END WORKOUT K-POP demo playlist ----


-- ---- BEGIN Zone 2 running demo playlist ----

-- Spotify Search API matched from a Zone 2 running demo playlist screenshot.
-- BPM, genre, and music_energy_level are inferred dummy metadata because Spotify Search API does not provide them.
-- The inferred BPM range is intentionally low so the playlist is grouped as low-intensity running.

UPDATE playlists
SET title = 'Zone 2 데모',
    description = 'Spotify Zone 2 러닝을 위한 잔잔한 팝 플레이리스트'
WHERE playlist_type = 'OFFICIAL'
  AND title IN ('Zone 2 러닝', 'Zone 2 데모')
  AND deleted_at IS NULL;

INSERT INTO music (
    title,
    artist,
    album_name,
    image_url,
    genre,
    bpm,
    duration_ms,
    music_energy_level,
    source_type,
    spotify_track_id,
    spotify_uri,
    spotify_external_url
) VALUES
    ('Dancing With A Stranger (with Normani)', 'Sam Smith, Normani', 'Dancing With A Stranger (with Normani)', 'https://i.scdn.co/image/ab67616d0000b2733b52eca47232bedfbb5e9443', 'POP', 104, 171029, 'STEADY', 'SPOTIFY', '6Qs4SXO9dwPj5GKvVOv8Ki', 'spotify:track:6Qs4SXO9dwPj5GKvVOv8Ki', 'https://open.spotify.com/track/6Qs4SXO9dwPj5GKvVOv8Ki'),
    ('Too Good At Goodbyes', 'Sam Smith', 'The Thrill Of It All (Special Edition)', 'https://i.scdn.co/image/ab67616d0000b273005cd7d0ae87b081601f6cca', 'POP BALLAD', 92, 201000, 'CALM', 'SPOTIFY', '1mXVgsBdtIVeCLJnSnmtdV', 'spotify:track:1mXVgsBdtIVeCLJnSnmtdV', 'https://open.spotify.com/track/1mXVgsBdtIVeCLJnSnmtdV'),
    ('In My Blood', 'Shawn Mendes', 'Shawn Mendes', 'https://i.scdn.co/image/ab67616d0000b273269423eb6467e308c0fbce24', 'POP ROCK', 108, 211360, 'STEADY', 'SPOTIFY', '2QZ7WLBE8h2y1Y5Fb8RYbH', 'spotify:track:2QZ7WLBE8h2y1Y5Fb8RYbH', 'https://open.spotify.com/track/2QZ7WLBE8h2y1Y5Fb8RYbH'),
    ('Somebody Else', 'The 1975', 'I like it when you sleep, for you are so beautiful yet so unaware of it', 'https://i.scdn.co/image/ab67616d0000b273206517a3f7e4c34bf0bfc531', 'INDIE POP', 101, 347525, 'CALM', 'SPOTIFY', '4m0q0xQ2BNl9SCAGKyfiGZ', 'spotify:track:4m0q0xQ2BNl9SCAGKyfiGZ', 'https://open.spotify.com/track/4m0q0xQ2BNl9SCAGKyfiGZ'),
    ('Robbers', 'The 1975', 'The 1975', 'https://i.scdn.co/image/ab67616d0000b27304f21e12e67ba0dfd5db4fbf', 'INDIE ROCK', 100, 254509, 'STEADY', 'SPOTIFY', '73jVPicY2G9YHmzgjk69ae', 'spotify:track:73jVPicY2G9YHmzgjk69ae', 'https://open.spotify.com/track/73jVPicY2G9YHmzgjk69ae'),
    ('Yellow', 'Coldplay', 'Parachutes', 'https://i.scdn.co/image/ab67616d0000b2739164bafe9aaa168d93f4816a', 'ALTERNATIVE ROCK', 86, 266773, 'CALM', 'SPOTIFY', '3AJwUDP919kvQ9QcozQPxg', 'spotify:track:3AJwUDP919kvQ9QcozQPxg', 'https://open.spotify.com/track/3AJwUDP919kvQ9QcozQPxg'),
    ('A Head Full of Dreams', 'Coldplay', 'A Head Full of Dreams', 'https://i.scdn.co/image/ab67616d0000b2738ff7c3580d429c8212b9a3b6', 'POP ROCK', 115, 223773, 'STEADY', 'SPOTIFY', '6f49kbOuQSOsStBpyGvQfA', 'spotify:track:6f49kbOuQSOsStBpyGvQfA', 'https://open.spotify.com/track/6f49kbOuQSOsStBpyGvQfA'),
    ('Up&Up', 'Coldplay', 'A Head Full of Dreams', 'https://i.scdn.co/image/ab67616d0000b2738ff7c3580d429c8212b9a3b6', 'POP ROCK', 122, 405320, 'STEADY', 'SPOTIFY', '31L9yLXSj6LpCFupyMV6CR', 'spotify:track:31L9yLXSj6LpCFupyMV6CR', 'https://open.spotify.com/track/31L9yLXSj6LpCFupyMV6CR'),
    ('True Love', 'Coldplay', 'Ghost Stories', 'https://i.scdn.co/image/ab67616d0000b273e5a95573f1b91234630fd2cf', 'POP ROCK', 118, 246000, 'CALM', 'SPOTIFY', '0fQuzbQNLcD1ofo7B2NcFI', 'spotify:track:0fQuzbQNLcD1ofo7B2NcFI', 'https://open.spotify.com/track/0fQuzbQNLcD1ofo7B2NcFI'),
    ('A Sky Full of Stars', 'Coldplay', 'Ghost Stories', 'https://i.scdn.co/image/ab67616d0000b273e5a95573f1b91234630fd2cf', 'POP ROCK', 124, 267866, 'STEADY', 'SPOTIFY', '0FDzzruyVECATHXKHFs9eJ', 'spotify:track:0FDzzruyVECATHXKHFs9eJ', 'https://open.spotify.com/track/0FDzzruyVECATHXKHFs9eJ'),
    ('Paradise', 'Coldplay', 'Mylo Xyloto', 'https://i.scdn.co/image/ab67616d0000b273de0cd11d7b31c3bd1fd5983d', 'POP ROCK', 110, 278719, 'STEADY', 'SPOTIFY', '6nek1Nin9q48AVZcWs9e9D', 'spotify:track:6nek1Nin9q48AVZcWs9e9D', 'https://open.spotify.com/track/6nek1Nin9q48AVZcWs9e9D'),
    ('Clocks', 'Coldplay', 'A Rush of Blood to the Head', 'https://i.scdn.co/image/ab67616d0000b273de09e02aa7febf30b7c02d82', 'ALTERNATIVE ROCK', 108, 307879, 'STEADY', 'SPOTIFY', '0BCPKOYdS2jbQ8iyB56Zns', 'spotify:track:0BCPKOYdS2jbQ8iyB56Zns', 'https://open.spotify.com/track/0BCPKOYdS2jbQ8iyB56Zns'),
    ('Speed of Sound', 'Coldplay', 'X&Y', 'https://i.scdn.co/image/ab67616d0000b2734e0362c225863f6ae2432651', 'ALTERNATIVE ROCK', 118, 287906, 'STEADY', 'SPOTIFY', '7clUVcSOtkNWa58Gw5RfD4', 'spotify:track:7clUVcSOtkNWa58Gw5RfD4', 'https://open.spotify.com/track/7clUVcSOtkNWa58Gw5RfD4'),
    ('Flames', 'David Guetta, Sia', 'Flames', 'https://i.scdn.co/image/ab67616d0000b273d0e5bf44bdbe8a824e921eae', 'DANCE POP', 124, 195000, 'STEADY', 'SPOTIFY', '33IOhptvC2Qoy2UhjiHXLV', 'spotify:track:33IOhptvC2Qoy2UhjiHXLV', 'https://open.spotify.com/track/33IOhptvC2Qoy2UhjiHXLV'),
    ('Attention', 'Charlie Puth', 'Voicenotes', 'https://i.scdn.co/image/ab67616d0000b273897f73256b9128a9d70eaf66', 'POP FUNK', 100, 208786, 'STEADY', 'SPOTIFY', '5cF0dROlMOK5uNZtivgu50', 'spotify:track:5cF0dROlMOK5uNZtivgu50', 'https://open.spotify.com/track/5cF0dROlMOK5uNZtivgu50'),
    ('Pray For Me (with Kendrick Lamar)', 'The Weeknd, Kendrick Lamar', 'Black Panther The Album Music From And Inspired By', 'https://i.scdn.co/image/ab67616d0000b273c027ad28821777b00dcaa888', 'R&B POP', 101, 211440, 'STEADY', 'SPOTIFY', '77UjLW8j5UAGAGVGhR5oUK', 'spotify:track:77UjLW8j5UAGAGVGhR5oUK', 'https://open.spotify.com/track/77UjLW8j5UAGAGVGhR5oUK'),
    ('I Feel It Coming', 'The Weeknd, Daft Punk', 'Starboy', 'https://i.scdn.co/image/ab67616d0000b2734718e2b124f79258be7bc452', 'R&B POP', 93, 269186, 'CALM', 'SPOTIFY', '3dhjNA0jGA8vHBQ1VdD6vV', 'spotify:track:3dhjNA0jGA8vHBQ1VdD6vV', 'https://open.spotify.com/track/3dhjNA0jGA8vHBQ1VdD6vV'),
    ('Can''t Feel My Face', 'The Weeknd', 'Beauty Behind The Madness', 'https://i.scdn.co/image/ab67616d0000b2737fcead687e99583072cc217b', 'R&B POP', 108, 213520, 'STEADY', 'SPOTIFY', '22VdIZQfgXJea34mQxlt81', 'spotify:track:22VdIZQfgXJea34mQxlt81', 'https://open.spotify.com/track/22VdIZQfgXJea34mQxlt81'),
    ('Runaway', 'Ed Sheeran', 'x (Deluxe Edition)', 'https://i.scdn.co/image/ab67616d0000b27313b3e37318a0c247b550bccd', 'POP', 94, 205133, 'CALM', 'SPOTIFY', '2VmA7bm5ECiYZfw7x4Nzku', 'spotify:track:2VmA7bm5ECiYZfw7x4Nzku', 'https://open.spotify.com/track/2VmA7bm5ECiYZfw7x4Nzku'),
    ('I Like Me Better', 'LAUV', 'I Like Me Better', 'https://i.scdn.co/image/ab67616d0000b273c2f8018c984a8acb592bc7d8', 'POP', 92, 197436, 'CALM', 'SPOTIFY', '1jXfVzMk8cSbvAXMML4Y4f', 'spotify:track:1jXfVzMk8cSbvAXMML4Y4f', 'https://open.spotify.com/track/1jXfVzMk8cSbvAXMML4Y4f')
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    artist = VALUES(artist),
    album_name = VALUES(album_name),
    image_url = VALUES(image_url),
    genre = VALUES(genre),
    bpm = VALUES(bpm),
    duration_ms = VALUES(duration_ms),
    music_energy_level = VALUES(music_energy_level),
    spotify_uri = VALUES(spotify_uri),
    spotify_external_url = VALUES(spotify_external_url);

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
    'Zone 2 데모',
    'Spotify Zone 2 러닝을 위한 잔잔한 팝 플레이리스트',
    'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1
    FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title IN ('Zone 2 러닝', 'Zone 2 데모')
);

INSERT IGNORE INTO playlist_items (playlist_id, music_id, order_no)
SELECT
    p.id,
    m.id,
    seed.order_no
FROM playlists p
INNER JOIN (
    SELECT '6Qs4SXO9dwPj5GKvVOv8Ki' AS spotify_track_id, 1 AS order_no
    UNION ALL SELECT '1mXVgsBdtIVeCLJnSnmtdV', 2
    UNION ALL SELECT '2QZ7WLBE8h2y1Y5Fb8RYbH', 3
    UNION ALL SELECT '4m0q0xQ2BNl9SCAGKyfiGZ', 4
    UNION ALL SELECT '73jVPicY2G9YHmzgjk69ae', 5
    UNION ALL SELECT '3AJwUDP919kvQ9QcozQPxg', 6
    UNION ALL SELECT '6f49kbOuQSOsStBpyGvQfA', 7
    UNION ALL SELECT '31L9yLXSj6LpCFupyMV6CR', 8
    UNION ALL SELECT '0fQuzbQNLcD1ofo7B2NcFI', 9
    UNION ALL SELECT '0FDzzruyVECATHXKHFs9eJ', 10
    UNION ALL SELECT '6nek1Nin9q48AVZcWs9e9D', 11
    UNION ALL SELECT '0BCPKOYdS2jbQ8iyB56Zns', 12
    UNION ALL SELECT '7clUVcSOtkNWa58Gw5RfD4', 13
    UNION ALL SELECT '33IOhptvC2Qoy2UhjiHXLV', 14
    UNION ALL SELECT '5cF0dROlMOK5uNZtivgu50', 15
    UNION ALL SELECT '77UjLW8j5UAGAGVGhR5oUK', 16
    UNION ALL SELECT '3dhjNA0jGA8vHBQ1VdD6vV', 17
    UNION ALL SELECT '22VdIZQfgXJea34mQxlt81', 18
    UNION ALL SELECT '2VmA7bm5ECiYZfw7x4Nzku', 19
    UNION ALL SELECT '1jXfVzMk8cSbvAXMML4Y4f', 20
) seed
INNER JOIN music m
    ON m.spotify_track_id = seed.spotify_track_id
WHERE p.playlist_type = 'OFFICIAL'
  AND p.title = 'Zone 2 데모'
  AND p.deleted_at IS NULL;

-- ---- END Zone 2 running demo playlist ----


-- ---- BEGIN demo accounts and running data ----

-- SSAFIT PlayList 로컬 개발용 더미 데이터
-- schema.sql 실행 후 사용한다.
-- 비밀번호 해시는 화면/DB 확인 전용 값이며 실제 로그인 비밀번호로 사용하지 않는다.


INSERT IGNORE INTO members (
    login_id,
    password_hash,
    email,
    nickname,
    status
) VALUES
    ('runner_low', 'pbkdf2_sha256$210000$YrkwRlujBFDdMgZbhzTRPw==$AF7GbWwvORg9FXpuct/hqK1n0F8NxusFdU2fZ1YG5ik=', 'runner.low@example.test', '저강도 러너', 'ACTIVE'),
    ('runner_high', 'pbkdf2_sha256$210000$YrkwRlujBFDdMgZbhzTRPw==$AF7GbWwvORg9FXpuct/hqK1n0F8NxusFdU2fZ1YG5ik=', 'runner.high@example.test', '고강도 러너', 'ACTIVE'),
    ('runner_interval', 'pbkdf2_sha256$210000$YrkwRlujBFDdMgZbhzTRPw==$AF7GbWwvORg9FXpuct/hqK1n0F8NxusFdU2fZ1YG5ik=', 'runner.interval@example.test', '인터벌 러너', 'ACTIVE'),
    ('admin_candidate', 'pbkdf2_sha256$210000$YrkwRlujBFDdMgZbhzTRPw==$AF7GbWwvORg9FXpuct/hqK1n0F8NxusFdU2fZ1YG5ik=', 'admin@example.test', '관리자 후보', 'ACTIVE');

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

-- ---- END demo accounts and running data ----

