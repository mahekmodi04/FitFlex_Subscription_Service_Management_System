import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { getPlans } from "@/api/plans";
import { getSubscriptionsByUser } from "@/api/subscriptions";
import { useAuth } from "@/context/AuthContext";
import { getActiveSubscription } from "@/lib/subscriptionHelpers";
import { PlanCard } from "@/components/shared/PlanCard";
import { Skeleton } from "@/components/ui/skeleton";
import { UserRole } from "@/types/enums";

export default function PlansPublic() {
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();
  const isAdmin = user?.role === UserRole.ADMIN;

  const {
    data: plans,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["plans"],
    queryFn: getPlans,
  });

  const { data: subscriptions } = useQuery({
    queryKey: ["subscriptions", "user", user?.id],
    queryFn: () => getSubscriptionsByUser(user.id),
    enabled: isAuthenticated && !isAdmin,
  });

  const currentActive = getActiveSubscription(subscriptions);
  const currentPlanObj = (plans ?? []).find((p) => p.name === currentActive?.planName);

  const activePlans = (plans ?? []).filter((p) => p.active);

  const getCardProps = (plan) => {
    if (!isAuthenticated) {
      return { actionLabel: "Get started", disabled: false, onAction: () => navigate("/register") };
    }
    if (isAdmin) {
      return { actionLabel: undefined, disabled: false, onAction: () => {} };
    }
    if (!currentActive) {
      return {
        actionLabel: "Subscribe",
        disabled: false,
        onAction: () => navigate(`/subscribe?planId=${plan.id}`),
      };
    }
    if (currentActive.planName === plan.name) {
      return { actionLabel: "Current plan", disabled: true, onAction: () => {} };
    }
    if (currentPlanObj && plan.price > currentPlanObj.price) {
      return {
        actionLabel: "Upgrade to this plan",
        disabled: false,
        onAction: () => navigate(`/subscription/${currentActive.id}/upgrade?planId=${plan.id}`),
      };
    }
    return { actionLabel: "Not available", disabled: true, onAction: () => {} };
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
      <div className="mb-12 text-center">
        <h1 className="font-display text-3xl font-bold text-ink">Membership plans</h1>
        <p className="mt-2 text-muted-foreground">Simple pricing. Upgrade any time.</p>
      </div>

      {isLoading && (
        <div className="grid gap-6 sm:grid-cols-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-80 w-full rounded-xl" />
          ))}
        </div>
      )}

      {isError && (
        <p className="text-center text-danger">
          Couldn&apos;t load plans right now. Please refresh the page.
        </p>
      )}

      {!isLoading && !isError && activePlans.length === 0 && (
        <p className="text-center text-muted-foreground">No plans are available right now.</p>
      )}

      {!isLoading && !isError && activePlans.length > 0 && (
        <div className="grid gap-6 sm:grid-cols-3">
          {activePlans.map((plan, i) => {
            const cardProps = getCardProps(plan);
            return (
              <PlanCard
                key={plan.id}
                plan={plan}
                highlighted={i === Math.floor(activePlans.length / 2)}
                actionLabel={cardProps.actionLabel}
                disabled={cardProps.disabled}
                onAction={cardProps.onAction}
              />
            );
          })}
        </div>
      )}
    </div>
  );
}
