# FlowOps unified application visual design

## Goal

Give every signed-in FlowOps page the same operational dark interface used by Home, Dashboard, and Workflows. The refresh applies to all primary and detail pages while preserving existing routes, API contracts, mutations, permissions, keyboard behavior, and data semantics.

## Visual language

- **Canvas and surfaces:** use the existing near-black app canvas, charcoal panels, restrained white borders, and 8–12px radii.
- **Typography:** use the existing compact hierarchy: mono uppercase eyebrow, 24px page title, muted supporting copy, and tabular numeric values.
- **Color:** white is reserved for primary actions; neutral grays provide structure. Emerald means healthy/success, amber means warning or draft, and red means failed or critical. Pages must not introduce competing accent palettes.
- **Interaction:** preserve the existing subtle hover elevation, focus treatment, loading skeletons, empty states, badge language, and reduced-motion support.
- **Layout:** pages use a consistent content width and spacing, followed by a standard page header, optional metric strip, toolbar, and primary content area.

## Shared page primitives

The implementation will create presentation-level primitives that compose existing content rather than altering business logic:

1. **App page header** — eyebrow, title, description, optional context, and page actions.
2. **Metric strip** — compact grouped summary metrics with a label, tabular value, and semantic status caption.
3. **Page toolbar** — a consistent search, filter, tab, and secondary-action layout.
4. **Content panels** — shared surface, border, heading, spacing, table, card, skeleton, and empty-state treatments.
5. **Status language** — shared colors and badges for workflow state, execution state, reliability severity, agent state, and integration health.

These primitives accept content and actions through props; they must not call APIs or own feature state.

## Page coverage

### Operations

- Executions list and execution detail
- Logs
- Anomalies/reliability list and anomaly detail

Use status-first tables, consistent filters, metric strips where useful, and structured event/detail panels.

### Automation

- AI agents list and agent detail
- Integrations
- Templates
- Workflow detail/editor

Use catalog cards and connection rows consistently. The workflow canvas stays a specialized workspace; only its surrounding header, toolbars, panels, dialogs, and empty/loading states are visually aligned.

### Organization

- Team
- Settings

Use grouped configuration panels, compact member/status tables, and clear separation for destructive actions.

### Existing reference pages

Home, Dashboard, and the Workflows list define the visual baseline. They may receive targeted extraction of shared presentation primitives only when doing so preserves their current appearance and behavior.

## Behavior and error handling

- Existing query keys, API calls, mutations, authorization checks, and navigation stay unchanged.
- Loading, empty, error, disabled, and permission-restricted states remain explicit and use the shared visual treatment.
- Visual refactors cannot change query parameters, filtering rules, action labels, keyboard shortcuts, or destructive-action confirmation flows.

## Verification

- Lint and typecheck affected frontend code.
- Exercise representative page states: populated, loading, empty, filtered, failure, and disabled where available.
- Visually verify the shared shell, typography, surface hierarchy, status colors, and responsive layouts on the changed pages.

## Out of scope

- Backend changes, API contract changes, new product capabilities, information architecture changes, or unrelated behavior refactors.
