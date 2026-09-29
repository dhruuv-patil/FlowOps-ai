"use client";

import * as React from "react";
import {
  ChevronDown,
  PanelLeftClose,
  PanelLeftOpen,
  Search,
  X,
} from "lucide-react";

import { cn } from "@/lib/utils";
import type { NodeDefinition } from "@/types";
import { Button } from "@/components/ui/button";
import {
  DND_MIME,
  groupByCategory,
  resolveIcon,
} from "@/components/app/builder/shared";

interface NodePaletteProps {
  defs: NodeDefinition[];
  collapsed: boolean;
  onToggle: () => void;
  /** Click-to-add fallback (drops the node at the canvas center). */
  onAdd: (type: string) => void;
  isLoading?: boolean;
  isError?: boolean;
}

/**
 * Maps provider/node identity -> theSVG slug.
 *
 * We intentionally do NOT use `def.icon` here because the backend's
 * `NodeDefinition.icon` is a Lucide icon name such as "GitBranch",
 * "UserPlus", "Upload", etc.
 */
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
  sheets: "google-sheets",

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

/**
 * Normalize a backend node/provider string.
 */
function normalizeKey(value?: string) {
  return (value ?? "")
    .trim()
    .toLowerCase()
    .replace(/[\s./-]+/g, "_");
}

/**
 * Resolve a real theSVG brand from the node's identity.
 *
 * Priority:
 *   1. node type
 *   2. node label
 *   3. known icon identity
 *
 * This allows nodes such as:
 *   github_create_issue
 *   salesforce_create_lead
 *   pagerduty_trigger_incident
 *   notion_create_page
 *   s3_upload
 *
 * to correctly resolve to their real brand.
 */
