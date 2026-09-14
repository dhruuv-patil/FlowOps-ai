"use client";

import * as React from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertTriangle,
  ArrowLeft,
  Check,
  ChevronDown,
  PauseCircle,
  RefreshCw,
  X,
} from "lucide-react";
import { toast } from "sonner";

import {
  decideApproval,
  fetchExecution,
  getErrorMessage,
  retryExecution,
  cancelExecution,
} from "@/lib/api";
import { openExecutionStream } from "@/lib/execution-stream";
import type {
  ExecutionDetail,
  ExecutionLogEntry,
  ExecutionNodeState,
  ExecutionStatus,
  ExecutionSummary,
  LogLevel,
} from "@/types";
import {
  executionStatusVariant,
  formatDateTime,
  formatDuration,
  formatRelativeTime,
  isLiveStatus,
  nodeStatusVariant,
} from "@/lib/format";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

/* --------------------------------------------------------------- live state */

/** The volatile slice of a run — everything the worker mutates as it executes. */
interface LiveState {
  status: ExecutionStatus;
  error: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number | null;
  nodes: ExecutionNodeState[];
  logs: ExecutionLogEntry[];
}

type LiveAction =
  | { type: "seed"; detail: ExecutionDetail }
  | { type: "execution"; execution: ExecutionSummary }
  | { type: "node"; node: ExecutionNodeState }
  | { type: "log"; log: ExecutionLogEntry };

const EMPTY_STATE: LiveState = {
  status: "QUEUED",
  error: null,
  startedAt: null,
  finishedAt: null,
  durationMs: null,
  nodes: [],
  logs: [],
};

/**
 * Merge the authoritative fetch (`seed`) with live SSE deltas. Node frames upsert
 * by graph node id, preserving order; log frames dedup by their monotonic `seq`.
 * An `execution` frame patches only run-level fields — never node/log arrays.
 */
function liveReducer(state: LiveState, action: LiveAction): LiveState {
  switch (action.type) {
    case "seed": {
      const d = action.detail;
      return {
        status: d.status,
        error: d.error,
        startedAt: d.startedAt,
        finishedAt: d.finishedAt,
        durationMs: d.durationMs,
        nodes: d.nodes,
        logs: [...d.logs].sort((a, b) => a.seq - b.seq),
      };
    }
    case "execution": {
      const e = action.execution;
      return {
        ...state,
        status: e.status,
        error: e.error,
        startedAt: e.startedAt,
        finishedAt: e.finishedAt,
        durationMs: e.durationMs,
      };
    }
    case "node": {
      const idx = state.nodes.findIndex(
        (n) => n.nodeId === action.node.nodeId,
      );
      const nodes =
        idx === -1
          ? [...state.nodes, action.node]
          : state.nodes.map((n, i) => (i === idx ? action.node : n));
      return { ...state, nodes };
    }
    case "log": {
      if (state.logs.some((l) => l.seq === action.log.seq)) return state;
      return {
        ...state,
        logs: [...state.logs, action.log].sort((a, b) => a.seq - b.seq),
      };
    }
  }
}

const MAX_STREAM_FAILURES = 5;

/* ========================================================================= */

