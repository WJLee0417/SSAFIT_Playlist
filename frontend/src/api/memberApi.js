import { apiClient } from "./http";

export const memberApi = {
  join(payload) {
    return apiClient.post("/members", payload);
  },
  getMe() {
    return apiClient.get("/members/me");
  },
};
