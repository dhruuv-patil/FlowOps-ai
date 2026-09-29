"use client";

import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import {
  Activity,
  ArrowLeft,
  CheckCircle2,
  ChevronRight,
  Circle,
  Clock3,
  Copy,
  Layers3,
  XCircle,
} from "lucide-react";
import { Suspense, type ComponentType } from "react";

import { fetchExecutionEvents, getErrorMessage } from "@/lib/api";

import type { ExecutionEvent } from "@/types";

import {
  formatDuration,
  formatRelativeTime,
} from "@/lib/format";

import { cn } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";

function ExternalExecutionPageContent() {
  const params = useParams();
  const searchParams = useSearchParams();

  const executionExternalId = String(
    params.executionExternalId ?? "",
  );

  const workflowExternalId =
    searchParams.get("workflowExternalId") ?? "";

  const query = useQuery({
    queryKey: [
      "external-execution",
      executionExternalId,
      workflowExternalId,
    ],
    queryFn: () =>
      fetchExecutionEvents({
        limit: 100,
      }),
    enabled: Boolean(executionExternalId),
    refetchInterval: 4000,
  });

  const allEvents = query.data?.events ?? [];

  const events = allEvents
    .filter(
      (event) =>
        event.executionExternalId === executionExternalId &&
        (!workflowExternalId ||
          event.workflowExternalId === workflowExternalId),
    )
    .sort(
      (a, b) =>
        Date.parse(a.createdAt) -
        Date.parse(b.createdAt),
    );

  const workflowName = getWorkflowName(events[0]);

  const overallStatus =
    deriveExecutionStatus(events);

  const startedAt = earliestTimestamp(
    events.map((event) => event.startedAt),
  );

  const finishedAt = latestTimestamp(
    events.map((event) => event.finishedAt),
  );

  const durationMs =
    startedAt && finishedAt
      ? Math.max(
          0,
          Date.parse(finishedAt) -
            Date.parse(startedAt),
        )
      : null;

  const nodeCount = new Set(
    events.map(
      (event) => event.stepExternalId,
    ),
  ).size;

  const retryCount = events.reduce(
    (total, event) =>
      total + Math.max(event.retryCount ?? 0, 0),
    0,
  );

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      {/* ================================================================
          Header
      ================================================================= */}

      <div className="flex items-start gap-3">
        <Link
          href="/executions"
          className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-white/[0.08] bg-white/[0.02] text-white/45 transition hover:bg-white/[0.05] hover:text-white"
        >
          <ArrowLeft className="h-4 w-4" />
        </Link>

        <div className="min-w-0 flex-1">
          <p className="mono-eyebrow">
            External execution
          </p>

          <h1 className="mt-1 truncate text-xl font-semibold tracking-tight text-white/90">
            {workflowName}
          </h1>

          <div className="mt-1 flex flex-wrap items-center gap-2">
            <span className="font-mono text-[10px] text-white/30">
              {events[0]?.source ?? "external"}
            </span>

            <span className="text-white/15">·</span>

            <span className="font-mono text-[10px] text-white/30">
              run {executionExternalId}
            </span>
          </div>
        </div>

        {events.length > 0 && (
          <ExternalStatusBadge
            status={overallStatus}
          />
        )}
      </div>

      {/* ================================================================
          Loading
      ================================================================= */}

      {query.isPending && (
        <ExternalExecutionSkeleton />
      )}

      {/* ================================================================
          Error
      ================================================================= */}

      {query.isError && (
        <div className="rounded-xl border border-red-500/10 bg-red-500/[0.03] px-4 py-5">
          <p className="text-sm text-red-300/80">
            Unable to load this execution.
          </p>

          <p className="mt-1 text-xs text-white/35">
            {getErrorMessage(query.error)}
          </p>

          <button
            type="button"
            onClick={() => query.refetch()}
            className="mt-2 text-xs text-white/50 underline underline-offset-2 transition hover:text-white/80"
          >
            Retry
          </button>
        </div>
      )}

      {/* ================================================================
          Not found
      ================================================================= */}

      {!query.isPending &&
        !query.isError &&
        events.length === 0 && (
          <div className="rounded-xl border border-dashed border-white/[0.08] bg-white/[0.015] px-6 py-12 text-center">
            <Circle className="mx-auto h-5 w-5 text-white/20" />

            <p className="mt-3 text-sm font-medium text-white/55">
              Execution not found
            </p>

            <p className="mx-auto mt-1 max-w-md text-xs leading-5 text-white/30">
              No captured events were found for
              execution{" "}
              <span className="font-mono text-white/40">
                {executionExternalId}
              </span>
              .
            </p>

            <Link
              href="/executions"
              className="mt-4 inline-flex items-center gap-1 text-xs text-white/50 transition hover:text-white/80"
            >
              Back to executions
              <ChevronRight className="h-3.5 w-3.5" />
            </Link>
          </div>
        )}

      {/* ================================================================
          Execution
      ================================================================= */}

      {events.length > 0 && (
        <>
          {/* ============================================================
              Summary
          ============================================================= */}

          <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <SummaryCard
              icon={Activity}
              label="Status"
              value={overallStatus.toLowerCase()}
              accent={statusAccent(overallStatus)}
            />

            <SummaryCard
              icon={Layers3}
              label="Nodes"
              value={String(nodeCount)}
            />

            <SummaryCard
              icon={Clock3}
              label="Duration"
              value={formatDuration(durationMs)}
            />

            <SummaryCard
              icon={Clock3}
              label="Started"
              value={
                startedAt
                  ? formatRelativeTime(startedAt)
                  : "—"
              }
            />
          </section>

          {/* ============================================================
              Retry / reliability signal
          ============================================================= */}

          {retryCount > 0 && (
            <div className="flex items-center gap-3 rounded-xl border border-amber-500/10 bg-amber-500/[0.025] px-4 py-3">
              <Clock3 className="h-4 w-4 shrink-0 text-amber-300/70" />

              <div className="min-w-0">
                <p className="text-xs font-medium text-amber-200/80">
                  Execution required a retry
                </p>

                <p className="mt-0.5 text-[11px] text-white/30">
                  {retryCount} retry
                  {retryCount === 1 ? "" : "ies"} were
                  recorded during this execution.
                </p>
              </div>
            </div>
          )}

          {/* ============================================================
              Timeline
          ============================================================= */}

          <section className="overflow-hidden rounded-xl border border-white/[0.08] bg-white/[0.02]">
            <div className="flex items-center justify-between border-b border-white/[0.06] px-5 py-4">
              <div>
                <div className="flex items-center gap-2">
                  <Activity className="h-3.5 w-3.5 text-white/30" />

                  <p className="text-sm font-medium text-white/80">
                    Execution timeline
                  </p>
                </div>

                <p className="mt-0.5 text-xs text-white/35">
                  Node-level events captured from the
                  external workflow.
                </p>
              </div>

              <span className="font-mono text-[10px] text-white/25">
                {events.length} event
                {events.length === 1 ? "" : "s"}
              </span>
            </div>

            <div className="divide-y divide-white/[0.06]">
              {events.map((event, index) => (
                <ExecutionEventRow
                  key={event.id}
                  event={event}
                  index={index}
                  total={events.length}
                />
              ))}
            </div>
          </section>

          {/* ============================================================
              Details
          ============================================================= */}

          <section className="rounded-xl border border-white/[0.08] bg-white/[0.02]">
            <div className="border-b border-white/[0.06] px-5 py-4">
              <p className="text-sm font-medium text-white/80">
                Execution details
              </p>

              <p className="mt-0.5 text-xs text-white/35">
                Identifiers and source information.
              </p>
            </div>

            <div className="grid sm:grid-cols-2">
              <DetailItem
                label="Execution ID"
                value={executionExternalId}
                copyable
              />

              <DetailItem
                label="Workflow ID"
                value={
                  events[0]?.workflowExternalId ??
                  workflowExternalId ??
                  "—"
                }
                copyable
              />

              <DetailItem
                label="Source"
                value={
                  events[0]?.source ?? "—"
                }
              />

              <DetailItem
                label="Events captured"
                value={String(events.length)}
              />

              <DetailItem
                label="Started"
                value={
                  startedAt
                    ? formatAbsoluteDate(startedAt)
                    : "—"
                }
              />

              <DetailItem
                label="Finished"
                value={
                  finishedAt
                    ? formatAbsoluteDate(finishedAt)
                    : "—"
                }
              />
            </div>
          </section>
        </>
      )}
    </div>
  );
}

