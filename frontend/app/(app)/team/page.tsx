"use client";

import { Suspense, useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "next/navigation";
import { Loader2, UserPlus, X } from "lucide-react";
import { toast } from "sonner";

import {
  acceptInvitation,
  changeMemberRole,
  fetchCurrentOrganizationMembers,
  fetchInvitations,
  getErrorMessage,
  removeMember,
  revokeInvitation,
} from "@/lib/api";
import { hasRole, useAuthStore } from "@/lib/auth-store";
import { initialsFrom } from "@/lib/utils";
import type { Invitation, OrganizationMember, Role } from "@/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { InviteMemberDialog } from "@/components/app/invite-member-dialog";

/** Roles assignable via the member row dropdown. OWNER is granted only by transfer, never here. */
const ASSIGNABLE_ROLES: Role[] = ["ADMIN", "MEMBER", "VIEWER"];

export default function TeamPage() {
  const role = useAuthStore((s) => s.currentRole);
  const isAdmin = hasRole(role, "ADMIN");

  const [inviteOpen, setInviteOpen] = useState(false);

  return (
    <div className="mx-auto max-w-4xl space-y-8">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="mono-eyebrow">Organization</p>
          <h1 className="mt-1 text-xl font-semibold tracking-tight text-white/90">Team</h1>
          <p className="mt-1 text-sm text-white/44">
            Manage who belongs to this organization and what they can do.
          </p>
        </div>
        {isAdmin && (
          <Button onClick={() => setInviteOpen(true)}>
            <UserPlus className="size-4" /> Invite member
          </Button>
        )}
      </div>

      {/* `useSearchParams` needs a Suspense boundary during prerender. */}
      <Suspense fallback={null}>
        <AcceptInvitationHandler />
      </Suspense>
      <MemberList isAdmin={isAdmin} />
      {isAdmin && (
        <>
          <Separator />
          <PendingInvitations />
        </>
      )}

      <InviteMemberDialog open={inviteOpen} onOpenChange={setInviteOpen} />
    </div>
  );
}

/* ------------------------------------------------------------------ members */

function MemberList({ isAdmin }: { isAdmin: boolean }) {
  const currentUserId = useAuthStore((s) => s.user?.id ?? null);
  const query = useQuery({
    queryKey: ["members"],
    queryFn: fetchCurrentOrganizationMembers,
  });

  if (query.isError) {
    return (
      <p className="text-sm text-destructive">
        Could not load members. Please try again.
      </p>
    );
  }
  if (query.isPending) {
    return (
      <div className="space-y-2">
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-16 w-full" />
      </div>
    );
  }

  const members = query.data.members;

  return (
    <section className="space-y-3">
      <h2 className="text-sm font-medium text-white/44">
        {members.length} member{members.length === 1 ? "" : "s"}
      </h2>
      <ul className="divide-y divide-white/[0.06] rounded-xl border border-white/[0.08]">
        {members.map((member) => (
          <MemberRow
            key={member.userId}
            member={member}
            isAdmin={isAdmin}
            isSelf={member.userId === currentUserId}
          />
        ))}
      </ul>
    </section>
  );
}

function MemberRow({
  member,
  isAdmin,
  isSelf,
}: {
  member: OrganizationMember;
  isAdmin: boolean;
  isSelf: boolean;
}) {
  const queryClient = useQueryClient();
  const [removeOpen, setRemoveOpen] = useState(false);

  const roleMutation = useMutation({
    mutationFn: (role: Role) => changeMemberRole(member.userId, role),
    onSuccess: (data) => {
      queryClient.setQueryData(["members"], data);
      toast.success("Role updated.");
    },
    onError: (err) => toast.error(getErrorMessage(err, "Could not change the role.")),
  });

  // OWNER rows and your own row are never editable here (backend enforces both:
  // an admin cannot touch an owner, and no one can change their own role).
  const canManage = isAdmin && !isSelf && member.role !== "OWNER";

  return (
    <li className="flex items-center gap-3 px-4 py-3">
      <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary/15 text-xs font-semibold text-primary">
        {initialsFrom(member.fullName)}
      </span>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium">
          {member.fullName}
          {isSelf && (
            <span className="ml-2 text-xs font-normal text-muted-foreground">
              (you)
            </span>
          )}
        </p>
        <p className="truncate text-xs text-muted-foreground">{member.email}</p>
      </div>

      {canManage ? (
        <select
          aria-label={`Role for ${member.fullName}`}
          value={member.role}
          disabled={roleMutation.isPending}
          onChange={(e) => roleMutation.mutate(e.target.value as Role)}
          className="h-9 rounded-md border border-input bg-transparent px-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:opacity-50"
        >
          {ASSIGNABLE_ROLES.map((r) => (
            <option key={r} value={r}>
              {r}
            </option>
          ))}
        </select>
      ) : (
        <Badge variant={member.role === "OWNER" ? "default" : "secondary"}>
          {member.role}
        </Badge>
      )}

      {canManage && (
        <Button
          variant="ghost"
          size="icon"
          aria-label={`Remove ${member.fullName}`}
          onClick={() => setRemoveOpen(true)}
          className="text-muted-foreground hover:text-destructive"
        >
          <X className="size-4" />
        </Button>
      )}

      <RemoveMemberDialog
        member={member}
        open={removeOpen}
        onOpenChange={setRemoveOpen}
      />
    </li>
  );
}

function RemoveMemberDialog({
  member,
  open,
  onOpenChange,
}: {
  member: OrganizationMember;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => removeMember(member.userId),
    onSuccess: (data) => {
      queryClient.setQueryData(["members"], data);
      toast.success(`Removed ${member.fullName}.`);
      onOpenChange(false);
    },
    onError: (err) => toast.error(getErrorMessage(err, "Could not remove the member.")),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Remove {member.fullName}?</DialogTitle>
          <DialogDescription>
            They lose access to this organization immediately. Their workflows and
            past executions are preserved. This cannot be undone.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button
            type="button"
            variant="destructive"
            onClick={() => mutation.mutate()}
            disabled={mutation.isPending}
          >
            {mutation.isPending && <Loader2 className="size-4 animate-spin" />}
            Remove
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/* -------------------------------------------------------------- invitations */

function PendingInvitations() {
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ["invitations"], queryFn: fetchInvitations });

  const revokeMutation = useMutation({
    mutationFn: (id: string) => revokeInvitation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["invitations"] });
      toast.success("Invitation revoked.");
    },
    onError: (err) => toast.error(getErrorMessage(err, "Could not revoke.")),
  });

  if (query.isError) {
    return (
      <p className="text-sm text-destructive">Could not load invitations.</p>
    );
  }
  if (query.isPending) {
    return <Skeleton className="h-24 w-full" />;
  }

  const pending = query.data.invitations.filter((i) => i.status === "PENDING");

  return (
    <section className="space-y-3">
      <h2 className="text-sm font-medium text-white/44">
        Pending invitations
      </h2>
      {pending.length === 0 ? (
        <div className="rounded-xl border border-dashed border-white/[0.10] py-16 text-center">
          <p className="text-sm text-white/44">
            No pending invitations. Invite a teammate to get started.
          </p>
        </div>
      ) : (
        <ul className="divide-y divide-white/[0.06] rounded-xl border border-white/[0.08]">
          {pending.map((invitation) => (
            <InvitationRow
              key={invitation.id}
              invitation={invitation}
              onRevoke={() => revokeMutation.mutate(invitation.id)}
              revoking={revokeMutation.isPending}
            />
          ))}
        </ul>
      )}
    </section>
  );
}

