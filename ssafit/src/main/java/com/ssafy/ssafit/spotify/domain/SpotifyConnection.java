package com.ssafy.ssafit.spotify.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpotifyConnection {

    private Long id;
    private Long memberId;
    private String spotifyUserId;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
