"use client";

import * as React from "react";
import { cn } from "@/lib/utils";
import { resolveIcon } from "@/components/app/builder/shared";

const ICON_SLUGS: Record<string, string> = {
  n8n: "n8n",
  make: "make",
  zapier: "zapier",
  temporal: "temporal",

  github: "github",
  githubactions: "github-actions",
  "github-actions": "github-actions",
  gitlab: "gitlab",
  vercel: "vercel",
  sentry: "sentry",
  pagerduty: "pagerduty",
  linear: "linear",
  jira: "jira",
  asana: "asana",

  hubspot: "hubspot",
  salesforce: "salesforce",
  pipedrive: "pipedrive",

  slack: "slack",
  discord: "discord",
  telegram: "telegram",
  twilio: "twilio",

  sendgrid: "sendgrid",
  resend: "resend",

  notion: "notion",
  "google-sheets": "google-sheets",

  stripe: "stripe",

  anthropic: "anthropic",
  openai: "openai",

  aws: "aws",
  s3: "s3",
};

interface TheSvgIconProps {
  icon?: string;
  className?: string;
}

export function TheSvgIcon({
  icon,
  className = "size-5",
}: TheSvgIconProps) {
  const normalized = icon?.trim().toLowerCase() ?? "";
  const slug = ICON_SLUGS[normalized];

  const [failed, setFailed] = React.useState(false);

  if (!slug || failed) {
    const FallbackIcon = resolveIcon(icon);

    return (
      <FallbackIcon
        className={className}
        aria-hidden="true"
      />
    );
  }

  return (
    <img
      src={`https://thesvg.org/icons/${slug}/default.svg`}
      alt=""
      aria-hidden="true"
      draggable={false}
      loading="lazy"
      className={cn("object-contain", className)}
      onError={() => setFailed(true)}
    />
  );
}