"use client";

import * as React from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Bot, Loader2, Play, Save, Trash2 } from "lucide-react";
import { toast } from "sonner";

import {
  deleteAiAgent,
  fetchAiAgent,
  getErrorMessage,
  getFieldErrors,
  runAiAgent,
  updateAiAgent,
} from "@/lib/api";
import { AI_AGENT_TOOLS, type AgentRunResult, type AiAgentDetail } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

/* ================================================================ page shell */

export default function AgentEditorPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;

  const query = useQuery({
    queryKey: ["ai-agent", id],
    queryFn: () => fetchAiAgent(id),
    enabled: Boolean(id),
  });

  if (query.isPending) {
    return (
      <div className="mx-auto max-w-5xl space-y-4">
        <Skeleton className="h-8 w-64 rounded-xl border border-white/[0.08]" />
        <div className="grid gap-6 lg:grid-cols-2">
          <Skeleton className="h-[70vh] w-full rounded-xl border border-white/[0.08]" />
          <Skeleton className="h-[70vh] w-full rounded-xl border border-white/[0.08]" />
        </div>
      </div>
    );
  }

  if (query.isError) {
    return (
      <div className="mx-auto flex max-w-5xl flex-col items-center justify-center gap-3 py-24 text-center">
        <p className="text-sm text-destructive">
          {getErrorMessage(query.error)}
        </p>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => query.refetch()}>
            Retry
          </Button>
          <Button variant="ghost" asChild>
            <Link href="/agents">Back to agents</Link>
          </Button>
        </div>
      </div>
    );
  }

  return <AgentEditor agent={query.data} />;
}

/* ==================================================================== editor */

interface FormState {
  name: string;
  description: string;
  instructions: string;
  model: string;
  tools: string[];
}

function formFromAgent(agent: AiAgentDetail): FormState {
  return {
    name: agent.name,
    description: agent.description ?? "",
    instructions: agent.instructions,
    model: agent.model ?? "",
    tools: agent.tools,
  };
}

