import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, Plus, Sparkles, XCircle } from "lucide-react";
import { toast } from "sonner";
import { getSubscription, cancelSubscription } from "@/api/subscriptions";
import { getSubscriptionAddOns, getAddOns, attachAddOn, logAddOnUsage } from "@/api/addons";
import { extractErrorMessage } from "@/api/client";
import { formatCurrency, formatDate } from "@/lib/format";
import { SubscriptionStatus } from "@/types/enums";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { UsageMeter } from "@/components/shared/UsageMeter";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";

export default function SubscriptionDetail() {
  const { id } = useParams();
  const subscriptionId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [attachOpen, setAttachOpen] = useState(false);
  const [selectedAddOnId, setSelectedAddOnId] = useState(null);
  const [units, setUnits] = useState(1);
  const [attachError, setAttachError] = useState("");

  const {
    data: subscription,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["subscription", subscriptionId],
    queryFn: () => getSubscription(subscriptionId),
  });

  const { data: attachedAddOns, isLoading: addOnsLoading } = useQuery({
    queryKey: ["subscription-addons", subscriptionId],
    queryFn: () => getSubscriptionAddOns(subscriptionId),
  });

  const { data: catalog } = useQuery({
    queryKey: ["addons"],
    queryFn: getAddOns,
  });

  const attachedUnitsById = new Map((attachedAddOns ?? []).map((a) => [a.addOnId, a.unitsIncluded]));
  // active catalog add-ons are always buyable — attaching one already on this subscription
  // tops up its unitsIncluded server-side rather than creating a duplicate
  const catalogAddOns = (catalog ?? []).filter((a) => a.active);

  const openAttachDialog = (addOnId = null) => {
    setSelectedAddOnId(addOnId);
    setUnits(1);
    setAttachError("");
    setAttachOpen(true);
  };

  const selectedAddOn = catalogAddOns.find((a) => a.id === selectedAddOnId);
  const attachTotal = selectedAddOn ? selectedAddOn.unitPrice * units : 0;

  const attachMutation = useMutation({
    mutationFn: () => attachAddOn(subscriptionId, selectedAddOnId, units),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["subscription-addons", subscriptionId] });
      setAttachOpen(false);
      setSelectedAddOnId(null);
      setUnits(1);
      toast.success(`Charged ${formatCurrency(attachTotal)} — add-on updated`);
    },
    onError: (error) => setAttachError(extractErrorMessage(error)),
  });

  const usageMutation = useMutation({
    mutationFn: ({ addOnId }) => logAddOnUsage(subscriptionId, addOnId, 1),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["subscription-addons", subscriptionId] });
    },
    onError: (error) => toast.error(extractErrorMessage(error)),
  });

  const cancelMutation = useMutation({
    mutationFn: () => cancelSubscription(subscriptionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["subscription", subscriptionId] });
      queryClient.invalidateQueries({ queryKey: ["subscriptions"] });
      toast.success("Subscription cancelled");
    },
    onError: (error) => toast.error(extractErrorMessage(error)),
  });

  if (isLoading) {
    return (
      <div className="mx-auto max-w-3xl space-y-6 px-4 py-12 sm:px-6">
        <Skeleton className="h-48 w-full rounded-xl" />
        <Skeleton className="h-40 w-full rounded-xl" />
      </div>
    );
  }

  if (isError || !subscription) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
        <p className="rounded-md bg-danger-soft px-4 py-3 text-danger">
          Couldn&apos;t load this subscription.
        </p>
      </div>
    );
  }

  // buying more add-ons requires an active plan (also enforced server-side)
  const canPurchaseAddOns = subscription.status === SubscriptionStatus.ACTIVE;
  // but a cancelled subscription still ran (and was paid for) until its billing period ends,
  // so members should keep logging usage on whatever add-on units they already paid for -
  // they just can't buy more
  const canUseAddOns =
    subscription.status === SubscriptionStatus.ACTIVE ||
    subscription.status === SubscriptionStatus.CANCELLED;
  const canManage = canPurchaseAddOns;

  return (
    <div className="mx-auto max-w-3xl space-y-6 px-4 py-12 sm:px-6">
      <Card>
        <CardHeader className="flex-row items-start justify-between space-y-0">
          <div>
            <StatusBadge status={subscription.status} className="mb-2" />
            <h1 className="font-display text-2xl font-bold text-ink">{subscription.planName} plan</h1>
            <p className="text-sm text-muted-foreground">Subscription #{subscription.id}</p>
          </div>
          {canManage && (
            <Button size="sm" onClick={() => navigate(`/subscription/${subscription.id}/upgrade`)}>
              Upgrade
            </Button>
          )}
        </CardHeader>
        <CardContent className="grid gap-4 border-t border-border pt-4 sm:grid-cols-2">
          <div>
            <p className="text-xs text-muted-foreground">Billing cycle</p>
            <p className="font-medium text-ink">
              {formatDate(subscription.startDate)} – {formatDate(subscription.endDate)}
            </p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Price</p>
            <p className="font-medium text-ink">{formatCurrency(subscription.finalPrice)}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Auto-renew</p>
            <p className="font-medium text-ink">{subscription.autoRenew ? "On" : "Off"}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Coupon</p>
            <p className="font-medium text-ink">{subscription.couponCode ?? "—"}</p>
          </div>
        </CardContent>

        {subscription.status === SubscriptionStatus.GRACE && (
          <CardContent className="border-t border-border pt-4">
            <div className="flex items-start gap-3 rounded-lg bg-warning-soft px-4 py-3 text-sm text-yellow-800">
              <AlertTriangle className="mt-0.5 size-4 shrink-0 text-yellow-700" />
              <p>
                {subscription.nextRetryDate && (
                  <>Payment retry scheduled for {formatDate(subscription.nextRetryDate)}. </>
                )}
                {subscription.graceEndDate && (
                  <>Plan expires {formatDate(subscription.graceEndDate)} if payment isn&apos;t recovered.</>
                )}
              </p>
            </div>
          </CardContent>
        )}

        {subscription.status === SubscriptionStatus.CANCELLED && (
          <CardContent className="border-t border-border pt-4">
            <div className="rounded-lg bg-muted px-4 py-3 text-sm text-muted-foreground">
              This subscription is cancelled and won&apos;t renew. You can still use any add-on
              units you&apos;ve already paid for until {formatDate(subscription.endDate)} — you just
              can&apos;t buy more.
            </div>
          </CardContent>
        )}

        {canManage && (
          <CardContent className="border-t border-border pt-4">
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button variant="outline" size="sm" className="text-danger hover:text-danger">
                  <XCircle className="size-3.5" />
                  Cancel subscription
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Cancel this subscription?</AlertDialogTitle>
                  <AlertDialogDescription>
                    You&apos;ll lose access at the end of your current billing period. This can&apos;t be
                    undone.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Keep subscription</AlertDialogCancel>
                  <AlertDialogAction
                    className="bg-danger text-white hover:bg-danger/90"
                    onClick={() => cancelMutation.mutate()}
                  >
                    Yes, cancel
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </CardContent>
        )}
      </Card>

      <Card>
        <CardHeader className="flex-row items-center justify-between space-y-0">
          <h2 className="font-display text-lg font-semibold text-ink">Add-ons</h2>
          {canManage && (
            <Dialog open={attachOpen} onOpenChange={(open) => (open ? openAttachDialog() : setAttachOpen(false))}>
              <DialogTrigger asChild>
                <Button size="sm" variant="secondary">
                  <Plus className="size-3.5" />
                  Buy add-on
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Buy an add-on</DialogTitle>
                </DialogHeader>
                <div className="space-y-4">
                  {catalogAddOns.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No add-ons are available right now.</p>
                  ) : (
                    <>
                      <div className="space-y-1.5">
                        <Label>Add-on</Label>
                        <div className="space-y-2">
                          {catalogAddOns.map((addOn) => {
                            const existingUnits = attachedUnitsById.get(addOn.id);
                            return (
                              <button
                                type="button"
                                key={addOn.id}
                                onClick={() => setSelectedAddOnId(addOn.id)}
                                className={`w-full rounded-lg border px-3 py-2 text-left text-sm transition-colors ${
                                  selectedAddOnId === addOn.id
                                    ? "border-accent bg-accent-soft/40"
                                    : "border-border hover:border-accent/50"
                                }`}
                              >
                                <span className="font-medium text-ink">{addOn.name}</span>
                                <span className="ml-2 text-muted-foreground">
                                  {formatCurrency(addOn.unitPrice)} / {addOn.unitName}
                                </span>
                                {existingUnits != null && (
                                  <span className="ml-2 text-xs text-accent">
                                    already have {existingUnits}
                                  </span>
                                )}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                      <div className="space-y-1.5">
                        <Label htmlFor="units">
                          {attachedUnitsById.has(selectedAddOnId) ? "Additional units" : "Units"}
                        </Label>
                        <Input
                          id="units"
                          type="number"
                          min={1}
                          value={units}
                          onChange={(e) => setUnits(Math.max(1, Number(e.target.value)))}
                        />
                      </div>
                      {selectedAddOn && (
                        <div className="flex items-center justify-between rounded-lg bg-accent-soft/40 px-3 py-2.5 text-sm">
                          <span className="text-muted-foreground">
                            {units} × {formatCurrency(selectedAddOn.unitPrice)}
                          </span>
                          <span className="font-semibold text-ink">
                            {formatCurrency(attachTotal)} charged now
                          </span>
                        </div>
                      )}
                    </>
                  )}
                  {attachError && <p className="text-sm text-danger">{attachError}</p>}
                </div>
                <DialogFooter>
                  <Button
                    disabled={!selectedAddOnId || attachMutation.isPending}
                    onClick={() => {
                      setAttachError("");
                      attachMutation.mutate();
                    }}
                  >
                    {attachMutation.isPending
                      ? "Charging…"
                      : selectedAddOn
                        ? `Pay ${formatCurrency(attachTotal)} & attach`
                        : "Attach"}
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          )}
        </CardHeader>
        <CardContent className="space-y-5">
          {addOnsLoading && <Skeleton className="h-16 w-full" />}
          {!addOnsLoading && (attachedAddOns?.length ?? 0) === 0 && (
            <p className="text-sm text-muted-foreground">No add-ons attached yet.</p>
          )}
          {!addOnsLoading &&
            attachedAddOns?.map((addOn) => {
              const exhausted = addOn.unitsUsed >= addOn.unitsIncluded;
              return (
                <div key={addOn.addOnId} className="space-y-2">
                  <UsageMeter
                    label={addOn.addOnName}
                    used={addOn.unitsUsed}
                    total={addOn.unitsIncluded}
                  />
                  <p className="text-xs text-muted-foreground">
                    Cycle: {formatDate(addOn.billingCycleStart)} – {formatDate(addOn.billingCycleEnd)}
                  </p>
                  {(canUseAddOns || canPurchaseAddOns) && (
                    <div className="flex gap-2">
                      {canUseAddOns && (
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={exhausted || usageMutation.isPending}
                          onClick={() => usageMutation.mutate({ addOnId: addOn.addOnId })}
                        >
                          <Sparkles className="size-3.5" />
                          {exhausted ? "No units left" : "Log usage"}
                        </Button>
                      )}
                      {canPurchaseAddOns && (
                        <Button size="sm" variant="ghost" onClick={() => openAttachDialog(addOn.addOnId)}>
                          <Plus className="size-3.5" />
                          Buy more
                        </Button>
                      )}
                    </div>
                  )}
                  {!canUseAddOns && !canPurchaseAddOns && exhausted && (
                    <p className="text-xs text-muted-foreground">
                      This subscription has ended — these units are no longer accessible.
                    </p>
                  )}
                </div>
              );
            })}
        </CardContent>
      </Card>
    </div>
  );
}
