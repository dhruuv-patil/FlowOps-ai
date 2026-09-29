"use client";

import * as React from "react";

import { useParams, useRouter } from "next/navigation";

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
  runWorkflow,
  saveWorkflowGraph,
  updateWorkflow,
  validateWorkflow,
} from "@/lib/api";

import type {
  GenerateWorkflowResult,
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

import { WorkflowHeader } from "@/components/app/workflow-header";
import { WorkflowExecutionsView } from "@/components/app/workflow-executions-view";
import { WorkflowAnomaliesView } from "@/components/app/workflow-anomalies-view";

import { FlowNode, NodeDefsContext } from "@/components/app/builder/flow-node";
import { NodePalette } from "@/components/app/builder/node-palette";
import { ConfigPanel } from "@/components/app/builder/config-panel";
import { Toolbar } from "@/components/app/builder/toolbar";
import { ValidationPanel } from "@/components/app/builder/validation-panel";
import { PublishDialog } from "@/components/app/builder/publish-dialog";
import { GenerateWorkflowDialog } from "@/components/app/builder/generate-workflow-dialog";
import { WebhookDialog } from "@/components/app/builder/webhook-dialog";

import { hasRole, useAuthStore } from "@/lib/auth-store";

/* ================================================================
   Layout configuration
   ================================================================ */

/* ================================================================
   Layout configuration
   ================================================================ */

const LAYOUT = {
  nodeWidth: 180,
  nodeHeight: 84,

  horizontalGap: 110,
  verticalGap: 72,

  componentGap: 220,

  topPadding: 80,
  leftPadding: 80,
};

/* ================================================================
   Production Workflow Layout
   ================================================================ */

function layoutWorkflow(
  nodes: FlowNodeType[],
  edges: FlowEdge[],
): FlowNodeType[] {
  const LAYOUT = {
    nodeWidth: 180,
    nodeHeight: 84,
    horizontalGap: 110,
    verticalGap: 72,
    componentGap: 220,
    topPadding: 80,
    leftPadding: 80,
  };

  if (nodes.length === 0) {
    return [];
  }

  const nodeById = new Map(
    nodes.map((node) => [
      node.id,
      node,
    ]),
  );

  const outgoing =
    new Map<string, string[]>();

  const incoming =
    new Map<string, string[]>();

  const undirected =
    new Map<string, Set<string>>();

  for (const node of nodes) {
    outgoing.set(node.id, []);
    incoming.set(node.id, []);
    undirected.set(
      node.id,
      new Set(),
    );
  }

  for (const edge of edges) {
    if (
      !nodeById.has(edge.source) ||
      !nodeById.has(edge.target)
    ) {
      continue;
    }

    outgoing
      .get(edge.source)!
      .push(edge.target);

    incoming
      .get(edge.target)!
      .push(edge.source);

    undirected
      .get(edge.source)!
      .add(edge.target);

    undirected
      .get(edge.target)!
      .add(edge.source);
  }

  /* ------------------------------------------------------------
     Connected components
     ------------------------------------------------------------ */

  const components: string[][] = [];
  const visited = new Set<string>();

  for (const node of nodes) {
    if (visited.has(node.id)) {
      continue;
    }

    const component: string[] = [];
    const queue = [node.id];

    visited.add(node.id);

    while (queue.length > 0) {
      const current = queue.shift()!;

      component.push(current);

      for (
        const next of
          undirected.get(current) ?? []
      ) {
        if (visited.has(next)) {
          continue;
        }

        visited.add(next);
        queue.push(next);
      }
    }

    components.push(component);
  }

  const positions =
    new Map<
      string,
      {
        x: number;
        y: number;
      }
    >();

  let componentOffsetX =
    LAYOUT.leftPadding;

  /* ============================================================
     Layout each component
     ============================================================ */

  for (const component of components) {
    const componentSet =
      new Set(component);

    /* ----------------------------------------------------------
       Calculate levels
       ---------------------------------------------------------- */

    const indegree =
      new Map<string, number>();

    const level =
      new Map<string, number>();

    for (const id of component) {
      const degree = (
        incoming.get(id) ?? []
      ).filter((source) =>
        componentSet.has(source),
      ).length;

      indegree.set(id, degree);
      level.set(id, 0);
    }

    const queue =
      component
        .filter(
          (id) =>
            (indegree.get(id) ?? 0) === 0,
        )
        .sort((a, b) =>
          a.localeCompare(b),
        );

    const processed =
      new Set<string>();

    while (queue.length > 0) {
      const current =
        queue.shift()!;

      processed.add(current);

      const currentLevel =
        level.get(current) ?? 0;

      for (
        const target of
          outgoing.get(current) ?? []
      ) {
        if (
          !componentSet.has(target)
        ) {
          continue;
        }

        level.set(
          target,
          Math.max(
            level.get(target) ?? 0,
            currentLevel + 1,
          ),
        );

        const nextDegree =
          (indegree.get(target) ?? 0) - 1;

        indegree.set(
          target,
          nextDegree,
        );

        if (nextDegree === 0) {
          queue.push(target);
        }
      }
    }

    /* ----------------------------------------------------------
       Cycle fallback
       ---------------------------------------------------------- */

    let deepestLevel = 0;

    for (const id of component) {
      deepestLevel = Math.max(
        deepestLevel,
        level.get(id) ?? 0,
      );
    }

    component
      .filter(
        (id) => !processed.has(id),
      )
      .forEach((id, index) => {
        level.set(
          id,
          deepestLevel + index + 1,
        );
      });

    /* ----------------------------------------------------------
       Group by level
       ---------------------------------------------------------- */

    const levels =
      new Map<number, string[]>();

    for (const id of component) {
      const currentLevel =
        level.get(id) ?? 0;

      if (!levels.has(currentLevel)) {
        levels.set(currentLevel, []);
      }

      levels
        .get(currentLevel)!
        .push(id);
    }

    const levelNumbers = [
      ...levels.keys(),
    ].sort((a, b) => a - b);

    /* ----------------------------------------------------------
       Deterministic branch ordering
       ---------------------------------------------------------- */

    for (const levelNumber of levelNumbers) {
      const ids =
        levels.get(levelNumber)!;

      ids.sort((a, b) => {
        const aNode =
          nodeById.get(a);

        const bNode =
          nodeById.get(b);

        const aLabel =
          String(
            aNode?.data?.label ?? a,
          );

        const bLabel =
          String(
            bNode?.data?.label ?? b,
          );

        return (
          aLabel.localeCompare(bLabel) ||
          a.localeCompare(b)
        );
      });
    }

    /* ----------------------------------------------------------
       Calculate subtree heights.

       This makes branch nodes reserve space for their entire
       downstream branch instead of independently centering
       every column.
       ---------------------------------------------------------- */

    const subtreeHeight =
      new Map<string, number>();

    const visiting =
      new Set<string>();

    const calculateHeight = (
      id: string,
    ): number => {
      const cached =
        subtreeHeight.get(id);

      if (cached !== undefined) {
        return cached;
      }

      if (visiting.has(id)) {
        return LAYOUT.nodeHeight;
      }

      visiting.add(id);

      const children =
        (outgoing.get(id) ?? [])
          .filter((child) =>
            componentSet.has(child),
          );

      if (children.length === 0) {
        visiting.delete(id);

        subtreeHeight.set(
          id,
          LAYOUT.nodeHeight,
        );

        return LAYOUT.nodeHeight;
      }

      const childHeights =
        children.map(
          calculateHeight,
        );

      const childrenHeight =
        childHeights.reduce(
          (sum, height) =>
            sum + height,
          0,
        ) +
        Math.max(
          0,
          children.length - 1,
        ) *
          LAYOUT.verticalGap;

      const height =
        Math.max(
          LAYOUT.nodeHeight,
          childrenHeight,
        );

      visiting.delete(id);

      subtreeHeight.set(
        id,
        height,
      );

      return height;
    };

    for (const id of component) {
      calculateHeight(id);
    }

    /* ----------------------------------------------------------
       Find roots
       ---------------------------------------------------------- */

    const roots =
      component.filter((id) => {
        const parents =
          (incoming.get(id) ?? [])
            .filter((parent) =>
              componentSet.has(parent),
            );

        return parents.length === 0;
      });

    /*
     * In a cyclic component there may be no root.
     * Use the first node as a deterministic fallback.
     */
    if (roots.length === 0) {
      roots.push(
        [...component].sort(
          (a, b) =>
            a.localeCompare(b),
        )[0],
      );
    }

    /* ----------------------------------------------------------
       Place tree recursively
       ---------------------------------------------------------- */

    const placed =
      new Set<string>();

    const placeNode = (
      id: string,
      x: number,
      centerY: number,
    ) => {
      if (placed.has(id)) {
        return;
      }

      placed.add(id);

      positions.set(id, {
        x,
        y:
          centerY -
          LAYOUT.nodeHeight / 2,
      });

      const children =
        (outgoing.get(id) ?? [])
          .filter((child) =>
            componentSet.has(child),
          );

      if (children.length === 0) {
        return;
      }

      const childHeights =
        children.map(
          (child) =>
            subtreeHeight.get(
              child,
            ) ??
            LAYOUT.nodeHeight,
        );

      const totalHeight =
        childHeights.reduce(
          (sum, height) =>
            sum + height,
          0,
        ) +
        Math.max(
          0,
          children.length - 1,
        ) *
          LAYOUT.verticalGap;

      let childTop =
        centerY -
        totalHeight / 2;

      children.forEach(
        (child, index) => {
          const height =
            childHeights[index];

          const childCenter =
            childTop +
            height / 2;

          placeNode(
            child,
            x +
              LAYOUT.nodeWidth +
              LAYOUT.horizontalGap,
            childCenter,
          );

          childTop +=
            height +
            LAYOUT.verticalGap;
        },
      );
    };

    /* ----------------------------------------------------------
       Place roots
       ---------------------------------------------------------- */

    let rootY =
      LAYOUT.topPadding;

    for (const root of roots) {
      const height =
        subtreeHeight.get(root) ??
        LAYOUT.nodeHeight;

      placeNode(
        root,
        componentOffsetX,
        rootY +
          height / 2,
      );

      rootY +=
        height +
        LAYOUT.verticalGap;
    }

    /* ----------------------------------------------------------
       Place any nodes not reached by recursive traversal.

       This handles unusual graphs/cycles safely.
       ---------------------------------------------------------- */

    const unplaced =
      component.filter(
        (id) => !placed.has(id),
      );

    for (
      const id of unplaced
    ) {
      const currentLevel =
        level.get(id) ?? 0;

      const sameLevel =
        component.filter(
          (candidate) =>
            (level.get(candidate) ?? 0) ===
              currentLevel &&
            placed.has(candidate),
        );

      positions.set(id, {
        x:
          componentOffsetX +
          currentLevel *
            (
              LAYOUT.nodeWidth +
              LAYOUT.horizontalGap
            ),
        y:
          LAYOUT.topPadding +
          sameLevel.length *
            (
              LAYOUT.nodeHeight +
              LAYOUT.verticalGap
            ),
      });

      placed.add(id);
    }

    /* ----------------------------------------------------------
       Component width
       ---------------------------------------------------------- */

    const maxLevel =
      Math.max(...levelNumbers);

    const componentWidth =
      (maxLevel + 1) *
        (
          LAYOUT.nodeWidth +
          LAYOUT.horizontalGap
        ) -
      LAYOUT.horizontalGap;

    componentOffsetX +=
      componentWidth +
      LAYOUT.componentGap;
  }

  /* ------------------------------------------------------------
     Final collision cleanup
     ------------------------------------------------------------ */

  for (const component of components) {
    const byLevel =
      new Map<
        number,
        FlowNodeType["id"][]
      >();

    for (const id of component) {
      const node =
        nodeById.get(id);

      if (!node) continue;

      const levelIndex = Math.round(
        (
          (
            positions.get(id)?.x ??
            LAYOUT.leftPadding
          ) -
          LAYOUT.leftPadding
        ) /
          (
            LAYOUT.nodeWidth +
            LAYOUT.horizontalGap
          ),
      );

      if (!byLevel.has(levelIndex)) {
        byLevel.set(levelIndex, []);
      }

      byLevel
        .get(levelIndex)!
        .push(id);
    }

    for (const ids of byLevel.values()) {
      ids.sort(
        (a, b) =>
          (
            positions.get(a)?.y ??
            0
          ) -
          (
            positions.get(b)?.y ??
            0
          ),
      );

      for (
        let index = 1;
        index < ids.length;
        index++
      ) {
        const previous =
          positions.get(
            ids[index - 1],
          );

        const current =
          positions.get(
            ids[index],
          );

        if (!previous || !current) {
          continue;
        }

        const minimumY =
          previous.y +
          LAYOUT.nodeHeight +
          LAYOUT.verticalGap;

        if (
          current.y < minimumY
        ) {
          positions.set(
            ids[index],
            {
              ...current,
              y: minimumY,
            },
          );
        }
      }
    }
  }

  /* ------------------------------------------------------------
     Return final nodes
     ------------------------------------------------------------ */

  return nodes.map((node) => ({
    ...node,
    position:
      positions.get(node.id) ?? {
        x: LAYOUT.leftPadding,
        y: LAYOUT.topPadding,
      },
  }));
}

/* ================================================================
   Edge routing
   ================================================================ */

function layoutWorkflowEdges(
  nodes: FlowNodeType[],
  edges: FlowEdge[],
): FlowEdge[] {
  if (edges.length === 0) {
    return [];
  }

  const nodeById = new Map(
    nodes.map((node) => [
      node.id,
      node,
    ]),
  );

  const outgoingCount =
    new Map<string, number>();

  const incomingCount =
    new Map<string, number>();

  for (const edge of edges) {
    outgoingCount.set(
      edge.source,
      (outgoingCount.get(edge.source) ?? 0) + 1,
    );

    incomingCount.set(
      edge.target,
      (incomingCount.get(edge.target) ?? 0) + 1,
    );
  }

  const xValues = [
    ...new Set(
      nodes.map(
        (node) => node.position.x,
      ),
    ),
  ].sort((a, b) => a - b);

  const xLevel =
    new Map<number, number>();

  xValues.forEach((x, index) => {
    xLevel.set(x, index);
  });

  return edges.map((edge) => {
    const source =
      nodeById.get(edge.source);

    const target =
      nodeById.get(edge.target);

    if (!source || !target) {
      return {
        ...edge,
        type: "default",
        animated: false,
      };
    }

    const sourceBranches =
      outgoingCount.get(edge.source) ?? 0;

    const targetMerges =
      incomingCount.get(edge.target) ?? 0;

    const sourceLevel =
      xLevel.get(
        source.position.x,
      ) ?? 0;

    const targetLevel =
      xLevel.get(
        target.position.x,
      ) ?? 0;

    const isSimpleForward =
      sourceBranches === 1 &&
      targetMerges === 1 &&
      targetLevel ===
      sourceLevel + 1;

    return {
      ...edge,

      type: isSimpleForward
        ? "straight"
        : "default",

      animated: false,
    };
  });
}

/* ================================================================
   Page shell
   ================================================================ */

export default function WorkflowDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const queryClient = useQueryClient();

  // IMPORTANT:
  // This hook MUST be before any conditional return.
  const [activeView, setActiveView] =
    React.useState<"editor" | "executions" | "anomalies">("editor");

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

        <Button
          variant="outline"
          onClick={() => workflowQuery.refetch()}
        >
          Retry
        </Button>
      </div>
    );
  }

  const workflow = workflowQuery.data;

  const current = queryClient.getQueryData<WorkflowDetail>([
    "workflow",
    workflow.id,
  ]);

  const status = current?.status ?? workflow.status;
  const version = current?.latestVersion ?? workflow.latestVersion;
  const name = current?.name ?? workflow.name;
  return (
    <div className="flex h-screen flex-col bg-[#050505]">
      <WorkflowHeader
        workflow={workflow}
        version={version}
        dirty={false} // TODO: Implement dirty state tracking
        canRun={version != null}
        runPending={false} // TODO: Implement runPending state
        namePending={false} // TODO: Implement namePending state
        onRename={(n) => {
          // TODO: Implement rename logic
        }}
        onRun={() => {
          // TODO: Implement run logic
        }}
        onGenerate={() => {
          // TODO: Implement generate logic
        }}
        onWebhook={() => {
          // TODO: Implement webhook logic
        }}
        hasWebhookTrigger={false} // TODO: Implement webhook trigger detection
        activeView={activeView}
        onViewChange={setActiveView}
      />

      <div className="flex-1 min-h-0 overflow-hidden bg-[#050505]">
        {activeView === "editor" ? (
          // Editor: NO outer padding - uses full available width
          <ReactFlowProvider>
            <Builder
              workflow={workflow}
              nodeDefs={nodeTypesQuery.data?.nodeTypes ?? []}
              nodeDefsLoading={nodeTypesQuery.isPending}
              nodeDefsError={nodeTypesQuery.isError}
              showToolbar={false} // Toolbar is now in WorkflowHeader
            />
          </ReactFlowProvider>
        ) : activeView === "executions" ? (
          // Executions: keep existing outer padding
          <div className="px-2 lg:px-3 pt-3">
            <WorkflowExecutionsView workflowId={workflow.id} />
          </div>
        ) : (
          // Anomalies: keep existing outer padding
          <div className="px-2 lg:px-3 pt-3">
            <WorkflowAnomaliesView workflowId={workflow.id} />
          </div>
        )}
      </div>
    </div>
  );
}

