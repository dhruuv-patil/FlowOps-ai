"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import {
  ArrowUpRight,
  Calendar,
  Loader2,
  MoreHorizontal,
  Plus,
  Search,
  Trash2,
  Workflow,
} from "lucide-react";

import { deleteWorkflow, fetchWorkflows } from "@/lib/api";
import { cn } from "@/lib/utils";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Card,
  CardContent,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { CreateWorkflowDialog } from "@/components/app/create-workflow-dialog";

import {
  Reveal,
  StaggerGroup,
  StaggerItem,
  HoverLift,
} from "@/components/motion/motion-primitives";

type StatusFilter =
  | "ALL"
  | "DRAFT"
  | "PUBLISHED";

type WorkflowItem = {
  id: string;
  name: string;
  description?: string | null;
  status: string;
  updatedAt?: string | null;
};

export default function WorkflowsPage() {
  const queryClient = useQueryClient();

  const [search, setSearch] =
    useState("");

  const [filter, setFilter] =
    useState<StatusFilter>("ALL");

  const [createOpen, setCreateOpen] =
    useState(false);

  const [toDelete, setToDelete] =
    useState<WorkflowItem | null>(null);

  const workflows = useQuery({
    queryKey: ["workflows"],
    queryFn: () => fetchWorkflows(),
  });

  const allWorkflows =
    workflows.data?.workflows ?? [];

  const removeMutation = useMutation({
    mutationFn: (id: string) =>
      deleteWorkflow(id),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["workflows"],
      });
      setToDelete(null);
    },
  });

  const items = useMemo(() => {
    const query =
      search.trim().toLowerCase();

    return allWorkflows.filter(
      workflow => {
        if (
          filter !== "ALL" &&
          workflow.status !== filter
        ) {
          return false;
        }

        if (
          query &&
          !workflow.name
            .toLowerCase()
            .includes(query)
        ) {
          return false;
        }

        return true;
      },
    );
  }, [
    allWorkflows,
    filter,
    search,
  ]);

  const publishedCount =
    allWorkflows.filter(
      workflow =>
        workflow.status ===
        "PUBLISHED",
    ).length;

  const draftCount =
    allWorkflows.filter(
      workflow =>
        workflow.status === "DRAFT",
    ).length;

  const hasFilters =
    search.trim().length > 0 ||
    filter !== "ALL";

  return (
    <main className="min-h-full bg-black text-white">
      <div className="mx-auto max-w-7xl px-4 pb-10 pt-5 sm:px-6 lg:px-8">

        {/* ================================================================
            HEADER
        ================================================================ */}

        <Reveal>
          <section className="rounded-2xl border border-white/[0.07] bg-[#0D0D10] px-5 py-5 sm:px-6">
            <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">

              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <div className="flex size-7 items-center justify-center rounded-lg border border-white/[0.07] bg-[#111114]">
                    <Workflow className="size-3.5 text-white/50" />
                  </div>

                  <span className="font-mono text-[10px] uppercase tracking-[0.14em] text-white/30">
                    Workflow Control Plane
                  </span>
                </div>

                <h1 className="mt-2.5 text-[24px] font-semibold tracking-[-0.035em] text-white">
                  Workflows
                </h1>

                <p className="mt-1 max-w-xl text-sm leading-5 text-white/38">
                  Build, version, execute and
                  monitor your automation
                  workflows from one place.
                </p>
              </div>

              <Button
                onClick={() =>
                  setCreateOpen(true)
                }
                className="h-9 shrink-0 gap-2 self-start rounded-lg bg-white px-4 text-sm font-medium text-black shadow-none hover:bg-white/90"
              >
                <Plus className="size-4" />
                New Workflow
              </Button>
            </div>

            {/* ============================================================
                METRICS
            ============================================================ */}

            <div className="mt-6 grid grid-cols-2 overflow-hidden rounded-xl border border-white/[0.06] bg-[#0A0A0C] sm:grid-cols-4">
              <OverviewMetric
                label="Total workflows"
                value={
                  allWorkflows.length
                }
                detail="All workflows"
              />

              <OverviewMetric
                label="Published"
                value={
                  publishedCount
                }
                tone={
                  publishedCount > 0
                    ? "positive"
                    : "neutral"
                }
                detail="Active"
              />

              <OverviewMetric
                label="Drafts"
                value={draftCount}
                tone={
                  draftCount > 0
                    ? "warning"
                    : "neutral"
                }
                detail="Needs attention"
              />

              <OverviewMetric
                label="Currently showing"
                value={items.length}
                detail={
                  hasFilters
                    ? "Filtered results"
                    : "All workflows"
                }
              />
            </div>
          </section>
        </Reveal>

        {/* ================================================================
            SEARCH / FILTER
        ================================================================ */}

        <Reveal
          delay={0.04}
          distance={8}
        >
          <div className="mt-5 flex flex-col gap-2.5 sm:flex-row sm:items-center">

            <div className="relative min-w-0 flex-1">
              <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-white/22" />

              <Input
                value={search}
                onChange={event =>
                  setSearch(
                    event.target.value,
                  )
                }
                placeholder="Search workflows..."
                className="h-10 rounded-lg border-white/[0.075] bg-[#0D0D10] pl-10 text-sm text-white/80 placeholder:text-white/22 shadow-none focus:border-indigo-400/25 focus:ring-0"
              />

              {search && (
                <button
                  type="button"
                  onClick={() =>
                    setSearch("")
                  }
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[10px] text-white/25 hover:text-white/60"
                >
                  Clear
                </button>
              )}
            </div>

            <div className="flex h-10 shrink-0 items-center gap-1 rounded-lg border border-white/[0.075] bg-[#0D0D10] p-1">
              {(
                [
                  "ALL",
                  "DRAFT",
                  "PUBLISHED",
                ] as const
              ).map(status => {
                const active =
                  filter === status;

                return (
                  <button
                    key={status}
                    type="button"
                    onClick={() =>
                      setFilter(status)
                    }
                    className={cn(
                      "h-8 rounded-md px-3 text-xs font-medium transition-colors",
                      active
                        ? "bg-white/[0.09] text-white"
                        : "text-white/32 hover:bg-white/[0.035] hover:text-white/65",
                    )}
                  >
                    {status === "ALL"
                      ? "All"
                      : status === "DRAFT"
                        ? "Drafts"
                        : "Published"}
                  </button>
                );
              })}
            </div>
          </div>
        </Reveal>

        {/* ================================================================
            RESULT COUNT
        ================================================================ */}

        {!workflows.isLoading &&
          allWorkflows.length > 0 && (
            <div className="mt-4 flex items-center justify-between px-0.5">
              <p className="text-[10px] text-white/22">
                {items.length} workflow
                {items.length === 1
                  ? ""
                  : "s"}
                {hasFilters
                  ? " matching filters"
                  : " in workspace"}
              </p>

              {hasFilters && (
                <button
                  type="button"
                  onClick={() => {
                    setSearch("");
                    setFilter("ALL");
                  }}
                  className="text-[10px] text-white/25 hover:text-white/60"
                >
                  Clear filters
                </button>
              )}
            </div>
          )}

        {/* ================================================================
            CONTENT
        ================================================================ */}

        <div className="mt-3">
          {workflows.isLoading ? (
            <WorkflowGridSkeleton />
          ) : items.length === 0 ? (
            <EmptyState
              hasFilters={hasFilters}
              onClear={() => {
                setSearch("");
                setFilter("ALL");
              }}
              onCreate={() =>
                setCreateOpen(true)
              }
            />
          ) : (
            <StaggerGroup
              key={`${filter}:${search}`}
              stagger={0.035}
            >
              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                {items.map(workflow => (
                  <StaggerItem
                    key={workflow.id}
                    distance={8}
                  >
                    <HoverLift
                      scale={1.004}
                      y={-2}
                    >
                      <WorkflowCard
                        workflow={workflow}
                        onDelete={() => {
                          removeMutation.reset();
                          setToDelete(
                            workflow,
                          );
                        }}
                      />
                    </HoverLift>
                  </StaggerItem>
                ))}
              </div>
            </StaggerGroup>
          )}
        </div>

        {/* ================================================================
            FOOTER
        ================================================================ */}

        {!workflows.isLoading &&
          allWorkflows.length > 0 && (
            <Reveal delay={0.08}>
              <div className="mt-6 flex items-center justify-between border-t border-white/[0.045] pt-3">
                <span className="text-[9px] text-white/18">
                  {items.length} visible
                </span>

                <span className="font-mono text-[9px] uppercase tracking-[0.13em] text-white/12">
                  FLOWOPS / WORKFLOW CONTROL
                </span>
              </div>
            </Reveal>
          )}
      </div>

      <CreateWorkflowDialog
        open={createOpen}
        onOpenChange={
          setCreateOpen
        }
      />

      {/* ================================================================
          DELETE CONFIRMATION
      ================================================================ */}

      <AlertDialog
        open={!!toDelete}
        onOpenChange={open => {
          if (
            !open &&
            !removeMutation.isPending
          ) {
            setToDelete(null);
          }
        }}
      >
        <AlertDialogContent className="border-white/[0.09] bg-[#0D0D10] text-white sm:max-w-md">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-base font-semibold tracking-[-0.015em]">
              Delete this workflow?
            </AlertDialogTitle>
            <AlertDialogDescription className="text-sm leading-6 text-white/45">
              <span className="font-medium text-white/80">
                {toDelete?.name}
              </span>{" "}
              will be permanently
              removed. This can&apos;t be
              undone.
            </AlertDialogDescription>
          </AlertDialogHeader>

          {removeMutation.isError && (
            <p
              role="alert"
              className="rounded-lg border border-red-400/20 bg-red-400/[0.06] px-3 py-2 text-xs text-red-300"
            >
              Couldn&apos;t delete the
              workflow. Try again.
            </p>
          )}

          <AlertDialogFooter className="gap-2 sm:gap-2">
            <AlertDialogCancel
              disabled={
                removeMutation.isPending
              }
              className="mt-0 border-white/[0.09] bg-transparent text-white/70 hover:bg-white/[0.05] hover:text-white"
            >
              Cancel
            </AlertDialogCancel>

            <AlertDialogAction
              disabled={
                removeMutation.isPending
              }
              onClick={event => {
                event.preventDefault();
                if (toDelete) {
                  removeMutation.mutate(
                    toDelete.id,
                  );
                }
              }}
              className="gap-2 border border-red-400/25 bg-red-500/15 text-red-300 shadow-none hover:bg-red-500/25 hover:text-red-200"
            >
              {removeMutation.isPending && (
                <Loader2 className="size-4 animate-spin" />
              )}
              Delete workflow
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </main>
  );
}

