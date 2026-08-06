import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, Check, Minus, Plus, RotateCcw, Tag } from "lucide-react";
import { getPlans } from "@/api/plans";
import { getAddOns } from "@/api/addons";
import { createSubscription, getSubscriptionsByUser } from "@/api/subscriptions";
import { getCouponByCode } from "@/api/coupons";
import { getUser } from "@/api/users";
import { useAuth } from "@/context/AuthContext";
import { extractErrorMessage } from "@/api/client";
import { PaymentMethod, PaymentStatus } from "@/types/enums";
import { formatCurrency } from "@/lib/format";
import { applyCouponDiscount, getCouponValidationError } from "@/lib/pricing";
import { getActiveSubscription } from "@/lib/subscriptionHelpers";
import { PaymentSection, validatePaymentFields } from "@/components/shared/PaymentSection";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";

export default function Subscribe() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const preselectedPlanId = searchParams.get("planId");

  const [planId, setPlanId] = useState(preselectedPlanId ? Number(preselectedPlanId) : null);
  const [quantities, setQuantities] = useState({});
  const [couponCode, setCouponCode] = useState("");
  const [autoRenew, setAutoRenew] = useState(true);
  const [paymentMethod, setPaymentMethod] = useState(PaymentMethod.CARD);
  const [cardDetails, setCardDetails] = useState({ number: "", expiry: "", cvv: "" });
  const [upiId, setUpiId] = useState("");
  const [useWallet, setUseWallet] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitError, setSubmitError] = useState("");
  const [paymentFailed, setPaymentFailed] = useState(false);

  const { data: plans, isLoading: plansLoading, isError: plansError } = useQuery({
    queryKey: ["plans"],
    queryFn: getPlans,
  });

  const { data: addOns, isLoading: addOnsLoading } = useQuery({
    queryKey: ["addons"],
    queryFn: getAddOns,
  });

  const { data: subscriptions } = useQuery({
    queryKey: ["subscriptions", "user", user.id],
    queryFn: () => getSubscriptionsByUser(user.id),
  });

  const { data: freshUser } = useQuery({
    queryKey: ["user", user.id],
    queryFn: () => getUser(user.id),
  });
  const walletBalance = freshUser?.walletBalance ?? 0;

  const currentActive = getActiveSubscription(subscriptions);

  const activePlans = (plans ?? []).filter((p) => p.active);
  const activeAddOns = (addOns ?? []).filter((a) => a.active);
  const selectedPlan = activePlans.find((p) => p.id === planId);

  // already on this exact plan — bounce to subscription detail instead of letting them pay twice
  useEffect(() => {
    if (currentActive && selectedPlan && currentActive.planName === selectedPlan.name) {
      navigate(`/subscription/${currentActive.id}`, { replace: true });
    }
  }, [currentActive, selectedPlan, navigate]);

  const trimmedCode = couponCode.trim();
  const {
    data: coupon,
    isFetching: couponLoading,
    isError: couponNotFound,
  } = useQuery({
    queryKey: ["coupon", trimmedCode],
    queryFn: () => getCouponByCode(trimmedCode),
    enabled: trimmedCode.length > 2,
    retry: false,
  });
  const couponError =
    trimmedCode.length > 2
      ? couponNotFound
        ? "Coupon code not found."
        : getCouponValidationError(coupon)
      : null;
  const appliedCoupon = coupon && !couponError ? coupon : null;

  const addOnSubtotal = useMemo(() => {
    return activeAddOns.reduce((sum, addOn) => {
      const qty = quantities[addOn.id] ?? 0;
      return sum + qty * addOn.unitPrice;
    }, 0);
  }, [activeAddOns, quantities]);

  const preDiscountTotal = (selectedPlan?.price ?? 0) + addOnSubtotal;
  const discountedTotal = applyCouponDiscount(selectedPlan?.price ?? 0, appliedCoupon) + addOnSubtotal;
  const walletToUse = useWallet ? Math.min(walletBalance, discountedTotal) : 0;
  const amountDue = Math.max(0, discountedTotal - walletToUse);

  const setQuantity = (addOnId, qty) => {
    setQuantities((prev) => ({ ...prev, [addOnId]: Math.max(0, qty) }));
  };

  const mutation = useMutation({
    mutationFn: createSubscription,
    onSuccess: (subscription) => {
      if (subscription.paymentStatus === PaymentStatus.FAILED) {
        setPaymentFailed(true);
        setSubmitError("");
        return;
      }
      setPaymentFailed(false);
      queryClient.invalidateQueries({ queryKey: ["subscriptions", "user", user.id] });
      queryClient.invalidateQueries({ queryKey: ["user", user.id] });
      navigate("/dashboard", { replace: true });
    },
    onError: (error) => {
      setSubmitError(extractErrorMessage(error));
    },
  });

  const handleSubmit = () => {
    setSubmitError("");
    setPaymentFailed(false);

    if (amountDue > 0) {
      const errors = validatePaymentFields(paymentMethod, cardDetails, upiId);
      if (Object.keys(errors).length > 0) {
        setFieldErrors(errors);
        return;
      }
    }
    setFieldErrors({});

    const addOnsPayload = activeAddOns
      .filter((a) => (quantities[a.id] ?? 0) > 0)
      .map((a) => ({ addOnId: a.id, unitsIncluded: quantities[a.id] }));

    mutation.mutate({
      userId: user.id,
      planId,
      couponCode: appliedCoupon ? trimmedCode : undefined,
      autoRenew,
      paymentMethod,
      addOns: addOnsPayload.length ? addOnsPayload : undefined,
      useWalletBalance: useWallet,
    });
  };

  return (
    <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6">
      <h1 className="font-display text-3xl font-bold text-ink">Subscribe</h1>
      <p className="mt-1 text-muted-foreground">Pick a plan, add extras, and check out.</p>

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_320px]">
        <div className="space-y-8">
          <section>
            <h2 className="mb-3 font-display text-lg font-semibold text-ink">1. Choose a plan</h2>
            {plansLoading && (
              <div className="grid gap-4 sm:grid-cols-3">
                {[0, 1, 2].map((i) => (
                  <Skeleton key={i} className="h-32 w-full rounded-xl" />
                ))}
              </div>
            )}
            {plansError && <p className="text-danger">Couldn&apos;t load plans. Please refresh.</p>}
            {!plansLoading && !plansError && (
              <div className="grid gap-4 sm:grid-cols-3">
                {activePlans.map((plan) => {
                  const isSelected = plan.id === planId;
                  const isCurrent = currentActive?.planName === plan.name;
                  return (
                    <button
                      type="button"
                      key={plan.id}
                      disabled={isCurrent}
                      onClick={() => setPlanId(plan.id)}
                      className={`relative rounded-xl border p-4 text-left transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${
                        isSelected
                          ? "border-accent bg-accent-soft/40 ring-1 ring-accent"
                          : "border-border bg-white hover:border-accent/50"
                      }`}
                    >
                      {isCurrent && (
                        <Badge className="absolute -top-2.5 right-3 bg-ink text-white">Current plan</Badge>
                      )}
                      <div className="flex items-center justify-between">
                        <span className="font-medium text-ink">{plan.name}</span>
                        {isSelected && <Check className="size-4 text-accent" />}
                      </div>
                      <p className="mt-1 text-lg font-bold text-ink">{formatCurrency(plan.price)}</p>
                      <p className="text-xs text-muted-foreground">{plan.durationDays} days</p>
                    </button>
                  );
                })}
              </div>
            )}
          </section>

          <section>
            <h2 className="mb-3 font-display text-lg font-semibold text-ink">
              2. Add-ons <span className="font-normal text-muted-foreground">(optional)</span>
            </h2>
            {addOnsLoading && <Skeleton className="h-24 w-full rounded-xl" />}
            {!addOnsLoading && activeAddOns.length === 0 && (
              <p className="text-sm text-muted-foreground">No add-ons available right now.</p>
            )}
            {!addOnsLoading && activeAddOns.length > 0 && (
              <div className="space-y-3">
                {activeAddOns.map((addOn) => {
                  const qty = quantities[addOn.id] ?? 0;
                  return (
                    <Card key={addOn.id}>
                      <CardContent className="flex items-center justify-between gap-4 py-4">
                        <div>
                          <p className="font-medium text-ink">{addOn.name}</p>
                          <p className="text-sm text-muted-foreground">
                            {formatCurrency(addOn.unitPrice)} / {addOn.unitName}
                          </p>
                        </div>
                        <div className="flex items-center gap-2">
                          <Button
                            type="button"
                            size="icon-sm"
                            variant="outline"
                            onClick={() => setQuantity(addOn.id, qty - 1)}
                            disabled={qty === 0}
                          >
                            <Minus className="size-3.5" />
                          </Button>
                          <span className="w-6 text-center text-sm font-medium">{qty}</span>
                          <Button
                            type="button"
                            size="icon-sm"
                            variant="outline"
                            onClick={() => setQuantity(addOn.id, qty + 1)}
                          >
                            <Plus className="size-3.5" />
                          </Button>
                        </div>
                      </CardContent>
                    </Card>
                  );
                })}
              </div>
            )}
          </section>

          <section>
            <h2 className="mb-3 font-display text-lg font-semibold text-ink">3. Coupon & payment</h2>
            <Card>
              <CardContent className="space-y-4 pt-6">
                <div className="space-y-1.5">
                  <Label htmlFor="coupon">Coupon code (optional)</Label>
                  <Input
                    id="coupon"
                    placeholder="e.g. WELCOME10"
                    value={couponCode}
                    onChange={(e) => setCouponCode(e.target.value)}
                  />
                  {couponLoading && <p className="text-xs text-muted-foreground">Checking code…</p>}
                  {!couponLoading && trimmedCode.length > 2 && appliedCoupon && (
                    <p className="flex items-center gap-1 text-xs text-success">
                      <Tag className="size-3.5" />
                      Coupon applied — discount reflected below.
                    </p>
                  )}
                  {!couponLoading && trimmedCode.length > 2 && couponError && (
                    <p className="text-xs text-danger">{couponError}</p>
                  )}
                </div>

                <PaymentSection
                  paymentMethod={paymentMethod}
                  onPaymentMethodChange={setPaymentMethod}
                  cardDetails={cardDetails}
                  onCardDetailsChange={setCardDetails}
                  upiId={upiId}
                  onUpiIdChange={setUpiId}
                  walletBalance={walletBalance}
                  useWallet={useWallet}
                  onUseWalletChange={setUseWallet}
                  amountDue={amountDue}
                  fieldErrors={fieldErrors}
                />

                <div className="flex items-center justify-between rounded-lg border border-border px-3 py-2.5">
                  <div>
                    <p className="text-sm font-medium text-ink">Auto-renew</p>
                    <p className="text-xs text-muted-foreground">Renew automatically when this cycle ends</p>
                  </div>
                  <Switch checked={autoRenew} onCheckedChange={setAutoRenew} />
                </div>
              </CardContent>
            </Card>
          </section>
        </div>

        <div className="lg:sticky lg:top-24 lg:self-start">
          <Card>
            <CardHeader>
              <h2 className="font-display text-lg font-semibold text-ink">Order summary</h2>
            </CardHeader>
            <CardContent className="space-y-3">
              {selectedPlan ? (
                <>
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">{selectedPlan.name} plan</span>
                    <span className="font-medium text-ink">{formatCurrency(selectedPlan.price)}</span>
                  </div>
                  {addOnSubtotal > 0 && (
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Add-ons</span>
                      <span className="font-medium text-ink">{formatCurrency(addOnSubtotal)}</span>
                    </div>
                  )}
                  {appliedCoupon && (
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Coupon ({trimmedCode})</span>
                      <span className="font-medium text-success">
                        −{formatCurrency(preDiscountTotal - discountedTotal)}
                      </span>
                    </div>
                  )}
                  {walletToUse > 0 && (
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Wallet balance applied</span>
                      <span className="font-medium text-success">−{formatCurrency(walletToUse)}</span>
                    </div>
                  )}
                  <div className="border-t border-border pt-3">
                    <div className="flex justify-between">
                      <span className="font-medium text-ink">Due today</span>
                      <span className="font-bold text-ink">{formatCurrency(amountDue)}</span>
                    </div>
                  </div>
                </>
              ) : (
                <p className="text-sm text-muted-foreground">Select a plan to see your total.</p>
              )}

              {paymentFailed && (
                <div className="flex items-start gap-2 rounded-md bg-danger-soft px-3 py-2.5 text-sm text-danger">
                  <AlertTriangle className="mt-0.5 size-4 shrink-0" />
                  <div>
                    <p className="font-medium">Payment failed</p>
                    <p>Your card/payment method was declined. No charge was made — try again.</p>
                  </div>
                </div>
              )}

              {submitError && (
                <p role="alert" className="rounded-md bg-danger-soft px-3 py-2 text-sm text-danger">
                  {submitError}
                </p>
              )}

              <Button
                className="w-full"
                size="lg"
                disabled={!planId || mutation.isPending}
                onClick={handleSubmit}
              >
                {mutation.isPending ? (
                  "Processing…"
                ) : paymentFailed ? (
                  <>
                    <RotateCcw className="size-4" />
                    Retry payment
                  </>
                ) : (
                  "Confirm & pay"
                )}
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
