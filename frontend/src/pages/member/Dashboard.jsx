import { Link, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, ArrowUpRight, Plus, Receipt, Settings, Wallet } from "lucide-react";
import { getSubscriptionsByUser } from "@/api/subscriptions";
import { getSubscriptionAddOns } from "@/api/addons";
import { getUser } from "@/api/users";
import { useAuth } from "@/context/AuthContext";
import { formatDate, formatCurrency } from "@/lib/format";
import { getCurrentSubscription } from "@/lib/subscriptionHelpers";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { UsageMeter } from "@/components/shared/UsageMeter";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { SubscriptionStatus } from "@/types/enums";

export default function Dashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const {
    data: subscriptions,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["subscriptions", "user", user.id],
    queryFn: () => getSubscriptionsByUser(user.id),
  });

  const current = getCurrentSubscription(subscriptions);

  const { data: addOns, isLoading: addOnsLoading } = useQuery({
    queryKey: ["subscription-addons", current?.id],
    queryFn: () => getSubscriptionAddOns(current.id),
    enabled: !!current && current.status !== SubscriptionStatus.PENDING,
  });

  const { data: freshUser } = useQuery({
    queryKey: ["user", user.id],
    queryFn: () => getUser(user.id),
  });

  if (isLoading) {
    return (
      <div className="mx-auto max-w-5xl space-y-6 px-4 py-12 sm:px-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-56 w-full rounded-xl" />
        <Skeleton className="h-40 w-full rounded-xl" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6">
        <p className="rounded-md bg-danger-soft px-4 py-3 text-danger">
          Couldn&apos;t load your subscription. Please refresh the page.
        </p>
      </div>
    );
  }

  if (!current) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-16 text-center sm:px-6">
        <h1 className="font-display text-2xl font-bold text-ink">Welcome, {user.name}</h1>
        <p className="mt-2 text-muted-foreground">You don&apos;t have a membership yet.</p>
        <Button asChild className="mt-6">
          <Link to="/plans">Browse plans</Link>
        </Button>
      </div>
    );
  }

  const cycleStart = new Date(current.startDate).getTime();
  const cycleEnd = new Date(current.endDate).getTime();
  const now = Date.now();
  const cyclePct = Math.min(100, Math.max(0, ((now - cycleStart) / (cycleEnd - cycleStart)) * 100));
  const daysLeft = Math.max(0, Math.ceil((cycleEnd - now) / (1000 * 60 * 60 * 24)));

  return (
    <div className="mx-auto max-w-5xl space-y-6 px-4 py-12 sm:px-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Welcome back, {user.name}</h1>
          <p className="mt-1 text-muted-foreground">Here&apos;s what&apos;s happening with your membership.</p>
        </div>
        {freshUser && (
          <div className="flex items-center gap-2 rounded-full border border-border bg-white px-4 py-2 text-sm shadow-sm">
            <Wallet className="size-4 text-accent" />
            <span className="font-medium text-ink">{formatCurrency(freshUser.walletBalance)}</span>
            <span className="text-muted-foreground">wallet</span>
          </div>
        )}
      </div>

      <Card className="overflow-hidden border-none bg-gradient-to-br from-ink via-ink to-slate-800 text-white shadow-lg">
        <CardHeader className="flex-row items-start justify-between space-y-0">
          <div>
            <div className="mb-2 flex items-center gap-2">
              <StatusBadge status={current.status} />
              {current.couponCode && (
                <span className="text-xs text-white/60">Coupon: {current.couponCode}</span>
              )}
            </div>
            <h2 className="font-display text-2xl font-semibold text-white">{current.planName} plan</h2>
            <p className="text-sm text-white/70">
              {formatDate(current.startDate)} – {formatDate(current.endDate)} ·{" "}
              {formatCurrency(current.finalPrice)}
            </p>
          </div>
          <Button
            variant="outline"
            size="sm"
            className="border-white/20 bg-white/5 text-white hover:bg-white/15 hover:text-white"
            onClick={() => navigate(`/subscription/${current.id}`)}
          >
            View details
            <ArrowUpRight className="size-3.5" />
          </Button>
        </CardHeader>

        {(current.status === SubscriptionStatus.ACTIVE || current.status === SubscriptionStatus.GRACE) && (
          <CardContent className="pb-2">
            <div className="mb-1.5 flex justify-between text-xs text-white/60">
              <span>Billing cycle</span>
              <span>{daysLeft} day{daysLeft === 1 ? "" : "s"} left</span>
            </div>
            <Progress value={cyclePct} className="h-1.5 bg-white/10 [&>div]:bg-accent" />
          </CardContent>
        )}

        {current.status === SubscriptionStatus.GRACE && (
          <CardContent>
            <div className="flex items-start gap-3 rounded-lg bg-warning-soft px-4 py-3 text-sm">
              <AlertTriangle className="mt-0.5 size-4 shrink-0 text-yellow-700" />
              <p className="text-yellow-800">
                {current.nextRetryDate ? (
                  <>Payment retry scheduled for {formatDate(current.nextRetryDate)}. </>
                ) : (
                  "A payment retry is scheduled. "
                )}
                {current.graceEndDate ? (
                  <>Your plan will expire on {formatDate(current.graceEndDate)} if payment isn&apos;t recovered.</>
                ) : (
                  "Your plan will expire if payment isn't recovered in time."
                )}
              </p>
            </div>
          </CardContent>
        )}

        <CardContent className="flex flex-wrap items-center gap-2 border-t border-white/10 pt-4">
          <span className="mr-1 text-sm text-white/60">Auto-renew</span>
          <StatusBadge
            status={current.autoRenew ? SubscriptionStatus.ACTIVE : SubscriptionStatus.CANCELLED}
            className="pointer-events-none"
          />
          <span className="text-xs text-white/50">
            {current.autoRenew ? "On" : "Off"} · set at signup, not yet editable
          </span>
        </CardContent>

        {current.status === SubscriptionStatus.ACTIVE && (
          <CardContent className="flex flex-wrap gap-2 border-t border-white/10 pt-4">
            <Button size="sm" onClick={() => navigate(`/subscription/${current.id}/upgrade`)}>
              <Settings className="size-3.5" />
              Upgrade plan
            </Button>
            <Button
              size="sm"
              variant="outline"
              className="border-white/20 bg-white/5 text-white hover:bg-white/15 hover:text-white"
              onClick={() => navigate(`/subscription/${current.id}`)}
            >
              <Plus className="size-3.5" />
              Buy add-ons
            </Button>
            <Button
              size="sm"
              variant="ghost"
              className="text-white/80 hover:bg-white/10 hover:text-white"
              onClick={() => navigate("/billing")}
            >
              <Receipt className="size-3.5" />
              Billing history
            </Button>
          </CardContent>
        )}
      </Card>

      {current.status !== SubscriptionStatus.PENDING && (
        <Card>
          <CardHeader>
            <h2 className="font-display text-lg font-semibold text-ink">Add-on usage</h2>
          </CardHeader>
          <CardContent className="space-y-4">
            {addOnsLoading && <Skeleton className="h-12 w-full" />}
            {!addOnsLoading && (addOns?.length ?? 0) === 0 && (
              <p className="text-sm text-muted-foreground">
                No add-ons attached yet.{" "}
                <Link to={`/subscription/${current.id}`} className="text-accent hover:underline">
                  Buy one
                </Link>
                .
              </p>
            )}
            {!addOnsLoading &&
              addOns?.map((addOn) => (
                <UsageMeter
                  key={addOn.addOnId}
                  label={addOn.addOnName}
                  used={addOn.unitsUsed}
                  total={addOn.unitsIncluded}
                />
              ))}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
