/**
 * Wire types for the FlowOps M1 API (auth + multi-tenancy).
 *
 * These mirror the frozen API contract exactly — camelCase everywhere, UUIDs as
 * lowercase 36-char strings, timestamps as ISO-8601 UTC with milliseconds and a
 * `Z` suffix. Do not add fields that the backend does not send: request bodies
 * are validated with `fail-on-unknown-properties: true`, so an extra key on the
 * way out is a `400 MALFORMED_REQUEST`.
 */

/* ------------------------------------------------------------------ errors */

export type ErrorCode =
  | "VALIDATION_ERROR"
  | "MALFORMED_REQUEST"
  | "UNSUPPORTED_MEDIA_TYPE"
  | "AUTHENTICATION_REQUIRED"
  | "INVALID_CREDENTIALS"
  | "TOKEN_EXPIRED"
  | "TOKEN_INVALID"
  | "REFRESH_TOKEN_INVALID"
  | "FORBIDDEN_ROLE"
  | "NO_ORGANIZATION_CONTEXT"
  | "ORGANIZATION_NOT_FOUND"
  | "WORKFLOW_NOT_FOUND"
  | "WORKFLOW_INVALID"
  | "WORKFLOW_NOT_PUBLISHED"
  | "EXECUTION_NOT_FOUND"
  | "EXECUTION_NOT_RETRYABLE"
  | "APPROVAL_NOT_PENDING"
  | "NOT_FOUND"
  | "METHOD_NOT_ALLOWED"
  | "EMAIL_ALREADY_REGISTERED"
  | "RATE_LIMITED"
  | "INTERNAL_ERROR";

export interface FieldError {
  field: string;
  message: string;
}

/** The single error envelope returned by every non-2xx response. */
export interface ApiError {
  error: {
    code: ErrorCode;
    message: string;
    status: number;
    timestamp: string;
    path: string;
    requestId: string;
    /** Key is always present; an array only when `code === "VALIDATION_ERROR"`. */
    fieldErrors: FieldError[] | null;
  };
}

/* --------------------------------------------------------------- resources */

/** Ordered `OWNER > ADMIN > MEMBER > VIEWER`. Role checks compare rank. */
export type Role = "OWNER" | "ADMIN" | "MEMBER" | "VIEWER";