function resolveTheSvgSlug(
  type?: string,
  label?: string,
  icon?: string,
): string | null {
  const values = [
    normalizeKey(type),
    normalizeKey(label),
    normalizeKey(icon),
  ].filter(Boolean);

  for (const value of values) {
    // ---------------------------------------------------------------
    // Specific providers first
    // ---------------------------------------------------------------

    if (
      value.includes("github_actions") ||
      value.includes("githubactions")
    ) {
      return THESVG_SLUGS.githubactions;
    }

    if (value.includes("google_sheets") || value.includes("googlesheets")) {
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

    // ---------------------------------------------------------------
    // Standard providers
    // ---------------------------------------------------------------

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
      value.includes("googlesheets") ||
      value === "sheets"
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

/**
 * Real brand SVG loaded directly from theSVG.
 *
 * Falls back to the existing Lucide resolver for generic/internal nodes.
 */
function TheSvgIcon({
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
    () => resolveTheSvgSlug(type, label, icon),
    [type, label, icon],
  );

  React.useEffect(() => {
    setFailed(false);
  }, [slug]);

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

export function NodePalette({
  defs,
  collapsed,
  onToggle,
  onAdd,
  isLoading,
  isError,
}: NodePaletteProps) {
  const [searchOpen, setSearchOpen] = React.useState(false);
  const [search, setSearch] = React.useState("");

  const searchInputRef = React.useRef<HTMLInputElement>(null);

  const groups = React.useMemo(() => {
    const term = search.trim().toLowerCase();

    const filtered = !term
      ? defs
      : defs.filter((def) => {
          return (
            def.label.toLowerCase().includes(term) ||
            def.type.toLowerCase().includes(term) ||
            def.category.toLowerCase().includes(term) ||
            def.description?.toLowerCase().includes(term)
          );
        });

    return groupByCategory(filtered);
  }, [defs, search]);

  React.useEffect(() => {
    if (!searchOpen) return;

    requestAnimationFrame(() => {
      searchInputRef.current?.focus();
    });
  }, [searchOpen]);

  const toggleSearch = () => {
    setSearchOpen((open) => {
      if (open) {
        setSearch("");
      }

      return !open;
    });
  };

  if (collapsed) {
    return (
      <div className="flex h-full w-12 shrink-0 flex-col items-center border-r border-white/[0.075] bg-[#090909] py-3">
        <Button
          variant="ghost"
          size="icon"
          onClick={onToggle}
          aria-label="Expand node palette"
          title="Expand node palette"
          className="size-8 text-white/45 hover:bg-white/[0.06] hover:text-white"
        >
          <PanelLeftOpen className="size-4" />
        </Button>
      </div>
    );
  }

  return (
    <aside className="flex h-full w-[270px] shrink-0 flex-col border-r border-white/[0.075] bg-[#090909]">
      {/* ================================================================
          HEADER
          ================================================================ */}

      <div className="flex h-12 shrink-0 items-center justify-between border-b border-white/[0.075] px-3.5">
        <div className="flex min-w-0 items-center gap-2">
          <div className="flex size-6 shrink-0 items-center justify-center rounded-md bg-white/[0.06]">
            <span className="size-1.5 rounded-full bg-brand-400 shadow-[0_0_8px_rgba(99,102,241,0.75)]" />
          </div>

          <span className="text-[13px] font-semibold tracking-tight text-white/90">
            Nodes
          </span>

          <span className="rounded-full bg-white/[0.06] px-1.5 py-0.5 text-[9px] font-medium text-white/35">
            {defs.length}
          </span>
        </div>

        <div className="flex items-center gap-1">
          {/* Search */}
          <Button
            variant="ghost"
            size="icon"
            onClick={toggleSearch}
            aria-label={searchOpen ? "Close node search" : "Search nodes"}
            title={searchOpen ? "Close search" : "Search nodes"}
            className={cn(
              "size-8 text-white/35",
              "hover:bg-white/[0.06] hover:text-white",
              searchOpen && "bg-white/[0.06] text-white",
            )}
          >
            {searchOpen ? (
              <X className="size-4" />
            ) : (
              <Search className="size-4" />
            )}
          </Button>

          {/* Collapse */}
          <Button
            variant="ghost"
            size="icon"
            onClick={onToggle}
            aria-label="Collapse node palette"
            title="Collapse node palette"
            className="size-8 text-white/35 hover:bg-white/[0.06] hover:text-white"
          >
            <PanelLeftClose className="size-4" />
          </Button>
        </div>
      </div>

      {/* ================================================================
          SEARCH
          ================================================================ */}

      {searchOpen && (
        <div className="shrink-0 border-b border-white/[0.075] px-3 py-2.5">
          <div className="relative">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-white/25" />

            <input
              ref={searchInputRef}
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search nodes..."
              className={cn(
                "h-9 w-full rounded-lg",
                "border border-white/[0.08]",
                "bg-white/[0.025]",
                "pl-8 pr-3",
                "text-xs text-white/80",
                "outline-none",
                "placeholder:text-white/25",
                "transition-colors",
                "focus:border-brand-400/30",
                "focus:bg-white/[0.04]",
              )}
            />
          </div>
        </div>
      )}

      {/* ================================================================
          NODE LIST
          ================================================================ */}

      <div className="min-h-0 flex-1 overflow-y-auto px-2.5 pb-3 pt-3">
        {isLoading ? (
          <LoadingState />
        ) : isError ? (
          <p className="px-2 py-4 text-xs text-red-300/80">
            Could not load node types.
          </p>
        ) : groups.length === 0 ? (
          <EmptyState search={search} />
        ) : (
          <div className="space-y-5">
            {groups.map((group) => (
              <PaletteGroup
                key={group.category}
                label={group.category}
                defs={group.defs}
                onAdd={onAdd}
              />
            ))}
          </div>
        )}
      </div>

      {/* ================================================================
          FOOTER
          ================================================================ */}

      <div className="shrink-0 border-t border-white/[0.075] px-3 py-2.5">
        <p className="text-[10px] leading-4 text-white/25">
          Drag a node onto the canvas
          <span className="mx-1 text-white/15">·</span>
          Click to add
        </p>
      </div>
    </aside>
  );
}

function PaletteGroup({
  label,
  defs,
  onAdd,
}: {
  label: string;
  defs: NodeDefinition[];
  onAdd: (type: string) => void;
}) {
  const [open, setOpen] = React.useState(true);

  return (
    <section>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className={cn(
          "mb-1.5 flex w-full items-center gap-1.5 px-1",
          "text-left",
          "text-[9px] font-semibold uppercase tracking-[0.16em]",
          "text-white/30",
          "transition-colors hover:text-white/55",
        )}
      >
        <ChevronDown
          className={cn(
            "size-3 transition-transform duration-150",
            !open && "-rotate-90",
          )}
        />

        <span>{formatCategory(label)}</span>

        <span className="ml-auto text-[9px] font-normal text-white/15">
          {defs.length}
        </span>
      </button>

      {open && (
        <div className="space-y-0.5">
          {defs.map((def) => (
            <PaletteItem key={def.type} def={def} onAdd={onAdd} />
          ))}
        </div>
      )}
    </section>
  );
}

function PaletteItem({
  def,
  onAdd,
}: {
  def: NodeDefinition;
  onAdd: (type: string) => void;
}) {
  return (
    <button
      type="button"
      draggable
      onDragStart={(e) => {
        e.dataTransfer.setData(DND_MIME, def.type);
        e.dataTransfer.effectAllowed = "move";
      }}
      onClick={() => onAdd(def.type)}
      title={def.description}
      className={cn(
        "group flex w-full items-center gap-2.5",
        "rounded-lg border border-transparent",
        "px-2 py-2",
        "text-left",
        "transition-all duration-150",
        "hover:border-white/[0.08]",
        "hover:bg-white/[0.035]",
        "active:cursor-grabbing",
      )}
    >
      {/* ================================================================
          REAL BRAND ICON FROM THESVG.ORG
          ================================================================ */}

      <span
        className={cn(
          "relative flex size-9 shrink-0 items-center justify-center",
          "rounded-lg",
          "border border-white/[0.07]",
          "bg-[#111111]",
          "transition-all duration-150",
          "group-hover:border-white/[0.12]",
          "group-hover:bg-white/[0.055]",
        )}
      >
        <TheSvgIcon
          type={def.type}
          label={def.label}
          icon={def.icon}
          className="size-[20px]"
        />
      </span>

      {/* ================================================================
          TEXT
          ================================================================ */}

      <span className="min-w-0 flex-1">
        <span
          className={cn(
            "block truncate text-[12px] font-medium",
            "text-white/75",
            "transition-colors",
            "group-hover:text-white",
          )}
        >
          {def.label}
        </span>

        {def.description && (
          <span className="mt-0.5 block truncate text-[10px] leading-4 text-white/25">
            {def.description}
          </span>
        )}
      </span>
    </button>
  );
}

function LoadingState() {
  return (
    <div className="space-y-4 px-1 pt-1">
      {[1, 2, 3].map((group) => (
        <div key={group} className="space-y-2">
          <div className="h-2.5 w-20 animate-pulse rounded bg-white/[0.05]" />

          {[1, 2, 3].map((item) => (
            <div
              key={item}
              className="h-12 animate-pulse rounded-lg bg-white/[0.025]"
            />
          ))}
        </div>
      ))}
    </div>
  );
}

function EmptyState({ search }: { search: string }) {
  return (
    <div className="flex flex-col items-center justify-center px-4 py-12 text-center">
      <div className="mb-3 flex size-9 items-center justify-center rounded-lg border border-white/[0.07] bg-white/[0.025]">
        <Search className="size-4 text-white/25" />
      </div>

      <p className="text-xs font-medium text-white/55">
        {search ? "No nodes found" : "No nodes available"}
      </p>

      {search && (
        <p className="mt-1 text-[10px] text-white/25">
          Try a different search.
        </p>
      )}
    </div>
  );
}

function formatCategory(category: string) {
  return category.replace(/[_-]/g, " ");
}