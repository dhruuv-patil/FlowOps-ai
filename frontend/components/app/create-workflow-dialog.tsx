"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { createWorkflow, getErrorMessage, getFieldErrors } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

/**
 * Create-workflow dialog. On success it navigates straight into the builder for
 * the new (empty, DRAFT) workflow — the natural next action.
 */
export function CreateWorkflowDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const router = useRouter();
  const queryClient = useQueryClient();

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});

  const mutation = useMutation({
    mutationFn: () =>
      createWorkflow({
        name: name.trim(),
        description: description.trim() || undefined,
      }),
    onSuccess: (workflow) => {
      queryClient.invalidateQueries({ queryKey: ["workflows"] });
      onOpenChange(false);
      router.push(`/workflows/${workflow.id}`);
    },
    onError: (err) => {
      const fieldErrors = getFieldErrors(err);
      if (Object.keys(fieldErrors).length > 0) setErrors(fieldErrors);
      else toast.error(getErrorMessage(err, "Could not create the workflow."));
    },
  });

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErrors({});
    if (!name.trim()) {
      setErrors({ name: "Give your workflow a name." });
      return;
    }
    mutation.mutate();
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <form onSubmit={onSubmit}>
          <DialogHeader>
            <DialogTitle>New workflow</DialogTitle>
            <DialogDescription>
              Start with an empty canvas. You can rename it any time.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="wf-name">Name</Label>
              <Input
                id="wf-name"
                autoFocus
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Onboarding automation"
                aria-invalid={!!errors.name}
              />
              {errors.name && (
                <p className="text-xs text-destructive">{errors.name}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="wf-desc">Description (optional)</Label>
              <Textarea
                id="wf-desc"
                rows={3}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What does this workflow do?"
                aria-invalid={!!errors.description}
              />
              {errors.description && (
                <p className="text-xs text-destructive">{errors.description}</p>
              )}
            </div>
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="ghost"
              onClick={() => onOpenChange(false)}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending && <Loader2 className="animate-spin" />}
              Create
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
