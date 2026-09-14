"use client";

import * as React from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { PanelRightClose } from "lucide-react";

import { cn } from "@/lib/utils";
import { fetchAiAgents } from "@/lib/api";
import type { ConfigField, NodeDefinition } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { resolveIcon, type FlowNode } from "@/components/app/builder/shared";

interface ConfigPanelProps {
  node: FlowNode | null;
  def: NodeDefinition | undefined;
  /** Node ids reachable upstream — offered as {{nodeId.field}} references. */
  upstreamIds: string[];
  onClose: () => void;
  onLabelChange: (label: string) => void;
  onConfigChange: (key: string, value: unknown) => void;
}

/**
 * Right rail.
 *
 * Important layout rules:
 * - The panel has a bounded width so it cannot push the canvas outside the viewport.
 * - `min-w-0` allows the panel contents to shrink correctly inside flex/grid layouts.
 * - `overflow-hidden` prevents long labels/controls from expanding the rail.
 * - The inner content owns vertical scrolling.
 */
export function ConfigPanel({
  node,
  def,
  upstreamIds,
  onClose,
  onLabelChange,
  onConfigChange,
}: ConfigPanelProps) {
  const panelClassName =
    "flex h-full w-[300px] max-w-[300px] min-w-0 shrink-0 flex-col overflow-hidden border-l border-white/[0.075] bg-[#0a0a0a]";

  if (!node) {
    return (
      <aside className={panelClassName} aria-label="Inspector">
        <div className="flex h-12 min-w-0 shrink-0 items-center border-b border-white/[0.075] px-3">
          <span className="truncate text-sm font-semibold">Inspector</span>
        </div>

        <div className="flex min-w-0 flex-1 items-center justify-center overflow-hidden p-6 text-center text-sm text-white/40">
          <p className="max-w-[220px]">
            Select a node to edit its configuration.
          </p>
        </div>
      </aside>
    );
  }

  const Icon = resolveIcon(def?.icon);
  const config = node.data.config ?? {};

  return (
    <aside className={panelClassName} aria-label="Inspector">
      <div className="flex h-12 min-w-0 shrink-0 items-center gap-2 border-b border-white/[0.075] px-3">
        <span className="flex size-6 shrink-0 items-center justify-center rounded bg-primary/15 text-primary">
          <Icon className="size-3.5" />
        </span>

        <span className="min-w-0 flex-1 truncate text-sm font-semibold">
          {def?.label ?? node.type}
        </span>

        <Button
          variant="ghost"
          size="icon"
          className="shrink-0"
          onClick={onClose}
          aria-label="Close inspector"
        >
          <PanelRightClose className="size-4" />
        </Button>
      </div>

      <div className="min-h-0 min-w-0 flex-1 space-y-4 overflow-x-hidden overflow-y-auto p-4">
        {def?.description && (
          <p className="break-words text-xs leading-relaxed text-muted-foreground">
            {def.description}
          </p>
        )}

        {/* Display label — always editable, independent of config fields. */}
        <div className="min-w-0 space-y-1.5">
          <Label htmlFor="node-label" className="text-white/60">
            Label
          </Label>

          <Input
            id="node-label"
            value={node.data.label ?? ""}
            placeholder={def?.label ?? "Node label"}
            className="min-w-0"
            onChange={(e) => onLabelChange(e.target.value)}
          />
        </div>

        {def && def.configFields.length > 0 && (
          <div className="min-w-0 space-y-4 border-t border-white/[0.075] pt-4">
            {def.configFields.map((field) => (
              <FieldControl
                key={field.key}
                field={field}
                value={config[field.key]}
                onChange={(v) => onConfigChange(field.key, v)}
              />
            ))}
          </div>
        )}

        <VariableHint upstreamIds={upstreamIds} />
      </div>

      <p className="min-w-0 shrink-0 truncate border-t border-white/[0.075] px-4 py-2 font-mono text-[10px] text-white/40">
        id: {node.id}
      </p>
    </aside>
  );
}

