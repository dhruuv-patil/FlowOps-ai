"use client";

import { AlertTriangle, CheckCircle, HelpCircle, Loader2, Zap } from "lucide-react";
import { cn } from "@/lib/utils";

import type { AnomalySeverity } from "@/types";

interface NodeHealthBadgeProps {
  /** The node's execution status */
  status: "PENDING" | "RUNNING" | "WAITING" | "SUCCEEDED" | "FAILED" | "SKIPPED";
  /** Whether this node has an open anomaly */
  hasAnomaly?: boolean;
  /** The anomaly severity if applicable */
  anomalySeverity?: AnomalySeverity;
  /** Show label text */
  showLabel?: boolean;
  /** Compact variant for tight spaces */
  compact?: boolean;
}

/**
 * Visual health indicator for a workflow node.
 * Combines execution status with anomaly detection state.
 */
export function NodeHealthBadge({
  status,
  hasAnomaly,
  anomalySeverity,
  showLabel = true,
  compact = false,
}: NodeHealthBadgeProps) {
  if (compact) {
    return <CompactBadge status={status} hasAnomaly={hasAnomaly} anomalySeverity={anomalySeverity} />;
  }

  return (
    <div className={cn("inline-flex items-center gap-1.5", hasAnomaly && "animate-pulse")}>
      <StatusDot status={status} hasAnomaly={hasAnomaly} anomalySeverity={anomalySeverity} />
      {showLabel && <span className="text-sm font-medium">{getLabel(status, hasAnomaly)}</span>}
    </div>
  );
}

function StatusDot({
  status,
  hasAnomaly,
  anomalySeverity,
}: {
  status: NodeHealthBadgeProps["status"];
  hasAnomaly: boolean | undefined;
  anomalySeverity?: AnomalySeverity;
}) {
  const base = "size-2.5 rounded-full ring-2 ring-[#050505]";

  if (hasAnomaly && anomalySeverity) {
    return (
      <span
        className={cn(
          base,
          anomalySeverity === "CRITICAL" && "bg-red-500 ring-red-500/50",
          anomalySeverity === "HIGH" && "bg-orange-500 ring-orange-500/50",
          anomalySeverity === "MEDIUM" && "bg-yellow-500 ring-yellow-500/50",
          anomalySeverity === "LOW" && "bg-green-500 ring-green-500/50",
        )}
        title={`Anomaly detected: ${anomalySeverity}`}
      />
    );
  }

  switch (status) {
    case "SUCCEEDED":
      return <span className={cn(base, "bg-green-500")} title="Succeeded" />;
    case "FAILED":
      return <span className={cn(base, "bg-red-500")} title="Failed" />;
    case "RUNNING":
      return (
        <span className={cn(base, "bg-blue-500 animate-pulse")} title="Running" />
      );
    case "WAITING":
      return (
        <span className={cn(base, "bg-amber-500 animate-pulse")} title="Waiting" />
      );
    case "PENDING":
      return <span className={cn(base, "bg-white/30")} title="Pending" />;
    case "SKIPPED":
      return <span className={cn(base, "bg-gray-500")} title="Skipped" />;
    default:
      return (
        <span className="size-4 text-white/40" title="Unknown">
          <HelpCircle className="size-4" />
        </span>
      );
  }
}

function CompactBadge({
  status,
  hasAnomaly,
  anomalySeverity,
}: {
  status: NodeHealthBadgeProps["status"];
  hasAnomaly?: boolean;
  anomalySeverity?: AnomalySeverity;
}) {
  if (hasAnomaly && anomalySeverity) {
    const Icon = anomalySeverity === "CRITICAL" ? AlertTriangle : Zap;
    return (
      <span
        className={cn(
          "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium",
          anomalySeverity === "CRITICAL" &&
            "bg-red-500/10 text-red-400 border border-red-500/20",
          anomalySeverity === "HIGH" &&
            "bg-orange-500/10 text-orange-400 border border-orange-500/20",
          anomalySeverity === "MEDIUM" &&
            "bg-yellow-500/10 text-yellow-400 border border-yellow-500/20",
          anomalySeverity === "LOW" &&
            "bg-green-500/10 text-green-400 border border-green-500/20",
        )}
        title={`Anomaly: ${anomalySeverity}`}
      >
        <Icon className="size-3" />
        {anomalySeverity}
      </span>
    );
  }

  switch (status) {
    case "SUCCEEDED":
      return (
        <span
          className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium bg-green-500/10 text-green-400 border border-green-500/20"
        >
          <CheckCircle className="size-3" />
          Success
        </span>
      );
    case "FAILED":
      return (
        <span
          className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium bg-red-500/10 text-red-400 border border-red-500/20"
        >
          <AlertTriangle className="size-3" />
          Failed
        </span>
      );
    case "RUNNING":
      return (
        <span
          className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium bg-blue-500/10 text-blue-400 border border-blue-500/20"
        >
          <Loader2 className="size-3 animate-spin" />
          Running
        </span>
      );
    case "WAITING":
      return (
        <span
          className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20"
        >
          <Zap className="size-3" />
          Waiting
        </span>
      );
    case "PENDING":
      return (
        <span
          className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium bg-white/[0.06] text-white/50 border border-white/[0.08]"
        >
          <HelpCircle className="size-3" />
          Pending
        </span>
      );
    case "SKIPPED":
      return (
        <span
          className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium bg-white/[0.04] text-white/40 border border-white/[0.06]"
        >
          Skipped
        </span>
      );
    default:
      return (
        <span className="text-xs text-white/40">Unknown</span>
      );
  }
}

function getLabel(status: NodeHealthBadgeProps["status"], hasAnomaly: boolean | undefined): string {
  if (hasAnomaly) {
    return `Anomaly detected`;
  }
  switch (status) {
    case "SUCCEEDED":
      return "Succeeded";
    case "FAILED":
      return "Failed";
    case "RUNNING":
      return "Running";
    case "WAITING":
      return "Waiting";
    case "PENDING":
      return "Pending";
    case "SKIPPED":
      return "Skipped";
    default:
      return "Unknown";
  }
}