export interface User {
  id: string;
  /** Always the normalized (trimmed, lowercased) form. */
  email: string;
  fullName: string;
  /** Always `null` in M1 — render initials from `fullName` instead. */
  avatarUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Organization {
  id: string;
  /** As typed by the user (trimmed). Not unique across tenants. */
  name: string;
  /** Server-derived and globally unique. Immutable across renames. */
  slug: string;
  createdAt: string;
  updatedAt: string;
}

/** Denormalized org-switcher payload. Sorted `joinedAt ASC, organizationId ASC`. */
export interface Membership {
  organizationId: string;
  organizationName: string;
  organizationSlug: string;
  role: Role;
  joinedAt: string;
}

/** A row of the current organization's member list. */
export interface OrganizationMember {
  userId: string;
  email: string;
  fullName: string;
  avatarUrl: string | null;
  role: Role;
  joinedAt: string;
}

/* --------------------------------------------------------------- responses */

/** Identity + tenant snapshot. Returned by `GET /api/auth/me`. */
export interface SessionResponse {
  user: User;
  currentOrganization: Organization;
  currentRole: Role;
  memberships: Membership[];
}

/**
 * `SessionResponse` plus the three token fields. Returned by register, login,
 * refresh and switch-org — one shape, one "hydrate auth state" function.
 *
 * There is deliberately no `refreshToken` field: the refresh token only ever
 * exists in the HttpOnly `flowops_refresh` cookie.
 */
export interface AuthResponse extends SessionResponse {
  accessToken: string;
  tokenType: "Bearer";
  /** Access-token lifetime in seconds (900), not a timestamp. */
  expiresIn: number;
}

/** `GET /api/organizations` — always an object, never a bare array. */
export interface OrganizationsResponse {
  memberships: Membership[];
}

/** `GET /api/organizations/current` and `PATCH /api/organizations/current`. */
export interface CurrentOrganizationResponse {
  organization: Organization;
  role: Role;
  memberCount: number;
}

/** `POST /api/organizations` — creating an org does not switch you into it. */
export interface CreateOrganizationResponse {
  organization: Organization;
  role: Role;
}

/** `GET /api/organizations/current/members`. Sorted `joinedAt ASC, userId ASC`. */
export interface OrganizationMembersResponse {
  members: OrganizationMember[];
}

/* --------------------------------------------------------------- workflows */

export type WorkflowStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";

export type NodeCategory = "TRIGGER" | "LOGIC" | "ACTION" | "AI";

/** One configurable field on a node's config panel (mirrors backend ConfigField). */
export interface ConfigField {
  key: string;
  label: string;
  /** Widget hint: string | text | number | boolean | select | json | code. */
  type: string;
  required: boolean;
  options: string[];
  help: string | null;
  placeholder: string | null;
}

/** A node type from the registry. `GET /api/node-types`. */
export interface NodeDefinition {
  type: string;
  label: string;
  description: string;
  category: NodeCategory;
  /** lucide-react icon name. */
  icon: string;
  trigger: boolean;
  maxInputs: number;
  outputs: string[];
  configFields: ConfigField[];
}

export interface NodeTypesResponse {
  nodeTypes: NodeDefinition[];
}

/* ----- graph -----
 * The graph is stored verbatim as jsonb. It uses React Flow's node/edge shape;
 * the backend only reads `id`, `type`, `data.config` on nodes and the endpoints
 * on edges, and ignores unknown keys (position, selection, styling). */

export interface GraphNode {
  id: string;
  type: string;
  position: { x: number; y: number };
  data: {
    label?: string;
    config?: Record<string, unknown>;
    [key: string]: unknown;
  };
}

export interface GraphEdge {
  id: string;
  source: string;
  target: string;
  sourceHandle?: string | null;
  targetHandle?: string | null;
}

export interface WorkflowGraph {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

/** Row in the workflows list. `GET /api/workflows`. */
export interface WorkflowSummary {
  id: string;
  name: string;
  description: string | null;
  status: WorkflowStatus;
  latestVersion: number | null;
  nodeCount: number;
  createdAt: string;
  updatedAt: string;
}

/** Full workflow with its editable draft graph. */
export interface WorkflowDetail {
  id: string;
  name: string;
  description: string | null;
  status: WorkflowStatus;
  latestVersion: number | null;
  graph: WorkflowGraph;
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowsResponse {
  workflows: WorkflowSummary[];
}

export type ValidationSeverity = "ERROR" | "WARNING";

export interface ValidationIssue {
  severity: ValidationSeverity;
  code: string;
  message: string;
  nodeId: string | null;
  edgeId: string | null;
}

export interface ValidationResult {
  valid: boolean;
  issues: ValidationIssue[];
}

/** A published, immutable snapshot. */
export interface WorkflowVersion {
  id: string;
  versionNumber: number;
  note: string | null;
  /** Present only on the single-version detail read; null in the list. */
  graph: WorkflowGraph | null;
  publishedBy: string;
  createdAt: string;
}

export interface WorkflowVersionsResponse {
  versions: WorkflowVersion[];
}

/** `POST /api/workflows/{id}/publish` → always 200. */
export interface PublishResult {
  published: boolean;
  version: WorkflowVersion | null;
  validation: ValidationResult;
}

/* -------------------------------------------------------------- executions */

/** Lifecycle of a run. `WAITING` is a Human Approval pause; the rest are ordinary. */
export type ExecutionStatus =
  | "QUEUED"
  | "RUNNING"
  | "WAITING"
  | "SUCCEEDED"
  | "FAILED"
  | "CANCELED";

/** Whether a status is settled — the run will not change without a retry. */
export const TERMINAL_EXECUTION_STATUSES: ReadonlySet<ExecutionStatus> =
  new Set<ExecutionStatus>(["SUCCEEDED", "FAILED", "CANCELED"]);

/** Per-node lifecycle. `SKIPPED` is a node on an untaken branch. */
export type NodeRunStatus =
  | "PENDING"
  | "RUNNING"
  | "WAITING"
  | "SUCCEEDED"
  | "FAILED"
  | "SKIPPED";

export type LogLevel = "DEBUG" | "INFO" | "WARN" | "ERROR";

/** How a run was started. Derived from the graph's trigger node, never client-set. */
export type TriggerType = "MANUAL" | "WEBHOOK";

/**
 * A run as it appears in the executions list and in live `execution` SSE frames.
 * `workflowName` is resolved on reads but sent as `null` on the stream snapshot —
 * merge stream frames onto the detail fetch, never overwriting a known name.
 */
export interface ExecutionSummary {
  id: string;
  workflowId: string;
  workflowName: string | null;
  versionNumber: number;
  status: ExecutionStatus;
  triggerType: TriggerType;
  error: string | null;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number | null;
}

/**
 * One node's state within a run. `input`/`output` are the interpolated JSON the
 * node consumed and produced; `activeHandles` are the output ports the run took
 * (e.g. `["true"]` on a condition, `["approved"]` on an approval).
 */
export interface ExecutionNodeState {
  id: string;
  /** Graph node id — stable across attempts; matches `GraphNode.id`. */
  nodeId: string;
  nodeType: string;
  label: string | null;
  status: NodeRunStatus;
  /** 1-based; climbs on retry/backoff. */
  attempt: number;
  input: unknown | null;
  output: unknown | null;
  activeHandles: string[];
  error: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number | null;
}

/** One line in a run's log. `nodeId` is null for execution-level lines. */
export interface ExecutionLogEntry {
  id: string;
  nodeId: string | null;
  level: LogLevel;
  message: string;
  /** Monotonic per run — the dedup + ordering key for merging live frames. */
  seq: number;
  createdAt: string;
}

/**
 * Full run detail. Returned by `GET /api/executions/{id}`, the run/decide/retry
 * mutations, and mirrored (minus `workflowName`/`triggerPayload`) by the SSE
 * snapshot.
 */
export interface ExecutionDetail {
  id: string;
  workflowId: string;
  workflowName: string | null;
  workflowVersionId: string;
  versionNumber: number;
  status: ExecutionStatus;
  triggerType: TriggerType;
  triggerPayload: unknown | null;
  error: string | null;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number | null;
  nodes: ExecutionNodeState[];
  logs: ExecutionLogEntry[];
}

/** `GET /api/executions` — always an object, never a bare array. */
export interface ExecutionsResponse {
  executions: ExecutionSummary[];
}

/** The four dashboard windows. `24h` buckets hourly; the rest bucket daily. */
export type StatsRange = "24h" | "7d" | "30d" | "90d";

/** Aggregate KPIs over a window. `successRate` is a 0–1 fraction of decided runs. */
export interface ExecutionStatsTotals {
  total: number;
  succeeded: number;
  failed: number;
  running: number;
  waiting: number;
  queued: number;
  canceled: number;
  successRate: number;
  avgDurationMs: number | null;
}

/** One bucket of the time series. `bucketStart` is the bucket's left edge (UTC). */
export interface ExecutionStatsPoint {
  bucketStart: string;
  total: number;
  succeeded: number;
  failed: number;
}

/** `GET /api/executions/stats?range=…` → KPIs + a fixed-width bucketed series. */
export interface ExecutionStats {
  range: string;
  since: string;
  totals: ExecutionStatsTotals;
  series: ExecutionStatsPoint[];
}
