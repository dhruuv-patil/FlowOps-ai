"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  AlertTriangle,
  ChevronDown,
  Loader2,
  Search,
  ExternalLink,
} from "lucide-react";
import { toast } from "sonner";

import {
  fetchAnomalies,
  acknowledgeAnomaly,
  resolveAnomaly,
  markAnomalyFalsePositive,
} from "@/lib/api";

import type {
  AnomaliesResponse,
  AnomalySummary,
  AnomalyStatus,
  AnomalySeverity,
} from "@/types";

import { format } from "date-fns";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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

const STATUS_COLORS: Record<AnomalyStatus, string> = {
  OPEN: "bg-blue-500/10 text-blue-400 border-blue-500/20",
  ACKNOWLEDGED: "bg-purple-500/10 text-purple-400 border-purple-500/20",
  VERIFYING_RECOVERY: "bg-amber-500/10 text-amber-400 border-amber-500/20",
  RESOLVED: "bg-green-500/10 text-green-400 border-green-500/20",
  FALSE_POSITIVE: "bg-gray-500/10 text-gray-400 border-gray-500/20",
};

const TYPE_ICONS: Record<string, React.ComponentType<{ className?: string }>> = {
  LATENCY: AlertTriangle,
  VOLUME: AlertTriangle,
  OUTPUT: AlertTriangle,
  BEHAVIORAL: AlertTriangle,
};

function SeverityBadge({ severity }: { severity: AnomalySeverity }) {
  return (
    <Badge className={SEVERITY_COLORS[severity]} variant="secondary">
      {severity}
    </Badge>
  );
}

function StatusBadge({ status }: { status: AnomalyStatus }) {
  return (
    <Badge className={STATUS_COLORS[status]} variant="secondary">
      {status.replace("_", " ")}
    </Badge>
  );
}

function TypeBadge({ type }: { type: string }) {
  const Icon = TYPE_ICONS[type] || AlertTriangle;
  return (
    <Badge variant="secondary" className="text-xs gap-1">
      <Icon className="size-3" />
      {type}
    </Badge>
  );
}

interface WorkflowAnomaliesViewProps {
  workflowId: string;
}

