"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";

import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";

import {
  ArrowRight,
  Bot,
  CalendarDays,
  Globe2,
  Loader2,
  Mail,
  PanelsTopLeft,
  Search,
  Sparkles,
  UserCheck,
  Webhook,
} from "lucide-react";

import { toast } from "sonner";

import {
  fetchTemplates,
  getErrorMessage,
  useTemplate as createTemplate,
} from "@/lib/api";

import type { WorkflowTemplateSummary } from "@/types";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";

import { cn } from "@/lib/utils";

type TemplateApp = {
  key: string;
  name: string;
  color: string;
  iconSlug?: string;
  kind: "brand" | "generic";
};

const EMPTY_TEMPLATES: WorkflowTemplateSummary[] = [];

const THESVG_BASE = "https://thesvg.org/icons";

const THESVG_SLUGS: Record<string, string> = {
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
  teams: "microsoft-teams",
  "microsoft-teams": "microsoft-teams",
  telegram: "telegram",
  twilio: "twilio",

  gmail: "gmail",
  sendgrid: "sendgrid",
  resend: "resend",

  notion: "notion",
  "google-sheets": "google-sheets",
  googlesheets: "google-sheets",

  calendar: "google-calendar-2026",
  googlecalendar: "google-calendar-2026",
  google_calendar: "google-calendar-2026",
  "google-calendar": "google-calendar-2026",

  stripe: "stripe",

  openai: "openai",
  anthropic: "anthropic",

  aws: "aws",
  s3: "amazon-s3",
  "aws-s3": "amazon-s3",
};

function theSvgUrl(slug: string) {
  return `${THESVG_BASE}/${slug}/default.svg`;
}

function normalize(value?: string) {
  return (value ?? "")
    .trim()
    .toLowerCase()
    .replace(/[\s./-]+/g, "_");
}

