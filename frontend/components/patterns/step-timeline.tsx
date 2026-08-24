import * as React from "react";
import { cn } from "@/lib/utils";

/**
 * Numbered vertical timeline for step-by-step flows (setup guides, onboarding).
 *
 * Renders a hairline rail down the left with a marker per step. Presentational
 * and hook-free, so it works in server components; step content may itself
 * contain client components.
 */
export interface StepTimelineItem {
  /** Short step heading. */
  title: React.ReactNode;
  /** Optional supporting copy shown under the title. */
  description?: React.ReactNode;
  /** Step body — form controls, code blocks, callouts. */
  children?: React.ReactNode;
}

export interface StepTimelineProps
  extends React.HTMLAttributes<HTMLOListElement> {
  items: StepTimelineItem[];
  /** Show 1,2,3… in the markers instead of plain dots. */
  numbered?: boolean;
}

export function StepTimeline({
  items,
  numbered = false,
  className,
  ...props
}: StepTimelineProps) {
  return (
    <ol className={cn("relative space-y-8", className)} {...props}>
      {items.map((item, index) => {
        const isLast = index === items.length - 1;

        return (
          <li key={index} className="relative pl-10">
            {/* Rail segment — omitted on the final step so it doesn't dangle. */}
            {!isLast && (
              <span
                aria-hidden="true"
                className="absolute left-[11px] top-6 h-[calc(100%+2rem)] w-px bg-border"
              />
            )}

            {/* Marker */}
            <span
              aria-hidden="true"
              className={cn(
                "absolute left-0 top-1 flex h-[23px] w-[23px] items-center justify-center rounded-full border border-border bg-card",
                numbered && "font-mono text-[11px] text-muted-foreground",
              )}
            >
              {numbered ? (
                index + 1
              ) : (
                <span className="h-1.5 w-1.5 rounded-full bg-primary" />
              )}
            </span>

            <h3 className="text-sm font-medium text-foreground">{item.title}</h3>
            {item.description && (
              <p className="mt-1 text-sm text-muted-foreground">
                {item.description}
              </p>
            )}
            {item.children && <div className="mt-4">{item.children}</div>}
          </li>
        );
      })}
    </ol>
  );
}
