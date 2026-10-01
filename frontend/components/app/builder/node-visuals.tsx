"use client";

import * as React from "react";

import { cn } from "@/lib/utils";
import { resolveIcon } from "@/components/app/builder/shared";

/* ==========================================================================
   BRAND RULES  (slug on theSVG -> identifiers that map to it)
   Order matters: more specific rules first.
   Keywords match whole underscore-separated tokens, so "make" in
   "Make HTTP Request" no longer matches the Make brand.
   ========================================================================== */

const BRAND_RULES: ReadonlyArray<readonly [string, readonly string[]]> = [
  ["github-actions", ["github_actions", "githubactions"]],
  ["google-sheets", ["google_sheets", "googlesheets", "sheets"]],
  ["microsoft-teams", ["microsoft_teams", "ms_teams", "teams"]],
  ["amazon-s3", ["s3", "aws_s3"]],

  ["salesforce", ["salesforce"]],
  ["pagerduty", ["pagerduty"]],
  ["github", ["github"]],
  ["gitlab", ["gitlab"]],
  ["vercel", ["vercel"]],
  ["sentry", ["sentry"]],
  ["linear", ["linear"]],
  ["jira", ["jira"]],
  ["asana", ["asana"]],
  ["hubspot", ["hubspot"]],
  ["pipedrive", ["pipedrive"]],
  ["slack", ["slack"]],
  ["discord", ["discord"]],
  ["telegram", ["telegram"]],
  ["twilio", ["twilio"]],
  ["sendgrid", ["sendgrid"]],
  ["resend", ["resend"]],
  ["gmail", ["gmail"]],
  ["notion", ["notion"]],
  ["stripe", ["stripe"]],
  ["openai", ["openai"]],
  ["anthropic", ["anthropic"]],
  ["n8n", ["n8n"]],
  ["zapier", ["zapier"]],
  ["temporal", ["temporal"]],
  ["aws", ["aws"]],
];

function normalize(value?: string) {
  return (value ?? "")
    .trim()
    .toLowerCase()
    .replace(/[\s./-]+/g, "_");
}

export function resolveTheSvgSlug(
  type?: string,
  label?: string,
  icon?: string,
): string | null {
  for (const raw of [type, label, icon]) {
    const value = normalize(raw);
    if (!value) continue;

    // "make" is a common verb, so only match it as the whole value
    if (value === "make") return "make";

    const haystack = `_${value}_`;

    for (const [slug, keywords] of BRAND_RULES) {
      if (keywords.some((k) => haystack.includes(`_${k}_`))) {
        return slug;
      }
    }
  }

  return null;
}

/**
 * Brands whose default logo is black / near-black and disappears on a
 * dark surface. Rendered as white silhouettes.
 */
export const MONO_DARK_SLUGS = new Set([
  "github",
  "github-actions",
  "vercel",
  "notion",
  "openai",
  "anthropic",
  "resend",
  "sentry",
  "pipedrive",
  "temporal",
  "aws",
]);

/* ==========================================================================
   CATEGORY ACCENTS  (space-separated RGB so we can add alpha)
   ========================================================================== */

const DEFAULT_ACCENT = "129 140 248";
export const TRIGGER_ACCENT = "245 158 11";

// Adjust the keys to match your backend's category names.
const CATEGORY_ACCENTS: Record<string, string> = {
  trigger: TRIGGER_ACCENT,
  ai: "167 139 250",
  agent: "167 139 250",
  logic: "251 146 60",
  flow: "251 146 60",
  condition: "251 146 60",
  data: "56 189 248",
  transform: "56 189 248",
  http: "45 212 191",
  communication: "52 211 153",
  email: "52 211 153",
  crm: "244 114 182",
};

export function resolveAccent(category?: string, isTrigger?: boolean) {
  if (isTrigger) return TRIGGER_ACCENT;

  const tokens = (category ?? "").toLowerCase().split(/[^a-z]+/);

  for (const token of tokens) {
    if (CATEGORY_ACCENTS[token]) return CATEGORY_ACCENTS[token];
  }

  return DEFAULT_ACCENT;
}

/* ==========================================================================
   ICON
   node    -> bare logo / colored icon on the canvas
   palette -> same, in a fixed 32px slot so labels line up
   ========================================================================== */

const SIZES = {
  node: {
    logo: "size-[38px]",
    glyph: "size-[30px]",
  },
  palette: {
    logo: "size-[24px]",
    glyph: "size-[22px]",
  },
} as const;

export function NodeIcon({
  type,
  label,
  icon,
  accent,
  variant = "node",
  className,
}: {
  type?: string;
  label?: string;
  icon?: string;
  accent: string;
  variant?: keyof typeof SIZES;
  className?: string;
}) {
  const [failed, setFailed] = React.useState(false);

  const slug = React.useMemo(
    () => resolveTheSvgSlug(type, label, icon),
    [type, label, icon],
  );

  React.useEffect(() => {
    setFailed(false);
  }, [slug]);

  const size = SIZES[variant];

  /* Brand logo: dark logos flipped to white */
  const visual =
    slug && !failed ? (
      <img
        src={`https://thesvg.org/icons/${slug}/default.svg`}
        alt=""
        aria-hidden="true"
        draggable={false}
        className={cn(
          "object-contain",
          size.logo,
          variant === "node" &&
            "drop-shadow-[0_1px_3px_rgba(0,0,0,0.45)]",
          MONO_DARK_SLUGS.has(slug) && "brightness-0 invert",
        )}
        onError={() => setFailed(true)}
      />
    ) : (
      /* Generic node: colored Lucide icon */
      (() => {
        const FallbackIcon = resolveIcon(icon);

        return (
          <FallbackIcon
            className={size.glyph}
            style={{ color: `rgb(${accent})` }}
            strokeWidth={1.75}
            aria-hidden="true"
          />
        );
      })()
    );

  /* Canvas: bare icon, no wrapper */
  if (variant === "node") return visual;

  /* Palette: fixed slot so every row's text lines up */
  return (
    <span
      className={cn(
        "flex size-8 shrink-0 items-center justify-center",
        className,
      )}
    >
      {visual}
    </span>
  );
}