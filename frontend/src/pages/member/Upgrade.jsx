import { useMemo, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, Check, Minus, Plus } from "lucide-react";
import { getSubscription, changePlan } from "@/api/subscriptions";
import { getPlans } from "@/api/plans";
import { getAddOns, getSubscriptionAddOns } from "@/api/addons";
import { getUser } from "@/api/users";
import { useAuth } from "@/context/AuthContext";
import { extractErrorMessage } from "@/api/client";
import { formatCurrency } from "@/lib/format";
import { PaymentMethod, PaymentStatus, SubscriptionStatus } from "@/types/enums";
import { PaymentSection, validatePaymentFields } from "@/components/shared/PaymentSection";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

export default function Upgrade() {
  const { id } = useParams();
  const subscriptionId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const preselectedPlanId = searchParams.get("planId");

  const [newPlanId, setNewPlanId] = useState(preselectedPlanId ? Number(preselectedPlanId) : null);
  const [addOnQuantities, setAddOnQuantities] = useState({});
  const [paymentMethod, setPaymentMethod] = useState(PaymentMethod.CARD);
  const [cardDetails, setCardDetails] = useState({ number: "", expiry: "", cvv: "" });
  const [upiId, setUpiId] = useState("");
  const [useWallet, setUseWallet] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitError, setSubmitError] = useState("");
  const [paymentFailed, setPaymentFailed] = useState(false);

  const { data: subscription, isLoading: subLoading } = useQuery({
    queryKey: ["subscription", subscriptionId],
    queryFn: () => getSubscription(subscriptionId),
  });

  const { data: plans, isLoading: plansLoading } = useQuery({
    queryKey: ["plans"],
    queryFn: getPlans,
  });

  const { data: catalog, isLoading: catalogLoading } = useQuery({
    queryKey: ["addons"],
    queryFn: getAddOns,
  });

  const { data: currentAddOns } = useQuery({
    queryKey: ["subscription-addons", subscriptionId],
    queryFn: () => getSubscriptionAddOns(subscriptionId),
  });

  const { data: freshUser } = useQuery({
    queryKey: ["user", user.id],
    queryFn: () => getUser(user.id),
  });
  const walletBalance = freshUser?.walletBalance ?? 0;

  const currentPlan = (plans ?? []).find((p) => p.name === subscription?.planName);
  const upgradeOptions = (plans ?? []).filter(
    (p) => p.active && currentPlan && p.price > currentPlan.price
  );
  const newPlan = upgradeOptions.find((p) => p.id === newPlanId);
  const activeAddOns = (catalog ?? []).filter((a) => a.active);
  const unusedByAddOnId = new Map(
    (currentAddOns ?? []).map((a) => [a.addOnId, Math.max(0, a.unitsIncluded - a.unitsUsed)])
  );

  const setAddOnQuantity = (addOnId, qty) => {
    setAddOnQuantities((prev) => ({ ...prev, [addOnId]: Math.max(0, qty) }));
  };

  const newAddOnCharge = useMemo(() => {
    return activeAddOns.reduce((sum, addOn) => {
      const qty = addOnQuantities[addOn.id] ?? 0;
      return sum + qty * addOn.unitPrice;
    }, 0);
  }, [activeAddOns, addOnQuantities]);

  // mirrors the backend's proration formula for display only — the actual charge is
  // authoritative from the PUT /subscriptions/change-plan response
  const proration = useMemo(() => {
    if (!subscription || !currentPlan || !newPlan) return null;
    const daysUsed = Math.floor(
      (Date.now() - new Date(subscription.startDate).getTime()) / (1000 * 60 * 60 * 24)
    );
    const pricePerDay = currentPlan.price / currentPlan.durationDays;
    const priceConsumed = Math.min(pricePerDay * daysUsed, currentPlan.price);
    const creditRemaining = currentPlan.price - priceConsumed;
    const proratedPlanPrice = Math.max(0, newPlan.price - creditRemaining);
    return { daysUsed, creditRemaining, proratedPlanPrice, dueBeforeWallet: proratedPlanPrice + newAddOnCharge };
  }, [subscription, currentPlan, newPlan, newAddOnCharge]);

  const walletToUse = useWallet && proration ? Math.min(walletBalance, proration.dueBeforeWallet) : 0;
  const amountDue = proration ? Math.max(0, proration.dueBeforeWallet - walletToUse) : 0;

  const mutation = useMutation({
    mutationFn: changePlan,
    onSuccess: (response) => {
      if (response.paymentStatus === PaymentStatus.FAILED) {
        setPaymentFailed(true);
        return;
      }
      setPaymentFailed(false);
      queryClient.invalidateQueries({ queryKey: ["subscription", subscriptionId] });
      queryClient.invalidateQueries({ queryKey: ["subscriptions"] });
      queryClient.invalidateQueries({ queryKey: ["subscription-addons", subscriptionId] });
      queryClient.invalidateQueries({ queryKey: ["user", user.id] });
      navigate(`/subscription/${subscriptionId}`, { replace: true });
    },
    onError: (error) => setSubmitError(extractErrorMessage(error)),
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
      .filter((a) => (addOnQuantities[a.id] ?? 0) > 0)
      .map((a) => ({ addOnId: a.id, unitsIncluded: addOnQuantities[a.id] }));

    mutation.mutate({
      subscriptionId,
      newPlanId,
      paymentMethod,
      useWalletBalance: useWallet,
      addOns: addOnsPayload.length ? addOnsPayload : undefined,
    });
  };

  if (subLoading || plansLoading) {
    return (
      <div className="mx-auto max-w-3xl space-y-6 px-4 py-12 sm:px-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    );
  }

  if (subscription && subscription.status !== SubscriptionStatus.ACTIVE) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
        <p className="rounded-md bg-danger-soft px-4 py-3 text-danger">
          Only active subscriptions can be upgraded.
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6 px-4 py-12 sm:px-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-ink">Upgrade your plan</h1>
        <p className="mt-1 text-muted-foreground">
          Currently on {subscription?.planName} · {formatCurrency(subscription?.finalPrice ?? 0)}
        </p>
      </div>

      {upgradeOptions.length === 0 ? (
        <Card>
          <CardContent className="py-8 text-center text-muted-foreground">
            You&apos;re already on the highest available plan.
          </CardContent>
        </Card>
      ) : (
        <>
          <div>
            <h2 className="mb-3 font-display text-lg font-semibold text-ink">1. Choose a plan</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              {upgradeOptions.map((plan) => (
                <button
                  type="button"
                  key={plan.id}
                  onClick={() => setNewPlanId(plan.id)}
                  className={`rounded-xl border p-4 text-left transition-colors ${
                    newPlanId === plan.id
                      ? "border-accent bg-accent-soft/40 ring-1 ring-accent"
                      : "border-border bg-white hover:border-accent/50"
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-medium text-ink">{plan.name}</span>
                    {newPlanId === plan.id && <Check className="size-4 text-accent" />}
                  </div>
                  <p className="mt-1 text-lg font-bold text-ink">{formatCurrency(plan.price)}</p>
                  <p className="text-xs text-muted-foreground">{plan.durationDays} days</p>
                </button>
              ))}
            </div>
          </div>

          {newPlan && (
            <>
              <div>
                <h2 className="mb-3 font-display text-lg font-semibold text-ink">
                  2. Add-ons <span className="font-normal text-muted-foreground">(optional)</span>
                </h2>
                {catalogLoading && <Skeleton className="h-16 w-full rounded-xl" />}
                {!catalogLoading && activeAddOns.length === 0 && (
                  <p className="text-sm text-muted-foreground">No add-ons available right now.</p>
                )}
                {!catalogLoading && activeAddOns.length > 0 && (
                  <div className="space-y-3">
                    {activeAddOns.map((addOn) => {
                      const qty = addOnQuantities[addOn.id] ?? 0;
                      const unused = unusedByAddOnId.get(addOn.id) ?? 0;
                      return (
                        <Card key={addOn.id}>
                          <CardContent className="flex items-center justify-between gap-4 py-4">
                            <div>
                              <p className="font-medium text-ink">{addOn.name}</p>
                              <p className="text-sm text-muted-foreground">
                                {formatCurrency(addOn.unitPrice)} / {addOn.unitName}
                              </p>
                              {unused > 0 && (
                                <p className="text-xs text-accent">
                                  {unused} unused unit{unused === 1 ? "" : "s"} carry over free
                                </p>
                              )}
                            </div>
                            <div className="flex items-center gap-2">
                              <Button
                                type="button"
                                size="icon-sm"
                                variant="outline"
                                onClick={() => setAddOnQuantity(addOn.id, qty - 1)}
                                disabled={qty === 0}
                              >
                                <Minus className="size-3.5" />
                              </Button>
                              <span className="w-6 text-center text-sm font-medium">{qty}</span>
                              <Button
                                type="button"
                                size="icon-sm"
                                variant="outline"
                                onClick={() => setAddOnQuantity(addOn.id, qty + 1)}
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
              </div>

              <Card>
                <CardHeader>
                  <h2 className="font-display text-lg font-semibold text-ink">3. Review & pay</h2>
                </CardHeader>
                <CardContent className="space-y-3">
                  {proration && (
                    <>
                      <div className="flex justify-between text-sm">
                        <span className="text-muted-foreground">Days used on {currentPlan.name}</span>
                        <span className="font-medium text-ink">{proration.daysUsed} days</span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span className="text-muted-foreground">Credit for unused time</span>
                        <span className="font-medium text-ink">
                          −{formatCurrency(proration.creditRemaining)}
                        </span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span className="text-muted-foreground">{newPlan.name} plan (prorated)</span>
                        <span className="font-medium text-ink">
                          {formatCurrency(proration.proratedPlanPrice)}
                        </span>
                      </div>
                      {newAddOnCharge > 0 && (
                        <div className="flex justify-between text-sm">
                          <span className="text-muted-foreground">New add-ons</span>
                          <span className="font-medium text-ink">{formatCurrency(newAddOnCharge)}</span>
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
                        <p className="mt-1 text-xs text-muted-foreground">
                          You&apos;re only charged for newly added add-on units — any unused units on
                          your current plan carry forward for free. The exact charge is confirmed by
                          the server on submit.
                        </p>
                      </div>
                    </>
                  )}

                  <div className="border-t border-border pt-4">
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
                  </div>

                  {paymentFailed && (
                    <div className="flex items-start gap-2 rounded-md bg-danger-soft px-3 py-2.5 text-sm text-danger">
                      <AlertTriangle className="mt-0.5 size-4 shrink-0" />
                      <p>
                        Payment failed. Your current plan is unaffected — payment did not go
                        through. Try again.
                      </p>
                    </div>
                  )}
                  {submitError && <p className="text-sm text-danger">{submitError}</p>}

                  <Button className="w-full" size="lg" disabled={mutation.isPending} onClick={handleSubmit}>
                    {mutation.isPending ? "Processing…" : `Pay ${formatCurrency(amountDue)} & upgrade`}
                  </Button>
                </CardContent>
              </Card>
            </>
          )}
        </>
      )}
    </div>
  );
}
