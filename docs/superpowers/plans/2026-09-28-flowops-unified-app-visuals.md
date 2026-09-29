# FlowOps Unified App Visuals Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply one coherent FlowOps dark operational visual system to every signed-in primary and detail page without changing product behavior.

**Architecture:** Add a small set of presentation-only primitives for page headers, metric strips, toolbars, panels, and state surfaces. Refactor pages in functional groups to compose those primitives while retaining each page’s existing query, mutation, navigation, and permission logic. Keep the workflow builder canvas specialized, updating only its shell and supporting panels.

**Tech Stack:** Next.js 14, React 18, TypeScript, Tailwind CSS, shadcn/ui, Framer Motion, TanStack Query.

**Spec:** `docs/superpowers/specs/2026-09-28-flowops-unified-app-visual-design.md`

## Global Constraints

- Preserve routes, query keys, API calls, mutations, permissions, keyboard behavior, and data semantics.
- Use the existing near-black canvas, charcoal surfaces, subtle white borders, 8–12px radii, compact typography, and monochrome hierarchy.
- Use only white for primary actions, emerald for healthy/success, amber for warning/draft, and red for failed/critical status.
- Do not introduce a competing accent palette, backend changes, information architecture changes, or new product capability.
- Preserve loading, empty, error, disabled, and permission-restricted states; only align their presentation.

## Review Focus

- Filtered list state: changing a status filter or search query must render matching content rather than leaving newly mounted cards hidden.
- Responsive toolbars: page actions, search, and filters remain available and usable at narrow widths.
- Status semantics: all visible status colors match the approved success/warning/error palette without changing their underlying state mapping.
- Empty/error/loading states: each can be seen and understood without relying on color alone.
- Workflow builder containment: the canvas remains height-bounded and interactive after its outer shell styling changes.

---

## File Structure

- Create `frontend/components/app/page-primitives.tsx` — presentation-only `AppPageHeader`, `MetricStrip`, `PageToolbar`, `AppPanel`, and `AppStateSurface` interfaces and shared Tailwind treatments.
- Create `frontend/components/app/page-primitives.test.tsx` — render-level tests for shared primitive semantics and responsive class contracts.
- Modify `frontend/package.json` and `frontend/package-lock.json` — add the minimal frontend test script and test dependencies.
- Create `frontend/vitest.config.ts` and `frontend/test/setup.ts` — Vitest browser-like test configuration.
- Modify the 13 signed-in page modules named in the tasks below — replace locally duplicated visual wrappers only; preserve page logic.
- Modify `frontend/components/app/workflow-header.tsx`, `frontend/components/app/builder/{toolbar,node-palette,config-panel,validation-panel}.tsx` — align the workflow-editor chrome to the shared visual language.

### Task 1: Establish test support and shared presentation primitives

**Files:**
- Create: `frontend/components/app/page-primitives.tsx`
- Create: `frontend/components/app/page-primitives.test.tsx`
- Create: `frontend/vitest.config.ts`
- Create: `frontend/test/setup.ts`
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`

**Interfaces:**
- Produces `AppPageHeader`, `MetricStrip`, `PageToolbar`, `AppPanel`, and `AppStateSurface` from `@/components/app/page-primitives`.
- `MetricStrip` accepts `items: Array<{ label: string; value: ReactNode; detail?: string; tone?: "neutral" | "success" | "warning" | "danger" }>`.
- `AppStateSurface` accepts `kind: "loading" | "empty" | "error"`, `title`, `description`, and optional `action`.

- [ ] **Step 1: Write failing primitive rendering tests**

```tsx
it("renders every metric label, value, and accessible status detail", () => {
  render(<MetricStrip items={[{ label: "Published", value: 4, detail: "Active", tone: "success" }]} />);
  expect(screen.getByText("Published")).toBeInTheDocument();
  expect(screen.getByText("4")).toBeInTheDocument();
  expect(screen.getByText("Active")).toBeInTheDocument();
});

