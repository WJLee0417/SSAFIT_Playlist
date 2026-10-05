package com.ssafy.ssafit.spotify.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ssafy.ssafit.mapper.DatabaseIntegrationTestSupport;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.spotify.domain.SpotifyConnection;

class SpotifyConnectionMapperIntegrationTest
        extends DatabaseIntegrationTestSupport {

    @Autowired
    private SpotifyConnectionMapper spotifyConnectionMapper;

    @Test
    void spotifyConnectionMapperInsertsAndRefreshesToken() {
        Member member = insertMember("spotify_connection_member");
        SpotifyConnection connection = new SpotifyConnection();
        connection.setMemberId(member.getId());
        connection.setSpotifyUserId("spotify-user");
        connection.setAccessToken("first-access");
        connection.setRefreshToken("first-refresh");
        connection.setTokenExpiresAt(java.time.LocalDateTime.now().plusHours(1));

        spotifyConnectionMapper.insert(connection);
        SpotifyConnection selected = spotifyConnectionMapper
                .selectByMemberId(member.getId())
                .orElseThrow();
        assertThat(selected.getSpotifyUserId()).isEqualTo("spotify-user");
        assertThat(selected.getAccessToken()).isEqualTo("first-access");

        selected.setAccessToken("refreshed-access");
        selected.setRefreshToken("refreshed-refresh");
        selected.setTokenExpiresAt(java.time.LocalDateTime.now().plusHours(2));
        spotifyConnectionMapper.updateToken(selected);

        assertThat(spotifyConnectionMapper.selectByMemberId(member.getId())
                .orElseThrow()
                .getAccessToken()).isEqualTo("refreshed-access");
    }
}
