import { useQuery } from "@tanstack/react-query";
import { getSubscriptionsByUser } from "@/api/subscriptions";
import { getPaymentsBySubscription } from "@/api/payments";
import { useAuth } from "@/context/AuthContext";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { getCurrentSubscription } from "@/lib/subscriptionHelpers";
import { PaymentStatus } from "@/types/enums";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

const TYPE_LABEL = {
  SUBSCRIPTION: "Subscription",
  RENEWAL: "Renewal",
  UPGRADE: "Upgrade",
  ADDON: "Add-on",
};

const STATUS_STYLES = {
  [PaymentStatus.SUCCESS]: "bg-success-soft text-success border-transparent",
  [PaymentStatus.FAILED]: "bg-danger-soft text-danger border-transparent",
  [PaymentStatus.REFUNDED]: "bg-muted text-muted-foreground border-transparent",
};

export default function Billing() {
  const { user } = useAuth();

  const { data: subscriptions, isLoading: subsLoading } = useQuery({
    queryKey: ["subscriptions", "user", user.id],
    queryFn: () => getSubscriptionsByUser(user.id),
  });

  const current = getCurrentSubscription(subscriptions);

  const {
    data: payments,
    isLoading: paymentsLoading,
    isError,
  } = useQuery({
    queryKey: ["payments", "subscription", current?.id],
    queryFn: () => getPaymentsBySubscription(current.id),
    enabled: !!current,
  });

  const isLoading = subsLoading || (!!current && paymentsLoading);

  return (
    <div className="mx-auto max-w-4xl px-4 py-12 sm:px-6">
      <h1 className="font-display text-2xl font-bold text-ink">Billing history</h1>
      <p className="mt-1 text-muted-foreground">Every payment on your account.</p>

      <div className="mt-8 overflow-x-auto rounded-xl border border-border bg-white">
        {isLoading && (
          <div className="space-y-2 p-4">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        )}

        {!isLoading && !current && (
          <p className="p-8 text-center text-muted-foreground">
            You don&apos;t have a membership yet, so there&apos;s no billing history.
          </p>
        )}

        {!isLoading && current && isError && (
          <p className="p-8 text-center text-danger">Couldn&apos;t load your payments.</p>
        )}

        {!isLoading && current && !isError && (payments?.length ?? 0) === 0 && (
          <p className="p-8 text-center text-muted-foreground">No payments yet.</p>
        )}

        {!isLoading && current && !isError && (payments?.length ?? 0) > 0 && (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Date</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Method</TableHead>
                <TableHead>Amount</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Refund</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {[...payments]
                .sort((a, b) => new Date(b.paymentDate) - new Date(a.paymentDate))
                .map((payment) => (
                  <TableRow key={payment.paymentId}>
                    <TableCell className="text-sm text-muted-foreground">
                      {formatDateTime(payment.paymentDate)}
                    </TableCell>
                    <TableCell>{TYPE_LABEL[payment.paymentType] ?? payment.paymentType}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {payment.paymentMethod}
                    </TableCell>
                    <TableCell className="font-medium text-ink">
                      {formatCurrency(payment.amount)}
                    </TableCell>
                    <TableCell>
                      <Badge className={STATUS_STYLES[payment.paymentStatus] ?? ""}>
                        {payment.paymentStatus}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <span>
                            <Button size="sm" variant="outline" disabled>
                              Request refund
                            </Button>
                          </span>
                        </TooltipTrigger>
                        <TooltipContent>Coming soon</TooltipContent>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
            </TableBody>
          </Table>
        )}
      </div>
    </div>
  );
}
