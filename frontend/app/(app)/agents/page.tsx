"use client";

import Link from "next/link";
import { Suspense, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Bot, Plus, Search } from "lucide-react";

import { fetchAiAgents } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { CreateAgentDialog } from "@/components/app/create-agent-dialog";

/**
 * AI Agents library. Lists the current org's saved agents; each row opens the
 * editor + test console. Search is client-side (the list endpoint returns the
 * full org set, ordered by most recently updated).
 */
function AgentsPageContent() {
  const [search, setSearch] = useState("");
  const [createOpen, setCreateOpen] = useState(false);

  // The ⌘K palette's "New AI agent" action deep-links here with ?new=1.
  const params = useSearchParams();
  useEffect(() => {
    if (params.get("new") === "1") setCreateOpen(true);
  }, [params]);

  const query = useQuery({
    queryKey: ["ai-agents"],
    queryFn: fetchAiAgents,
  });

  const agents = useMemo(() => {
    const all = query.data?.agents ?? [];
    const term = search.trim().toLowerCase();
    if (!term) return all;
    return all.filter(
      (a) =>
        a.name.toLowerCase().includes(term) ||
        (a.description ?? "").toLowerCase().includes(term),
    );
  }, [query.data, search]);

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="mono-eyebrow">Automation</p>
          <h1 className="mt-1 text-xl font-semibold tracking-tight text-white/90">AI Agents</h1>
          <p className="mt-1 text-sm text-white/44">
            Reusable AI agents you can test here or drop into a workflow.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="size-4" /> New agent
        </Button>
      </div>

      <div className="relative sm:max-w-xs">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-white/44" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search agents…"
          className="pl-9"
        />
      </div>

      {query.isPending ? (
        <div className="space-y-2">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      ) : query.isError ? (
        <p className="text-sm text-destructive">
          Could not load agents. Please try again.
        </p>
      ) : agents.length === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-white/[0.10] py-16 text-center">
          <Bot className="size-10 text-white/20" />
          <div>
            <p className="font-medium text-white/90">
              {search ? "No agents match your search." : "No agents yet."}
            </p>
            <p className="text-sm text-white/44">
              Create an agent to reuse across your workflows.
            </p>
          </div>
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="size-4" /> New agent
          </Button>
        </div>
      ) : (
        <ul className="rounded-xl border border-white/[0.08] divide-y divide-white/[0.06] overflow-hidden">
          {agents.map((a) => (
            <li key={a.id}>
              <Link
                href={`/agents/${a.id}`}
                className="flex items-center gap-4 px-4 py-3 transition-colors hover:bg-white/[0.03]"
              >
                <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-white/[0.04] text-white/60">
                  <Bot className="size-4" />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="flex items-center gap-2">
                    <span className="truncate font-medium text-white/90">{a.name}</span>
                    {a.model && (
                      <Badge variant="outline" className="font-mono text-[10px]">
                        {a.model}
                      </Badge>
                    )}
                  </span>
                  <span className="block truncate text-sm text-white/44">
                    {a.description || "No description"}
                  </span>
                </span>
                <span className="hidden shrink-0 text-right text-xs text-white/44 sm:block">
                  {a.tools.length} {a.tools.length === 1 ? "tool" : "tools"}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}

      <CreateAgentDialog open={createOpen} onOpenChange={setCreateOpen} />
    </div>
  );
}

export default function AgentsPage() {
  return (
    <Suspense fallback={null}>
      <AgentsPageContent />
    </Suspense>
  );
}
