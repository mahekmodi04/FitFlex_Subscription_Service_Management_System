import { apiClient } from "./client";

export function getCoupons() {
  return apiClient.get("/coupons").then((r) => r.data);
}

export function getCoupon(id) {
  return apiClient.get(`/coupons/${id}`).then((r) => r.data);
}

export function getCouponByCode(code) {
  return apiClient.get(`/coupons/code/${code}`).then((r) => r.data);
}

/** @param {import("@/types/dto").Coupon} payload */
export function createCoupon(payload) {
  return apiClient.post("/coupons", payload).then((r) => r.data);
}

export function updateCoupon(id, payload) {
  return apiClient.put(`/coupons/${id}`, payload).then((r) => r.data);
}

export function deleteCoupon(id) {
  return apiClient.delete(`/coupons/${id}`).then((r) => r.data);
}
