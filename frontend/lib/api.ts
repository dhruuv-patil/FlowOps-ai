import axios, {
  AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";

import { useAuthStore } from "@/lib/auth-store";
import type {
  ApiError,
  AuthResponse,
  CreateOrganizationResponse,
  CurrentOrganizationResponse,
  ErrorCode,
  ExecutionDetail,
  ExecutionStats,
  ExecutionStatus,
  ExecutionsResponse,
  FieldError,
  OrganizationMembersResponse,
  OrganizationsResponse,
  PublishResult,
  SessionResponse,
  StatsRange,
  ValidationResult,
  WorkflowDetail,
  WorkflowGraph,
  WorkflowStatus,
  WorkflowsResponse,
  WorkflowVersion,
  WorkflowVersionsResponse,
  NodeTypesResponse,
} from "@/types";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/** Endpoints that must never trigger a refresh-and-replay (they *are* the auth flow). */
const NO_REFRESH_PATHS = [
  "/api/auth/refresh",
  "/api/auth/login",
  "/api/auth/logout",
  "/api/auth/register",
];

/** 401 codes that are terminal: clear the store and go to /login, never retry. */
const TERMINAL_CODES: ReadonlySet<ErrorCode> = new Set<ErrorCode>([
  "REFRESH_TOKEN_INVALID",
  "AUTHENTICATION_REQUIRED",
  "TOKEN_INVALID",
]);

interface RetryableConfig extends InternalAxiosRequestConfig {
  /** Set once a request has already been replayed after a refresh. */
  _retried?: boolean;
}

/**
 * The application client. Every request carries credentials so the browser
 * attaches the HttpOnly `flowops_refresh` cookie on the two endpoints scoped to
 * `/api/auth`, and `Content-Type: application/json` so cookie-reading endpoints
 * always trigger a CORS preflight (the contract's CSRF defense).
 */
export const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

/**
 * A second, interceptor-free instance used exclusively for the refresh call, so
 * a 401 from `/api/auth/refresh` can never re-enter the refresh interceptor.
 */
const refreshClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

/* ------------------------------------------------------------ error access */

/**
 * Narrow an unknown thrown value to the contract's error envelope.
 * Returns `null` for network failures, CORS errors and cancellations, which
 * have no response body at all.
 */
export function getApiError(error: unknown): ApiError["error"] | null {
  if (!axios.isAxiosError(error)) return null;
  const data = (error as AxiosError<ApiError>).response?.data;
  if (!data || typeof data !== "object") return null;
  const envelope = (data as ApiError).error;
  if (!envelope || typeof envelope.code !== "string") return null;
  return envelope;
}

/** The stable `error.code`, or `null` when the failure had no envelope. */
export function getErrorCode(error: unknown): ErrorCode | null {
  return getApiError(error)?.code ?? null;
}

/**
 * A human-readable message safe to show in a toast. Falls back to a generic
 * line for transport-level failures, which carry no server message.
 */
export function getErrorMessage(
  error: unknown,
  fallback = "Something went wrong. Please try again.",
): string {
  const envelope = getApiError(error);
  if (envelope?.message) return envelope.message;
  if (axios.isAxiosError(error) && !error.response) {
    return "Cannot reach the FlowOps API. Check that the server is running.";
  }
  return fallback;
}

/**
 * Field-level messages from a `400 VALIDATION_ERROR`, keyed by the camelCase
 * JSON field name. Empty for every other code, so callers can render a
 * form-level message instead.
 */
export function getFieldErrors(error: unknown): Record<string, string> {
  const envelope = getApiError(error);
  if (envelope?.code !== "VALIDATION_ERROR" || !envelope.fieldErrors) return {};
  const out: Record<string, string> = {};
  for (const fe of envelope.fieldErrors as FieldError[]) {
    // First message per field wins; the backend may send several and order is
    // not guaranteed.
    if (!(fe.field in out)) out[fe.field] = fe.message;
  }
  return out;
}

/* --------------------------------------------------------------- redirects */

function redirectToLogin() {
  if (typeof window === "undefined") return;
  if (window.location.pathname === "/login") return;
  // A hard navigation is intentional: it guarantees no in-memory tenant data
  // from the dead session survives into the login screen.
  window.location.replace("/login");
}

/* ---------------------------------------------------- single-flight refresh */

let refreshPromise: Promise<AuthResponse> | null = null;

/**
 * Rotate the refresh cookie and mint a new access token.
 *
 * Refresh-token reuse detection is destructive — presenting an already-rotated
 * token revokes every session the user has — so concurrent callers must share
 * one in-flight request rather than each firing their own. The promise is
 * cleared in `finally` so the next 401 starts a fresh attempt.
 */
export function refreshSession(): Promise<AuthResponse> {
  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post<AuthResponse>("/api/auth/refresh", {})
      .then((res) => {
        useAuthStore.getState().setAuth(res.data);
        return res.data;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

/**
 * App-boot session recovery. The access token lives in memory only, so a reload
 * starts with nothing; one refresh call restores the token *and* a fresh
 * user/org/membership snapshot in a single round trip.
 *
 * A 401 here is the normal cold start for a logged-out visitor — it resolves
 * `false` rather than throwing, and must not raise an error toast.
 */
export async function bootstrapSession(): Promise<boolean> {
  try {
    await refreshSession();
    return true;
  } catch {
    useAuthStore.getState().clearAuth();
    return false;
  }
}

/* ------------------------------------------------------------ interceptors */

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

api.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError<ApiError>) => {
    const config = error.config as RetryableConfig | undefined;
    const status = error.response?.status;

    if (!config || status !== 401) {
      return Promise.reject(error);
    }

    const url = config.url ?? "";
    if (NO_REFRESH_PATHS.some((p) => url.startsWith(p))) {
      return Promise.reject(error);
    }

    const code = getErrorCode(error);

    if (code && TERMINAL_CODES.has(code)) {
      useAuthStore.getState().clearAuth();
      redirectToLogin();
      return Promise.reject(error);
    }

    // `TOKEN_EXPIRED` is the designed refresh trigger. A 401 with no readable
    // envelope is treated the same way — one refresh attempt, then terminal.
    if (code !== null && code !== "TOKEN_EXPIRED") {
      return Promise.reject(error);
    }

    if (config._retried) {
      useAuthStore.getState().clearAuth();
      redirectToLogin();
      return Promise.reject(error);
    }

    config._retried = true;

    try {
      await refreshSession();
      // Replaying through `api.request` re-runs the request interceptor, which
      // overwrites the stale Authorization header with the freshly minted token.
      return await api.request(config);
    } catch (refreshError) {
      useAuthStore.getState().clearAuth();
      redirectToLogin();
      return Promise.reject(refreshError);
    }
  },
);

/* --------------------------------------------------------------- endpoints */

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
  organizationName: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

/** `POST /api/auth/register` → 201 `AuthResponse` + refresh cookie. */
export async function register(
  payload: RegisterPayload,
): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>("/api/auth/register", payload);
  useAuthStore.getState().setAuth(data);
  return data;
}

/** `POST /api/auth/login` → 200 `AuthResponse` + refresh cookie. */
export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>("/api/auth/login", payload);
  useAuthStore.getState().setAuth(data);
  return data;
}

