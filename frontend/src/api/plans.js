import { apiClient } from "./client";

export function getPlans() {
  return apiClient.get("/plans").then((r) => r.data);
}

export function getPlan(id) {
  return apiClient.get(`/plans/${id}`).then((r) => r.data);
}

/** @param {import("@/types/dto").Plan} payload */
export function createPlan(payload) {
  return apiClient.post("/plans", payload).then((r) => r.data);
}

export function updatePlan(id, payload) {
  return apiClient.put(`/plans/${id}`, payload).then((r) => r.data);
}

export function deletePlan(id) {
  return apiClient.delete(`/plans/${id}`).then((r) => r.data);
}
