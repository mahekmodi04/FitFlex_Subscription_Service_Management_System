import { apiClient } from "./client";

export function getPayment(id) {
  return apiClient.get(`/payments/${id}`).then((r) => r.data);
}

export function getAllPayments() {
  return apiClient.get("/payments").then((r) => r.data);
}

export function getPaymentsBySubscription(subscriptionId) {
  return apiClient.get(`/payments/subscription/${subscriptionId}`).then((r) => r.data);
}
