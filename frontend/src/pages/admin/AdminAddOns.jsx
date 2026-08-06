import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { Plus } from "lucide-react";
import { toast } from "sonner";
import { getAddOns, createAddOn } from "@/api/addons";
import { extractErrorMessage } from "@/api/client";
import { formatCurrency } from "@/lib/format";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";

const addOnSchema = z.object({
  name: z.string().min(1, "Name is required").max(100),
  description: z.string().optional(),
  unitName: z.string().min(1, "Unit name is required (e.g. sessions, passes)").max(30),
  unitPrice: z.coerce.number().positive("Price must be positive"),
});

const emptyValues = { name: "", description: "", unitName: "", unitPrice: "" };

export default function AdminAddOns() {
  const queryClient = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [formError, setFormError] = useState("");

  const { data: addOns, isLoading } = useQuery({ queryKey: ["addons"], queryFn: getAddOns });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(addOnSchema),
    defaultValues: emptyValues,
  });

  const createMutation = useMutation({
    mutationFn: (values) => createAddOn({ ...values, active: true }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["addons"] });
      setDialogOpen(false);
      reset(emptyValues);
      toast.success("Add-on created");
    },
    onError: (error) => setFormError(extractErrorMessage(error)),
  });

  const onSubmit = (values) => {
    setFormError("");
    createMutation.mutate({ ...values, description: values.description || null });
  };

  return (
    <div className="pb-12">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">Add-ons</h1>
          <p className="mt-1 text-muted-foreground">
            Catalog of add-ons members can attach to their subscription.
          </p>
        </div>
        <Dialog
          open={dialogOpen}
          onOpenChange={(open) => {
            setDialogOpen(open);
            if (open) {
              reset(emptyValues);
              setFormError("");
            }
          }}
        >
          <DialogTrigger asChild>
            <Button>
              <Plus className="size-4" />
              New add-on
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Create add-on</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
              <div className="space-y-1.5">
                <Label htmlFor="name">Name</Label>
                <Input id="name" {...register("name")} />
                {errors.name && <p className="text-sm text-danger">{errors.name.message}</p>}
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label htmlFor="unitName">Unit name</Label>
                  <Input id="unitName" placeholder="e.g. sessions" {...register("unitName")} />
                  {errors.unitName && <p className="text-sm text-danger">{errors.unitName.message}</p>}
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="unitPrice">Price per unit</Label>
                  <Input id="unitPrice" type="number" step="0.01" {...register("unitPrice")} />
                  {errors.unitPrice && (
                    <p className="text-sm text-danger">{errors.unitPrice.message}</p>
                  )}
                </div>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="description">Description</Label>
                <Textarea id="description" {...register("description")} />
              </div>
              {formError && <p className="text-sm text-danger">{formError}</p>}
              <DialogFooter>
                <Button type="submit" disabled={isSubmitting || createMutation.isPending}>
                  {createMutation.isPending ? "Saving…" : "Create"}
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
                <TableHead>Description</TableHead>
                <TableHead>Unit price</TableHead>
                <TableHead>Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {addOns?.map((addOn) => (
                <TableRow key={addOn.id}>
                  <TableCell className="font-medium text-ink">{addOn.name}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {addOn.description ?? "—"}
                  </TableCell>
                  <TableCell>
                    {formatCurrency(addOn.unitPrice)} / {addOn.unitName}
                  </TableCell>
                  <TableCell>
                    <Badge className={addOn.active ? "bg-success-soft text-success" : "bg-muted text-muted-foreground"}>
                      {addOn.active ? "Active" : "Inactive"}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>
      <p className="mt-3 text-xs text-muted-foreground">
        Editing and deactivating add-ons isn&apos;t supported by the backend yet — only creating new ones.
      </p>
    </div>
  );
}