function resolveTheSvgSlug(
  type?: string,
  label?: string,
): string | null {
  const values = [
    normalize(type),
    normalize(label),
  ].filter(Boolean);

  for (const value of values) {
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
      value.includes("google_calendar") ||
      value.includes("googlecalendar")
    ) {
      return THESVG_SLUGS.calendar;
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

    if (
      value.includes("google_calendar") ||
      value.includes("googlecalendar")
    ) {
      return THESVG_SLUGS.calendar;
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

const TEMPLATE_APPS: Record<string, TemplateApp> = {
  gmail: {
    key: "gmail",
    name: "Gmail",
    color: "#EA4335",
    iconSlug: THESVG_SLUGS.gmail,
    kind: "brand",
  },

  calendar: {
    key: "calendar",
    name: "Google Calendar",
    color: "#1A73E8",
    iconSlug: THESVG_SLUGS.calendar,
    kind: "brand",
  },

  sheets: {
    key: "sheets",
    name: "Google Sheets",
    color: "#34A853",
    iconSlug: THESVG_SLUGS["google-sheets"],
    kind: "brand",
  },

  "google-sheets": {
    key: "google-sheets",
    name: "Google Sheets",
    color: "#34A853",
    iconSlug: THESVG_SLUGS["google-sheets"],
    kind: "brand",
  },

  github: {
    key: "github",
    name: "GitHub",
    color: "#181717",
    iconSlug: THESVG_SLUGS.github,
    kind: "brand",
  },

  githubactions: {
    key: "githubactions",
    name: "GitHub Actions",
    color: "#2088FF",
    iconSlug: THESVG_SLUGS.githubactions,
    kind: "brand",
  },

  "github-actions": {
    key: "github-actions",
    name: "GitHub Actions",
    color: "#2088FF",
    iconSlug: THESVG_SLUGS.githubactions,
    kind: "brand",
  },

  gitlab: {
    key: "gitlab",
    name: "GitLab",
    color: "#FC6D26",
    iconSlug: THESVG_SLUGS.gitlab,
    kind: "brand",
  },

  vercel: {
    key: "vercel",
    name: "Vercel",
    color: "#111111",
    iconSlug: THESVG_SLUGS.vercel,
    kind: "brand",
  },

  sentry: {
    key: "sentry",
    name: "Sentry",
    color: "#362D59",
    iconSlug: THESVG_SLUGS.sentry,
    kind: "brand",
  },

  pagerduty: {
    key: "pagerduty",
    name: "PagerDuty",
    color: "#06AC38",
    iconSlug: THESVG_SLUGS.pagerduty,
    kind: "brand",
  },

  linear: {
    key: "linear",
    name: "Linear",
    color: "#5E6AD2",
    iconSlug: THESVG_SLUGS.linear,
    kind: "brand",
  },

  jira: {
    key: "jira",
    name: "Jira",
    color: "#0C66E4",
    iconSlug: THESVG_SLUGS.jira,
    kind: "brand",
  },

  asana: {
    key: "asana",
    name: "Asana",
    color: "#F06A6A",
    iconSlug: THESVG_SLUGS.asana,
    kind: "brand",
  },

  hubspot: {
    key: "hubspot",
    name: "HubSpot",
    color: "#FF7A59",
    iconSlug: THESVG_SLUGS.hubspot,
    kind: "brand",
  },

  salesforce: {
    key: "salesforce",
    name: "Salesforce",
    color: "#00A1E0",
    iconSlug: THESVG_SLUGS.salesforce,
    kind: "brand",
  },

  pipedrive: {
    key: "pipedrive",
    name: "Pipedrive",
    color: "#017737",
    iconSlug: THESVG_SLUGS.pipedrive,
    kind: "brand",
  },

  slack: {
    key: "slack",
    name: "Slack",
    color: "#E01E5A",
    iconSlug: THESVG_SLUGS.slack,
    kind: "brand",
  },

  discord: {
    key: "discord",
    name: "Discord",
    color: "#5865F2",
    iconSlug: THESVG_SLUGS.discord,
    kind: "brand",
  },

  teams: {
    key: "teams",
    name: "Microsoft Teams",
    color: "#6264A7",
    iconSlug: THESVG_SLUGS.teams,
    kind: "brand",
  },

  telegram: {
    key: "telegram",
    name: "Telegram",
    color: "#229ED9",
    iconSlug: THESVG_SLUGS.telegram,
    kind: "brand",
  },

  twilio: {
    key: "twilio",
    name: "Twilio",
    color: "#F22F46",
    iconSlug: THESVG_SLUGS.twilio,
    kind: "brand",
  },

  sendgrid: {
    key: "sendgrid",
    name: "SendGrid",
    color: "#1A82E2",
    iconSlug: THESVG_SLUGS.sendgrid,
    kind: "brand",
  },

  resend: {
    key: "resend",
    name: "Resend",
    color: "#111111",
    iconSlug: THESVG_SLUGS.resend,
    kind: "brand",
  },

  notion: {
    key: "notion",
    name: "Notion",
    color: "#111111",
    iconSlug: THESVG_SLUGS.notion,
    kind: "brand",
  },

  stripe: {
    key: "stripe",
    name: "Stripe",
    color: "#635BFF",
    iconSlug: THESVG_SLUGS.stripe,
    kind: "brand",
  },

  openai: {
    key: "openai",
    name: "OpenAI",
    color: "#10A37F",
    iconSlug: THESVG_SLUGS.openai,
    kind: "brand",
  },

  anthropic: {
    key: "anthropic",
    name: "Anthropic",
    color: "#D97757",
    iconSlug: THESVG_SLUGS.anthropic,
    kind: "brand",
  },

  n8n: {
    key: "n8n",
    name: "n8n",
    color: "#EA4B71",
    iconSlug: THESVG_SLUGS.n8n,
    kind: "brand",
  },

  zapier: {
    key: "zapier",
    name: "Zapier",
    color: "#FF4A00",
    iconSlug: THESVG_SLUGS.zapier,
    kind: "brand",
  },

  make: {
    key: "make",
    name: "Make",
    color: "#6D00CC",
    iconSlug: THESVG_SLUGS.make,
    kind: "brand",
  },

  temporal: {
    key: "temporal",
    name: "Temporal",
    color: "#7C3AED",
    iconSlug: THESVG_SLUGS.temporal,
    kind: "brand",
  },

  aws: {
    key: "aws",
    name: "AWS",
    color: "#FF9900",
    iconSlug: THESVG_SLUGS.aws,
    kind: "brand",
  },

  s3: {
    key: "s3",
    name: "Amazon S3",
    color: "#569A31",
    iconSlug: THESVG_SLUGS.s3,
    kind: "brand",
  },

  ai: {
    key: "ai",
    name: "AI",
    color: "#8B5CF6",
    kind: "generic",
  },

  webhook: {
    key: "webhook",
    name: "Webhook",
    color: "#F59E0B",
    kind: "generic",
  },

  http: {
    key: "http",
    name: "HTTP",
    color: "#3B82F6",
    kind: "generic",
  },

  approval: {
    key: "approval",
    name: "Human Approval",
    color: "#10B981",
    kind: "generic",
  },

  email: {
    key: "email",
    name: "Email",
    color: "#EF4444",
    kind: "generic",
  },
};

function getTemplateApps(
  template: WorkflowTemplateSummary,
): TemplateApp[] {
  const providers = template.providers ?? [];

  const aliases: Record<string, string> = {
    googlesheets: "google-sheets",
    google_sheets: "google-sheets",
    "google-sheets": "google-sheets",
    googlesheet: "google-sheets",

    googlecalendar: "calendar",
    google_calendar: "calendar",
    "google-calendar": "calendar",

    githubactions: "githubactions",
    github_actions: "githubactions",
    "github-actions": "github-actions",

    microsoftteams: "teams",
    microsoft_teams: "teams",
    "microsoft-teams": "teams",
    "ms-teams": "teams",
    msteams: "teams",

    amazonaws: "aws",
    "amazon-web-services": "aws",
    aws_s3: "s3",
    amazons3: "s3",
    "amazon-s3": "s3",

    rest: "http",
    "rest-api": "http",
    restapi: "http",
    httpapi: "http",

    humanapproval: "approval",
    human_approval: "approval",
    "human-approval": "approval",

    llm: "ai",
    agent: "ai",
    aiagent: "ai",
  };

  return providers
    .map((provider) => {
      const normalized = provider
        .trim()
        .toLowerCase()
        .replace(/[\s_]+/g, "-");

      if (TEMPLATE_APPS[normalized]) {
        return TEMPLATE_APPS[normalized];
      }

      const alias = aliases[normalized];

      if (alias) {
        return TEMPLATE_APPS[alias] ?? null;
      }

      const slug = resolveTheSvgSlug(
        normalized,
        provider,
      );

      if (slug) {
        const matchingKey = Object.keys(
          TEMPLATE_APPS,
        ).find(
          (key) =>
            TEMPLATE_APPS[key].iconSlug === slug,
        );

        if (matchingKey) {
          return TEMPLATE_APPS[matchingKey];
        }
      }

      return null;
    })
    .filter(
      (app): app is TemplateApp =>
        app !== null,
    );
}

export default function TemplatesPage() {
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("All");

  const query = useQuery({
    queryKey: ["templates"],
    queryFn: fetchTemplates,
  });

  const all =
    query.data?.templates ?? EMPTY_TEMPLATES;

  const categories = useMemo(() => {
    const values = new Set<string>();

    for (const template of all) {
      if (template.category) {
        values.add(template.category);
      }
    }

    return [
      "All",
      ...Array.from(values).sort(),
    ];
  }, [all]);

  const filteredTemplates = useMemo(() => {
    const normalizedSearch =
      search.trim().toLowerCase();

    return all.filter((template) => {
      const matchesCategory =
        category === "All" ||
        template.category === category;

      if (!matchesCategory) {
        return false;
      }

      if (!normalizedSearch) {
        return true;
      }

      const searchable = [
        template.name,
        template.description,
        template.category,
        ...(template.tags ?? []),
        ...(template.providers ?? []),
      ]
        .join(" ")
        .toLowerCase();

      return searchable.includes(
        normalizedSearch,
      );
    });
  }, [all, category, search]);

  const recommended = filteredTemplates.slice(
    0,
    3,
  );

  const remaining = filteredTemplates.filter(
    (template) =>
      !recommended.some(
        (item) =>
          item.slug === template.slug,
      ),
  );

  const hasFilters =
    Boolean(search.trim()) ||
    category !== "All";

  const clearFilters = () => {
    setSearch("");
    setCategory("All");
  };

  return (
    <main className="min-h-screen bg-[#050505] text-white">
      <div className="mx-auto w-full max-w-[1500px] px-6 py-8 lg:px-8">
        <div className="mb-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="mb-2 text-[10px] font-semibold uppercase tracking-[0.18em] text-white/30">
                Workflow library
              </p>

              <h1 className="text-2xl font-semibold tracking-tight text-white">
                Templates
              </h1>

              <p className="mt-2 max-w-2xl text-sm leading-6 text-white/40">
                Production-ready workflow blueprints
                for automation, reliability,
                operations, AI, and business
                processes.
              </p>
            </div>

            <div className="w-full lg:w-[320px]">
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-white/25" />

                <Input
                  value={search}
                  onChange={(event) =>
                    setSearch(event.target.value)
                  }
                  placeholder="Search templates..."
                  className="h-10 rounded-lg border-white/[0.08] bg-white/[0.025] pl-9 text-sm text-white placeholder:text-white/25 focus-visible:ring-1 focus-visible:ring-white/20"
                />
              </div>
            </div>
          </div>

          <div className="mt-6 flex gap-2 overflow-x-auto pb-1">
            {categories.map((item) => (
              <CategoryPill
                key={item}
                label={item}
                active={category === item}
                onClick={() =>
                  setCategory(item)
                }
              />
            ))}
          </div>
        </div>

        {query.isLoading ? (
          <TemplateLoading />
        ) : query.isError ? (
          <div className="rounded-lg border border-red-500/10 bg-red-500/[0.03] px-5 py-12 text-center">
            <p className="text-sm font-medium text-red-300/80">
              Could not load templates
            </p>

            <p className="mx-auto mt-2 max-w-md text-xs leading-5 text-white/30">
              {getErrorMessage(
                query.error,
                "Something went wrong while loading the template library.",
              )}
            </p>
          </div>
        ) : filteredTemplates.length === 0 ? (
          <EmptyTemplates
            search={search}
            hasFilters={hasFilters}
            onClear={clearFilters}
          />
        ) : (
          <>
            {recommended.length > 0 && (
              <section className="mb-10">
                <div className="mb-4 flex items-center justify-between">
                  <div>
                    <h2 className="text-sm font-semibold text-white/80">
                      Recommended
                    </h2>

                    <p className="mt-1 text-xs text-white/25">
                      Start with a proven workflow
                      pattern.
                    </p>
                  </div>

                  <span className="text-[10px] text-white/20">
                    {recommended.length} templates
                  </span>
                </div>

                <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                  {recommended.map(
                    (template) => (
                      <TemplateCard
                        key={template.slug}
                        template={template}
                        featured
                      />
                    ),
                  )}
                </div>
              </section>
            )}

            {remaining.length > 0 && (
              <section>
                <div className="mb-4 flex items-center justify-between">
                  <div>
                    <h2 className="text-sm font-semibold text-white/80">
                      All templates
                    </h2>

                    <p className="mt-1 text-xs text-white/25">
                      Browse the complete FlowOps
                      template library.
                    </p>
                  </div>

                  <span className="text-[10px] text-white/20">
                    {remaining.length} templates
                  </span>
                </div>

                <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                  {remaining.map(
                    (template) => (
                      <TemplateCard
                        key={template.slug}
                        template={template}
                      />
                    ),
                  )}
                </div>
              </section>
            )}
          </>
        )}
      </div>
    </main>
  );
}

function CategoryPill({
  label,
  active,
  onClick,
}: {
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "shrink-0 rounded-md border px-3 py-1.5 text-xs transition-colors",
        active
          ? "border-white bg-white font-medium text-[#050505]"
          : "border-white/[0.08] bg-white/[0.025] text-white/45 hover:border-white/[0.14] hover:bg-white/[0.05] hover:text-white/80",
      )}
    >
      {label}
    </button>
  );
}

function TemplateCard({
  template,
  featured = false,
}: {
  template: WorkflowTemplateSummary;
  featured?: boolean;
}) {
  const router = useRouter();
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: () =>
      createTemplate(template.slug),

    onSuccess: (workflow) => {
      queryClient.invalidateQueries({
        queryKey: ["workflows"],
      });

      toast.success(
        `Created "${workflow.name}" from a template.`,
      );

      router.push(
        `/workflows/${workflow.id}`,
      );
    },

    onError: (error) => {
      toast.error(
        getErrorMessage(
          error,
          "Could not create from this template.",
        ),
      );
    },
  });

  return (
    <article
      className={cn(
        "group overflow-hidden rounded-lg border bg-[#0a0a0a]",
        "transition-colors duration-200",
        featured
          ? "border-white/[0.14] hover:border-white/[0.22]"
          : "border-white/[0.075] hover:border-white/[0.15]",
      )}
    >
      <TemplatePreview
        template={template}
        featured={featured}
      />

      <div className="flex min-h-[190px] flex-col p-4">
        <div>
          <h3 className="line-clamp-2 text-[15px] font-semibold leading-5 text-white/90 group-hover:text-white">
            {template.name}
          </h3>

          <p className="mt-1 text-[10px] font-medium uppercase tracking-[0.10em] text-white/25">
            {template.category}
          </p>
        </div>

        <p className="mt-3 line-clamp-3 text-xs leading-5 text-white/40">
          {template.description}
        </p>

        <div className="mt-auto flex items-center justify-between gap-3 pt-4">
          <div className="flex min-w-0 flex-wrap gap-1.5">
            {(template.tags ?? [])
              .slice(0, 2)
              .map((tag) => (
                <span
                  key={tag}
                  className="rounded-md border border-white/[0.07] px-2 py-1 text-[10px] text-white/30"
                >
                  {tag}
                </span>
              ))}
          </div>

          <Button
            size="sm"
            onClick={() =>
              mutation.mutate()
            }
            disabled={mutation.isPending}
            className="h-8 shrink-0 rounded-md bg-white px-3 text-xs font-medium text-black hover:bg-white/90"
          >
            {mutation.isPending ? (
              <Loader2 className="mr-1.5 size-3.5 animate-spin" />
            ) : (
              <ArrowRight className="mr-1.5 size-3.5" />
            )}

            {mutation.isPending
              ? "Creating..."
              : "Use template"}
          </Button>
        </div>
      </div>
    </article>
  );
}
function TemplatePreview({
  template,
  featured,
}: {
  template: WorkflowTemplateSummary;
  featured: boolean;
}) {
  const apps = getTemplateApps(template);

  const visibleApps = apps.slice(0, 3);

  const extraCount = Math.max(
    0,
    apps.length - visibleApps.length,
  );

  return (
    <div className="relative h-[178px] overflow-hidden border-b border-white/[0.07] bg-[#080808]">
      {/* BACKGROUND */}

      <div
        className="absolute inset-0 opacity-[0.14]"
        style={{
          backgroundImage:
            "radial-gradient(circle, rgba(255,255,255,.20) 1px, transparent 1px)",
          backgroundSize: "18px 18px",
        }}
      />

      {/* FEATURED BADGE */}

      {featured && (
        <div className="absolute left-3 top-3 z-20 flex items-center gap-1.5 rounded-md border border-white/[0.10] bg-[#0a0a0a]/90 px-2.5 py-1.5 text-[10px] font-medium text-white/70 backdrop-blur">
          <Sparkles className="size-3" />
          Top recommendation
        </div>
      )}

      {/* APP TRAY */}

      <div className="absolute inset-0 flex items-center justify-center px-5">
        <div className="flex items-center rounded-xl border border-slate-200 bg-white px-3 py-3 shadow-[0_18px_45px_rgba(0,0,0,0.28)]">
          {visibleApps.length > 0 ? (
            <>
              {visibleApps.map(
                (app, index) => (
                  <div
                    key={`${app.key}-${index}`}
                    className="flex items-center"
                  >
                    <LandingIntegrationIcon
                      app={app}
                    />

                    {index <
                      visibleApps.length - 1 && (
                      <ArrowRight className="mx-2 size-4 text-slate-500" />
                    )}
                  </div>
                ),
              )}

              {extraCount > 0 && (
                <>
                  <ArrowRight className="mx-2 size-4 text-slate-500" />

                  <div className="flex size-10 items-center justify-center rounded-lg border border-slate-200 bg-slate-50 text-sm font-semibold text-slate-600">
                    +{extraCount}
                  </div>
                </>
              )}
            </>
          ) : (
            <>
              <LandingIntegrationIcon
                app={TEMPLATE_APPS.webhook}
              />

              <ArrowRight className="mx-2 size-4 text-slate-500" />

              <LandingIntegrationIcon
                app={TEMPLATE_APPS.http}
              />
            </>
          )}
        </div>
      </div>

      {/* STEP COUNT */}

      <div className="absolute bottom-3 left-3 rounded-md border border-white/[0.09] bg-[#0a0a0a]/90 px-2.5 py-1 text-[10px] text-white/40 backdrop-blur">
        {template.nodeCount} steps
      </div>
    </div>
  );
}

