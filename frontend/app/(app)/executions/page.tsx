"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import {
  Activity,
  ArrowUpRight,
  Clock3,
  ExternalLink,
  Search,
  X,
} from "lucide-react";
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
        executionExternalId:
          execution.executionExternalId,
        workflowExternalId:
          execution.workflowExternalId,
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

    const failed = all.filter(
      (execution) =>
        execution.status.toUpperCase() === "FAILED",
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
      failed,
      successRate:
        completed.length > 0
          ? Math.round(
            (succeeded / completed.length) * 1000,
          ) / 10
          : null,
      avgDuration,
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
    <div className="w-full px-8 pb-12 lg:px-8">

      {/* ================================================================ */}
      {/* PAGE HEADER                                                       */}
      {/* ================================================================ */}

      <section className="mb-6">
        <div className="flex items-end justify-between gap-6">

          <div>
            <div className="mb-3 flex items-center gap-2">
              <div className="flex h-7 w-7 items-center justify-center rounded-md border border-white/[0.08] bg-white/[0.025]">
                <Activity className="h-3.5 w-3.5 text-white/35" />
              </div>

              <p className="font-mono text-[10px] uppercase tracking-[0.18em] text-white/25">
                Execution Control Plane
              </p>
            </div>

            <h1 className="text-[27px] font-semibold leading-tight tracking-[-0.025em] text-white/90">
              Executions
            </h1>

            <p className="mt-1.5 text-sm text-white/35">
              Monitor workflow runs, execution health and
              connected provider activity.
            </p>
          </div>

          {/* LIVE STATE */}
          <div className="mb-1 hidden items-center gap-2 rounded-lg border border-white/[0.07] bg-white/[0.02] px-3 py-2 md:flex">
            <span className="h-1.5 w-1.5 rounded-full bg-white/40" />

            <span className="font-mono text-[9px] uppercase tracking-[0.14em] text-white/30">
              Live monitoring
            </span>
          </div>
        </div>
      </section>

      {/* ================================================================ */}
      {/* OPERATIONAL SNAPSHOT                                             */}
      {/* ================================================================ */}

      <section
        aria-label="Execution summary"
        className="mb-6 grid grid-cols-2 overflow-hidden rounded-xl border border-white/[0.08] bg-[#0b0b0c] md:grid-cols-4"
      >
        <Metric
          label="Workflow runs"
          value={stats.total.toLocaleString()}
          detail="All executions"
        />

        <Metric
          label="Success rate"
          value={
            stats.successRate === null
              ? "—"
              : `${stats.successRate}%`
          }
          detail={
            stats.successRate === null
              ? "No completed runs"
              : "Completed runs"
          }
        />

        <Metric
          label="Failed runs"
          value={stats.failed.toLocaleString()}
          detail={
            stats.failed === 0
              ? "No failures"
              : "Requires attention"
          }
          tone={
            stats.failed > 0
              ? "warning"
              : "neutral"
          }
        />

        <Metric
          label="Active"
          value={stats.active.toLocaleString()}
          detail={
            stats.active > 0
              ? "Currently running"
              : "No active runs"
          }
          tone={
            stats.active > 0
              ? "active"
              : "neutral"
          }
        />
      </section>

      {/* FILTERS */}
      <section className="mb-6">
        <div className="flex flex-col gap-2 lg:flex-row lg:items-center">

          {/* SEARCH */}
          <label className="relative min-w-0 flex-1">
            <Search className="pointer-events-none absolute left-3.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-white/25" />

            <input
              value={search}
              onChange={(event) =>
                setSearch(event.target.value)
              }
              placeholder="Search executions..."
              className="h-10 w-full rounded-md border border-white/[0.08] bg-[#0b0b0c] pl-10 pr-10 text-xs text-white/70 outline-none transition placeholder:text-white/25 focus:border-white/[0.14]"
            />

            {search && (
              <button
                type="button"
                onClick={() => setSearch("")}
                aria-label="Clear search"
                className="absolute right-3 top-1/2 -translate-y-1/2 text-white/25 transition hover:text-white/60"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            )}
          </label>

          {/* SOURCE FILTER */}
          <div className="flex shrink-0 items-center gap-1">
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
                    "h-10 rounded-md border px-3 text-xs transition",
                    active
                      ? "border-white/[0.08] bg-white text-black"
                      : "border-white/[0.07] bg-[#0b0b0c] text-white/35 hover:bg-white/[0.035] hover:text-white/65",
                  )}
                >
                  {label}
                </button>
              );
            })}
          </div>

          {/* STATUS FILTER */}
          <div className="flex shrink-0 items-center gap-1">
            {STATUS_FILTERS.map((filter) => {
              const active = status === filter.value;

              return (
                <button
                  key={filter.value}
                  type="button"
                  onClick={() =>
                    setStatus(filter.value)
                  }
                  className={cn(
                    "h-10 rounded-md border px-3 text-xs transition",
                    active
                      ? "border-white/[0.08] bg-white text-black"
                      : "border-white/[0.07] bg-[#0b0b0c] text-white/35 hover:bg-white/[0.035] hover:text-white/65",
                  )}
                >
                  {filter.value === "ALL"
                    ? "All"
                    : filter.label}
                </button>
              );
            })}
          </div>

          {/* CLEAR */}
          {hasFilters && (
            <button
              type="button"
              onClick={clearFilters}
              className="h-10 shrink-0 rounded-md border border-white/[0.07] bg-[#0b0b0c] px-3 text-xs text-white/30 transition hover:bg-white/[0.035] hover:text-white/60"
            >
              Clear
            </button>
          )}
        </div>
      </section>
      {/* ================================================================ */}
      {/* EXECUTION LIST                                                    */}
      {/* ================================================================ */}

      <section>
        <div className="mb-3 flex items-end justify-between">
          <div>
            <h2 className="text-sm font-medium text-white/70">
              Recent executions
            </h2>

            <p className="mt-1 text-xs text-white/25">
              {hasFilters
                ? `${visibleExecutions.length} matching ${visibleExecutions.length === 1
                  ? "run"
                  : "runs"
                }`
                : "Newest first"}
            </p>
          </div>

          {visibleExecutions.length > 0 && (
            <span className="font-mono text-[9px] uppercase tracking-[0.12em] text-white/20">
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
                : getErrorMessage(
                  monitoredQuery.error,
                )
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
                  className="mt-4 rounded-lg border border-white/[0.08] bg-white/[0.03] px-3 py-2 text-xs text-white/45 hover:bg-white/[0.06] hover:text-white/70"
                >
                  Clear filters
                </button>
              ) : undefined
            }
          />
        ) : (
          <div className="overflow-hidden rounded-xl border border-white/[0.08] bg-[#0b0b0c]">

            {/* TABLE HEADER */}
            <div className="hidden h-10 items-center border-b border-white/[0.055] bg-white/[0.012] px-4 font-mono text-[9px] uppercase tracking-[0.13em] text-white/20 md:flex">
              <div className="w-[115px]">
                Status
              </div>

              <div className="flex-1">
                Workflow
              </div>

              <div className="w-[110px] text-right">
                Duration
              </div>

              <div className="w-[105px] text-right">
                Started
              </div>

              <div className="w-5" />
            </div>

            <ul>
              {visibleExecutions.map(
                (execution, index) => (
                  <ExecutionRow
                    key={
                      execution.kind === "INTERNAL"
                        ? `internal:${execution.id}`
                        : `external:${execution.key}`
                    }
                    execution={execution}
                    showDivider={
                      index <
                      visibleExecutions.length - 1
                    }
                  />
                ),
              )}
            </ul>
          </div>
        )}
      </section>
    </div>
  );
}

