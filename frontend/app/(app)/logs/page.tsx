"use client";

import Link from "next/link";
import { useState } from "react";
import {
  keepPreviousData,
  useQuery,
} from "@tanstack/react-query";
import {
  FileText,
  ShieldCheck,
} from "lucide-react";

import {
  fetchAuditLogs,
  fetchLogs,
} from "@/lib/api";
import {
  hasRole,
  useAuthStore,
} from "@/lib/auth-store";
import {
  formatRelativeTime,
} from "@/lib/format";
import type {
  AuditLogEntry,
  LogEntry,
  LogLevel,
} from "@/types";
import { cn } from "@/lib/utils";

import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { SegmentedToggle } from "@/components/patterns/segmented-toggle";

/**
 * The observability surface: run logs for everyone,
 * the audit trail for admins.
 *
 * Run logs are the SAME execution_logs the engine writes.
 * This is a viewer, not a second logging system.
 *
 * The audit trail is append-only and admin-gated on the
 * server; the tab is hidden from non-admins as a courtesy,
 * never as the control.
 */

const LEVEL_FILTERS: {
  label: string;
  value: LogLevel | "ALL";
}[] = [
  {
    label: "All",
    value: "ALL",
  },
  {
    label: "Info",
    value: "INFO",
  },
  {
    label: "Warn",
    value: "WARN",
  },
  {
    label: "Error",
    value: "ERROR",
  },
];

export default function LogsPage() {
  const role = useAuthStore(
    (s) => s.currentRole
  );

  const isAdmin = hasRole(
    role,
    "ADMIN"
  );

  const [tab, setTab] = useState<
    "runs" | "audit"
  >("runs");

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      {/* Page header */}
      <div>
        <p className="mono-eyebrow">
          Observability
        </p>

        <h1 className="mt-1 text-xl font-semibold tracking-tight text-white/90">
          Logs
        </h1>

        <p className="mt-1 text-sm text-white/44">
          Everything this workspace has recorded —
          run output and, for admins, the audit trail.
        </p>
      </div>

      {/* Log source selector */}
      {isAdmin && (
        <SegmentedToggle
          label="Log source"
          value={tab}
          onValueChange={(v) =>
            setTab(
              v as "runs" | "audit"
            )
          }
          options={[
            {
              value: "runs",
              label: "Run logs",
              icon: (
                <FileText className="size-4" />
              ),
            },
            {
              value: "audit",
              label: "Audit trail",
              icon: (
                <ShieldCheck className="size-4" />
              ),
            },
          ]}
        />
      )}

      {tab === "audit" && isAdmin ? (
        <AuditTrail />
      ) : (
        <RunLogs />
      )}
    </div>
  );
}

/* =========================================================
   RUN LOGS
   ========================================================= */

function RunLogs() {
  const [level, setLevel] =
    useState<LogLevel | "ALL">(
      "ALL"
    );

  const query = useQuery({
    queryKey: [
      "logs",
      { level },
    ],
    queryFn: () =>
      fetchLogs({
        level:
          level === "ALL"
            ? undefined
            : level,
      }),
    placeholderData:
      keepPreviousData,
    refetchInterval: 15_000,
  });

  const logs =
    query.data?.logs ?? [];

  return (
    <div className="space-y-4">
      {/* Level filters */}
      <div className="flex flex-wrap gap-1">
        {LEVEL_FILTERS.map(
          (f) => (
            <button
              key={f.value}
              type="button"
              onClick={() =>
                setLevel(f.value)
              }
              className={cn(
                "rounded-full border px-3 py-1 text-sm transition-colors",
                level === f.value
                  ? "border-white bg-white font-medium text-[#050505]"
                  : "border-white/[0.08] bg-white/[0.03] text-white/60 hover:bg-white/[0.06] hover:text-white/90"
              )}
            >
              {f.label}
            </button>
          )
        )}
      </div>

      {/* Loading */}
      {query.isPending ? (
        <div className="space-y-2">
          <Skeleton className="h-12 w-full" />
          <Skeleton className="h-12 w-full" />
          <Skeleton className="h-12 w-full" />
        </div>
      ) : query.isError ? (
        <p className="text-sm text-destructive">
          Could not load logs. Please try again.
        </p>
      ) : logs.length === 0 ? (
        <EmptyState
          icon={
            <FileText className="size-10 text-white/20" />
          }
          title={
            level !== "ALL"
              ? "No log lines at this level."
              : "No run logs yet."
          }
          hint="Run a published workflow and its log lines appear here."
          action={{
            href: "/workflows",
            label: "Go to workflows",
          }}
        />
      ) : (
        <ul className="divide-y divide-white/[0.06] overflow-hidden rounded-xl border border-white/[0.08]">
          {logs.map(
            (entry) => (
              <LogRow
                key={entry.id}
                entry={entry}
              />
            )
          )}
        </ul>
      )}
    </div>
  );
}