/* ==========================================================================
   INTEGRATION ICON
   ========================================================================== */

function LandingIntegrationIcon({
  app,
}: {
  app: TemplateApp;
}) {
  const [failed, setFailed] =
    useState(false);

  /*
   * Actual third-party integrations:
   * use exactly the same theSVG URL convention
   * as the Workflow Builder.
   */

  if (
    app.kind === "brand" &&
    app.iconSlug &&
    !failed
  ) {
    return (
      <div
        className="flex size-10 items-center justify-center rounded-lg border border-slate-200 bg-white"
        title={app.name}
      >
        <img
          src={theSvgUrl(app.iconSlug)}
          alt={app.name}
          width={22}
          height={22}
          loading="lazy"
          decoding="async"
          referrerPolicy="no-referrer"
          draggable={false}
          className="size-[22px] object-contain"
          onError={() =>
            setFailed(true)
          }
        />
      </div>
    );
  }

  /*
   * FlowOps-native nodes do NOT pretend to be
   * third-party brands.
   */

  return (
    <div
      className="flex size-10 items-center justify-center rounded-lg border border-slate-200 bg-slate-50"
      style={{
        color: app.color,
      }}
      title={app.name}
    >
      {app.key === "ai" ? (
        <Bot className="size-5" />
      ) : app.key === "approval" ? (
        <UserCheck className="size-5" />
      ) : app.key === "webhook" ? (
        <Webhook className="size-5" />
      ) : app.key === "email" ? (
        <Mail className="size-5" />
      ) : app.key === "http" ? (
        <Globe2 className="size-5" />
      ) : app.key === "calendar" ? (
        <CalendarDays className="size-5" />
      ) : (
        <Bot className="size-5" />
      )}
    </div>
  );
}

