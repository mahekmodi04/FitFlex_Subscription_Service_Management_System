import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { z } from "zod";
import { toast } from "sonner";
import { updateUser as updateUserRequest } from "@/api/users";
import { useAuth } from "@/context/AuthContext";
import { extractErrorMessage } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

const accountSchema = z.object({
  name: z.string().min(1, "Name is required").max(100),
  email: z.string().min(1, "Email is required").email("Enter a valid email"),
  password: z.string().min(8, "Password must be at least 8 characters"),
});

export default function Account() {
  const { user, updateUser } = useAuth();
  const [formError, setFormError] = useState("");

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(accountSchema),
    defaultValues: { name: user.name, email: user.email, password: "" },
  });

  const mutation = useMutation({
    mutationFn: (values) => updateUserRequest(user.id, values),
    onSuccess: (updated) => {
      updateUser({ name: updated.name, email: updated.email });
      toast.success("Profile updated");
    },
    onError: (error) => setFormError(extractErrorMessage(error)),
  });

  const onSubmit = (values) => {
    setFormError("");
    mutation.mutate(values);
  };

  return (
    <div className="mx-auto max-w-lg px-4 py-12 sm:px-6">
      <h1 className="font-display text-2xl font-bold text-ink">Account</h1>
      <p className="mt-1 text-muted-foreground">Update your profile details.</p>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="text-lg">Profile</CardTitle>
          <CardDescription>{user.role === "ADMIN" ? "Administrator" : "Member"}</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <div className="space-y-1.5">
              <Label htmlFor="name">Full name</Label>
              <Input id="name" {...register("name")} />
              {errors.name && <p className="text-sm text-danger">{errors.name.message}</p>}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" {...register("email")} />
              {errors.email && <p className="text-sm text-danger">{errors.email.message}</p>}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="password">Password</Label>
              <Input id="password" type="password" autoComplete="new-password" {...register("password")} />
              {errors.password ? (
                <p className="text-sm text-danger">{errors.password.message}</p>
              ) : (
                <p className="text-xs text-muted-foreground">
                  Re-enter your password to save changes — the API requires it on every update, even if
                  you&apos;re not changing it.
                </p>
              )}
            </div>

            {formError && <p className="text-sm text-danger">{formError}</p>}

            <Button type="submit" disabled={isSubmitting || mutation.isPending}>
              {mutation.isPending ? "Saving…" : "Save changes"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
