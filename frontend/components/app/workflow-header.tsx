"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, CheckCircle2, Clock3, Play, Sparkles, Webhook } from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { runWorkflow, updateWorkflow, getErrorMessage } from "@/lib/api";
import type { WorkflowDetail, WorkflowStatus } from "@/types";

import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { SegmentedControl } from "@/components/ui/segmented-control";

interface WorkflowHeaderProps {
  workflow: WorkflowDetail;
  version: number | null;
  dirty: boolean;
  canRun: boolean;
  runPending: boolean;
  namePending: boolean;
  onRename: (name: string) => void;
  onRun: () => void;
  onGenerate: () => void;
  onWebhook: () => void;
  hasWebhookTrigger: boolean;
  activeView: "editor" | "executions" | "anomalies";
  onViewChange: (view: "editor" | "executions" | "anomalies") => void;
}

const statusVariant: Record<
  WorkflowStatus,
  "secondary" | "success" | "outline"
> = {
  DRAFT: "secondary",
  PUBLISHED: "success",
  ARCHIVED: "outline",
};

export function WorkflowHeader({
  workflow,
  version,
  dirty,
  canRun,
  runPending,
  namePending,
  onRename,
  onRun,
  onGenerate,
  onWebhook,
  hasWebhookTrigger,
  activeView,
  onViewChange,
}: WorkflowHeaderProps) {
  const router = useRouter();
  const queryClient = useQueryClient();

  const runMutation = useMutation({
    mutationFn: () => runWorkflow(workflow.id),
    onSuccess: (execution) => {
      toast.success("Run started.");
      router.push(`/executions/${execution.id}`);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const renameMutation = useMutation({
    mutationFn: (name: string) =>
      updateWorkflow(workflow.id, {
        name,
        description: workflow.description ?? undefined,
      }),
    onSuccess: (detail) => {
      queryClient.setQueryData(["workflow", workflow.id], detail);
      toast.success("Workflow renamed.");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const views: { value: "editor" | "executions" | "anomalies"; label: string }[] = [
    { value: "editor", label: "Editor" },
    { value: "executions", label: "Executions" },
    { value: "anomalies", label: "Anomalies" },
  ];

  return (
    <TooltipProvider delayDuration={300}>
      <header className="flex h-16 shrink-0 items-center gap-4 border-b border-white/[0.07] bg-[#0a0a0a] px-4 lg:px-6">
        {/* LEFT: Workflow identity */}
        <div className="flex items-center gap-2 min-w-0 flex-1">
          <NameField
            name={workflow.name}
            pending={namePending || renameMutation.isPending}
            onRename={(n) => renameMutation.mutate(n)}
            className="w-full lg:w-72"
          />

          <Badge variant={statusVariant[workflow.status]}>
            {workflow.status}
            {workflow.status === "PUBLISHED" && version != null && (
              <span className="ml-1 opacity-80">v{version}</span>
            )}
          </Badge>

          {/* Unsaved changes indicator */}
          <span
            className={cn(
              "flex items-center gap-1.5 text-xs text-muted-foreground transition-opacity",
              dirty ? "opacity-100" : "opacity-0",
            )}
            aria-live="polite"
          >
            <span className="size-2 rounded-full bg-amber-500" />
            Unsaved changes
          </span>
        </div>

        {/* CENTER: Segmented Control - perfectly centered */}
        <div className="flex items-center justify-center flex-1">
          <SegmentedControl
            tabs={views}
            value={activeView}
            onValueChange={onViewChange}
          />
        </div>

        {/* RIGHT: Actions */}
        <div className="flex items-center gap-2 shrink-0 flex-1 justify-end">
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="outline"
                size="sm"
                onClick={onGenerate}
                className="text-primary"
              >
                <Sparkles className="size-4" />
                Create with AI
              </Button>
            </TooltipTrigger>
            <TooltipContent>Generate workflow with AI</TooltipContent>
          </Tooltip>

          {hasWebhookTrigger && (
            <Tooltip>
              <TooltipTrigger asChild>
                <Button variant="outline" size="sm" onClick={onWebhook}>
                  <Webhook className="size-4" />
                  Webhook
                </Button>
              </TooltipTrigger>
              <TooltipContent>Manage inbound webhook</TooltipContent>
            </Tooltip>
          )}

          <Tooltip>
            <TooltipTrigger asChild>
              <span tabIndex={canRun ? -1 : 0}>
                <Button
                  variant="default"
                  size="sm"
                  onClick={runMutation.isPending ? undefined : onRun}
                  disabled={!canRun || runMutation.isPending}
                >
                  <Play className="size-4" />
                  {runMutation.isPending ? "Starting…" : "Run"}
                </Button>
              </span>
            </TooltipTrigger>
            <TooltipContent>
              {canRun
                ? "Run the latest published version"
                : "Publish a version to run it"}
            </TooltipContent>
          </Tooltip>
        </div>
      </header>
    </TooltipProvider>
  );
}

function NameField({
  name,
  pending,
  onRename,
  className = "",
}: {
  name: string;
  pending: boolean;
  onRename: (name: string) => void;
  className?: string;
}) {
  const [draft, setDraft] = React.useState(name);
  const [editing, setEditing] = React.useState(false);

  React.useEffect(() => {
    if (!editing) setDraft(name);
  }, [name, editing]);

  function commit() {
    setEditing(false);
    const trimmed = draft.trim();
    if (trimmed && trimmed !== name) {
      onRename(trimmed);
    } else {
      setDraft(name);
    }
  }

  return (
    <Input
      value={draft}
      disabled={pending}
      onFocus={() => setEditing(true)}
      onChange={(e) => setDraft(e.target.value)}
      onBlur={commit}
      onKeyDown={(e) => {
        if (e.key === "Enter") e.currentTarget.blur();
        if (e.key === "Escape") {
          setDraft(name);
          setEditing(false);
          e.currentTarget.blur();
        }
      }}
      aria-label="Workflow name"
      className={cn(
        "h-8 border-transparent bg-transparent px-2 text-sm font-semibold shadow-none hover:border-input focus-visible:border-input",
        "truncate",
        className,
      )}
    />
  );
}