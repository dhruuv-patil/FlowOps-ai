"use client";

import * as React from "react";
import { Handle, Position, type NodeProps } from "@xyflow/react";

import { cn } from "@/lib/utils";
import type { NodeDefinition } from "@/types";
import {
  ErrorNodesContext,
  resolveIcon,
  type FlowNodeData,
} from "@/components/app/builder/shared";

/**
 * The registry of node definitions, keyed by backend type. Provided by the page
 * so the node renderer can look up its icon, label, trigger flag and outputs
 * without re-fetching. React Flow only hands the renderer `id`/`type`/`data`.
 */
export const NodeDefsContext = React.createContext<
  Record<string, NodeDefinition>
>({});

/**
 * A single canvas node. One component renders every backend type — the type
 * key lives on `node.type` and we resolve its definition from context.
 *
 *  - Target handle on top, omitted for trigger nodes (they start a flow).
 *  - One source handle per `outputs` entry along the bottom, labelled when a
 *    node has more than one (e.g. condition → true/false).
 *  - Destructive ring when the node id is in the validation-error set.
 */
function FlowNodeComponent({ id, type, data, selected }: NodeProps) {
  const defs = React.useContext(NodeDefsContext);
  const errorNodes = React.useContext(ErrorNodesContext);

  const def = type ? defs[type] : undefined;
  const nodeData = data as FlowNodeData;

  const Icon = resolveIcon(def?.icon);
  const label = nodeData.label || def?.label || type || "Node";
  const outputs = def?.outputs?.length ? def.outputs : ["out"];
  const isTrigger = def?.trigger ?? false;
  const hasError = errorNodes.has(id);
  const multiOut = outputs.length > 1;

  return (
    <div
      className={cn(
        "relative min-w-[180px] rounded-xl border border-white/[0.10] bg-[#0c0c0c] text-white/80 shadow-card transition-all",
        selected
          ? "border-white/[0.20] ring-1 ring-white/[0.10]"
          : "hover:border-white/[0.10]",
        hasError && "border-red-500/60 ring-1 ring-red-500/30",
      )}
    >
      {/* Input — triggers have no upstream, so no target handle. */}
      {!isTrigger && (
        <Handle
          type="target"
          position={Position.Top}
          className="!size-2.5 !border-2 !border-background !bg-brand-500"
        />
      )}

      <div className="flex items-center gap-2.5 px-3 py-2.5">
        <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-brand-500/15 text-brand-500">
          <Icon className="size-4" />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium">{label}</span>
          <span className="block truncate font-mono text-[10px] uppercase tracking-wide text-white/40">
            {def?.category ?? type}
          </span>
        </span>
      </div>

      {/* Outputs. Single output → one centered handle; multiple → labelled and
          spread across the bottom edge. */}
      {multiOut ? (
        <div className="flex items-stretch border-t border-white/[0.075]">
          {outputs.map((out, i) => (
            <div
              key={out}
              className="relative flex-1 px-2 py-1 text-center text-[10px] font-medium text-white/40"
            >
              {out}
              <Handle
                id={out}
                type="source"
                position={Position.Bottom}
                style={{ left: `${((i + 0.5) / outputs.length) * 100}%` }}
                className="!size-2.5 !border-2 !border-background !bg-brand-500"
              />
            </div>
          ))}
        </div>
      ) : (
        <Handle
          id={outputs[0]}
          type="source"
          position={Position.Bottom}
          className="!size-2.5 !border-2 !border-background !bg-brand-500"
        />
      )}
    </div>
  );
}

export const FlowNode = React.memo(FlowNodeComponent);
