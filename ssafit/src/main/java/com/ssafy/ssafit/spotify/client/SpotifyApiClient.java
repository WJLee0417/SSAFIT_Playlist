package com.ssafy.ssafit.spotify.client;

import java.util.List;

import com.ssafy.ssafit.spotify.domain.SpotifyPlaylistResult;
import com.ssafy.ssafit.spotify.domain.SpotifyToken;
import com.ssafy.ssafit.spotify.domain.SpotifyTrack;
import com.ssafy.ssafit.spotify.domain.SpotifyUserProfile;

public interface SpotifyApiClient {

    String issueClientAccessToken();

    List<SpotifyTrack> searchTracks(
            String accessToken,
            String query,
            int offset,
            int limit);

    default List<SpotifyTrack> searchTracks(
            String accessToken,
            String query,
            int limit) {
        return searchTracks(accessToken, query, 0, limit);
    }

    SpotifyToken exchangeAuthorizationCode(String code);

    SpotifyToken refreshAccessToken(String refreshToken);

    SpotifyUserProfile getCurrentUser(String accessToken);

    SpotifyPlaylistResult createPlaylist(
            String accessToken,
            String name,
            String description,
            boolean isPublic);

    void addTracks(String accessToken, String spotifyPlaylistId, List<String> uris);
}
