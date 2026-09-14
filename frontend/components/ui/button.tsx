import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg text-sm font-medium tracking-tight transition-all duration-[180ms] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/60 focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:pointer-events-none disabled:opacity-40 [&_svg]:size-4 [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        default:
          "bg-white/90 text-[#050505] hover:bg-white border border-white/[0.18] shadow-none hover:shadow-[inset_0_1px_0_rgba(255,255,255,0.7),0_12px_30px_rgba(0,0,0,0.28)] hover:-translate-y-px",
        outline:
          "bg-white/[0.035] text-white/78 border border-white/[0.13] hover:bg-white/[0.075] hover:border-white/[0.20] hover:text-white hover:-translate-y-px",
        secondary:
          "bg-white/[0.035] text-white/78 border border-white/[0.13] hover:bg-white/[0.075] hover:text-white",
        ghost: "text-white/60 hover:bg-white/[0.06] hover:text-white/90",
        link: "text-white/60 underline-offset-4 hover:text-white/90",
        destructive:
          "bg-red-500/90 text-white hover:bg-red-500 hover:-translate-y-px",
        contrast:
          "bg-white/90 text-[#050505] hover:bg-white border border-white/[0.18] shadow-none hover:shadow-[inset_0_1px_0_rgba(255,255,255,0.7),0_12px_30px_rgba(0,0,0,0.28)] hover:-translate-y-px",
      },
      size: {
        default: "h-9 px-5 py-2",
        sm: "h-8 rounded-lg px-3 text-xs",
        lg: "h-10 rounded-lg px-7 text-base",
        xl: "h-11 rounded-lg px-9 text-base",
        icon: "h-9 w-9",
      },
    },
    defaultVariants: { variant: "default", size: "default" },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    );
  },
);
Button.displayName = "Button";

export { Button, buttonVariants };
