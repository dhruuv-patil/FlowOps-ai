"use client";

import {
  useMemo,
  useState,
  type ReactNode,
} from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import {
  Activity,
  ArrowRight,
  Clock3,
  Plus,
  Play,
} from "lucide-react";

import {
  fetchAnomalies,
  fetchCurrentOrganization,
  fetchExecutionStats,
  fetchExecutions,
} from "@/lib/api";

import {
  formatDuration,
  formatRelativeTime,
  isLiveStatus,
} from "@/lib/format";

import type {
  ExecutionStatus,
  ExecutionSummary,
  StatsRange,
} from "@/types";

import { cn } from "@/lib/utils";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

import {
  Reveal,
  StaggerGroup,
  StaggerItem,
  HoverLift,
} from "@/components/motion/motion-primitives";


/* ==========================================================================
   CONSTANTS
============================================================================ */

const RANGE_OPTIONS: StatsRange[] = [
  "24h",
  "7d",
  "30d",
  "90d",
];

/* ==========================================================================
   PAGE
============================================================================ */

export default function DashboardPage() {
  const [range, setRange] =
    useState<StatsRange>("7d");

  /* ------------------------------------------------------------------------
     EXECUTIONS
  ------------------------------------------------------------------------ */

  const executionsQuery = useQuery({
    queryKey: [
      "executions",
      "dashboard",
    ],

    queryFn: () =>
      fetchExecutions({
        limit: 12,
      }),

    staleTime: 10_000,

    refetchInterval: query => {
      const executions =
        query.state.data
          ?.executions ?? [];

      const hasLiveExecution =
        executions.some(execution =>
          isLiveStatus(
            execution.status,
          ),
        );

      return hasLiveExecution
        ? 5_000
        : false;
    },
  });

  /* ------------------------------------------------------------------------
     STATS
  ------------------------------------------------------------------------ */

  const statsQuery = useQuery({
    queryKey: [
      "executions",
      "stats",
      range,
    ],

    queryFn: () =>
      fetchExecutionStats(range),

    staleTime: 15_000,

    refetchInterval: 15_000,
  });

  /* ------------------------------------------------------------------------
     ORGANIZATION
  ------------------------------------------------------------------------ */

  const organizationQuery =
    useQuery({
      queryKey: [
        "org",
        "current",
      ],

      queryFn:
        fetchCurrentOrganization,

      staleTime:
        5 * 60 * 1000,
    });

  /* ------------------------------------------------------------------------
     OPEN ANOMALIES
     ------------------------------------------------------------------------

     ExecutionStats intentionally does not contain anomaly counts.
     Anomalies are their own reliability resource.
  ------------------------------------------------------------------------ */

  const anomaliesQuery =
    useQuery({
      queryKey: [
        "reliability",
        "anomalies",
        "open",
      ],

      queryFn: () =>
        fetchAnomalies({
          status: "OPEN",
        }),

      staleTime: 15_000,

      refetchInterval: 15_000,
    });

  /* ------------------------------------------------------------------------
     DATA
  ------------------------------------------------------------------------ */

  const executions =
    executionsQuery.data
      ?.executions ?? [];

  const stats =
    statsQuery.data;

  const totals =
    stats?.totals;

  const series =
    stats?.series ?? [];

  const openAnomalies =
    anomaliesQuery.data
      ?.anomalies ?? [];

  /* ------------------------------------------------------------------------
     KPI VALUES
  ------------------------------------------------------------------------ */

  const totalRuns =
    totals?.total ?? 0;

  /*
   * Backend returns successRate as a 0–1 fraction.
   *
   * Example:
   * 0.9872 -> 98.72%
   */
  const successRate =
    (totals?.successRate ?? 0) *
    100;

  const failedRuns =
    totals?.failed ?? 0;

  /*
   * Active means all non-terminal execution
   * states represented by the stats contract.
   */
  const activeRuns =
    (totals?.running ?? 0) +
    (totals?.waiting ?? 0) +
    (totals?.queued ?? 0);

  const avgDurationMs =
    totals?.avgDurationMs ??
    null;

  const activeAnomalies =
    openAnomalies.length;

  const highPriorityAnomalies =
    openAnomalies.filter(
      anomaly =>
        anomaly.severity ===
          "HIGH" ||
        anomaly.severity ===
          "CRITICAL",
    ).length;

  /* ------------------------------------------------------------------------
     RECENT EXECUTIONS
  ------------------------------------------------------------------------ */

  const recentExecutions =
    useMemo<ExecutionSummary[]>(
      () => executions,
      [executions],
    );

  /* ------------------------------------------------------------------------
     RANGE LABEL
  ------------------------------------------------------------------------ */

  const rangeLabel =
    getRangeLabel(range);

  /* ------------------------------------------------------------------------
     ORGANIZATION NAME
  ------------------------------------------------------------------------ */

  const organizationName =
    organizationQuery.data
      ?.organization.name ??
    "Your organization";

  /* ------------------------------------------------------------------------
     CHART
  ------------------------------------------------------------------------ */

  const chart =
    useMemo(
      () =>
        buildChart(
          series,
        ),
      [series],
    );

  /* ------------------------------------------------------------------------
     LOADING
  ------------------------------------------------------------------------ */

  const initialLoading =
    executionsQuery.isPending ||
    statsQuery.isPending;

  /* ------------------------------------------------------------------------
     RENDER
  ------------------------------------------------------------------------ */

  return (
    <main className="min-h-full bg-black text-white">
      <div className="mx-auto max-w-7xl px-4 pb-10 pt-5 sm:px-6 lg:px-8">

        {/* ================================================================
            HEADER
        ================================================================ */}

        <Reveal>
          <div className="flex flex-col gap-4 border-b border-white/[0.055] pb-5 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <div className="flex size-7 items-center justify-center rounded-lg border border-white/[0.07] bg-[#111114]">
                  <Activity className="size-3.5 text-white/50" />
                </div>
                <span className="mono-eyebrow">Execution Control Plane</span>
              </div>

              <h1 className="mt-3 text-[24px] font-semibold tracking-[-0.04em] text-white">
                Overview
              </h1>

              <p className="mt-1 text-xs text-white/35">
                Monitor workflow health and reliability across {organizationName}.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
                <div className="flex h-10 items-center gap-1 rounded-lg border border-white/[0.075] bg-[#0A0A0C] p-1">
                  {RANGE_OPTIONS.map(
                    value => {
                      const active =
                        range ===
                        value;

                      return (
                        <button
                          key={
                            value
                          }
                          type="button"
                          onClick={() =>
                            setRange(
                              value,
                            )
                          }
                          className={cn(
                            "h-8 rounded-md px-3 text-[11px] font-medium transition-colors",
                            active
                              ? "bg-white/[0.09] text-white"
                              : "text-white/30 hover:bg-white/[0.035] hover:text-white/65",
                          )}
                        >
                          {getRangeLabel(
                            value,
                          )}
                        </button>
                      );
                    },
                  )}
                </div>

                <Link href="/workflows">
                  <Button className="h-10 gap-2 rounded-lg bg-white px-4 text-sm font-medium text-black shadow-none hover:bg-white/90">
                    <Plus className="size-4" />
                    New workflow
                  </Button>
                </Link>
            </div>
          </div>
        </Reveal>

        {/* ================================================================
            KPI STRIP
        ================================================================ */}

        <Reveal
          delay={0.04}
          distance={8}
        >
          <div className="grid grid-cols-2 overflow-hidden rounded-xl border border-white/[0.065] bg-[#0D0D10] sm:grid-cols-3 lg:grid-cols-5">
            {[
              {
                label: "Workflow runs",
                value: initialLoading ? "—" : totalRuns.toLocaleString(),
                detail: initialLoading ? "Loading" : rangeLabel,
                tone: "neutral",
              },
              {
                label: "Success rate",
                value: initialLoading ? "—" : `${successRate.toFixed(2)}%`,
                detail: initialLoading ? "Loading" : getHealthLabel(successRate),
                tone: getHealthTone(successRate),
              },
              {
                label: "Failed runs",
                value: initialLoading ? "—" : failedRuns.toLocaleString(),
                detail: initialLoading ? "Loading" : failedRuns > 0 ? "Needs attention" : "No failures",
                tone: failedRuns > 0 ? "danger" : "success",
              },
              {
                label: "Active anomalies",
                value: anomaliesQuery.isPending ? "—" : activeAnomalies.toLocaleString(),
                detail: anomaliesQuery.isPending ? "Loading" : highPriorityAnomalies > 0 ? `${highPriorityAnomalies} high priority` : "Baseline monitoring",
                tone: activeAnomalies > 0 ? "warning" : "neutral",
              },
              {
                label: "Mean execution",
                value: initialLoading ? "—" : avgDurationMs !== null ? formatDuration(avgDurationMs) : "—",
                detail: avgDurationMs !== null ? "Average duration" : "No completed runs",
                tone: "neutral",
              },
            ].map((item, index) => (
              <div
                key={item.label}
                className={`min-h-[104px] px-5 py-4 ${
                  index > 0 ? "border-l border-white/[0.05]" : ""
                }`}
              >
                <p className="text-[10px] font-medium text-white/35">
                  {item.label}
                </p>

                <p className="mt-3 text-[28px] font-semibold leading-none tracking-[-0.05em] tabular-nums text-white/95">
                  {item.value}
                </p>

                <div className="mt-3 flex items-center gap-1.5">
                  <span
                    className={cn(
                      "size-1.5 rounded-full",
                      item.tone === "success" && "bg-emerald-400/75",
                      item.tone === "warning" && "bg-amber-400/75",
                      item.tone === "danger" && "bg-red-400/70",
                      item.tone === "neutral" && "bg-white/20",
                    )}
                  />
                  <span className="truncate text-[10px] text-white/22">
                    {item.detail}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </Reveal>

        {/* ================================================================
            HEALTH / RELIABILITY
        ================================================================ */}

        <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_330px]">

          {/* ==============================================================
              WORKFLOW HEALTH
          ============================================================== */}

          <Reveal
            delay={0.06}
            distance={10}
          >
            <Card className="overflow-hidden rounded-xl border-white/[0.065] bg-[#0D0D10] shadow-none">
              <CardContent className="p-0">

                <div className="flex items-center justify-between border-b border-white/[0.055] px-5 py-4">
                  <div>
                    <h2 className="text-sm font-medium text-white/80">
                      Workflow health
                    </h2>

                    <p className="mt-1 text-[11px] text-white/28">
                      Executions and failures
                      over time
                    </p>
                  </div>

                  <div className="flex items-center gap-4">
                    <ChartLegend
                      label="Runs"
                      color="bg-indigo-400"
                    />

                    <ChartLegend
                      label="Failed"
                      color="bg-red-400"
                    />
                  </div>
                </div>

                <div className="relative h-[300px] px-4 pb-5 pt-5 sm:px-5">
                  {statsQuery.isPending ? (
                    <ChartSkeleton />
                  ) : series.length ===
                    0 ? (
                    <EmptyChart />
                  ) : (
                    <ExecutionChart
                      chart={chart}
                    />
                  )}
                </div>
              </CardContent>
            </Card>
          </Reveal>

          {/* ==============================================================
              RELIABILITY
          ============================================================== */}

          <Reveal
            delay={0.09}
            distance={10}
          >
            <ReliabilityCard
              loading={
                statsQuery.isPending
              }
              successRate={
                successRate
              }
              failedRuns={
                failedRuns
              }
              activeRuns={
                activeRuns
              }
              anomalies={
                activeAnomalies
              }
            />
          </Reveal>
        </div>

        {/* ================================================================
            RECENT EXECUTIONS
        ================================================================ */}

        <Reveal
          delay={0.12}
          distance={10}
        >
          <section className="mt-5 overflow-hidden rounded-xl border border-white/[0.065] bg-[#0D0D10]">

            <div className="flex items-center justify-between border-b border-white/[0.055] px-5 py-4">
              <div>
                <h2 className="text-sm font-medium text-white/80">
                  Recent executions
                </h2>

                <p className="mt-1 text-[11px] text-white/28">
                  Latest workflow activity
                  across your workspace
                </p>
              </div>

              <Link
                href="/executions"
                className="flex items-center gap-1.5 text-[11px] text-white/35 transition-colors hover:text-white/75"
              >
                View all
                <ArrowRight className="size-3" />
              </Link>
            </div>

            {executionsQuery.isPending ? (
              <ExecutionTableSkeleton />
            ) : executionsQuery.isError ? (
              <QueryError
                message="Unable to load recent executions."
                onRetry={() =>
                  executionsQuery.refetch()
                }
              />
            ) : recentExecutions.length ===
              0 ? (
              <EmptyExecutions />
            ) : (
              <ExecutionTable
                executions={
                  recentExecutions
                }
              />
            )}
          </section>
        </Reveal>

        {/* ================================================================
            QUICK LINKS
        ================================================================ */}

        <StaggerGroup stagger={0.035}>
          <div className="mt-5 grid gap-3 sm:grid-cols-3">

            <StaggerItem distance={8}>
              <DashboardLink
                href="/executions"
                title="Execution history"
                description="Inspect runs, timing and outputs"
              />
            </StaggerItem>

            <StaggerItem distance={8}>
              <DashboardLink
                href="/anomalies"
                title="Reliability"
                description="Review anomalies and behavioral drift"
              />
            </StaggerItem>

            <StaggerItem distance={8}>
              <DashboardLink
                href="/integrations"
                title="Integrations"
                description="Manage workflow connections"
              />
            </StaggerItem>
          </div>
        </StaggerGroup>

        {/* ================================================================
            FOOTER
        ================================================================ */}

        <div className="mt-6 flex items-center justify-between border-t border-white/[0.045] pt-3">
          <span className="text-[9px] text-white/18">
            {rangeLabel}
          </span>

          <span className="font-mono text-[9px] uppercase tracking-[0.13em] text-white/12">
            FLOWOPS / EXECUTION CONTROL
          </span>
        </div>
      </div>
    </main>
  );
}

/* ==========================================================================
   DASHBOARD METRIC
============================================================================ */

function DashboardMetric({
  label,
  value,
  sublabel,
  icon,
  tone = "neutral",
}: {
  label: string;
  value: string;
  sublabel: string;
  icon?: ReactNode;
  tone?:
    | "neutral"
    | "success"
    | "warning"
    | "danger";
}) {
  return (
    <div className="min-h-[142px] border-r border-white/[0.05] px-5 py-4 last:border-r-0">
      <div className="flex items-center gap-1.5">
        {icon && (
          <span className="text-white/25">
            {icon}
          </span>
        )}

        <span className="text-[10px] font-medium text-white/35">
          {label}
        </span>
      </div>

      <div className="mt-4 text-[30px] font-semibold leading-none tracking-[-0.05em] tabular-nums text-white/95">
        {value}
      </div>

      <div className="mt-3 flex items-center gap-1.5">
        <span
          className={cn(
            "size-1.5 rounded-full",
            tone === "success" &&
              "bg-emerald-400/75",
            tone === "warning" &&
              "bg-amber-400/75",
            tone === "danger" &&
              "bg-red-400/70",
            tone === "neutral" &&
              "bg-white/20",
          )}
        />

        <span className="truncate text-[10px] text-white/22">
          {sublabel}
        </span>
      </div>
    </div>
  );
}

/* ==========================================================================
   CHART
============================================================================ */

type ChartPoint = {
  x: number;
  total: number;
  failed: number;
  label: string;
};

function buildChart(
  series: Array<{
    bucketStart: string;
    total: number;
    failed: number;
  }>,
) {
  const width = 760;
  const height = 240;
  const paddingX = 12;
  const paddingTop = 12;
  const paddingBottom = 12;

  if (!series.length) {
    return {
      width,
      height,
      points: [] as ChartPoint[],
      max: 1,
    };
  }

  const max = Math.max(
    ...series.map(point =>
      Math.max(
        point.total,
        point.failed,
      ),
    ),
    1,
  );

  const usableWidth =
    width - paddingX * 2;

  const usableHeight =
    height -
    paddingTop -
    paddingBottom;

  const points: ChartPoint[] =
    series.map(
      (point, index) => {
        const x =
          series.length === 1
            ? width / 2
            : paddingX +
              (index /
                (series.length -
                  1)) *
                usableWidth;

        return {
          x,
          total: point.total,
          failed: point.failed,
          label: formatBucket(
            point.bucketStart,
          ),
        };
      },
    );

  return {
    width,
    height,
    points,
    max,
    paddingTop,
    usableHeight,
  };
}

function ExecutionChart({
  chart,
}: {
  chart: ReturnType<
    typeof buildChart
  >;
}) {
  const {
    width,
    height,
    points,
    max,
    paddingTop,
    usableHeight,
  } = chart;

  const totalPoints =
    points.map(point => ({
      x: point.x,
      y:
        height -
        12 -
        (point.total / max) *
          usableHeight,
    }));

  const failedPoints =
    points.map(point => ({
      x: point.x,
      y:
        height -
        12 -
        (point.failed / max) *
          usableHeight,
    }));

  const totalPath =
    createLinePath(totalPoints);

  const failedPath =
    createLinePath(
      failedPoints,
    );

  const totalArea =
    totalPoints.length
      ? `${totalPath} L ${
          totalPoints[
            totalPoints.length - 1
          ].x
        } ${height - 12} L ${
          totalPoints[0].x
        } ${height - 12} Z`
      : "";

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      preserveAspectRatio="none"
      className="h-full w-full"
    >
      <defs>
        <linearGradient
          id="runs-area"
          x1="0"
          y1="0"
          x2="0"
          y2="1"
        >
          <stop
            offset="0%"
            stopColor="rgb(99 102 241)"
            stopOpacity="0.12"
          />

          <stop
            offset="100%"
            stopColor="rgb(99 102 241)"
            stopOpacity="0"
          />
        </linearGradient>
      </defs>

      {/* GRID */}

      {[0, 1, 2, 3, 4].map(
        index => {
          const y =
            paddingTop +
            (index *
              usableHeight) /
              4;

          return (
            <line
              key={index}
              x1="12"
              x2="748"
              y1={y}
              y2={y}
              stroke="rgba(255,255,255,0.045)"
              strokeWidth="1"
            />
          );
        },
      )}

      {/* AREA */}

      {totalArea && (
        <path
          d={totalArea}
          fill="url(#runs-area)"
        />
      )}

      {/* RUNS */}

      {totalPath && (
        <path
          d={totalPath}
          fill="none"
          stroke="rgb(129 140 248)"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      )}

      {/* FAILED */}

      {failedPath && (
        <path
          d={failedPath}
          fill="none"
          stroke="rgb(248 113 113)"
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      )}

      {/* RUN POINTS */}

      {totalPoints.map(
        (point, index) => (
          <circle
            key={`total-${index}`}
            cx={point.x}
            cy={point.y}
            r="2.25"
            fill="rgb(129 140 248)"
            stroke="#0D0D10"
            strokeWidth="2"
          />
        ),
      )}

      {/* FAILED POINTS */}

      {failedPoints.map(
        (point, index) => {
          if (
            points[index]
              .failed === 0
          ) {
            return null;
          }

          return (
            <circle
              key={`failed-${index}`}
              cx={point.x}
              cy={point.y}
              r="2"
              fill="rgb(248 113 113)"
              stroke="#0D0D10"
              strokeWidth="2"
            />
          );
        },
      )}
    </svg>
  );
}

function createLinePath(
  points: Array<{
    x: number;
    y: number;
  }>,
) {
  if (!points.length) {
    return "";
  }

  return points
    .map(
      (point, index) =>
        `${index === 0 ? "M" : "L"} ${point.x} ${point.y}`,
    )
    .join(" ");
}

/* ==========================================================================
   RELIABILITY
============================================================================ */

function ReliabilityCard({
  loading,
  successRate,
  failedRuns,
  activeRuns,
  anomalies,
}: {
  loading: boolean;
  successRate: number;
  failedRuns: number;
  activeRuns: number;
  anomalies: number;
}) {
  const clamped =
    Math.max(
      0,
      Math.min(
        100,
        successRate,
      ),
    );

  const radius = 42;

  const circumference =
    2 * Math.PI * radius;

  const offset =
    circumference -
    (clamped / 100) *
      circumference;

  return (
    <Card className="h-full overflow-hidden rounded-xl border-white/[0.065] bg-[#0D0D10] shadow-none">
      <CardContent className="p-0">

        <div className="border-b border-white/[0.055] px-5 py-4">
          <h2 className="text-sm font-medium text-white/80">
            Reliability
          </h2>

          <p className="mt-1 text-[11px] text-white/28">
            Execution health
          </p>
        </div>

        <div className="px-5 py-5">

          <div className="flex items-center gap-5">
            <div className="relative size-[112px] shrink-0">
              <svg
                viewBox="0 0 112 112"
                className="size-full -rotate-90"
              >
                <circle
                  cx="56"
                  cy="56"
                  r={radius}
                  fill="none"
                  stroke="rgba(255,255,255,0.055)"
                  strokeWidth="9"
                />

                {!loading && (
                  <circle
                    cx="56"
                    cy="56"
                    r={radius}
                    fill="none"
                    stroke={getHealthStroke(
                      successRate,
                    )}
                    strokeWidth="9"
                    strokeLinecap="round"
                    strokeDasharray={
                      circumference
                    }
                    strokeDashoffset={
                      offset
                    }
                    className="transition-all duration-500"
                  />
                )}
              </svg>

              <div className="absolute inset-0 flex flex-col items-center justify-center">
                <span className="text-[19px] font-semibold tracking-[-0.04em] text-white">
                  {loading
                    ? "—"
                    : `${successRate.toFixed(
                        1,
                      )}%`}
                </span>

                <span className="mt-0.5 font-mono text-[7px] uppercase tracking-[0.12em] text-white/20">
                  success
                </span>
              </div>
            </div>

            <div>
              <p className="text-sm font-medium text-white/70">
                {loading
                  ? "Loading"
                  : getHealthLabel(
                      successRate,
                    )}
              </p>

              <p className="mt-1 text-[11px] leading-5 text-white/25">
                Workflow execution
                reliability
              </p>
            </div>
          </div>

          <div className="mt-6 border-t border-white/[0.05]">

            <ReliabilityRow
              label="Succeeded"
              value={
                loading
                  ? "—"
                  : `${successRate.toFixed(
                      1,
                    )}%`
              }
              tone="success"
            />

            <ReliabilityRow
              label="Failed"
              value={
                loading
                  ? "—"
                  : failedRuns.toLocaleString()
              }
              tone={
                failedRuns > 0
                  ? "danger"
                  : "neutral"
              }
            />

            <ReliabilityRow
              label="Active"
              value={
                loading
                  ? "—"
                  : activeRuns.toLocaleString()
              }
              tone={
                activeRuns > 0
                  ? "active"
                  : "neutral"
              }
            />

            <ReliabilityRow
              label="Open anomalies"
              value={anomalies.toLocaleString()}
              tone={
                anomalies > 0
                  ? "warning"
                  : "neutral"
              }
            />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

/* ==========================================================================
   RELIABILITY ROW
============================================================================ */

function ReliabilityRow({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone:
    | "success"
    | "danger"
    | "warning"
    | "active"
    | "neutral";
}) {
  return (
    <div className="flex items-center justify-between border-b border-white/[0.045] py-3 last:border-b-0">
      <div className="flex items-center gap-2">
        <span
          className={cn(
            "size-1.5 rounded-full",
            tone === "success" &&
              "bg-emerald-400/75",
            tone === "danger" &&
              "bg-red-400/70",
            tone === "warning" &&
              "bg-amber-400/70",
            tone === "active" &&
              "bg-indigo-400/70",
            tone === "neutral" &&
              "bg-white/15",
          )}
        />

        <span className="text-[11px] text-white/32">
          {label}
        </span>
      </div>

      <span className="text-[11px] tabular-nums text-white/55">
        {value}
      </span>
    </div>
  );
}

/* ==========================================================================
   EXECUTION TABLE
============================================================================ */

function ExecutionTable({
  executions,
}: {
  executions: ExecutionSummary[];
}) {
  return (
    <div className="overflow-x-auto">
      <div className="min-w-[760px]">

        {/* HEADER */}

        <div className="grid grid-cols-[minmax(280px,1.8fr)_110px_90px_110px_100px] border-b border-white/[0.045] px-5 py-2.5">
          <TableHeader>
            Workflow
          </TableHeader>

          <TableHeader>
            Status
          </TableHeader>

          <TableHeader>
            Duration
          </TableHeader>

          <TableHeader>
            Started
          </TableHeader>

          <TableHeader>
            Trigger
          </TableHeader>
        </div>

        {/* ROWS */}

        {executions.map(
          execution => (
            <Link
              key={execution.id}
              href={`/executions/${execution.id}`}
              className="group grid grid-cols-[minmax(280px,1.8fr)_110px_90px_110px_100px] items-center border-b border-white/[0.04] px-5 py-3.5 transition-colors last:border-b-0 hover:bg-white/[0.018]"
            >
              {/* WORKFLOW */}

              <div className="min-w-0">
                <p className="truncate text-[12px] font-medium text-white/70 transition-colors group-hover:text-white">
                  {execution.workflowName ??
                    "Untitled workflow"}
                </p>

                <p className="mt-1 truncate font-mono text-[9px] text-white/18">
                  v
                  {
                    execution.versionNumber
                  }{" "}
                  ·{" "}
                  {execution.id.slice(
                    0,
                    10,
                  )}
                </p>
              </div>

              {/* STATUS */}

              <div>
                <ExecutionStatusBadge
                  status={
                    execution.status
                  }
                />
              </div>

              {/* DURATION */}

              <span className="text-[10px] tabular-nums text-white/35">
                {execution.durationMs !==
                null
                  ? formatDuration(
                      execution.durationMs,
                    )
                  : "—"}
              </span>

              {/* STARTED */}

              <span className="text-[10px] text-white/30">
                {formatRelativeTime(
                  execution.createdAt,
                )}
              </span>

              {/* TRIGGER */}

              <span className="truncate text-[10px] text-white/28">
                {formatTrigger(
                  execution.triggerType,
                )}
              </span>
            </Link>
          ),
        )}
      </div>
    </div>
  );
}

/* ==========================================================================
   STATUS
============================================================================ */

function ExecutionStatusBadge({
  status,
}: {
  status: ExecutionStatus;
}) {
  const config =
    getExecutionStatusConfig(
      status,
    );

  return (
    <Badge
      variant="outline"
      className={cn(
        "rounded-md border px-2 py-0.5 text-[9px] font-medium",
        config.className,
      )}
    >
      {config.label}
    </Badge>
  );
}

function getExecutionStatusConfig(
  status: ExecutionStatus,
) {
  switch (status) {
    case "SUCCEEDED":
      return {
        label: "Succeeded",
        className:
          "border-emerald-400/15 bg-emerald-400/[0.045] text-emerald-300/70",
      };

    case "FAILED":
      return {
        label: "Failed",
        className:
          "border-red-400/15 bg-red-400/[0.045] text-red-300/70",
      };

    case "RUNNING":
      return {
        label: "Running",
        className:
          "border-indigo-400/15 bg-indigo-400/[0.045] text-indigo-300/70",
      };

    case "WAITING":
      return {
        label: "Waiting",
        className:
          "border-blue-400/15 bg-blue-400/[0.045] text-blue-300/70",
      };

    case "QUEUED":
      return {
        label: "Queued",
        className:
          "border-amber-400/15 bg-amber-400/[0.045] text-amber-300/70",
      };

    case "CANCELED":
      return {
        label: "Canceled",
        className:
          "border-white/[0.08] bg-white/[0.025] text-white/35",
      };
  }
}

/* ==========================================================================
   DASHBOARD LINKS
============================================================================ */

function DashboardLink({
  href,
  title,
  description,
}: {
  href: string;
  title: string;
  description: string;
}) {
  return (
    <Link
      href={href}
      className="group block rounded-xl border border-white/[0.06] bg-[#0D0D10] px-4 py-3.5 transition-colors hover:border-white/[0.10] hover:bg-[#0F0F12]"
    >
      <div className="flex items-center justify-between">
        <span className="text-[12px] font-medium text-white/60 transition-colors group-hover:text-white/85">
          {title}
        </span>

        <ArrowRight className="size-3 text-white/18 transition-all group-hover:translate-x-0.5 group-hover:text-white/50" />
      </div>

      <p className="mt-1 text-[10px] text-white/22">
        {description}
      </p>
    </Link>
  );
}

/* ==========================================================================
   CHART LEGEND
============================================================================ */

function ChartLegend({
  label,
  color,
}: {
  label: string;
  color: string;
}) {
  return (
    <div className="flex items-center gap-1.5">
      <span
        className={cn(
          "h-1.5 w-3 rounded-full",
          color,
        )}
      />

      <span className="text-[10px] text-white/30">
        {label}
      </span>
    </div>
  );
}

/* ==========================================================================
   LOADING
============================================================================ */

function ChartSkeleton() {
  return (
    <div className="flex h-full flex-col justify-end gap-3 pb-8">
      <Skeleton className="h-px w-full bg-white/[0.045]" />
      <Skeleton className="h-px w-full bg-white/[0.045]" />
      <Skeleton className="h-px w-full bg-white/[0.045]" />
      <Skeleton className="h-20 w-full rounded-lg bg-white/[0.02]" />
    </div>
  );
}

function ExecutionTableSkeleton() {
  return (
    <div className="min-w-[760px]">
      {Array.from(
        { length: 5 },
        (_, index) => (
          <div
            key={index}
            className="grid grid-cols-[minmax(280px,1.8fr)_110px_90px_110px_100px] items-center gap-3 border-b border-white/[0.04] px-5 py-3.5"
          >
            <div>
              <Skeleton className="h-3.5 w-36 bg-white/[0.045]" />
              <Skeleton className="mt-1.5 h-2.5 w-24 bg-white/[0.025]" />
            </div>

            <Skeleton className="h-5 w-16 rounded-md bg-white/[0.04]" />

            <Skeleton className="h-3 w-10 bg-white/[0.03]" />

            <Skeleton className="h-3 w-14 bg-white/[0.03]" />

            <Skeleton className="h-3 w-14 bg-white/[0.03]" />
          </div>
        ),
      )}
    </div>
  );
}

/* ==========================================================================
   EMPTY / ERROR
============================================================================ */

function EmptyChart() {
  return (
    <div className="flex h-full items-center justify-center">
      <div className="text-center">
        <Activity className="mx-auto size-5 text-white/15" />

        <p className="mt-2 text-xs text-white/28">
          No execution activity
        </p>

        <p className="mt-1 text-[10px] text-white/16">
          Activity will appear here once
          workflows run.
        </p>
      </div>
    </div>
  );
}

function EmptyExecutions() {
  return (
    <div className="flex min-h-[180px] flex-col items-center justify-center px-5 text-center">
      <div className="flex size-10 items-center justify-center rounded-lg border border-white/[0.07] bg-[#111114]">
        <Activity className="size-4 text-white/25" />
      </div>

      <p className="mt-3 text-sm text-white/50">
        No executions yet
      </p>

      <p className="mt-1 text-[11px] text-white/22">
        Run a workflow to start seeing
        activity here.
      </p>
    </div>
  );
}

function QueryError({
  message,
  onRetry,
}: {
  message: string;
  onRetry: () => void;
}) {
  return (
    <div className="flex min-h-[160px] flex-col items-center justify-center px-5 text-center">
      <p className="text-sm text-white/45">
        {message}
      </p>

      <button
        type="button"
        onClick={onRetry}
        className="mt-3 text-[11px] text-white/35 underline underline-offset-4 hover:text-white/70"
      >
        Retry
      </button>
    </div>
  );
}

/* ==========================================================================
   HELPERS
============================================================================ */

function getRangeLabel(
  range: StatsRange,
) {
  switch (range) {
    case "24h":
      return "Today";

    case "7d":
      return "Last 7 days";

    case "30d":
      return "Last 30 days";

    case "90d":
      return "Last 90 days";
  }
}

function getHealthLabel(
  successRate: number,
) {
  if (successRate >= 99) {
    return "Excellent";
  }

  if (successRate >= 95) {
    return "Healthy";
  }

  if (successRate >= 90) {
    return "Needs attention";
  }

  return "At risk";
}

function getHealthTone(
  successRate: number,
):
  | "neutral"
  | "success"
  | "warning"
  | "danger" {
  if (successRate >= 95) {
    return "success";
  }

  if (successRate >= 90) {
    return "warning";
  }

  return "danger";
}

function getHealthStroke(
  successRate: number,
) {
  if (successRate >= 95) {
    return "rgb(52 211 153)";
  }

  if (successRate >= 90) {
    return "rgb(251 191 36)";
  }

  return "rgb(248 113 113)";
}

function formatBucket(
  value: string,
) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "—";
  }

  return date.toLocaleDateString(
    undefined,
    {
      month: "short",
      day: "numeric",
    },
  );
}

function formatTrigger(
  value: string,
) {
  if (!value) {
    return "—";
  }

  return value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(
      /\b\w/g,
      character =>
        character.toUpperCase(),
    );
}

function getHealthTextClass(
  successRate: number,
) {
  if (successRate >= 95) {
    return "text-emerald-300/70";
  }

  if (successRate >= 90) {
    return "text-amber-300/70";
  }

  return "text-red-300/70";
}

function getHealthDot(
  successRate: number,
) {
  if (successRate >= 95) {
    return "bg-emerald-400/80";
  }

  if (successRate >= 90) {
    return "bg-amber-400/80";
  }

  return "bg-red-400/80";
}

function TableHeader({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <span className="text-[9px] font-medium uppercase tracking-[0.1em] text-white/22">
      {children}
    </span>
  );
}