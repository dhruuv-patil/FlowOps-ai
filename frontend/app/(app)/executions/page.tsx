"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { Activity, ExternalLink, Search, X } from "lucide-react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";

import {
  fetchExecutionEvents,
  fetchExecutions,
  getErrorMessage,
} from "@/lib/api";

import type {
  ExecutionEvent,
  ExecutionStatus,
} from "@/types";

import {
  executionStatusVariant,
  formatDuration,
  formatRelativeTime,
  isLiveStatus,
} from "@/lib/format";

import { cn } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";

type SourceFilter = "ALL" | "INTERNAL" | "EXTERNAL";

const STATUS_FILTERS: {
  label: string;
  value: ExecutionStatus | "ALL";
}[] = [
  { label: "All statuses", value: "ALL" },
  { label: "Running", value: "RUNNING" },
  { label: "Waiting", value: "WAITING" },
  { label: "Succeeded", value: "SUCCEEDED" },
  { label: "Failed", value: "FAILED" },
  { label: "Canceled", value: "CANCELED" },
];

interface MonitoredExecution {
  key: string;
  source: string;
  workflowExternalId: string;
  workflowName: string;
  executionExternalId: string;
  status: ExecutionStatus | string;
  events: ExecutionEvent[];
  nodeCount: number;
  latestEvent: ExecutionEvent;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number | null;
}

type UnifiedExecution =
  | {
      kind: "INTERNAL";
      id: string;
      workflowName: string;
      status: ExecutionStatus;
      createdAt: string;
      durationMs: number | null;
      versionNumber: number;
      triggerType: string;
      error: string | null;
      searchText: string;
    }
  | {
      kind: "EXTERNAL";
      key: string;
      workflowName: string;
      status: string;
      createdAt: string;
      durationMs: number | null;
      source: string;
      executionExternalId: string;
      workflowExternalId: string;
      nodeCount: number;
      searchText: string;
    };

