package com.ssafy.ssafit.spotify.metadata;

public record TrackMetadata(Integer musicBpm, String genre) {

    private static final TrackMetadata EMPTY = new TrackMetadata(null, null);

    public static TrackMetadata empty() {
        return EMPTY;
    }
}