/* ==========================================================================
   METRIC
============================================================================ */

function OverviewMetric({
  label,
  value,
  tone = "neutral",
  detail,
}: {
  label: string;
  value: number;
  tone?:
    | "positive"
    | "warning"
    | "neutral";
  detail: string;
}) {
  return (
    <div className="min-h-[94px] border-r border-white/[0.05] px-4 py-3.5 last:border-r-0">
      <p className="text-[9px] font-medium uppercase tracking-[0.1em] text-white/23">
        {label}
      </p>

      <p className="mt-2 text-[24px] font-semibold leading-none tracking-[-0.045em] tabular-nums text-white/90">
        {value.toLocaleString()}
      </p>

      <div className="mt-2 flex items-center gap-1.5">
        <span
          className={cn(
            "size-1.5 rounded-full",
            tone === "positive"
              ? "bg-emerald-400/70"
              : tone === "warning"
                ? "bg-amber-400/70"
                : "bg-white/18",
          )}
        />

        <span className="text-[9px] text-white/18">
          {detail}
        </span>
      </div>
    </div>
  );
}

/* ==========================================================================
   WORKFLOW CARD
============================================================================ */

function WorkflowCard({
  workflow,
  onDelete,
}: {
  workflow: WorkflowItem;
  onDelete: () => void;
}) {
  const published =
    workflow.status === "PUBLISHED";

  return (
    <article
      className={cn(
        "group relative flex h-full min-h-[184px] flex-col overflow-hidden rounded-xl",
        "border border-white/[0.07] bg-[#0D0D10]",
        "transition-[border-color,background-color,box-shadow] duration-200",
        "hover:border-white/[0.14] hover:bg-[#0F0F12]",
        "hover:shadow-[0_18px_45px_rgba(0,0,0,0.3)]",
        "focus-within:border-white/20",
      )}
    >
      {/* Whole card is clickable; the menu sits above this link */}
      <Link
        href={`/workflows/${workflow.id}`}
        aria-label={`Open ${workflow.name}`}
        className="absolute inset-0 z-0 rounded-xl focus-visible:outline-none"
      />

      {/* Top: identity */}
      <div className="pointer-events-none relative z-[1] flex flex-1 flex-col p-4 pb-3">
        <div className="flex items-start justify-between gap-3">
          <div className="flex min-w-0 items-center gap-3">
            <div className="flex size-9 shrink-0 items-center justify-center rounded-lg border border-white/[0.08] bg-[#111114] transition-colors group-hover:border-white/[0.14]">
              <Workflow className="size-4 text-white/45 transition-colors group-hover:text-white/75" />
            </div>

            <div className="min-w-0">
              <h3 className="truncate text-[15px] font-medium tracking-[-0.015em] text-white/90 group-hover:text-white">
                {workflow.name}
              </h3>

              <div className="mt-1 flex items-center gap-1.5">
                <span
                  className={cn(
                    "size-1.5 rounded-full",
                    published
                      ? "bg-emerald-400"
                      : "bg-amber-400",
                  )}
                />
                <span
                  className={cn(
                    "text-[11px] font-medium",
                    published
                      ? "text-emerald-300/80"
                      : "text-amber-300/75",
                  )}
                >
                  {published
                    ? "Published"
                    : "Draft"}
                </span>
              </div>
            </div>
          </div>

          <span
            aria-hidden
            className="size-7 shrink-0"
          />
        </div>

        <p
          className={cn(
            "mt-3.5 line-clamp-2 text-[12.5px] leading-[1.6]",
            workflow.description
              ? "text-white/45"
              : "text-white/25",
          )}
        >
          {workflow.description ||
            "No description added."}
        </p>
      </div>

      {/* Footer */}
      <div className="pointer-events-none relative z-[1] flex items-center justify-between gap-3 border-t border-white/[0.06] bg-white/[0.012] px-4 py-3">
        <div className="flex min-w-0 items-center gap-1.5 text-white/35">
          <Calendar className="size-3 shrink-0" />
          <span className="truncate text-[11.5px]">
            {formatUpdatedAt(
              workflow.updatedAt,
            )}
          </span>
        </div>

        <span className="flex shrink-0 items-center gap-1 text-[11.5px] font-medium text-white/40 transition-colors group-hover:text-white">
          Open
          <ArrowUpRight className="size-3 transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5" />
        </span>
      </div>

      {/* Actions */}
      <div className="absolute right-3 top-3 z-10">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              aria-label={`Actions for ${workflow.name}`}
              className="flex size-7 items-center justify-center rounded-md border border-transparent text-white/35 transition-colors hover:border-white/[0.09] hover:bg-white/[0.06] hover:text-white focus-visible:border-white/25 focus-visible:outline-none data-[state=open]:border-white/[0.09] data-[state=open]:bg-white/[0.06] data-[state=open]:text-white"
            >
              <MoreHorizontal className="size-4" />
            </button>
          </DropdownMenuTrigger>

          <DropdownMenuContent
            align="end"
            className="w-44 border-white/[0.09] bg-[#0D0D10] text-white"
          >
            <DropdownMenuItem
              asChild
              className="cursor-pointer text-white/80 focus:bg-white/[0.06] focus:text-white"
            >
              <Link
                href={`/workflows/${workflow.id}`}
              >
                <ArrowUpRight className="mr-2 size-4 text-white/45" />
                Open workflow
              </Link>
            </DropdownMenuItem>

            <DropdownMenuSeparator className="bg-white/[0.07]" />

            <DropdownMenuItem
              onSelect={onDelete}
              className="cursor-pointer text-red-300 focus:bg-red-500/10 focus:text-red-200"
            >
              <Trash2 className="mr-2 size-4" />
              Delete
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </article>
  );
}

