import { apiClient } from "./client";

/** @param {import("@/types/dto").CreateSubscriptionRequest} payload */
export function createSubscription(payload) {
  return apiClient.post("/subscriptions", payload).then((r) => r.data);
}

export function getSubscription(id) {
  return apiClient.get(`/subscriptions/${id}`).then((r) => r.data);
}

export function getSubscriptionsByUser(userId) {
  return apiClient.get(`/subscriptions/user/${userId}`).then((r) => r.data);
}

export function getAllSubscriptions() {
  return apiClient.get("/subscriptions").then((r) => r.data);
}

/** @param {import("@/types/dto").ChangePlanRequestDTO} payload */
export function changePlan(payload) {
  return apiClient.put("/subscriptions/change-plan", payload).then((r) => r.data);
}

export function cancelSubscription(id) {
  return apiClient.put(`/subscriptions/${id}/cancel`).then((r) => r.data);
}

// Admin-only: exercises the real renewal/dunning scheduler logic on demand for testing
export function testRenewal(id) {
  return apiClient.post(`/subscriptions/${id}/test-renewal`).then((r) => r.data);
}

export function testRetry(id) {
  return apiClient.post(`/subscriptions/${id}/test-retry`).then((r) => r.data);
}
