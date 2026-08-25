"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Check, ChevronsUpDown, Loader2, Plus } from "lucide-react";
import { toast } from "sonner";

import { getErrorMessage, switchOrganization } from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import { cn, initialsFrom } from "@/lib/utils";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

/**
 * The organization switcher. Switching calls the server
 * (`POST /api/organizations/{id}/switch`), which mints a new access token with
 * the new `orgId`; the store is updated from that response only — the client
 * never invents tenant context (contract §5.10).
 */
export function OrgSwitcher({ collapsed }: { collapsed?: boolean }) {
  const router = useRouter();
  const current = useAuthStore((s) => s.currentOrganization);
  const memberships = useAuthStore((s) => s.memberships);
  const [switching, setSwitching] = useState<string | null>(null);

  if (!current) return null;

  async function select(organizationId: string) {
    if (organizationId === current!.id || switching) return;
    setSwitching(organizationId);
    try {
      await switchOrganization(organizationId);
      router.refresh();
    } catch (err) {
      toast.error(getErrorMessage(err, "Could not switch organization."));
    } finally {
      setSwitching(null);
    }
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        className={cn(
          "flex w-full items-center gap-2 rounded-md border border-border bg-card px-2 py-2 text-left text-sm transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
          collapsed && "justify-center px-0",
        )}
      >
        <span className="flex size-6 shrink-0 items-center justify-center rounded bg-primary/15 text-xs font-semibold text-primary">
          {initialsFrom(current.name)}
        </span>
        {!collapsed && (
          <>
            <span className="min-w-0 flex-1 truncate font-medium">
              {current.name}
            </span>
            <ChevronsUpDown className="size-4 shrink-0 text-muted-foreground" />
          </>
        )}
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="w-64">
        <DropdownMenuLabel>Organizations</DropdownMenuLabel>
        <DropdownMenuSeparator />
        {memberships.map((m) => (
          <DropdownMenuItem
            key={m.organizationId}
            onSelect={(e) => {
              e.preventDefault();
              void select(m.organizationId);
            }}
            className="gap-2"
          >
            <span className="flex size-6 shrink-0 items-center justify-center rounded bg-primary/15 text-xs font-semibold text-primary">
              {initialsFrom(m.organizationName)}
            </span>
            <span className="min-w-0 flex-1 truncate">{m.organizationName}</span>
            <span className="text-xs text-muted-foreground">{m.role}</span>
            {switching === m.organizationId ? (
              <Loader2 className="size-4 animate-spin" />
            ) : (
              m.organizationId === current.id && (
                <Check className="size-4 text-primary" />
              )
            )}
          </DropdownMenuItem>
        ))}
        <DropdownMenuSeparator />
        <DropdownMenuItem disabled className="gap-2 text-muted-foreground">
          <Plus className="size-4" />
          New organization
          <span className="ml-auto text-xs">Soon</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
