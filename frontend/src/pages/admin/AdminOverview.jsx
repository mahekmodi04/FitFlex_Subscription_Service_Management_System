import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, ArrowUpRight, DollarSign, Users } from "lucide-react";
import { getAllSubscriptions } from "@/api/subscriptions";
import { getAllPayments } from "@/api/payments";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { PaymentStatus, SubscriptionStatus } from "@/types/enums";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";

const PAYMENT_STATUS_STYLES = {
  [PaymentStatus.SUCCESS]: "bg-success-soft text-success border-transparent",
  [PaymentStatus.FAILED]: "bg-danger-soft text-danger border-transparent",
  [PaymentStatus.REFUNDED]: "bg-muted text-muted-foreground border-transparent",
};

export default function AdminOverview() {
  const { data: subscriptions, isLoading: subsLoading } = useQuery({
    queryKey: ["admin", "subscriptions"],
    queryFn: getAllSubscriptions,
  });

  const { data: payments, isLoading: paymentsLoading } = useQuery({
    queryKey: ["admin", "payments"],
    queryFn: getAllPayments,
  });

  const activeSubs = subscriptions?.filter((s) => s.status === SubscriptionStatus.ACTIVE) ?? [];
  const graceSubs = subscriptions?.filter((s) => s.status === SubscriptionStatus.GRACE) ?? [];
  const mrr = activeSubs.reduce((sum, s) => sum + s.finalPrice, 0);
  const atRiskRevenue = graceSubs.reduce((sum, s) => sum + s.finalPrice, 0);

  const recentPayments = payments?.length
    ? [...payments].sort((a, b) => new Date(b.paymentDate) - new Date(a.paymentDate)).slice(0, 8)
    : [];

  return (
    <div className="space-y-6 pb-12">
      <div>
        <h1 className="font-display text-2xl font-bold text-ink">Overview</h1>
        <p className="mt-1 text-muted-foreground">A snapshot of the business right now.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardHeader className="flex-row items-center justify-between space-y-0 pb-2">
            <p className="text-sm text-muted-foreground">Active subscribers</p>
            <Users className="size-4 text-accent" />
          </CardHeader>
          <CardContent>
            {subsLoading ? (
              <Skeleton className="h-8 w-16" />
            ) : (
              <p className="text-3xl font-bold text-ink">{activeSubs.length}</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-row items-center justify-between space-y-0 pb-2">
            <p className="text-sm text-muted-foreground">Active revenue (MRR)</p>
            <DollarSign className="size-4 text-success" />
          </CardHeader>
          <CardContent>
            {subsLoading ? (
              <Skeleton className="h-8 w-24" />
            ) : (
              <p className="text-3xl font-bold text-ink">{formatCurrency(mrr)}</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-row items-center justify-between space-y-0 pb-2">
            <p className="text-sm text-muted-foreground">At-risk revenue (grace)</p>
            <AlertTriangle className="size-4 text-yellow-600" />
          </CardHeader>
          <CardContent>
            {subsLoading ? (
              <Skeleton className="h-8 w-24" />
            ) : (
              <>
                <p className="text-3xl font-bold text-ink">{formatCurrency(atRiskRevenue)}</p>
                <p className="text-xs text-muted-foreground">{graceSubs.length} subscription(s)</p>
              </>
            )}
          </CardContent>
        </Card>
      </div>

      {graceSubs.length > 0 && (
        <Card>
          <CardHeader>
            <h2 className="font-display text-lg font-semibold text-ink">Subscriptions in grace period</h2>
          </CardHeader>
          <CardContent className="space-y-3">
            {graceSubs.map((s) => (
              <div key={s.id} className="flex items-center justify-between rounded-lg bg-warning-soft px-3 py-2 text-sm">
                <span className="text-ink">
                  #{s.id} · {s.userName} · {s.planName}
                </span>
                <Badge className="bg-warning text-white">{formatCurrency(s.finalPrice)}</Badge>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader className="flex-row items-center justify-between space-y-0">
          <h2 className="font-display text-lg font-semibold text-ink">Recent payments</h2>
          <Link to="/admin/subscriptions" className="flex items-center gap-1 text-sm text-accent hover:underline">
            View subscriptions
            <ArrowUpRight className="size-3.5" />
          </Link>
        </CardHeader>
        <CardContent>
          {paymentsLoading ? (
            <Skeleton className="h-32 w-full" />
          ) : recentPayments.length === 0 ? (
            <p className="text-sm text-muted-foreground">No payments yet.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Subscription</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Amount</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {recentPayments.map((p) => (
                  <TableRow key={p.paymentId}>
                    <TableCell className="text-sm text-muted-foreground">
                      {formatDateTime(p.paymentDate)}
                    </TableCell>
                    <TableCell>#{p.subscriptionId}</TableCell>
                    <TableCell>{p.paymentType}</TableCell>
                    <TableCell className="font-medium text-ink">{formatCurrency(p.amount)}</TableCell>
                    <TableCell>
                      <Badge className={PAYMENT_STATUS_STYLES[p.paymentStatus] ?? ""}>
                        {p.paymentStatus}
                      </Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
