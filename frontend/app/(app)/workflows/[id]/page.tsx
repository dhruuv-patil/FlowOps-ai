"use client";

import * as React from "react";
import { useParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Background,
  Controls,
  MiniMap,
  ReactFlow,
  ReactFlowProvider,
  addEdge,
  useEdgesState,
  useNodesState,
  useReactFlow,
  type Connection,
  type NodeTypes,
  type OnSelectionChangeParams,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";
import { toast } from "sonner";

import {
  fetchNodeTypes,
  fetchWorkflow,
  getErrorMessage,
  publishWorkflow,
  saveWorkflowGraph,
  updateWorkflow,
  validateWorkflow,
} from "@/lib/api";
import type {
  NodeDefinition,
  ValidationResult,
  WorkflowDetail,
} from "@/types";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  DND_MIME,
  ErrorNodesContext,
  flowToGraph,
  graphToFlow,
  newNodeId,
  type FlowEdge,
  type FlowNode as FlowNodeType,
} from "@/components/app/builder/shared";
import { FlowNode, NodeDefsContext } from "@/components/app/builder/flow-node";
import { NodePalette } from "@/components/app/builder/node-palette";
import { ConfigPanel } from "@/components/app/builder/config-panel";
import { Toolbar } from "@/components/app/builder/toolbar";
import { ValidationPanel } from "@/components/app/builder/validation-panel";
import { PublishDialog } from "@/components/app/builder/publish-dialog";

/* ================================================================ page shell */

export default function WorkflowBuilderPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;

  const workflowQuery = useQuery({
    queryKey: ["workflow", id],
    queryFn: () => fetchWorkflow(id),
    enabled: Boolean(id),
  });
  const nodeTypesQuery = useQuery({
    queryKey: ["node-types"],
    queryFn: fetchNodeTypes,
    staleTime: 5 * 60 * 1000,
  });

  if (workflowQuery.isPending) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-14 w-full" />
        <Skeleton className="h-[70vh] w-full" />
      </div>
    );
  }

  if (workflowQuery.isError) {
    return (
      <div className="flex h-[60vh] flex-col items-center justify-center gap-3 text-center">
        <p className="text-sm text-destructive">
          {getErrorMessage(workflowQuery.error)}
        </p>
        <Button variant="outline" onClick={() => workflowQuery.refetch()}>
          Retry
        </Button>
      </div>
    );
  }

  return (
    <ReactFlowProvider>
      <Builder
        workflow={workflowQuery.data}
        nodeDefs={nodeTypesQuery.data?.nodeTypes ?? []}
        nodeDefsLoading={nodeTypesQuery.isPending}
        nodeDefsError={nodeTypesQuery.isError}
      />
    </ReactFlowProvider>
  );
}

/* ==================================================================== builder */

interface BuilderProps {
  workflow: WorkflowDetail;
  nodeDefs: NodeDefinition[];
  nodeDefsLoading: boolean;
  nodeDefsError: boolean;
}

