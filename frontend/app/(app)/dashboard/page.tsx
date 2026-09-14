"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  Activity,
  AlertCircle,
  ArrowRight,
  CheckCircle2,
  Clock,
  ExternalLink,
  Gauge,
  Radio,
  Search,
  TrendingUp,
  X,
  Zap,
} from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import { cn } from "@/lib/utils";
import {
  fetchCurrentOrganization,
  fetchExecutionStats,
  fetchExecutions,
} from "@/lib/api";
import { formatDuration, formatRelativeTime } from "@/lib/format";
import type { ExecutionStatus, StatsRange } from "@/types";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Reveal, HoverLift } from "@/components/motion/motion-primitives";

const RANGE_OPTIONS: StatsRange[] = ["24h", "7d", "30d", "90d"];

type ExecutionItem = {
  id: string;
  workflowName: string | null;
  workflowId: string;
  status: ExecutionStatus;
  createdAt: string;
  durationMs: number | null;
  triggerType: string;
  versionNumber: number;
};

export default function DashboardPage() {
  const [range, setRange] = useState<StatsRange>("7d");

  const executions = useQuery({
    queryKey: ["executions", "dashboard"],
    queryFn: () => fetchExecutions({ limit: 12 }),
    refetchInterval: (query) =>
      (query.state.data?.executions ?? []).some((execution) =>
        isLiveStatus(execution.status),
      )
        ? 5000
        : false,
  });

  const stats = useQuery({
    queryKey: ["executions", "stats", range],
    queryFn: () => fetchExecutionStats(range),
  });

  const org = useQuery({
    queryKey: ["org", "current"],
    queryFn: () => fetchCurrentOrganization(),
  });

  const loading = executions.isPending || stats.isPending;

  const recentExecutions = useMemo<ExecutionItem[]>(
    () => executions.data?.executions ?? [],
    [executions.data?.executions],
  );

  const totals = stats.data?.totals;

  /*
   * Keep the dashboard resilient if the stats contract is extended later.
   * failed/running/queued are optional and fall back to the executions
   * currently returned for the dashboard.
   */
  const dashboardTotals = useMemo(() => {
    const typedTotals = totals as
      | (typeof totals & {
          failed?: number;
          running?: number;
          queued?: number;
        })
      | undefined;

    const recentFailed = recentExecutions.filter(
      (execution) => execution.status === "FAILED",
    ).length;

    const recentActive = recentExecutions.filter((execution) =>
      isLiveStatus(execution.status),
    ).length;

    return {
      total: totals?.total ?? 0,
      successRate: totals?.successRate ?? 0,
      avgDurationMs: totals?.avgDurationMs ?? null,
      failed:
        typeof typedTotals?.failed === "number"
          ? typedTotals.failed
          : recentFailed,
      active:
        typeof typedTotals?.running === "number"
          ? typedTotals.running
          : recentActive,
    };
  }, [recentExecutions, totals]);

  const failedExecutions = recentExecutions.filter(
    (execution) => execution.status === "FAILED",
  );

  const activeExecutions = recentExecutions.filter((execution) =>
    isLiveStatus(execution.status),
  );

  const healthLabel = getHealthLabel(
    dashboardTotals.successRate,
    dashboardTotals.failed,
  );

  return (
    <div className="space-y-7 pb-10">
      {/* ------------------------------------------------------------------ */}
      {/* Header                                                              */}
      {/* ------------------------------------------------------------------ */}
      <Reveal>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="mono-eyebrow">Workspace</p>

            <h1 className="mt-1 text-xl font-semibold tracking-tight text-white/90">
              Dashboard
            </h1>

            <p className="mt-1 text-sm text-white/44">
              {org.data?.organization.name ?? "Your workspace"} — activity
              overview
            </p>
          </div>

          <div className="flex w-fit rounded-full border border-white/[0.08] bg-white/[0.03] p-0.5">
            {RANGE_OPTIONS.map((r) => (
              <button
                key={r}
                type="button"
                onClick={() => setRange(r)}
                className={cn(
                  "rounded-full px-3 py-1.5 text-xs font-medium transition-all",
                  range === r
                    ? "bg-white text-[#050505]"
                    : "text-white/45 hover:text-white/80",
                )}
              >
                {r}
              </button>
            ))}
          </div>
        </div>
      </Reveal>

      {/* ------------------------------------------------------------------ */}
      {/* Health snapshot                                                     */}
      {/* ------------------------------------------------------------------ */}
      <Reveal distance={16}>
        <div className="grid overflow-hidden rounded-xl border border-white/[0.08] bg-white/[0.02] sm:grid-cols-2 lg:grid-cols-4">
          <DashboardMetric
            label="Total runs"
            value={
              loading
                ? "—"
                : dashboardTotals.total.toLocaleString()
            }
            icon={Activity}
          />

          <DashboardMetric
            label="Success rate"
            value={
              loading
                ? "—"
                : `${Math.round(dashboardTotals.successRate * 100)}%`
            }
            icon={CheckCircle2}
            sublabel={!loading ? healthLabel : undefined}
            tone={getHealthTone(dashboardTotals.successRate)}
          />

          <DashboardMetric
            label="Failed"
            value={loading ? "—" : dashboardTotals.failed.toLocaleString()}
            icon={AlertCircle}
            tone={
              dashboardTotals.failed > 0 ? "danger" : "neutral"
            }
          />

          <DashboardMetric
            label="Active"
            value={loading ? "—" : dashboardTotals.active.toLocaleString()}
            icon={Radio}
            tone={
              dashboardTotals.active > 0 ? "active" : "neutral"
            }
          />
        </div>
      </Reveal>

      {/* ------------------------------------------------------------------ */}
      {/* Activity + health                                                   */}
      {/* ------------------------------------------------------------------ */}
      <div className="grid gap-5 lg:grid-cols-[minmax(0,1.75fr)_minmax(280px,0.75fr)]">
        <Reveal distance={18}>
          <Card className="overflow-hidden">
            <CardHeader className="flex-row items-center justify-between border-b border-white/[0.05] pb-4">
              <div>
                <CardTitle className="text-sm font-medium text-white/70">
                  Execution activity
                </CardTitle>
                <p className="mt-1 text-xs text-white/30">
                  {range === "24h"
                    ? "Last 24 hours"
                    : range === "7d"
                      ? "Last 7 days"
                      : range === "30d"
                        ? "Last 30 days"
                        : "Last 90 days"}
                </p>
              </div>

              <div className="flex items-center gap-1.5 text-[10px] font-mono uppercase tracking-wider text-white/25">
                <TrendingUp className="size-3.5" />
                Runs
              </div>
            </CardHeader>

            <CardContent className="pt-5">
              {stats.isPending ? (
                <ChartSkeleton />
              ) : (
                <ActivityChart data={stats.data?.series ?? []} />
              )}
            </CardContent>
          </Card>
        </Reveal>

        <Reveal delay={0.06} distance={18}>
          <Card className="h-full">
            <CardHeader className="pb-3">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <CardTitle className="text-sm font-medium text-white/70">
                    Workspace health
                  </CardTitle>
                  <p className="mt-1 text-xs text-white/30">
                    Current execution signal
                  </p>
                </div>

                <Gauge className="size-4 text-white/20" />
              </div>
            </CardHeader>

            <CardContent>
              <div className="flex items-end justify-between">
                <div>
                  <p className="text-3xl font-semibold tracking-tight text-white/90">
                    {loading
                      ? "—"
                      : `${Math.round(
                          dashboardTotals.successRate * 100,
                        )}%`}
                  </p>

                  <p
                    className={cn(
                      "mt-1 text-xs",
                      getHealthTextClass(
                        dashboardTotals.successRate,
                      ),
                    )}
                  >
                    {loading ? "Loading..." : healthLabel}
                  </p>
                </div>

                <span
                  className={cn(
                    "mb-1 h-2 w-2 rounded-full",
                    getHealthDot(
                      dashboardTotals.successRate,
                    ),
                  )}
                />
              </div>

              <div className="mt-5 h-1.5 overflow-hidden rounded-full bg-white/[0.06]">
                <div
                  className="h-full rounded-full bg-white/70 transition-all duration-500"
                  style={{
                    width: `${
                      Math.min(
                        100,
                        Math.max(
                          0,
                          dashboardTotals.successRate * 100,
                        ),
                      )
                    }%`,
                  }}
                />
              </div>

              <div className="mt-4 grid grid-cols-2 gap-3">
                <MiniMetric
                  label="Failed"
                  value={dashboardTotals.failed}
                  tone={
                    dashboardTotals.failed > 0
                      ? "danger"
                      : "neutral"
                  }
                />
                <MiniMetric
                  label="Active"
                  value={dashboardTotals.active}
                  tone={
                    dashboardTotals.active > 0
                      ? "active"
                      : "neutral"
                  }
                />
              </div>
            </CardContent>
          </Card>
        </Reveal>
      </div>

      {/* ------------------------------------------------------------------ */}
      {/* Recent + attention                                                  */}
      {/* ------------------------------------------------------------------ */}
      <div className="grid gap-5 lg:grid-cols-[minmax(0,1.2fr)_minmax(0,0.8fr)]">
        <Reveal distance={18}>
          <Card>
            <CardHeader className="flex-row items-center justify-between pb-3">
              <div>
                <CardTitle className="text-sm font-medium text-white/70">
                  Recent executions
                </CardTitle>
                <p className="mt-1 text-xs text-white/30">
                  Latest workflow activity
                </p>
              </div>

              <Link
                href="/executions"
                className="flex items-center gap-1 text-xs text-white/35 transition-colors hover:text-white/75"
              >
                View all
                <ArrowRight className="size-3" />
              </Link>
            </CardHeader>

            <CardContent className="pt-0">
              {executions.isPending ? (
                <RecentExecutionsSkeleton />
              ) : recentExecutions.length === 0 ? (
                <EmptyPanel
                  icon={Zap}
                  title="No executions yet"
                  description="Run a workflow to start seeing activity here."
                  href="/workflows"
                  action="Open workflows"
                />
              ) : (
                <div>
                  {recentExecutions.slice(0, 7).map((execution, index) => (
                    <Link
                      key={execution.id}
                      href={`/executions/${execution.id}`}
                      className={cn(
                        "group flex items-center gap-3 px-1 py-3 transition-colors hover:bg-white/[0.025]",
                        index < Math.min(recentExecutions.length, 7) - 1 &&
                          "border-b border-white/[0.05]",
                      )}
                    >
                      <span
                        className={cn(
                          "size-2 shrink-0 rounded-full",
                          statusDotColor(execution.status),
                        )}
                      />

                      <span className="min-w-0 flex-1">
                        <span className="flex items-center gap-2">
                          <span className="truncate text-xs font-medium text-white/75 group-hover:text-white/90">
                            {execution.workflowName ??
                              execution.workflowId.slice(0, 8)}
                          </span>

                          <span className="hidden rounded border border-white/[0.06] bg-white/[0.02] px-1.5 py-0.5 font-mono text-[8px] uppercase tracking-wider text-white/25 sm:inline">
                            internal
                          </span>
                        </span>

                        <span className="mt-0.5 block truncate font-mono text-[10px] text-white/25">
                          v{execution.versionNumber} ·{" "}
                          {execution.triggerType.toLowerCase()}
                        </span>
                      </span>

                      <span className="hidden shrink-0 text-right sm:block">
                        <span className="block text-[10px] text-white/35">
                          {formatRelativeTime(execution.createdAt)}
                        </span>
                        <span className="mt-0.5 block font-mono text-[9px] text-white/20">
                          {formatDuration(execution.durationMs)}
                        </span>
                      </span>

                      <ArrowRight className="size-3.5 shrink-0 text-white/10 transition group-hover:text-white/35" />
                    </Link>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </Reveal>

        <Reveal delay={0.06} distance={18}>
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-medium text-white/70">
                Attention required
              </CardTitle>
              <p className="mt-1 text-xs text-white/30">
                Things worth looking at now
              </p>
            </CardHeader>

            <CardContent className="space-y-2 pt-0">
              <AttentionItem
                tone="danger"
                icon={AlertCircle}
                title={
                  dashboardTotals.failed > 0
                    ? `${dashboardTotals.failed} failed execution${
                        dashboardTotals.failed === 1 ? "" : "s"
                      }`
                    : "No failed executions"
                }
                description={
                  failedExecutions.length > 0
                    ? "Recent failures are available for investigation."
                    : "Your recent execution feed is currently clean."
                }
                href={
                  dashboardTotals.failed > 0
                    ? "/executions?status=FAILED"
                    : "/executions"
                }
              />

              <AttentionItem
                tone="active"
                icon={Radio}
                title={
                  dashboardTotals.active > 0
                    ? `${dashboardTotals.active} active execution${
                        dashboardTotals.active === 1 ? "" : "s"
                      }`
                    : "No active executions"
                }
                description={
                  activeExecutions.length > 0
                    ? "A workflow is currently running or waiting."
                    : "Nothing is currently running."
                }
                href="/executions?status=RUNNING"
              />

              <AttentionItem
                tone="neutral"
                icon={Clock}
                title={
                  dashboardTotals.avgDurationMs !== null
                    ? `Avg duration ${formatDuration(
                        dashboardTotals.avgDurationMs,
                      )}`
                    : "Average duration unavailable"
                }
                description="Monitor this over time for execution drift."
                href="/executions"
              />
            </CardContent>
          </Card>
        </Reveal>
      </div>

      {/* ------------------------------------------------------------------ */}
      {/* Operational footer                                                 */}
      {/* ------------------------------------------------------------------ */}
      <Reveal distance={14}>
        <div className="grid gap-5 lg:grid-cols-2">
          <OperationalCard
            icon={Activity}
            title="Internal execution"
            description="Workflows executed by FlowOps."
            value="Native"
            href="/executions"
          />

          <OperationalCard
            icon={ExternalLink}
            title="External monitoring"
            description="Provider executions monitored by FlowOps."
            value="Connected"
            href="/executions"
          />
        </div>
      </Reveal>
    </div>
  );
}

function DashboardMetric({
  label,
  value,
  icon: Icon,
  sublabel,
  tone = "neutral",
}: {
  label: string;
  value: string;
  icon: typeof Activity;
  sublabel?: string;
  tone?: "neutral" | "danger" | "active" | "healthy";
}) {
  return (
    <HoverLift scale={1.01} y={-1}>
      <div className="border-b border-white/[0.06] px-5 py-4 first:lg:border-l-0 lg:border-b-0 lg:border-r lg:last:border-r-0">
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-white/38">
            {label}
          </span>
          <Icon
            className={cn(
              "size-4",
              tone === "danger"
                ? "text-red-400/65"
                : tone === "active"
                  ? "text-amber-300/70"
                  : tone === "healthy"
                    ? "text-emerald-400/65"
                    : "text-white/20",
            )}
          />
        </div>

        <div className="mt-1.5 text-2xl font-semibold tracking-tight text-white/90">
          {value}
        </div>

        {sublabel && (
          <p
            className={cn(
              "mt-1 text-[10px]",
              tone === "healthy"
                ? "text-emerald-400/60"
                : "text-white/25",
            )}
          >
            {sublabel}
          </p>
        )}
      </div>
    </HoverLift>
  );
}

function MiniMetric({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone: "neutral" | "danger" | "active";
}) {
  return (
    <div className="rounded-lg border border-white/[0.06] bg-white/[0.018] px-3 py-2.5">
      <p className="text-[9px] font-mono uppercase tracking-wider text-white/25">
        {label}
      </p>
      <p
        className={cn(
          "mt-1 text-sm font-semibold tabular-nums",
          tone === "danger"
            ? "text-red-300/85"
            : tone === "active"
              ? "text-amber-200/85"
              : "text-white/60",
        )}
      >
        {value}
      </p>
    </div>
  );
}

function ActivityChart({
  data,
}: {
  data: { bucketStart: string; total: number }[];
}) {
  if (data.length === 0) {
    return (
      <div className="flex h-44 items-center justify-center rounded-lg border border-dashed border-white/[0.06] bg-white/[0.012] text-sm text-white/25">
        No execution data for this period
      </div>
    );
  }

  const values = data.map((item) => item.total);
  const max = Math.max(...values, 1);

  return (
    <div className="space-y-3">
      <div className="flex h-44 items-end gap-1.5 rounded-lg border border-white/[0.06] bg-white/[0.012] px-3 pb-3 pt-5">
        {data.map((item, index) => {
          const percentage = Math.max(
            item.total > 0 ? 5 : 1,
            (item.total / max) * 100,
          );

          return (
            <div
              key={`${item.bucketStart}-${index}`}
              className="group relative flex h-full min-w-0 flex-1 items-end"
              title={`${item.total} runs`}
            >
              <div
                className="w-full rounded-t-sm bg-white/[0.16] transition-all duration-300 group-hover:bg-white/[0.28]"
                style={{ height: `${percentage}%` }}
              />
            </div>
          );
        })}
      </div>

      <div className="flex items-center justify-between px-1 font-mono text-[9px] text-white/20">
        <span>{formatBucket(data[0]?.bucketStart)}</span>
        <span>{formatBucket(data[Math.floor(data.length / 2)]?.bucketStart)}</span>
        <span>{formatBucket(data[data.length - 1]?.bucketStart)}</span>
      </div>
    </div>
  );
}

function AttentionItem({
  tone,
  icon: Icon,
  title,
  description,
  href,
}: {
  tone: "danger" | "active" | "neutral";
  icon: typeof AlertCircle;
  title: string;
  description: string;
  href: string;
}) {
  return (
    <Link
      href={href}
      className="group flex items-start gap-3 rounded-lg border border-white/[0.06] bg-white/[0.018] p-3 transition hover:border-white/[0.1] hover:bg-white/[0.03]"
    >
      <span
        className={cn(
          "mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-md",
          tone === "danger"
            ? "bg-red-500/[0.08] text-red-300/75"
            : tone === "active"
              ? "bg-amber-500/[0.08] text-amber-300/75"
              : "bg-white/[0.05] text-white/35",
        )}
      >
        <Icon className="size-3.5" />
      </span>

      <span className="min-w-0 flex-1">
        <span className="block text-xs font-medium text-white/70 group-hover:text-white/90">
          {title}
        </span>
        <span className="mt-1 block text-[10px] leading-4 text-white/28">
          {description}
        </span>
      </span>

      <ArrowRight className="mt-1 size-3.5 shrink-0 text-white/10 transition group-hover:text-white/35" />
    </Link>
  );
}

function OperationalCard({
  icon: Icon,
  title,
  description,
  value,
  href,
}: {
  icon: typeof Activity;
  title: string;
  description: string;
  value: string;
  href: string;
}) {
  return (
    <Link
      href={href}
      className="group flex items-center gap-4 rounded-xl border border-white/[0.08] bg-white/[0.02] px-4 py-3.5 transition hover:bg-white/[0.035]"
    >
      <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-white/[0.04] text-white/35">
        <Icon className="size-4" />
      </span>

      <span className="min-w-0 flex-1">
        <span className="block text-xs font-medium text-white/65 group-hover:text-white/85">
          {title}
        </span>
        <span className="mt-0.5 block truncate text-[10px] text-white/25">
          {description}
        </span>
      </span>

      <span className="shrink-0 rounded-full border border-white/[0.07] bg-white/[0.025] px-2 py-1 font-mono text-[9px] uppercase tracking-wider text-white/30">
        {value}
      </span>
    </Link>
  );
}

function EmptyPanel({
  icon: Icon,
  title,
  description,
  action,
  href,
}: {
  icon: typeof Zap;
  title: string;
  description: string;
  action: string;
  href: string;
}) {
  return (
    <div className="flex flex-col items-center justify-center px-4 py-10 text-center">
      <span className="flex size-8 items-center justify-center rounded-lg bg-white/[0.04] text-white/25">
        <Icon className="size-4" />
      </span>

      <p className="mt-3 text-sm font-medium text-white/55">
        {title}
      </p>

      <p className="mt-1 max-w-xs text-xs leading-5 text-white/25">
        {description}
      </p>

      <Link
        href={href}
        className="mt-4 text-xs text-white/45 underline underline-offset-2 hover:text-white/80"
      >
        {action}
      </Link>
    </div>
  );
}

function ChartSkeleton() {
  return (
    <div className="flex h-44 items-end gap-1.5 rounded-lg border border-white/[0.06] bg-white/[0.012] px-3 pb-3 pt-5">
      {Array.from({ length: 18 }).map((_, index) => (
        <Skeleton
          key={index}
          className="flex-1 rounded-t-sm"
          style={{
            height: `${25 + ((index * 17) % 65)}%`,
          }}
        />
      ))}
    </div>
  );
}

function RecentExecutionsSkeleton() {
  return (
    <div>
      {Array.from({ length: 6 }).map((_, index) => (
        <div
          key={index}
          className="flex items-center gap-3 border-b border-white/[0.05] px-1 py-3 last:border-b-0"
        >
          <Skeleton className="size-2 rounded-full" />
          <span className="flex min-w-0 flex-1 flex-col gap-1.5">
            <Skeleton className="h-3 w-36 max-w-full" />
            <Skeleton className="h-2.5 w-24 max-w-full" />
          </span>
          <Skeleton className="h-2.5 w-12" />
        </div>
      ))}
    </div>
  );
}

function getHealthLabel(
  successRate: number,
  failed: number,
): string {
  if (failed > 0) return "Needs attention";
  if (successRate >= 0.99) return "Healthy";
  if (successRate >= 0.95) return "Stable";
  if (successRate >= 0.9) return "Watch closely";
  return "Needs attention";
}

function getHealthTone(
  successRate: number,
): "neutral" | "healthy" | "danger" {
  if (successRate >= 0.95) return "healthy";
  if (successRate >= 0.9) return "neutral";
  return "danger";
}

function getHealthTextClass(
  successRate: number,
): string {
  if (successRate >= 0.95) return "text-emerald-400/60";
  if (successRate >= 0.9) return "text-white/35";
  return "text-red-300/70";
}

function getHealthDot(successRate: number): string {
  if (successRate >= 0.95) return "bg-emerald-400/80";
  if (successRate >= 0.9) return "bg-amber-300/80";
  return "bg-red-400/80";
}

function isLiveStatus(status: ExecutionStatus): boolean {
  return (
    status === "RUNNING" ||
    status === "WAITING" ||
    status === "QUEUED"
  );
}

function statusDotColor(status: ExecutionStatus): string {
  switch (status) {
    case "SUCCEEDED":
      return "bg-emerald-500";
    case "FAILED":
      return "bg-red-500";
    case "RUNNING":
    case "QUEUED":
      return "bg-amber-500 animate-pulse";
    case "WAITING":
      return "bg-blue-500 animate-pulse";
    default:
      return "bg-white/20";
  }
}

function formatBucket(value?: string): string {
  if (!value) return "—";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return "—";

  return date.toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
  });
}
