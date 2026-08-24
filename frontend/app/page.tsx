import Link from "next/link";
import {
  Activity,
  ArrowRight,
  Bot,
  GitBranch,
  Plug,
  Shield,
  Workflow,
  Zap,
} from "lucide-react";
import { Logo } from "@/components/brand/logo";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";
import { WorkflowDiagram } from "@/components/marketing/workflow-diagram";

const NAV_LINKS = [
  { label: "How it works", href: "#how" },
  { label: "Features", href: "#features" },
  { label: "Integrations", href: "#features" },
];

const STEPS = [
  {
    icon: Workflow,
    title: "Build visually",
    body: "Drag, drop, and connect triggers, logic, AI, and actions on an infinite canvas. No glue code.",
  },
  {
    icon: Bot,
    title: "Add AI agents",
    body: "Drop in LLM agents, classifiers, and extractors. Describe a workflow in plain English and let AI draft the graph.",
  },
  {
    icon: Activity,
    title: "Execute & monitor",
    body: "Publish immutable versions, run them on triggers or webhooks, and watch every node execute in real time.",
  },
];

const FEATURES = [
  {
    icon: Workflow,
    title: "Visual workflow builder",
    body: "A production-grade canvas with validation, versioning, and a rich node library.",
  },
  {
    icon: Bot,
    title: "AI agents & generation",
    body: "Reusable agents with structured output, plus natural-language workflow generation.",
  },
  {
    icon: GitBranch,
    title: "Branching & logic",
    body: "Conditions, branches, loops, delays, and transforms with a safe expression system.",
  },
  {
    icon: Activity,
    title: "Real-time monitoring",
    body: "Live execution streams, per-node inputs and outputs, retries, and full logs.",
  },
  {
    icon: Plug,
    title: "Integrations",
    body: "Connect Slack, GitHub, Jira, databases, and any REST API with encrypted credentials.",
  },
  {
    icon: Shield,
    title: "Enterprise security",
    body: "Multi-tenant isolation, RBAC, encrypted secrets, audit logs, and rate limiting.",
  },
];

