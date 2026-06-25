import { apiClient } from "./http";

export const runningSessionApi = {
  getList() {
    return apiClient.get("/running-sessions");
  },
  getDetail(runningSessionId) {
    return apiClient.get(`/running-sessions/${runningSessionId}`);
  },
  create(payload) {
    return apiClient.post("/running-sessions", payload);
  },
  update(runningSessionId, payload) {
    return apiClient.put(`/running-sessions/${runningSessionId}`, payload);
  },
  changeStatus(runningSessionId, status) {
    return apiClient.patch(`/running-sessions/${runningSessionId}/status`, { status });
  },
  cancel(runningSessionId) {
    return apiClient.post(`/running-sessions/${runningSessionId}/cancel`);
  },
  getParticipants(runningSessionId) {
    return apiClient.get(`/running-sessions/${runningSessionId}/participants`);
  },
  join(runningSessionId) {
    return apiClient.post(`/running-sessions/${runningSessionId}/participants/me`);
  },
  leave(runningSessionId) {
    return apiClient.delete(`/running-sessions/${runningSessionId}/participants/me`);
  },
};