function AgentEditor({ agent }: { agent: AiAgentDetail }) {
  const router = useRouter();
  const queryClient = useQueryClient();

  const [form, setForm] = React.useState<FormState>(() => formFromAgent(agent));
  const [errors, setErrors] = React.useState<Record<string, string>>({});
  const [confirmDelete, setConfirmDelete] = React.useState(false);

  // Re-seed the form when the server copy changes (initial load and post-save,
  // keyed on updatedAt so a save resets the dirty baseline).
  const baseline = React.useMemo(() => formFromAgent(agent), [agent]);
  React.useEffect(() => {
    setForm(baseline);
    setErrors({});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [agent.id, agent.updatedAt]);

  const dirty = React.useMemo(
    () => JSON.stringify(form) !== JSON.stringify(baseline),
    [form, baseline],
  );

  function toggleTool(name: string, on: boolean) {
    setForm((f) => ({
      ...f,
      tools: on ? [...f.tools, name] : f.tools.filter((t) => t !== name),
    }));
  }

  const saveMutation = useMutation({
    mutationFn: () =>
      updateAiAgent(agent.id, {
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        instructions: form.instructions.trim(),
        model: form.model.trim() || undefined,
        tools: form.tools,
      }),
    onSuccess: (detail) => {
      queryClient.setQueryData(["ai-agent", agent.id], detail);
      queryClient.invalidateQueries({ queryKey: ["ai-agents"] });
      toast.success("Agent saved.");
    },
    onError: (err) => {
      const fieldErrors = getFieldErrors(err);
      if (Object.keys(fieldErrors).length > 0) setErrors(fieldErrors);
      else toast.error(getErrorMessage(err, "Could not save the agent."));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteAiAgent(agent.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ai-agents"] });
      toast.success("Agent deleted.");
      router.push("/agents");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  function onSave() {
    const next: Record<string, string> = {};
    if (!form.name.trim()) next.name = "Give your agent a name.";
    if (!form.instructions.trim())
      next.instructions = "Instructions tell the agent what to do.";
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    saveMutation.mutate();
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center gap-3">
        <Button variant="ghost" size="icon" asChild aria-label="Back to agents">
          <Link href="/agents">
            <ArrowLeft className="size-4" />
          </Link>
        </Button>
        <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-white/[0.04] text-white/60">
          <Bot className="size-4" />
        </span>
        <div className="min-w-0 flex-1">
          <h1 className="truncate text-xl font-semibold tracking-tight text-white/90">
            {baseline.name}
          </h1>
          <p className="text-xs text-white/44">
            Edit the agent, then try it in the console.
          </p>
        </div>
        <Button
          variant="outline"
          className="text-destructive hover:text-destructive"
          onClick={() => setConfirmDelete(true)}
        >
          <Trash2 className="size-4" /> Delete
        </Button>
        <Button onClick={onSave} disabled={saveMutation.isPending || !dirty}>
          {saveMutation.isPending ? (
            <Loader2 className="size-4 animate-spin" />
          ) : (
            <Save className="size-4" />
          )}
          Save
        </Button>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Editor pane */}
        <div className="space-y-4 rounded-xl border bg-[#0a0a0a] border-white/[0.08] p-5">
          <h2 className="text-sm font-semibold text-white/90">Configuration</h2>

          <div className="space-y-1.5">
            <Label htmlFor="name">Name</Label>
            <Input
              id="name"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              aria-invalid={!!errors.name}
            />
            {errors.name && (
              <p className="text-xs text-destructive">{errors.name}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="description">Description</Label>
            <Input
              id="description"
              value={form.description}
              placeholder="What is this agent for?"
              onChange={(e) =>
                setForm((f) => ({ ...f, description: e.target.value }))
              }
              aria-invalid={!!errors.description}
            />
            {errors.description && (
              <p className="text-xs text-destructive">{errors.description}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="instructions">Instructions</Label>
            <Textarea
              id="instructions"
              rows={8}
              value={form.instructions}
              placeholder="You are a helpful assistant that…"
              onChange={(e) =>
                setForm((f) => ({ ...f, instructions: e.target.value }))
              }
              aria-invalid={!!errors.instructions}
            />
            {errors.instructions && (
              <p className="text-xs text-destructive">{errors.instructions}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="model">Model</Label>
            <Input
              id="model"
              value={form.model}
              placeholder="Default (server-configured)"
              onChange={(e) =>
                setForm((f) => ({ ...f, model: e.target.value }))
              }
              aria-invalid={!!errors.model}
            />
            <p className="text-[11px] text-white/44">
              Leave blank to use the AI service&apos;s configured model.
            </p>
            {errors.model && (
              <p className="text-xs text-destructive">{errors.model}</p>
            )}
          </div>

          <div className="space-y-2 border-t border-white/[0.06] pt-4">
            <Label>Tools</Label>
            <p className="text-[11px] text-white/44">
              Allowlisted tools the agent may call. Only these are ever executed.
            </p>
            <div className="space-y-2">
              {AI_AGENT_TOOLS.map((tool) => {
                const on = form.tools.includes(tool.name);
                return (
                  <label
                    key={tool.name}
                    htmlFor={`tool-${tool.name}`}
                    className="flex cursor-pointer items-start gap-2 rounded-md border border-white/[0.08] p-2.5 text-sm"
                  >
                    <input
                      id={`tool-${tool.name}`}
                      type="checkbox"
                      checked={on}
                      onChange={(e) => toggleTool(tool.name, e.target.checked)}
                      className="mt-0.5 size-4 rounded border-input accent-primary"
                    />
                    <span className="min-w-0">
                      <span className="block font-medium text-white/90">{tool.label}</span>
                      <span className="block text-[11px] text-white/44">
                        {tool.help}
                      </span>
                    </span>
                  </label>
                );
              })}
            </div>
          </div>
        </div>

        {/* Test console pane */}
        <TestConsole agentId={agent.id} />
      </div>

      <DeleteAgentDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        name={baseline.name}
        pending={deleteMutation.isPending}
        onConfirm={() => deleteMutation.mutate()}
      />
    </div>
  );
}

/* ============================================================== test console */

/**
 * Runs the agent's *saved* configuration against test input. Because it calls
 * the persisted agent, unsaved edits in the left pane are not reflected until
 * you Save — the console mirrors exactly what a workflow node would run.
 */
function TestConsole({ agentId }: { agentId: string }) {
  const [input, setInput] = React.useState("");
  const [result, setResult] = React.useState<AgentRunResult | null>(null);

  const runMutation = useMutation({
    mutationFn: () => runAiAgent(agentId, input.trim() || undefined),
    onSuccess: (res) => setResult(res),
    onError: (err) => toast.error(getErrorMessage(err, "The agent run failed.")),
  });

  return (
    <div className="flex flex-col gap-4 rounded-xl border bg-[#0a0a0a] border-white/[0.08] p-5">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold text-white/90">Test console</h2>
        <Button
          size="sm"
          onClick={() => runMutation.mutate()}
          disabled={runMutation.isPending}
        >
          {runMutation.isPending ? (
            <Loader2 className="size-4 animate-spin" />
          ) : (
            <Play className="size-4" />
          )}
          Run
        </Button>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="test-input">Input</Label>
        <Textarea
          id="test-input"
          rows={4}
          value={input}
          placeholder="Message to send to the agent…"
          onChange={(e) => setInput(e.target.value)}
        />
        <p className="text-[11px] text-white/44">
          Runs the last saved version of this agent.
        </p>
      </div>

      <div className="min-h-0 flex-1 space-y-3">
        <Label>Output</Label>
        {runMutation.isPending ? (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="size-4 animate-spin" /> Running…
          </div>
        ) : result === null ? (
          <p className="rounded-md border border-dashed border-white/[0.10] p-4 text-sm text-white/44">
            Run the agent to see its output here.
          </p>
        ) : !result.configured ? (
          <div className="rounded-md border border-amber-500/40 bg-amber-500/10 p-4 text-sm">
            <p className="font-medium text-amber-600 dark:text-amber-400">
              AI is not configured
            </p>
            <p className="mt-1 text-muted-foreground">
              The AI service has no provider key, so no completion was produced.
              Set <code className="font-mono">GEMINI_API_KEY</code> on the AI
              service to enable real runs. Nothing is fabricated.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {result.model && (
              <Badge variant="outline" className="font-mono text-[10px]">
                {result.model}
              </Badge>
            )}
            <pre className="whitespace-pre-wrap rounded-md border border-white/[0.08] bg-[#0a0a0a] p-3 text-sm font-mono">
              {result.output && result.output.length > 0
                ? result.output
                : "(empty completion)"}
            </pre>
            {result.toolCalls.length > 0 && (
              <div className="space-y-2">
                <Label className="text-xs">Tool calls</Label>
                <ul className="space-y-2">
                  {result.toolCalls.map((call, i) => (
                    <li
                      key={i}
                      className="rounded-md border border-white/[0.08] p-2.5 text-xs"
                    >
                      <p className="font-mono font-medium text-white/90">{call.name}</p>
                      <p className="mt-1 text-white/44">
                        args: {JSON.stringify(call.arguments)}
                      </p>
                      <p className="mt-0.5 text-white/44">
                        → {call.result}
                      </p>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

/* ============================================================= delete confirm */

function DeleteAgentDialog({
  open,
  onOpenChange,
  name,
  pending,
  onConfirm,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  name: string;
  pending: boolean;
  onConfirm: () => void;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Delete agent</DialogTitle>
          <DialogDescription>
            Delete <span className="font-medium text-foreground">{name}</span>?
            This cannot be undone. Workflow nodes that reference it will fall
            back to their inline instructions.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button
            type="button"
            variant="ghost"
            onClick={() => onOpenChange(false)}
          >
            Cancel
          </Button>
          <Button
            type="button"
            variant="destructive"
            onClick={onConfirm}
            disabled={pending}
          >
            {pending && <Loader2 className="size-4 animate-spin" />}
            Delete
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
