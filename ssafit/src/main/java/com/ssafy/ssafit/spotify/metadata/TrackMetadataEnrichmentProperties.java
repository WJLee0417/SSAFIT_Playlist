package com.ssafy.ssafit.spotify.metadata;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spotify.metadata-enrichment")
public class TrackMetadataEnrichmentProperties {

    private boolean enabled = true;
    private Lastfm lastfm = new Lastfm();
    private Getsong getsong = new Getsong();

    @Getter
    @Setter
    public static class Lastfm {
        private String apiKey;
        private String apiBaseUrl = "https://ws.audioscrobbler.com/2.0/";
    }

    @Getter
    @Setter
    public static class Getsong {
        private String apiKey;
        private String apiBaseUrl = "https://api.getsong.co";
    }
}
