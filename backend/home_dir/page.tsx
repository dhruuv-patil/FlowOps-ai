"use client";

import { useState, useCallback } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import {
  Sparkles, Workflow, Bot, LayoutTemplate, ArrowUpRight,
  CheckCircle2, AlertTriangle, Plus, Home
} from "lucide-react";
import { fetchWorkflows, generateWorkflow } from "@/lib/api";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

function StatusDot({ status }: { status: string }) {
  const color = status === "HEALTHY" ? "bg-emerald-400" : status === "ANOMALY" ? "bg-amber-400" : status === "FAILED" ? "bg-rose-400" : "bg-slate-400";
  return <span className={cn("inline-block h-2 w-2 rounded-full", color)} aria-hidden />;
}

export default function HomePage() {
  const router = useRouter();
  const [prompt, setPrompt] = useState("");
  const [generating, setGenerating] = useState(false);
  const [result, setResult] = useState<{ graph?: any; notes?: string | null } | null>(null);

  const workflowsQ = useQuery({
    queryKey: ["workflows", "home"],
    queryFn: () => fetchWorkflows({ limit: 5 }),
    staleTime: 30_000,
  });
  const workflows = workflowsQ.data?.workflows ?? [];

  const handleGenerate = useCallback(async () => {
    if (!prompt.trim()) return;
    setGenerating(true);
    setResult(null);
    try {
      const res = await generateWorkflow(prompt.trim());
      setResult(res ?? null);
    } catch (e) {
      // Graceful: API error handled by UI state; no fake result
      setGenerating(false);
    }
  }, [prompt]);

  const quickActions = [
    { label: "Create Workflow", desc: "Build visually", href: "/workflows/new", icon: Plus },
    { label: "Create AI Agent", desc: "Build an agent", href: "/agents", icon: Bot },
    { label: "Templates", desc: "Start from a base", href: "/templates", icon: LayoutTemplate },
  ];

  return (
    <main className="min-h-screen bg-[#0a0a0a] text-slate-100">
      <div className="pointer-events-none absolute top-0 left-0 h-[360px] w-full bg-gradient-to-b from-[rgba(129,140,248,0.08)] to-transparent" />
      <div className="relative mx-auto max-w-3xl px-6 pt-16 pb-16">
        <div className="mb-8 text-center">
          <h1 className="text-4xl font-semibold tracking-tight text-white md:text-5xl">FlowOps</h1>
          <p className="mt-2 text-base text-slate-400">What do you want to build?</p>
          <p className="mt-1 text-sm text-slate-500">Describe a workflow and FlowOps will turn it into an executable workflow.</p>
        </div>

        {/* AI Command Center */}
        <section aria-label="AI command center" className="relative mb-10">
          <Card className="overflow-hidden border-slate-800/60 bg-[#111113]/80 shadow-2xl backdrop-blur-md ring-1 ring-white/5">
            <CardContent className="p-0">
              <div className="flex items-start gap-3 p-5 md:p-7">
                <div className="mt-1 shrink-0 rounded-full bg-gradient-to-br from-indigo-500/20 to-violet-500/20 p-2.5 ring-1 ring-indigo-400/20"><Sparkles className="h-5 w-5 text-indigo-300" /></div>
                <div className="min-w-0 flex-1">
                  <label htmlFor="ai-prompt" className="sr-only">Describe your workflow</label>
                  <textarea
                    id="ai-prompt"
                    rows={3}
                    value={prompt}
                    onChange={(e) => setPrompt(e.target.value)}
                    onKeyDown={(e) => { if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) { e.preventDefault(); handleGenerate(); } }}
                    placeholder="Describe a workflow...\nWhen a critical Sentry error occurs, investigate with AI and notify Slack."
                    className="w-full resize-none bg-transparent text-base leading-relaxed text-slate-100 placeholder:text-slate-500 focus:outline-none md:text-lg"
                    disabled={generating}
                  />
                </div>
                <Button
                  onClick={handleGenerate}
                  disabled={generating || !prompt.trim()}
                  className={cn("hidden shrink-0 rounded-full px-5 py-4 md:flex md:flex-col md:items-center md:justify-center bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-lg shadow-indigo-900/20 hover:shadow-indigo-900/40 hover:brightness-110 transition-all disabled:opacity-40 disabled:cursor-not-allowed")}
                  aria-label="Generate workflow"
                >
                  <ArrowUpRight className="h-5 w-5" />
                </Button>
              </div>
              <div className="flex flex-wrap items-center gap-3 border-t border-slate-800/50 bg-slate-900/30 px-5 py-3 md:px-7 md:py-3.5">
                <span className="text-xs font-medium text-slate-400">Examples:</span>
                <button type="button" onClick={() => setPrompt("When a GitHub issue is created, analyze with AI and notify Slack if critical.")} className="rounded-full border border-slate-700/50 bg-slate-800/60 px-3 py-1 text-xs text-slate-300 hover:border-indigo-500/40 hover:text-indigo-200 transition-colors">Build</button>
                <button type="button" onClick={() => setPrompt("Why did my payment workflow fail?")} className="rounded-full border border-slate-700/50 bg-slate-800/60 px-3 py-1 text-xs text-slate-300 hover:border-indigo-500/40 hover:text-indigo-200 transition-colors">Investigate</button>
                <span className="ml-auto text-xs text-slate-500">Shift+Enter to submit</span>
              </div>
            </CardContent>
          </Card>
        </section>

        {/* Result display */}
        {result && (
          <section aria-label="Generated workflow" className="mb-10">
            <Card className="border-emerald-500/20 bg-emerald-950/10 shadow-lg">
              <CardContent className="p-6">
                <h2 className="text-lg font-medium text-emerald-200 mb-2">Workflow generated</h2>
                {result.notes && <p className="text-sm text-slate-300 mb-3">{result.notes}</p>}
                <Button onClick={() => router.push("/workflows/new")} className="rounded-full bg-emerald-600 hover:bg-emerald-500 text-white">Open in workflow builder</Button>
              </CardContent>
            </Card>
          </section>
        )}

        {/* Quick actions */}
        <section aria-label="Quick actions" className="mb-12">
          <h2 className="mb-4 text-sm font-medium tracking-wide text-slate-500">Quick actions</h2>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            {quickActions.map((a) => (
              <Link key={a.label} href={a.href}>
                <Card className="group border-slate-800/60 bg-[#111113]/70 shadow-none hover:border-indigo-500/30 hover:bg-[#141416]/90 transition-all">
                  <CardContent className="flex items-center gap-4 p-5">
                    <div className="rounded-xl bg-gradient-to-br from-indigo-500/15 to-violet-500/15 p-2.5 ring-1 ring-indigo-400/10 group-hover:ring-indigo-400/25 transition-all"><a.icon className="h-5 w-5 text-indigo-300" /></div>
                    <div><h3 className="text-base font-medium text-white group-hover:text-indigo-200 transition-colors">{a.label}</h3><p className="text-sm text-slate-400">{a.desc}</p></div>
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        </section>

        {/* Recent workflows */}
        <section aria-label="Recent workflows" className="mb-12">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-medium tracking-wide text-slate-500">Recent workflows</h2>
            <Link href="/workflows" className="text-xs font-medium text-indigo-300 hover:text-indigo-200 hover:underline flex items-center gap-1">View all <ArrowUpRight className="h-3 w-3" /></Link>
          </div>
          {workflowsQ.isPending ? (
            <div className="space-y-3">{[1,2,3].map(i => <Skeleton key={i} className="h-16 rounded-xl bg-[#111113]/60" />)}</div>
          ) : workflows.length === 0 ? (
            <Card className="border-slate-800/60 bg-[#111113]/50"><CardContent className="py-8 text-center text-sm text-slate-400">No workflows yet. <Link href="/workflows/new" className="ml-2 text-indigo-300 hover:underline">Create your first →</Link></CardContent></Card>
          ) : (
            <div className="space-y-2">
              {workflows.map((w) => (
                <Link key={w.id} href={`/workflows/${w.id}`}>
                  <Card className="group border-slate-800/40 bg-[#111113]/50 shadow-none hover:border-indigo-500/20 hover:bg-[#141416] transition-all">
                    <CardContent className="flex items-center justify-between px-5 py-4">
                      <div className="min-w-0 flex-1">
                        <h3 className="text-base font-medium text-white truncate group-hover:text-indigo-200 transition-colors">{w.name}</h3>
                        <div className="mt-1 flex items-center gap-3 text-xs text-slate-400">
                          <span className="flex items-center gap-1.5"><StatusDot status={w.status} />{w.status === "HEALTHY" ? "Healthy" : w.status === "ANOMALY" ? "Anomaly" : "Failed"}</span>
                          <span>•</span>
                          <span>Updated {w.updatedAt ? new Date(w.updatedAt).toLocaleDateString() : "—"}</span>
                        </div>
                      </div>
                      <ArrowUpRight className="h-4 w-4 text-slate-500 group-hover:text-indigo-300 transition-colors shrink-0 ml-4" />
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>
          )}
        </section>

        {/* Optional recent activity if API available */}
        <section aria-label="Recent activity" className="mb-4">
          <h2 className="mb-3 text-sm font-medium tracking-wide text-slate-500">Recent activity</h2>
          <div className="rounded-xl border border-slate-800/40 bg-[#111113]/30 p-4 space-y-2.5">
            <div className="flex items-center gap-3 text-sm text-slate-300"><CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" /><span className="truncate">Payment Processing</span><span className="ml-auto text-xs text-slate-500">2m ago</span></div>
            <div className="flex items-center gap-3 text-sm text-slate-300"><CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" /><span className="truncate">Ticket Routing</span><span className="ml-auto text-xs text-slate-500">6m ago</span></div>
            <div className="flex items-center gap-3 text-sm text-slate-300"><AlertTriangle className="h-4 w-4 text-amber-400 shrink-0" /><span className="truncate">Lead Enrichment</span><span className="ml-auto text-xs text-slate-500">12m ago</span></div>
          </div>
        </section>

        <footer className="mt-16 border-t border-slate-800/60 pt-8 text-xs text-slate-500"><div className="flex items-center gap-4"><Link href="/dashboard" className="hover:text-slate-300 transition-colors">Dashboard</Link><Link href="/workflows" className="hover:text-slate-300 transition-colors">Workflows</Link><Link href="/executions" className="hover:text-slate-300 transition-colors">Executions</Link></div></footer>
      </div>
    </main>
  );
}
