"use client";

import * as React from "react";
import { PanelRightClose } from "lucide-react";

import { cn } from "@/lib/utils";
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
 * Right rail. When a node is selected we render a form derived from its node
 * type's `configFields`; every edit writes straight back through the callbacks
 * so the canvas graph is the single source of truth (no local shadow copy that
 * could drift on save).
 */
export function ConfigPanel({
  node,
  def,
  upstreamIds,
  onClose,
  onLabelChange,
  onConfigChange,
}: ConfigPanelProps) {
  if (!node) {
    return (
      <div className="flex h-full w-80 shrink-0 flex-col border-l border-border/60 bg-card/40">
        <div className="flex h-12 shrink-0 items-center border-b border-border/60 px-3">
          <span className="text-sm font-semibold">Inspector</span>
        </div>
        <div className="flex flex-1 items-center justify-center p-6 text-center text-sm text-muted-foreground">
          Select a node to edit its configuration.
        </div>
      </div>
    );
  }

  const Icon = resolveIcon(def?.icon);
  const config = node.data.config ?? {};

  return (
    <div className="flex h-full w-80 shrink-0 flex-col border-l border-border/60 bg-card/40">
      <div className="flex h-12 shrink-0 items-center gap-2 border-b border-border/60 px-3">
        <span className="flex size-6 items-center justify-center rounded bg-primary/15 text-primary">
          <Icon className="size-3.5" />
        </span>
        <span className="min-w-0 flex-1 truncate text-sm font-semibold">
          {def?.label ?? node.type}
        </span>
        <Button
          variant="ghost"
          size="icon"
          onClick={onClose}
          aria-label="Close inspector"
        >
          <PanelRightClose className="size-4" />
        </Button>
      </div>

      <div className="flex-1 space-y-4 overflow-y-auto p-4">
        {def?.description && (
          <p className="text-xs text-muted-foreground">{def.description}</p>
        )}

        {/* Display label — always editable, independent of config fields. */}
        <div className="space-y-1.5">
          <Label htmlFor="node-label">Label</Label>
          <Input
            id="node-label"
            value={node.data.label ?? ""}
            placeholder={def?.label ?? "Node label"}
            onChange={(e) => onLabelChange(e.target.value)}
          />
        </div>

        {def && def.configFields.length > 0 && (
          <div className="space-y-4 border-t border-border/60 pt-4">
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

      <p className="border-t border-border/60 px-4 py-2 font-mono text-[10px] text-muted-foreground">
        id: {node.id}
      </p>
    </div>
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
    <div className="space-y-1.5">
      {field.type !== "boolean" && (
        <Label htmlFor={controlId} className="flex items-center gap-1">
          {field.label}
          {field.required && <span className="text-destructive">*</span>}
        </Label>
      )}

      <FieldWidget
        controlId={controlId}
        field={field}
        value={value}
        onChange={onChange}
      />

      {field.help && (
        <p className="text-[11px] text-muted-foreground">{field.help}</p>
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
          className={cn(field.type === "code" && "font-mono text-xs")}
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
          onChange={(e) =>
            onChange(e.target.value === "" ? null : Number(e.target.value))
          }
        />
      );

    case "boolean":
      return (
        <label
          htmlFor={controlId}
          className="flex cursor-pointer items-center gap-2 text-sm font-medium"
        >
          <input
            id={controlId}
            type="checkbox"
            checked={value === true}
            onChange={(e) => onChange(e.target.checked)}
            className="size-4 rounded border-input accent-primary"
          />
          {field.label}
          {field.required && <span className="text-destructive">*</span>}
        </label>
      );

    case "select":
      return (
        <select
          id={controlId}
          value={typeof value === "string" ? value : ""}
          onChange={(e) => onChange(e.target.value || null)}
          className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
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

    case "string":
    default:
      return (
        <Input
          id={controlId}
          value={typeof value === "string" ? value : ""}
          placeholder={field.placeholder ?? undefined}
          onChange={(e) => onChange(e.target.value)}
        />
      );
  }
}

/**
 * JSON editor: keeps raw text locally while typing and only commits the parsed
 * value on blur. Invalid JSON is surfaced inline and NOT written to config, so
 * we never persist an unparseable string where an object is expected.
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
    value === undefined || value === null ? "" : JSON.stringify(value, null, 2),
  );
  const [error, setError] = React.useState<string | null>(null);

  // Re-sync when a different node (and thus a different value) is selected.
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
    <>
      <Textarea
        id={controlId}
        value={text}
        rows={5}
        aria-invalid={error ? true : undefined}
        placeholder={placeholder ?? '{ "key": "value" }'}
        className="font-mono text-xs"
        onChange={(e) => setText(e.target.value)}
        onBlur={commit}
      />
      {error && <p className="text-[11px] text-destructive">{error}</p>}
    </>
  );
}

/** Small helper listing upstream node ids the user can reference in templates. */
function VariableHint({ upstreamIds }: { upstreamIds: string[] }) {
  if (upstreamIds.length === 0) return null;
  return (
    <div className="space-y-1.5 border-t border-border/60 pt-4">
      <Label>Available variables</Label>
      <p className="text-[11px] text-muted-foreground">
        Reference upstream output with{" "}
        <code className="rounded bg-muted px-1 font-mono">
          {"{{nodeId.field}}"}
        </code>
        .
      </p>
      <div className="flex flex-wrap gap-1">
        {upstreamIds.map((id) => (
          <Badge key={id} variant="outline" className="font-mono">
            {`{{${id}}}`}
          </Badge>
        ))}
      </div>
    </div>
  );
}
