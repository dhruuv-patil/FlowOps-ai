"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { Search, X, AlertTriangle, CheckCircle2, Clock, XCircle } from "lucide-react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";

import {
  fetchExecutions,
  getErrorMessage,
} from "@/lib/api";

import type {
  ExecutionStatus,
  ExecutionSummary,
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
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";


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

interface WorkflowExecutionsViewProps {
  workflowId: string;
}

export function WorkflowExecutionsView({ workflowId }: WorkflowExecutionsViewProps) {
  const [status, setStatus] = useState<ExecutionStatus | "ALL">("ALL");
  const [search, setSearch] = useState("");

  const query = useQuery({
    queryKey: ["executions", "workflow", workflowId, { status }],
    queryFn: () =>
      fetchExecutions({
        workflowId,
        status: status === "ALL" ? undefined : status,
        limit: 100,
      }),
    placeholderData: keepPreviousData,
    refetchInterval: (q) =>
      (q.state.data?.executions ?? []).some((e) =>
        isLiveStatus(e.status),
      )
        ? 4000
        : false,
  });

  const executions = query.data?.executions ?? [];

  const visibleExecutions = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();

    return executions
      .filter((execution) => {
        if (
          status !== "ALL" &&
          execution.status.toUpperCase() !== status
        ) {
          return false;
        }

        // Build search text from execution fields
        const searchText = [
          execution.workflowName,
          execution.id,
          execution.triggerType,
          execution.error,
        ]
          .filter(Boolean)
          .join(" ")
          .toLowerCase();

        if (normalizedSearch && !searchText.includes(normalizedSearch)) {
          return false;
        }

        return true;
      })
      .sort(
        (a, b) =>
          Date.parse(b.createdAt) - Date.parse(a.createdAt),
      );
  }, [executions, status, search]);

  const stats = useMemo(() => {
    const all = executions;
    const active = all.filter((execution) =>
      isLiveStatus(execution.status),
    ).length;
    const succeeded = all.filter(
      (execution) =>
        execution.status.toUpperCase() === "SUCCEEDED",
    ).length;
    const failed = all.filter(
      (execution) =>
        execution.status.toUpperCase() === "FAILED",
    ).length;
    // Anomaly is not a native execution status, keep as 0 for now
    const anomalies = 0;
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
      anomalies,
      successRate:
        completed.length > 0
          ? Math.round((succeeded / completed.length) * 1000) / 10
          : null,
      avgDuration,
    };
  }, [executions]);

  const hasFilters = status !== "ALL" || search.trim().length > 0;

  const clearFilters = () => {
    setStatus("ALL");
    setSearch("");
  };

  const loading = query.isPending;
  const hasError = query.isError;

  return (
    <div className="space-y-4 pt-4">
      {/* Stats bar */}
      <div className="grid grid-cols-2 overflow-hidden rounded-xl border border-white/[0.065] bg-[#0D0D10] sm:grid-cols-5">
        {[
          ["Total", stats.total.toLocaleString(), "neutral"],
          ["Succeeded", stats.succeeded.toLocaleString(), "success"],
          ["Failed", stats.failed.toLocaleString(), "danger"],
          [
            "Anomalies",
            stats.anomalies.toLocaleString(),
            stats.anomalies > 0 ? "warning" : "neutral",
          ],
          ["Avg duration", formatDuration(stats.avgDuration), "neutral"],
        ].map(([label, value, tone], index) => (
          <div
            key={label}
            className={`min-h-[88px] px-4 py-3 ${
              index > 0 ? "border-l border-white/[0.05]" : ""
            }`}
          >
            <p className="text-[10px] font-medium text-white/35">
              {label}
            </p>
            <p className="mt-2 text-xl font-semibold tracking-[-0.03em] tabular-nums text-white/90">
              {value}
            </p>
            <span
              className={cn(
                "mt-2 block size-1.5 rounded-full",
                tone === "success" && "bg-emerald-400/75",
                tone === "danger" && "bg-red-400/70",
                tone === "warning" && "bg-amber-400/75",
                tone === "neutral" && "bg-white/20",
              )}
            />
          </div>
        ))}
      </div>

      {/* Search + filters */}
      <div className="flex flex-wrap items-center gap-2">
        <label className="relative min-w-0 flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-white/25" />
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search execution ID, trigger type, error..."
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
              event.target.value as ExecutionStatus | "ALL",
            )
          }
          className="h-9 rounded-lg border border-white/[0.08] bg-[#111111] px-3 text-xs text-white/60 outline-none focus:border-white/[0.16]"
          aria-label="Filter by status"
        >
          {STATUS_FILTERS.map((filter) => (
            <option key={filter.value} value={filter.value}>
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
                    visibleExecutions.length === 1 ? "run" : "runs"
                  }`
                : executions.length === 0
                ? "No executions yet"
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
        ) : visibleExecutions.length === 0 ? (
          <EmptyState
            title={hasFilters ? "No matching executions" : "No executions yet"}
            description={
              hasFilters
                ? "Try a different status or search term."
                : "Run this workflow to see executions here."
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
        ) : hasError ? (
          <ErrorState
            title="Unable to load executions."
            description={getErrorMessage(query.error)}
            action={
              <button
                type="button"
                onClick={() => void query.refetch()}
                className="mt-4 rounded-lg border border-white/[0.09] bg-white/[0.04] px-3 py-2 text-xs text-white/60 hover:bg-white/[0.07] hover:text-white"
              >
                Retry
              </button>
            }
          />
        ) : (
          <ul className="overflow-hidden rounded-xl border border-white/[0.08] bg-white/[0.02]">
            {visibleExecutions.map((execution, index) => (
              <ExecutionRow
                key={execution.id}
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
  execution: ExecutionSummary;
  showDivider: boolean;
}) {
  const live = isLiveStatus(execution.status);

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
        <StatusDot status={execution.status} live={live} />

        <Badge variant={executionStatusVariant(execution.status)}>
          {execution.status.toLowerCase()}
        </Badge>

        <span className="min-w-0 flex-1">
          <span className="flex min-w-0 items-center gap-2">
            <span className="truncate text-xs font-medium text-white/90">
              {execution.workflowName ?? "Untitled workflow"}
            </span>
          </span>

          <span className="mt-0.5 block truncate font-mono text-[10px] text-white/35">
            v{execution.versionNumber} ·{" "}
            {execution.triggerType.toLowerCase()} trigger
            {execution.error ? ` · ${execution.error}` : ""}
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

function StatCard({
  label,
  value,
  icon,
  emphasis = false,
}: {
  label: string;
  value: string;
  icon?: React.ReactNode;
  emphasis?: boolean;
}) {
  return (
    <Card className="!bg-[#0a0a0a] border-white/[0.08]">
      <CardContent className="flex items-center gap-3 px-3 py-2.5">
        {icon && (
          <div className="flex size-7 shrink-0 items-center justify-center rounded-md border border-white/[0.06] bg-white/[0.02]">
            {icon}
          </div>
        )}
        <div className="min-w-0">
          <p className="font-mono text-[9px] uppercase tracking-wider text-white/25">
            {label}
          </p>
          <p
            className={cn(
              "mt-0.5 text-sm font-semibold tabular-nums",
              emphasis ? "text-amber-300/90" : "text-white/80",
            )}
          >
            {value}
          </p>
        </div>
      </CardContent>
    </Card>
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
      <span className="block">{formatRelativeTime(createdAt)}</span>
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

function statusDotColor(status: ExecutionStatus): string {
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
      <p className="text-sm text-red-300/80">Unable to load executions.</p>
      <p className="mt-1 text-xs text-white/35">{message}</p>
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
      <AlertTriangle className="h-5 w-5 text-white/20" />

      <p className="mt-3 text-sm font-medium text-white/55">{title}</p>

      <p className="mt-1 max-w-md text-xs leading-5 text-white/30">
        {description}
      </p>

      {action}
    </div>
  );
}