"use client";

import * as React from "react";
import { Check, Copy } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Monospace code block with a copy-to-clipboard control.
 *
 * Falls back gracefully when the Clipboard API is unavailable (non-secure
 * contexts): the button reports failure rather than silently doing nothing.
 */
export interface CodeBlockProps {
  code: string;
  /** Small label shown top-left, e.g. "bash" or "python". */
  language?: string;
  className?: string;
}

export function CodeBlock({ code, language, className }: CodeBlockProps) {
  const [copied, setCopied] = React.useState(false);
  const [failed, setFailed] = React.useState(false);
  const timeout = React.useRef<ReturnType<typeof setTimeout> | null>(null);

  // Clear any pending reset timer on unmount.
  React.useEffect(() => {
    return () => {
      if (timeout.current) clearTimeout(timeout.current);
    };
  }, []);

  const copy = async () => {
    if (timeout.current) clearTimeout(timeout.current);

    try {
      if (!navigator.clipboard) throw new Error("Clipboard API unavailable");
      await navigator.clipboard.writeText(code);
      setCopied(true);
      setFailed(false);
    } catch {
      setCopied(false);
      setFailed(true);
    }

    timeout.current = setTimeout(() => {
      setCopied(false);
      setFailed(false);
    }, 2000);
  };

  return (
    <div
      className={cn(
        "group relative overflow-hidden rounded-lg border border-white/[0.08] bg-[#050505]",
        className,
      )}
    >
      {language && (
        <div className="border-b border-border px-4 py-2 font-mono text-[11px] uppercase tracking-wider text-muted-foreground">
          {language}
        </div>
      )}

      <button
        type="button"
        onClick={copy}
        aria-label={copied ? "Copied" : "Copy code"}
        className={cn(
          "absolute right-2 inline-flex items-center gap-1.5 rounded-md border border-border bg-background/80 px-2 py-1 font-mono text-[11px] text-muted-foreground backdrop-blur transition-colors hover:text-foreground",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
          language ? "top-9" : "top-2",
        )}
      >
        {copied ? (
          <>
            <Check className="size-3 text-success" />
            Copied
          </>
        ) : failed ? (
          <>
            <Copy className="size-3" />
            Press ⌘C
          </>
        ) : (
          <>
            <Copy className="size-3" />
            Copy
          </>
        )}
      </button>

      <pre className="scrollbar-thin overflow-x-auto px-4 py-3 pr-24">
        <code className="font-mono text-xs leading-relaxed text-foreground">
          {code}
        </code>
      </pre>
    </div>
  );
}
