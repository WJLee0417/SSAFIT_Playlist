import { apiClient } from "./http";

export const reviewApi = {
  getByMusic(musicId) {
    return apiClient.get(`/music/${musicId}/reviews`);
  },
  create(musicId, payload) {
    return apiClient.post(`/music/${musicId}/reviews`, payload);
  },
  update(reviewId, payload) {
    return apiClient.put(`/music-reviews/${reviewId}`, payload);
  },
  remove(reviewId) {
    return apiClient.delete(`/music-reviews/${reviewId}`);
  },
};