/**
 * `POST /api/auth/logout` → 204. Idempotent and never fails, so the local store
 * is cleared unconditionally: an already-issued access token stays valid
 * server-side for up to 15 minutes and must not linger in memory.
 */
export async function logout(): Promise<void> {
  try {
    await api.post("/api/auth/logout", {});
  } finally {
    useAuthStore.getState().clearAuth();
  }
}

/** `GET /api/auth/me` → fresh identity + tenant context from the database. */
export async function fetchMe(): Promise<SessionResponse> {
  const { data } = await api.get<SessionResponse>("/api/auth/me");
  return data;
}

/** `GET /api/organizations` → every org the authenticated user belongs to. */
export async function fetchOrganizations(): Promise<OrganizationsResponse> {
  const { data } = await api.get<OrganizationsResponse>("/api/organizations");
  return data;
}

/** `GET /api/organizations/current` → the org named by the token's `orgId`. */
export async function fetchCurrentOrganization(): Promise<CurrentOrganizationResponse> {
  const { data } = await api.get<CurrentOrganizationResponse>(
    "/api/organizations/current",
  );
  return data;
}

/** `PATCH /api/organizations/current` → rename. Requires ADMIN or OWNER. */
export async function renameCurrentOrganization(
  name: string,
): Promise<CurrentOrganizationResponse> {
  const { data } = await api.patch<CurrentOrganizationResponse>(
    "/api/organizations/current",
    { name },
  );
  return data;
}

