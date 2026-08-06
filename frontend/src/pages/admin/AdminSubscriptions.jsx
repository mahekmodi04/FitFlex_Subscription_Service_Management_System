import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { RefreshCw, RotateCw } from "lucide-react";
import { getAllSubscriptions, testRenewal, testRetry } from "@/api/subscriptions";
import { extractErrorMessage } from "@/api/client";
import { formatCurrency, formatDate } from "@/lib/format";
import { SubscriptionStatus } from "@/types/enums";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";

const STATUS_OPTIONS = ["ALL", ...Object.values(SubscriptionStatus)];

export default function AdminSubscriptions() {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const queryClient = useQueryClient();

  const { data: subscriptions, isLoading, isError } = useQuery({
    queryKey: ["admin", "subscriptions"],
    queryFn: getAllSubscriptions,
  });

  const invalidateAll = () => {
    queryClient.invalidateQueries({ queryKey: ["admin", "subscriptions"] });
    queryClient.invalidateQueries({ queryKey: ["subscription"] });
    queryClient.invalidateQueries({ queryKey: ["subscriptions"] });
  };

  const renewalMutation = useMutation({
    mutationFn: testRenewal,
    onSuccess: (updated) => {
      invalidateAll();
      toast.success(`Renewal simulated — subscription #${updated.id} is now ${updated.status}`);
    },
    onError: (error) => toast.error(extractErrorMessage(error)),
  });

  const retryMutation = useMutation({
    mutationFn: testRetry,
    onSuccess: (updated) => {
      invalidateAll();
      toast.success(`Retry simulated — subscription #${updated.id} is now ${updated.status}`);
    },
    onError: (error) => toast.error(extractErrorMessage(error)),
  });

  const filtered = useMemo(() => {
    if (!subscriptions) return [];
    return subscriptions
      .filter((s) => statusFilter === "ALL" || s.status === statusFilter)
      .filter((s) => s.userName.toLowerCase().includes(search.trim().toLowerCase()))
      .sort((a, b) => b.id - a.id);
  }, [subscriptions, statusFilter, search]);

  return (
    <div className="pb-12">
      <h1 className="font-display text-2xl font-bold text-ink">Subscriptions</h1>
      <p className="mt-1 text-muted-foreground">Every subscription across all members.</p>

      <div className="mt-6 flex flex-wrap gap-3">
        <Input
          placeholder="Search by member name…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="max-w-xs"
        />
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {STATUS_OPTIONS.map((s) => (
              <SelectItem key={s} value={s}>
                {s === "ALL" ? "All statuses" : s}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="mt-6 overflow-x-auto rounded-xl border border-border bg-white">
        {isLoading && (
          <div className="space-y-2 p-4">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        )}

        {isError && <p className="p-8 text-center text-danger">Couldn&apos;t load subscriptions.</p>}

        {!isLoading && !isError && filtered.length === 0 && (
          <p className="p-8 text-center text-muted-foreground">No subscriptions match your filters.</p>
        )}

        {!isLoading && !isError && filtered.length > 0 && (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Member</TableHead>
                <TableHead>Plan</TableHead>
                <TableHead>Price</TableHead>
                <TableHead>Billing cycle</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Test</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((s) => (
                <TableRow key={s.id}>
                  <TableCell className="text-muted-foreground">#{s.id}</TableCell>
                  <TableCell className="font-medium text-ink">{s.userName}</TableCell>
                  <TableCell>{s.planName}</TableCell>
                  <TableCell>{formatCurrency(s.finalPrice)}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatDate(s.startDate)} – {formatDate(s.endDate)}
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={s.status} />
                  </TableCell>
                  <TableCell className="text-right">
                    {s.status === SubscriptionStatus.ACTIVE && (
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            size="icon-sm"
                            variant="ghost"
                            disabled={renewalMutation.isPending}
                            onClick={() => renewalMutation.mutate(s.id)}
                          >
                            <RefreshCw className="size-3.5" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>
                          Simulate cycle end — fast-forwards the billing date and runs the real
                          renewal charge, exactly like the midnight scheduler would
                        </TooltipContent>
                      </Tooltip>
                    )}
                    {s.status === SubscriptionStatus.GRACE && (
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            size="icon-sm"
                            variant="ghost"
                            disabled={retryMutation.isPending}
                            onClick={() => retryMutation.mutate(s.id)}
                          >
                            <RotateCw className="size-3.5" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>
                          Simulate today&apos;s dunning retry — runs the real retry charge, exactly
                          like the daily scheduler would
                        </TooltipContent>
                      </Tooltip>
                    )}
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
