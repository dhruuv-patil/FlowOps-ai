"use client";

import * as React from "react";
import { Handle, Position, type NodeProps } from "@xyflow/react";

import { cn } from "@/lib/utils";
import type { NodeDefinition } from "@/types";
import {
  ErrorNodesContext,
  resolveIcon,
  type FlowNodeData,
} from "@/components/app/builder/shared";

export const NodeDefsContext = React.createContext<
  Record<string, NodeDefinition>
>({});

/* ==========================================================================
   theSVG BRAND REGISTRY
   ========================================================================== */

const THESVG_SLUGS: Record<string, string> = {
  // Automation
  n8n: "n8n",
  make: "make",
  zapier: "zapier",
  temporal: "temporal",

  // Developer
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

  // CRM
  hubspot: "hubspot",
  salesforce: "salesforce",
  pipedrive: "pipedrive",

  // Communication
  slack: "slack",
  discord: "discord",
  teams: "microsoft-teams",
  "microsoft-teams": "microsoft-teams",
  telegram: "telegram",
  twilio: "twilio",

  // Email
  gmail: "gmail",
  sendgrid: "sendgrid",
  resend: "resend",

  // Productivity
  notion: "notion",
  "google-sheets": "google-sheets",
  googlesheets: "google-sheets",

  // Payments
  stripe: "stripe",

  // AI
  openai: "openai",
  anthropic: "anthropic",

  // Cloud / storage
  aws: "aws",
  s3: "amazon-s3",
  "aws-s3": "amazon-s3",
};

function normalize(value?: string) {
  return (value ?? "")
    .trim()
    .toLowerCase()
    .replace(/[\s./-]+/g, "_");
}

/**
 * Resolve a brand from the actual node identity.
 *
 * `def.icon` is intentionally only the fallback because the backend
 * stores Lucide names there, e.g. GitBranch, UserPlus, Upload.
 */
function resolveTheSvgSlug(
  type?: string,
  label?: string,
): string | null {
  const values = [normalize(type), normalize(label)].filter(Boolean);

  for (const value of values) {
    /* ---------------------------------------------------------------
       Specific integrations first
       --------------------------------------------------------------- */

    if (
      value.includes("github_actions") ||
      value.includes("githubactions")
    ) {
      return THESVG_SLUGS.githubactions;
    }

    if (
      value.includes("google_sheets") ||
      value.includes("googlesheets")
    ) {
      return THESVG_SLUGS["google-sheets"];
    }

    if (
      value.includes("microsoft_teams") ||
      value.includes("ms_teams") ||
      value === "teams"
    ) {
      return THESVG_SLUGS.teams;
    }

    if (
      value === "s3" ||
      value.startsWith("s3_") ||
      value.includes("_s3_") ||
      value.includes("aws_s3")
    ) {
      return THESVG_SLUGS.s3;
    }

    /* ---------------------------------------------------------------
       Provider matching
       --------------------------------------------------------------- */

    if (value.includes("salesforce")) {
      return THESVG_SLUGS.salesforce;
    }

    if (value.includes("pagerduty")) {
      return THESVG_SLUGS.pagerduty;
    }

    if (value.includes("github")) {
      return THESVG_SLUGS.github;
    }

    if (value.includes("gitlab")) {
      return THESVG_SLUGS.gitlab;
    }

    if (value.includes("vercel")) {
      return THESVG_SLUGS.vercel;
    }

    if (value.includes("sentry")) {
      return THESVG_SLUGS.sentry;
    }

    if (value.includes("linear")) {
      return THESVG_SLUGS.linear;
    }

    if (value.includes("jira")) {
      return THESVG_SLUGS.jira;
    }

    if (value.includes("asana")) {
      return THESVG_SLUGS.asana;
    }

    if (value.includes("hubspot")) {
      return THESVG_SLUGS.hubspot;
    }

    if (value.includes("pipedrive")) {
      return THESVG_SLUGS.pipedrive;
    }

    if (value.includes("slack")) {
      return THESVG_SLUGS.slack;
    }

    if (value.includes("discord")) {
      return THESVG_SLUGS.discord;
    }

    if (value.includes("telegram")) {
      return THESVG_SLUGS.telegram;
    }

    if (value.includes("twilio")) {
      return THESVG_SLUGS.twilio;
    }

    if (value.includes("sendgrid")) {
      return THESVG_SLUGS.sendgrid;
    }

    if (value.includes("resend")) {
      return THESVG_SLUGS.resend;
    }

    if (value.includes("gmail")) {
      return THESVG_SLUGS.gmail;
    }

    if (value.includes("notion")) {
      return THESVG_SLUGS.notion;
    }

    if (
      value.includes("google_sheets") ||
      value.includes("googlesheets")
    ) {
      return THESVG_SLUGS["google-sheets"];
    }

    if (value.includes("stripe")) {
      return THESVG_SLUGS.stripe;
    }

    if (value.includes("openai")) {
      return THESVG_SLUGS.openai;
    }

    if (value.includes("anthropic")) {
      return THESVG_SLUGS.anthropic;
    }

    if (value.includes("n8n")) {
      return THESVG_SLUGS.n8n;
    }

    if (value.includes("zapier")) {
      return THESVG_SLUGS.zapier;
    }

    if (value.includes("make")) {
      return THESVG_SLUGS.make;
    }

    if (value.includes("temporal")) {
      return THESVG_SLUGS.temporal;
    }

    if (value.includes("aws")) {
      return THESVG_SLUGS.aws;
    }
  }

  return null;
}

