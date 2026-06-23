import axios from "axios";

const DEFAULT_API_BASE_URL = "http://127.0.0.1:8080/api";

export class ApiError extends Error {
  constructor({ message, errorCode, status, cause, response }) {
    super(message);
    this.name = "ApiError";
    this.errorCode = errorCode;
    this.code = errorCode;
    this.status = status;
    this.cause = cause;
    this.response = response;
  }

  get loginRequired() {
    return this.status === 401 && this.errorCode === "AUTH_REQUIRED";
  }

  get unauthorized() {
    return this.status === 403;
  }

  get notFound() {
    return this.status === 404;
  }

  get conflict() {
    return this.status === 409;
  }

  get upstreamUnavailable() {
    return this.status === 502;
  }
}

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL,
  withCredentials: true,
  headers: {
    Accept: "application/json",
  },
});

apiClient.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (
      body &&
      typeof body === "object" &&
      Object.prototype.hasOwnProperty.call(body, "data")
    ) {
      return body.data;
    }
    return body;
  },
  (error) => {
    const status = error.response?.status;
    const body = error.response?.data;

    return Promise.reject(new ApiError({
      status,
      errorCode: body?.errorCode ?? body?.code ?? "UNKNOWN_ERROR",
      message: body?.message ?? "요청을 처리하지 못했습니다.",
      cause: error,
      response: body,
    }));
  },
);

export const isApiError = (error) => error instanceof ApiError;
