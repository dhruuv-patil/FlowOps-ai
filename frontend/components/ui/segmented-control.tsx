"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

interface SegmentedControlProps<T extends string = string> {
  /** Array of tab values and labels */
  tabs: readonly { value: T; label: string }[];
  /** Currently active tab value */
  value: T;
  /** Callback when tab changes */
  onValueChange: (value: T) => void;
  /** Additional className for the container */
  className?: string;
  /** Whether the control is disabled */
  disabled?: boolean;
}

/**
 * A compact segmented control (pill-style) for switching between views.
 * Features:
 * - Rounded pill container with dark translucent background
 * - Thin subtle border
 * - Active tab uses bright filled pill inside container
 * - Inactive tabs remain muted gray
 * - Smooth transition when switching
 * - No large colored tabs or excessive shadows
 */
export function SegmentedControl<T extends string>({
  tabs,
  value,
  onValueChange,
  className,
  disabled = false,
}: SegmentedControlProps<T>) {
  const [activeTab, setActiveTab] = React.useState(value);

  // Sync with controlled value
  React.useEffect(() => {
    setActiveTab(value);
  }, [value]);

  return (
    <div
      className={cn(
        "inline-flex items-center gap-1 rounded-full bg-white/[0.035] border border-white/[0.10] p-1 transition-all duration-200",
        "h-9",
        disabled && "opacity-50 pointer-events-none",
        className,
      )}
      role="tablist"
      aria-label="View selector"
    >
      {tabs.map((tab, index) => (
        <button
          key={tab.value}
          type="button"
          role="tab"
          aria-selected={activeTab === tab.value}
          aria-controls={`panel-${tab.value}`}
          id={`tab-${tab.value}`}
          onClick={() => {
            if (!disabled) {
              setActiveTab(tab.value);
              onValueChange(tab.value);
            }
          }}
          disabled={disabled}
          className={cn(
            "relative z-10 flex items-center justify-center rounded-full px-4 text-xs font-medium tracking-tight transition-all duration-200 ease-out",
            "focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-white/20",
            "disabled:opacity-50 disabled:pointer-events-none",
            "h-8",
            activeTab === tab.value
              ? "bg-white text-[#050505] shadow-sm"
              : "text-white/45 hover:text-white/75",
          )}
          style={{
            // Equal width for all tabs
            flex: 1,
            minWidth: 0,
          }}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}