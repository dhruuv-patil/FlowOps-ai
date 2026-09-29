import type { ReactNode } from "react";

import { cn } from "@/lib/utils";

type MetricTone = "neutral" | "success" | "warning" | "danger";

type MetricItem = {
  label: string;
  value: ReactNode;
  detail?: string;
  tone?: MetricTone;
};

export function AppPageHeader({
  eyebrow,
  title,
  description,
  actions,
  className,
}: {
  eyebrow?: ReactNode;
  title: ReactNode;
  description?: ReactNode;
  actions?: ReactNode;
  className?: string;
}) {
  return (
    <header
      className={cn(
        "flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between",
        className,
      )}
    >
      <div className="min-w-0">
        {eyebrow && <div className="mono-eyebrow">{eyebrow}</div>}
        <h1 className="mt-1.5 text-[24px] font-semibold tracking-[-0.035em] text-white">
          {title}
        </h1>
        {description && (
          <p className="mt-1.5 max-w-2xl text-sm leading-5 text-white/40">
            {description}
          </p>
        )}
      </div>
      {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
    </header>
  );
}

export function MetricStrip({
  items,
  className,
}: {
  items: MetricItem[];
  className?: string;
}) {
  return (
    <dl
      className={cn(
        "grid overflow-hidden rounded-xl border border-white/[0.06] bg-[#0a0a0c] sm:grid-cols-2 lg:grid-cols-4",
        className,
      )}
    >
      {items.map((item) => (
        <div
          key={item.label}
          className="min-h-[94px] border-b border-white/[0.05] px-4 py-3.5 last:border-b-0 sm:border-r sm:last:border-r-0 lg:border-b-0"
        >
          <dt className="text-[9px] font-medium uppercase tracking-[0.1em] text-white/25">
            {item.label}
          </dt>
          <dd className="mt-2 text-[24px] font-semibold leading-none tracking-[-0.045em] tabular-nums text-white/90">
            {item.value}
          </dd>
          {item.detail && (
            <div className="mt-2 flex items-center gap-1.5 text-[9px] text-white/25">
              <span
                aria-hidden="true"
                className={cn(
                  "size-1.5 rounded-full",
                  item.tone === "success"
                    ? "bg-emerald-400/70"
                    : item.tone === "warning"
                      ? "bg-amber-400/70"
                      : item.tone === "danger"
                        ? "bg-red-400/70"
                        : "bg-white/20",
                )}
              />
              {item.detail}
            </div>
          )}
        </div>
      ))}
    </dl>
  );
}

export function PageToolbar({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "flex flex-col gap-2.5 sm:flex-row sm:items-center",
        className,
      )}
    >
      {children}
    </div>
  );
}

export function AppPanel({
  title,
  description,
  actions,
  children,
  className,
}: {
  title?: ReactNode;
  description?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section
      className={cn(
        "overflow-hidden rounded-xl border border-white/[0.07] bg-[#0d0d10]",
        className,
      )}
    >
      {(title || description || actions) && (
        <div className="flex flex-wrap items-start justify-between gap-3 border-b border-white/[0.06] px-4 py-3.5">
          <div>
            {title && <h2 className="text-sm font-medium text-white/85">{title}</h2>}
            {description && <p className="mt-1 text-xs text-white/35">{description}</p>}
          </div>
          {actions}
        </div>
      )}
      {children}
    </section>
  );
}

export function AppStateSurface({
  kind,
  title,
  description,
  action,
  className,
}: {
  kind: "loading" | "empty" | "error";
  title: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
  className?: string;
}) {
  return (
    <section
      className={cn(
        "flex flex-col items-center justify-center rounded-xl border px-6 py-14 text-center",
        kind === "error"
          ? "border-red-400/15 bg-red-400/[0.03]"
          : "border-dashed border-white/[0.09] bg-[#0d0d10]",
        className,
      )}
    >
      <h2 className="text-sm font-medium text-white/75">{title}</h2>
      {description && <p className="mt-1.5 max-w-md text-xs leading-5 text-white/35">{description}</p>}
      {action && <div className="mt-5">{action}</div>}
    </section>
  );
}
