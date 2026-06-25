package com.ssafy.ssafit.music.tempo;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class NoOpMusicTempoProvider implements MusicTempoProvider {

    @Override
    public Optional<Integer> resolveMusicBpm(String spotifyTrackId) {
        return Optional.empty();
    }
}