function Builder({
  workflow,
  nodeDefs,
  nodeDefsLoading,
  nodeDefsError,
}: BuilderProps) {
  const queryClient = useQueryClient();
  const rf = useReactFlow<FlowNodeType, FlowEdge>();
  const wrapperRef = React.useRef<HTMLDivElement>(null);

  // --- registry lookups ---------------------------------------------------
  const defsByType = React.useMemo(() => {
    const map: Record<string, NodeDefinition> = {};
    for (const d of nodeDefs) map[d.type] = d;
    return map;
  }, [nodeDefs]);

  /**
   * React Flow needs a nodeTypes map keyed by the string on `node.type`. We map
   * EVERY backend type to the same `FlowNode` renderer so saved graphs keep
   * their real registry key while sharing one component.
   */
  const nodeTypes = React.useMemo<NodeTypes>(() => {
    const map: NodeTypes = {};
    for (const d of nodeDefs) map[d.type] = FlowNode;
    return map;
  }, [nodeDefs]);

  // --- graph state ---------------------------------------------------------
  const initial = React.useMemo(
    () => graphToFlow(workflow.graph),
    [workflow.graph],
  );
  const [nodes, setNodes, onNodesChange] = useNodesState<FlowNodeType>(
    initial.nodes,
  );
  const [edges, setEdges, onEdgesChange] = useEdgesState<FlowEdge>(
    initial.edges,
  );
  const [selectedId, setSelectedId] = React.useState<string | null>(null);

  // Baseline of the last persisted graph, for the dirty indicator.
  const savedRef = React.useRef<string>(
    JSON.stringify(flowToGraph(initial.nodes, initial.edges)),
  );
  const [dirty, setDirty] = React.useState(false);

  const recomputeDirty = React.useCallback(
    (nextNodes: FlowNodeType[], nextEdges: FlowEdge[]) => {
      const now = JSON.stringify(flowToGraph(nextNodes, nextEdges));
      setDirty(now !== savedRef.current);
    },
    [],
  );

  React.useEffect(() => {
    recomputeDirty(nodes, edges);
  }, [nodes, edges, recomputeDirty]);

  // --- history (undo / redo) ----------------------------------------------
  interface Snapshot {
    nodes: FlowNodeType[];
    edges: FlowEdge[];
  }
  const past = React.useRef<Snapshot[]>([]);
  const future = React.useRef<Snapshot[]>([]);
  const [, setHistTick] = React.useState(0);

  const snapshot = React.useCallback(
    (): Snapshot => ({
      nodes: structuredClone(rf.getNodes()),
      edges: structuredClone(rf.getEdges()),
    }),
    [rf],
  );

  /** Record the current graph as an undo point before a mutating action. */
  const commit = React.useCallback(() => {
    past.current.push(snapshot());
    if (past.current.length > 100) past.current.shift();
    future.current = [];
    setHistTick((t) => t + 1);
  }, [snapshot]);

  const undo = React.useCallback(() => {
    const prev = past.current.pop();
    if (!prev) return;
    future.current.push(snapshot());
    setNodes(prev.nodes);
    setEdges(prev.edges);
    setHistTick((t) => t + 1);
  }, [snapshot, setNodes, setEdges]);

  const redo = React.useCallback(() => {
    const next = future.current.pop();
    if (!next) return;
    past.current.push(snapshot());
    setNodes(next.nodes);
    setEdges(next.edges);
    setHistTick((t) => t + 1);
  }, [snapshot, setNodes, setEdges]);

  // --- graph editing -------------------------------------------------------

  const onConnect = React.useCallback(
    (conn: Connection) => {
      commit();
      setEdges((eds) => addEdge(conn, eds));
    },
    [commit, setEdges],
  );

  /** Add a node of `type`, either at a screen point or the viewport center. */
  const addNode = React.useCallback(
    (type: string, screenPos?: { x: number; y: number }) => {
      const def = defsByType[type];
      if (!def) return;

      const bounds = wrapperRef.current?.getBoundingClientRect();
      const position =
        screenPos && bounds
          ? rf.screenToFlowPosition({ x: screenPos.x, y: screenPos.y })
          : rf.screenToFlowPosition({
              x: (bounds?.left ?? 0) + (bounds?.width ?? 800) / 2,
              y: (bounds?.top ?? 0) + (bounds?.height ?? 600) / 2,
            });

      const node: FlowNodeType = {
        id: newNodeId(type),
        type,
        position,
        data: { label: def.label, config: {} },
      };
      commit();
      setNodes((nds) => nds.concat(node));
      setSelectedId(node.id);
    },
    [defsByType, rf, commit, setNodes],
  );

  const onDrop = React.useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();
      const type = event.dataTransfer.getData(DND_MIME);
      if (!type) return;
      addNode(type, { x: event.clientX, y: event.clientY });
    },
    [addNode],
  );

  const onDragOver = React.useCallback((event: React.DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
  }, []);

  const onSelectionChange = React.useCallback(
    ({ nodes: sel }: OnSelectionChangeParams) => {
      // Single-node selection drives the inspector; multi-select clears it.
      setSelectedId(sel.length === 1 ? sel[0].id : null);
    },
    [],
  );

  // Record an undo point when a node/edge deletion is about to happen.
  const onBeforeDelete = React.useCallback(async () => {
    commit();
    return true;
  }, [commit]);

  // Snapshot before a drag so position moves are individually undoable.
  const onNodeDragStart = React.useCallback(() => {
    commit();
  }, [commit]);

  // --- inspector edits -----------------------------------------------------

  const selectedNode = React.useMemo(
    () => nodes.find((n) => n.id === selectedId) ?? null,
    [nodes, selectedId],
  );

  const updateSelected = React.useCallback(
    (updater: (data: FlowNodeType["data"]) => FlowNodeType["data"]) => {
      if (!selectedId) return;
      setNodes((nds) =>
        nds.map((n) =>
          n.id === selectedId ? { ...n, data: updater(n.data) } : n,
        ),
      );
    },
    [selectedId, setNodes],
  );

  const onLabelChange = React.useCallback(
    (label: string) => updateSelected((data) => ({ ...data, label })),
    [updateSelected],
  );

  const onConfigChange = React.useCallback(
    (key: string, value: unknown) =>
      updateSelected((data) => ({
        ...data,
        config: { ...(data.config ?? {}), [key]: value },
      })),
    [updateSelected],
  );

  /** Node ids that feed into the selected node (direct upstream sources). */
  const upstreamIds = React.useMemo(() => {
    if (!selectedId) return [];
    return edges
      .filter((e) => e.target === selectedId)
      .map((e) => e.source)
      .filter((v, i, a) => a.indexOf(v) === i);
  }, [edges, selectedId]);

  // --- validation state ----------------------------------------------------

  const [validation, setValidation] = React.useState<ValidationResult | null>(
    null,
  );
  const [showValidation, setShowValidation] = React.useState(false);
  const errorNodeIds = React.useMemo(() => {
    const set = new Set<string>();
    for (const issue of validation?.issues ?? []) {
      if (issue.severity === "ERROR" && issue.nodeId) set.add(issue.nodeId);
    }
    return set;
  }, [validation]);

  const focusNode = React.useCallback(
    (nodeId: string) => {
      const node = rf.getNode(nodeId);
      if (!node) return;
      setSelectedId(nodeId);
      rf.setCenter(
        node.position.x + 90,
        node.position.y + 40,
        { zoom: 1.2, duration: 400 },
      );
    },
    [rf],
  );

  // --- server mutations ----------------------------------------------------

  const saveMutation = useMutation({
    mutationFn: () => saveWorkflowGraph(workflow.id, flowToGraph(nodes, edges)),
    onSuccess: (detail) => {
      savedRef.current = JSON.stringify(detail.graph);
      setDirty(false);
      queryClient.setQueryData(["workflow", workflow.id], detail);
      toast.success("Workflow saved.");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const validateMutation = useMutation({
    mutationFn: () => validateWorkflow(workflow.id),
    onSuccess: (result) => {
      setValidation(result);
      setShowValidation(true);
      if (result.valid) {
        toast.success("Validation passed.");
      } else {
        const errs = result.issues.filter((i) => i.severity === "ERROR").length;
        toast.error(`${errs} validation error(s) found.`);
      }
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const [publishOpen, setPublishOpen] = React.useState(false);
  const [publishBlock, setPublishBlock] = React.useState<ValidationResult | null>(
    null,
  );
  const publishMutation = useMutation({
    mutationFn: (note: string) =>
      publishWorkflow(workflow.id, note || undefined),
    onSuccess: (result) => {
      setValidation(result.validation);
      if (result.published && result.version) {
        setPublishBlock(null);
        setPublishOpen(false);
        setShowValidation(false);
        queryClient.invalidateQueries({ queryKey: ["workflow", workflow.id] });
        toast.success(`Published version v${result.version.versionNumber}.`);
      } else {
        // Rejected: surface the blocking issues in the dialog and panel.
        setPublishBlock(result.validation);
        setShowValidation(true);
        toast.error("Publish blocked by validation errors.");
      }
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const renameMutation = useMutation({
    mutationFn: (name: string) =>
      updateWorkflow(workflow.id, {
        name,
        description: workflow.description ?? undefined,
      }),
    onSuccess: (detail) => {
      queryClient.setQueryData(["workflow", workflow.id], detail);
      toast.success("Workflow renamed.");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const doSave = React.useCallback(() => {
    if (!saveMutation.isPending) saveMutation.mutate();
  }, [saveMutation]);

  // Ctrl/Cmd+S saves, Ctrl/Cmd+Z / Shift+Z undo/redo (when not in a field).
  React.useEffect(() => {
    function onKey(e: KeyboardEvent) {
      const target = e.target as HTMLElement | null;
      const inField =
        target &&
        (target.tagName === "INPUT" ||
          target.tagName === "TEXTAREA" ||
          target.tagName === "SELECT" ||
          target.isContentEditable);
      const mod = e.metaKey || e.ctrlKey;
      if (mod && e.key.toLowerCase() === "s") {
        e.preventDefault();
        doSave();
      } else if (mod && !inField && e.key.toLowerCase() === "z") {
        e.preventDefault();
        if (e.shiftKey) redo();
        else undo();
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [doSave, undo, redo]);

  const current = queryClient.getQueryData<WorkflowDetail>([
    "workflow",
    workflow.id,
  ]);
  const status = current?.status ?? workflow.status;
  const version = current?.latestVersion ?? workflow.latestVersion;
  const name = current?.name ?? workflow.name;

  // --- palette collapse ----------------------------------------------------
  const [paletteCollapsed, setPaletteCollapsed] = React.useState(false);
  const [inspectorOpen, setInspectorOpen] = React.useState(true);

  // Auto-open the inspector when a node is selected.
  React.useEffect(() => {
    if (selectedId) setInspectorOpen(true);
  }, [selectedId]);

  return (
    <ErrorNodesContext.Provider value={errorNodeIds}>
      <NodeDefsContext.Provider value={defsByType}>
        {/* Break out of the app-shell padding to use the full content area. */}
        <div className="-mx-6 -my-6 flex h-[calc(100vh-4rem)] flex-col lg:-mx-8">
          <Toolbar
            name={name}
            status={status}
            version={version}
            dirty={dirty}
            canUndo={past.current.length > 0}
            canRedo={future.current.length > 0}
            savePending={saveMutation.isPending}
            validatePending={validateMutation.isPending}
            namePending={renameMutation.isPending}
            onRename={(n) => renameMutation.mutate(n)}
            onUndo={undo}
            onRedo={redo}
            onZoomIn={() => rf.zoomIn({ duration: 200 })}
            onZoomOut={() => rf.zoomOut({ duration: 200 })}
            onFitView={() => rf.fitView({ duration: 300, padding: 0.2 })}
            onValidate={() => validateMutation.mutate()}
            onSave={doSave}
            onPublish={() => {
              setPublishBlock(null);
              setPublishOpen(true);
            }}
          />

          <div className="flex min-h-0 flex-1">
            <NodePalette
              defs={nodeDefs}
              collapsed={paletteCollapsed}
              onToggle={() => setPaletteCollapsed((c) => !c)}
              onAdd={(type) => addNode(type)}
              isLoading={nodeDefsLoading}
              isError={nodeDefsError}
            />

            <div
              ref={wrapperRef}
              className="relative min-w-0 flex-1"
              onDrop={onDrop}
              onDragOver={onDragOver}
            >
              <ReactFlow
                nodes={nodes}
                edges={edges}
                nodeTypes={nodeTypes}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                onConnect={onConnect}
                onSelectionChange={onSelectionChange}
                onNodeDragStart={onNodeDragStart}
                onBeforeDelete={onBeforeDelete}
                deleteKeyCode={["Delete", "Backspace"]}
                multiSelectionKeyCode={["Meta", "Control", "Shift"]}
                fitView
                proOptions={{ hideAttribution: true }}
                className="bg-background"
              >
                <Background className="!bg-muted/20" gap={16} />
                <Controls className="!shadow-md" />
                <MiniMap
                  pannable
                  zoomable
                  className="!bg-card"
                  nodeClassName={() => "!fill-primary/40"}
                />
              </ReactFlow>

              {nodes.length === 0 && (
                <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
                  <p className="rounded-md border border-dashed border-border/60 bg-background/80 px-4 py-3 text-sm text-muted-foreground">
                    Drag a node from the left, or click one to add it.
                  </p>
                </div>
              )}

              <ValidationPanel
                result={showValidation ? validation : null}
                onClose={() => setShowValidation(false)}
                onFocusNode={focusNode}
              />
            </div>

            {inspectorOpen && (
              <ConfigPanel
                node={selectedNode}
                def={selectedNode?.type ? defsByType[selectedNode.type] : undefined}
                upstreamIds={upstreamIds}
                onClose={() => setInspectorOpen(false)}
                onLabelChange={onLabelChange}
                onConfigChange={onConfigChange}
              />
            )}
          </div>
        </div>

        <PublishDialog
          open={publishOpen}
          onOpenChange={setPublishOpen}
          pending={publishMutation.isPending}
          blockingValidation={publishBlock}
          onPublish={(note) => publishMutation.mutate(note)}
        />
      </NodeDefsContext.Provider>
    </ErrorNodesContext.Provider>
  );
}
