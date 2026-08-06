import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { getCoupons, createCoupon, updateCoupon, deleteCoupon } from "@/api/coupons";
import { extractErrorMessage } from "@/api/client";
import { formatCurrency, formatDate } from "@/lib/format";
import { CouponType } from "@/types/enums";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
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

const couponSchema = z
  .object({
    code: z.string().min(1, "Code is required").max(30),
    type: z.enum([CouponType.PERCENTAGE, CouponType.AMOUNT, CouponType.BOTH]),
    discountPercentage: z.coerce.number().min(0).max(100).optional().or(z.literal("")),
    discountAmount: z.coerce.number().min(0).optional().or(z.literal("")),
    usageLimit: z.coerce.number().int().positive("Usage limit must be positive"),
    expiryDate: z.string().min(1, "Expiry date is required"),
    active: z.boolean(),
  })
  .refine(
    (v) =>
      v.type === CouponType.AMOUNT
        ? v.discountAmount !== "" && v.discountAmount != null
        : v.type === CouponType.PERCENTAGE
          ? v.discountPercentage !== "" && v.discountPercentage != null
          : (v.discountPercentage !== "" && v.discountPercentage != null) ||
            (v.discountAmount !== "" && v.discountAmount != null),
    { message: "Provide the discount value(s) this coupon type needs", path: ["discountAmount"] }
  );

const emptyValues = {
  code: "",
  type: CouponType.PERCENTAGE,
  discountPercentage: "",
  discountAmount: "",
  usageLimit: 100,
  expiryDate: "",
  active: true,
};

