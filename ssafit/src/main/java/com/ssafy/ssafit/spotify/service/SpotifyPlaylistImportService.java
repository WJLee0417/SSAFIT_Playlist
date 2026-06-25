package com.ssafy.ssafit.spotify.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ssafy.ssafit.common.exception.CustomException;
import com.ssafy.ssafit.common.exception.ErrorCode;
import com.ssafy.ssafit.music.domain.Music;
import com.ssafy.ssafit.playlist.domain.Playlist;
import com.ssafy.ssafit.playlist.domain.PlaylistItem;
import com.ssafy.ssafit.playlist.domain.PlaylistVisibility;
import com.ssafy.ssafit.playlist.service.PlaylistItemService;
import com.ssafy.ssafit.playlist.service.PlaylistService;
import com.ssafy.ssafit.spotify.client.SpotifyApiClient;
import com.ssafy.ssafit.spotify.domain.SpotifyPlaylistSummary;
import com.ssafy.ssafit.spotify.domain.SpotifyTrack;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpotifyPlaylistImportService {

    private static final int MAX_IMPORT_TRACKS = 50;

    private final SpotifyOAuthService spotifyOAuthService;
    private final SpotifyApiClient spotifyApiClient;
    private final SpotifyService spotifyService;
    private final PlaylistService playlistService;
    private final PlaylistItemService playlistItemService;

    @Transactional
    public ImportResult importPlaylist(
            Long loginMemberId,
            String spotifyPlaylistId,
            String title) {
        validateRequest(spotifyPlaylistId);
        return spotifyOAuthService.executeWithValidConnection(
                loginMemberId,
                connection -> {
                    String accessToken = connection.getAccessToken();
                    SpotifyPlaylistSummary source = findSourcePlaylist(
                            accessToken,
                            spotifyPlaylistId.trim());
                    Playlist playlist = createPlaylist(
                            loginMemberId,
                            source,
                            title);
                    int importedCount = importTracks(
                            loginMemberId,
                            playlist.getId(),
                            accessToken,
                            source.getId());
                    return new ImportResult(playlist, importedCount);
                });
    }

    private SpotifyPlaylistSummary findSourcePlaylist(
            String accessToken,
            String spotifyPlaylistId) {
        List<SpotifyPlaylistSummary> playlists =
                spotifyApiClient.getCurrentUserPlaylists(accessToken, 0, 50);
        return playlists.stream()
                .filter(playlist -> spotifyPlaylistId.equals(playlist.getId()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.SPOTIFY_API_FAILED));
    }

    private Playlist createPlaylist(
            Long loginMemberId,
            SpotifyPlaylistSummary source,
            String title) {
        Playlist playlist = new Playlist();
        playlist.setTitle(StringUtils.hasText(title) ? title.trim() : source.getName());
        playlist.setDescription("Spotify에서 가져온 플레이리스트입니다.");
        playlist.setVisibility(PlaylistVisibility.PRIVATE);
        return playlistService.create(loginMemberId, playlist);
    }

    private int importTracks(
            Long loginMemberId,
            Long playlistId,
            String accessToken,
            String spotifyPlaylistId) {
        List<SpotifyTrack> tracks = spotifyApiClient.getPlaylistTracks(
                accessToken,
                spotifyPlaylistId,
                0,
                MAX_IMPORT_TRACKS);
        Set<String> seenTrackIds = new LinkedHashSet<>();
        int orderNo = 1;
        for (SpotifyTrack track : tracks) {
            if (track == null || !StringUtils.hasText(track.getId())
                    || !seenTrackIds.add(track.getId())) {
                continue;
            }
            Music music = spotifyService.saveSpotifyTrack(accessToken, track);
            PlaylistItem item = new PlaylistItem();
            item.setPlaylistId(playlistId);
            item.setMusicId(music.getId());
            item.setOrderNo(orderNo++);
            playlistItemService.add(loginMemberId, item);
        }
        return orderNo - 1;
    }

    private void validateRequest(String spotifyPlaylistId) {
        if (!StringUtils.hasText(spotifyPlaylistId)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    public record ImportResult(
            Playlist playlist,
            int importedCount) {
    }
}