/* ================================================================
   Builder
   ================================================================ */

interface BuilderProps {
  workflow: WorkflowDetail;
  nodeDefs: NodeDefinition[];
  nodeDefsLoading: boolean;
  nodeDefsError: boolean;
  showToolbar?: boolean;
}

function Builder({
  workflow,
  nodeDefs,
  nodeDefsLoading,
  nodeDefsError,
  showToolbar = true,
}: BuilderProps) {
  const queryClient = useQueryClient();
  const router = useRouter();

  const rf = useReactFlow<FlowNodeType, FlowEdge>();

  const wrapperRef =
    React.useRef<HTMLDivElement>(null);

  /* ================================================================
     Registry lookups
     ================================================================ */

  const defsByType = React.useMemo(() => {
    const map: Record<string, NodeDefinition> = {};

    for (const d of nodeDefs) {
      map[d.type] = d;
    }

    return map;
  }, [nodeDefs]);

  const nodeTypes = React.useMemo<NodeTypes>(() => {
    const map: NodeTypes = {};

    for (const d of nodeDefs) {
      map[d.type] = FlowNode;
    }

    return map;
  }, [nodeDefs]);

  /* ================================================================
     Graph state
     ================================================================ */

  const initial = React.useMemo(
    () => graphToFlow(workflow.graph),
    [workflow.graph],
  );

  const [nodes, setNodes, onNodesChange] =
    useNodesState<FlowNodeType>(initial.nodes);

  const [edges, setEdges, onEdgesChange] =
    useEdgesState<FlowEdge>(initial.edges);

  const [selectedId, setSelectedId] =
    React.useState<string | null>(null);

  const savedRef = React.useRef<string>(
    JSON.stringify(
      flowToGraph(
        initial.nodes,
        initial.edges,
      ),
    ),
  );

  const [dirty, setDirty] =
    React.useState(false);

  const recomputeDirty = React.useCallback(
    (
      nextNodes: FlowNodeType[],
      nextEdges: FlowEdge[],
    ) => {
      const now = JSON.stringify(
        flowToGraph(nextNodes, nextEdges),
      );

      setDirty(now !== savedRef.current);
    },
    [],
  );

  React.useEffect(() => {
    recomputeDirty(nodes, edges);
  }, [
    nodes,
    edges,
    recomputeDirty,
  ]);

  /* ================================================================
     History
     ================================================================ */

  interface Snapshot {
    nodes: FlowNodeType[];
    edges: FlowEdge[];
  }

  const past =
    React.useRef<Snapshot[]>([]);

  const future =
    React.useRef<Snapshot[]>([]);

  const [, setHistTick] =
    React.useState(0);

  const snapshot = React.useCallback(
    (): Snapshot => ({
      nodes: structuredClone(
        rf.getNodes(),
      ),
      edges: structuredClone(
        rf.getEdges(),
      ),
    }),
    [rf],
  );

  const commit = React.useCallback(() => {
    past.current.push(snapshot());

    if (past.current.length > 100) {
      past.current.shift();
    }

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
  }, [
    snapshot,
    setNodes,
    setEdges,
  ]);

  const redo = React.useCallback(() => {
    const next = future.current.pop();

    if (!next) return;

    past.current.push(snapshot());

    setNodes(next.nodes);
    setEdges(next.edges);

    setHistTick((t) => t + 1);
  }, [
    snapshot,
    setNodes,
    setEdges,
  ]);

  /* ================================================================
     Connections
     ================================================================ */

  const onConnect = React.useCallback(
    (conn: Connection) => {
      commit();

      setEdges((eds) =>
        addEdge(
          {
            ...conn,
            type: "straight",
            animated: false,
          },
          eds,
        ),
      );
    },
    [commit, setEdges],
  );

  /* ================================================================
     Add node
     ================================================================ */

  const addNode = React.useCallback(
    (
      type: string,
      screenPos?: {
        x: number;
        y: number;
      },
    ) => {
      const def = defsByType[type];

      if (!def) return;

      const bounds =
        wrapperRef.current?.getBoundingClientRect();

      let position: {
        x: number;
        y: number;
      };

      if (screenPos && bounds) {
        position =
          rf.screenToFlowPosition({
            x: screenPos.x,
            y: screenPos.y,
          });
      } else if (nodes.length > 0) {
        const rightmost = nodes.reduce(
          (current, node) =>
            node.position.x >
              current.position.x
              ? node
              : current,
          nodes[0],
        );

        position = {
          x:
            rightmost.position.x +
            LAYOUT.nodeWidth +
            LAYOUT.horizontalGap,

          y: rightmost.position.y,
        };
      } else {
        const center =
          rf.screenToFlowPosition({
            x:
              (bounds?.left ?? 0) +
              (bounds?.width ?? 800) / 2,

            y:
              (bounds?.top ?? 0) +
              (bounds?.height ?? 600) / 2,
          });

        position = {
          x:
            center.x -
            LAYOUT.nodeWidth / 2,

          y:
            center.y -
            LAYOUT.nodeHeight / 2,
        };
      }

      const node: FlowNodeType = {
        id: newNodeId(type),
        type,
        position,
        data: {
          label: def.label,
          config: {},
        },
      };

      commit();

      setNodes((nds) =>
        nds.concat(node),
      );

      setSelectedId(node.id);
    },
    [
      defsByType,
      nodes,
      rf,
      commit,
      setNodes,
    ],
  );

  const onDrop = React.useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();

      const type =
        event.dataTransfer.getData(
          DND_MIME,
        );

      if (!type) return;

      addNode(type, {
        x: event.clientX,
        y: event.clientY,
      });
    },
    [addNode],
  );

  const onDragOver = React.useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();

      event.dataTransfer.dropEffect =
        "move";
    },
    [],
  );

  /* ================================================================
     Selection
     ================================================================ */

  const onSelectionChange =
    React.useCallback(
      ({
        nodes: selected,
      }: OnSelectionChangeParams) => {
        setSelectedId(
          selected.length === 1
            ? selected[0].id
            : null,
        );
      },
      [],
    );

  /* ================================================================
     Delete / drag history
     ================================================================ */

  const onBeforeDelete =
    React.useCallback(async () => {
      commit();
      return true;
    }, [commit]);

  const onNodeDragStart =
    React.useCallback(() => {
      commit();
    }, [commit]);

  /* ================================================================
     Inspector
     ================================================================ */

  const selectedNode = React.useMemo(
    () =>
      nodes.find(
        (n) => n.id === selectedId,
      ) ?? null,
    [nodes, selectedId],
  );

  const updateSelected =
    React.useCallback(
      (
        updater: (
          data: FlowNodeType["data"],
        ) => FlowNodeType["data"],
      ) => {
        if (!selectedId) return;

        setNodes((nds) =>
          nds.map((n) =>
            n.id === selectedId
              ? {
                ...n,
                data: updater(n.data),
              }
              : n,
          ),
        );
      },
      [selectedId, setNodes],
    );

  const onLabelChange =
    React.useCallback(
      (label: string) =>
        updateSelected((data) => ({
          ...data,
          label,
        })),
      [updateSelected],
    );

  const onConfigChange =
    React.useCallback(
      (
        key: string,
        value: unknown,
      ) =>
        updateSelected((data) => ({
          ...data,
          config: {
            ...(data.config ?? {}),
            [key]: value,
          },
        })),
      [updateSelected],
    );

  const upstreamIds =
    React.useMemo(() => {
      if (!selectedId) return [];

      return edges
        .filter(
          (e) =>
            e.target === selectedId,
        )
        .map((e) => e.source)
        .filter(
          (v, i, a) =>
            a.indexOf(v) === i,
        );
    }, [edges, selectedId]);

  /* ================================================================
     Validation
     ================================================================ */

  const [
    validation,
    setValidation,
  ] =
    React.useState<ValidationResult | null>(
      null,
    );

  const [
    showValidation,
    setShowValidation,
  ] = React.useState(false);

  const errorNodeIds =
    React.useMemo(() => {
      const set = new Set<string>();

      for (const issue of
        validation?.issues ?? []) {
        if (
          issue.severity === "ERROR" &&
          issue.nodeId
        ) {
          set.add(issue.nodeId);
        }
      }

      return set;
    }, [validation]);

  const focusNode =
    React.useCallback(
      (nodeId: string) => {
        const node =
          rf.getNode(nodeId);

        if (!node) return;

        setSelectedId(nodeId);

        rf.setCenter(
          node.position.x +
          LAYOUT.nodeWidth / 2,

          node.position.y +
          LAYOUT.nodeHeight / 2,

          {
            zoom: 1.2,
            duration: 400,
          },
        );
      },
      [rf],
    );

  /* ================================================================
     Server mutations
     ================================================================ */

  const saveMutation =
    useMutation({
      mutationFn: () =>
        saveWorkflowGraph(
          workflow.id,
          flowToGraph(
            nodes,
            edges,
          ),
        ),

      onSuccess: (detail) => {
        savedRef.current =
          JSON.stringify(
            detail.graph,
          );

        setDirty(false);

        queryClient.setQueryData(
          ["workflow", workflow.id],
          detail,
        );

        toast.success(
          "Workflow saved.",
        );
      },

      onError: (err) =>
        toast.error(
          getErrorMessage(err),
        ),
    });

  const validateMutation =
    useMutation({
      mutationFn: () =>
        validateWorkflow(
          workflow.id,
        ),

      onSuccess: (result) => {
        setValidation(result);
        setShowValidation(true);

        if (result.valid) {
          toast.success(
            "Validation passed.",
          );
        } else {
          const errs =
            result.issues.filter(
              (i) =>
                i.severity ===
                "ERROR",
            ).length;

          toast.error(
            `${errs} validation error(s) found.`,
          );
        }
      },

      onError: (err) =>
        toast.error(
          getErrorMessage(err),
        ),
    });

  const [
    publishOpen,
    setPublishOpen,
  ] = React.useState(false);

  const [
    publishBlock,
    setPublishBlock,
  ] =
    React.useState<ValidationResult | null>(
      null,
    );

  const publishMutation =
    useMutation({
      mutationFn: (note: string) =>
        publishWorkflow(
          workflow.id,
          note || undefined,
        ),

      onSuccess: (result) => {
        setValidation(
          result.validation,
        );

        if (
          result.published &&
          result.version
        ) {
          setPublishBlock(null);
          setPublishOpen(false);
          setShowValidation(false);

          queryClient.invalidateQueries({
            queryKey: [
              "workflow",
              workflow.id,
            ],
          });

          toast.success(
            `Published version v${result.version.versionNumber}.`,
          );
        } else {
          setPublishBlock(
            result.validation,
          );

          setShowValidation(true);

          toast.error(
            "Publish blocked by validation errors.",
          );
        }
      },

      onError: (err) =>
        toast.error(
          getErrorMessage(err),
        ),
    });

  const renameMutation =
    useMutation({
      mutationFn: (name: string) =>
        updateWorkflow(
          workflow.id,
          {
            name,
            description:
              workflow.description ??
              undefined,
          },
        ),

      onSuccess: (detail) => {
        queryClient.setQueryData(
          ["workflow", workflow.id],
          detail,
        );

        toast.success(
          "Workflow renamed.",
        );
      },

      onError: (err) =>
        toast.error(
          getErrorMessage(err),
        ),
    });

  const runMutation =
    useMutation({
      mutationFn: () =>
        runWorkflow(workflow.id),

      onSuccess: (execution) => {
        toast.success(
          "Run started.",
        );

        router.push(
          `/executions/${execution.id}`,
        );
      },

      onError: (err) =>
        toast.error(
          getErrorMessage(err),
        ),
    });

  const doSave =
    React.useCallback(() => {
      if (
        !saveMutation.isPending
      ) {
        saveMutation.mutate();
      }
    }, [saveMutation]);

  /* ================================================================
     AI generation
     ================================================================ */

  const [
    generateOpen,
    setGenerateOpen,
  ] = React.useState(false);

  /* ================================================================
     Webhook
     ================================================================ */

  const [
    webhookOpen,
    setWebhookOpen,
  ] = React.useState(false);

  const hasWebhookTrigger =
    React.useMemo(
      () =>
        nodes.some(
          (n) =>
            n.type ===
            "webhook_trigger",
        ),
      [nodes],
    );

  const canManageWebhook =
    hasRole(
      useAuthStore(
        (s) => s.currentRole,
      ),
      "ADMIN",
    );

  /* ================================================================
     Accept AI-generated workflow
     ================================================================ */

  const acceptGenerated =
    React.useCallback(
      (
        result: GenerateWorkflowResult,
      ) => {
        const next =
          graphToFlow(
            result.graph,
          );

        /*
         * First arrange the graph using topology.
         */
        const laidOutNodes =
          layoutWorkflow(
            next.nodes,
            next.edges,
          );

        /*
         * Then decide edge routing based
         * on actual graph geometry.
         */
        const laidOutEdges =
          layoutWorkflowEdges(
            laidOutNodes,
            next.edges,
          );

        commit();

        setNodes(
          laidOutNodes,
        );

        setEdges(
          laidOutEdges,
        );

        setSelectedId(null);

        /*
         * Two animation frames allow React Flow
         * to measure the newly-created nodes
         * before fitting the viewport.
         */
        window.requestAnimationFrame(
          () => {
            window.requestAnimationFrame(
              () => {
                rf.fitView({
                  duration: 350,
                  padding: 0.2,
                });
              },
            );
          },
        );

        const n =
          laidOutNodes.length;

        toast.success(
          `Added ${n} node${n === 1 ? "" : "s"
          }. Review, then save.`,
        );
      },
      [
        commit,
        setNodes,
        setEdges,
        rf,
      ],
    );

  /* ================================================================
     Keyboard shortcuts
     ================================================================ */

  React.useEffect(() => {
    function onKey(
      e: KeyboardEvent,
    ) {
      const target =
        e.target as HTMLElement | null;

      const inField =
        target &&
        (target.tagName ===
          "INPUT" ||
          target.tagName ===
          "TEXTAREA" ||
          target.tagName ===
          "SELECT" ||
          target.isContentEditable);

      const mod =
        e.metaKey || e.ctrlKey;

      if (
        mod &&
        e.key.toLowerCase() ===
        "s"
      ) {
        e.preventDefault();
        doSave();
      } else if (
        mod &&
        !inField &&
        e.key.toLowerCase() ===
        "z"
      ) {
        e.preventDefault();

        if (e.shiftKey) {
          redo();
        } else {
          undo();
        }
      }
    }

    window.addEventListener(
      "keydown",
      onKey,
    );

    return () =>
      window.removeEventListener(
        "keydown",
        onKey,
      );
  }, [
    doSave,
    undo,
    redo,
  ]);

  /* ================================================================
     Current workflow state
     ================================================================ */

  const current =
    queryClient.getQueryData<WorkflowDetail>(
      [
        "workflow",
        workflow.id,
      ],
    );

  const status =
    current?.status ??
    workflow.status;

  const version =
    current?.latestVersion ??
    workflow.latestVersion;

  const name =
    current?.name ??
    workflow.name;

  /* ================================================================
     Panels
     ================================================================ */

  const [
    paletteCollapsed,
    setPaletteCollapsed,
  ] = React.useState(false);

  const [
    inspectorOpen,
    setInspectorOpen,
  ] = React.useState(true);

  React.useEffect(() => {
    if (selectedId) {
      setInspectorOpen(true);
    }
  }, [selectedId]);

  /* ================================================================
     Render
     ================================================================ */

  return (
    <ErrorNodesContext.Provider
      value={errorNodeIds}
    >
      <NodeDefsContext.Provider
        value={defsByType}
      >
        <div className="flex h-full flex-col">
          {showToolbar && (
            <Toolbar
              name={name}
              status={status}
              version={version}
              dirty={dirty}
              canUndo={
                past.current.length > 0
              }
              canRedo={
                future.current.length > 0
              }
              savePending={
                saveMutation.isPending
              }
              validatePending={
                validateMutation.isPending
              }
              namePending={
                renameMutation.isPending
              }
              canRun={version != null}
              runPending={
                runMutation.isPending
              }
              onRename={(n) =>
                renameMutation.mutate(n)
              }
              onUndo={undo}
              onRedo={redo}
              onZoomIn={() =>
                rf.zoomIn({
                  duration: 200,
                })
              }
              onZoomOut={() =>
                rf.zoomOut({
                  duration: 200,
                })
              }
              onFitView={() =>
                rf.fitView({
                  duration: 300,
                  padding: 0.2,
                })
              }
              onValidate={() =>
                validateMutation.mutate()
              }
              onSave={doSave}
              onPublish={() => {
                setPublishBlock(null);
                setPublishOpen(true);
              }}
              onRun={() =>
                runMutation.mutate()
              }
              onGenerate={() =>
                setGenerateOpen(true)
              }
              onWebhook={() =>
                setWebhookOpen(true)
              }
              hasWebhookTrigger={
                hasWebhookTrigger
              }
            />
          )}

          <div className="flex min-h-0 flex-1">
            <NodePalette
              defs={nodeDefs}
              collapsed={
                paletteCollapsed
              }
              onToggle={() =>
                setPaletteCollapsed(
                  (c) => !c,
                )
              }
              onAdd={(type) =>
                addNode(type)
              }
              isLoading={
                nodeDefsLoading
              }
              isError={
                nodeDefsError
              }
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
                onNodesChange={
                  onNodesChange
                }
                onEdgesChange={
                  onEdgesChange
                }
                onConnect={
                  onConnect
                }
                onSelectionChange={
                  onSelectionChange
                }
                onNodeDragStart={
                  onNodeDragStart
                }
                onBeforeDelete={
                  onBeforeDelete
                }
                deleteKeyCode={[
                  "Delete",
                  "Backspace",
                ]}
                multiSelectionKeyCode={[
                  "Meta",
                  "Control",
                  "Shift",
                ]}
                defaultEdgeOptions={{
                  type: "straight",
                  animated: false,
                }}
                fitView
                proOptions={{
                  hideAttribution:
                    true,
                }}
                className="bg-[#050505]"
              >
                <Background
                  className="!bg-white/[0.04]"
                  gap={16}
                />

                <Controls className="!shadow-md" />

                <MiniMap
                  pannable
                  zoomable
                  className="!bg-card"
                  nodeClassName={() =>
                    "!fill-primary/40"
                  }
                />
              </ReactFlow>

              {nodes.length === 0 && (
                <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
                  <p className="rounded-md border border-dashed border-white/[0.08] bg-[#0a0a0a]/80 px-4 py-3 text-sm text-white/40">
                    Drag a node from the
                    left, or click one to
                    add it.
                  </p>
                </div>
              )}

              <ValidationPanel
                result={
                  showValidation
                    ? validation
                    : null
                }
                onClose={() =>
                  setShowValidation(
                    false,
                  )
                }
                onFocusNode={
                  focusNode
                }
              />
            </div>

            {inspectorOpen && (
              <ConfigPanel
                node={selectedNode}
                def={
                  selectedNode?.type
                    ? defsByType[
                    selectedNode.type
                    ]
                    : undefined
                }
                upstreamIds={
                  upstreamIds
                }
                onClose={() =>
                  setInspectorOpen(
                    false,
                  )
                }
                onLabelChange={
                  onLabelChange
                }
                onConfigChange={
                  onConfigChange
                }
              />
            )}
          </div>
        </div>

        <PublishDialog
          open={publishOpen}
          onOpenChange={
            setPublishOpen
          }
          pending={
            publishMutation.isPending
          }
          blockingValidation={
            publishBlock
          }
          onPublish={(note) =>
            publishMutation.mutate(
              note,
            )
          }
        />

        <GenerateWorkflowDialog
          open={generateOpen}
          onOpenChange={
            setGenerateOpen
          }
          onAccept={
            acceptGenerated
          }
        />

        <WebhookDialog
          open={webhookOpen}
          onOpenChange={
            setWebhookOpen
          }
          workflowId={
            workflow.id
          }
          canManage={
            canManageWebhook
          }
        />
      </NodeDefsContext.Provider>
    </ErrorNodesContext.Provider>
  );
}