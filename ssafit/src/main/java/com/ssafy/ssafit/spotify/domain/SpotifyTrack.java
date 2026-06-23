package com.ssafy.ssafit.spotify.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpotifyTrack {

    private String id;
    private String uri;
    private String externalUrl;
    private String title;
    private String artist;
    private String albumName;
    private String imageUrl;
    private Integer durationMs;

    public SpotifyTrack(
            String id,
            String uri,
            String externalUrl,
            String title,
            String artist,
            String albumName,
            String imageUrl) {
        this(id, uri, externalUrl, title, artist, albumName, imageUrl, null);
    }
}