export default function ExecutionsPage() {
  const [source, setSource] = useState<SourceFilter>("ALL");
  const [status, setStatus] =
    useState<ExecutionStatus | "ALL">("ALL");
  const [search, setSearch] = useState("");

  const query = useQuery({
    queryKey: ["executions", { status }],
    queryFn: () =>
      fetchExecutions({
        status: status === "ALL" ? undefined : status,
      }),
    placeholderData: keepPreviousData,
    refetchInterval: (q) =>
      (q.state.data?.executions ?? []).some((e) =>
        isLiveStatus(e.status),
      )
        ? 4000
        : false,
  });

  const monitoredQuery = useQuery({
    queryKey: ["execution-events"],
    queryFn: () =>
      fetchExecutionEvents({
        limit: 100,
      }),
    refetchInterval: (q) =>
      (q.state.data?.events ?? []).some((event) =>
        isExternalLiveStatus(event.status),
      )
        ? 4000
        : false,
  });

  const executions = query.data?.executions ?? [];

  const monitoredExecutions = useMemo(
    () =>
      groupExternalExecutions(
        monitoredQuery.data?.events ?? [],
      ),
    [monitoredQuery.data?.events],
  );

  const unifiedExecutions = useMemo<UnifiedExecution[]>(
    () => [
      ...executions.map((execution) => ({
        kind: "INTERNAL" as const,
        id: execution.id,
        workflowName:
          execution.workflowName ?? "Untitled workflow",
        status: execution.status,
        createdAt: execution.createdAt,
        durationMs: execution.durationMs,
        versionNumber: execution.versionNumber,
        triggerType: execution.triggerType,
        error: execution.error ?? null,
        searchText: [
          execution.workflowName,
          execution.id,
          execution.triggerType,
          execution.error,
        ]
          .filter(Boolean)
          .join(" ")
          .toLowerCase(),
      })),
      ...monitoredExecutions.map((execution) => ({
        kind: "EXTERNAL" as const,
        key: execution.key,
        workflowName: execution.workflowName,
        status: execution.status,
        createdAt: execution.latestEvent.createdAt,
        durationMs: execution.durationMs,
        source: execution.source,
        executionExternalId: execution.executionExternalId,
        workflowExternalId: execution.workflowExternalId,
        nodeCount: execution.nodeCount,
        searchText: [
          execution.workflowName,
          execution.source,
          execution.executionExternalId,
          execution.workflowExternalId,
        ]
          .filter(Boolean)
          .join(" ")
          .toLowerCase(),
      })),
    ],
    [executions, monitoredExecutions],
  );

  const visibleExecutions = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();

    return unifiedExecutions
      .filter((execution) => {
        if (
          source !== "ALL" &&
          execution.kind !== source
        ) {
          return false;
        }

        if (
          status !== "ALL" &&
          execution.status.toUpperCase() !== status
        ) {
          return false;
        }

        if (
          normalizedSearch &&
          !execution.searchText.includes(normalizedSearch)
        ) {
          return false;
        }

        return true;
      })
      .sort(
        (a, b) =>
          Date.parse(b.createdAt) -
          Date.parse(a.createdAt),
      );
  }, [
    unifiedExecutions,
    source,
    status,
    search,
  ]);

  const stats = useMemo(() => {
    const all = unifiedExecutions;
    const active = all.filter((execution) =>
      isAnyLiveStatus(execution.status),
    ).length;
    const succeeded = all.filter(
      (execution) =>
        execution.status.toUpperCase() === "SUCCEEDED",
    ).length;
    const completed = all.filter((execution) =>
      ["SUCCEEDED", "FAILED", "CANCELED"].includes(
        execution.status.toUpperCase(),
      ),
    );

    const avgDuration =
      completed.length > 0
        ? completed.reduce(
            (sum, execution) =>
              sum + (execution.durationMs ?? 0),
            0,
          ) / completed.length
        : null;

    return {
      total: all.length,
      active,
      succeeded,
      successRate:
        completed.length > 0
          ? Math.round(
              (succeeded / completed.length) * 1000,
            ) / 10
          : null,
      avgDuration,
      internal: all.filter(
        (execution) => execution.kind === "INTERNAL",
      ).length,
      external: all.filter(
        (execution) => execution.kind === "EXTERNAL",
      ).length,
    };
  }, [unifiedExecutions]);

  const hasFilters =
    source !== "ALL" ||
    status !== "ALL" ||
    search.trim().length > 0;

  const clearFilters = () => {
    setSource("ALL");
    setStatus("ALL");
    setSearch("");
  };

  const loading =
    query.isPending || monitoredQuery.isPending;

  const hasError =
    query.isError || monitoredQuery.isError;

  return (
    <div className="mx-auto w-full max-w-5xl space-y-6 pb-10">
      <div>
        <p className="mono-eyebrow">
          Execution
        </p>

        <h1 className="mt-1 text-xl font-semibold tracking-tight text-white/90">
          Executions
        </h1>

        <p className="mt-1 text-sm text-white/44">
          Every run in this workspace, newest first.
        </p>
      </div>

      {/* Operational snapshot */}
      <section
        aria-label="Execution summary"
        className="grid grid-cols-2 overflow-hidden rounded-xl border border-white/[0.08] bg-white/[0.02] sm:grid-cols-4"
      >
        <Metric
          label="Total runs"
          value={stats.total.toLocaleString()}
        />
        <Metric
          label="Active"
          value={stats.active.toLocaleString()}
          emphasis={stats.active > 0}
        />
        <Metric
          label="Success rate"
          value={
            stats.successRate === null
              ? "—"
              : `${stats.successRate}%`
          }
        />
        <Metric
          label="Avg duration"
          value={formatDuration(stats.avgDuration)}
        />
      </section>

      {/* Primary source navigation */}
      <section className="space-y-3">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex w-fit rounded-full border border-white/[0.08] bg-white/[0.025] p-0.5">
            {(
              [
                ["ALL", "All"],
                ["INTERNAL", "Internal"],
                ["EXTERNAL", "External"],
              ] as const
            ).map(([value, label]) => {
              const active = source === value;

              return (
                <button
                  key={value}
                  type="button"
                  onClick={() => setSource(value)}
                  className={cn(
                    "rounded-full px-3.5 py-1.5 text-xs font-medium transition",
                    active
                      ? "bg-white text-black shadow-sm"
                      : "text-white/45 hover:text-white/75",
                  )}
                >
                  {label}
                </button>
              );
            })}
          </div>

          <div className="text-xs text-white/30">
            {source === "ALL"
              ? "Internal + external runs"
              : source === "INTERNAL"
                ? "Executed by FlowOps"
                : "Monitored from connected providers"}
          </div>
        </div>

        {/* Search + filters */}
        <div className="flex flex-col gap-2 md:flex-row">
          <label className="relative min-w-0 flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-white/25" />
            <input
              value={search}
              onChange={(event) =>
                setSearch(event.target.value)
              }
              placeholder="Search workflow, execution ID, provider..."
              className="h-9 w-full rounded-lg border border-white/[0.08] bg-white/[0.025] pl-9 pr-9 text-xs text-white/80 outline-none placeholder:text-white/25 focus:border-white/[0.16] focus:bg-white/[0.04]"
            />
            {search && (
              <button
                type="button"
                aria-label="Clear search"
                onClick={() => setSearch("")}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded p-1 text-white/30 hover:text-white/70"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            )}
          </label>

          <select
            value={status}
            onChange={(event) =>
              setStatus(
                event.target.value as
                  | ExecutionStatus
                  | "ALL",
              )
            }
            className="h-9 rounded-lg border border-white/[0.08] bg-[#111111] px-3 text-xs text-white/60 outline-none focus:border-white/[0.16]"
            aria-label="Filter by status"
          >
            {STATUS_FILTERS.map((filter) => (
              <option
                key={filter.value}
                value={filter.value}
              >
                {filter.label}
              </option>
            ))}
          </select>

          {hasFilters && (
            <button
              type="button"
              onClick={clearFilters}
              className="h-9 shrink-0 rounded-lg border border-white/[0.08] px-3 text-xs text-white/40 transition hover:bg-white/[0.04] hover:text-white/75"
            >
              Clear
            </button>
          )}
        </div>
      </section>

      {/* Main execution feed */}
      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-white/80">
              Recent executions
            </p>
            <p className="mt-0.5 text-xs text-white/30">
              {hasFilters
                ? `${visibleExecutions.length} matching ${
                    visibleExecutions.length === 1
                      ? "run"
                      : "runs"
                  }`
                : "Newest first"}
            </p>
          </div>

          {visibleExecutions.length > 0 && (
            <span className="font-mono text-[10px] text-white/25">
              {visibleExecutions.length} shown
            </span>
          )}
        </div>

        {loading ? (
          <ExecutionListSkeleton />
        ) : hasError ? (
          <ErrorState
            message={
              query.isError
                ? getErrorMessage(query.error)
                : getErrorMessage(monitoredQuery.error)
            }
            onRetry={() => {
              void query.refetch();
              void monitoredQuery.refetch();
            }}
          />
        ) : visibleExecutions.length === 0 ? (
          <EmptyState
            title={
              hasFilters
                ? "No matching executions"
                : "No executions yet"
            }
            description={
              hasFilters
                ? "Try a different source, status, or search term."
                : "Run a FlowOps workflow or connect a provider to see executions here."
            }
            action={
              hasFilters ? (
                <button
                  type="button"
                  onClick={clearFilters}
                  className="mt-4 rounded-lg border border-white/[0.09] bg-white/[0.04] px-3 py-2 text-xs text-white/60 hover:bg-white/[0.07] hover:text-white"
                >
                  Clear filters
                </button>
              ) : undefined
            }
          />
        ) : (
          <ul className="overflow-hidden rounded-xl border border-white/[0.08] bg-white/[0.02]">
            {visibleExecutions.map((execution, index) => (
              <ExecutionRow
                key={
                  execution.kind === "INTERNAL"
                    ? `internal:${execution.id}`
                    : `external:${execution.key}`
                }
                execution={execution}
                showDivider={index < visibleExecutions.length - 1}
              />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function ExecutionRow({
  execution,
  showDivider,
}: {
  execution: UnifiedExecution;
  showDivider: boolean;
}) {
  const live = isAnyLiveStatus(execution.status);

  if (execution.kind === "INTERNAL") {
    return (
      <li
        className={cn(
          showDivider && "border-b border-white/[0.06]",
        )}
      >
        <Link
          href={`/executions/${execution.id}`}
          className="group flex min-w-0 items-center gap-3 px-4 py-3.5 transition hover:bg-white/[0.035] sm:gap-4"
        >
          <StatusDot
            status={execution.status}
            live={live}
          />

          <Badge
            variant={executionStatusVariant(
              execution.status,
            )}
          >
            {execution.status.toLowerCase()}
          </Badge>

          <span className="min-w-0 flex-1">
            <span className="flex min-w-0 items-center gap-2">
              <span className="truncate text-xs font-medium text-white/90">
                {execution.workflowName}
              </span>

              <SourceBadge label="Internal" />
            </span>

            <span className="mt-0.5 block truncate font-mono text-[10px] text-white/35">
              v{execution.versionNumber} ·{" "}
              {execution.triggerType.toLowerCase()} trigger
              {execution.error
                ? ` · ${execution.error}`
                : ""}
            </span>
          </span>

          <ExecutionMeta
            createdAt={execution.createdAt}
            durationMs={execution.durationMs}
          />
        </Link>
      </li>
    );
  }

  return (
    <li
      className={cn(
        showDivider && "border-b border-white/[0.06]",
      )}
    >
      <Link
        href={`/executions/external/${encodeURIComponent(
          execution.executionExternalId,
        )}?workflowExternalId=${encodeURIComponent(
          execution.workflowExternalId,
        )}`}
        className="group flex min-w-0 items-center gap-3 px-4 py-3.5 transition hover:bg-white/[0.035] sm:gap-4"
      >
        <ExternalStatusDot
          status={execution.status}
          live={live}
        />

        <ExternalStatusBadge
          status={execution.status}
        />

        <span className="min-w-0 flex-1">
          <span className="flex min-w-0 items-center gap-2">
            <span className="truncate text-xs font-medium text-white/90">
              {execution.workflowName}
            </span>

            <SourceBadge label="External" />
            <SourceBadge
              label={execution.source}
              muted
            />
          </span>

          <span className="mt-0.5 block truncate font-mono text-[10px] text-white/35">
            run {execution.executionExternalId} ·{" "}
            {execution.nodeCount}{" "}
            {execution.nodeCount === 1
              ? "node"
              : "nodes"}
          </span>
        </span>

        <ExecutionMeta
          createdAt={execution.createdAt}
          durationMs={execution.durationMs}
        />

        <ExternalLink className="hidden h-3.5 w-3.5 shrink-0 text-white/15 transition group-hover:text-white/40 sm:block" />
      </Link>
    </li>
  );
}

function Metric({
  label,
  value,
  emphasis = false,
}: {
  label: string;
  value: string;
  emphasis?: boolean;
}) {
  return (
    <div className="border-b border-white/[0.06] px-4 py-3.5 first:border-r sm:border-b-0 sm:border-r last:border-r-0">
      <p className="font-mono text-[9px] uppercase tracking-wider text-white/25">
        {label}
      </p>
      <p
        className={cn(
          "mt-1 text-base font-semibold tabular-nums",
          emphasis ? "text-amber-300/90" : "text-white/80",
        )}
      >
        {value}
      </p>
    </div>
  );
}

function SourceBadge({
  label,
  muted = false,
}: {
  label: string;
  muted?: boolean;
}) {
  return (
    <span
      className={cn(
        "hidden shrink-0 rounded border px-1.5 py-0.5 font-mono text-[9px] uppercase tracking-wider sm:inline-flex",
        muted
          ? "border-white/[0.06] bg-white/[0.02] text-white/25"
          : "border-white/[0.08] bg-white/[0.025] text-white/35",
      )}
    >
      {label}
    </span>
  );
}

function ExecutionMeta({
  createdAt,
  durationMs,
}: {
  createdAt: string;
  durationMs: number | null;
}) {
  return (
    <span className="hidden shrink-0 text-right font-mono text-[10px] text-white/40 sm:block">
      <span className="block">
        {formatRelativeTime(createdAt)}
      </span>
      <span className="mt-0.5 block opacity-60">
        {formatDuration(durationMs)}
      </span>
    </span>
  );
}

function StatusDot({
  status,
  live,
}: {
  status: ExecutionStatus;
  live: boolean;
}) {
  return (
    <span
      className={cn(
        "h-2 w-2 shrink-0 rounded-full",
        statusDotColor(status),
        live && "animate-pulse",
      )}
    />
  );
}

function ExternalStatusDot({
  status,
  live,
}: {
  status: string;
  live: boolean;
}) {
  return (
    <span
      className={cn(
        "h-2 w-2 shrink-0 rounded-full",
        externalStatusDotColor(status),
        live && "animate-pulse",
      )}
    />
  );
}

/* -------------------------------------------------------------------------- */
/* External execution grouping                                                 */
/* -------------------------------------------------------------------------- */

function groupExternalExecutions(
  events: ExecutionEvent[],
): MonitoredExecution[] {
  const groups = new Map<
    string,
    ExecutionEvent[]
  >();

  for (const event of events) {
    const key = [
      event.source,
      event.workflowExternalId,
      event.executionExternalId,
    ].join(":");

    const existing = groups.get(key);

    if (existing) {
      existing.push(event);
    } else {
      groups.set(key, [event]);
    }
  }

  return Array.from(groups.entries())
    .map(([key, group]) => {
      const sorted = [...group].sort(
        (a, b) =>
          Date.parse(b.createdAt) -
          Date.parse(a.createdAt),
      );

      const latestEvent = sorted[0];
      const status = deriveExternalStatus(group);

      const stepIds = new Set(
        group
          .map((event) => event.stepExternalId)
          .filter(Boolean),
      );

      const workflowName =
        getExternalWorkflowName(latestEvent);

      const startedAt = earliestTimestamp(
        group.map((event) => event.startedAt),
      );

      const finishedAt = latestTimestamp(
        group.map((event) => event.finishedAt),
      );

      const durationMs =
        startedAt && finishedAt
          ? Math.max(
              0,
              Date.parse(finishedAt) -
                Date.parse(startedAt),
            )
          : null;

      return {
        key,
        source: latestEvent.source || "external",
        workflowExternalId:
          latestEvent.workflowExternalId,
        workflowName,
        executionExternalId:
          latestEvent.executionExternalId,
        status,
        events: sorted,
        nodeCount: stepIds.size || group.length,
        latestEvent,
        startedAt,
        finishedAt,
        durationMs,
      };
    })
    .sort(
      (a, b) =>
        Date.parse(b.latestEvent.createdAt) -
        Date.parse(a.latestEvent.createdAt),
    );
}

function deriveExternalStatus(
  events: ExecutionEvent[],
): string {
  const statuses = events.map((event) =>
    event.status.toUpperCase(),
  );

  if (statuses.includes("FAILED")) return "FAILED";
  if (statuses.includes("CANCELED")) return "CANCELED";
  if (statuses.includes("RUNNING")) return "RUNNING";
  if (statuses.includes("WAITING")) return "WAITING";
  if (statuses.includes("QUEUED")) return "QUEUED";
  if (statuses.includes("SUCCEEDED")) return "SUCCEEDED";
  if (statuses.includes("SKIPPED")) return "SKIPPED";

  return statuses[0] ?? "UNKNOWN";
}

function getExternalWorkflowName(
  event: ExecutionEvent,
): string {
  const metadata = event.metadata;

  if (metadata) {
    const name = metadata.workflowName;

    if (
      typeof name === "string" &&
      name.trim()
    ) {
      return name;
    }

    const workflow = metadata.workflow;

    if (
      typeof workflow === "string" &&
      workflow.trim()
    ) {
      return workflow;
    }
  }

  return (
    event.workflowExternalId ||
    "External workflow"
  );
}

function earliestTimestamp(
  values: Array<
    string | null | undefined
  >,
): string | null {
  const valid = values.filter(
    (value): value is string =>
      Boolean(value),
  );

  if (valid.length === 0) return null;

  return valid.reduce((earliest, value) =>
    Date.parse(value) < Date.parse(earliest)
      ? value
      : earliest,
  );
}

function latestTimestamp(
  values: Array<
    string | null | undefined
  >,
): string | null {
  const valid = values.filter(
    (value): value is string =>
      Boolean(value),
  );

  if (valid.length === 0) return null;

  return valid.reduce((latest, value) =>
    Date.parse(value) > Date.parse(latest)
      ? value
      : latest,
  );
}

/* -------------------------------------------------------------------------- */
/* Status UI                                                                   */
/* -------------------------------------------------------------------------- */

function ExternalStatusBadge({
  status,
}: {
  status: string;
}) {
  const normalized = status.toUpperCase();

  return (
    <span
      className={cn(
        "shrink-0 rounded-full border px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide",
        externalStatusClasses(normalized),
      )}
    >
      {normalized.toLowerCase()}
    </span>
  );
}

function externalStatusClasses(
  status: string,
): string {
  switch (status) {
    case "SUCCEEDED":
      return "border-emerald-500/20 bg-emerald-500/10 text-emerald-300";
    case "FAILED":
      return "border-red-500/20 bg-red-500/10 text-red-300";
    case "RUNNING":
      return "border-amber-500/20 bg-amber-500/10 text-amber-300";
    case "WAITING":
      return "border-blue-500/20 bg-blue-500/10 text-blue-300";
    case "CANCELED":
      return "border-white/10 bg-white/[0.04] text-white/50";
    case "QUEUED":
      return "border-purple-500/20 bg-purple-500/10 text-purple-300";
    case "SKIPPED":
      return "border-white/10 bg-white/[0.04] text-white/40";
    default:
      return "border-white/10 bg-white/[0.04] text-white/45";
  }
}

function externalStatusDotColor(
  status: string,
): string {
  switch (status.toUpperCase()) {
    case "SUCCEEDED":
      return "bg-emerald-500";
    case "FAILED":
      return "bg-red-500";
    case "RUNNING":
      return "bg-amber-500";
    case "WAITING":
      return "bg-blue-500";
    case "QUEUED":
      return "bg-purple-500";
    default:
      return "bg-white/20";
  }
}

function statusDotColor(
  status: ExecutionStatus,
): string {
  switch (status) {
    case "SUCCEEDED":
      return "bg-emerald-500";
    case "FAILED":
      return "bg-red-500";
    case "RUNNING":
      return "bg-amber-500";
    case "WAITING":
      return "bg-blue-500";
    case "QUEUED":
      return "bg-purple-500";
    default:
      return "bg-white/20";
  }
}

function isExternalLiveStatus(
  status: string,
): boolean {
  const normalized = status.toUpperCase();

  return (
    normalized === "RUNNING" ||
    normalized === "WAITING" ||
    normalized === "QUEUED"
  );
}

function isAnyLiveStatus(
  status: string,
): boolean {
  return isExternalLiveStatus(status);
}

/* -------------------------------------------------------------------------- */
/* Shared UI                                                                   */
/* -------------------------------------------------------------------------- */

function ExecutionListSkeleton() {
  return (
    <div className="overflow-hidden rounded-xl border border-white/[0.08] bg-white/[0.02]">
      {Array.from({ length: 6 }).map((_, index) => (
        <div
          key={index}
          className="flex items-center gap-3 border-b border-white/[0.06] px-4 py-3.5 last:border-0"
        >
          <Skeleton className="h-2 w-2 rounded-full" />
          <Skeleton className="h-5 w-16 rounded-full" />

          <span className="flex min-w-0 flex-1 flex-col gap-1.5">
            <Skeleton className="h-3 w-40 max-w-full" />
            <Skeleton className="h-2.5 w-56 max-w-full" />
          </span>

          <span className="hidden flex-col items-end gap-1 sm:flex">
            <Skeleton className="h-2.5 w-16" />
            <Skeleton className="h-2.5 w-12" />
          </span>
        </div>
      ))}
    </div>
  );
}

function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry: () => void;
}) {
  return (
    <div className="rounded-xl border border-red-500/10 bg-red-500/[0.03] px-4 py-5">
      <p className="text-sm text-red-300/80">
        Unable to load executions.
      </p>
      <p className="mt-1 text-xs text-white/35">
        {message}
      </p>
      <button
        type="button"
        onClick={onRetry}
        className="mt-2 text-xs text-white/50 underline underline-offset-2 hover:text-white/80"
      >
        Retry
      </button>
    </div>
  );
}

function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-white/[0.08] bg-white/[0.015] px-6 py-12 text-center">
      <Activity className="h-5 w-5 text-white/20" />

      <p className="mt-3 text-sm font-medium text-white/55">
        {title}
      </p>

      <p className="mt-1 max-w-md text-xs leading-5 text-white/30">
        {description}
      </p>

      {action}
    </div>
  );
}
