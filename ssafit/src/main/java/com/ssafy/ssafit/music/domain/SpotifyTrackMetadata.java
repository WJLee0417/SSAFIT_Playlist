package com.ssafy.ssafit.music.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
    private Integer durationMs;
    private Integer musicBpm;
    private MusicEnergyLevel musicEnergyLevel;
    private String genre;

    public SpotifyTrackMetadata(
            String spotifyTrackId,
            String spotifyUri,
            String spotifyExternalUrl,
            String title,
            String artist,
            String albumName,
            String imageUrl,
            Integer bpm,
            String genre) {
        this(
                spotifyTrackId,
                spotifyUri,
                spotifyExternalUrl,
                title,
                artist,
                albumName,
                imageUrl,
                null,
                bpm,
                null,
                genre);
    }

    @JsonIgnore
    @Deprecated
    public Integer getBpm() {
        return musicBpm;
    }

    @Deprecated
    public void setBpm(Integer bpm) {
        this.musicBpm = bpm;
    }
}
