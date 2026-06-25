package com.ssafy.ssafit.spotify.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpotifyToken {

    private String accessToken;
    private String refreshToken;
    private Integer expiresInSeconds;
}
