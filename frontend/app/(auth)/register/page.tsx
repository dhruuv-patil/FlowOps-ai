"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
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
  const searchParams = useSearchParams();
  const status = useAuthStore((s) => s.status);
  const next = searchParams.get("next") ?? "/dashboard";

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
    if (status === "authenticated") {
      router.replace(next);
    }
  }, [status, router, next]);

  function set(key: keyof typeof values) {
    return (e: React.ChangeEvent<HTMLInputElement>) => {
      setValues((v) => ({ ...v, [key]: e.target.value }));
      if (formError) setFormError(null);
      if (errors[key])
        setErrors((prev) => {
          const n = { ...prev };
          delete n[key];
          return n;
        });
    };
  }

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    // Guard against duplicate submissions while a request is in flight.
    if (submitting) return;
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
      router.replace(next);
    } catch (err) {
      const fieldErrors = getFieldErrors(err);

      if (Object.keys(fieldErrors).length > 0) {
        setErrors(fieldErrors);
      }

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
      placeholder: "Your name",
    },
    {
      key: "organizationName" as const,
      label: "Organization name",
      type: "text",
      autoComplete: "organization",
      placeholder: "Your company",
    },
    {
      key: "email" as const,
      label: "Email",
      type: "email",
      autoComplete: "email",
      placeholder: "you@company.com",
    },
    {
      key: "password" as const,
      label: "Password",
      type: "password",
      autoComplete: "new-password",
      placeholder: "Create a password",
    },
  ];

  return (
    <div className="w-full">
      <div className="mb-8 text-center">
        <h1 className="text-[30px] font-semibold tracking-[-0.035em] text-foreground sm:text-[32px]">
          Create your FlowOps account
        </h1>

        <p className="mt-2 text-sm text-muted-foreground">
          Start building and running workflows in minutes.
        </p>
      </div>

      <div className="rounded-lg border border-white/[0.10] bg-[#050505] p-7 shadow-none">
        <div className="mb-7">
          <h2 className="text-[22px] font-semibold tracking-[-0.025em]">
            Create your workspace
          </h2>

          <p className="mt-1.5 text-sm text-muted-foreground">
            Set up your account to get started.
          </p>
        </div>

        {formError ? (
          <div
            role="alert"
            className="mb-5 rounded-md border border-red-500/20 bg-red-500/[0.06] px-3.5 py-3 text-sm text-destructive"
          >
            {formError}
          </div>
        ) : null}

        <form onSubmit={onSubmit} noValidate className="space-y-5">
          {fields.map((field) => (
            <div key={field.key} className="space-y-2">
              <Label
                htmlFor={field.key}
                className="text-sm"
              >
                {field.label}
              </Label>

              <Input
                id={field.key}
                type={field.type}
                autoComplete={field.autoComplete}
                autoFocus={field.autoFocus}
                value={values[field.key]}
                onChange={set(field.key)}
                aria-invalid={!!errors[field.key]}
                aria-describedby={
                  errors[field.key] ? `${field.key}-error` : undefined
                }
                placeholder={field.placeholder}
                className="h-11 rounded-md border-white/[0.11] bg-[#080808]"
                disabled={submitting}
              />

              {field.key === "password" && !errors.password ? (
                <p className="text-[11px] leading-5 text-muted-foreground/55">
                  At least 8 characters, with a letter and a number.
                </p>
              ) : null}

              {errors[field.key] ? (
                <p
                  id={`${field.key}-error`}
                  className="text-xs text-destructive"
                >
                  {errors[field.key]}
                </p>
              ) : null}
            </div>
          ))}

          <Button
            type="submit"
            className="h-11 w-full rounded-md bg-[#f2f2f0] text-sm font-medium text-[#080808] hover:bg-white"
            disabled={submitting}
          >
            {submitting ? (
              <>
                <Loader2 className="size-4 animate-spin" />
                Creating account
              </>
            ) : (
              "Create account"
            )}
          </Button>
        </form>

        <div className="my-6 flex items-center gap-3">
          <div className="h-px flex-1 bg-white/[0.08]" />
          <span className="text-[10px] text-muted-foreground/55">OR</span>
          <div className="h-px flex-1 bg-white/[0.08]" />
        </div>

        <p className="text-center text-sm text-muted-foreground">
          Already have an account?{" "}
          <Link
            href="/login"
            className="font-medium text-foreground hover:underline"
          >
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
