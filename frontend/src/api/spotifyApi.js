import { apiClient } from "./http";
import { isSafeSpotifyReturnPath } from "../utils/oauthReturn";

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

function oauthStartUrl(returnTo) {
  const baseUrl = String(apiClient.defaults.baseURL || "").replace(/\/$/, "");
  const url = new URL(`${baseUrl}/spotify/oauth/authorize`, window.location.origin);
  if (isSafeSpotifyReturnPath(returnTo)) {
    url.searchParams.set("returnTo", returnTo);
  }
  return url.toString();
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
  getMyPlaylists(params = {}) {
    return apiClient.get("/spotify/me/playlists", { params });
  },
  importPlaylist(payload) {
    return apiClient.post("/spotify/playlists/import", payload);
  },
  disconnect() {
    return apiClient.delete("/spotify/me/connection");
  },
  exportPlaylist(playlistId) {
    return apiClient.post(`/playlists/${playlistId}/export/spotify`);
  },
  startAuthorization(returnTo = `${window.location.pathname}${window.location.search}`) {
    // Vue Router가 아닌 백엔드 OAuth 시작 Endpoint로 브라우저 전체 이동한다.
    window.location.assign(oauthStartUrl(returnTo));
  },
};
