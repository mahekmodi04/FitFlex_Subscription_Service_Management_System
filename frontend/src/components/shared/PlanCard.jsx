import { Check, Crown, Dumbbell, Flame } from "lucide-react";
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { formatCurrency } from "@/lib/format";

const TIER = {
  BASIC: { label: "Basic", icon: Dumbbell },
  PRO: { label: "Pro", icon: Flame },
  PREMIUM: { label: "Premium", icon: Crown },
};

export function PlanCard({ plan, highlighted = false, actionLabel, onAction, disabled = false }) {
  const tier = TIER[plan.tier] ?? { label: plan.tier, icon: Dumbbell };
  const TierIcon = tier.icon;

  return (
    <Card
      className={
        highlighted
          ? "relative overflow-hidden border-accent shadow-lg shadow-accent/15 ring-1 ring-accent transition-transform hover:-translate-y-1"
          : "relative overflow-hidden transition-transform hover:-translate-y-1 hover:shadow-md"
      }
    >
      {highlighted && (
        <>
          <div className="pointer-events-none absolute -right-10 -top-10 size-32 rounded-full bg-accent/10 blur-2xl" />
          <Badge className="absolute -top-3 left-1/2 -translate-x-1/2 bg-accent text-white hover:bg-accent">
            Most popular
          </Badge>
        </>
      )}
      <CardHeader>
        <div className="flex w-fit items-center gap-1.5 rounded-full border border-border px-2.5 py-1 text-xs font-medium text-muted-foreground">
          <TierIcon className="size-3.5 text-accent" />
          {tier.label}
        </div>
        <h3 className="mt-2 font-display text-xl font-semibold text-ink">{plan.name}</h3>
        <div className="mt-1 flex items-baseline gap-1">
          <span className="text-3xl font-bold text-ink">{formatCurrency(plan.price)}</span>
          <span className="text-sm text-muted-foreground">/ {plan.durationDays} days</span>
        </div>
      </CardHeader>
      <CardContent className="flex-1">
        {plan.description ? (
          <p className="text-sm text-muted-foreground">{plan.description}</p>
        ) : (
          <p className="flex items-center gap-2 text-sm text-muted-foreground">
            <Check className="size-4 text-success" />
            Full gym access included
          </p>
        )}
      </CardContent>
      {actionLabel && (
        <CardFooter>
          <Button
            className="w-full"
            variant={highlighted ? "default" : "secondary"}
            onClick={onAction}
            disabled={disabled}
          >
            {actionLabel}
          </Button>
        </CardFooter>
      )}
    </Card>
  );
}
