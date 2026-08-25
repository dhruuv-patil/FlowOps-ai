"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

import { useAuthStore } from "@/lib/auth-store";
import { Logo } from "@/components/brand/logo";

/**
 * Client-side gate for the authenticated app (contract §6.3: route protection is
 * client-side, never in middleware, because the token lives in memory).
 *
 * - `idle` — boot refresh has not settled. Show a splash, never the app and
 *   never a redirect (a redirect here would bounce a logged-in user on reload).
 * - `unauthenticated` — send to /login.
 * - `authenticated` — render the app.
 */
export function AppGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const status = useAuthStore((s) => s.status);

  useEffect(() => {
    if (status === "unauthenticated") router.replace("/login");
  }, [status, router]);

  if (status !== "authenticated") {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <Logo />
          <div className="h-1 w-24 overflow-hidden rounded-full bg-muted">
            <div className="h-full w-1/2 animate-pulse rounded-full bg-primary" />
          </div>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
