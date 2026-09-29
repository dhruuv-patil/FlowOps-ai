"use client";

import React from "react";
import { Check, X, Minus, Clock } from "lucide-react";
import { cn } from "@/lib/utils";
import type { ExecutionNodeState } from "@/types";

export interface TimelineNodeRowProps {
  node: ExecutionNodeState;
  onClick?: (nodeId: string) => void;
}

function statusIcon(status: string) {
  switch (status) {
    case "SUCCEEDED":
      return <Check className="w-3.5 h-3.5 text-emerald-400" />;
    case "FAILED":
      return <X className="w-3.5 h-3.5 text-red-400" />;
    case "SKIPPED":
      return <Minus className="w-3.5 h-3.5 text-white/30" />;
    case "RUNNING":
      return <Clock className="w-3.5 h-3.5 text-blue-300 animate-pulse" />;
    case "QUEUED":
      return <span className="w-3.5 h-3.5 inline-block rounded-full bg-white/15" />;
    case "WAITING":
      return <Clock className="w-3.5 h-3.5 text-amber-400/60" />;
    case "CANCELED":
      return <Minus className="w-3.5 h-3.5 text-white/20" />;
    default:
      return <span className="w-3.5 h-3.5 inline-block rounded-full bg-white/[0.06]" />;
  }
}

export function TimelineNodeRow({ node, onClick }: TimelineNodeRowProps) {
  const duration =
    node.durationMs != null ? `${node.durationMs}ms` : "—";

  const statusLabel =
    node.status === "SUCCEEDED" || node.status === "FAILED"
      ? duration
      : node.status === "SKIPPED"
      ? "skipped"
      : "—";

  return (
    <div
      onClick={() => onClick?.(node.nodeId)}
      className={cn(
        "cursor-pointer group flex items-center justify-between gap-3 rounded-lg border border-transparent hover:border-white/[0.08] hover:bg-white/[0.02] px-3 py-2 transition"
      )}
    >
      <div className="flex items-center gap-3 min-w-0">
        <span className="shrink-0">{statusIcon(node.status)}</span>
        <div className="min-w-0">
          <div className="text-[12px] font-medium text-white/90 truncate">
            {node.label ?? node.nodeId}
          </div>
          <div className="text-[10px] text-white/40">
            {node.nodeType}
            {node.activeHandles && node.activeHandles.length > 0
              ? ` → ${node.activeHandles.join(", ")}`
              : ""}
          </div>
        </div>
      </div>

      <div className="shrink-0 text-[11px] font-mono text-white/40">
        {statusLabel}
      </div>
    </div>
  );
}
