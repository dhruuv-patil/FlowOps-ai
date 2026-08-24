import { cn } from "@/lib/utils";

/**
 * FlowOps brand mark — a compact workflow glyph (a trigger node branching into
 * two actions) on a periwinkle→blue gradient tile.
 */
export function LogoMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 32 32"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={cn("h-7 w-7", className)}
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="flowops-mark" x1="0" y1="0" x2="32" y2="32">
          <stop offset="0%" stopColor="#9EC5F5" />
          <stop offset="100%" stopColor="#2563EB" />
        </linearGradient>
      </defs>
      <rect width="32" height="32" rx="8" fill="url(#flowops-mark)" />
      <g stroke="white" strokeWidth="1.6" strokeLinecap="round">
        <path d="M11 16H16" opacity="0.9" />
        <path d="M16 16C18 16 18 10.5 21 10.5" opacity="0.9" />
        <path d="M16 16C18 16 18 21.5 21 21.5" opacity="0.9" />
      </g>
      <g fill="white">
        <circle cx="10" cy="16" r="2.6" />
        <circle cx="22" cy="10.5" r="2.2" />
        <circle cx="22" cy="21.5" r="2.2" />
      </g>
    </svg>
  );
}

export function Logo({ className }: { className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-2", className)}>
      <LogoMark />
      <span className="text-lg font-semibold tracking-tight">FlowOps</span>
    </span>
  );
}
