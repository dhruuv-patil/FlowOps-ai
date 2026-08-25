"use client";

import { useEffect, useRef } from "react";

import { bootstrapSession } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";

/**
 * Runs the app-boot session recovery exactly once per mount (contract §6.1).
 *
 * The access token lives in memory only, so a reload starts anonymous; one
 * `POST /api/auth/refresh` restores both the token and a fresh user/org snapshot.
 * A React 18 StrictMode double-mount would otherwise fire two refreshes, and
 * refresh rotates the cookie — the second call would present an already-rotated
 * token and trip reuse detection. The ref guard makes it fire once.
 */
export function SessionBootstrapper() {
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    // Single-flight is enforced inside refreshSession(); this only avoids the
    // needless second attempt.
    if (useAuthStore.getState().status === "idle") {
      void bootstrapSession();
    }
  }, []);

  return null;
}
