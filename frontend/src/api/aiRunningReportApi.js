import { apiClient } from "./http";

export const aiRunningReportApi = {
  getLatest() {
    return apiClient.get("/ai/running-reports/latest");
  },
  generate({ forceRefresh = false } = {}) {
    return apiClient.post("/ai/running-reports", { forceRefresh });
  },
};