export function WorkflowAnomaliesView({ workflowId }: WorkflowAnomaliesViewProps) {
  const [anomalies, setAnomalies] = useState<AnomalySummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [searchTerm, setSearchTerm] = useState("");
  const [falsePositiveId, setFalsePositiveId] = useState<string | null>(null);
  const [falsePositiveReason, setFalsePositiveReason] = useState("");
  const [actionPending, setActionPending] = useState(false);

  useEffect(() => {
    loadAnomalies();
  }, [statusFilter]);

  async function runAction(
    action: (id: string) => Promise<unknown>,
    id: string
  ) {
    setActionPending(true);

    try {
      await action(id);
      toast.success("Anomaly updated.");
      await loadAnomalies();
    } catch (err) {
      console.error(err);
      toast.error("Could not update the anomaly.");
    } finally {
      setActionPending(false);
    }
  }

  async function submitFalsePositive() {
    if (!falsePositiveId) return;

    const reason = falsePositiveReason.trim();

    if (!reason) return;

    setActionPending(true);

    try {
      await markAnomalyFalsePositive(falsePositiveId, reason);

      toast.success("Anomaly marked as false positive.");

      await loadAnomalies();

      setFalsePositiveId(null);
      setFalsePositiveReason("");
    } catch (err) {
      console.error(err);
      toast.error("Could not update the anomaly.");
    } finally {
      setActionPending(false);
    }
  }

  async function loadAnomalies() {
    setLoading(true);
    setError(null);

    try {
      const response: AnomaliesResponse = await fetchAnomalies(
        {
          workflowId,
          status: statusFilter ? (statusFilter as AnomalyStatus) : undefined,
        }
      );

      setAnomalies(response.anomalies);
    } catch (err) {
      setError("Failed to load anomalies");
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  const filteredAnomalies = anomalies.filter((a) => {
    if (searchTerm) {
      const search = searchTerm.toLowerCase();

      return (
        a.workflowName.toLowerCase().includes(search) ||
        a.nodeId?.toLowerCase().includes(search) ||
        a.type.toLowerCase().includes(search) ||
        a.metric?.toLowerCase().includes(search)
      );
    }

    return true;
  });

  return (
    <div className="space-y-4 pt-4">
      {/* Header with search/filter */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-sm font-medium text-white/80">Anomalies</p>
          <p className="mt-0.5 text-xs text-white/30">
            {filteredAnomalies.length} of {anomalies.length} anomalies shown
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="relative max-w-sm">
            <Input
              placeholder="Search anomalies…"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-9"
            />
            <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-white/44" />
          </div>

          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder="All statuses" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">All statuses</SelectItem>
              <SelectItem value="OPEN">Open</SelectItem>
              <SelectItem value="ACKNOWLEDGED">Acknowledged</SelectItem>
              <SelectItem value="VERIFYING_RECOVERY">Verifying Recovery</SelectItem>
              <SelectItem value="RESOLVED">Resolved</SelectItem>
              <SelectItem value="FALSE_POSITIVE">False Positive</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Error */}
      {error && (
        <Card className="border-destructive/50 bg-destructive/5">
          <CardContent className="px-4 pb-4 pt-6">
            <p className="text-destructive">{error}</p>
            <Button variant="outline" size="sm" onClick={loadAnomalies} className="mt-2">
              Retry
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Main card */}
      <Card className="!bg-[#0a0a0a] border-white/[0.08]">
        <CardContent className="p-0">
          {loading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="size-8 animate-spin text-white/44" />
            </div>
          ) : filteredAnomalies.length === 0 ? (
            <div className="rounded-xl border border-dashed border-white/[0.10] py-16">
              <div className="flex w-full flex-col items-center justify-center text-center">
                <AlertTriangle className="mb-4 size-12 text-white/20" />
                <p className="text-white/44">
                  {anomalies.length === 0
                    ? "No anomalies detected for this workflow. Baselines need warm-up data."
                    : "No anomalies match your filters."}
                </p>
              </div>
            </div>
          ) : (
            <div className="divide-y divide-white/[0.06]">
              {filteredAnomalies.map((anomaly) => (
                <AnomalyRow
                  key={anomaly.id}
                  anomaly={anomaly}
                  actionPending={actionPending}
                  onAction={runAction}
                  onFalsePositive={() => setFalsePositiveId(anomaly.id)}
                />
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* False positive dialog */}
      {falsePositiveId && (
        <Dialog
          open
          onOpenChange={() => setFalsePositiveId(null)}
        >
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Mark as false positive</DialogTitle>
              <DialogDescription>
                Record why this anomaly is not a real issue. This is kept for the audit trail
                and helps the detector avoid repeating it.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-2">
              <Label htmlFor="fp-reason">Reason</Label>
              <Input
                id="fp-reason"
                autoFocus
                value={falsePositiveReason}
                onChange={(e) => setFalsePositiveReason(e.target.value)}
                placeholder="e.g. Expected during deploy window"
              />
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="ghost"
                onClick={() => {
                  setFalsePositiveId(null);
                  setFalsePositiveReason("");
                }}
              >
                Cancel
              </Button>

              <Button
                type="button"
                onClick={submitFalsePositive}
                disabled={!falsePositiveReason.trim() || actionPending}
              >
                {actionPending ? "Saving…" : "Mark as false positive"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}

function AnomalyRow({
  anomaly,
  actionPending,
  onAction,
  onFalsePositive,
}: {
  anomaly: AnomalySummary;
  actionPending: boolean;
  onAction: (action: (id: string) => Promise<unknown>, id: string) => void;
  onFalsePositive: () => void;
}) {
  // AnomalySummary doesn't have evidence, only AnomalyDetail does
  // We'll show metric info instead for the summary view
  const hasMetricInfo = anomaly.metric !== null;

  return (
    <div className="p-4 hover:bg-white/[0.02] transition-colors">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex flex-1 gap-3 min-w-0">
          {/* Type & Severity */}
          <div className="flex flex-col gap-1.5 shrink-0">
            <TypeBadge type={anomaly.type} />
            <SeverityBadge severity={anomaly.severity} />
          </div>

          {/* Main content */}
          <div className="min-w-0 flex-1">
            <div className="flex items-start gap-3">
              <div className="min-w-0 flex-1">
                <Link
                  href={`/workflows/${anomaly.workflowId}`}
                  className="font-medium text-white/90 hover:underline truncate block"
                >
                  {anomaly.workflowName}
                </Link>

                <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-white/44">
                  <span className="font-mono">{anomaly.id.slice(0, 8)}</span>

                  {anomaly.nodeId && (
                    <>
                      <span>·</span>
                      <span className="font-mono">{anomaly.nodeId}</span>
                    </>
                  )}

                  {anomaly.nodeType && (
                    <>
                      <span>·</span>
                      <span className="capitalize">{anomaly.nodeType.replace("_", " ")}</span>
                    </>
                  )}

                  <span>·</span>
                  <span>{format(new Date(anomaly.detectedAt), "MMM d, HH:mm")}</span>
                </div>
              </div>

              <StatusBadge status={anomaly.status} />
            </div>

            {/* Evidence / Details */}
            {/* Metric info for latency/output anomalies */}
            {hasMetricInfo && (
              <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-white/50">
                <span className="font-mono">
                  {anomaly.metric}: {anomaly.expectedValue} → {anomaly.actualValue}
                </span>
                <span className="px-2 py-0.5 rounded border border-white/[0.1] bg-white/[0.02]">
                  {anomaly.deviation > 0 ? "+" : ""}{anomaly.deviation.toFixed(1)}σ
                </span>
                <span className="px-2 py-0.5 rounded border border-white/[0.1] bg-white/[0.02]">
                  {Math.round(anomaly.confidence * 100)}% confidence
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-1 sm:ml-4">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-8 w-8">
                <ChevronDown className="size-4" />
              </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
              <DropdownMenuLabel>
                Anomaly {anomaly.id.slice(0, 8)}
              </DropdownMenuLabel>

              <DropdownMenuSeparator />

              <DropdownMenuItem asChild className="flex items-center gap-2">
                <Link href={`/reliability/${anomaly.id}`}>
                  <ExternalLink className="size-3.5" />
                  View details
                </Link>
              </DropdownMenuItem>

              {anomaly.status === "OPEN" && (
                <>
                  <DropdownMenuSeparator />

                  <DropdownMenuItem
                    onClick={() => onAction(acknowledgeAnomaly, anomaly.id)}
                    disabled={actionPending}
                  >
                    Acknowledge
                  </DropdownMenuItem>

                  <DropdownMenuItem
                    onClick={() => onAction(resolveAnomaly, anomaly.id)}
                    disabled={actionPending}
                  >
                    Resolve
                  </DropdownMenuItem>

                  <DropdownMenuItem
                    onClick={onFalsePositive}
                    disabled={actionPending}
                  >
                    Mark false positive
                  </DropdownMenuItem>
                </>
              )}

              {anomaly.status === "ACKNOWLEDGED" && (
                <>
                  <DropdownMenuSeparator />

                  <DropdownMenuItem
                    onClick={() => onAction(resolveAnomaly, anomaly.id)}
                    disabled={actionPending}
                  >
                    Resolve
                  </DropdownMenuItem>
                </>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </div>
  );
}