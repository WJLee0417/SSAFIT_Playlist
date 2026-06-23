import { apiClient } from "./http";

function cleanSearchParams(params) {
  return {
    query: params.query.trim(),
    offset: Math.min(Math.max(Number(params.offset) || 0, 0), 1000),
    limit: Math.min(Math.max(Number(params.limit) || 10, 1), 10),
  };
}

function importRequest(track) {
  return {
    spotifyTrackId: track.spotifyTrackId,
    spotifyUri: track.spotifyUri,
    spotifyExternalUrl: track.spotifyExternalUrl || null,
    title: track.title,
    artist: track.artist,
    albumName: track.albumName || null,
    imageUrl: track.imageUrl || null,
    durationMs: track.durationMs ?? null,
    musicBpm: track.musicBpm ?? null,
    genre: track.genre || null,
  };
}

function oauthStartUrl() {
  const baseUrl = String(apiClient.defaults.baseURL || "").replace(/\/$/, "");
  return `${baseUrl}/spotify/oauth/authorize`;
}

export const spotifyApi = {
  searchTracks(params) {
    return apiClient.get("/spotify/tracks/search", {
      params: cleanSearchParams(params),
    });
  },
  importTrack(track) {
    return apiClient.post("/music/from-spotify", importRequest(track));
  },
  getConnection() {
    return apiClient.get("/spotify/me/connection");
  },
  disconnect() {
    return apiClient.delete("/spotify/me/connection");
  },
  exportPlaylist(playlistId) {
    return apiClient.post(`/playlists/${playlistId}/export/spotify`);
  },
  startAuthorization() {
    // Vue Router가 아닌 백엔드 OAuth 시작 Endpoint로 브라우저 전체 이동한다.
    window.location.assign(oauthStartUrl());
  },
};