export default function ExecutionDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const queryClient = useQueryClient();

  const detailQuery = useQuery({
    queryKey: ["execution", id],
    queryFn: () => fetchExecution(id),
    enabled: Boolean(id),
  });

  const [state, dispatch] = React.useReducer(liveReducer, EMPTY_STATE);
  const [seeded, setSeeded] = React.useState(false);

  // Seed once from the initial fetch; later re-seeds are explicit (decide/retry
  // reset node states server-side, so their fresh detail supersedes live state).
  React.useEffect(() => {
    if (detailQuery.data && !seeded) {
      dispatch({ type: "seed", detail: detailQuery.data });
      setSeeded(true);
    }
  }, [detailQuery.data, seeded]);

  // --- live stream ---------------------------------------------------------
  const [streamNonce, setStreamNonce] = React.useState(0);
  const [connected, setConnected] = React.useState(false);
  const [disconnected, setDisconnected] = React.useState(false);
  const statusRef = React.useRef<ExecutionStatus>("QUEUED");
  const failuresRef = React.useRef(0);

  React.useEffect(() => {
    statusRef.current = state.status;
  }, [state.status]);

  const reconnect = React.useCallback(() => {
    failuresRef.current = 0;
    setDisconnected(false);
    setStreamNonce((n) => n + 1);
  }, []);

  React.useEffect(() => {
    if (!id) return;
    let retimer: ReturnType<typeof setTimeout> | undefined;
    const liveNow = () =>
      statusRef.current === "QUEUED" || statusRef.current === "RUNNING";

    setConnected(true);
    const stop = openExecutionStream(id, {
      onExecution: (e) => {
        failuresRef.current = 0;
        dispatch({ type: "execution", execution: e });
      },
      onNode: (n) => dispatch({ type: "node", node: n }),
      onLog: (l) => dispatch({ type: "log", log: l }),
      onDone: () => {
        setConnected(false);
        // A graceful close while still live is the server's SSE timeout — re-open.
        if (liveNow()) retimer = setTimeout(() => setStreamNonce((n) => n + 1), 1000);
      },
      onError: () => {
        setConnected(false);
        failuresRef.current += 1;
        if (liveNow() && failuresRef.current <= MAX_STREAM_FAILURES) {
          retimer = setTimeout(
            () => setStreamNonce((n) => n + 1),
            Math.min(1000 * failuresRef.current, 5000),
          );
        } else {
          setDisconnected(true);
        }
      },
    });

    return () => {
      stop();
      if (retimer) clearTimeout(retimer);
    };
  }, [id, streamNonce]);

  // --- mutations -----------------------------------------------------------
  const reseedFrom = React.useCallback(
    (detail: ExecutionDetail) => {
      queryClient.setQueryData(["execution", id], detail);
      dispatch({ type: "seed", detail });
      reconnect();
    },
    [id, queryClient, reconnect],
  );

  const decideMutation = useMutation({
    mutationFn: (vars: { nodeId: string; approved: boolean; note: string }) =>
      decideApproval(id, vars.nodeId, {
        approved: vars.approved,
        note: vars.note || undefined,
      }),
    onSuccess: (detail, vars) => {
      reseedFrom(detail);
      toast.success(vars.approved ? "Approved — resuming run." : "Rejected — resuming run.");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const retryMutation = useMutation({
    mutationFn: () => retryExecution(id),
    onSuccess: (detail) => {
      reseedFrom(detail);
      toast.success("Retrying the failed run.");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const cancelMutation = useMutation({
    mutationFn: () => cancelExecution(id),
    onSuccess: (detail) => {
      reseedFrom(detail);
      toast.success("Run canceled.");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  // --- render guards -------------------------------------------------------
  if (detailQuery.isPending || !seeded) {
    if (detailQuery.isError) {
      return (
        <div className="mx-auto flex max-w-5xl flex-col items-center justify-center gap-3 py-24 text-center">
          <p className="text-sm text-destructive">
            {getErrorMessage(detailQuery.error)}
          </p>
          <Button variant="outline" onClick={() => detailQuery.refetch()}>
            Retry
          </Button>
          <Link href="/executions" className="text-sm text-white/44 hover:underline">
            Back to executions
          </Link>
        </div>
      );
    }
    return (
      <div className="mx-auto max-w-5xl space-y-4">
        <Skeleton className="h-8 w-64 rounded-xl border border-white/[0.08]" />
        <Skeleton className="h-24 w-full rounded-xl border border-white/[0.08]" />
        <Skeleton className="h-64 w-full rounded-xl border border-white/[0.08]" />
      </div>
    );
  }

  const meta = detailQuery.data!;
  const live = isLiveStatus(state.status);
  const approvalNode = state.nodes.find(
    (n) => n.nodeType === "human_approval" && n.status === "WAITING",
  );
  const showApproval = state.status === "WAITING" && approvalNode;

  const triggerPayloadEmpty =
    meta.triggerPayload == null ||
    (typeof meta.triggerPayload === "object" &&
      !Array.isArray(meta.triggerPayload) &&
      Object.keys(meta.triggerPayload as object).length === 0);

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      {/* header */}
      <div className="space-y-3">
        <Link
          href="/executions"
          className="inline-flex items-center gap-1 text-sm text-white/44 hover:text-white/90"
        >
          <ArrowLeft className="size-4" /> Executions
        </Link>

        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0 space-y-1">
            <h1 className="flex items-center gap-2 text-2xl font-semibold tracking-tight text-white/90">
              <Link
                href={`/workflows/${meta.workflowId}`}
                className="truncate hover:underline"
              >
                {meta.workflowName ?? "Untitled workflow"}
              </Link>
            </h1>
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-white/44">
              <Badge
                variant={executionStatusVariant(state.status)}
                className={cn(live && "animate-pulse")}
              >
                {state.status.toLowerCase()}
              </Badge>
              <span>v{meta.versionNumber}</span>
              <span>{meta.triggerType.toLowerCase()} trigger</span>
              <span title={formatDateTime(meta.createdAt)}>
                started {formatRelativeTime(meta.startedAt ?? meta.createdAt)}
              </span>
              <span>·</span>
              <span>{formatDuration(state.durationMs)}</span>
              {connected && live && (
                <span className="inline-flex items-center gap-1 text-emerald-600 dark:text-emerald-400">
                  <span className="size-1.5 animate-pulse rounded-full bg-emerald-500" />
                  Live
                </span>
              )}
            </div>
          </div>

          {state.status === "FAILED" && (
            <Button
              onClick={() => retryMutation.mutate()}
              disabled={retryMutation.isPending}
            >
              <RefreshCw className={cn("size-4", retryMutation.isPending && "animate-spin")} />
              {retryMutation.isPending ? "Retrying…" : "Retry run"}
            </Button>
          )}

          {(state.status === "QUEUED" || state.status === "RUNNING") && (
            <Button
              variant="outline"
              onClick={() => cancelMutation.mutate()}
              disabled={cancelMutation.isPending}
            >
              <X className="size-4" />
              {cancelMutation.isPending ? "Canceling…" : "Cancel run"}
            </Button>
          )}
        </div>
      </div>

      {/* disconnected banner */}
      {disconnected && (
        <div className="flex items-center justify-between gap-3 rounded-md border border-white/[0.08] bg-white/[0.03] px-4 py-2.5 text-sm">
          <span className="text-white/44">
            Live updates disconnected.
          </span>
          <Button variant="outline" size="sm" onClick={reconnect}>
            <RefreshCw className="size-4" /> Reconnect
          </Button>
        </div>
      )}

      {/* failure banner */}
      {state.status === "FAILED" && state.error && (
        <div className="flex items-start gap-2 rounded-md border border-white/[0.08] bg-white/[0.03] px-4 py-3 text-sm text-destructive">
          <AlertTriangle className="mt-0.5 size-4 shrink-0" />
          <span className="min-w-0 break-words">{state.error}</span>
        </div>
      )}

      {/* approval panel */}
      {showApproval && approvalNode && (
        <ApprovalPanel
          node={approvalNode}
          pending={decideMutation.isPending}
          onDecide={(approved, note) =>
            decideMutation.mutate({ nodeId: approvalNode.nodeId, approved, note })
          }
        />
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        {/* nodes */}
        <div className="space-y-3 lg:col-span-2">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-white/44">
              Nodes ({state.nodes.length})
            </h2>
          </div>

          {!triggerPayloadEmpty && (
            <details className="group rounded-xl border border-white/[0.08] [&_summary::-webkit-details-marker]:hidden">
              <summary className="flex cursor-pointer list-none items-center gap-2 px-4 py-2.5 text-sm">
                <ChevronDown className="size-4 text-white/44 transition-transform group-open:rotate-180" />
                <span className="font-medium">Trigger payload</span>
              </summary>
              <div className="border-t border-white/[0.06] p-4">
                <JsonBlock value={meta.triggerPayload} />
              </div>
            </details>
          )}

          {state.nodes.length === 0 ? (
            <p className="rounded-xl border border-dashed border-white/[0.10] px-4 py-8 text-center text-sm text-white/44">
              This run has no nodes.
            </p>
          ) : (
            state.nodes.map((node) => <NodeCard key={node.id} node={node} />)
          )}
        </div>

        {/* logs */}
        <div className="lg:col-span-1">
          <LogPanel logs={state.logs} />
        </div>
      </div>
    </div>
  );
}

/* ----------------------------------------------------------------- pieces */

function ApprovalPanel({
  node,
  pending,
  onDecide,
}: {
  node: ExecutionNodeState;
  pending: boolean;
  onDecide: (approved: boolean, note: string) => void;
}) {
  const [note, setNote] = React.useState("");
  return (
    <Card className="border-amber-500/40 bg-amber-500/5 bg-[#0a0a0a]">
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2 text-base">
          <PauseCircle className="size-4 text-amber-600 dark:text-amber-400" />
          Awaiting approval
        </CardTitle>
        <CardDescription>
          “{node.label ?? node.nodeType}” is paused for a human decision. The run
          resumes down the branch you choose.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <Textarea
          value={note}
          onChange={(e) => setNote(e.target.value)}
          placeholder="Optional note (recorded in the run log)…"
          rows={2}
          disabled={pending}
        />
        <div className="flex gap-2">
          <Button onClick={() => onDecide(true, note)} disabled={pending}>
            <Check className="size-4" /> Approve
          </Button>
          <Button
            variant="outline"
            onClick={() => onDecide(false, note)}
            disabled={pending}
          >
            <X className="size-4" /> Reject
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

function NodeCard({ node }: { node: ExecutionNodeState }) {
  const running = node.status === "RUNNING";
  return (
    <details className="group rounded-xl border bg-[#0a0a0a] border-white/[0.08] [&_summary::-webkit-details-marker]:hidden">
      <summary className="flex cursor-pointer list-none items-center gap-3 px-4 py-3">
        <Badge
          variant={nodeStatusVariant(node.status)}
          className={cn("shrink-0", running && "animate-pulse")}
        >
          {node.status.toLowerCase()}
        </Badge>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium text-white/90">
            {node.label ?? node.nodeType}
          </p>
          <p className="truncate font-mono text-xs text-white/44">
            {node.nodeType}
            {node.attempt > 1 && ` · attempt ${node.attempt}`}
            {node.activeHandles.length > 0 &&
              ` · → ${node.activeHandles.join(", ")}`}
          </p>
        </div>
        <span className="shrink-0 text-xs text-white/44">
          {formatDuration(node.durationMs)}
        </span>
        <ChevronDown className="size-4 shrink-0 text-white/44 transition-transform group-open:rotate-180" />
      </summary>

      <div className="space-y-3 border-t border-white/[0.06] p-4">
        {node.error && (
          <div className="flex items-start gap-2 rounded-md border border-white/[0.08] bg-white/[0.03] px-3 py-2 text-xs text-destructive">
            <AlertTriangle className="mt-0.5 size-3.5 shrink-0" />
            <span className="min-w-0 break-words">{node.error}</span>
          </div>
        )}
        <Field label="Input">
          <JsonBlock value={node.input} />
        </Field>
        <Field label="Output">
          <JsonBlock value={node.output} />
        </Field>
      </div>
    </details>
  );
}

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-1">
      <p className="text-xs font-medium uppercase tracking-wide text-white/44">
        {label}
      </p>
      {children}
    </div>
  );
}

function JsonBlock({ value }: { value: unknown }) {
  if (value === null || value === undefined) {
    return <p className="text-xs text-white/44">None recorded.</p>;
  }
  return (
    <pre className="max-h-64 overflow-auto rounded-md border border-white/[0.08] bg-[#0a0a0a] p-3 text-xs leading-relaxed font-mono">
      {JSON.stringify(value, null, 2)}
    </pre>
  );
}

function logLevelClass(level: LogLevel): string {
  switch (level) {
    case "ERROR":
      return "text-destructive";
    case "WARN":
      return "text-amber-400";
    case "DEBUG":
      return "text-white/30";
    default:
      return "text-white/44";
  }
}

function LogPanel({ logs }: { logs: ExecutionLogEntry[] }) {
  const scrollRef = React.useRef<HTMLOListElement>(null);

  // Keep the newest line in view as logs stream in.
  React.useEffect(() => {
    const el = scrollRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [logs.length]);

  return (
    <Card className="lg:sticky lg:top-6 bg-[#0a0a0a] border-white/[0.08]">
      <CardHeader className="pb-3">
        <CardTitle className="text-base text-white/90">Logs</CardTitle>
      </CardHeader>
      <CardContent>
        {logs.length === 0 ? (
          <p className="text-sm text-white/44">No log output yet.</p>
        ) : (
          <ol
            ref={scrollRef}
            className="max-h-[60vh] space-y-1.5 overflow-auto font-mono text-xs"
          >
            {logs.map((l) => (
              <li key={l.id} className="flex gap-2">
                <span
                  className={cn("w-10 shrink-0 font-medium", logLevelClass(l.level))}
                >
                  {l.level}
                </span>
                <span className="min-w-0 flex-1 whitespace-pre-wrap break-words text-white/60">
                  {l.message}
                </span>
              </li>
            ))}
          </ol>
        )}
      </CardContent>
    </Card>
  );
}