/** `POST /api/organizations` → 201. Does **not** switch the caller into it. */
export async function createOrganization(
  organizationName: string,
): Promise<CreateOrganizationResponse> {
  const { data } = await api.post<CreateOrganizationResponse>(
    "/api/organizations",
    { organizationName },
  );
  return data;
}

/**
 * `POST /api/organizations/{organizationId}/switch` → 200 `AuthResponse` with a
 * new access token. No body, no `Set-Cookie`; the session row now carries the
 * new org so a later refresh returns it.
 */
export async function switchOrganization(
  organizationId: string,
): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>(
    `/api/organizations/${organizationId}/switch`,
  );
  useAuthStore.getState().setAuth(data);
  return data;
}

/** `GET /api/organizations/current/members` → readable by any role. */
export async function fetchCurrentOrganizationMembers(): Promise<OrganizationMembersResponse> {
  const { data } = await api.get<OrganizationMembersResponse>(
    "/api/organizations/current/members",
  );
  return data;
}

/* ---------------------------------------------------------------- workflows */

/** `GET /api/node-types` → the global node registry for the builder palette. */
export async function fetchNodeTypes(): Promise<NodeTypesResponse> {
  const { data } = await api.get<NodeTypesResponse>("/api/node-types");
  return data;
}

/** `GET /api/workflows` → org-scoped list with optional search / status filter. */
export async function fetchWorkflows(params?: {
  search?: string;
  status?: WorkflowStatus;
}): Promise<WorkflowsResponse> {
  const { data } = await api.get<WorkflowsResponse>("/api/workflows", {
    params: {
      search: params?.search || undefined,
      status: params?.status || undefined,
    },
  });
  return data;
}

/** `POST /api/workflows` → 201 with an empty draft graph. */
export async function createWorkflow(payload: {
  name: string;
  description?: string;
}): Promise<WorkflowDetail> {
  const { data } = await api.post<WorkflowDetail>("/api/workflows", payload);
  return data;
}

/** `GET /api/workflows/{id}` → workflow with its editable draft graph. */
export async function fetchWorkflow(id: string): Promise<WorkflowDetail> {
  const { data } = await api.get<WorkflowDetail>(`/api/workflows/${id}`);
  return data;
}

/** `PATCH /api/workflows/{id}` → rename / re-describe (metadata only). */
export async function updateWorkflow(
  id: string,
  payload: { name: string; description?: string },
): Promise<WorkflowDetail> {
  const { data } = await api.patch<WorkflowDetail>(
    `/api/workflows/${id}`,
    payload,
  );
  return data;
}

/** `PUT /api/workflows/{id}/graph` → save the working graph verbatim. */
export async function saveWorkflowGraph(
  id: string,
  graph: WorkflowGraph,
): Promise<WorkflowDetail> {
  const { data } = await api.put<WorkflowDetail>(`/api/workflows/${id}/graph`, {
    graph,
  });
  return data;
}

