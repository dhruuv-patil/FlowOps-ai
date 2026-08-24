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
        info: "border-primary/25 bg-primary/10 text-foreground",
        warning: "border-warning/30 bg-warning/10 text-foreground",
        success: "border-success/30 bg-success/10 text-foreground",
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
  info: "text-primary",
  warning: "text-warning",
  success: "text-success",
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
          className="inline-flex shrink-0 items-center gap-1 font-mono text-xs text-primary underline-offset-4 hover:underline"
        >
          {linkLabel}
          <ArrowUpRight className="size-3" />
        </a>
      )}
    </div>
  );
}