/* ========================================================================== */
/* METRIC                                                                    */
/* ========================================================================== */

function Metric({
  label,
  value,
  detail,
  tone = "neutral",
}: {
  label: string;
  value: string;
  detail: string;
  tone?: "neutral" | "warning" | "active";
}) {
  return (
    <div className="min-h-[116px] border-b border-white/[0.055] px-5 py-4 md:border-b-0 md:border-r md:last:border-r-0">
      <p className="font-mono text-[9px] uppercase tracking-[0.14em] text-white/25">
        {label}
      </p>

      <p className="mt-2 text-[25px] font-semibold leading-none tracking-[-0.02em] text-white/85">
        {value}
      </p>

      <div className="mt-3 flex items-center gap-2">
        <span
          className={cn(
            "h-1.5 w-1.5 rounded-full",
            tone === "warning"
              ? "bg-amber-500/70"
              : tone === "active"
                ? "bg-white/45"
                : "bg-white/15",
          )}
        />

        <span className="text-[10px] text-white/30">
          {detail}
        </span>
      </div>
    </div>
  );
}

/* ========================================================================== */
/* EXECUTION ROW                                                             */
/* ========================================================================== */

function ExecutionRow({
  execution,
  showDivider,
}: {
  execution: UnifiedExecution;
  showDivider: boolean;
}) {
  const live = isAnyLiveStatus(
    execution.status,
  );

  if (execution.kind === "INTERNAL") {
    return (
      <li
        className={cn(
          showDivider &&
          "border-b border-white/[0.055]",
        )}
      >
        <Link
          href={`/executions/${execution.id}`}
          className="group flex min-w-0 items-center gap-3 px-4 py-4 transition hover:bg-white/[0.018] md:gap-4"
        >
          <StatusIndicator
            status={execution.status}
            live={live}
          />

          <div className="w-[96px] shrink-0">
            <StatusText
              status={execution.status}
            />
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex min-w-0 items-center gap-2">
              <span className="truncate text-xs font-medium text-white/75">
                {execution.workflowName}
              </span>

              <span className="hidden rounded border border-white/[0.06] px-1.5 py-0.5 font-mono text-[8px] uppercase tracking-[0.1em] text-white/20 lg:inline-flex">
                Internal
              </span>
            </div>

            <div className="mt-1 truncate font-mono text-[10px] text-white/22">
              v{execution.versionNumber} ·{" "}
              {execution.triggerType.toLowerCase()} trigger
              {execution.error
                ? ` · ${execution.error}`
                : ""}
            </div>
          </div>

          <ExecutionMeta
            createdAt={execution.createdAt}
            durationMs={execution.durationMs}
          />

          <ArrowUpRight className="hidden h-3.5 w-3.5 shrink-0 text-white/10 transition group-hover:text-white/35 md:block" />
        </Link>
      </li>
    );
  }

  return (
    <li
      className={cn(
        showDivider &&
        "border-b border-white/[0.055]",
      )}
    >
      <Link
        href={`/executions/external/${encodeURIComponent(
          execution.executionExternalId,
        )}?workflowExternalId=${encodeURIComponent(
          execution.workflowExternalId,
        )}`}
        className="group flex min-w-0 items-center gap-3 px-4 py-4 transition hover:bg-white/[0.018] md:gap-4"
      >
        <ExternalStatusIndicator
          status={execution.status}
          live={live}
        />

        <div className="w-[96px] shrink-0">
          <StatusText
            status={execution.status}
          />
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex min-w-0 items-center gap-2">
            <span className="truncate text-xs font-medium text-white/75">
              {execution.workflowName}
            </span>

            <span className="hidden rounded border border-white/[0.06] px-1.5 py-0.5 font-mono text-[8px] uppercase tracking-[0.1em] text-white/20 lg:inline-flex">
              External
            </span>

            <span className="hidden truncate font-mono text-[8px] uppercase tracking-[0.1em] text-white/15 xl:inline">
              {execution.source}
            </span>
          </div>

          <div className="mt-1 truncate font-mono text-[10px] text-white/22">
            run {execution.executionExternalId} ·{" "}
            {execution.nodeCount}{" "}
            {execution.nodeCount === 1
              ? "node"
              : "nodes"}
          </div>
        </div>

        <ExecutionMeta
          createdAt={execution.createdAt}
          durationMs={execution.durationMs}
        />

        <ExternalLink className="hidden h-3.5 w-3.5 shrink-0 text-white/10 transition group-hover:text-white/35 md:block" />
      </Link>
    </li>
  );
}

