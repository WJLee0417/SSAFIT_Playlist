-- SSAFIT PlayList 기준 데이터
-- schema.sql 실행 후 MySQL Workbench 또는 CLI에서 직접 실행한다.
-- 애플리케이션 자동 초기화 대상이 아니며, 반복 실행해도 중복되지 않도록 작성한다.
--
-- 기본 플레이리스트는 Spotify Search API로 실제 검색 확인한 트랙 ID/URI 기반 샘플이다.
-- Spotify Search API는 BPM을 제공하지 않으므로 bpm/music_energy_level은 러닝 분류용 메타데이터로 관리한다.

SET NAMES utf8mb4;

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
