"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { MoreHorizontal, Search, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { format, formatDistanceToNowStrict, isValid } from "date-fns";

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

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

/* -------------------------------------------------------------------------- */
/* Display config                                                             */
/* -------------------------------------------------------------------------- */

const ALL = "all" as const;

type StatusFilter = AnomalyStatus | typeof ALL;

const REASON_MAX_LENGTH = 500;

const SEVERITY: Record<
  AnomalySeverity,
  { label: string; dot: string; text: string }
> = {
  LOW: {
    label: "Low",
    dot: "bg-green-400",
    text: "text-white/60",
  },
  MEDIUM: {
    label: "Medium",
    dot: "bg-yellow-400",
    text: "text-white/70",
  },
  HIGH: {
    label: "High",
    dot: "bg-orange-400",
    text: "text-white/80",
  },
  CRITICAL: {
    label: "Critical",
    dot: "bg-red-400",
    text: "text-red-300",
  },
};

const STATUS: Record<
  AnomalyStatus,
  { label: string; className: string }
> = {
  OPEN: {
    label: "Open",
    className: "bg-blue-500/10 text-blue-400 border-blue-500/20",
  },
  ACKNOWLEDGED: {
    label: "Acknowledged",
    className: "bg-purple-500/10 text-purple-400 border-purple-500/20",
  },
  VERIFYING_RECOVERY: {
    label: "Verifying recovery",
    className: "bg-amber-500/10 text-amber-400 border-amber-500/20",
  },
  RESOLVED: {
    label: "Resolved",
    className: "bg-green-500/10 text-green-400 border-green-500/20",
  },
  FALSE_POSITIVE: {
    label: "False positive",
    className: "bg-gray-500/10 text-gray-400 border-gray-500/20",
  },
};

const STATUS_OPTIONS = Object.keys(STATUS) as AnomalyStatus[];

/* -------------------------------------------------------------------------- */
/* Small presentational pieces                                                */
/* -------------------------------------------------------------------------- */

function SeverityLabel({
  severity,
}: {
  severity: AnomalySeverity;
}) {
  const config = SEVERITY[severity];

  if (!config) {
    return (
      <span className="text-white/44">
        {severity}
      </span>
    );
  }

  return (
    <span
      className={`inline-flex items-center gap-2 text-sm ${config.text}`}
    >
      <span
        className={`size-1.5 rounded-full ${config.dot}`}
        aria-hidden
      />
      {config.label}
    </span>
  );
}

function StatusBadge({
  status,
}: {
  status: AnomalyStatus;
}) {
  const config = STATUS[status];

  return (
    <Badge
      variant="secondary"
      className={config?.className ?? "text-white/60"}
    >
      {config?.label ?? status}
    </Badge>
  );
}

function DetectedAt({
  value,
}: {
  value: string;
}) {
  const date = new Date(value);

  if (!isValid(date)) {
    return (
      <span className="text-white/20">
        —
      </span>
    );
  }

  return (
    <time
      dateTime={date.toISOString()}
      title={format(date, "MMM d, yyyy 'at' HH:mm")}
      className="whitespace-nowrap"
    >
      {formatDistanceToNowStrict(date, {
        addSuffix: true,
      })}
    </time>
  );
}

function SkeletonRows() {
  return (
    <div
      className="divide-y divide-white/[0.06]"
      aria-hidden
    >
      {Array.from({ length: 6 }).map((_, i) => (
        <div
          key={i}
          className="flex items-center gap-6 px-4 py-4"
        >
          <div className="h-4 w-40 animate-pulse rounded bg-white/[0.06]" />

          <div className="hidden h-4 w-24 animate-pulse rounded bg-white/[0.06] lg:block" />

          <div className="h-4 w-16 animate-pulse rounded bg-white/[0.06]" />

          <div className="h-5 w-20 animate-pulse rounded-full bg-white/[0.06]" />

          <div className="ml-auto hidden h-4 w-20 animate-pulse rounded bg-white/[0.06] md:block" />
        </div>
      ))}
    </div>
  );
}

