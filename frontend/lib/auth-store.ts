import { create } from "zustand";

import type {
  AuthResponse,
  Membership,
  Organization,
  Role,
  SessionResponse,
  User,
} from "@/types";

/**
 * Auth state, in memory only.
 *
 * The access token is deliberately NOT persisted — no `persist` middleware, no
 * `localStorage`, no `sessionStorage`, no JS-readable cookie. It is lost on
 * reload and recovered by the boot refresh (`POST /api/auth/refresh`), which
 * reads the HttpOnly `flowops_refresh` cookie the browser sends for us. An XSS
 * payload therefore gets at most one 15-minute access token and cannot
 * establish persistence.
 *
 * SSR note: this module only calls `create()` at import time and never touches
 * `window`, `document` or `localStorage`, so it is safe to import from a
 * component that Next renders on the server. Nothing on the server ever calls a
 * mutating action, so the server-side module singleton stays at its initial
 * state and cannot leak one request's identity into another's.
 */

/**
 * - `idle` — boot refresh has not settled yet. Render a splash, never the app
 *   and never the login screen.
 * - `authenticated` — a token and session snapshot are in memory.
 * - `unauthenticated` — boot refresh returned 401, or the user logged out.
 */
export type AuthStatus = "idle" | "authenticated" | "unauthenticated";

export interface AuthState {
  status: AuthStatus;
  accessToken: string | null;
  user: User | null;
  currentOrganization: Organization | null;
  currentRole: Role | null;
  memberships: Membership[];

  /** Hydrate everything from an `AuthResponse` (register/login/refresh/switch). */
  setAuth: (auth: AuthResponse) => void;
  /** Re-sync identity + tenant from a `SessionResponse`, keeping the token. */
  setSession: (session: SessionResponse) => void;
  /**
   * Point the store at another organization the user already belongs to.
   * Used after `POST /api/organizations/{id}/switch` has been confirmed by the
   * server, and only accepts an org present in `memberships` — the client never
   * invents tenant context.
   */
  setCurrentOrganization: (organization: Organization) => void;
  /** Drop all auth state and mark the session unauthenticated. */
  clearAuth: () => void;
  /** Mark the boot refresh as settled-and-anonymous without clearing twice. */
  setStatus: (status: AuthStatus) => void;
}

const ANONYMOUS = {
  accessToken: null,
  user: null,
  currentOrganization: null,
  currentRole: null,
  memberships: [] as Membership[],
};

export const useAuthStore = create<AuthState>((set) => ({
  status: "idle",
  ...ANONYMOUS,

  setAuth: (auth) =>
    set({
      status: "authenticated",
      accessToken: auth.accessToken,
      user: auth.user,
      currentOrganization: auth.currentOrganization,
      currentRole: auth.currentRole,
      memberships: auth.memberships,
    }),

  setSession: (session) =>
    set({
      status: "authenticated",
      user: session.user,
      currentOrganization: session.currentOrganization,
      currentRole: session.currentRole,
      memberships: session.memberships,
    }),

  setCurrentOrganization: (organization) =>
    set((state) => {
      const membership = state.memberships.find(
        (m) => m.organizationId === organization.id,
      );
      if (!membership) return state;
      return {
        currentOrganization: organization,
        currentRole: membership.role,
      };
    }),

  clearAuth: () => set({ status: "unauthenticated", ...ANONYMOUS }),

  setStatus: (status) => set({ status }),
}));

/** Read the current access token outside React (used by the axios interceptor). */
export function getAccessToken(): string | null {
  return useAuthStore.getState().accessToken;
}

const ROLE_RANK: Record<Role, number> = {
  OWNER: 4,
  ADMIN: 3,
  MEMBER: 2,
  VIEWER: 1,
};

/**
 * `true` when `role` is at least `required` — "requires ADMIN" means ADMIN or
 * OWNER. UX only: the backend is the authority on every RBAC decision.
 */
export function hasRole(role: Role | null, required: Role): boolean {
  if (!role) return false;
  return ROLE_RANK[role] >= ROLE_RANK[required];
}
