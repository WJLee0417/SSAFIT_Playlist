import { apiClient } from "./http";

export const spotifyApi = {
  searchTracks(params) {
    return apiClient.get("/spotify/tracks/search", { params });
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
  async startAuthorization() {
    const authorization = await this.getAuthorization();
    window.location.assign(authorization.authorizationUrl);
  },
};
