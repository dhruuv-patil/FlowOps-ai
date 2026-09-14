import * as React from "react";
import { cn } from "@/lib/utils";

export interface InputProps
  extends React.InputHTMLAttributes<HTMLInputElement> {
  isCode?: boolean;
}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, isCode, ...props }, ref) => {
    return (
      <input
        type={type}
        className={cn(
          "flex h-10 w-full rounded-inputs bg-white/[0.04] border border-white/[0.10] text-white/80 placeholder:text-white/30 px-3.5 py-2 text-sm",
          "transition-all duration-[180ms] focus-visible:outline-none focus-visible:border-white/[0.20] focus-visible:ring-1 focus-visible:ring-white/[0.10]",
          "disabled:cursor-not-allowed disabled:opacity-40",
          isCode && "font-mono text-xs",
          className,
        )}
        ref={ref}
        {...props}
      />
    );
  },
);
Input.displayName = "Input";

export { Input };
