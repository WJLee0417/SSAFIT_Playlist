import { apiClient } from "./http";

function cleanRecommendationParams(condition) {
  return {
    intensity: condition.intensity,
    ...(condition.targetDistanceKm != null
      ? { targetDistanceKm: condition.targetDistanceKm }
      : {}),
    ...(condition.targetCadenceSpm != null
      ? { targetCadenceSpm: condition.targetCadenceSpm }
      : {}),
    ...(condition.runningIntensityGoal
      ? { runningIntensityGoal: condition.runningIntensityGoal }
      : {}),
    ...(condition.genre?.trim() ? { genre: condition.genre.trim() } : {}),
    ...(condition.size != null ? { size: condition.size } : {}),
    ...(condition.limit != null ? { limit: condition.limit } : {}),
    ...(condition.page != null ? { page: condition.page } : {}),
  };
}

function saveRequest(payload) {
  return {
    title: payload.title?.trim() || null,
    visibility: payload.visibility || null,
  };
}

export const recommendationApi = {
  recommend(condition) {
    return apiClient.get("/recommendations", {
      params: cleanRecommendationParams(condition),
    });
  },
  getMyLogs() {
    return apiClient.get("/recommendations/logs/me");
  },
  getResult(logId) {
    return apiClient.get(`/recommendations/${logId}`);
  },
  deleteLog(logId) {
    return apiClient.delete(`/recommendations/${logId}`);
  },
  save(logId, payload) {
    return apiClient.post(
      `/recommendations/${logId}/save`,
      saveRequest(payload),
    );
  },
};
