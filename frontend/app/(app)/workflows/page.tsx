"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, Calendar, Plus, Search, Workflow } from "lucide-react";
import { fetchWorkflows } from "@/lib/api";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { CreateWorkflowDialog } from "@/components/app/create-workflow-dialog";
import {
  Reveal,
  StaggerGroup,
  StaggerItem,
} from "@/components/motion/motion-primitives";
import { HoverLift } from "@/components/motion/motion-primitives";

type StatusFilter = "ALL" | "DRAFT" | "PUBLISHED";

export default function WorkflowsPage() {
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<StatusFilter>("ALL");
  const [createOpen, setCreateOpen] = useState(false);

  const workflows = useQuery({
    queryKey: ["workflows"],
    queryFn: () => fetchWorkflows(),
  });

  const items = (workflows.data?.workflows ?? [])
    .filter((w) => {
      if (filter !== "ALL" && w.status !== filter) return false;
      if (search && !w.name.toLowerCase().includes(search.toLowerCase()))
        return false;
      return true;
    });

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <Reveal>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="mono-eyebrow">Workspace</p>
            <h1 className="mt-1 text-xl font-semibold tracking-tight text-white/90">Workflows</h1>
            <p className="mt-1 text-sm text-white/44">
              {workflows.data?.workflows.length ?? 0} workflows total
            </p>
          </div>
          <Button onClick={() => setCreateOpen(true)} className="gap-2">
            <Plus className="size-4" />
            New Workflow
          </Button>
        </div>
      </Reveal>

      {/* Filters */}
      <Reveal delay={0.1} distance={15}>
        <div className="flex flex-col gap-3 sm:flex-row">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-white/30" />
            <Input
              placeholder="Search workflows…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9 border-white/[0.08] bg-white/[0.04] text-white/80 placeholder:text-white/30"
            />
          </div>
          <div className="flex gap-1 rounded-full border border-white/[0.08] bg-white/[0.03] p-0.5">
            {(["ALL", "DRAFT", "PUBLISHED"] as const).map((s) => (
              <button
                key={s}
                onClick={() => setFilter(s)}
                className={cn(
                  "rounded-full px-3 py-1.5 text-xs font-medium transition-all",
                  filter === s
                    ? "bg-white text-[#050505]"
                    : "text-white/50 hover:text-white/80",
                )}
              >
                {s === "ALL" ? "All" : s.charAt(0) + s.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        </div>
      </Reveal>

      {/* Grid */}
      {workflows.isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Card key={i}>
              <CardContent className="p-5">
                <Skeleton className="mb-3 h-4 w-32" />
                <Skeleton className="mb-4 h-3 w-48" />
                <div className="flex items-center gap-2">
                  <Skeleton className="h-5 w-16 rounded-full" />
                  <Skeleton className="h-4 w-20" />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : items.length === 0 ? (
        <Reveal>
          <Card className="rounded-xl border border-dashed border-white/[0.10] py-16 text-center bg-transparent">
            <CardContent>
              <Workflow className="mx-auto mb-4 size-10 text-white/20" />
              <p className="text-sm text-white/40">
                {search || filter !== "ALL"
                  ? "No workflows match your filters."
                  : "No workflows yet. Create your first one."}
              </p>
              {!search && filter === "ALL" && (
                <Button
                  variant="outline"
                  size="sm"
                  className="mt-4"
                  onClick={() => setCreateOpen(true)}
                >
                  <Plus className="size-3.5 mr-1" />
                  Create Workflow
                </Button>
              )}
            </CardContent>
          </Card>
        </Reveal>
      ) : (
        <StaggerGroup stagger={0.06}>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {items.map((w) => (
              <StaggerItem key={w.id} distance={16}>
                <HoverLift scale={1.015} y={-3}>
                  <Link href={`/workflows/${w.id}`}>
                    <Card className="h-full hoverable group cursor-pointer bg-[#0a0a0a] border border-white/[0.08]">
                      <CardContent className="p-5">
                        <div className="flex items-start justify-between">
                          <h3 className="font-medium text-white/90 group-hover:text-white transition-colors truncate">
                            {w.name}
                          </h3>
                          <ArrowRight className="size-4 text-white/20 group-hover:text-white/60 transition-colors shrink-0 mt-0.5" />
                        </div>
                        <p className="mt-1 text-sm text-white/40 line-clamp-2">
                          {w.description || "No description"}
                        </p>
                        <div className="mt-4 flex items-center gap-2">
                          <Badge
                            variant={
                              w.status === "PUBLISHED" ? "success" : "secondary"
                            }
                          >
                            {w.status.toLowerCase()}
                          </Badge>
                          <span className="flex items-center gap-1 text-xs text-white/30">
                            <Calendar className="size-3" />
                            {w.updatedAt
                              ? new Date(w.updatedAt).toLocaleDateString()
                              : "—"}
                          </span>
                        </div>
                      </CardContent>
                    </Card>
                  </Link>
                </HoverLift>
              </StaggerItem>
            ))}
          </div>
        </StaggerGroup>
      )}

      <CreateWorkflowDialog open={createOpen} onOpenChange={setCreateOpen} />
    </div>
  );
}