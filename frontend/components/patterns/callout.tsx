import * as React from "react";
import { ArrowUpRight, CircleCheck, Info, TriangleAlert } from "lucide-react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

/**
 * Tinted callout for inline guidance, with an optional documentation link.
 * Hook-free so it can render in server components.
 */
const calloutVariants = cva(
  "flex items-start gap-3 rounded-lg border px-4 py-3 text-sm",
  {
    variants: {
      variant: {
        info: "border-white/[0.10] bg-white/[0.03] text-white/70",
        warning: "border-amber-500/[0.20] bg-amber-500/[0.06] text-[#facc15]",
        success: "border-emerald-500/[0.20] bg-emerald-500/[0.06] text-[#4ade80]",
      },
    },
    defaultVariants: { variant: "info" },
  },
);

const ICONS = {
  info: Info,
  warning: TriangleAlert,
  success: CircleCheck,
} as const;

const ICON_TONE = {
  info: "text-white/50",
  warning: "text-[#facc15]",
  success: "text-[#4ade80]",
} as const;

export interface CalloutProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof calloutVariants> {
  /** Optional trailing link, e.g. "Read the docs". */
  linkHref?: string;
  linkLabel?: string;
}

export function Callout({
  variant,
  linkHref,
  linkLabel = "Read the docs",
  className,
  children,
  ...props
}: CalloutProps) {
  const tone = variant ?? "info";
  const Icon = ICONS[tone];

  return (
    <div className={cn(calloutVariants({ variant }), className)} {...props}>
      <Icon className={cn("mt-0.5 size-4 shrink-0", ICON_TONE[tone])} />

      <div className="flex-1 leading-relaxed">{children}</div>

      {linkHref && (
        <a
          href={linkHref}
          target="_blank"
          rel="noreferrer"
          className="inline-flex shrink-0 items-center gap-1 font-mono text-xs text-white/60 underline-offset-4 hover:text-white/90 hover:underline"
        >
          {linkLabel}
          <ArrowUpRight className="size-3" />
        </a>
      )}
    </div>
  );
}
