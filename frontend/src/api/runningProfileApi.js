import { apiClient } from "./http";

export const runningProfileApi = {
  getMine() {
    return apiClient.get("/members/me/running-profile");
  },
  save(payload) {
    return apiClient.put("/members/me/running-profile", payload);
  },
};
