import * as React from "react";
import { cn } from "@/lib/utils";

/**
 * Decorative flowing-line background art.
 *
 * Curved paths converge from the left edge toward a junction, then fan back
 * out — a visual echo of a workflow graph. Purely presentational: rendered
 * behind content, never interactive, and hidden from assistive tech.
 *
 * Colors come from design tokens so it adapts to light/dark automatically.
 */
export function FlowLines({ className }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 1200 800"
      preserveAspectRatio="xMidYMid slice"
      className={cn(
        "pointer-events-none absolute inset-0 h-full w-full select-none",
        className,
      )}
    >
      <defs>
        {/* Lines fade out toward the right so content stays legible. */}
        <linearGradient id="flowlines-fade" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="hsl(var(--primary))" stopOpacity="0" />
          <stop offset="35%" stopColor="hsl(var(--primary))" stopOpacity="0.55" />
          <stop offset="70%" stopColor="hsl(var(--display))" stopOpacity="0.35" />
          <stop offset="100%" stopColor="hsl(var(--display))" stopOpacity="0" />
        </linearGradient>

        <radialGradient id="flowlines-glow">
          <stop offset="0%" stopColor="hsl(var(--display))" stopOpacity="0.5" />
          <stop offset="100%" stopColor="hsl(var(--display))" stopOpacity="0" />
        </radialGradient>
      </defs>

      {/* Soft bloom behind the convergence point. */}
      <circle cx="330" cy="470" r="190" fill="url(#flowlines-glow)" />

      <g
        fill="none"
        stroke="url(#flowlines-fade)"
        strokeWidth="1.25"
        strokeLinecap="round"
      >
        {/* Converging from upper-left into the junction. */}
        <path d="M -40 250 C 150 250, 220 470, 330 470" />
        <path d="M -40 330 C 170 330, 240 470, 330 470" />
        {/* The spine. */}
        <path d="M -40 470 H 330" />
        {/* Converging from lower-left. */}
        <path d="M -40 610 C 170 610, 240 470, 330 470" />
        <path d="M -40 690 C 150 690, 220 470, 330 470" />

        {/* Fanning back out to the right. */}
        <path d="M 330 470 C 520 470, 600 210, 820 210" />
        <path d="M 330 470 C 540 470, 640 350, 900 350" />
        <path d="M 330 470 H 1240" />
        <path d="M 330 470 C 540 470, 640 600, 900 600" />
        <path d="M 330 470 C 520 470, 600 730, 820 730" />
      </g>

      {/* Junction and endpoint nodes. */}
      <g fill="hsl(var(--display))">
        <circle cx="330" cy="470" r="5" opacity="0.9" />
        <circle cx="352" cy="470" r="3.5" opacity="0.55" />
        <circle cx="820" cy="210" r="3" opacity="0.4" />
        <circle cx="900" cy="350" r="3" opacity="0.4" />
        <circle cx="900" cy="600" r="3" opacity="0.4" />
        <circle cx="820" cy="730" r="3" opacity="0.4" />
      </g>
    </svg>
  );
}