/* =========================================================
   LOG LEVEL STYLES
   ========================================================= */

const LEVEL_TONE: Record<
  LogLevel,
  string
> = {
  DEBUG: "text-white/44",
  INFO: "text-white/90",
  WARN: "text-warning",
  ERROR: "text-destructive",
};

/* =========================================================
   LOG ROW
   ========================================================= */

function LogRow({
  entry,
}: {
  entry: LogEntry;
}) {
  return (
    <li>
      <Link
        href={`/executions/${entry.executionId}`}
        className="flex items-start gap-3 px-4 py-2.5 transition-colors hover:bg-white/[0.03]"
      >
        <span
          className={cn(
            "w-14 shrink-0 pt-0.5 font-mono text-[11px] uppercase",
            LEVEL_TONE[
              entry.level
            ]
          )}
        >
          {entry.level}
        </span>

        <span className="min-w-0 flex-1">
          <span className="block break-words font-mono text-xs text-white/70">
            {entry.message}
          </span>

          <span className="mt-1 block truncate text-[11px] text-white/44">
            {entry.workflowName}
            {entry.nodeId
              ? ` · node ${entry.nodeId}`
              : ""}
          </span>
        </span>

        <span className="hidden shrink-0 text-right text-[11px] text-white/44 sm:block">
          {formatRelativeTime(
            entry.createdAt
          )}
        </span>
      </Link>
    </li>
  );
}

/* =========================================================
   AUDIT TRAIL
   ========================================================= */

function AuditTrail() {
  const query = useQuery({
    queryKey: [
      "audit-logs",
    ],
    queryFn: () =>
      fetchAuditLogs(),
    placeholderData:
      keepPreviousData,
  });

  const entries =
    query.data?.auditLogs ?? [];

  if (query.isPending) {
    return (
      <div className="space-y-2">
        <Skeleton className="h-14 w-full" />
        <Skeleton className="h-14 w-full" />
        <Skeleton className="h-14 w-full" />
      </div>
    );
  }

  if (query.isError) {
    return (
      <p className="text-sm text-destructive">
        Could not load the audit trail.
        Please try again.
      </p>
    );
  }

  if (entries.length === 0) {
    return (
      <EmptyState
        icon={
          <ShieldCheck className="size-10 text-white/20" />
        }
        title="No audited actions yet."
        hint="Invites, role changes, integrations and webhook activity are recorded here."
      />
    );
  }

  return (
    <ul className="divide-y divide-white/[0.06] overflow-hidden rounded-xl border border-white/[0.08]">
      {entries.map(
        (entry) => (
          <AuditRow
            key={entry.id}
            entry={entry}
          />
        )
      )}
    </ul>
  );
}

/* =========================================================
   AUDIT ROW
   ========================================================= */

function AuditRow({
  entry,
}: {
  entry: AuditLogEntry;
}) {
  // A null actor is not an error:
  // an inbound webhook that fails authentication
  // is anonymous by definition.
  const actor =
    entry.actorEmail ??
    "anonymous";

  const failed =
    entry.action ===
    "WEBHOOK_AUTH_FAILED";

  return (
    <li className="flex items-start gap-3 px-4 py-3">
      <Badge
        variant={
          failed
            ? "destructive"
            : "secondary"
        }
        className="shrink-0 font-mono text-[10px]"
      >
        {entry.action
          .toLowerCase()
          .replace(
            /_/g,
            " "
          )}
      </Badge>

      <span className="min-w-0 flex-1">
        <span className="block text-sm text-white/90">
          {entry.summary}
        </span>

        <span className="mt-0.5 block truncate text-[11px] text-white/44">
          {actor}
          {entry.ip
            ? ` · ${entry.ip}`
            : ""}
          {entry.targetType
            ? ` · ${entry.targetType}`
            : ""}
        </span>
      </span>

      <span className="hidden shrink-0 text-right text-[11px] text-white/44 sm:block">
        {formatRelativeTime(
          entry.createdAt
        )}
      </span>
    </li>
  );
}

/* =========================================================
   EMPTY STATE
   ========================================================= */

function EmptyState({
  icon,
  title,
  hint,
  action,
}: {
  icon: React.ReactNode;
  title: string;
  hint: string;
  action?: {
    href: string;
    label: string;
  };
}) {
  return (
    <div className="rounded-xl border border-dashed border-white/[0.10] py-16">
      <div className="flex w-full flex-col items-center justify-center text-center">
        {/* Icon */}
        <div className="flex size-12 items-center justify-center">
          {icon}
        </div>

        {/* Text */}
        <div className="mt-3">
          <p className="font-medium text-white/90">
            {title}
          </p>

          <p className="mt-1 text-sm text-white/44">
            {hint}
          </p>
        </div>

        {/* Action */}
        {action && (
          <Link
            href={action.href}
            className="mt-3 inline-block text-sm font-medium text-primary hover:underline"
          >
            {action.label}
          </Link>
        )}
      </div>
    </div>
  );
}