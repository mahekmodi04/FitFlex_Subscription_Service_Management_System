import { Badge } from "@/components/ui/badge";
import { SubscriptionStatus } from "@/types/enums";

const STYLES = {
  [SubscriptionStatus.ACTIVE]: "bg-success-soft text-success border-transparent",
  [SubscriptionStatus.GRACE]: "bg-warning-soft text-yellow-700 border-transparent",
  [SubscriptionStatus.PENDING]: "bg-muted text-muted-foreground border-transparent",
  [SubscriptionStatus.CANCELLED]: "bg-danger-soft text-danger border-transparent",
  [SubscriptionStatus.EXPIRED]: "bg-danger-soft text-danger border-transparent",
};

const LABEL = {
  [SubscriptionStatus.ACTIVE]: "Active",
  [SubscriptionStatus.GRACE]: "Grace period",
  [SubscriptionStatus.PENDING]: "Pending",
  [SubscriptionStatus.CANCELLED]: "Cancelled",
  [SubscriptionStatus.EXPIRED]: "Expired",
};

export function StatusBadge({ status, className = "" }) {
  return (
    <Badge className={`${STYLES[status] ?? "bg-muted text-muted-foreground"} ${className}`}>
      {LABEL[status] ?? status}
    </Badge>
  );
}