/* ========================================================================== */
/* STATUS                                                                     */
/* ========================================================================== */

function StatusIndicator({
  status,
  live,
}: {
  status: ExecutionStatus;
  live: boolean;
}) {
  return (
    <span
      className={cn(
        "h-1.5 w-1.5 shrink-0 rounded-full",
        statusIndicatorColor(status),
        live && "animate-pulse",
      )}
    />
  );
}

function ExternalStatusIndicator({
  status,
  live,
}: {
  status: string;
  live: boolean;
}) {
  return (
    <span
      className={cn(
        "h-1.5 w-1.5 shrink-0 rounded-full",
        externalStatusIndicatorColor(status),
        live && "animate-pulse",
      )}
    />
  );
}

function StatusText({
  status,
}: {
  status: string;
}) {
  return (
    <span
      className={cn(
        "text-[11px] capitalize",
        statusTextColor(status),
      )}
    >
      {status.toLowerCase()}
    </span>
  );
}

function statusIndicatorColor(
  status: ExecutionStatus,
) {
  switch (status) {
    case "FAILED":
      return "bg-red-400/60";

    case "RUNNING":
      return "bg-white/55";

    case "WAITING":
      return "bg-white/35";

    case "QUEUED":
      return "bg-white/30";

    case "SUCCEEDED":
      return "bg-emerald-400/55";

    default:
      return "bg-white/20";
  }
}

