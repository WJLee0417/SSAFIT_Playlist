package com.ssafy.ssafit.music.domain;

import java.time.LocalDateTime;

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
    private String genre;
    private Integer bpm;
    private MusicSourceType sourceType;
    private String spotifyTrackId;
    private String spotifyUri;
    private String spotifyExternalUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
