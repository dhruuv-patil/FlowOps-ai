"use client";

import { motion } from "framer-motion";
import type { ComponentType } from "react";
import {
  Activity,
  BarChart2,
  Database,
  FileText,
  GitBranch,
  Globe,
  Layers,
  Mail,
  MessageSquare,
  Users,
  Workflow,
} from "lucide-react";

import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { WorkflowDiagram } from "./workflow-diagram";
import { Reveal } from "@/components/motion/motion-primitives";
import { AuroraBackground } from "@/components/decor/aurora-background";

/**
 * Trusted by strip — neutral/fictional company names, no real trademarks.
 */
export function TrustedByStrip({
  className,
}: {
  className?: string;
} = {}) {
  const companies = [
    "Acme Corp",
    "Globex",
    "Wayne Enterprises",
    "Stark Industries",
    "Umbrella Corp",
    "Initech",
  ];

  return (
    <div className={cn("w-full overflow-hidden", className)}>
      <div className="flex items-center justify-center gap-12 py-8 text-muted-foreground/40 font-mono text-xs uppercase tracking-widest">
        Trusted by modern teams
      </div>

      <div className="flex animate-marquee gap-16 whitespace-nowrap">
        {companies.map((company, index) => (
          <span
            key={index}
            className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-muted-foreground/50 border border-border/50 rounded-full"
          >
            {company}
          </span>
        ))}

        {companies.map((company, index) => (
          <span
            key={index + companies.length}
            className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-muted-foreground/50 border border-border/50 rounded-full"
          >
            {company}
          </span>
        ))}
      </div>

      <style jsx>{`
        @keyframes marquee {
          from {
            transform: translateX(0);
          }

          to {
            transform: translateX(-50%);
          }
        }

        .animate-marquee {
          animation: marquee 30s linear infinite;
        }
      `}</style>
    </div>
  );
}

/**
 * Animated feature demos — three product mockups with live feel.
 */
export function FeatureDemos({
  className,
}: {
  className?: string;
} = {}) {
  const features = [
    {
      icon: Workflow,
      title: "Visual builder",
      description:
        "Drag, drop, connect. Real-time validation, versioning, and a searchable node palette.",
      demo: (
        <div className="aspect-video rounded-xl border border-border/60 bg-card/80 p-4">
          <WorkflowDiagram />
        </div>
      ),
    },

    {
      icon: Activity,
      title: "Live executions",
      description:
        "Watch every node execute in real time. Inputs, outputs, retries, and logs — all streaming.",
      demo: (
        <div className="aspect-video rounded-xl border border-border/60 bg-card/80 p-4 font-mono text-xs">
          <div className="flex items-center gap-2 text-muted-foreground/60 mb-3">
            <span className="size-2 rounded-full bg-emerald-500 animate-pulse" />
            Running · v3 · 2.3s
          </div>

          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <span className="size-6 rounded bg-emerald-500/20 flex items-center justify-center">
                <span className="size-2 rounded-full bg-emerald-500" />
              </span>
              <span>Webhook received</span>
            </div>

            <div className="flex items-center gap-2 ml-8">
              <span className="size-6 rounded bg-emerald-500/20 flex items-center justify-center">
                <span className="size-2 rounded-full bg-emerald-500" />
              </span>
              <span>AI Agent processing</span>
            </div>

            <div className="flex items-center gap-2 ml-8">
              <span className="size-6 rounded bg-amber-500/20 flex items-center justify-center animate-pulse">
                <span className="size-2 rounded-full bg-amber-500" />
              </span>
              <span>Decision: escalate</span>
            </div>

            <div className="flex items-center gap-2 ml-8">
              <span className="size-6 rounded bg-muted/30 flex items-center justify-center">
                <span className="size-2 rounded-full bg-muted" />
              </span>
              <span>Create Incident</span>
            </div>
          </div>
        </div>
      ),
    },

    {
      icon: BarChart2,
      title: "Reliability insights",
      description:
        "Automatic anomaly detection, health scores, and AI-powered root cause analysis.",
      demo: (
        <div className="aspect-video rounded-xl border border-border/60 bg-card/80 p-4">
          <div className="h-full flex items-end justify-around gap-1 px-2">
            {[45, 62, 58, 74, 81, 69, 78, 85, 82, 90, 88, 94].map(
              (height, index) => (
                <motion.div
                  key={index}
                  className="flex-1 max-w-8 rounded-t bg-brand-gradient"
                  style={{ height: `${height}%` }}
                  initial={{ height: 0 }}
                  animate={{ height: `${height}%` }}
                  transition={{
                    delay: index * 0.05,
                    duration: 0.5,
                  }}
                />
              ),
            )}
          </div>
        </div>
      ),
    },
  ];

  return (
    <div className={cn("grid gap-8 md:grid-cols-3", className)}>
      {features.map((feature, index) => (
        <Reveal
          key={feature.title}
          delay={index * 0.1}
          distance={20}
        >
          <Card className="h-full hoverable">
            <CardHeader>
              <div className="flex size-10 items-center justify-center rounded-xl bg-brand-500/10 text-brand-500 mb-3">
                <feature.icon className="size-5" />
              </div>

              <CardTitle className="text-lg">
                {feature.title}
              </CardTitle>

              <CardDescription>
                {feature.description}
              </CardDescription>
            </CardHeader>

            <CardContent className="pt-0">
              {feature.demo}
            </CardContent>
          </Card>
        </Reveal>
      ))}
    </div>
  );
}

