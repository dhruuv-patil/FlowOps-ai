"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  AlertTriangle,
  ArrowLeft,
  Brain,
  ChevronDown,
  Clock,
  Code2,
  FileText,
  Loader2,
  Shield,
  Target,
  Zap,
} from "lucide-react";
import { format } from "date-fns";
import { toast } from "sonner";

import {
  fetchAnomaly,
  acknowledgeAnomaly,
  resolveAnomaly,
  markAnomalyFalsePositive,
  investigateAnomaly,
} from "@/lib/api";

import type {
  AnomalyDetail,
  AnomalySeverity,
  AnomalyStatus,
  AnomalyType,
  AIInvestigationResult,
} from "@/types";

import { Button } from "@/components/ui/button";

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";

import { Badge } from "@/components/ui/badge";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

const SEVERITY_COLORS: Record<AnomalySeverity, string> = {
  LOW: "bg-green-500/10 text-green-400 border-green-500/20",
  MEDIUM: "bg-yellow-500/10 text-yellow-400 border-yellow-500/20",
  HIGH: "bg-orange-500/10 text-orange-400 border-orange-500/20",
  CRITICAL: "bg-red-500/10 text-red-400 border-red-500/20",
};

const TYPE_ICONS: Record<
  AnomalyType,
  React.ComponentType<{ className?: string }>
> = {
  VOLUME: Zap,
  LATENCY: Clock,
  OUTPUT: Code2,
  BEHAVIORAL: Target,
};

const STATUS_COLORS: Record<AnomalyStatus, string> = {
  OPEN: "bg-blue-500/10 text-blue-400 border-blue-500/20",
  ACKNOWLEDGED: "bg-purple-500/10 text-purple-400 border-purple-500/20",
  RESOLVED: "bg-green-500/10 text-green-400 border-green-500/20",
  FALSE_POSITIVE: "bg-gray-500/10 text-gray-400 border-gray-500/20",
};

function SeverityBadge({
  severity,
}: {
  severity: AnomalySeverity;
}) {
  return (
    <Badge
      className={SEVERITY_COLORS[severity]}
      variant="secondary"
    >
      {severity}
    </Badge>
  );
}

function StatusBadge({
  status,
}: {
  status: AnomalyStatus;
}) {
  return (
    <Badge
      className={STATUS_COLORS[status]}
      variant="secondary"
    >
      {status}
    </Badge>
  );
}

function TypeBadge({
  type,
}: {
  type: AnomalyType;
}) {
  const Icon = TYPE_ICONS[type] ?? AlertTriangle;

  return (
    <Badge variant="secondary" className="gap-1">
      <Icon className="size-3" />
      {type}
    </Badge>
  );
}