export default function LandingPage() {
  return (
    <div className="relative flex min-h-screen flex-col">
      {/* Nav */}
      <header className="sticky top-0 z-40 border-b border-border/60 bg-background/80 backdrop-blur">
        <div className="container flex h-16 items-center justify-between">
          <Logo />
          <nav className="hidden items-center gap-8 md:flex">
            {NAV_LINKS.map((l) => (
              <a
                key={l.label}
                href={l.href}
                className="text-sm text-muted-foreground transition-colors hover:text-foreground"
              >
                {l.label}
              </a>
            ))}
          </nav>
          <div className="flex items-center gap-2">
            <ThemeToggle />
            <Button asChild>
              <a href="#how">
                Start Building <ArrowRight />
              </a>
            </Button>
          </div>
        </div>
      </header>

      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="grid-bg pointer-events-none absolute inset-0 opacity-40 [mask-image:radial-gradient(ellipse_at_top,black,transparent_70%)]" />
        <div
          className="pointer-events-none absolute inset-x-0 top-0 h-[420px] opacity-60"
          style={{
            background:
              "radial-gradient(60% 60% at 50% 0%, hsl(var(--primary) / 0.18), transparent 70%)",
          }}
        />
        <div className="container relative grid gap-12 py-20 lg:grid-cols-2 lg:items-center lg:py-28">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card px-3 py-1 text-xs font-medium text-muted-foreground">
              <Zap className="size-3.5 text-primary" />
              AI-native workflow automation
            </span>
            <h1 className="mt-6 text-balance text-4xl font-semibold tracking-tight sm:text-5xl lg:text-6xl">
              Build workflows.{" "}
              <span className="text-primary">Let AI run the work.</span>
            </h1>
            <p className="mt-5 max-w-xl text-pretty text-lg text-muted-foreground">
              FlowOps gives your team a visual automation platform for building,
              executing, and monitoring intelligent workflows — from a single
              trigger to a full multi-step, human-in-the-loop process.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Button asChild size="lg">
                <a href="#how">
                  Start Building <ArrowRight />
                </a>
              </Button>
              <Button asChild size="lg" variant="outline">
                <a href="#features">Explore Workflows</a>
              </Button>
            </div>
            <p className="mt-6 text-sm text-muted-foreground">
              Trigger → Process → AI → Decision → Action → Monitor.
            </p>
          </div>

          <div className="relative">
            <div className="rounded-2xl border border-border bg-card/80 p-2 shadow-2xl shadow-primary/5 backdrop-blur">
              <div className="flex items-center gap-1.5 px-3 py-2">
                <span className="size-2.5 rounded-full bg-destructive/70" />
                <span className="size-2.5 rounded-full bg-warning/70" />
                <span className="size-2.5 rounded-full bg-success/70" />
                <span className="ml-3 text-xs text-muted-foreground">
                  incident-response.flow
                </span>
              </div>
              <div className="rounded-xl border border-border bg-background/60 p-4">
                <WorkflowDiagram />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* How it works */}
      <section id="how" className="border-t border-border/60 py-20">
        <div className="container">
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-3xl font-semibold tracking-tight">
              How FlowOps works
            </h2>
            <p className="mt-3 text-muted-foreground">
              From idea to a running, monitored automation in three steps.
            </p>
          </div>
          <div className="mt-12 grid gap-6 md:grid-cols-3">
            {STEPS.map((s, i) => (
              <div
                key={s.title}
                className="rounded-xl border border-border bg-card p-6"
              >
                <div className="flex size-10 items-center justify-center rounded-lg bg-accent text-accent-foreground">
                  <s.icon className="size-5" />
                </div>
                <div className="mt-4 text-xs font-medium text-muted-foreground">
                  Step {i + 1}
                </div>
                <h3 className="mt-1 text-lg font-semibold">{s.title}</h3>
                <p className="mt-2 text-sm text-muted-foreground">{s.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="border-t border-border/60 py-20">
        <div className="container">
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-3xl font-semibold tracking-tight">
              Everything you need to automate
            </h2>
            <p className="mt-3 text-muted-foreground">
              A serious platform for serious automation — built for teams.
            </p>
          </div>
          <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map((f) => (
              <div
                key={f.title}
                className="group rounded-xl border border-border bg-card p-6 transition-colors hover:border-primary/40"
              >
                <div className="flex size-10 items-center justify-center rounded-lg bg-accent text-accent-foreground">
                  <f.icon className="size-5" />
                </div>
                <h3 className="mt-4 text-lg font-semibold">{f.title}</h3>
                <p className="mt-2 text-sm text-muted-foreground">{f.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="border-t border-border/60 py-20">
        <div className="container">
          <div className="relative overflow-hidden rounded-2xl border border-border bg-card px-8 py-14 text-center">
            <div
              className="pointer-events-none absolute inset-0"
              style={{
                background:
                  "radial-gradient(50% 80% at 50% 0%, hsl(var(--primary) / 0.14), transparent 70%)",
              }}
            />
            <h2 className="relative text-3xl font-semibold tracking-tight">
              Ship your first workflow today
            </h2>
            <p className="relative mx-auto mt-3 max-w-xl text-muted-foreground">
              Build it visually or generate it with AI. Publish, execute, and
              monitor — end to end.
            </p>
            <div className="relative mt-8">
              <Button asChild size="lg">
                <a href="#how">
                  Start Building <ArrowRight />
                </a>
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="mt-auto border-t border-border/60 py-10">
        <div className="container flex flex-col items-center justify-between gap-4 sm:flex-row">
          <Logo />
          <p className="text-sm text-muted-foreground">
            © {new Date().getFullYear()} FlowOps. Build workflows. Let AI run the
            work.
          </p>
        </div>
      </footer>
    </div>
  );
}
