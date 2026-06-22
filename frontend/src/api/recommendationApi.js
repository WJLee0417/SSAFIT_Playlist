import { apiClient } from "./http";

export const recommendationApi = {
  recommend(params) {
    return apiClient.get("/recommendations", { params });
  },
  getMyLogs() {
    return apiClient.get("/recommendations/logs/me");
  },
  save(logId, payload) {
    return apiClient.post(`/recommendations/${logId}/save`, payload);
  },
};
