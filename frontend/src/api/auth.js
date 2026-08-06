import { apiClient } from "./client";

/** @param {import("@/types/dto").LoginRequest} payload */
export function login(payload) {
  return apiClient.post("/auth/login", payload).then((r) => r.data);
}

/** @param {import("@/types/dto").UserWriteRequest} payload */
export function register(payload) {
  return apiClient.post("/users", payload).then((r) => r.data);
}
