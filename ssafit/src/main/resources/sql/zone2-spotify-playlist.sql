-- Spotify Search API matched from a Zone 2 running demo playlist screenshot.
-- BPM, genre, and music_energy_level are inferred dummy metadata because Spotify Search API does not provide them.
-- The inferred BPM range is intentionally low so the playlist is grouped as low-intensity running.

UPDATE playlists
SET description = 'Spotify Search API로 확인한 캡처 기반 저강도 Zone 2 러닝 데모 플레이리스트'
WHERE playlist_type = 'OFFICIAL'
  AND title = 'Zone 2 러닝'
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
    'Zone 2 러닝',
    'Spotify Search API로 확인한 캡처 기반 저강도 Zone 2 러닝 데모 플레이리스트',
    'NOT_EXPORTED'
WHERE NOT EXISTS (
    SELECT 1
    FROM playlists
    WHERE playlist_type = 'OFFICIAL'
      AND title = 'Zone 2 러닝'
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
  AND p.title = 'Zone 2 러닝'
  AND p.deleted_at IS NULL;
