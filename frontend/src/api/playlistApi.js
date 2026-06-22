import { apiClient } from "./http";

function cleanParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => (
      value !== undefined && value !== null && value !== ""
    )),
  );
}

export const playlistApi = {
  getList(params = {}) {
    return apiClient.get("/playlists", { params: cleanParams(params) });
  },
  getDetail(playlistId) {
    return apiClient.get(`/playlists/${playlistId}`);
  },
  create(payload) {
    return apiClient.post("/playlists", payload);
  },
  update(playlistId, payload) {
    return apiClient.put(`/playlists/${playlistId}`, payload);
  },
  remove(playlistId) {
    return apiClient.delete(`/playlists/${playlistId}`);
  },
  getItems(playlistId) {
    return apiClient.get(`/playlists/${playlistId}/items`);
  },
  addItem(playlistId, payload) {
    return apiClient.post(`/playlists/${playlistId}/items`, payload);
  },
  updateItemOrder(itemId, payload) {
    return apiClient.patch(`/playlist-items/${itemId}/order`, payload);
  },
  removeItem(itemId) {
    return apiClient.delete(`/playlist-items/${itemId}`);
  },
  exportToSpotify(playlistId) {
    return apiClient.post(`/playlists/${playlistId}/export/spotify`);
  },
};