/** One config field. The widget is chosen from `field.type`. */
function FieldControl({
  field,
  value,
  onChange,
}: {
  field: ConfigField;
  value: unknown;
  onChange: (value: unknown) => void;
}) {
  const controlId = `cfg-${field.key}`;

  return (
    <div className="min-w-0 space-y-1.5">
      {field.type !== "boolean" && (
        <Label
          htmlFor={controlId}
          className="flex min-w-0 items-center gap-1 text-white/60"
        >
          <span className="min-w-0 truncate">{field.label}</span>

          {field.required && (
            <span className="shrink-0 text-destructive">*</span>
          )}
        </Label>
      )}

      <div className="min-w-0">
        <FieldWidget
          controlId={controlId}
          field={field}
          value={value}
          onChange={onChange}
        />
      </div>

      {field.help && (
        <p className="break-words text-[11px] leading-relaxed text-muted-foreground">
          {field.help}
        </p>
      )}
    </div>
  );
}

function FieldWidget({
  controlId,
  field,
  value,
  onChange,
}: {
  controlId: string;
  field: ConfigField;
  value: unknown;
  onChange: (value: unknown) => void;
}) {
  switch (field.type) {
    case "text":
    case "code":
      return (
        <Textarea
          id={controlId}
          value={typeof value === "string" ? value : ""}
          placeholder={field.placeholder ?? undefined}
          rows={field.type === "code" ? 6 : 3}
          className={cn(
            "min-w-0 max-w-full resize-y",
            field.type === "code" && "font-mono text-xs",
          )}
          onChange={(e) => onChange(e.target.value)}
        />
      );

    case "number":
      return (
        <Input
          id={controlId}
          type="number"
          value={
            typeof value === "number" || typeof value === "string"
              ? String(value)
              : ""
          }
          placeholder={field.placeholder ?? undefined}
          className="min-w-0 max-w-full"
          onChange={(e) =>
            onChange(e.target.value === "" ? null : Number(e.target.value))
          }
        />
      );

    case "boolean":
      return (
        <label
          htmlFor={controlId}
          className="flex min-w-0 cursor-pointer items-center gap-2 text-sm font-medium"
        >
          <input
            id={controlId}
            type="checkbox"
            checked={value === true}
            onChange={(e) => onChange(e.target.checked)}
            className="size-4 shrink-0 rounded border-input accent-primary"
          />

          <span className="min-w-0 break-words">{field.label}</span>

          {field.required && (
            <span className="shrink-0 text-destructive">*</span>
          )}
        </label>
      );

    case "select":
      return (
        <select
          id={controlId}
          value={typeof value === "string" ? value : ""}
          onChange={(e) => onChange(e.target.value || null)}
          className="flex h-9 w-full min-w-0 max-w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
        >
          <option value="">— Select —</option>

          {field.options.map((opt) => (
            <option key={opt} value={opt}>
              {opt}
            </option>
          ))}
        </select>
      );

    case "json":
      return (
        <JsonField
          controlId={controlId}
          value={value}
          placeholder={field.placeholder}
          onChange={onChange}
        />
      );

    case "agent":
      return (
        <AgentSelect
          controlId={controlId}
          value={value}
          onChange={onChange}
        />
      );

    case "string":
    default:
      return (
        <Input
          id={controlId}
          value={typeof value === "string" ? value : ""}
          placeholder={field.placeholder ?? undefined}
          className="min-w-0 max-w-full"
          onChange={(e) => onChange(e.target.value)}
        />
      );
  }
}

/**
 * Dynamic picker for the `ai_agent` node's saved-agent reference.
 * Loads the org's agents and offers them as options.
 */