/* ==========================================================================
   EMPTY
============================================================================ */

function EmptyState({
  hasFilters,
  onClear,
  onCreate,
}: {
  hasFilters: boolean;
  onClear: () => void;
  onCreate: () => void;
}) {
  return (
    <Reveal>
      <Card className="rounded-xl border border-dashed border-white/[0.085] bg-[#0D0D10] shadow-none">
        <CardContent className="flex flex-col items-center justify-center px-6 py-20 text-center">
          <div className="flex size-11 items-center justify-center rounded-xl border border-white/[0.07] bg-[#111114]">
            <Workflow className="size-5 text-white/22" />
          </div>

          <p className="mt-4 text-sm font-medium text-white/65">
            {hasFilters
              ? "No workflows found"
              : "No workflows yet"}
          </p>

          <p className="mt-1.5 max-w-sm text-xs leading-5 text-white/28">
            {hasFilters
              ? "Try changing your search or status filter."
              : "Create your first workflow to start building an automation."}
          </p>

          <Button
            variant="outline"
            size="sm"
            onClick={
              hasFilters
                ? onClear
                : onCreate
            }
            className="mt-5 gap-1.5 border-white/[0.09] bg-transparent text-white/60 hover:bg-white/[0.045] hover:text-white"
          >
            {hasFilters ? (
              "Clear filters"
            ) : (
              <>
                <Plus className="size-3.5" />
                Create Workflow
              </>
            )}
          </Button>
        </CardContent>
      </Card>
    </Reveal>
  );
}

