import { API_BASE_URL, refreshSession } from "@/lib/api";
import { getAccessToken } from "@/lib/auth-store";
import type {
  ExecutionLogEntry,
  ExecutionNodeState,
  ExecutionSummary,
} from "@/types";

export interface ExecutionStreamHandlers {
  /** An execution-level status/timing frame (`ExecutionSummary`; `workflowName` is null). */
  onExecution?: (execution: ExecutionSummary) => void;
  /** A node's latest state. */
  onNode?: (node: ExecutionNodeState) => void;
  /** One log line. */
  onLog?: (log: ExecutionLogEntry) => void;
  /** The server closed the stream: the run is settled or paused (`WAITING`). */
  onDone?: () => void;
  /** A transport or auth failure. Never fired for a caller-initiated abort. */
  onError?: (error: unknown) => void;
}

/**
 * Open a live SSE stream for a run.
 *
 * Uses `fetch` + a stream reader rather than `EventSource` for one reason: the
 * bearer token must travel in the `Authorization` header. `EventSource` cannot
 * set headers, so authenticating it means putting the token in the URL — where
 * it lands in access logs, violating "never log tokens" (contract §9). The
 * refresh cookie rides along via `credentials: "include"`.
 *
 * Returns an unsubscribe function; call it to abort (on unmount, or before a
 * deliberate reconnect). Aborting never fires `onError` or `onDone`.
 */
export function openExecutionStream(
  executionId: string,
  handlers: ExecutionStreamHandlers,
): () => void {
  const controller = new AbortController();
  let closed = false;

  const finish = (fn?: () => void) => {
    if (closed) return;
    closed = true;
    fn?.();
  };

  const connect = async (
    token: string | null,
    allowRefresh: boolean,
  ): Promise<void> => {
    const res = await fetch(
      `${API_BASE_URL}/api/executions/${executionId}/stream`,
      {
        method: "GET",
        headers: {
          Accept: "text/event-stream",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        credentials: "include",
        signal: controller.signal,
      },
    );

    // A stale access token on (re)connect: mint a new one once and retry, the
    // same one-shot refresh the axios interceptor does for ordinary requests.
    if (res.status === 401 && allowRefresh) {
      const auth = await refreshSession();
      return connect(auth.accessToken, false);
    }
    if (!res.ok || !res.body) {
      throw new Error(`Execution stream failed with status ${res.status}.`);
    }

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";

    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      let sep = buffer.indexOf("\n\n");
      while (sep !== -1) {
        const frame = buffer.slice(0, sep);
        buffer = buffer.slice(sep + 2);
        if (dispatchFrame(frame, handlers)) {
          // A `done` frame — the run is settled or paused. Stop reading.
          finish(handlers.onDone);
          return;
        }
        sep = buffer.indexOf("\n\n");
      }
    }
    // The body ended without a `done` frame (server-side SSE timeout/close).
    finish(handlers.onDone);
  };

  connect(getAccessToken(), true).catch((err: unknown) => {
    if (controller.signal.aborted) return;
    finish(() => handlers.onError?.(err));
  });

  return () => {
    closed = true;
    controller.abort();
  };
}

/**
 * Parse one SSE frame (`event:`/`data:` lines) and fire the matching handler.
 * Returns true only for a `done` event, so the reader can stop.
 */
function dispatchFrame(
  frame: string,
  handlers: ExecutionStreamHandlers,
): boolean {
  let event = "message";
  const dataLines: string[] = [];

  for (const rawLine of frame.split("\n")) {
    const line = rawLine.endsWith("\r") ? rawLine.slice(0, -1) : rawLine;
    if (line === "" || line.startsWith(":")) continue; // blank or heartbeat comment
    if (line.startsWith("event:")) {
      event = line.slice("event:".length).trim();
    } else if (line.startsWith("data:")) {
      const value = line.slice("data:".length);
      dataLines.push(value.startsWith(" ") ? value.slice(1) : value);
    }
  }

  if (event === "done") return true;
  if (dataLines.length === 0) return false;

  let payload: unknown;
  try {
    payload = JSON.parse(dataLines.join("\n"));
  } catch {
    return false; // a partial/garbled frame — skip it, keep the stream alive
  }

  switch (event) {
    case "execution":
      handlers.onExecution?.(payload as ExecutionSummary);
      break;
    case "node":
      handlers.onNode?.(payload as ExecutionNodeState);
      break;
    case "log":
      handlers.onLog?.(payload as ExecutionLogEntry);
      break;
  }
  return false;
}