function AgentSelect({
  controlId,
  value,
  onChange,
}: {
  controlId: string;
  value: unknown;
  onChange: (value: unknown) => void;
}) {
  const query = useQuery({
    queryKey: ["ai-agents"],
    queryFn: fetchAiAgents,
  });

  const agents = query.data?.agents ?? [];
  const current = typeof value === "string" ? value : "";

  if (query.isError) {
    return (
      <p className="break-words text-[11px] leading-relaxed text-destructive">
        Could not load agents. The inline instructions below will be used.
      </p>
    );
  }

  if (!query.isPending && agents.length === 0) {
    return (
      <p className="break-words text-[11px] leading-relaxed text-muted-foreground">
        No saved agents yet.{" "}
        <Link
          href="/agents"
          className="text-primary underline-offset-2 hover:underline"
        >
          Create one
        </Link>
        , or use the inline instructions below.
      </p>
    );
  }

  return (
    <select
      id={controlId}
      value={current}
      disabled={query.isPending}
      onChange={(e) => onChange(e.target.value || null)}
      className="flex h-9 w-full min-w-0 max-w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
    >
      <option value="">— None (use inline instructions) —</option>

      {/* Keep a dangling reference visible rather than silently showing "None". */}
      {current && !agents.some((a) => a.id === current) && (
        <option value={current}>Unavailable agent</option>
      )}

      {agents.map((a) => (
        <option key={a.id} value={a.id}>
          {a.name}
        </option>
      ))}
    </select>
  );
}

/**
 * JSON editor.
 *
 * Keeps raw text locally while typing and only commits the parsed value
 * on blur. Invalid JSON is surfaced inline and is not written to config.
 */
function JsonField({
  controlId,
  value,
  placeholder,
  onChange,
}: {
  controlId: string;
  value: unknown;
  placeholder: string | null;
  onChange: (value: unknown) => void;
}) {
  const [text, setText] = React.useState<string>(() =>
    value === undefined || value === null
      ? ""
      : JSON.stringify(value, null, 2),
  );

  const [error, setError] = React.useState<string | null>(null);

  // Re-sync when a different node/value is selected.
  const nodeKey = controlId;

  React.useEffect(() => {
    setText(
      value === undefined || value === null
        ? ""
        : JSON.stringify(value, null, 2),
    );

    setError(null);

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nodeKey]);

  function commit() {
    if (text.trim() === "") {
      setError(null);
      onChange(null);
      return;
    }

    try {
      const parsed = JSON.parse(text);
      setError(null);
      onChange(parsed);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Invalid JSON");
    }
  }

  return (
    <div className="min-w-0">
      <Textarea
        id={controlId}
        value={text}
        rows={5}
        aria-invalid={error ? true : undefined}
        placeholder={placeholder ?? '{ "key": "value" }'}
        className="min-w-0 max-w-full resize-y font-mono text-xs"
        onChange={(e) => setText(e.target.value)}
        onBlur={commit}
      />

      {error && (
        <p className="mt-1 break-words text-[11px] text-destructive">
          {error}
        </p>
      )}
    </div>
  );
}

/** Small helper listing upstream node ids the user can reference in templates. */
function VariableHint({ upstreamIds }: { upstreamIds: string[] }) {
  if (upstreamIds.length === 0) return null;

  return (
    <div className="min-w-0 space-y-1.5 border-t border-white/[0.075] pt-4">
      <Label className="text-white/60">Available variables</Label>

      <p className="break-words text-[11px] leading-relaxed text-muted-foreground">
        Reference upstream output with{" "}
        <code className="break-all rounded bg-muted px-1 font-mono">
          {"{{nodeId.field}}"}
        </code>
        .
      </p>

      <div className="flex min-w-0 flex-wrap gap-1 overflow-hidden">
        {upstreamIds.map((id) => (
          <Badge
            key={id}
            variant="outline"
            className="max-w-full break-all font-mono"
          >
            {`{{${id}}}`}
          </Badge>
        ))}
      </div>
    </div>
  );
}