import { apiClient } from "./client";

export function getAddOns() {
  return apiClient.get("/addons").then((r) => r.data);
}

/** @param {import("@/types/dto").AddOn} payload */
export function createAddOn(payload) {
  return apiClient.post("/addons", payload).then((r) => r.data);
}

export function getSubscriptionAddOns(subscriptionId) {
  return apiClient.get(`/addons/subscription/${subscriptionId}`).then((r) => r.data);
}

export function attachAddOn(subscriptionId, addOnId, unitsIncluded) {
  return apiClient
    .post("/addons/attach", null, { params: { subscriptionId, addOnId, unitsIncluded } })
    .then((r) => r.data);
}

export function logAddOnUsage(subscriptionId, addOnId, units) {
  return apiClient
    .post("/addons/usage", null, { params: { subscriptionId, addOnId, units } })
    .then((r) => r.data);
}
