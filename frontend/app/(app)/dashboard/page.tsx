"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, Users, Workflow as WorkflowIcon } from "lucide-react";

import {
  fetchCurrentOrganization,
  fetchCurrentOrganizationMembers,
} from "@/lib/api";
import { useAuthStore } from "@/lib/auth-store";
import { initialsFrom } from "@/lib/utils";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);

  const orgQuery = useQuery({
    queryKey: ["organization", "current"],
    queryFn: fetchCurrentOrganization,
  });
  const membersQuery = useQuery({
    queryKey: ["organization", "current", "members"],
    queryFn: fetchCurrentOrganizationMembers,
  });

  const firstName = user?.fullName?.split(" ")[0] ?? "there";

  return (
    <div className="mx-auto max-w-5xl space-y-8">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">
          Welcome back, {firstName}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Here&apos;s the state of your workspace.
        </p>
      </div>

      {/* Real workspace data — no fabricated execution metrics. Those KPIs
          arrive in M3 once the execution engine is live. */}
      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardHeader className="pb-2">
            <CardDescription className="flex items-center gap-2">
              <WorkflowIcon className="size-4" /> Organization
            </CardDescription>
          </CardHeader>
          <CardContent>
            {orgQuery.isPending ? (
              <Skeleton className="h-7 w-40" />
            ) : orgQuery.isError ? (
              <p className="text-sm text-destructive">
                Could not load organization.
              </p>
            ) : (
              <>
                <p className="text-xl font-semibold">
                  {orgQuery.data.organization.name}
                </p>
                <p className="text-xs text-muted-foreground">
                  Your role: {orgQuery.data.role}
                </p>
              </>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription className="flex items-center gap-2">
              <Users className="size-4" /> Members
            </CardDescription>
          </CardHeader>
          <CardContent>
            {orgQuery.isPending ? (
              <Skeleton className="h-7 w-16" />
            ) : orgQuery.isError ? (
              <p className="text-sm text-destructive">—</p>
            ) : (
              <p className="text-xl font-semibold">
                {orgQuery.data.memberCount}
              </p>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Team</CardTitle>
          <CardDescription>People in this organization.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-2">
          {membersQuery.isPending ? (
            <>
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </>
          ) : membersQuery.isError ? (
            <p className="text-sm text-destructive">
              Could not load the member list.
            </p>
          ) : (
            membersQuery.data.members.map((m) => (
              <div
                key={m.userId}
                className="flex items-center gap-3 rounded-md border border-border/60 px-3 py-2"
              >
                <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/15 text-xs font-semibold text-primary">
                  {initialsFrom(m.fullName)}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-medium">
                    {m.fullName}
                  </span>
                  <span className="block truncate text-xs text-muted-foreground">
                    {m.email}
                  </span>
                </span>
                <span className="text-xs text-muted-foreground">{m.role}</span>
              </div>
            ))
          )}
        </CardContent>
      </Card>

      {/* Workflows land in M2. Honest empty state rather than a fake count. */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Workflows</CardTitle>
          <CardDescription>
            Build and automate your first workflow.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col items-center gap-2 rounded-md border border-dashed border-border/60 py-10 text-center">
            <WorkflowIcon className="size-8 text-muted-foreground/50" />
            <p className="text-sm text-muted-foreground">
              No workflows yet. The visual builder ships in the next milestone.
            </p>
            <Link
              href="/workflows"
              aria-disabled="true"
              className="pointer-events-none inline-flex items-center gap-1 text-sm font-medium text-muted-foreground/50"
            >
              Open builder <ArrowRight className="size-4" />
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
