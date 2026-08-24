import { cn } from "@/lib/utils";

type Node = {
  x: number;
  y: number;
  label: string;
  kind: "trigger" | "ai" | "logic" | "action";
};

/* Node accent dots. Kept within the blue family so the diagram reads as one
   system, with amber/emerald reserved for logic and action semantics. */
const KIND_COLOR: Record<Node["kind"], string> = {
  trigger: "#9EC5F5",
  ai: "#3B82F6",
  logic: "#F59E0B",
  action: "#10B981",
};

const NODE_W = 150;
const NODE_H = 52;

const NODES: Node[] = [
  { x: 16, y: 154, label: "Webhook", kind: "trigger" },
  { x: 200, y: 154, label: "AI Agent", kind: "ai" },
  { x: 384, y: 154, label: "Decision", kind: "logic" },
  { x: 556, y: 72, label: "Create Incident", kind: "action" },
  { x: 556, y: 236, label: "Notify Slack", kind: "action" },
];

function edge(from: Node, to: Node) {
  const x1 = from.x + NODE_W;
  const y1 = from.y + NODE_H / 2;
  const x2 = to.x;
  const y2 = to.y + NODE_H / 2;
  const mx = (x1 + x2) / 2;
  return `M ${x1} ${y1} C ${mx} ${y1}, ${mx} ${y2}, ${x2} ${y2}`;
}

/**
 * Decorative, animated workflow visualization for the marketing hero.
 * Pure SVG + CSS (the `animate-flow-dash` keyframe), so it renders on the
 * server with no JS and scales cleanly at any width.
 */
export function WorkflowDiagram({ className }: { className?: string }) {
  const [trigger, ai, decision, actionA, actionB] = NODES;

  return (
    <svg
      viewBox="0 0 720 340"
      className={cn("h-auto w-full", className)}
      role="img"
      aria-label="Example FlowOps workflow: a webhook triggers an AI agent, a decision node branches to create an incident or notify Slack."
    >
      <g
        fill="none"
        stroke="hsl(var(--primary))"
        strokeWidth="2"
        strokeDasharray="6 6"
        className="animate-flow-dash"
        opacity="0.7"
      >
        <path d={edge(trigger, ai)} />
        <path d={edge(ai, decision)} />
        <path d={edge(decision, actionA)} />
        <path d={edge(decision, actionB)} />
      </g>

      {NODES.map((n) => (
        <g key={n.label}>
          <rect
            x={n.x}
            y={n.y}
            width={NODE_W}
            height={NODE_H}
            rx={12}
            style={{
              fill: "hsl(var(--card))",
              stroke: "hsl(var(--border))",
            }}
            strokeWidth={1}
          />
          <circle cx={n.x + 20} cy={n.y + NODE_H / 2} r={5} fill={KIND_COLOR[n.kind]} />
          <text
            x={n.x + 36}
            y={n.y + NODE_H / 2 + 4}
            style={{ fill: "hsl(var(--card-foreground))" }}
            fontSize={13}
            fontWeight={500}
            fontFamily="var(--font-sans)"
          >
            {n.label}
          </text>
        </g>
      ))}
    </svg>
  );
}
