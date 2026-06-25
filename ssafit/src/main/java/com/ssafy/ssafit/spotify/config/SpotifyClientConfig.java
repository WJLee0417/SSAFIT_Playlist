package com.ssafy.ssafit.spotify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SpotifyClientConfig {

    @Bean
    RestClient.Builder spotifyRestClientBuilder() {
        return RestClient.builder();
    }
}
