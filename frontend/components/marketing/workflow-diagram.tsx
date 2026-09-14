"use client";

import { useEffect, useState } from "react";
import { cn } from "@/lib/utils";

type WorkflowNode = {
  x: number;
  y: number;
  label: string;
  kind: "trigger" | "ai" | "logic" | "action";
};

const KIND_COLOR: Record<WorkflowNode["kind"], string> = {
  trigger: "#818cf8",
  ai: "#6366f1",
  logic: "#f59e0b",
  action: "#22c55e",
};

const NODE_W = 150;
const NODE_H = 52;

const NODES: WorkflowNode[] = [
  {
    x: 16,
    y: 154,
    label: "Webhook",
    kind: "trigger",
  },
  {
    x: 200,
    y: 154,
    label: "AI Agent",
    kind: "ai",
  },
  {
    x: 384,
    y: 154,
    label: "Decision",
    kind: "logic",
  },
  {
    x: 556,
    y: 72,
    label: "Create Incident",
    kind: "action",
  },
  {
    x: 556,
    y: 236,
    label: "Notify Slack",
    kind: "action",
  },
];

const EDGES = [
  { from: 0, to: 1 },
  { from: 1, to: 2 },
  { from: 2, to: 3 },
  { from: 2, to: 4 },
];

function edgePath(from: WorkflowNode, to: WorkflowNode) {
  const x1 = from.x + NODE_W;
  const y1 = from.y + NODE_H / 2;

  const x2 = to.x;
  const y2 = to.y + NODE_H / 2;

  const mx = (x1 + x2) / 2;

  return `M ${x1} ${y1} C ${mx} ${y1}, ${mx} ${y2}, ${x2} ${y2}`;
}

/**
 * Animated workflow visualization used by the FlowOps marketing page.
 */
