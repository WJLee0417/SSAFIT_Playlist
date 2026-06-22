import { apiClient } from "./http";

export const runningRecordApi = {
  getMine() {
    return apiClient.get("/running-records/me");
  },
  getDetail(runningRecordId) {
    return apiClient.get(`/running-records/${runningRecordId}`);
  },
  create(payload) {
    return apiClient.post("/running-records", payload);
  },
  update(runningRecordId, payload) {
    return apiClient.put(`/running-records/${runningRecordId}`, payload);
  },
  remove(runningRecordId) {
    return apiClient.delete(`/running-records/${runningRecordId}`);
  },
};
