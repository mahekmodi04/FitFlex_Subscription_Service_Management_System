// JSDoc typedefs mirroring the backend DTOs/entities exactly (see FRONTEND_IMPLEMENTATION_PLAN.md
// cross-check). No runtime code here — this file exists purely so editors can show field hints.

/**
 * @typedef {Object} LoginRequest
 * @property {string} email
 * @property {string} password
 */

/**
 * @typedef {Object} AuthResponse
 * @property {string} token
 * @property {string} message
 * @property {number} userId
 * @property {string} name
 * @property {string} email
 * @property {"USER"|"ADMIN"} role
 */

/**
 * @typedef {Object} User
 * @property {number} id
 * @property {string} name
 * @property {string} email
 * @property {"USER"|"ADMIN"} role
 * @property {number} walletBalance
 */

/**
 * @typedef {Object} UserWriteRequest
 * @property {string} name
 * @property {string} email
 * @property {string} password
 */

/**
 * @typedef {Object} Plan
 * @property {number} id
 * @property {string} name
 * @property {number} price
 * @property {number} durationDays
 * @property {string|null} [description]
 * @property {"BASIC"|"PRO"|"PREMIUM"} tier
 * @property {boolean} active
 */

/**
 * @typedef {Object} Coupon
 * @property {number} id
 * @property {string} code
 * @property {number|null} discountPercentage
 * @property {number|null} discountAmount
 * @property {number} usageLimit
 * @property {number} usedCount
 * @property {boolean} active
 * @property {string} expiryDate
 * @property {"PERCENTAGE"|"AMOUNT"|"BOTH"} type
 */

/**
 * @typedef {Object} AddOn
 * @property {number} id
 * @property {string} name
 * @property {string|null} [description]
 * @property {string} unitName
 * @property {number} unitPrice
 * @property {boolean} active
 */

/**
 * @typedef {Object} AddOnRequestDTO
 * @property {number} addOnId
 * @property {number} unitsIncluded
 */

/**
 * @typedef {Object} SubscriptionAddOnResponseDTO
 * @property {number} addOnId
 * @property {string} addOnName
 * @property {number} unitsIncluded
 * @property {number} unitsUsed
 * @property {number} unitPrice
 * @property {string} billingCycleStart
 * @property {string} billingCycleEnd
 */

/**
 * @typedef {Object} CreateSubscriptionRequest
 * @property {number} userId
 * @property {number} planId
 * @property {string} [couponCode]
 * @property {boolean} autoRenew
 * @property {"CARD"|"UPI"|"WALLET"} paymentMethod
 * @property {AddOnRequestDTO[]} [addOns]
 * @property {boolean} [useWalletBalance]
 */

/**
 * @typedef {Object} SubscriptionResponseDTO
 * @property {number} id
 * @property {string} userName
 * @property {string} planName
 * @property {string|null} couponCode
 * @property {number} finalPrice
 * @property {boolean} autoRenew
 * @property {string} startDate
 * @property {string} endDate
 * @property {"PENDING"|"ACTIVE"|"GRACE"|"CANCELLED"|"EXPIRED"} status
 * @property {"SUCCESS"|"FAILED"|"REFUNDED"|null} paymentStatus - only set on create/cancel responses, not on plain GETs
 * @property {string|null} nextRetryDate
 * @property {string|null} graceEndDate
 */

/**
 * @typedef {Object} ChangePlanRequestDTO
 * @property {number} subscriptionId
 * @property {number} newPlanId
 * @property {"CARD"|"UPI"|"WALLET"} paymentMethod
 * @property {AddOnRequestDTO[]} [addOns]
 * @property {boolean} [useWalletBalance]
 */

/**
 * @typedef {Object} ChangePlanResponseDTO
 * @property {string} userName
 * @property {number} subscriptionId
 * @property {string} newPlanName
 * @property {number} newFinalPrice
 * @property {string} newStartDate
 * @property {string} newEndDate
 * @property {boolean} autoRenew
 * @property {string} status
 * @property {string} paymentStatus
 */

/**
 * @typedef {Object} PaymentResponseDTO
 * @property {number} paymentId
 * @property {number} subscriptionId
 * @property {number} amount
 * @property {"CARD"|"UPI"|"WALLET"} paymentMethod
 * @property {"SUCCESS"|"FAILED"|"REFUNDED"} paymentStatus
 * @property {string} transactionId
 * @property {string} paymentDate
 * @property {"SUBSCRIPTION"|"RENEWAL"|"UPGRADE"|"ADDON"} paymentType
 */

export {};