/* ==========================================================================
   ICON
   ========================================================================== */

function NodeIcon({
  type,
  label,
  icon,
  className,
}: {
  type?: string;
  label?: string;
  icon?: string;
  className?: string;
}) {
  const [failed, setFailed] = React.useState(false);

  const slug = React.useMemo(
    () => resolveTheSvgSlug(type, label),
    [type, label],
  );

  React.useEffect(() => {
    setFailed(false);
  }, [slug]);

  /*
   * Real brand logo from thesvg.org.
   *
   * Example:
   * https://thesvg.org/icons/github/default.svg
   */
  if (slug && !failed) {
    return (
      <img
        src={`https://thesvg.org/icons/${slug}/default.svg`}
        alt=""
        aria-hidden="true"
        draggable={false}
        className={cn("object-contain", className)}
        onError={() => setFailed(true)}
      />
    );
  }

  /* Generic FlowOps node → Lucide */
  const FallbackIcon = resolveIcon(icon);

  return (
    <FallbackIcon
      className={className}
      aria-hidden="true"
    />
  );
}

/* ==========================================================================
   NODE
   ========================================================================== */

function FlowNodeComponent({
  id,
  type,
  data,
  selected,
}: NodeProps) {
  const defs = React.useContext(NodeDefsContext);
  const errorNodes = React.useContext(ErrorNodesContext);

  const def = type ? defs[type] : undefined;
  const nodeData = data as FlowNodeData;

  const label =
    nodeData.label ||
    def?.label ||
    type ||
    "Node";

  const outputs =
    def?.outputs?.length
      ? def.outputs
      : ["out"];

  const isTrigger = def?.trigger ?? false;
  const hasError = errorNodes.has(id);
  const multiOut = outputs.length > 1;

  const handleClass = cn(
    "!size-[9px]",
    "!rounded-full",
    "!border-[2px]",
    "!border-[#050505]",
    "!bg-[#8b8b8b]",
    "!shadow-[0_0_0_1px_rgba(255,255,255,0.10),0_1px_4px_rgba(0,0,0,0.55)]",
    "transition-all duration-150",
    "hover:!scale-[1.18]",
    "group-hover:!bg-brand-400",
    selected && "!bg-brand-400",
    hasError && "!bg-red-400",
  );

  return (
    <div className="group relative flex w-[132px] flex-col items-center">

      {/* ================================================================
          NODE BODY
          ================================================================ */}

      <div
        className={cn(
  "relative flex size-[92px] shrink-0 items-center justify-center",
  "rounded-[14px]",
  "border border-white/[0.10]",
  "bg-[#111111]",
  "shadow-[0_4px_18px_rgba(0,0,0,0.28)]",
  "transition-all duration-150",

  "group-hover:-translate-y-[1px]",
  "group-hover:border-white/[0.18]",
  "group-hover:bg-[#141414]",

  selected && [
    "-translate-y-[1px]",
    "border-brand-400/60",
    "bg-[#141414]",
    "shadow-[0_0_0_3px_rgba(99,102,241,0.10),0_8px_24px_rgba(0,0,0,0.32)]",
  ],

  hasError && [
    "!border-red-500",
    "shadow-[0_0_0_1px_rgba(239,68,68,0.25),0_4px_18px_rgba(0,0,0,0.28)]",
  ],

  nodeData.executionStatus === "SUCCEEDED" && [
    "!border-emerald-400",
    "shadow-[0_0_0_1px_rgba(52,211,153,0.25),0_4px_18px_rgba(0,0,0,0.28)]",
  ],
)}
      >

        {/* ================================================================
            INPUT
            ================================================================ */}

        {!isTrigger && (
          <Handle
            type="target"
            position={Position.Left}
            className={cn(
              handleClass,
              "!left-[-6px]",
              "!top-1/2",
              "!-translate-y-1/2",
              "!translate-x-0",
            )}
          />
        )}

        {/* Inner highlight */}
        <div className="pointer-events-none absolute inset-[1px] rounded-[13px] border border-white/[0.025]" />

        {/* ================================================================
            REAL BRAND / GENERIC ICON
            ================================================================ */}

        <div
          className={cn(
            "relative flex size-[48px] items-center justify-center",
            "rounded-xl",
            "bg-white/[0.045]",
            "text-white/80",
            "transition-all duration-150",

            "group-hover:bg-brand-500/10",
            "group-hover:text-brand-400",

            selected && "bg-brand-500/10 text-brand-400",
            hasError && "bg-red-500/10 text-red-400",
          )}
        >
          <NodeIcon
            type={type}
            label={label}
            icon={def?.icon}
            className="size-[30px] transition-transform duration-150 group-hover:scale-[1.04]"
          />
        </div>

        {/* ================================================================
            TRIGGER INDICATOR
            ================================================================ */}

        {isTrigger && (
          <span
            className="absolute -left-[5px] top-1/2 -translate-y-1/2"
            aria-hidden="true"
          >
            <span className="block size-[4px] rounded-full bg-brand-400 shadow-[0_0_8px_rgba(99,102,241,0.8)]" />
          </span>
        )}

        {/* ================================================================
            ERROR
            ================================================================ */}

        {hasError && (
          <span
            className={cn(
              "absolute -right-[5px] -top-[5px]",
              "flex size-[16px] items-center justify-center",
              "rounded-full",
              "border-2 border-[#050505]",
              "bg-red-500",
              "text-[9px] font-bold text-white",
              "shadow-[0_0_10px_rgba(239,68,68,0.45)]",
            )}
            aria-label="Validation error"
          >
            !
          </span>
        )}

        {/* ================================================================
            SINGLE OUTPUT
            ================================================================ */}

        {!multiOut && (
          <Handle
            id={outputs[0]}
            type="source"
            position={Position.Right}
            className={cn(
              handleClass,
              "!right-[-6px]",
              "!left-auto",
              "!top-1/2",
              "!-translate-y-1/2",
              "!translate-x-0",
            )}
          />
        )}

        {/* ================================================================
            MULTIPLE OUTPUTS
            ================================================================ */}

        {multiOut && (
          <div
            className={cn(
              "pointer-events-none absolute",
              "right-[-6px]",
              "top-1/2",
              "-translate-y-1/2",
              "flex flex-col gap-3",
            )}
          >
            {outputs.map((out) => (
              <div
                key={out}
                className="relative flex h-[14px] items-center"
              >
                {/* Output label */}
                <span
                  className={cn(
                    "pointer-events-none absolute right-[14px]",
                    "whitespace-nowrap",
                    "text-[9px] font-medium leading-none",
                    "text-white/35",
                    "transition-colors",
                    "group-hover:text-white/55",
                  )}
                >
                  {out}
                </span>

                {/* Output handle */}
                <Handle
                  id={out}
                  type="source"
                  position={Position.Right}
                  className={cn(
                    handleClass,
                    "!right-[-1px]",
                    "!left-auto",
                    "!top-1/2",
                    "!-translate-y-1/2",
                    "!translate-x-0",
                    "!pointer-events-auto",
                  )}
                />
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ================================================================
          LABEL
          ================================================================ */}

      <div className="mt-2 w-[132px] text-center">
        <div
          className={cn(
            "truncate text-[13px] font-medium leading-5",
            "text-white/85",
            selected && "text-white",
            hasError && "text-red-300",
          )}
          title={label}
        >
          {label}
        </div>

        <div
          className={cn(
            "mt-[1px] truncate",
            "text-[9px] font-medium uppercase tracking-[0.12em]",
            "text-white/25",
          )}
          title={def?.category ?? type}
        >
          {def?.category ?? type}
        </div>
      </div>
    </div>
  );
}

export const FlowNode = React.memo(FlowNodeComponent);