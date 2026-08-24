"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

/**
 * Segmented pill toggle — a compact row of mutually exclusive options.
 *
 * Works controlled (pass `value` + `onValueChange`) or uncontrolled (pass
 * `defaultValue`). Implemented as a radiogroup so keyboard and screen-reader
 * users get proper semantics.
 */
export interface SegmentedOption {
  value: string;
  label: React.ReactNode;
  /** Optional leading icon. */
  icon?: React.ReactNode;
  disabled?: boolean;
}

export interface SegmentedToggleProps {
  options: SegmentedOption[];
  value?: string;
  defaultValue?: string;
  onValueChange?: (value: string) => void;
  /** Accessible name for the group. */
  label?: string;
  className?: string;
}

export function SegmentedToggle({
  options,
  value,
  defaultValue,
  onValueChange,
  label,
  className,
}: SegmentedToggleProps) {
  const [internal, setInternal] = React.useState(
    defaultValue ?? options[0]?.value ?? "",
  );

  // Controlled when `value` is supplied; otherwise track internally.
  const isControlled = value !== undefined;
  const selected = isControlled ? value : internal;

  const select = (next: string) => {
    if (!isControlled) setInternal(next);
    onValueChange?.(next);
  };

  return (
    <div
      role="radiogroup"
      aria-label={label}
      className={cn("inline-flex flex-wrap items-center gap-2", className)}
    >
      {options.map((option) => {
        const active = option.value === selected;

        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={active}
            disabled={option.disabled}
            onClick={() => select(option.value)}
            className={cn(
              "inline-flex items-center gap-2 rounded-md border px-3 py-1.5 font-mono text-xs transition-colors",
              "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
              "disabled:pointer-events-none disabled:opacity-50",
              active
                ? "border-primary/50 bg-primary/15 text-foreground"
                : "border-border bg-card text-muted-foreground hover:bg-secondary hover:text-foreground",
            )}
          >
            {option.icon && (
              <span className="[&_svg]:size-3.5 [&_svg]:shrink-0">
                {option.icon}
              </span>
            )}
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
