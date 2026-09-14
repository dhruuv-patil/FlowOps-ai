"use client";

import * as React from "react";
import { ChevronDown, PanelLeftClose, PanelLeftOpen } from "lucide-react";

import { cn } from "@/lib/utils";
import type { NodeDefinition } from "@/types";
import { Button } from "@/components/ui/button";
import {
  DND_MIME,
  groupByCategory,
  resolveIcon,
} from "@/components/app/builder/shared";

interface NodePaletteProps {
  defs: NodeDefinition[];
  collapsed: boolean;
  onToggle: () => void;
  /** Click-to-add fallback (drops the node at the canvas center). */
  onAdd: (type: string) => void;
  isLoading?: boolean;
  isError?: boolean;
}

/**
 * Left rail listing the node registry, grouped by category. Every entry is both
 * draggable (HTML5 DnD → canvas) and clickable (adds at center) so the builder
 * works with or without a drag gesture.
 */
export function NodePalette({
  defs,
  collapsed,
  onToggle,
  onAdd,
  isLoading,
  isError,
}: NodePaletteProps) {
  const groups = React.useMemo(() => groupByCategory(defs), [defs]);

  if (collapsed) {
    return (
      <div className="flex h-full w-12 shrink-0 flex-col items-center border-r border-white/[0.075] bg-[#0a0a0a] py-3">
        <Button
          variant="ghost"
          size="icon"
          onClick={onToggle}
          aria-label="Expand node palette"
          title="Expand node palette"
        >
          <PanelLeftOpen className="size-4" />
        </Button>
      </div>
    );
  }

  return (
    <div className="flex h-full w-64 shrink-0 flex-col border-r border-white/[0.075] bg-[#0a0a0a]">
      <div className="flex h-12 shrink-0 items-center justify-between border-b border-white/[0.075] px-3">
        <span className="text-sm font-semibold">Nodes</span>
        <Button
          variant="ghost"
          size="icon"
          onClick={onToggle}
          aria-label="Collapse node palette"
          title="Collapse node palette"
        >
          <PanelLeftClose className="size-4" />
        </Button>
      </div>

      <div className="flex-1 overflow-y-auto p-3">
        {isLoading ? (
          <p className="px-1 text-sm text-muted-foreground">Loading nodes…</p>
        ) : isError ? (
          <p className="px-1 text-sm text-destructive">
            Could not load node types.
          </p>
        ) : groups.length === 0 ? (
          <p className="px-1 text-sm text-muted-foreground">
            No node types available.
          </p>
        ) : (
          <div className="space-y-4">
            {groups.map((group) => (
              <PaletteGroup
                key={group.category}
                label={group.category}
                defs={group.defs}
                onAdd={onAdd}
              />
            ))}
          </div>
        )}
      </div>

      <p className="border-t border-white/[0.075] px-3 py-2 text-[11px] text-white/40">
        Drag onto the canvas, or click to add.
      </p>
    </div>
  );
}

function PaletteGroup({
  label,
  defs,
  onAdd,
}: {
  label: string;
  defs: NodeDefinition[];
  onAdd: (type: string) => void;
}) {
  const [open, setOpen] = React.useState(true);

  return (
    <div>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="flex w-full items-center gap-1 px-1 pb-1 font-mono text-[10px] uppercase tracking-widest text-white/40"
      >
        <ChevronDown
          className={cn("size-3 transition-transform", !open && "-rotate-90")}
        />
        {label}
      </button>
      {open && (
        <ul className="space-y-1">
          {defs.map((def) => (
            <PaletteItem key={def.type} def={def} onAdd={onAdd} />
          ))}
        </ul>
      )}
    </div>
  );
}

function PaletteItem({
  def,
  onAdd,
}: {
  def: NodeDefinition;
  onAdd: (type: string) => void;
}) {
  const Icon = resolveIcon(def.icon);

  return (
    <li>
      <button
        type="button"
        draggable
        onDragStart={(e) => {
          e.dataTransfer.setData(DND_MIME, def.type);
          e.dataTransfer.effectAllowed = "move";
        }}
        onClick={() => onAdd(def.type)}
        title={def.description}
        className="flex w-full items-center gap-2.5 rounded-md border border-transparent px-2 py-2 text-left transition-colors hover:border-white/[0.12] hover:bg-white/[0.06] active:cursor-grabbing"
      >
        <span className="flex size-7 shrink-0 items-center justify-center rounded-md bg-primary/15 text-primary">
          <Icon className="size-4" />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium">
            {def.label}
          </span>
          <span className="block truncate text-xs text-white/40">
            {def.description}
          </span>
        </span>
      </button>
    </li>
  );
}
