import { apiClient } from "./http";

export const musicApi = {
  getList(params = {}) {
    return apiClient.get("/music", { params });
  },
  getDetail(musicId) {
    return apiClient.get(`/music/${musicId}`);
  },
  importSpotifyTrack(payload) {
    return apiClient.post("/music/from-spotify", payload);
  },
};
