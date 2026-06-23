import { apiClient } from "./http";

function cleanParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => (
      value !== undefined && value !== null && value !== ""
    )),
  );
}

export const musicApi = {
  getList(params = {}) {
    return apiClient.get("/music", { params: cleanParams(params) });
  },
  getDetail(musicId) {
    return apiClient.get(`/music/${musicId}`);
  },
  importSpotifyTrack(payload) {
    return apiClient.post("/music/from-spotify", payload);
  },
  createInternal(payload) {
    return apiClient.post("/music/internal", payload);
  },
  updateInternal(musicId, payload) {
    return apiClient.put(`/music/internal/${musicId}`, payload);
  },
};