/* ==========================================================================
   TEMPLATE PROVIDER → APP RESOLUTION
   ========================================================================== */



/* ==========================================================================
   EMPTY TEMPLATES
   ========================================================================== */

function EmptyTemplates({
  search,
  hasFilters,
  onClear,
}: {
  search: string;
  hasFilters: boolean;
  onClear: () => void;
}) {
  return (
    <div className="rounded-lg border border-dashed border-white/[0.08] bg-white/[0.012] px-5 py-16 text-center">
      <div className="mx-auto flex size-11 items-center justify-center rounded-lg border border-white/[0.07] bg-white/[0.025] text-white/30">
        <PanelsTopLeft className="size-5" />
      </div>

      <h3 className="mt-4 text-sm font-medium text-white/70">
        No templates found
      </h3>

      <p className="mx-auto mt-1 max-w-sm text-xs leading-5 text-white/30">
        {search
          ? `Nothing matches "${search}". Try a different search or clear the filters.`
          : "There are no templates available yet."}
      </p>

      {hasFilters && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={onClear}
          className="mt-5 h-8 rounded-md border-white/[0.08] bg-white/[0.025] text-xs text-white/60 hover:bg-white/[0.06] hover:text-white"
        >
          Clear filters
        </Button>
      )}
    </div>
  );
}

/* ==========================================================================
   LOADING
   ========================================================================== */

function TemplateLoading() {
  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
      {Array.from({
        length: 6,
      }).map((_, index) => (
        <Skeleton
          key={index}
          className="h-[368px] rounded-lg bg-white/[0.035]"
        />
      ))}
    </div>
  );
}