export default function AdminCoupons() {
  const queryClient = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingCoupon, setEditingCoupon] = useState(null);
  const [formError, setFormError] = useState("");

  const { data: coupons, isLoading } = useQuery({ queryKey: ["coupons"], queryFn: getCoupons });

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(couponSchema),
    defaultValues: emptyValues,
  });

  const type = watch("type");

  const openCreate = () => {
    setEditingCoupon(null);
    reset(emptyValues);
    setFormError("");
    setDialogOpen(true);
  };

  const openEdit = (coupon) => {
    setEditingCoupon(coupon);
    reset({
      code: coupon.code,
      type: coupon.type,
      discountPercentage: coupon.discountPercentage ?? "",
      discountAmount: coupon.discountAmount ?? "",
      usageLimit: coupon.usageLimit,
      expiryDate: coupon.expiryDate,
      active: coupon.active,
    });
    setFormError("");
    setDialogOpen(true);
  };

  const saveMutation = useMutation({
    mutationFn: (values) =>
      editingCoupon ? updateCoupon(editingCoupon.id, values) : createCoupon(values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["coupons"] });
      setDialogOpen(false);
      toast.success(editingCoupon ? "Coupon updated" : "Coupon created");
    },
    onError: (error) => setFormError(extractErrorMessage(error)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => deleteCoupon(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["coupons"] });
      toast.success("Coupon deleted");
    },
    onError: (error) => toast.error(extractErrorMessage(error)),
  });

  const onSubmit = (values) => {
    setFormError("");
    // only keep the discount field(s) this coupon type actually uses - switching type in the
    // form doesn't clear the hidden field's value, so without this a coupon edited from BOTH
    // to AMOUNT would still save its old leftover discountPercentage
    const usesPercentage = values.type === CouponType.PERCENTAGE || values.type === CouponType.BOTH;
    const usesAmount = values.type === CouponType.AMOUNT || values.type === CouponType.BOTH;
    saveMutation.mutate({
      ...values,
      discountPercentage: usesPercentage && values.discountPercentage !== "" ? values.discountPercentage : null,
      discountAmount: usesAmount && values.discountAmount !== "" ? values.discountAmount : null,
      usedCount: editingCoupon?.usedCount ?? 0,
    });
  };

  return (
    <div className="pb-12">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Coupons</h1>
          <p className="mt-1 text-muted-foreground">Manage discount codes.</p>
        </div>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button onClick={openCreate}>
              <Plus className="size-4" />
              New coupon
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{editingCoupon ? "Edit coupon" : "Create coupon"}</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
              <div className="space-y-1.5">
                <Label htmlFor="code">Code</Label>
                <Input id="code" {...register("code")} className="uppercase" />
                {errors.code && <p className="text-sm text-danger">{errors.code.message}</p>}
              </div>
              <div className="space-y-1.5">
                <Label>Type</Label>
                <Select value={type} onValueChange={(v) => setValue("type", v)}>
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={CouponType.PERCENTAGE}>Percentage</SelectItem>
                    <SelectItem value={CouponType.AMOUNT}>Fixed amount</SelectItem>
                    <SelectItem value={CouponType.BOTH}>Both</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                {(type === CouponType.PERCENTAGE || type === CouponType.BOTH) && (
                  <div className="space-y-1.5">
                    <Label htmlFor="discountPercentage">Discount %</Label>
                    <Input id="discountPercentage" type="number" step="0.01" {...register("discountPercentage")} />
                  </div>
                )}
                {(type === CouponType.AMOUNT || type === CouponType.BOTH) && (
                  <div className="space-y-1.5">
                    <Label htmlFor="discountAmount">Discount amount</Label>
                    <Input id="discountAmount" type="number" step="0.01" {...register("discountAmount")} />
                  </div>
                )}
              </div>
              {errors.discountAmount && (
                <p className="text-sm text-danger">{errors.discountAmount.message}</p>
              )}
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label htmlFor="usageLimit">Usage limit</Label>
                  <Input id="usageLimit" type="number" {...register("usageLimit")} />
                  {errors.usageLimit && (
                    <p className="text-sm text-danger">{errors.usageLimit.message}</p>
                  )}
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="expiryDate">Expiry date</Label>
                  <Input id="expiryDate" type="date" {...register("expiryDate")} />
                  {errors.expiryDate && (
                    <p className="text-sm text-danger">{errors.expiryDate.message}</p>
                  )}
                </div>
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
                <TableHead>Code</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Discount</TableHead>
                <TableHead>Usage</TableHead>
                <TableHead>Expires</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {coupons?.map((coupon) => (
                <TableRow key={coupon.id}>
                  <TableCell className="font-medium text-ink">{coupon.code}</TableCell>
                  <TableCell>{coupon.type}</TableCell>
                  <TableCell>
                    {coupon.type !== CouponType.AMOUNT && coupon.discountPercentage
                      ? `${coupon.discountPercentage}%`
                      : ""}
                    {coupon.type === CouponType.BOTH && coupon.discountPercentage && coupon.discountAmount
                      ? " + "
                      : ""}
                    {coupon.type !== CouponType.PERCENTAGE && coupon.discountAmount
                      ? formatCurrency(coupon.discountAmount)
                      : ""}
                  </TableCell>
                  <TableCell>
                    {coupon.usedCount} total · {coupon.usageLimit}/user
                  </TableCell>
                  <TableCell>{formatDate(coupon.expiryDate)}</TableCell>
                  <TableCell>
                    <Badge className={coupon.active ? "bg-success-soft text-success" : "bg-muted text-muted-foreground"}>
                      {coupon.active ? "Active" : "Inactive"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button size="icon-sm" variant="ghost" onClick={() => openEdit(coupon)}>
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
                            <AlertDialogTitle>Delete &ldquo;{coupon.code}&rdquo;?</AlertDialogTitle>
                            <AlertDialogDescription>This can&apos;t be undone.</AlertDialogDescription>
                          </AlertDialogHeader>
                          <AlertDialogFooter>
                            <AlertDialogCancel>Cancel</AlertDialogCancel>
                            <AlertDialogAction
                              className="bg-danger text-white hover:bg-danger/90"
                              onClick={() => deleteMutation.mutate(coupon.id)}
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
