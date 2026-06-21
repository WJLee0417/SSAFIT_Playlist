package com.ssafy.ssafit.spotify.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spotify")
public class SpotifyProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authorizationBaseUrl = "https://accounts.spotify.com";
    private String apiBaseUrl = "https://api.spotify.com/v1";
    private List<String> scopes = new ArrayList<>();
}
