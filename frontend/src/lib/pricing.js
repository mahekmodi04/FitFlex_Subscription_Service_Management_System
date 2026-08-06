import { CouponType } from "@/types/enums";

// Mirrors SubscriptionService.calculateFinalPrice on the backend, for live preview only —
// the actual charge is always authoritative from the server response.
export function applyCouponDiscount(price, coupon) {
  if (!coupon) return price;
  let result = price;
  if (coupon.type === CouponType.PERCENTAGE) {
    result = result - (result * Number(coupon.discountPercentage)) / 100;
  } else if (coupon.type === CouponType.AMOUNT) {
    result = result - Number(coupon.discountAmount);
  } else if (coupon.type === CouponType.BOTH) {
    result = result - (result * Number(coupon.discountPercentage)) / 100 - Number(coupon.discountAmount);
  }
  return Math.max(0, result);
}

export function getCouponValidationError(coupon) {
  if (!coupon) return null;
  if (!coupon.active) return "This coupon is no longer active.";
  if (new Date(coupon.expiryDate) < new Date()) return "This coupon has expired.";
  if (coupon.usedCount >= coupon.usageLimit) return "This coupon has hit its usage limit.";
  return null;
}
