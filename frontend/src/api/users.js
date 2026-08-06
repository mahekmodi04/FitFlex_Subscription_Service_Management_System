import { apiClient } from "./client";

export function getUsers() {
  return apiClient.get("/users").then((r) => r.data);
}

export function getUser(id) {
  return apiClient.get(`/users/${id}`).then((r) => r.data);
}

/** @param {import("@/types/dto").UserWriteRequest} payload */
export function updateUser(id, payload) {
  return apiClient.put(`/users/${id}`, payload).then((r) => r.data);
}

export function deleteUser(id) {
  return apiClient.delete(`/users/${id}`).then((r) => r.data);
}
