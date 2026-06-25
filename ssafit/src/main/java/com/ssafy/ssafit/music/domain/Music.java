package com.ssafy.ssafit.music.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Music {

    private Long id;
    private String title;
    private String artist;
    private String albumName;
    private String imageUrl;
    private String genre;
    private Integer musicBpm;
    private Integer durationMs;
    private MusicEnergyLevel musicEnergyLevel;
    private MusicSourceType sourceType;
    private String spotifyTrackId;
    private String spotifyUri;
    private String spotifyExternalUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Legacy Java accessor retained while callers migrate to musicBpm.
     */
    @JsonIgnore
    @Deprecated
    public Integer getBpm() {
        return musicBpm;
    }

    /**
     * Legacy Java accessor retained while callers migrate to musicBpm.
     */
    @Deprecated
    public void setBpm(Integer bpm) {
        this.musicBpm = bpm;
    }
}
