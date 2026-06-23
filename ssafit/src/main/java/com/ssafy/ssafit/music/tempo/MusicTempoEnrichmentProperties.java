package com.ssafy.ssafit.music.tempo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spotify.audio-feature-enrichment")
public class MusicTempoEnrichmentProperties {

    private boolean enabled;
}