export default function ExternalExecutionPage() {
  return (
    <Suspense fallback={null}>
      <ExternalExecutionPageContent />
    </Suspense>
  );
}

/* ==========================================================================
 * Execution event row
 * ========================================================================== */

function ExecutionEventRow({
  event,
  index,
  total,
}: {
  event: ExecutionEvent;
  index: number;
  total: number;
}) {
  const status = event.status.toUpperCase();

  const retrying =
    event.retryCount !== null &&
    event.retryCount > 0;

  return (
    <div className="flex gap-4 px-5 py-4 transition-colors hover:bg-white/[0.015]">
      {/* Timeline */}

      <div className="flex w-5 shrink-0 flex-col items-center">
        <div
          className={cn(
            "flex h-7 w-7 items-center justify-center rounded-full border",
            timelineIconClasses(status),
          )}
        >
          <StatusIcon status={status} />
        </div>

        {index < total - 1 && (
          <div className="mt-2 h-full min-h-8 w-px bg-white/[0.08]" />
        )}
      </div>

      {/* Event */}

      <div className="min-w-0 flex-1 pb-2">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p
                className={cn(
                  "truncate text-sm font-medium",
                  status === "FAILED"
                    ? "text-red-200/85"
                    : "text-white/85",
                )}
              >
                {event.stepName ||
                  event.stepExternalId ||
                  "Workflow step"}
              </p>

              {retrying && (
                <span className="rounded-md border border-amber-500/15 bg-amber-500/[0.05] px-1.5 py-0.5 font-mono text-[9px] uppercase tracking-wider text-amber-300/70">
                  retry ×{event.retryCount}
                </span>
              )}
            </div>

            <p className="mt-0.5 truncate font-mono text-[10px] text-white/30">
              {event.stepExternalId}
            </p>
          </div>

          <ExternalStatusBadge
            status={status}
          />
        </div>

        {/* Metadata */}

        <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 font-mono text-[10px] text-white/35">
          {event.startedAt && (
            <span>
              started{" "}
              {formatRelativeTime(
                event.startedAt,
              )}
            </span>
          )}

          {event.durationMs !== null && (
            <span>
              {formatDuration(
                event.durationMs,
              )}
            </span>
          )}

          {event.finishedAt && (
            <span>
              finished{" "}
              {formatRelativeTime(
                event.finishedAt,
              )}
            </span>
          )}
        </div>

        {/* Error */}

        {event.errorMessage && (
          <div className="mt-3 rounded-lg border border-red-500/10 bg-red-500/[0.04] px-3 py-2.5">
            <p className="text-xs font-medium text-red-300/80">
              {event.errorType ||
                "Execution error"}
            </p>

            <p className="mt-1 text-xs leading-5 text-red-200/55">
              {event.errorMessage}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

/* ==========================================================================
 * Summary card
 * ========================================================================== */

function SummaryCard({
  icon: Icon,
  label,
  value,
  accent,
}: {
  icon: ComponentType<{
    className?: string;
  }>;
  label: string;
  value: string;
  accent?: "success" | "danger" | "neutral";
}) {
  return (
    <div className="group rounded-xl border border-white/[0.08] bg-white/[0.02] px-4 py-3.5 transition-colors hover:border-white/[0.11] hover:bg-white/[0.025]">
      <div className="flex items-center justify-between">
        <p className="font-mono text-[10px] uppercase tracking-wider text-white/30">
          {label}
        </p>

        <Icon
          className={cn(
            "h-3.5 w-3.5",
            accent === "success"
              ? "text-emerald-400/60"
              : accent === "danger"
                ? "text-red-400/60"
                : "text-white/20",
          )}
        />
      </div>

      <p
        className={cn(
          "mt-1.5 truncate text-sm font-medium",
          accent === "success"
            ? "text-emerald-300/85"
            : accent === "danger"
              ? "text-red-300/85"
              : "text-white/80",
        )}
      >
        {value}
      </p>
    </div>
  );
}

/* ==========================================================================
 * Detail item
 * ========================================================================== */

function DetailItem({
  label,
  value,
  copyable = false,
}: {
  label: string;
  value: string;
  copyable?: boolean;
}) {
  async function copyValue() {
    if (!value || value === "—") {
      return;
    }

    try {
      await navigator.clipboard.writeText(value);
    } catch {
      // Clipboard access may be unavailable.
    }
  }

  return (
    <div className="group flex min-w-0 items-center justify-between gap-4 border-b border-white/[0.06] px-5 py-4 last:border-b-0 sm:nth-[2n]:border-l sm:nth-[2n]:border-white/[0.06]">
      <div className="min-w-0">
        <p className="font-mono text-[10px] uppercase tracking-wider text-white/25">
          {label}
        </p>

        <p className="mt-1 break-all font-mono text-xs text-white/55">
          {value}
        </p>
      </div>

      {copyable && value !== "—" && (
        <button
          type="button"
          onClick={copyValue}
          aria-label={`Copy ${label}`}
          className="shrink-0 rounded-md p-1.5 text-white/20 opacity-0 transition hover:bg-white/[0.05] hover:text-white/60 group-hover:opacity-100"
        >
          <Copy className="h-3.5 w-3.5" />
        </button>
      )}
    </div>
  );
}

/* ==========================================================================
 * Status
 * ========================================================================== */

function deriveExecutionStatus(
  events: ExecutionEvent[],
): string {
  const statuses = events.map((event) =>
    event.status.toUpperCase(),
  );

  if (statuses.includes("FAILED")) {
    return "FAILED";
  }

  if (statuses.includes("CANCELED")) {
    return "CANCELED";
  }

  if (statuses.includes("RUNNING")) {
    return "RUNNING";
  }

  if (statuses.includes("WAITING")) {
    return "WAITING";
  }

  if (statuses.includes("QUEUED")) {
    return "QUEUED";
  }

  if (statuses.includes("SUCCEEDED")) {
    return "SUCCEEDED";
  }

  if (statuses.includes("SKIPPED")) {
    return "SKIPPED";
  }

  return statuses[0] ?? "UNKNOWN";
}

function StatusIcon({
  status,
}: {
  status: string;
}) {
  switch (status) {
    case "SUCCEEDED":
      return (
        <CheckCircle2 className="h-3.5 w-3.5" />
      );

    case "FAILED":
      return (
        <XCircle className="h-3.5 w-3.5" />
      );

    case "RUNNING":
    case "WAITING":
    case "QUEUED":
      return (
        <Clock3 className="h-3.5 w-3.5" />
      );

    default:
      return (
        <Circle className="h-3.5 w-3.5" />
      );
  }
}

function timelineIconClasses(
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

    case "QUEUED":
      return "border-purple-500/20 bg-purple-500/10 text-purple-300";

    default:
      return "border-white/[0.08] bg-white/[0.03] text-white/40";
  }
}

function ExternalStatusBadge({
  status,
}: {
  status: string;
}) {
  const normalized = status.toUpperCase();

  return (
    <Badge
      variant="outline"
      className={cn(
        "shrink-0 text-[10px] uppercase tracking-wide",
        statusBadgeClasses(normalized),
      )}
    >
      <span
        className={cn(
          "mr-1.5 h-1.5 w-1.5 rounded-full",
          statusDotClasses(normalized),
        )}
      />

      {normalized.toLowerCase()}
    </Badge>
  );
}

function statusBadgeClasses(
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

    default:
      return "border-white/10 bg-white/[0.04] text-white/45";
  }
}

function statusDotClasses(
  status: string,
): string {
  switch (status) {
    case "SUCCEEDED":
      return "bg-emerald-400";

    case "FAILED":
      return "bg-red-400";

    case "RUNNING":
      return "bg-amber-400";

    case "WAITING":
      return "bg-blue-400";

    case "QUEUED":
      return "bg-purple-400";

    default:
      return "bg-white/30";
  }
}

function statusAccent(
  status: string,
): "success" | "danger" | "neutral" {
  switch (status.toUpperCase()) {
    case "SUCCEEDED":
      return "success";

    case "FAILED":
      return "danger";

    default:
      return "neutral";
  }
}

/* ==========================================================================
 * Workflow name
 * ========================================================================== */

function getWorkflowName(
  event: ExecutionEvent | undefined,
): string {
  if (!event) {
    return "External workflow";
  }

  const metadata = event.metadata;

  if (metadata) {
    const workflowName =
      metadata.workflowName;

    if (
      typeof workflowName === "string" &&
      workflowName.trim()
    ) {
      return workflowName;
    }

    const workflow =
      metadata.workflow;

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

/* ==========================================================================
 * Dates
 * ========================================================================== */

function earliestTimestamp(
  values: Array<string | null | undefined>,
): string | null {
  const valid = values.filter(
    (value): value is string =>
      Boolean(value),
  );

  if (valid.length === 0) {
    return null;
  }

  return valid.reduce(
    (earliest, value) =>
      Date.parse(value) <
      Date.parse(earliest)
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

  if (valid.length === 0) {
    return null;
  }

  return valid.reduce(
    (latest, value) =>
      Date.parse(value) >
      Date.parse(latest)
        ? value
        : latest,
  );
}

function formatAbsoluteDate(
  value: string,
): string {
  try {
    return new Intl.DateTimeFormat(
      undefined,
      {
        dateStyle: "medium",
        timeStyle: "short",
      },
    ).format(new Date(value));
  } catch {
    return value;
  }
}

/* ==========================================================================
 * Loading
 * ========================================================================== */

function ExternalExecutionSkeleton() {
  return (
    <div className="space-y-5">
      {/* Summary */}

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map(
          (_, index) => (
            <div
              key={index}
              className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-4"
            >
              <Skeleton className="h-2.5 w-16" />
              <Skeleton className="mt-2 h-4 w-24" />
            </div>
          ),
        )}
      </div>

      {/* Timeline */}

      <div className="overflow-hidden rounded-xl border border-white/[0.08] bg-white/[0.02]">
        <div className="border-b border-white/[0.06] px-5 py-4">
          <Skeleton className="h-3 w-36" />
          <Skeleton className="mt-2 h-2.5 w-60" />
        </div>

        {Array.from({ length: 4 }).map(
          (_, index) => (
            <div
              key={index}
              className="flex gap-4 border-b border-white/[0.06] px-5 py-5 last:border-0"
            >
              <Skeleton className="h-7 w-7 shrink-0 rounded-full" />

              <div className="flex flex-1 flex-col gap-2">
                <Skeleton className="h-3 w-40" />
                <Skeleton className="h-2.5 w-56" />
                <Skeleton className="h-2.5 w-32" />
              </div>
            </div>
          ),
        )}
      </div>

      {/* Details */}

      <div className="rounded-xl border border-white/[0.08] bg-white/[0.02]">
        <div className="border-b border-white/[0.06] px-5 py-4">
          <Skeleton className="h-3 w-32" />
          <Skeleton className="mt-2 h-2.5 w-48" />
        </div>

        <div className="grid sm:grid-cols-2">
          {Array.from({ length: 4 }).map(
            (_, index) => (
              <div
                key={index}
                className="border-b border-white/[0.06] px-5 py-4"
              >
                <Skeleton className="h-2.5 w-20" />
                <Skeleton className="mt-2 h-3 w-36" />
              </div>
            ),
          )}
        </div>
      </div>
    </div>
  );
}