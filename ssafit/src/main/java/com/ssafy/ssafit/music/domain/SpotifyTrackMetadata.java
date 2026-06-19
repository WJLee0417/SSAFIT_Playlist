package com.ssafy.ssafit.music.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpotifyTrackMetadata {

    private String spotifyTrackId;
    private String spotifyUri;
    private String spotifyExternalUrl;
    private String title;
    private String artist;
    private String albumName;
    private String imageUrl;
    private Integer bpm;
    private String genre;
}
