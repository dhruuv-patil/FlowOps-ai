"use client";

import * as React from "react";
import { Handle, Position, type NodeProps } from "@xyflow/react";
import { Check, Clock3, Loader2, X, Zap } from "lucide-react";

import { cn } from "@/lib/utils";
import type { NodeDefinition } from "@/types";
import {
  ErrorNodesContext,
  type FlowNodeData,
} from "@/components/app/builder/shared";
import {
  NodeIcon,
  resolveAccent,
} from "@/components/app/builder/node-visuals";

export const NodeDefsContext = React.createContext<
  Record<string, NodeDefinition>
>({});

const STATUS_BADGES: Record<
  string,
  { icon: React.ElementType; className: string; spin?: boolean }
> = {
  SUCCEEDED: { icon: Check, className: "bg-emerald-500" },
  FAILED: { icon: X, className: "bg-red-500" },
  RUNNING: { icon: Loader2, className: "bg-indigo-500", spin: true },
  WAITING: { icon: Clock3, className: "bg-blue-500" },
  QUEUED: { icon: Clock3, className: "bg-amber-500" },
};

/* ==========================================================================
   NODE
   ========================================================================== */

function FlowNodeComponent({ id, type, data, selected }: NodeProps) {
  const defs = React.useContext(NodeDefsContext);
  const errorNodes = React.useContext(ErrorNodesContext);

  const def = type ? defs[type] : undefined;
  const nodeData = data as FlowNodeData;

  const label = nodeData.label || def?.label || type || "Node";

  const outputs = def?.outputs?.length ? def.outputs : ["out"];

  const isTrigger = def?.trigger ?? false;
  const hasError = errorNodes.has(id);
  const multiOut = outputs.length > 1;

  const accent = resolveAccent(def?.category, isTrigger);
  const status = nodeData.executionStatus
    ? STATUS_BADGES[nodeData.executionStatus as string]
    : undefined;
  const isRunning = nodeData.executionStatus === "RUNNING";

  // Grow the node so multiple output ports have room
  const bodyMinHeight = Math.max(88, outputs.length * 30 + 20);

  const handleClass = cn(
    "!size-[10px] !rounded-full !border-2 !border-[#0a0a0a]",
    "!bg-[#6f6f78]",
    "!shadow-[0_0_0_1px_rgba(255,255,255,0.12)]",
    "transition-all duration-150",
    // larger invisible hit area, easier to connect
    "after:absolute after:-inset-2 after:content-['']",
    "hover:!scale-125 hover:!bg-brand-400",
    "group-hover:!bg-[#9a9aa5]",
    selected && "!bg-brand-400",
    hasError && "!bg-red-400",
  );

  return (
    <div className="group relative flex w-[132px] flex-col items-center">
      {/* ================================================================
          NODE BODY
          ================================================================ */}

      <div
        style={{ minHeight: bodyMinHeight }}
        className={cn(
          "relative flex w-[88px] shrink-0 items-center justify-center",
          "rounded-[10px]",
          isTrigger && "rounded-l-[28px]",
          "border border-white/[0.09]",
          "bg-gradient-to-b from-[#18181b] to-[#101012]",
          "shadow-[inset_0_1px_0_rgba(255,255,255,0.06),0_6px_20px_rgba(0,0,0,0.35)]",
          "transition-all duration-150",

          "group-hover:-translate-y-px",
          "group-hover:border-white/[0.18]",

          selected && [
            "-translate-y-px border-brand-400/70",
            "shadow-[0_0_0_3px_rgba(99,102,241,0.18),0_8px_24px_rgba(0,0,0,0.4)]",
          ],

          nodeData.executionStatus === "SUCCEEDED" &&
            "border-emerald-400/50 shadow-[0_0_0_3px_rgba(52,211,153,0.10),0_6px_20px_rgba(0,0,0,0.35)]",

          nodeData.executionStatus === "FAILED" &&
            "border-red-400/60 shadow-[0_0_0_3px_rgba(248,113,113,0.12),0_6px_20px_rgba(0,0,0,0.35)]",

          hasError &&
            "!border-red-500 shadow-[0_0_0_3px_rgba(239,68,68,0.15),0_6px_20px_rgba(0,0,0,0.35)]",
        )}
      >
        {/* Running pulse ring */}
        {isRunning && (
          <span
            aria-hidden="true"
            className={cn(
              "pointer-events-none absolute -inset-[4px] animate-pulse",
              "rounded-[14px] border-2 border-indigo-400/60",
              isTrigger && "rounded-l-[32px]",
            )}
          />
        )}

        {/* Input handle */}
        {!isTrigger && (
          <Handle
            type="target"
            position={Position.Left}
            className={handleClass}
          />
        )}

        {/* Trigger bolt, outside the left edge like n8n */}
        {isTrigger && (
          <span
            aria-hidden="true"
            className="absolute -left-[22px] top-1/2 -translate-y-1/2 text-amber-400 drop-shadow-[0_0_6px_rgba(245,158,11,0.5)]"
          >
            <Zap className="size-[14px] fill-current" />
          </span>
        )}

        <NodeIcon
          type={type}
          label={label}
          icon={def?.icon}
          accent={accent}
        />

        {/* Corner badge: validation error beats execution status */}
        {hasError ? (
          <span
            className="absolute -right-[7px] -top-[7px] flex size-[18px] items-center justify-center rounded-full border-2 border-[#0a0a0a] bg-red-500 text-[10px] font-bold text-white"
            aria-label="Validation error"
          >
            !
          </span>
        ) : status ? (
          <span
            className={cn(
              "absolute -right-[7px] -top-[7px] flex size-[18px] items-center justify-center",
              "rounded-full border-2 border-[#0a0a0a] text-white",
              status.className,
            )}
          >
            <status.icon
              className={cn("size-[10px]", status.spin && "animate-spin")}
              strokeWidth={3}
            />
          </span>
        ) : null}

        {/* Single output */}
        {!multiOut && (
          <Handle
            id={outputs[0]}
            type="source"
            position={Position.Right}
            className={handleClass}
          />
        )}

        {/* Multiple outputs: evenly spaced, labels outside */}
        {multiOut &&
          outputs.map((out, index) => (
            <Handle
              key={out}
              id={out}
              type="source"
              position={Position.Right}
              style={{ top: `${((index + 1) / (outputs.length + 1)) * 100}%` }}
              className={handleClass}
            >
              <span
                className={cn(
                  "pointer-events-none absolute left-[18px] top-1/2 -translate-y-1/2",
                  "rounded bg-[#0a0a0a] px-1 py-px",
                  "whitespace-nowrap text-[9px] font-medium leading-none",
                  "text-white/40 transition-colors group-hover:text-white/65",
                )}
              >
                {out}
              </span>
            </Handle>
          ))}
      </div>

      {/* ================================================================
          LABEL
          ================================================================ */}

      <div className="mt-2.5 w-[132px] text-center">
        <div
          className={cn(
            "truncate text-[13px] font-medium leading-5 text-white/85",
            selected && "text-white",
            hasError && "text-red-300",
          )}
          title={label}
        >
          {label}
        </div>

        <div
          className="mt-px truncate text-[9px] font-medium uppercase tracking-[0.12em] text-white/25"
          title={def?.category ?? type}
        >
          {def?.category ?? type}
        </div>
      </div>
    </div>
  );
}

export const FlowNode = React.memo(FlowNodeComponent);