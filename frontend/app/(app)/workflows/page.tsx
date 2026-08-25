"use client";

import Link from "next/link";
import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Plus, Search, Workflow as WorkflowIcon } from "lucide-react";

import { fetchWorkflows } from "@/lib/api";
import type { WorkflowStatus } from "@/types";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { CreateWorkflowDialog } from "@/components/app/create-workflow-dialog";

const STATUS_FILTERS: { label: string; value: WorkflowStatus | "ALL" }[] = [
  { label: "Active", value: "ALL" },
  { label: "Draft", value: "DRAFT" },
  { label: "Published", value: "PUBLISHED" },
  { label: "Archived", value: "ARCHIVED" },
];

function statusVariant(status: WorkflowStatus) {
  switch (status) {
    case "PUBLISHED":
      return "success" as const;
    case "DRAFT":
      return "secondary" as const;
    default:
      return "outline" as const;
  }
}

export default function WorkflowsPage() {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<WorkflowStatus | "ALL">("ALL");
  const [createOpen, setCreateOpen] = useState(false);

  const query = useQuery({
    queryKey: ["workflows", { search, status }],
    queryFn: () =>
      fetchWorkflows({
        search: search.trim() || undefined,
        status: status === "ALL" ? undefined : status,
      }),
    placeholderData: keepPreviousData,
  });

  const workflows = query.data?.workflows ?? [];

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Workflows</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Build, validate, and publish automation workflows.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="size-4" /> New workflow
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 sm:max-w-xs">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search workflows…"
            className="pl-9"
          />
        </div>
        <div className="flex gap-1">
          {STATUS_FILTERS.map((f) => (
            <button
              key={f.value}
              type="button"
              onClick={() => setStatus(f.value)}
              className={cn(
                "rounded-md px-3 py-1.5 text-sm transition-colors",
                status === f.value
                  ? "bg-accent font-medium text-accent-foreground"
                  : "text-muted-foreground hover:bg-accent/60 hover:text-foreground",
              )}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {query.isPending ? (
        <div className="space-y-2">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      ) : query.isError ? (
        <p className="text-sm text-destructive">
          Could not load workflows. Please try again.
        </p>
      ) : workflows.length === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-border/60 py-16 text-center">
          <WorkflowIcon className="size-10 text-muted-foreground/40" />
          <div>
            <p className="font-medium">
              {search || status !== "ALL"
                ? "No workflows match your filters."
                : "No workflows yet."}
            </p>
            <p className="text-sm text-muted-foreground">
              Create your first workflow to get started.
            </p>
          </div>
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="size-4" /> New workflow
          </Button>
        </div>
      ) : (
        <ul className="divide-y divide-border/60 overflow-hidden rounded-lg border border-border/60">
          {workflows.map((w) => (
            <li key={w.id}>
              <Link
                href={`/workflows/${w.id}`}
                className="flex items-center gap-4 px-4 py-3 transition-colors hover:bg-accent/40"
              >
                <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
                  <WorkflowIcon className="size-4" />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="flex items-center gap-2">
                    <span className="truncate font-medium">{w.name}</span>
                    <Badge variant={statusVariant(w.status)}>
                      {w.status.toLowerCase()}
                    </Badge>
                  </span>
                  <span className="block truncate text-sm text-muted-foreground">
                    {w.description || "No description"}
                  </span>
                </span>
                <span className="hidden shrink-0 text-right text-xs text-muted-foreground sm:block">
                  <span className="block">
                    {w.nodeCount} {w.nodeCount === 1 ? "node" : "nodes"}
                  </span>
                  {w.latestVersion != null && (
                    <span className="block">v{w.latestVersion}</span>
                  )}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}

      <CreateWorkflowDialog open={createOpen} onOpenChange={setCreateOpen} />
    </div>
  );
}
