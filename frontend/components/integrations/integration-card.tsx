"use client";

import { Loader2, Plug } from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import {
  disconnectIntegration,
  getErrorMessage,
} from "@/lib/api";
import type { Integration } from "@/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

interface Provider {
  type: string;
  name: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
  available: boolean;
}

interface IntegrationCardProps {
  provider: Provider;
  connected: Integration | null;
  isAdmin: boolean;
  onConnect: (providerType: string) => void;
  onDisconnect: (integration: Integration) => void;
}

export function IntegrationCard({
  provider,
  connected,
  isAdmin,
  onConnect,
  onDisconnect,
}: IntegrationCardProps) {
  const Icon = provider.icon;

  const adminTitle = isAdmin
    ? undefined
    : "Requires an admin";

  const connectionState = connected
    ? "connected"
    : provider.available
      ? "available"
      : "unavailable";

  return (
    <Card className="flex h-full flex-col">
      <CardHeader className="flex-1">
        <div className="flex items-start justify-between gap-3">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-lg border border-white/[0.06] bg-white/[0.035] text-white/55">
            <Icon className="size-5" />
          </span>

          {connectionState === "connected" ? (
            <Badge variant="success">Connected</Badge>
          ) : connectionState === "available" ? (
            <Badge variant="outline">Not connected</Badge>
          ) : (
            <Badge variant="secondary">Coming soon</Badge>
          )}
        </div>

        <CardTitle className="mt-3 text-white/85">
          {provider.name}
        </CardTitle>

        <CardDescription className="text-white/35">
          {provider.description}
        </CardDescription>

        {connected?.hint && (
          <p className="pt-1 font-mono text-[10px] text-white/30">
            Webhook ••••{connected.hint}
          </p>
        )}
      </CardHeader>

      <CardFooter>
        {!provider.available ? (
          <Button
            type="button"
            variant="outline"
            disabled
            className="w-full"
          >
            Coming soon
          </Button>
        ) : connected ? (
          <Button
            type="button"
            variant="outline"
            className="w-full border-white/[0.08] text-white/55 hover:border-red-500/20 hover:bg-red-500/[0.04] hover:text-red-300"
            disabled={!isAdmin}
            title={adminTitle}
            onClick={() => onDisconnect(connected)}
          >
            Disconnect
          </Button>
        ) : (
          <Button
            type="button"
            className="w-full gap-2"
            disabled={!isAdmin}
            title={adminTitle}
            onClick={() => onConnect(provider.type)}
          >
            <Plug className="size-4" />
            Connect
          </Button>
        )}
      </CardFooter>
    </Card>
  );
}

/* ========================================================= disconnect confirm */

export function DisconnectDialog({
  integration,
  onOpenChange,
}: {
  integration: Integration | null;
  onOpenChange: (open: boolean) => void;
}) {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: async () => {
      if (!integration) {
        throw new Error("No integration selected.");
      }

      return disconnectIntegration(integration.id);
    },

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["integrations"],
      });

      toast.success("Integration disconnected.");
      onOpenChange(false);
    },

    onError: (err) => {
      toast.error(
        getErrorMessage(
          err,
          "Could not disconnect the integration.",
        ),
      );
    },
  });

  return (
    <Dialog
      open={Boolean(integration)}
      onOpenChange={(open) => {
        if (!mutation.isPending) {
          onOpenChange(open);
        }
      }}
    >
      <DialogContent className="border-white/[0.08] bg-[#0a0a0a] sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-white/90">
            Disconnect {integration?.name}
          </DialogTitle>

          <DialogDescription className="text-white/40">
            This removes the stored credential for this integration.
            Notification workflows using this connection will not be
            delivered until you reconnect it. This action cannot be undone.
          </DialogDescription>
        </DialogHeader>

        <DialogFooter>
          <Button
            type="button"
            variant="ghost"
            onClick={() => onOpenChange(false)}
            disabled={mutation.isPending}
          >
            Cancel
          </Button>

          <Button
            type="button"
            variant="destructive"
            onClick={() => mutation.mutate()}
            disabled={!integration || mutation.isPending}
            className="gap-2"
          >
            {mutation.isPending && (
              <Loader2 className="size-4 animate-spin" />
            )}
            {mutation.isPending
              ? "Disconnecting..."
              : "Disconnect"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
