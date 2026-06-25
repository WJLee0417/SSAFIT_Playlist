import { apiClient } from "./http";

let currentMember;
let checked = false;

export const authApi = {
  async login(credentials) {
    const response = await apiClient.post("/auth/login", credentials);
    currentMember = response?.member;
    checked = Boolean(currentMember);
    return response;
  },
  logout() {
    currentMember = undefined;
    checked = false;
    return apiClient.post("/auth/logout");
  },
  async getSession({ force = false } = {}) {
    if (checked && !force) {
      return currentMember;
    }
    const member = await apiClient.get("/members/me");
    currentMember = member;
    checked = true;
    return member;
  },
  clearSessionCache() {
    currentMember = undefined;
    checked = false;
  },
};
