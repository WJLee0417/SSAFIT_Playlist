package com.ssafy.ssafit.playlist.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistExportTrack {

    private Long musicId;
    private String spotifyTrackId;
    private String spotifyUri;

    public boolean exportable() {
        return spotifyTrackId != null
                && !spotifyTrackId.isBlank()
                && spotifyUri != null
                && !spotifyUri.isBlank();
    }
}