it("keeps empty state copy and an optional recovery action visible", () => {
  render(<AppStateSurface kind="empty" title="No workflows" description="Create one to begin." action={<button>Create workflow</button>} />);
  expect(screen.getByText("No workflows")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Create workflow" })).toBeInTheDocument();
});
```

- [ ] **Step 2: Run the primitive test to verify it fails**

Run: `npm run test -- page-primitives.test.tsx`

Expected: FAIL because the test runner and primitives do not exist.

- [ ] **Step 3: Add Vitest/Testing Library configuration and implement the five primitives**

Keep components presentation-only. `AppPageHeader` accepts eyebrow/title/description/actions; `PageToolbar` receives children; `AppPanel` accepts title, description, actions, and children. Use the approved palette and existing `cn` utility.

- [ ] **Step 4: Run primitive tests and typecheck**

Run: `npm run test -- page-primitives.test.tsx && npm run typecheck`

Expected: primitive tests pass; record any pre-existing typecheck failures separately.

- [ ] **Step 5: Commit the primitive foundation**

```bash
git add frontend/package.json frontend/package-lock.json frontend/vitest.config.ts frontend/test/setup.ts frontend/components/app/page-primitives.tsx frontend/components/app/page-primitives.test.tsx
git commit -m "feat(ui): add shared app page primitives"
```

### Task 2: Standardize catalog pages for agents, templates, and integrations

**Files:**
- Modify: `frontend/app/(app)/agents/page.tsx`
- Modify: `frontend/app/(app)/templates/page.tsx`
- Modify: `frontend/app/(app)/integrations/page.tsx`

**Interfaces:**
- Consumes the primitives produced in Task 1.
- Produces behaviorally unchanged catalog pages with a shared header, toolbar, state surfaces, and panel/card language.

- [ ] **Step 1: Write failing render tests for catalog state surfaces**

Test one representative empty/loading/error state per page using the page’s existing public components or extracted presentation sections; assert existing actions and current copy remain available.

- [ ] **Step 2: Run those tests to verify they fail before the refactor**

Run: `npm run test -- agents templates integrations`

Expected: FAIL because shared primitive-based page tests are absent.

- [ ] **Step 3: Refactor catalog page framing and state surfaces**

Use `AppPageHeader` and `PageToolbar` for the page framing. Preserve current category/search/filter state and mutations. Replace page-local panel colors with the approved grayscale surface language; retain provider logos and application icons as content, not theme accents.

- [ ] **Step 4: Verify catalog page tests and lint**

Run: `npm run test -- agents templates integrations && npx eslint 'app/(app)/agents/page.tsx' 'app/(app)/templates/page.tsx' 'app/(app)/integrations/page.tsx'`

Expected: PASS with no new errors.

- [ ] **Step 5: Commit catalog visual alignment**

```bash
git add frontend/app/'(app)'/agents/page.tsx frontend/app/'(app)'/templates/page.tsx frontend/app/'(app)'/integrations/page.tsx frontend/components/app
git commit -m "feat(ui): unify automation catalog pages"
```

### Task 3: Standardize operations list pages

**Files:**
- Modify: `frontend/app/(app)/executions/page.tsx`
- Modify: `frontend/app/(app)/logs/page.tsx`
- Modify: `frontend/app/(app)/reliability/page.tsx`

**Interfaces:**
- Consumes `AppPageHeader`, `MetricStrip`, `PageToolbar`, `AppPanel`, and `AppStateSurface`.
- Produces status-first operations lists with unchanged query/filter state.

- [ ] **Step 1: Write failing tests for filtered and empty operations views**

Cover a filtered executions result, an empty log list, and an empty anomalies list. Assert the filtered count and visible row/action copy match the existing behavior.

- [ ] **Step 2: Run the operations tests to verify failure**

Run: `npm run test -- executions logs reliability`

Expected: FAIL because the target presentation tests are not yet defined.

- [ ] **Step 3: Apply shared headers, metrics, toolbars, and panels**

Keep execution range/status controls, logs tabs, and anomalies filters intact. Normalize table headers, row hover states, filters, status badges, and loading/error/empty panels to the shared operational system.

- [ ] **Step 4: Verify operations tests and lint**

Run: `npm run test -- executions logs reliability && npx eslint 'app/(app)/executions/page.tsx' 'app/(app)/logs/page.tsx' 'app/(app)/reliability/page.tsx'`

Expected: PASS with no new errors.

- [ ] **Step 5: Commit operations list visual alignment**

```bash
git add frontend/app/'(app)'/executions/page.tsx frontend/app/'(app)'/logs/page.tsx frontend/app/'(app)'/reliability/page.tsx frontend/components/app
git commit -m "feat(ui): unify operations list pages"
```

### Task 4: Standardize execution and reliability detail pages

**Files:**
- Modify: `frontend/app/(app)/executions/[id]/page.tsx`
- Modify: `frontend/app/(app)/executions/external/[executionExternalId]/page.tsx`
- Modify: `frontend/app/(app)/reliability/[id]/page.tsx`
- Modify: `frontend/components/app/execution/ExecutionGraph.tsx`
- Modify: `frontend/components/app/execution/ExecutionInspector.tsx`
- Modify: `frontend/components/app/execution/ExecutionSummaryStrip.tsx`
- Modify: `frontend/components/app/execution/TimelineNodeRow.tsx`

**Interfaces:**
- Consumes shared primitives and existing detail component props unchanged.
- Produces aligned detail headers, summaries, timelines, inspectors, and error/approval panels.

- [ ] **Step 1: Write failing tests for detail-state readability**

Test a failure detail state exposes its error text and retry/approval actions, and a successful detail state exposes the status label and summary metrics without relying on color alone.

- [ ] **Step 2: Run the detail tests to verify failure**

Run: `npm run test -- execution-detail external-execution anomaly-detail`

Expected: FAIL because the test coverage does not yet exist.

- [ ] **Step 3: Refactor the detail shells and child display components**

Adopt the shared header, summary strip, and panel hierarchy. Keep live-event subscriptions, approval actions, event ordering, graph geometry, inspector behavior, and recovery controls untouched. Replace nonsemantic blue/purple decorative status colors with the approved neutral/success/warning/danger palette.

- [ ] **Step 4: Verify tests and lint**

Run: `npm run test -- execution-detail external-execution anomaly-detail && npx eslint 'app/(app)/executions/[id]/page.tsx' 'app/(app)/executions/external/[executionExternalId]/page.tsx' 'app/(app)/reliability/[id]/page.tsx' components/app/execution/*.tsx`

Expected: PASS with no new errors.

- [ ] **Step 5: Commit operations detail visual alignment**

```bash
git add frontend/app/'(app)'/executions frontend/app/'(app)'/reliability/'[id]'/page.tsx frontend/components/app/execution
git commit -m "feat(ui): unify operations detail pages"
```

### Task 5: Standardize organization pages and agent detail

**Files:**
- Modify: `frontend/app/(app)/team/page.tsx`
- Modify: `frontend/app/(app)/settings/page.tsx`
- Modify: `frontend/app/(app)/agents/[id]/page.tsx`

**Interfaces:**
- Consumes shared page primitives.
- Produces configuration-oriented layouts with unchanged role-based controls, forms, dialogs, and mutations.

- [ ] **Step 1: Write failing tests for configuration actions**

Test that a Team member row exposes its role/action controls under the same authorization condition, Settings exposes both existing sections, and Agent detail preserves save/test/delete action visibility.

- [ ] **Step 2: Run the organization tests to verify failure**

Run: `npm run test -- team settings agent-detail`

Expected: FAIL because page-level presentation tests are absent.

- [ ] **Step 3: Refactor configuration shells and panels**

Use shared headers and panels. Preserve every field, dialog, mutation, role check, and destructive-action confirmation. Destructive surfaces use the approved danger treatment without changing confirmation requirements.

- [ ] **Step 4: Verify tests and lint**

Run: `npm run test -- team settings agent-detail && npx eslint 'app/(app)/team/page.tsx' 'app/(app)/settings/page.tsx' 'app/(app)/agents/[id]/page.tsx'`

Expected: PASS with no new errors.

- [ ] **Step 5: Commit organization visual alignment**

```bash
git add frontend/app/'(app)'/team/page.tsx frontend/app/'(app)'/settings/page.tsx frontend/app/'(app)'/agents/'[id]'/page.tsx frontend/components/app
git commit -m "feat(ui): unify organization and agent detail pages"
```

### Task 6: Align the workflow editor chrome

**Files:**
- Modify: `frontend/app/(app)/workflows/[id]/page.tsx`
- Modify: `frontend/components/app/workflow-header.tsx`
- Modify: `frontend/components/app/builder/toolbar.tsx`
- Modify: `frontend/components/app/builder/node-palette.tsx`
- Modify: `frontend/components/app/builder/config-panel.tsx`
- Modify: `frontend/components/app/builder/validation-panel.tsx`

**Interfaces:**
- Keeps all existing builder props and graph operations unchanged.
- Produces workflow chrome that matches shared surfaces and status semantics.

- [ ] **Step 1: Write failing builder-shell tests**

Test that the header keeps publish/run controls visible, the palette and configuration panel retain accessible labels, and the builder root preserves a constrained height class.

- [ ] **Step 2: Run the builder-shell tests to verify failure**

Run: `npm run test -- workflow-header builder-shell`

Expected: FAIL because the target rendering tests are absent.

- [ ] **Step 3: Apply visual alignment without changing graph behavior**

Update only surrounding panels, toolbar/header spacing, borders, typography, buttons, status colors, loading/error states, and dialog surfaces. Do not alter graph save/run/publish/generate/webhook control flows, node positioning, drag-and-drop, connection behavior, or canvas sizing.

- [ ] **Step 4: Verify builder tests, lint, and a manual canvas check**

Run: `npm run test -- workflow-header builder-shell && npx eslint 'app/(app)/workflows/[id]/page.tsx' components/app/workflow-header.tsx components/app/builder/{toolbar,node-palette,config-panel,validation-panel}.tsx`

Manual check: open a workflow, move a node, open configuration, run validation, and confirm the canvas remains constrained below the shared header.

- [ ] **Step 5: Commit workflow editor visual alignment**

```bash
git add frontend/app/'(app)'/workflows/'[id]'/page.tsx frontend/components/app/workflow-header.tsx frontend/components/app/builder
git commit -m "feat(ui): align workflow editor chrome"
```

### Task 7: Reconcile reference pages and verify the full visual system

**Files:**
- Modify as needed: `frontend/app/(app)/home/page.tsx`
- Modify as needed: `frontend/app/(app)/dashboard/page.tsx`
- Modify as needed: `frontend/app/(app)/workflows/page.tsx`
- Modify as needed: `frontend/components/app/app-shell.tsx`

**Interfaces:**
- Preserves Home, Dashboard, Workflows, and shell behavior while reusing shared primitives only where appearance remains unchanged.
- Produces a coherent visual system across every signed-in route.

- [ ] **Step 1: Write failing cross-page regression tests**

Test representative headers and primary action labels for Home, Dashboard, and Workflows. Add a regression test ensuring newly filtered workflow results are visible after a status filter change.

- [ ] **Step 2: Run cross-page tests to verify failure**

Run: `npm run test -- home dashboard workflows`

Expected: FAIL because the regression tests do not exist.

- [ ] **Step 3: Extract only duplicated visual framing from reference pages and shell**

Do not redesign Home, Dashboard, or Workflows. Reuse the shared primitives only for matching, behavior-neutral markup. Keep the sidebar/topbar navigation and command palette behavior unchanged.

- [ ] **Step 4: Run the full frontend verification set**

Run: `npm run test && npm run typecheck && npx eslint app components`

Expected: all new tests pass. Document any typecheck or lint failures that predate this work and are outside the touched files.

- [ ] **Step 5: Perform signed-in visual checks and commit**

Check desktop and narrow-width layouts for each navigation page plus one detail page from each group. Verify palette, hierarchy, status meaning, empty/loading/error states, and workflow filtering.

```bash
git add frontend/app/'(app)' frontend/components/app
git commit -m "feat(ui): complete unified FlowOps visual system"
```

## Plan Self-Review

- **Spec coverage:** Tasks 1–7 cover the shared components, operations, automation, organization, workflow builder, reference pages, behavior boundaries, and verification requirements in the spec.
- **Step scan:** each task has one test-first cycle, one focused implementation objective, a direct verification command, and an isolated commit.
- **Type consistency:** all page tasks consume the Task 1 exports and do not introduce data-layer interfaces.
- **Review focus:** filter remounting is covered by Task 7; narrow toolbars by Tasks 2–3 and Task 7; status semantics by Tasks 3–4; state surfaces by Tasks 1–5; builder containment by Task 6.
- **Proportion:** the plan defines file ownership, interfaces, checks, and rollout order without prescribing implementation bodies.
