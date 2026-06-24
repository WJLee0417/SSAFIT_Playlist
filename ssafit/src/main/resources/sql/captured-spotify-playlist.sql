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
