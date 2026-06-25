import { apiClient } from "./http";

export const playlistSaveApi = {
  getMine() {
    return apiClient.get("/playlist-saves/me");
  },
  save(playlistId) {
    return apiClient.post(`/playlist-saves/${playlistId}`);
  },
  cancel(playlistId) {
    return apiClient.delete(`/playlist-saves/${playlistId}`);
  },
};
