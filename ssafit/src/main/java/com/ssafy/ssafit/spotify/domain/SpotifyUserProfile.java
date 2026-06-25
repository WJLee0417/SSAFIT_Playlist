package com.ssafy.ssafit.spotify.domain;

import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpotifyUserProfile {

    private String accountId;
    private String id;
    private String displayName;

    public SpotifyUserProfile(String id) {
        this(null, id, null);
    }

    public String stableIdentifier() {
        return StringUtils.hasText(accountId) ? accountId : id;
    }
}