function externalStatusIndicatorColor(
  status: string,
) {
  switch (status.toUpperCase()) {
    case "FAILED":
      return "bg-red-400/60";

    case "RUNNING":
      return "bg-white/55";

    case "WAITING":
      return "bg-white/35";

    case "QUEUED":
      return "bg-white/30";

    case "SUCCEEDED":
      return "bg-emerald-400/55";

    default:
      return "bg-white/20";
  }
}

function statusTextColor(
  status: string,
) {
  switch (status.toUpperCase()) {
    case "FAILED":
      return "text-red-300/60";

    case "RUNNING":
      return "text-white/65";

    case "WAITING":
      return "text-white/45";

    case "QUEUED":
      return "text-white/40";

    case "SUCCEEDED":
      return "text-white/45";

    case "CANCELED":
      return "text-white/30";

    default:
      return "text-white/35";
  }
}

/* ========================================================================== */
/* META                                                                       */
/* ========================================================================== */

function ExecutionMeta({
  createdAt,
  durationMs,
}: {
  createdAt: string;
  durationMs: number | null;
}) {
  return (
    <div className="hidden w-[215px] shrink-0 items-center justify-end gap-6 font-mono text-[10px] md:flex">
      <span className="w-[65px] text-right text-white/30">
        {formatDuration(durationMs)}
      </span>

      <span className="w-[85px] text-right text-white/22">
        {formatRelativeTime(createdAt)}
      </span>
    </div>
  );
}

/* ========================================================================== */
/* EXTERNAL EXECUTION GROUPING                                               */
/* ========================================================================== */

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
        source:
          latestEvent.source || "external",
        workflowExternalId:
          latestEvent.workflowExternalId,
        workflowName,
        executionExternalId:
          latestEvent.executionExternalId,
        status,
        events: sorted,
        nodeCount:
          stepIds.size || group.length,
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
  if (statuses.includes("CANCELED"))
    return "CANCELED";
  if (statuses.includes("RUNNING"))
    return "RUNNING";
  if (statuses.includes("WAITING"))
    return "WAITING";
  if (statuses.includes("QUEUED"))
    return "QUEUED";
  if (statuses.includes("SUCCEEDED"))
    return "SUCCEEDED";
  if (statuses.includes("SKIPPED"))
    return "SKIPPED";

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
  values: Array<string | null | undefined>,
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
  values: Array<string | null | undefined>,
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

/* ========================================================================== */
/* LIVE STATE                                                                 */
/* ========================================================================== */

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

/* ========================================================================== */
/* LOADING                                                                    */
/* ========================================================================== */

function ExecutionListSkeleton() {
  return (
    <div className="overflow-hidden rounded-xl border border-white/[0.08] bg-[#0b0b0c]">
      {Array.from({ length: 6 }).map(
        (_, index) => (
          <div
            key={index}
            className="flex items-center gap-4 border-b border-white/[0.055] px-4 py-4 last:border-0"
          >
            <Skeleton className="h-1.5 w-1.5 rounded-full" />

            <Skeleton className="h-3 w-16 rounded" />

            <span className="flex min-w-0 flex-1 flex-col gap-2">
              <Skeleton className="h-3 w-40 max-w-full" />
              <Skeleton className="h-2.5 w-56 max-w-full" />
            </span>

            <span className="hidden items-center gap-5 md:flex">
              <Skeleton className="h-2.5 w-12" />
              <Skeleton className="h-2.5 w-16" />
            </span>
          </div>
        ),
      )}
    </div>
  );
}

/* ========================================================================== */
/* ERROR                                                                      */
/* ========================================================================== */

function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry: () => void;
}) {
  return (
    <div className="rounded-xl border border-white/[0.08] bg-[#0b0b0c] px-5 py-6">
      <p className="text-sm text-white/55">
        Unable to load executions.
      </p>

      <p className="mt-1 text-xs text-white/25">
        {message}
      </p>

      <button
        type="button"
        onClick={onRetry}
        className="mt-3 text-xs text-white/40 underline underline-offset-2 hover:text-white/70"
      >
        Retry
      </button>
    </div>
  );
}

/* ========================================================================== */
/* EMPTY                                                                      */
/* ========================================================================== */

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
    <div className="flex min-h-[300px] flex-col items-center justify-center rounded-xl border border-dashed border-white/[0.08] bg-[#0b0b0c] px-6 py-12 text-center">
      <div className="flex h-9 w-9 items-center justify-center rounded-lg border border-white/[0.07] bg-white/[0.02]">
        <Activity className="h-4 w-4 text-white/20" />
      </div>

      <p className="mt-4 text-sm font-medium text-white/55">
        {title}
      </p>

      <p className="mt-1 max-w-md text-xs leading-5 text-white/25">
        {description}
      </p>

      {action}
    </div>
  );
}