package com.ssafy.ssafit.music.tempo;

import java.util.Optional;

/**
 * Optional provider for music tempo metadata.
 *
 * <p>The resolved value is a track's musical tempo in BPM. It must never be
 * populated from running cadence or average heart-rate BPM.</p>
 */
public interface MusicTempoProvider {

    Optional<Integer> resolveMusicBpm(String spotifyTrackId);
}
