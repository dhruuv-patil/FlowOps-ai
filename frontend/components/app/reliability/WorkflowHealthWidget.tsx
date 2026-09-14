"use client";

import { AlertTriangle, CheckCircle, Loader2, Shield, Zap } from "lucide-react";
import { format } from "date-fns";
import { cn } from "@/lib/utils";

import type { WorkflowHealth } from "@/types";

interface WorkflowHealthWidgetProps {
  health: WorkflowHealth;
  className?: string;
}

const SEVERITY_ICONS = {
  CRITICAL: AlertTriangle,
  HIGH: Zap,
  MEDIUM: Shield,
  LOW: CheckCircle,
} as const;

export function WorkflowHealthWidget({ health, className }: WorkflowHealthWidgetProps) {
  const score = health.reliabilityScore;
  const getScoreColor = () => {
    if (score >= 80) return "text-green-400";
    if (score >= 60) return "text-yellow-400";
    if (score >= 40) return "text-orange-400";
    return "text-red-400";
  };

  const getScoreBg = () => {
    if (score >= 80) return "bg-green-500/10 border-green-500/20";
    if (score >= 60) return "bg-yellow-500/10 border-yellow-500/20";
    if (score >= 40) return "bg-orange-500/10 border-orange-500/20";
    return "bg-red-500/10 border-red-500/20";
  };

  return (
    <div className={cn("space-y-4", className)}>
      {/* Reliability Score */}
      <div className="flex items-center gap-4 p-4 rounded-xl border border-white/[0.08] bg-[#0a0a0a]">
        <div className={cn(
          "flex size-16 shrink-0 items-center justify-center rounded-full font-bold text-2xl",
          getScoreColor(),
          getScoreBg(),
        )}>
          {score}
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-white/50">Reliability Score</p>
          <p className="text-lg font-semibold text-white/90">
            {score >= 80 ? "Healthy" : score >= 60 ? "Degraded" : score >= 40 ? "At Risk" : "Critical"}
          </p>
          <div className="mt-2 h-2 w-full bg-white/[0.06] rounded-full overflow-hidden">
            <div
              className={cn(
                "h-full rounded-full transition-all duration-500",
                getScoreColor().replace("text-", "bg-"),
              )}
              style={{ width: `${score}%` }}
            />
          </div>
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          label="Total Runs"
          value={health.totalExecutions.toLocaleString()}
          icon={CheckCircle}
          color="text-blue-400"
          bg="bg-blue-500/10"
        />
        <MetricCard
          label="Success Rate"
          value={`${(health.successRate * 100).toFixed(1)}%`}
          icon={CheckCircle}
          color={health.successRate >= 0.95 ? "text-[#4ade80]" : health.successRate >= 0.8 ? "text-[#facc15]" : "text-red-400"}
          bg={health.successRate >= 0.95 ? "bg-emerald-500/10" : health.successRate >= 0.8 ? "bg-amber-500/10" : "bg-red-500/10"}
        />
        <MetricCard
          label="Failed Runs"
          value={health.failedExecutions.toLocaleString()}
          icon={AlertTriangle}
          color={health.failedExecutions > 0 ? "text-red-400" : "text-[#4ade80]"}
          bg={health.failedExecutions > 0 ? "bg-red-500/10" : "bg-emerald-500/10"}
        />
        <MetricCard
          label="Open Anomalies"
          value={health.openAnomalies.toLocaleString()}
          icon={AlertTriangle}
          color={health.openAnomalies > 0 ? "text-[#facc15]" : "text-[#4ade80]"}
          bg={health.openAnomalies > 0 ? "bg-amber-500/10" : "bg-emerald-500/10"}
        />
      </div>

      {/* Anomaly Breakdown */}
      {health.openAnomalies > 0 && (
        <div className="rounded-xl border border-white/[0.08] bg-[#0a0a0a] p-4">
          <p className="text-sm font-medium text-white/70 mb-3">Open Anomalies by Severity</p>
          <div className="flex flex-wrap gap-2">
            {health.criticalAnomalies > 0 && (
              <AnomalyCountBadge
                count={health.criticalAnomalies}
                label="Critical"
                icon={AlertTriangle}
                color="text-red-400"
                bg="bg-red-500/10"
                border="border-red-500/20"
              />
            )}
            {health.highAnomalies > 0 && (
              <AnomalyCountBadge
                count={health.highAnomalies}
                label="High"
                icon={Zap}
                color="text-[#facc15]"
                bg="bg-amber-500/10"
                border="border-amber-500/20"
              />
            )}
            {health.mediumAnomalies > 0 && (
              <AnomalyCountBadge
                count={health.mediumAnomalies}
                label="Medium"
                icon={Shield}
                color="text-[#facc15]"
                bg="bg-amber-500/10"
                border="border-amber-500/20"
              />
            )}
            {health.lowAnomalies > 0 && (
              <AnomalyCountBadge
                count={health.lowAnomalies}
                label="Low"
                icon={CheckCircle}
                color="text-[#4ade80]"
                bg="bg-emerald-500/10"
                border="border-emerald-500/20"
              />
            )}
          </div>
        </div>
      )}

      {/* Last Updated */}
      <p className="text-xs text-white/35 text-center">
        Score computed at {format(new Date(health.scoreComputedAt), "MMM d, HH:mm")}
      </p>
    </div>
  );
}

function MetricCard({
  label,
  value,
  icon: Icon,
  color,
  bg,
}: {
  label: string;
  value: string;
  icon: React.ComponentType<{ className?: string }>;
  color: string;
  bg: string;
}) {
  return (
    <div className="rounded-xl border border-white/[0.08] bg-[#0a0a0a] p-4">
      <div className="flex items-center gap-3">
        <div className={cn("flex size-10 items-center justify-center rounded-lg", bg)}>
          <Icon className={cn("size-5", color)} />
        </div>
        <div>
          <p className="text-sm text-white/50">{label}</p>
          <p className="text-lg font-semibold text-white/90">{value}</p>
        </div>
      </div>
    </div>
  );
}

function AnomalyCountBadge({
  count,
  label,
  icon: Icon,
  color,
  bg,
  border,
}: {
  count: number;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  color: string;
  bg: string;
  border: string;
}) {
  return (
    <span className={cn(
      "inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-sm font-medium",
      color,
      bg,
      border,
    )}>
      <Icon className="size-3" />
      <span>{label}</span>
      <span className="ml-1">{count}</span>
    </span>
  );
}