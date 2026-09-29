/**
 * Client-side provider metadata registry.
 *
 * Brand integrations use their provider wire type as the icon identifier.
 * ProviderIcon resolves these identifiers to SVG files located in:
 *
 *   /public/integrations/
 *
 * Example:
 *
 *   github -> /integrations/github.svg
 *   slack  -> /integrations/slack.svg
 *   stripe -> /integrations/stripe.svg
 *
 * Generic/non-brand integrations continue using Lucide icons.
 */

import type { ProviderCategory } from "@/types";

export interface ProviderMeta {
  /**
   * Brand SVG identifier or Lucide icon name.
   *
   * Brand example:
   *   "github"
   *   "slack"
   *   "stripe"
   *
   * Lucide fallback example:
   *   "Mail"
   *   "Webhook"
   *   "Globe"
   */
  icon: string;

  category: ProviderCategory;
}

/**
 * Metadata for every registered backend provider.
 *
 * Keys must match the wire type returned by IntegrationType.wire().
 */
export const PROVIDER_META: Record<string, ProviderMeta> = {
  // ============================================================
  // WORKFLOW AUTOMATION
  // ============================================================

  n8n: {
    icon: "n8n",
    category: "AUTOMATION",
  },

  make: {
    icon: "make",
    category: "AUTOMATION",
  },

  zapier: {
    icon: "zapier",
    category: "AUTOMATION",
  },

  temporal: {
    icon: "temporal",
    category: "AUTOMATION",
  },

  // ============================================================
  // DEVELOPER / SOURCE CONTROL
  // ============================================================

  github: {
    icon: "github",
    category: "DEVELOPER",
  },

  gitlab: {
    icon: "gitlab",
    category: "DEVELOPER",
  },

  vercel: {
    icon: "vercel",
    category: "DEVELOPER",
  },

  sentry: {
    icon: "sentry",
    category: "DEVELOPER",
  },

  pagerduty: {
    icon: "pagerduty",
    category: "DEVELOPER",
  },

  linear: {
    icon: "linear",
    category: "DEVELOPER",
  },

  jira: {
    icon: "jira",
    category: "DEVELOPER",
  },

  asana: {
    icon: "asana",
    category: "PROJECT_MANAGEMENT",
  },

  // ============================================================
  // CRM / SALES
  // ============================================================

  hubspot: {
    icon: "hubspot",
    category: "CRM",
  },

  salesforce: {
    icon: "salesforce",
    category: "CRM",
  },

  pipedrive: {
    icon: "pipedrive",
    category: "CRM",
  },

  // ============================================================
  // COMMUNICATION
  // ============================================================

  slack: {
    icon: "slack",
    category: "COMMUNICATION",
  },

  discord: {
    icon: "discord",
    category: "COMMUNICATION",
  },

  teams: {
    icon: "teams",
    category: "COMMUNICATION",
  },

  telegram: {
    icon: "telegram",
    category: "COMMUNICATION",
  },

  twilio: {
    icon: "twilio",
    category: "COMMUNICATION",
  },

  // ============================================================
  // EMAIL
  // ============================================================

  /**
   * SMTP is a protocol rather than a specific brand,
   * so keep the generic Lucide Mail icon.
   */
  smtp: {
    icon: "Mail",
    category: "EMAIL",
  },

  sendgrid: {
    icon: "sendgrid",
    category: "EMAIL",
  },

  resend: {
    icon: "resend",
    category: "EMAIL",
  },

  // ============================================================
  // GENERIC / WEBHOOKS
  // ============================================================

  /**
   * These represent generic capabilities rather than
   * specific third-party brands.
   */
  webhook: {
    icon: "Webhook",
    category: "UNIVERSAL",
  },

  "rest-api": {
    icon: "Globe",
    category: "UNIVERSAL",
  },

  custom: {
    icon: "Globe",
    category: "UNIVERSAL",
  },

  // ============================================================
  // CLOUD / STORAGE
  // ============================================================

  aws: {
    icon: "aws",
    category: "CLOUD",
  },

  s3: {
    icon: "s3",
    category: "STORAGE",
  },

  // ============================================================
  // PRODUCTIVITY
  // ============================================================

  notion: {
    icon: "notion",
    category: "PRODUCTIVITY",
  },

  "google-sheets": {
    icon: "google-sheets",
    category: "PRODUCTIVITY",
  },

  // ============================================================
  // PAYMENTS
  // ============================================================

  stripe: {
    icon: "stripe",
    category: "PAYMENTS",
  },

  // ============================================================
  // AI / LLM
  // ============================================================

  anthropic: {
    icon: "anthropic",
    category: "AI",
  },

  openai: {
    icon: "openai",
    category: "AI",
  },
};

/**
 * Returns the icon identifier for a provider type.
 *
 * Brand providers return their SVG identifier.
 * Unknown providers fall back to the generic Lucide Plug icon.
 */
export function getProviderIcon(type: string): string {
  return PROVIDER_META[type]?.icon ?? "Plug";
}

/**
 * Returns the category for a provider type.
 *
 * Unknown providers fall back to UNIVERSAL.
 */
export function getProviderCategory(
  type: string,
): ProviderCategory {
  return (
    PROVIDER_META[type]?.category ??
    "UNIVERSAL"
  );
}