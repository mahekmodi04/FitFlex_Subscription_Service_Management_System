import { SubscriptionStatus } from "@/types/enums";

// Every retry after a failed payment creates a brand-new subscription row (no "retry same
// attempt" endpoint exists), so a user can end up with several PENDING/CANCELLED/EXPIRED rows
// alongside one real ACTIVE/GRACE one. "Most recent by id" is wrong here — an old successful
// subscription must always win over a newer failed attempt. Prefer ACTIVE/GRACE (most recent
// among those); only fall back to the latest row overall if the user has no live subscription,
// so we can still show them their last attempt's failed state.
export function getCurrentSubscription(subscriptions) {
  if (!subscriptions?.length) return null;
  const sorted = [...subscriptions].sort((a, b) => b.id - a.id);
  return getActiveSubscription(subscriptions) ?? sorted[0];
}

// Strict version for guards like "block re-subscribing to a plan you already hold" — returns
// null (not a fallback to a failed attempt) when the user has no live subscription.
export function getActiveSubscription(subscriptions) {
  if (!subscriptions?.length) return null;
  const sorted = [...subscriptions].sort((a, b) => b.id - a.id);
  return (
    sorted.find(
      (s) => s.status === SubscriptionStatus.ACTIVE || s.status === SubscriptionStatus.GRACE
    ) ?? null
  );
}
