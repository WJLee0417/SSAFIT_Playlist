package com.ssafy.ssafit.spotify.metadata;

import com.ssafy.ssafit.spotify.domain.SpotifyTrack;

public interface TrackMetadataProvider {

    TrackMetadata resolve(SpotifyTrack track);
}