/* -------------------------------------------------------------------------- */
/* Page                                                                       */
/* -------------------------------------------------------------------------- */

export default function AnomaliesPage() {
  const [anomalies, setAnomalies] = useState<AnomalySummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [statusFilter, setStatusFilter] =
    useState<StatusFilter>(ALL);

  const [searchTerm, setSearchTerm] = useState("");

  // Row-level pending state, so one action doesn't lock the whole table.
  const [pendingId, setPendingId] = useState<string | null>(null);

  // False-positive dialog
  const [fpTarget, setFpTarget] =
    useState<AnomalySummary | null>(null);

  const [fpReason, setFpReason] = useState("");
  const [fpSubmitting, setFpSubmitting] = useState(false);

  // Guards against out-of-order responses when filters change quickly.
  const requestId = useRef(0);

  const loadAnomalies = useCallback(
    async (options?: { silent?: boolean }) => {
      const id = ++requestId.current;

      if (!options?.silent) {
        setLoading(true);
      }

      setError(null);

      try {
        const response: AnomaliesResponse =
          await fetchAnomalies(
            statusFilter === ALL
              ? undefined
              : { status: statusFilter }
          );

        if (id !== requestId.current) {
          return;
        }

        setAnomalies(response.anomalies ?? []);
      } catch (err) {
        if (id !== requestId.current) {
          return;
        }

        console.error("Failed to load anomalies", err);

        setError(
          "We couldn't load anomalies. Check your connection and try again."
        );
      } finally {
        if (id === requestId.current) {
          setLoading(false);
        }
      }
    },
    [statusFilter]
  );

  useEffect(() => {
    void loadAnomalies();

    return () => {
      // Invalidate any in-flight request on filter change / unmount.
      requestId.current++;
    };
  }, [loadAnomalies]);

  const visibleAnomalies = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();

    const matches = query
      ? anomalies.filter((a) =>
          [
            a.workflowName,
            a.nodeId,
            a.type,
            a.metric,
          ].some((field) =>
            field?.toLowerCase().includes(query)
          )
        )
      : anomalies;

    return [...matches].sort(
      (a, b) =>
        new Date(b.detectedAt).getTime() -
        new Date(a.detectedAt).getTime()
    );
  }, [anomalies, searchTerm]);

  const isFiltered =
    searchTerm.trim() !== "" ||
    statusFilter !== ALL;

  const isInitialLoad =
    loading && anomalies.length === 0;

  function clearFilters() {
    setSearchTerm("");
    setStatusFilter(ALL);
  }

  async function runAction(
    action: (id: string) => Promise<unknown>,
    id: string,
    successMessage: string
  ) {
    setPendingId(id);

    try {
      await action(id);

      toast.success(successMessage);

      await loadAnomalies({
        silent: true,
      });
    } catch (err) {
      console.error(err);

      toast.error(
        "Couldn't update the anomaly. Try again."
      );
    } finally {
      setPendingId(null);
    }
  }

  function closeFalsePositiveDialog() {
    setFpTarget(null);
    setFpReason("");
  }

  async function submitFalsePositive(
    e: React.FormEvent
  ) {
    e.preventDefault();

    const reason = fpReason.trim();

    if (!fpTarget || !reason || fpSubmitting) {
      return;
    }

    setFpSubmitting(true);

    try {
      await markAnomalyFalsePositive(
        fpTarget.id,
        reason
      );

      toast.success(
        "Marked as false positive"
      );

      closeFalsePositiveDialog();

      await loadAnomalies({
        silent: true,
      });
    } catch (err) {
      console.error(err);

      toast.error(
        "Couldn't update the anomaly. Try again."
      );
    } finally {
      setFpSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6 pt-10">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight text-white/90">
            Anomalies
          </h1>

          <p className="mt-1 text-sm text-white/44">
            Unusual behavior detected in your workflow runs.
          </p>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative w-full sm:w-64">
            <Label
              htmlFor="anomaly-search"
              className="sr-only"
            >
              Search anomalies
            </Label>

            <Search
              className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-white/44"
              aria-hidden
            />

            <Input
              id="anomaly-search"
              type="search"
              placeholder="Search workflow, node, or type"
              value={searchTerm}
              onChange={(e) =>
                setSearchTerm(e.target.value)
              }
              className="pl-9"
            />
          </div>

          <Select
            value={statusFilter}
            onValueChange={(value) =>
              setStatusFilter(
                value as StatusFilter
              )
            }
          >
            <SelectTrigger
              className="w-full sm:w-[180px]"
              aria-label="Filter by status"
            >
              <SelectValue placeholder="All statuses" />
            </SelectTrigger>

            <SelectContent>
              <SelectItem value={ALL}>
                All statuses
              </SelectItem>

              {STATUS_OPTIONS.map((status) => (
                <SelectItem
                  key={status}
                  value={status}
                >
                  {STATUS[status].label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Error */}
      {error && (
        <div
          role="alert"
          className="flex items-center justify-between gap-4 rounded-xl border border-red-500/20 bg-red-500/[0.06] px-4 py-3"
        >
          <p className="text-sm text-red-300">
            {error}
          </p>

          <Button
            variant="outline"
            size="sm"
            onClick={() =>
              void loadAnomalies()
            }
          >
            Try again
          </Button>
        </div>
      )}

      {/* Table */}
      <div className="overflow-hidden rounded-xl border border-white/[0.08] bg-[#0a0a0a]">
        {isInitialLoad ? (
          <SkeletonRows />
        ) : visibleAnomalies.length === 0 ? (
          !error && (
            <div className="flex flex-col items-center justify-center px-6 py-20 text-center">
              <ShieldCheck
                className="mb-4 size-8 text-white/20"
                aria-hidden
              />

              {isFiltered ? (
                <>
                  <p className="text-sm font-medium text-white/80">
                    No anomalies match your filters
                  </p>

                  <Button
                    variant="ghost"
                    size="sm"
                    className="mt-3"
                    onClick={clearFilters}
                  >
                    Clear filters
                  </Button>
                </>
              ) : (
                <>
                  <p className="text-sm font-medium text-white/80">
                    No anomalies detected
                  </p>

                  <p className="mt-1 max-w-sm text-sm text-white/44">
                    Detection starts once your workflows have enough run
                    history to establish a baseline.
                  </p>
                </>
              )}
            </div>
          )
        ) : (
          <div
            className={`overflow-x-auto transition-opacity ${
              loading
                ? "opacity-60"
                : "opacity-100"
            }`}
            aria-busy={loading}
          >
            <Table>
              <TableHeader>
                <TableRow className="hover:bg-transparent">
                  <TableHead>
                    Workflow
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

                  <TableHead className="w-12">
                    <span className="sr-only">
                      Actions
                    </span>
                  </TableHead>
                </TableRow>
              </TableHeader>

              <TableBody>
                {visibleAnomalies.map(
                  (anomaly) => {
                    const rowPending =
                      pendingId === anomaly.id;

                    const canAcknowledge =
                      anomaly.status === "OPEN";

                    const canResolve =
                      anomaly.status === "OPEN" ||
                      anomaly.status ===
                        "ACKNOWLEDGED";

                    const canDismiss =
                      anomaly.status === "OPEN";

                    return (
                      <TableRow
                        key={anomaly.id}
                        className="hover:bg-white/[0.03]"
                        aria-busy={rowPending}
                      >
                        <TableCell>
                          <Link
                            href={`/workflows/${anomaly.workflowId}`}
                            className="font-medium text-white/90 hover:underline focus-visible:underline focus-visible:outline-none"
                          >
                            {anomaly.workflowName}
                          </Link>

                          {anomaly.nodeId && (
                            <p className="mt-0.5 font-mono text-xs text-white/44">
                              {anomaly.nodeId}
                            </p>
                          )}
                        </TableCell>

                        <TableCell className="hidden lg:table-cell">
                          <p className="text-sm text-white/70">
                            {anomaly.type}
                          </p>

                          {anomaly.metric && (
                            <p className="mt-0.5 text-xs text-white/44">
                              {anomaly.metric}
                            </p>
                          )}
                        </TableCell>

                        <TableCell>
                          <SeverityLabel
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

                        <TableCell className="hidden text-sm text-white/44 md:table-cell">
                          <DetectedAt
                            value={
                              anomaly.detectedAt
                            }
                          />
                        </TableCell>

                        <TableCell className="text-right">
                          <DropdownMenu>
                            <DropdownMenuTrigger
                              asChild
                            >
                              <Button
                                variant="ghost"
                                size="icon"
                                className="size-8"
                                disabled={
                                  rowPending
                                }
                                aria-label={`Actions for ${anomaly.workflowName}`}
                              >
                                <MoreHorizontal className="size-4" />
                              </Button>
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                              <DropdownMenuItem
                                asChild
                              >
                                <Link
                                  href={`/reliability/${anomaly.id}`}
                                >
                                  View details
                                </Link>
                              </DropdownMenuItem>

                              {(
                                canAcknowledge ||
                                canResolve ||
                                canDismiss
                              ) && (
                                <DropdownMenuSeparator />
                              )}

                              {canAcknowledge && (
                                <DropdownMenuItem
                                  onSelect={() =>
                                    void runAction(
                                      acknowledgeAnomaly,
                                      anomaly.id,
                                      "Anomaly acknowledged"
                                    )
                                  }
                                >
                                  Acknowledge
                                </DropdownMenuItem>
                              )}

                              {canResolve && (
                                <DropdownMenuItem
                                  onSelect={() =>
                                    void runAction(
                                      resolveAnomaly,
                                      anomaly.id,
                                      "Anomaly resolved"
                                    )
                                  }
                                >
                                  Resolve
                                </DropdownMenuItem>
                              )}

                              {canDismiss && (
                                <DropdownMenuItem
                                  onSelect={() =>
                                    setFpTarget(
                                      anomaly
                                    )
                                  }
                                >
                                  Mark as false positive
                                </DropdownMenuItem>
                              )}
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    );
                  }
                )}
              </TableBody>
            </Table>
          </div>
        )}

        {!isInitialLoad &&
          visibleAnomalies.length > 0 && (
            <div className="border-t border-white/[0.08] px-4 py-3 text-xs text-white/44">
              {isFiltered
                ? `Showing ${visibleAnomalies.length} of ${anomalies.length}`
                : `${anomalies.length} ${
                    anomalies.length === 1
                      ? "anomaly"
                      : "anomalies"
                  }`}
            </div>
          )}
      </div>

      {/* False positive dialog */}
      <Dialog
        open={fpTarget !== null}
        onOpenChange={(open) => {
          if (!open && !fpSubmitting) {
            closeFalsePositiveDialog();
          }
        }}
      >
        <DialogContent>
          <form
            onSubmit={submitFalsePositive}
            className="space-y-4"
          >
            <DialogHeader>
              <DialogTitle>
                Mark as false positive
              </DialogTitle>

              <DialogDescription>
                {fpTarget
                  ? `Explain why this anomaly on ${fpTarget.workflowName} isn't a real issue. The reason is saved to the audit trail and helps the detector avoid repeating it.`
                  : "Explain why this anomaly isn't a real issue."}
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-2">
              <Label htmlFor="fp-reason">
                Reason
              </Label>

              <Input
                id="fp-reason"
                autoFocus
                value={fpReason}
                maxLength={REASON_MAX_LENGTH}
                onChange={(e) =>
                  setFpReason(e.target.value)
                }
                placeholder="Expected during deploy window"
              />
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="ghost"
                disabled={fpSubmitting}
                onClick={
                  closeFalsePositiveDialog
                }
              >
                Cancel
              </Button>

              <Button
                type="submit"
                disabled={
                  !fpReason.trim() ||
                  fpSubmitting
                }
              >
                {fpSubmitting
                  ? "Saving…"
                  : "Mark as false positive"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}