-- 부분 성공한 Spotify 플레이리스트와 성공 chunk를 보존하기 위한 상태 확장.

ALTER TABLE playlists
    DROP CHECK ck_playlists_export_status,
    DROP CHECK ck_playlists_export_result,
    DROP CHECK ck_playlists_export_error;

ALTER TABLE playlists
    ADD CONSTRAINT ck_playlists_export_status
        CHECK (
            spotify_export_status IN (
                'NOT_EXPORTED',
                'PENDING',
                'SUCCESS',
                'PARTIAL',
                'FAILED'
            )
        ),
    ADD CONSTRAINT ck_playlists_export_result
        CHECK (
            (
                spotify_export_status IN ('SUCCESS', 'PARTIAL')
                AND spotify_playlist_id IS NOT NULL
                AND spotify_playlist_url IS NOT NULL
                AND spotify_exported_at IS NOT NULL
            )
            OR (
                spotify_export_status NOT IN ('SUCCESS', 'PARTIAL')
                AND spotify_exported_at IS NULL
            )
        ),
    ADD CONSTRAINT ck_playlists_export_error
        CHECK (
            (
                spotify_export_status IN ('FAILED', 'PARTIAL')
                AND spotify_export_error_code IS NOT NULL
            )
            OR (
                spotify_export_status NOT IN ('FAILED', 'PARTIAL')
                AND spotify_export_error_code IS NULL
            )
        );
