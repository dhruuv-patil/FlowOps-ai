"use client";

import Image from "next/image";

import {
  Mail,
  MessageSquare,
  Plug,
  Webhook,
  Bot,
  Code,
  Database,
  Cloud,
  HardDrive,
  Briefcase,
  Sheet,
  CreditCard,
  Globe,
  MoreHorizontal,
  type LucideIcon,
} from "lucide-react";

interface ProviderIconProps {
  icon: string;
  className?: string;
}

/**
 * Brand SVGs available in:
 *
 * public/integrations/
 *
 * The value must match the SVG filename
 * without the ".svg" extension.
 */
const brandIcons = new Set([
  // Workflow automation
  "n8n",
  "make",
  "zapier",
  "temporal",

  // Developer
  "github",
  "githubactions",
  "gitlab",
  "vercel",
  "sentry",
  "pagerduty",
  "linear",
  "jira",
  "asana",

  // CRM
  "hubspot",
  "salesforce",
  "pipedrive",

  // Communication
  "slack",
  "discord",
  "telegram",
  "twilio",

  // Email
  "sendgrid",
  "resend",

  // Productivity
  "notion",
  "google-sheets",

  // Payments
  "stripe",

  // AI
  "anthropic",
  "openai",

  // Cloud / Storage
  "aws",
  "s3",
]);

const lucideIconMap: Record<
  string,
  LucideIcon
> = {
  Mail,
  MessageSquare,
  Webhook,
  Globe,
  Plug,
  Bot,

  Code2: Code,

  Cloud,
  HardDrive,
  Briefcase,
  Sheet,
  CreditCard,
  Database,
  MoreHorizontal,
};

export function ProviderIcon({
  icon,
  className = "size-6",
}: ProviderIconProps) {
  const normalizedIcon =
    icon.trim().toLowerCase();

  /**
   * Use the actual local brand SVG
   * whenever one exists.
   */
  if (
    brandIcons.has(
      normalizedIcon,
    )
  ) {
    return (
      <Image
        src={`/integrations/${normalizedIcon}.svg`}
        alt=""
        width={24}
        height={24}
        className={`object-contain ${className}`}
        aria-hidden="true"
        unoptimized
      />
    );
  }

  /**
   * Generic integrations continue
   * using Lucide icons.
   */
  const IconComponent =
    lucideIconMap[icon] ??
    lucideIconMap[normalizedIcon] ??
    Plug;

  return (
    <IconComponent
      className={className}
      aria-hidden="true"
    />
  );
}