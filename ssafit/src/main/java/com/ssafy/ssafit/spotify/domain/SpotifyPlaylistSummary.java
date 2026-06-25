package com.ssafy.ssafit.spotify.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SpotifyPlaylistSummary {

    private String id;
    private String name;
    private String externalUrl;
    private String ownerId;
    private Boolean publicPlaylist;
    private Integer tracksTotal;
}
