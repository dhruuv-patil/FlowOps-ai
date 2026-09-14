import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-medium leading-4 tracking-tight transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 [&_svg]:size-3 [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        default: "bg-white/[0.06] text-white/60 border border-white/[0.08]",
        secondary: "bg-white/[0.04] text-white/50 border border-white/[0.06]",
        outline: "border-white/[0.12] text-white/60",
        success: "bg-emerald-500/[0.10] text-[#4ade80] border border-emerald-500/[0.20]",
        warning: "bg-amber-500/[0.10] text-[#facc15] border border-amber-500/[0.20]",
        destructive: "bg-red-500/[0.10] text-[#f87171] border border-red-500/[0.20]",
        brand: "bg-brand-500/15 text-brand-400 border border-brand-500/[0.20]",
      },
    },
    defaultVariants: { variant: "default" },
  },
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return (
    <span className={cn(badgeVariants({ variant }), className)} {...props} />
  );
}

export { Badge, badgeVariants };
