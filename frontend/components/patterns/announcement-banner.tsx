"use client";

import * as React from "react";
import { ArrowRight, Sparkles, X } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Dismissible announcement banner.
 *
 * When `storageKey` is provided the dismissal persists in localStorage so the
 * banner stays closed across reloads. All storage access is guarded and wrapped
 * — Safari private mode throws on localStorage writes, and this must never take
 * the page down.
 */
export interface AnnouncementBannerProps {
  title?: string;
  children: React.ReactNode;
  actionHref?: string;
  actionLabel?: string;
  /** Persist dismissal under this key. Omit for session-only behavior. */
  storageKey?: string;
  className?: string;
}

function readDismissed(key: string | undefined): boolean {
  if (!key || typeof window === "undefined") return false;
  try {
    return window.localStorage.getItem(key) === "dismissed";
  } catch {
    return false;
  }
}

export function AnnouncementBanner({
  title,
  children,
  actionHref,
  actionLabel = "Learn more",
  storageKey,
  className,
}: AnnouncementBannerProps) {
  // Start visible on both server and first client render, then reconcile in an
  // effect. Reading localStorage during render would break hydration.
  const [dismissed, setDismissed] = React.useState(false);
  const [ready, setReady] = React.useState(false);

  React.useEffect(() => {
    setDismissed(readDismissed(storageKey));
    setReady(true);
  }, [storageKey]);

  const dismiss = () => {
    setDismissed(true);
    if (!storageKey) return;
    try {
      window.localStorage.setItem(storageKey, "dismissed");
    } catch {
      // Storage unavailable (private mode / quota) — dismissal stays session-only.
    }
  };

  // Avoid a flash of a banner the user already dismissed.
  if (!ready || dismissed) return null;

  return (
    <div
      role="status"
      className={cn(
        "flex items-center gap-3 rounded-lg border border-white/[0.10] bg-white/[0.03] px-4 py-3 text-sm",
        className,
      )}
    >
      <Sparkles className="size-4 shrink-0 text-white/50" />

      <p className="flex-1 leading-relaxed text-foreground">
        {title && <span className="font-medium">{title} </span>}
        <span className="text-muted-foreground">{children}</span>
      </p>

      {actionHref && (
        <a
          href={actionHref}
          className="inline-flex shrink-0 items-center gap-1 font-mono text-xs text-white/60 underline-offset-4 hover:text-white/90 hover:underline"
        >
          {actionLabel}
          <ArrowRight className="size-3" />
        </a>
      )}

      <button
        type="button"
        onClick={dismiss}
        aria-label="Dismiss announcement"
        className="shrink-0 rounded-md p-1 text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
      >
        <X className="size-4" />
      </button>
    </div>
  );
}
