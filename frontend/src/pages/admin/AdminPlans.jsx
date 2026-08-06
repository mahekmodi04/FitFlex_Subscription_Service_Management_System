import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { getPlans, createPlan, updatePlan, deletePlan } from "@/api/plans";
import { extractErrorMessage } from "@/api/client";
import { formatCurrency } from "@/lib/format";
import { PlanType } from "@/types/enums";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

const planSchema = z.object({
  name: z.string().min(1, "Name is required").max(50),
  price: z.coerce.number().positive("Price must be positive"),
  durationDays: z.coerce.number().int().min(30, "Minimum 30 days"),
  description: z.string().optional(),
  tier: z.enum([PlanType.BASIC, PlanType.PRO, PlanType.PREMIUM]),
  active: z.boolean(),
});

const emptyValues = { name: "", price: "", durationDays: 30, description: "", tier: PlanType.BASIC, active: true };

export default function AdminPlans() {
  const queryClient = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingPlan, setEditingPlan] = useState(null);
  const [formError, setFormError] = useState("");

  const { data: plans, isLoading } = useQuery({ queryKey: ["plans"], queryFn: getPlans });

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(planSchema),
    defaultValues: emptyValues,
  });

  const openCreate = () => {
    setEditingPlan(null);
    reset(emptyValues);
    setFormError("");
    setDialogOpen(true);
  };

  const openEdit = (plan) => {
    setEditingPlan(plan);
    reset({
      name: plan.name,
      price: plan.price,
      durationDays: plan.durationDays,
      description: plan.description ?? "",
      tier: plan.tier,
      active: plan.active,
    });
    setFormError("");
    setDialogOpen(true);
  };

  const saveMutation = useMutation({
    mutationFn: (values) =>
      editingPlan ? updatePlan(editingPlan.id, values) : createPlan(values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["plans"] });
      setDialogOpen(false);
      toast.success(editingPlan ? "Plan updated" : "Plan created");
    },
    onError: (error) => setFormError(extractErrorMessage(error)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => deletePlan(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["plans"] });
      toast.success("Plan deleted");
    },
    onError: (error) => toast.error(extractErrorMessage(error)),
  });

  const onSubmit = (values) => {
    setFormError("");
    saveMutation.mutate({ ...values, description: values.description || null });
  };

  return (
    <div className="pb-12">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Plans</h1>
          <p className="mt-1 text-muted-foreground">Manage membership plans.</p>
        </div>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button onClick={openCreate}>
              <Plus className="size-4" />
              New plan
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{editingPlan ? "Edit plan" : "Create plan"}</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
              <div className="space-y-1.5">
                <Label htmlFor="name">Name</Label>
                <Input id="name" {...register("name")} />
                {errors.name && <p className="text-sm text-danger">{errors.name.message}</p>}
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label htmlFor="price">Price</Label>
                  <Input id="price" type="number" step="0.01" {...register("price")} />
                  {errors.price && <p className="text-sm text-danger">{errors.price.message}</p>}
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="durationDays">Duration (days)</Label>
                  <Input id="durationDays" type="number" {...register("durationDays")} />
                  {errors.durationDays && (
                    <p className="text-sm text-danger">{errors.durationDays.message}</p>
                  )}
                </div>
              </div>
              <div className="space-y-1.5">
                <Label>Tier</Label>
                <Select value={watch("tier")} onValueChange={(v) => setValue("tier", v)}>
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={PlanType.BASIC}>Basic</SelectItem>
                    <SelectItem value={PlanType.PRO}>Pro</SelectItem>
                    <SelectItem value={PlanType.PREMIUM}>Premium</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="description">Description</Label>
                <Textarea id="description" {...register("description")} />
              </div>
              <div className="flex items-center justify-between rounded-lg border border-border px-3 py-2.5">
                <Label htmlFor="active">Active</Label>
                <Switch
                  id="active"
                  checked={watch("active")}
                  onCheckedChange={(v) => setValue("active", v)}
                />
              </div>
              {formError && <p className="text-sm text-danger">{formError}</p>}
              <DialogFooter>
                <Button type="submit" disabled={isSubmitting || saveMutation.isPending}>
                  {saveMutation.isPending ? "Saving…" : "Save"}
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      <div className="overflow-x-auto rounded-xl border border-border bg-white">
        {isLoading ? (
          <div className="space-y-2 p-4">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Tier</TableHead>
                <TableHead>Price</TableHead>
                <TableHead>Duration</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {plans?.map((plan) => (
                <TableRow key={plan.id}>
                  <TableCell className="font-medium text-ink">{plan.name}</TableCell>
                  <TableCell>{plan.tier}</TableCell>
                  <TableCell>{formatCurrency(plan.price)}</TableCell>
                  <TableCell>{plan.durationDays} days</TableCell>
                  <TableCell>
                    <Badge className={plan.active ? "bg-success-soft text-success" : "bg-muted text-muted-foreground"}>
                      {plan.active ? "Active" : "Inactive"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button size="icon-sm" variant="ghost" onClick={() => openEdit(plan)}>
                        <Pencil className="size-3.5" />
                      </Button>
                      <AlertDialog>
                        <AlertDialogTrigger asChild>
                          <Button size="icon-sm" variant="ghost" className="text-danger hover:text-danger">
                            <Trash2 className="size-3.5" />
                          </Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                          <AlertDialogHeader>
                            <AlertDialogTitle>Delete &ldquo;{plan.name}&rdquo;?</AlertDialogTitle>
                            <AlertDialogDescription>
                              This can&apos;t be undone. Existing subscribers won&apos;t be affected.
                            </AlertDialogDescription>
                          </AlertDialogHeader>
                          <AlertDialogFooter>
                            <AlertDialogCancel>Cancel</AlertDialogCancel>
                            <AlertDialogAction
                              className="bg-danger text-white hover:bg-danger/90"
                              onClick={() => deleteMutation.mutate(plan.id)}
                            >
                              Delete
                            </AlertDialogAction>
                          </AlertDialogFooter>
                        </AlertDialogContent>
                      </AlertDialog>
                    </div>
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
