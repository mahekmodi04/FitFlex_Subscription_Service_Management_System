export const SubscriptionStatus = {
  PENDING: "PENDING",
  ACTIVE: "ACTIVE",
  GRACE: "GRACE",
  CANCELLED: "CANCELLED",
  EXPIRED: "EXPIRED",
};

export const PaymentStatus = {
  SUCCESS: "SUCCESS",
  FAILED: "FAILED",
  REFUNDED: "REFUNDED",
};

export const PaymentMethod = {
  CARD: "CARD",
  UPI: "UPI",
  WALLET: "WALLET",
};

export const PaymentType = {
  SUBSCRIPTION: "SUBSCRIPTION",
  RENEWAL: "RENEWAL",
  UPGRADE: "UPGRADE",
  ADDON: "ADDON",
};

// NOTE: backend CouponType has no FREE_TRIAL — the plan doc listed one that doesn't exist.
export const CouponType = {
  PERCENTAGE: "PERCENTAGE",
  AMOUNT: "AMOUNT",
  BOTH: "BOTH",
};

export const UserRole = {
  USER: "USER",
  ADMIN: "ADMIN",
};

// Not in the plan doc — required field on the Plan entity.
export const PlanType = {
  BASIC: "BASIC",
  PRO: "PRO",
  PREMIUM: "PREMIUM",
};