/* ==========================================================================
   SKELETON
============================================================================ */

function WorkflowGridSkeleton() {
  return (
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
      {Array.from(
        { length: 6 },
        (_, index) => (
          <div
            key={index}
            className="flex min-h-[184px] flex-col overflow-hidden rounded-xl border border-white/[0.07] bg-[#0D0D10]"
          >
            <div className="flex-1 p-4 pb-3">
              <div className="flex items-center gap-3">
                <Skeleton className="size-9 rounded-lg bg-white/[0.05]" />

                <div>
                  <Skeleton className="h-4 w-32 bg-white/[0.05]" />
                  <Skeleton className="mt-2 h-3 w-16 bg-white/[0.035]" />
                </div>
              </div>

              <Skeleton className="mt-4 h-3 w-full bg-white/[0.035]" />
              <Skeleton className="mt-1.5 h-3 w-2/3 bg-white/[0.03]" />
            </div>

            <div className="flex items-center justify-between border-t border-white/[0.06] px-4 py-3">
              <Skeleton className="h-3 w-24 bg-white/[0.035]" />
              <Skeleton className="h-3 w-10 bg-white/[0.03]" />
            </div>
          </div>
        ),
      )}
    </div>
  );
}

/* ==========================================================================
   DATE
============================================================================ */

function formatUpdatedAt(
  value?: string | null,
) {
  if (!value) {
    return "No recent update";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "No recent update";
  }

  return `Updated ${date.toLocaleDateString(
    undefined,
    {
      month: "short",
      day: "numeric",
      year: "numeric",
    },
  )}`;
}