export function WorkflowDiagram({
  className,
}: {
  className?: string;
}) {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  return (
    <svg
      viewBox="0 0 720 340"
      className={cn("h-auto w-full", className)}
      role="img"
      aria-label="Example FlowOps workflow showing a webhook triggering an AI agent, followed by a decision that creates an incident or notifies Slack."
    >
      <defs>
        {/* Background grid */}
        <pattern
          id="workflow-grid"
          width="24"
          height="24"
          patternUnits="userSpaceOnUse"
        >
          <path
            d="M 24 0 L 0 0 0 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="0.5"
            opacity="0.15"
          />
        </pattern>

        {/* Edge gradient */}
        <linearGradient
          id="workflow-edge-gradient"
          x1="0%"
          y1="0%"
          x2="100%"
          y2="0%"
        >
          <stop
            offset="0%"
            stopColor="#818cf8"
            stopOpacity="0.55"
          />
          <stop
            offset="50%"
            stopColor="#6366f1"
            stopOpacity="0.4"
          />
          <stop
            offset="100%"
            stopColor="#a78bfa"
            stopOpacity="0.55"
          />
        </linearGradient>

        {/* Node gradients */}
        {NODES.map((node) => (
          <linearGradient
            key={node.label}
            id={`workflow-node-${node.kind}`}
            x1="0%"
            y1="0%"
            x2="100%"
            y2="100%"
          >
            <stop
              offset="0%"
              stopColor={KIND_COLOR[node.kind]}
              stopOpacity="0.16"
            />
            <stop
              offset="100%"
              stopColor={KIND_COLOR[node.kind]}
              stopOpacity="0.04"
            />
          </linearGradient>
        ))}

        {/* Node shadow */}
        <filter
          id="workflow-shadow"
          x="-30%"
          y="-30%"
          width="160%"
          height="160%"
        >
          <feDropShadow
            dx="0"
            dy="4"
            stdDeviation="7"
            floodColor="#000000"
            floodOpacity="0.25"
          />
        </filter>

        {/* Glow */}
        <filter
          id="workflow-glow"
          x="-100%"
          y="-100%"
          width="300%"
          height="300%"
        >
          <feGaussianBlur
            stdDeviation="3"
            result="blur"
          />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>

        {/* Invisible paths used by particles */}
        {EDGES.map((edge) => {
          const from = NODES[edge.from];
          const to = NODES[edge.to];

          return (
            <path
              key={`motion-path-${edge.from}-${edge.to}`}
              id={`motion-path-${edge.from}-${edge.to}`}
              d={edgePath(from, to)}
              fill="none"
              stroke="none"
            />
          );
        })}
      </defs>

      {/* Grid */}
      <rect
        width="720"
        height="340"
        fill="url(#workflow-grid)"
        opacity="0.5"
      />

      {/* Connection lines */}
      <g
        fill="none"
        stroke="url(#workflow-edge-gradient)"
        strokeWidth="2"
        strokeLinecap="round"
      >
        {EDGES.map((edge, index) => {
          const from = NODES[edge.from];
          const to = NODES[edge.to];

          return (
            <path
              key={`edge-${edge.from}-${edge.to}`}
              d={edgePath(from, to)}
              strokeDasharray="12 8"
              opacity="0.75"
              style={{
                animation:
                  "workflow-flow 1.6s linear infinite",
                animationDelay: `${index * 250}ms`,
              }}
            />
          );
        })}
      </g>

      {/* Animated particles */}
      {mounted &&
        EDGES.map((edge, index) => (
          <circle
            key={`particle-${edge.from}-${edge.to}`}
            r="3"
            fill="#ffffff"
            filter="url(#workflow-glow)"
          >
            <animateMotion
              dur="2.5s"
              repeatCount="indefinite"
              begin={`${index * 0.45}s`}
            >
              <mpath
                href={`#motion-path-${edge.from}-${edge.to}`}
              />
            </animateMotion>
          </circle>
        ))}

      {/* Workflow nodes */}
      {NODES.map((node, index) => (
        <g
          key={node.label}
          filter="url(#workflow-shadow)"
        >
          {/* Node body */}
          <rect
            x={node.x}
            y={node.y}
            width={NODE_W}
            height={NODE_H}
            rx="12"
            fill={`url(#workflow-node-${node.kind})`}
            stroke={KIND_COLOR[node.kind]}
            strokeWidth="1.5"
            opacity={mounted ? 1 : 0}
            style={{
              animation: `workflow-rise 0.5s ease-out ${
                index * 120
              }ms both`,
            }}
          />

          {/* Accent bar */}
          <rect
            x={node.x}
            y={node.y}
            width="4"
            height={NODE_H}
            rx="2"
            fill={KIND_COLOR[node.kind]}
            opacity={mounted ? 1 : 0}
            style={{
              animation: `workflow-rise 0.5s ease-out ${
                index * 120
              }ms both`,
            }}
          />

          {/* Status dot */}
          <circle
            cx={node.x + 18}
            cy={node.y + NODE_H / 2}
            r="6"
            fill={KIND_COLOR[node.kind]}
            opacity={mounted ? 1 : 0}
            style={{
              animation: `workflow-rise 0.5s ease-out ${
                index * 120
              }ms both`,
            }}
          >
            <animate
              attributeName="r"
              values="5;6;5"
              dur="3s"
              repeatCount="indefinite"
            />
          </circle>

          {/* Label */}
          <text
            x={node.x + 36}
            y={node.y + NODE_H / 2 + 4}
            fill="currentColor"
            fontSize="13"
            fontWeight="500"
            opacity={mounted ? 1 : 0}
            style={{
              animation: `workflow-rise 0.5s ease-out ${
                index * 120 + 100
              }ms both`,
            }}
          >
            {node.label}
          </text>
        </g>
      ))}

      {/* Animation styles */}
      <style>
        {`
          @keyframes workflow-flow {
            from {
              stroke-dashoffset: 0;
            }

            to {
              stroke-dashoffset: -40;
            }
          }

          @keyframes workflow-rise {
            from {
              opacity: 0;
              transform: translateY(8px);
            }

            to {
              opacity: 1;
              transform: translateY(0);
            }
          }

          @media (prefers-reduced-motion: reduce) {
            path {
              animation: none !important;
            }

            circle {
              animation: none !important;
            }

            text,
            rect {
              animation: none !important;
              opacity: 1 !important;
            }
          }
        `}
      </style>
    </svg>
  );
}