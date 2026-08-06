import axios from "axios";
import { toast } from "sonner";

export const TOKEN_STORAGE_KEY = "fitflex_token";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let onUnauthorized = null;

export function registerUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      onUnauthorized?.();
    }
    // The backend silently drops expired/invalid JWTs instead of rejecting them
    // (JwtAuthenticationFilter swallows the parse error), so an expired token falls through
    // to Spring Security's authorization check and comes back as a 403 "Access Denied" —
    // identical to a real permission error. Since this app's UI already hides any action a
    // user's role can't perform, a real 403 essentially never happens through normal use, so
    // treat "Access Denied" while we believe we're logged in as an expired session.
    if (
      error.response?.status === 403 &&
      error.response?.data === "Access Denied" &&
      localStorage.getItem(TOKEN_STORAGE_KEY)
    ) {
      toast.error("Your session has expired. Please log in again.");
      onUnauthorized?.();
    }
    return Promise.reject(error);
  }
);

export function extractErrorMessage(error, fallback = "Something went wrong. Please try again.") {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data;
    if (typeof data === "string") return data;
    if (data?.message) return data.message;
    if (data?.error) return data.error;
    if (error.message) return error.message;
  }
  return fallback;
}
