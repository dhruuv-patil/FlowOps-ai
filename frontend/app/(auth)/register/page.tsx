"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";

import { getErrorMessage, getFieldErrors, register } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import { registerSchema, toFieldErrors } from "@/lib/validations";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function RegisterPage() {
  const router = useRouter();
  const status = useAuthStore((s) => s.status);

  const [values, setValues] = useState({
    fullName: "",
    organizationName: "",
    email: "",
    password: "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (status === "authenticated") router.replace("/dashboard");
  }, [status, router]);

  function set(key: keyof typeof values) {
    return (e: React.ChangeEvent<HTMLInputElement>) =>
      setValues((v) => ({ ...v, [key]: e.target.value }));
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setFormError(null);

    const parsed = registerSchema.safeParse(values);
    if (!parsed.success) {
      setErrors(toFieldErrors(parsed.error));
      return;
    }
    setErrors({});
    setSubmitting(true);
    try {
      await register(parsed.data);
      router.replace("/dashboard");
    } catch (err) {
      const fieldErrors = getFieldErrors(err);
      if (Object.keys(fieldErrors).length > 0) setErrors(fieldErrors);
      setFormError(getErrorMessage(err, "Could not create your account."));
    } finally {
      setSubmitting(false);
    }
  }

  const fields = [
    {
      key: "fullName" as const,
      label: "Full name",
      type: "text",
      autoComplete: "name",
      autoFocus: true,
    },
    {
      key: "organizationName" as const,
      label: "Organization name",
      type: "text",
      autoComplete: "organization",
    },
    { key: "email" as const, label: "Email", type: "email", autoComplete: "email" },
    {
      key: "password" as const,
      label: "Password",
      type: "password",
      autoComplete: "new-password",
    },
  ];

  return (
    <div>
      <h1 className="text-2xl font-semibold tracking-tight">
        Create your workspace
      </h1>
      <p className="mt-2 text-sm text-muted-foreground">
        Start building and running AI workflows in minutes.
      </p>

      <form onSubmit={onSubmit} noValidate className="mt-8 space-y-4">
        {formError && (
          <div
            role="alert"
            className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive"
          >
            {formError}
          </div>
        )}

        {fields.map((f) => (
          <div key={f.key} className="space-y-2">
            <Label htmlFor={f.key}>{f.label}</Label>
            <Input
              id={f.key}
              type={f.type}
              autoComplete={f.autoComplete}
              autoFocus={f.autoFocus}
              value={values[f.key]}
              onChange={set(f.key)}
              aria-invalid={!!errors[f.key]}
              aria-describedby={errors[f.key] ? `${f.key}-error` : undefined}
            />
            {f.key === "password" && !errors.password && (
              <p className="text-xs text-muted-foreground">
                At least 8 characters, with a letter and a number.
              </p>
            )}
            {errors[f.key] && (
              <p id={`${f.key}-error`} className="text-xs text-destructive">
                {errors[f.key]}
              </p>
            )}
          </div>
        ))}

        <Button type="submit" className="w-full" disabled={submitting}>
          {submitting && <Loader2 className="animate-spin" />}
          Create account
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Already have an account?{" "}
        <Link href="/login" className="font-medium text-primary hover:underline">
          Log in
        </Link>
      </p>
    </div>
  );
}
