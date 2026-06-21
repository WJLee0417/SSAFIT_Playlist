import axios from "axios";

export const apiClient = axios.create({
  baseURL: "http://localhost:8080/api",
  withCredentials: true,
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const body = error.response?.data;

    return Promise.reject({
      status,
      code: body?.code ?? "UNKNOWN_ERROR",
      message: body?.message ?? "요청 처리에 실패했습니다.",
      cause: error,
    });
  },
);