/** `POST /api/workflows/{id}/validate` → validation result, no side effects. */
export async function validateWorkflow(id: string): Promise<ValidationResult> {
  const { data } = await api.post<ValidationResult>(
    `/api/workflows/${id}/validate`,
  );
  return data;
}

/** `POST /api/workflows/{id}/publish` → always 200; check `published`. */
export async function publishWorkflow(
  id: string,
  note?: string,
): Promise<PublishResult> {
  const { data } = await api.post<PublishResult>(
    `/api/workflows/${id}/publish`,
    { note: note ?? null },
  );
  return data;
}

/** `GET /api/workflows/{id}/versions` → published versions, newest first. */
export async function fetchWorkflowVersions(
  id: string,
): Promise<WorkflowVersionsResponse> {
  const { data } = await api.get<WorkflowVersionsResponse>(
    `/api/workflows/${id}/versions`,
  );
  return data;
}

/** `GET /api/workflows/{id}/versions/{n}` → one version including its graph. */
export async function fetchWorkflowVersion(
  id: string,
  versionNumber: number,
): Promise<WorkflowVersion> {
  const { data } = await api.get<WorkflowVersion>(
    `/api/workflows/${id}/versions/${versionNumber}`,
  );
  return data;
}

/** `DELETE /api/workflows/{id}` → 204. */
export async function deleteWorkflow(id: string): Promise<void> {
  await api.delete(`/api/workflows/${id}`);
}

/* --------------------------------------------------------------- executions */

/**
 * `POST /api/workflows/{id}/run` → 201 `ExecutionDetail`. Runs the workflow's
 * latest published version; `input` is the optional trigger payload, readable
 * downstream as `{{trigger.*}}`. Only ever called from an explicit user action.
 */
export async function runWorkflow(
  workflowId: string,
  input?: unknown,
): Promise<ExecutionDetail> {
  const { data } = await api.post<ExecutionDetail>(
    `/api/workflows/${workflowId}/run`,
    input === undefined ? {} : { input },
  );
  return data;
}

/** `GET /api/executions` → org-scoped runs, newest first, optionally filtered. */
export async function fetchExecutions(params?: {
  workflowId?: string;
  status?: ExecutionStatus;
  limit?: number;
}): Promise<ExecutionsResponse> {
  const { data } = await api.get<ExecutionsResponse>("/api/executions", {
    params: {
      workflowId: params?.workflowId || undefined,
      status: params?.status || undefined,
      limit: params?.limit || undefined,
    },
  });
  return data;
}

/** `GET /api/executions/{id}` → a run with its node states and full log. */
export async function fetchExecution(id: string): Promise<ExecutionDetail> {
  const { data } = await api.get<ExecutionDetail>(`/api/executions/${id}`);
  return data;
}

/**
 * `POST /api/executions/{id}/nodes/{nodeId}/decision` → resolve a waiting Human
 * Approval step and resume the run. `approved` routes down the matching port;
 * `note` is an optional reason recorded in the log.
 */
export async function decideApproval(
  executionId: string,
  nodeId: string,
  decision: { approved: boolean; note?: string },
): Promise<ExecutionDetail> {
  const { data } = await api.post<ExecutionDetail>(
    `/api/executions/${executionId}/nodes/${nodeId}/decision`,
    { approved: decision.approved, note: decision.note ?? null },
  );
  return data;
}

/** `POST /api/executions/{id}/retry` → re-run a FAILED run from its failed step. */
export async function retryExecution(id: string): Promise<ExecutionDetail> {
  const { data } = await api.post<ExecutionDetail>(
    `/api/executions/${id}/retry`,
  );
  return data;
}

/** `GET /api/executions/stats?range=…` → dashboard KPIs + bucketed time series. */
export async function fetchExecutionStats(
  range: StatsRange,
): Promise<ExecutionStats> {
  const { data } = await api.get<ExecutionStats>("/api/executions/stats", {
    params: { range },
  });
  return data;
}
