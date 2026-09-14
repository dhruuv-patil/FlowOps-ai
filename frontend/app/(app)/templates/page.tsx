"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, PanelsTopLeft, Search } from "lucide-react";
import { toast } from "sonner";

import { fetchTemplates, getErrorMessage, useTemplate } from "@/lib/api";
import { resolveIcon } from "@/components/app/builder/shared";
import type { WorkflowTemplateSummary } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";

/**
 * The template gallery. Every card is a real graph made of registry node types;
 * "Use template" creates a genuine DRAFT workflow in the current org and drops
 * the user into the builder. Nothing is ever executed from here.
 */
export default function TemplatesPage() {
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ["templates"],
    queryFn: fetchTemplates,
  });

  const all = query.data?.templates ?? [];

  const categories = useMemo(() => {
    const set = new Set(all.map((t) => t.category));
    return Array.from(set).sort();
  }, [all]);

  const templates = useMemo(() => {
    const term = search.trim().toLowerCase();
    return all.filter((t) => {
      if (category && t.category !== category) return false;
      if (!term) return true;
      return (
        t.name.toLowerCase().includes(term) ||
        t.description.toLowerCase().includes(term) ||
        t.tags.some((tag) => tag.toLowerCase().includes(term))
      );
    });
  }, [all, search, category]);

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <p className="mono-eyebrow">Workspace</p>
        <h1 className="mt-1 text-xl font-semibold tracking-tight text-white/90">Templates</h1>
        <p className="mt-1 text-sm text-white/44">
          Start from a ready-made workflow. Using one creates an editable draft in
          your workspace — you review and publish it yourself.
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative sm:max-w-xs sm:flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search templates…"
            className="pl-9"
          />
        </div>
        {categories.length > 0 && (
          <div className="flex flex-wrap gap-1">
            <CategoryPill
              label="All"
              active={category === null}
              onClick={() => setCategory(null)}
            />
            {categories.map((c) => (
              <CategoryPill
                key={c}
                label={c}
                active={category === c}
                onClick={() => setCategory(c)}
              />
            ))}
          </div>
        )}
      </div>

      {query.isPending ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Skeleton className="h-44 w-full" />
          <Skeleton className="h-44 w-full" />
          <Skeleton className="h-44 w-full" />
        </div>
      ) : query.isError ? (
        <p className="text-sm text-destructive">
          Could not load templates. Please try again.
        </p>
      ) : templates.length === 0 ? (
        <div className="rounded-xl border border-dashed border-white/[0.10] py-16 text-center">
          <PanelsTopLeft className="size-10 text-white/20 mx-auto mb-3" />
          <div>
            <p className="font-medium text-white/90">No templates match your search.</p>
            <p className="text-sm text-white/44 mt-1">
              Try a different keyword or category.
            </p>
          </div>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {templates.map((t) => (
            <TemplateCard key={t.slug} template={t} />
          ))}
        </div>
      )}
    </div>
  );
}

function CategoryPill({
  label,
  active,
  onClick,
}: {
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={
        "rounded-full border px-3 py-1 text-sm transition-colors " +
        (active
          ? "bg-white text-[#050505] border-white font-medium"
          : "border-white/[0.08] bg-white/[0.03] text-white/60 hover:bg-white/[0.06] hover:text-white/90")
      }
    >
      {label}
    </button>
  );
}

function TemplateCard({ template }: { template: WorkflowTemplateSummary }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const Icon = resolveIcon(template.icon);

  const mutation = useMutation({
    mutationFn: () => useTemplate(template.slug),
    onSuccess: (workflow) => {
      queryClient.invalidateQueries({ queryKey: ["workflows"] });
      toast.success(`Created “${workflow.name}” from a template.`);
      router.push(`/workflows/${workflow.id}`);
    },
    onError: (err) =>
      toast.error(getErrorMessage(err, "Could not create from this template.")),
  });

  return (
    <div className="flex flex-col rounded-xl border border-white/[0.08] bg-[#0a0a0a] p-4">
      <div className="flex items-start gap-3">
        <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
          <Icon className="size-4" />
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate font-medium text-white/90">{template.name}</p>
          <Badge variant="outline" className="mt-1 text-[10px] border-white/[0.08] text-white/44">
            {template.category}
          </Badge>
        </div>
      </div>

      <p className="mt-3 line-clamp-3 flex-1 text-sm text-white/44">
        {template.description}
      </p>

      <div className="mt-3 flex items-center justify-between gap-2">
        <span className="text-xs text-white/44">
          {template.nodeCount} nodes
        </span>
        <Button
          size="sm"
          onClick={() => mutation.mutate()}
          disabled={mutation.isPending}
        >
          {mutation.isPending && <Loader2 className="size-4 animate-spin" />}
          Use template
        </Button>
      </div>
    </div>
  );
}
