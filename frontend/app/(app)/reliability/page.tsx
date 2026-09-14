"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  AlertTriangle,
  ChevronDown,
  Loader2,
  Search,
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";

import { Badge } from "@/components/ui/badge";
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

const STATUS_COLORS: Record<AnomalyStatus, string> = {
  OPEN: "bg-blue-500/10 text-blue-400 border-blue-500/20",
  ACKNOWLEDGED:
    "bg-purple-500/10 text-purple-400 border-purple-500/20",
  RESOLVED:
    "bg-green-500/10 text-green-400 border-green-500/20",
  FALSE_POSITIVE:
    "bg-gray-500/10 text-gray-400 border-gray-500/20",
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

export default function AnomaliesPage() {
  const [anomalies, setAnomalies] = useState<AnomalySummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [searchTerm, setSearchTerm] = useState("");
  const [falsePositiveId, setFalsePositiveId] =
    useState<string | null>(null);
  const [falsePositiveReason, setFalsePositiveReason] =
    useState("");
  const [actionPending, setActionPending] =
    useState(false);

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
      await markAnomalyFalsePositive(
        falsePositiveId,
        reason
      );

      toast.success(
        "Anomaly marked as false positive."
      );

      await loadAnomalies();

      setFalsePositiveId(null);
      setFalsePositiveReason("");
    } catch (err) {
      console.error(err);
      toast.error(
        "Could not update the anomaly."
      );
    } finally {
      setActionPending(false);
    }
  }

  async function loadAnomalies() {
    setLoading(true);
    setError(null);

    try {
      const response: AnomaliesResponse =
        await fetchAnomalies(
          statusFilter
            ? {
                status:
                  statusFilter as AnomalyStatus,
              }
            : undefined
        );

      setAnomalies(response.anomalies);
    } catch (err) {
      setError("Failed to load anomalies");
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  const filteredAnomalies = anomalies.filter(
    (a) => {
      if (searchTerm) {
        const search =
          searchTerm.toLowerCase();

        return (
          a.workflowName
            .toLowerCase()
            .includes(search) ||
          a.nodeId
            ?.toLowerCase()
            .includes(search) ||
          a.type
            .toLowerCase()
            .includes(search) ||
          a.metric
            ?.toLowerCase()
            .includes(search)
        );
      }

      return true;
    }
  );

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="mono-eyebrow">
            Reliability
          </p>

          <h1 className="mt-1 text-xl font-semibold tracking-tight text-white/90">
            Anomalies
          </h1>

          <p className="mt-1 text-sm text-white/44">
            Detected reliability anomalies across
            your workflows
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="relative max-w-sm">
            <Input
              placeholder="Search anomalies…"
              value={searchTerm}
              onChange={(e) =>
                setSearchTerm(e.target.value)
              }
              className="pl-9"
            />

            <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-white/44" />
          </div>

          <Select
            value={statusFilter}
            onValueChange={setStatusFilter}
          >
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder="All statuses" />
            </SelectTrigger>

            <SelectContent>
              <SelectItem value="">
                All statuses
              </SelectItem>

              <SelectItem value="OPEN">
                Open
              </SelectItem>

              <SelectItem value="ACKNOWLEDGED">
                Acknowledged
              </SelectItem>

              <SelectItem value="RESOLVED">
                Resolved
              </SelectItem>

              <SelectItem value="FALSE_POSITIVE">
                False Positive
              </SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Error */}
      {error && (
        <Card className="border-destructive/50 bg-destructive/5">
          <CardContent className="px-4 pb-4 pt-6">
            <p className="text-destructive">
              {error}
            </p>

            <Button
              variant="outline"
              size="sm"
              onClick={loadAnomalies}
              className="mt-2"
            >
              Retry
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Main card */}
      <Card className="!bg-[#0a0a0a] border-white/[0.08]">
        <CardHeader>
          <CardTitle className="text-white/90">
            Detected Anomalies
          </CardTitle>

          <CardDescription className="text-white/44">
            {filteredAnomalies.length} of{" "}
            {anomalies.length} anomalies shown
          </CardDescription>
        </CardHeader>

        <CardContent>
          {loading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="size-8 animate-spin text-white/44" />
            </div>
          ) : filteredAnomalies.length === 0 ? (
            /*
             * FIXED EMPTY STATE
             *
             * The icon is inside an explicit flex container
             * with items-center, so it cannot sit on the left.
             */
            <div className="rounded-xl border border-dashed border-white/[0.10] py-16">
              <div className="flex w-full flex-col items-center justify-center text-center">
                <AlertTriangle className="mb-4 size-12 text-white/20" />

                <p className="text-white/44">
                  {anomalies.length === 0
                    ? "No anomalies detected yet. Baselines need warm-up data."
                    : "No anomalies match your filters."}
                </p>
              </div>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-8" />
                    <TableHead>
                      Workflow
                    </TableHead>
                    <TableHead className="hidden md:table-cell">
                      Node
                    </TableHead>
                    <TableHead className="hidden lg:table-cell">
                      Type
                    </TableHead>
                    <TableHead>
                      Severity
                    </TableHead>
                    <TableHead>
                      Status
                    </TableHead>
                    <TableHead className="hidden md:table-cell">
                      Detected
                    </TableHead>
                    <TableHead className="w-24 text-right">
                      Actions
                    </TableHead>
                  </TableRow>
                </TableHeader>

                <TableBody>
                  {filteredAnomalies.map(
                    (anomaly) => (
                      <TableRow
                        key={anomaly.id}
                        className="hover:bg-white/[0.03]"
                      >
                        <TableCell className="font-mono text-xs text-white/44">
                          {anomaly.id.slice(0, 8)}
                        </TableCell>

                        <TableCell>
                          <Link
                            href={`/workflows/${anomaly.workflowId}`}
                            className="font-medium text-white/90 hover:underline"
                          >
                            {anomaly.workflowName}
                          </Link>
                        </TableCell>

                        <TableCell className="hidden md:table-cell">
                          {anomaly.nodeId ? (
                            <code className="text-sm font-mono text-white/70">
                              {anomaly.nodeId}
                            </code>
                          ) : (
                            <span className="text-white/20">
                              —
                            </span>
                          )}
                        </TableCell>

                        <TableCell className="hidden lg:table-cell">
                          <Badge
                            variant="secondary"
                            className="text-xs"
                          >
                            {anomaly.type}
                          </Badge>
                        </TableCell>

                        <TableCell>
                          <SeverityBadge
                            severity={
                              anomaly.severity
                            }
                          />
                        </TableCell>

                        <TableCell>
                          <StatusBadge
                            status={
                              anomaly.status
                            }
                          />
                        </TableCell>

                        <TableCell className="hidden text-white/44 md:table-cell">
                          {format(
                            new Date(
                              anomaly.detectedAt
                            ),
                            "MMM d, HH:mm"
                          )}
                        </TableCell>

                        <TableCell className="text-right">
                          <DropdownMenu>
                            <DropdownMenuTrigger
                              asChild
                            >
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-8 w-8"
                              >
                                <ChevronDown className="size-4" />
                              </Button>
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                              <DropdownMenuLabel>
                                Anomaly{" "}
                                {anomaly.id.slice(
                                  0,
                                  8
                                )}
                              </DropdownMenuLabel>

                              <DropdownMenuSeparator />

                              <DropdownMenuItem
                                asChild
                                className="flex items-center gap-2"
                              >
                                <Link
                                  href={`/reliability/${anomaly.id}`}
                                >
                                  View details
                                </Link>
                              </DropdownMenuItem>

                              {anomaly.status ===
                                "OPEN" && (
                                <>
                                  <DropdownMenuSeparator />

                                  <DropdownMenuItem
                                    onClick={() =>
                                      runAction(
                                        acknowledgeAnomaly,
                                        anomaly.id
                                      )
                                    }
                                    disabled={
                                      actionPending
                                    }
                                  >
                                    Acknowledge
                                  </DropdownMenuItem>

                                  <DropdownMenuItem
                                    onClick={() =>
                                      runAction(
                                        resolveAnomaly,
                                        anomaly.id
                                      )
                                    }
                                    disabled={
                                      actionPending
                                    }
                                  >
                                    Resolve
                                  </DropdownMenuItem>

                                  <DropdownMenuItem
                                    onClick={() =>
                                      setFalsePositiveId(
                                        anomaly.id
                                      )
                                    }
                                    disabled={
                                      actionPending
                                    }
                                  >
                                    Mark false positive
                                  </DropdownMenuItem>
                                </>
                              )}

                              {anomaly.status ===
                                "ACKNOWLEDGED" && (
                                <>
                                  <DropdownMenuSeparator />

                                  <DropdownMenuItem
                                    onClick={() =>
                                      runAction(
                                        resolveAnomaly,
                                        anomaly.id
                                      )
                                    }
                                    disabled={
                                      actionPending
                                    }
                                  >
                                    Resolve
                                  </DropdownMenuItem>
                                </>
                              )}
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    )
                  )}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* False positive dialog */}
      {falsePositiveId && (
        <Dialog
          open
          onOpenChange={() =>
            setFalsePositiveId(null)
          }
        >
          <DialogContent>
            <DialogHeader>
              <DialogTitle>
                Mark as false positive
              </DialogTitle>

              <DialogDescription>
                Record why this anomaly is not a real
                issue. This is kept for the audit trail
                and helps the detector avoid repeating it.
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
                  setFalsePositiveReason(
                    e.target.value
                  )
                }
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
                onClick={
                  submitFalsePositive
                }
                disabled={
                  !falsePositiveReason.trim() ||
                  actionPending
                }
              >
                {actionPending
                  ? "Saving…"
                  : "Mark as false positive"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}