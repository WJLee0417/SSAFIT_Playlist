import { apiClient } from "./http";

const SPOTIFY_AUTHORIZE_ORIGIN = "https://accounts.spotify.com";

function cleanSearchParams(params) {
  return {
    query: params.query.trim(),
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
    bpm: track.bpm ?? null,
    genre: track.genre || null,
  };
}

function safeAuthorizationUrl(rawUrl) {
  const url = new URL(rawUrl);
  if (
    url.origin !== SPOTIFY_AUTHORIZE_ORIGIN
    || url.pathname !== "/authorize"
  ) {
    throw new Error("허용되지 않은 Spotify 인증 주소입니다.");
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
  getAuthorization() {
    return apiClient.get("/spotify/oauth/authorize");
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
  async startAuthorization() {
    // 실제 Backend는 302가 아니라 authorizationUrl JSON을 반환한다.
    const authorization = await this.getAuthorization();
    window.location.assign(safeAuthorizationUrl(authorization.authorizationUrl));
  },
};