function InvitationRow({
  invitation,
  onRevoke,
  revoking,
}: {
  invitation: Invitation;
  onRevoke: () => void;
  revoking: boolean;
}) {
  const expired = new Date(invitation.expiresAt).getTime() < Date.now();
  return (
    <li className="flex items-center gap-3 px-4 py-3">
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium">{invitation.email}</p>
        <p className="truncate text-xs text-muted-foreground">
          {invitation.role} ·{" "}
          {expired
            ? "Expired"
            : `Expires ${new Date(invitation.expiresAt).toLocaleDateString()}`}
        </p>
      </div>
      <Badge variant={expired ? "warning" : "outline"}>
        {expired ? "Expired" : "Pending"}
      </Badge>
      <Button
        variant="ghost"
        size="sm"
        onClick={onRevoke}
        disabled={revoking}
        className="text-muted-foreground hover:text-destructive"
      >
        Revoke
      </Button>
    </li>
  );
}

/* ------------------------------------------------- accept an invitation link */

/**
 * Redeems a `?invite=<token>` link for the signed-in user. The org and role come
 * only from the stored invitation; a mismatched email fails opaquely. On success
 * we prompt the user to switch into the org via the switcher (the backend does
 * not switch them automatically).
 */
function AcceptInvitationHandler() {
  const searchParams = useSearchParams();
  const token = searchParams.get("invite");
  const [dismissed, setDismissed] = useState(false);

  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => acceptInvitation(token!),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["organizations"] });
      toast.success(
        `Joined ${data.organization.name}. Switch to it from the org switcher.`,
      );
      setDismissed(true);
      // Drop the token from the URL so a refresh doesn't re-attempt it.
      window.history.replaceState(null, "", window.location.pathname);
    },
    onError: (err) => {
      toast.error(getErrorMessage(err, "This invitation link is not valid."));
      setDismissed(true);
      window.history.replaceState(null, "", window.location.pathname);
    },
  });

  // Fire exactly once when a token is present.
  useEffect(() => {
    if (token && !dismissed && mutation.isIdle) {
      mutation.mutate();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  if (!token || dismissed) return null;

  return (
    <div className="flex items-center gap-2 rounded-lg border border-white/[0.08] bg-white/[0.03] px-4 py-3 text-sm text-white/44">
      <Loader2 className="size-4 animate-spin" />
      Redeeming your invitation…
    </div>
  );
}