export default function AnomalyDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const [anomaly, setAnomaly] = useState<AnomalyDetail | null>(null);
  const [investigation, setInvestigation] =
    useState<AIInvestigationResult | null>(null);

  const [loading, setLoading] = useState(true);
  const [investigating, setInvestigating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [activeTab, setActiveTab] = useState("overview");

  const [falsePositiveOpen, setFalsePositiveOpen] = useState(false);
  const [falsePositiveReason, setFalsePositiveReason] = useState("");
  const [falsePositivePending, setFalsePositivePending] = useState(false);

  useEffect(() => {
    loadAnomaly();
  }, []);

  async function loadAnomaly() {
    setLoading(true);
    setError(null);

    try {
      const { id } = await params;
      const data = await fetchAnomaly(id);

      setAnomaly(data);

      // Load existing AI investigation if present.
      if (data.evidence?.investigation) {
        setInvestigation(
          data.evidence.investigation as AIInvestigationResult
        );
      }
    } catch (err) {
      setError("Failed to load anomaly");
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  async function handleAcknowledge() {
    if (!anomaly) return;

    try {
      await acknowledgeAnomaly(anomaly.id);
      await loadAnomaly();
    } catch (err) {
      console.error(err);
      toast.error("Could not acknowledge this anomaly.");
    }
  }

  async function handleResolve() {
    if (!anomaly) return;

    try {
      await resolveAnomaly(anomaly.id);
      await loadAnomaly();
    } catch (err) {
      console.error(err);
      toast.error("Could not resolve this anomaly.");
    }
  }

  async function submitFalsePositive() {
    if (!anomaly) return;

    const reason = falsePositiveReason.trim();

    if (!reason) return;

    setFalsePositivePending(true);

    try {
      await markAnomalyFalsePositive(anomaly.id, reason);
      await loadAnomaly();

      setFalsePositiveOpen(false);
      setFalsePositiveReason("");

      toast.success("Anomaly marked as false positive.");
    } catch (err) {
      console.error(err);
      toast.error("Could not mark this anomaly as a false positive.");
    } finally {
      setFalsePositivePending(false);
    }
  }

  async function handleInvestigate() {
    if (!anomaly) return;

    setInvestigating(true);

    try {
      const result = await investigateAnomaly(anomaly.id);

      setInvestigation(result.result);
      await loadAnomaly();
      setActiveTab("investigation");

      toast.success("AI investigation completed.");
    } catch (err) {
      console.error(err);
      toast.error("Failed to run AI investigation");
    } finally {
      setInvestigating(false);
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <Loader2 className="size-8 animate-spin text-white/44" />
      </div>
    );
  }

  if (error || !anomaly) {
    return (
      <Card className="border-destructive/50 bg-destructive/5">
        <CardContent className="pt-6 pb-4 px-4 text-center">
          <p className="text-destructive">
            {error || "Anomaly not found"}
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <Link
          href="/reliability"
          className="flex items-center gap-2 text-sm text-white/44 hover:text-white/90"
        >
          <ArrowLeft className="size-4" />
          Back to Anomalies
        </Link>

        <div className="flex items-center gap-2">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" className="gap-2">
                <ChevronDown className="size-4" />
                Actions
              </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
              <DropdownMenuLabel>
                Anomaly Actions
              </DropdownMenuLabel>

              <DropdownMenuSeparator />

              {anomaly.status === "OPEN" && (
                <>
                  <DropdownMenuItem onClick={handleAcknowledge}>
                    Acknowledge
                  </DropdownMenuItem>

                  <DropdownMenuItem onClick={handleResolve}>
                    Resolve
                  </DropdownMenuItem>

                  <DropdownMenuItem
                    onClick={() => setFalsePositiveOpen(true)}
                  >
                    Mark false positive
                  </DropdownMenuItem>
                </>
              )}

              {anomaly.status === "ACKNOWLEDGED" && (
                <DropdownMenuItem onClick={handleResolve}>
                  Resolve
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>

          <Button
            onClick={handleInvestigate}
            disabled={investigating}
            className="gap-2"
          >
            <Brain className="size-4" />
            {investigating
              ? "Investigating…"
              : "Investigate with AI"}
          </Button>
        </div>
      </div>

      {/* Main content */}
      <div className="grid min-w-0 gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
        {/* Left column */}
        <div className="min-w-0 space-y-6">
          {/* Header card */}
          <Card className="!bg-[#0a0a0a] border-white/[0.08]">
            <CardContent className="pt-6">
              <div className="flex flex-wrap items-center gap-3 mb-4">
                <TypeBadge type={anomaly.type} />

                <SeverityBadge severity={anomaly.severity} />

                <StatusBadge status={anomaly.status} />

                <Badge
                  variant="outline"
                  className="text-white/44"
                >
                  {anomaly.metric || "N/A"}
                </Badge>
              </div>

              <h1 className="text-2xl font-semibold tracking-tight mb-2 text-white/90">
                Anomaly {anomaly.id.slice(0, 8)}
              </h1>

              <div className="flex flex-wrap items-center gap-4 text-sm text-white/44">
                <Link
                  href={`/workflows/${anomaly.workflowId}`}
                  className="hover:underline font-medium text-white/90"
                >
                  {anomaly.workflowName}
                </Link>

                {anomaly.nodeId && (
                  <>
                    <span>→</span>

                    <code className="max-w-full break-all bg-white/[0.06] px-1.5 py-0.5 rounded text-white/70">
                      {anomaly.nodeId}
                    </code>
                  </>
                )}

                {anomaly.nodeType && (
                  <>
                    <span>→</span>
                    <span className="text-white/44">
                      {anomaly.nodeType}
                    </span>
                  </>
                )}
              </div>
            </CardContent>
          </Card>

          {/* Tabs */}
          <Tabs
            value={activeTab}
            onValueChange={setActiveTab}
            className="w-full"
          >
            <TabsList className="w-full">
              <TabsTrigger
                value="overview"
                className="data-[state=active]:bg-white data-[state=active]:text-[#050505]"
              >
                Overview
              </TabsTrigger>

              <TabsTrigger
                value="evidence"
                className="data-[state=active]:bg-white data-[state=active]:text-[#050505]"
              >
                Evidence
              </TabsTrigger>

              {investigation && (
                <TabsTrigger
                  value="investigation"
                  className="data-[state=active]:bg-white data-[state=active]:text-[#050505]"
                >
                  AI Investigation
                </TabsTrigger>
              )}
            </TabsList>

            {/* OVERVIEW */}
            <TabsContent
              value="overview"
              className="space-y-6 pt-4"
            >
              <div className="grid gap-4 sm:grid-cols-2">
                <Card className="!bg-[#0a0a0a] border-white/[0.08]">
                  <CardHeader>
                    <CardTitle className="text-sm font-medium text-white/90">
                      What Happened
                    </CardTitle>

                    <CardDescription className="text-white/44">
                      Expected vs observed behavior
                    </CardDescription>
                  </CardHeader>

                  <CardContent className="space-y-3">
                    <div>
                      <p className="text-sm text-white/44">
                        Expected
                      </p>

                      <p className="font-mono text-sm text-white/90">
                        {anomaly.expectedValue}
                      </p>
                    </div>

                    <div>
                      <p className="text-sm text-white/44">
                        Observed
                      </p>

                      <p className="font-mono text-sm text-destructive">
                        {anomaly.actualValue}
                      </p>
                    </div>

                    <div>
                      <p className="text-sm text-white/44">
                        Deviation
                      </p>

                      <p className="font-mono text-sm text-white/90">
                        {anomaly.deviation > 0 ? "+" : ""}
                        {anomaly.deviation.toFixed(2)}σ
                      </p>
                    </div>

                    <div>
                      <p className="text-sm text-white/44">
                        Confidence
                      </p>

                      <p className="font-mono text-sm text-white/90">
                        {(anomaly.confidence * 100).toFixed(0)}%
                      </p>
                    </div>
                  </CardContent>
                </Card>

                <Card className="!bg-[#0a0a0a] border-white/[0.08]">
                  <CardHeader>
                    <CardTitle className="text-sm font-medium text-white/90">
                      Impact
                    </CardTitle>

                    <CardDescription className="text-white/44">
                      How many executions are affected
                    </CardDescription>
                  </CardHeader>

                  <CardContent>
                    <p className="text-3xl font-bold text-white/90">
                      {anomaly.affectedExecutions}
                    </p>

                    <p className="text-sm text-white/44">
                      {anomaly.affectedExecutions === 1
                        ? "Single execution"
                        : `${anomaly.affectedExecutions} executions affected`}
                    </p>
                  </CardContent>
                </Card>

                <Card className="sm:col-span-2 !bg-[#0a0a0a] border-white/[0.08]">
                  <CardHeader>
                    <CardTitle className="text-sm font-medium text-white/90">
                      Timeline
                    </CardTitle>

                    <CardDescription className="text-white/44">
                      Detection and update history
                    </CardDescription>
                  </CardHeader>

                  <CardContent className="space-y-2">
                    <div className="flex items-center gap-3 text-sm">
                      <div className="flex items-center gap-2">
                        <div className="size-2 rounded-full bg-blue-500" />
                        <span className="text-white/70">
                          Detected
                        </span>
                      </div>

                      <span className="font-mono text-white/44">
                        {format(
                          new Date(anomaly.detectedAt),
                          "MMM d, yyyy HH:mm:ss"
                        )}
                      </span>
                    </div>

                    <div className="flex items-center gap-3 text-sm">
                      <div className="flex items-center gap-2">
                        <div className="size-2 rounded-full bg-purple-500" />
                        <span className="text-white/70">
                          Last updated
                        </span>
                      </div>

                      <span className="font-mono text-white/44">
                        {format(
                          new Date(anomaly.updatedAt),
                          "MMM d, yyyy HH:mm:ss"
                        )}
                      </span>
                    </div>

                    <div className="flex items-center gap-3 text-sm">
                      <div className="flex items-center gap-2">
                        <div className="size-2 rounded-full bg-gray-500" />
                        <span className="text-white/70">
                          Created
                        </span>
                      </div>

                      <span className="font-mono text-white/44">
                        {format(
                          new Date(anomaly.createdAt),
                          "MMM d, yyyy HH:mm:ss"
                        )}
                      </span>
                    </div>

                    {anomaly.executionId && (
                      <div className="flex items-center gap-3 text-sm">
                        <div className="flex items-center gap-2">
                          <div className="size-2 rounded-full bg-green-500" />
                          <span className="text-white/70">
                            Related execution
                          </span>
                        </div>

                        <Link
                          href={`/executions/${anomaly.executionId}`}
                          className="font-mono text-sm text-white/90 hover:underline"
                        >
                          {anomaly.executionId.slice(0, 8)}…
                        </Link>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </div>
            </TabsContent>

            {/* EVIDENCE */}
            <TabsContent
              value="evidence"
              className="space-y-4 pt-4"
            >
              <Card className="!bg-[#0a0a0a] border-white/[0.08]">
                <CardHeader>
                  <CardTitle className="text-sm font-medium text-white/90">
                    Structured Evidence
                  </CardTitle>

                  <CardDescription className="text-white/44">
                    Machine-readable evidence from the statistical detector
                  </CardDescription>
                </CardHeader>

                <CardContent>
                  <pre className="min-w-0 max-w-full bg-white/[0.03] p-4 rounded-md text-sm font-mono text-white/70 overflow-auto max-h-96 whitespace-pre-wrap break-words [overflow-wrap:anywhere]">
                    {JSON.stringify(
                      anomaly.evidence,
                      null,
                      2
                    )}
                  </pre>
                </CardContent>
              </Card>
            </TabsContent>

            {/* AI INVESTIGATION */}
            {investigation && (
              <TabsContent
                value="investigation"
                className="space-y-6 pt-4"
              >
                <Card className="border-primary/30 bg-primary/5 !bg-[#0a0a0a] !border-primary/20">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-sm font-medium text-white/90">
                      <Brain className="size-4" />
                      AI Investigation Result
                    </CardTitle>

                    <CardDescription className="text-white/44">
                      {investigation.configured
                        ? `Generated by ${investigation.model} at ${format(
                            new Date(investigation.generatedAt),
                            "MMM d, HH:mm"
                          )}`
                        : "AI service not configured — configure a provider key to enable"}

                      <span className="ml-2 inline-flex items-center gap-1 rounded-full bg-white/[0.06] px-2 py-0.5 text-xs">
                        Confidence:{" "}
                        {(investigation.confidence * 100).toFixed(0)}%
                      </span>
                    </CardDescription>
                  </CardHeader>

                  <CardContent className="space-y-6">
                    {/* SUMMARY */}
                    {investigation.summary && (
                      <div className="space-y-2">
                        <h4 className="font-medium flex items-center gap-2 text-white/90">
                          <FileText className="size-4" />
                          Summary
                        </h4>

                        <p className="text-sm text-white/70">
                          {investigation.summary}
                        </p>
                      </div>
                    )}

                    {/* LIKELY CAUSES */}
                    {investigation.likelyCauses &&
                      investigation.likelyCauses.length > 0 && (
                        <div className="space-y-2">
                          <h4 className="font-medium flex items-center gap-2 text-white/90">
                            <Target className="size-4" />
                            Likely Causes
                          </h4>

                          <ul className="space-y-2">
                            {investigation.likelyCauses.map(
                              (cause, i) => (
                                <li
                                  key={i}
                                  className="text-sm border-l-2 border-primary/30 pl-3"
                                >
                                  <p className="font-medium text-white/90">
                                    {cause.cause}
                                  </p>

                                  <p className="text-xs text-white/44">
                                    Category: {cause.category} •
                                    Confidence:{" "}
                                    {(
                                      cause.confidence * 100
                                    ).toFixed(0)}
                                    %
                                  </p>

                                  {cause.uncertainty && (
                                    <p className="text-xs text-amber-400">
                                      Uncertainty:{" "}
                                      {cause.uncertainty}
                                    </p>
                                  )}
                                </li>
                              )
                            )}
                          </ul>
                        </div>
                      )}

                    {/* EVIDENCE */}
                    {investigation.evidence &&
                      investigation.evidence.length > 0 && (
                        <div className="space-y-2">
                          <h4 className="font-medium flex items-center gap-2 text-white/90">
                            <Shield className="size-4" />
                            Evidence
                          </h4>

                          <ul className="space-y-2">
                            {investigation.evidence.map(
                              (item, i) => (
                                <li
                                  key={i}
                                  className="text-sm border-l-2 border-green-500/30 pl-3"
                                >
                                  <p className="font-medium text-white/90">
                                    {item.type}:{" "}
                                    {item.description}
                                  </p>

                                  <p className="text-xs text-white/44">
                                    Source: {item.source}
                                  </p>

                                  {item.inference && (
                                    <p className="text-xs text-blue-400">
                                      Inference:{" "}
                                      {item.inference}
                                    </p>
                                  )}
                                </li>
                              )
                            )}
                          </ul>
                        </div>
                      )}

                    {/* IMPACT */}
                    {investigation.impact && (
                      <div className="space-y-2 border-l-2 border-amber-500/30 pl-3">
                        <h4 className="font-medium flex items-center gap-2 text-white/90">
                          <AlertTriangle className="size-4" />
                          Impact
                        </h4>

                        <p className="text-sm text-white/70">
                          {investigation.impact}
                        </p>
                      </div>
                    )}

                    {/* RECOMMENDED ACTIONS */}
                    {investigation.recommendedActions &&
                      investigation.recommendedActions.length > 0 && (
                        <div className="space-y-2">
                          <h4 className="font-medium flex items-center gap-2 text-white/90">
                            <Zap className="size-4" />
                            Recommended Actions
                          </h4>

                          <ul className="space-y-2">
                            {investigation.recommendedActions.map(
                              (action, i) => (
                                <li
                                  key={i}
                                  className="text-sm border-l-2 border-purple-500/30 pl-3"
                                >
                                  <p className="font-medium text-white/90">
                                    {action.action}
                                  </p>

                                  <p className="text-xs text-white/44">
                                    Rationale:{" "}
                                    {action.rationale}
                                  </p>

                                  <div className="flex gap-4 mt-1 text-xs text-white/44">
                                    <span>
                                      Risk: {action.risk}
                                    </span>

                                    <span>
                                      Effort: {action.effort}
                                    </span>
                                  </div>
                                </li>
                              )
                            )}
                          </ul>
                        </div>
                      )}
                  </CardContent>
                </Card>
              </TabsContent>
            )}
          </Tabs>
        </div>

        {/* Right column */}
        <div className="min-w-0 space-y-4">
          <Card className="!bg-[#0a0a0a] border-white/[0.08]">
            <CardHeader>
              <CardTitle className="text-sm font-medium text-white/90">
                Key Facts
              </CardTitle>
            </CardHeader>

            <CardContent className="space-y-3 text-sm">
              <dl className="grid gap-2 grid-cols-[auto_1fr]">
                <dt className="text-white/44">
                  Anomaly ID
                </dt>

                <dd className="font-mono text-white/90 break-all">
                  {anomaly.id}
                </dd>

                <dt className="text-white/44">
                  Dedup Key
                </dt>

                <dd className="min-w-0 font-mono text-xs break-all text-white/70">
                  {anomaly.dedupKey}
                </dd>

                <dt className="text-white/44">
                  Affected Executions
                </dt>

                <dd className="text-white/90">
                  {anomaly.affectedExecutions}
                </dd>

                <dt className="text-white/44">
                  Deviation
                </dt>

                <dd className="font-mono text-white/90">
                  {anomaly.deviation.toFixed(2)}σ
                </dd>

                <dt className="text-white/44">
                  Confidence
                </dt>

                <dd className="text-white/90">
                  {(anomaly.confidence * 100).toFixed(0)}%
                </dd>
              </dl>
            </CardContent>
          </Card>

          <Card className="!bg-[#0a0a0a] border-white/[0.08]">
            <CardHeader>
              <CardTitle className="text-sm font-medium text-white/90">
                Quick Actions
              </CardTitle>
            </CardHeader>

            <CardContent className="space-y-2">
              {anomaly.status === "OPEN" && (
                <>
                  <Button
                    variant="outline"
                    className="w-full justify-start"
                    onClick={handleAcknowledge}
                  >
                    Acknowledge
                  </Button>

                  <Button
                    variant="default"
                    className="w-full justify-start"
                    onClick={handleResolve}
                  >
                    Resolve
                  </Button>

                  <Button
                    variant="destructive"
                    className="w-full justify-start"
                    onClick={() =>
                      setFalsePositiveOpen(true)
                    }
                  >
                    Mark False Positive
                  </Button>
                </>
              )}

              {anomaly.status === "ACKNOWLEDGED" && (
                <Button
                  variant="default"
                  className="w-full justify-start"
                  onClick={handleResolve}
                >
                  Resolve
                </Button>
              )}

              <Button
                variant="outline"
                className="w-full justify-start"
                onClick={handleInvestigate}
                disabled={investigating}
              >
                {investigating
                  ? "Investigating…"
                  : "Run AI Investigation"}
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* False positive dialog */}
      <Dialog
        open={falsePositiveOpen}
        onOpenChange={setFalsePositiveOpen}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              Mark as false positive
            </DialogTitle>

            <DialogDescription>
              Record why this anomaly is not a real issue.
              This is kept for the audit trail and helps the
              detector avoid repeating it.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-2">
            <Label htmlFor="fp-reason">
              Reason
            </Label>

            <Input
              id="fp-reason"
              autoFocus
              value={falsePositiveReason}
              onChange={(e) =>
                setFalsePositiveReason(e.target.value)
              }
              placeholder="e.g. Expected during deploy window"
            />
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setFalsePositiveOpen(false);
                setFalsePositiveReason("");
              }}
            >
              Cancel
            </Button>

            <Button
              type="button"
              onClick={submitFalsePositive}
              disabled={
                !falsePositiveReason.trim() ||
                falsePositivePending
              }
            >
              {falsePositivePending
                ? "Saving…"
                : "Mark as false positive"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}