/**
 * Integrations marquee — scrolling row of integration tiles.
 */
export function IntegrationsMarquee({
  className,
}: {
  className?: string;
} = {}) {
  const integrations = [
    {
      name: "Slack",
      icon: MessageSquare,
      color: "#4A154B",
    },
    {
      name: "GitHub",
      icon: GitBranch,
      color: "#181717",
    },
    {
      name: "Jira",
      icon: Layers,
      color: "#0052CC",
    },
    {
      name: "PostgreSQL",
      icon: Database,
      color: "#336791",
    },
    {
      name: "MongoDB",
      icon: Database,
      color: "#47A248",
    },
    {
      name: "Redis",
      icon: Database,
      color: "#DC382D",
    },
    {
      name: "HTTP/Webhook",
      icon: Globe,
      color: "#818cf8",
    },
    {
      name: "Email",
      icon: Mail,
      color: "#EA4335",
    },
    {
      name: "Discord",
      icon: MessageSquare,
      color: "#5865F2",
    },
    {
      name: "Teams",
      icon: Users,
      color: "#6264A7",
    },
    {
      name: "Linear",
      icon: GitBranch,
      color: "#5E6AD2",
    },
    {
      name: "Notion",
      icon: FileText,
      color: "#000000",
    },
  ];

  return (
    <div className={cn("w-full overflow-hidden", className)}>
      <div className="flex animate-marquee-slow gap-4 whitespace-nowrap px-4">
        {integrations.map((integration, index) => (
          <IntegrationTile
            key={index}
            {...integration}
          />
        ))}

        {integrations.map((integration, index) => (
          <IntegrationTile
            key={index + integrations.length}
            {...integration}
          />
        ))}
      </div>

      <style jsx>{`
        @keyframes marquee-slow {
          from {
            transform: translateX(0);
          }

          to {
            transform: translateX(-50%);
          }
        }

        .animate-marquee-slow {
          animation: marquee-slow 40s linear infinite;
        }
      `}</style>
    </div>
  );
}

function IntegrationTile({
  name,
  icon: Icon,
  color,
}: {
  name: string;
  icon: ComponentType<{ className?: string; style?: React.CSSProperties }>;
  color: string;
}) {
  return (
    <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-card border border-border/60 shrink-0 transition-all hover:shadow-card-hover hover:border-brand-500/30">
      <div
        className="flex size-8 items-center justify-center rounded-lg"
        style={{
          background: `${color}15`,
        }}
      >
        <Icon
          className="size-4"
          style={{ color }}
        />
      </div>

      <span className="text-sm font-medium">
        {name}
      </span>
    </div>
  );
}

/**
 * CTA Banner with animated gradient background.
 */
export function CTABanner({
  className,
}: {
  className?: string;
} = {}) {
  return (
    <div
      className={cn(
        "relative rounded-xl overflow-hidden",
        className,
      )}
    >
      <AuroraBackground intensity="strong" />

      <div className="relative z-10 px-8 py-12 md:px-16 md:py-16 text-center">
        <h2 className="text-3xl md:text-4xl font-semibold tracking-tight">
          Ready to automate your workflows?
        </h2>

        <p className="mt-3 text-lg text-muted-foreground max-w-2xl mx-auto">
          Join thousands of teams building reliable AI-powered
          automations with FlowOps.
        </p>

        <div className="mt-8 flex items-center justify-center gap-4">
          <Button size="xl" asChild>
            <a href="/register">
              Start free — no credit card
            </a>
          </Button>

          <Button size="xl" variant="outline" asChild>
            <a href="/login">
              See a demo
            </a>
          </Button>
        </div>
      </div>
    </div>
  );
}

/**
 * Stats strip for landing page.
 */
export function StatsStrip({
  className,
}: {
  className?: string;
} = {}) {
  const stats = [
    {
      value: "9,000+",
      label: "Integrations",
    },
    {
      value: "99.9%",
      label: "Uptime SLA",
    },
    {
      value: "50M+",
      label: "Runs/month",
    },
    {
      value: "2.3s",
      label: "Avg latency",
    },
  ];

  return (
    <div
      className={cn(
        "grid grid-cols-2 md:grid-cols-4 gap-8 py-12",
        className,
      )}
    >
      {stats.map((stat, index) => (
        <Reveal
          key={stat.label}
          delay={index * 0.05}
        >
          <div className="text-center">
            <div className="text-4xl md:text-5xl font-bold tracking-tight text-gradient">
              {stat.value}
            </div>

            <div className="mt-1 text-sm text-muted-foreground">
              {stat.label}
            </div>
          </div>
        </Reveal>
      ))}
    </div>
  );
}