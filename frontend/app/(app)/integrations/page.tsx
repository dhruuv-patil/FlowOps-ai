"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Loader2,
  Mail,
  MessageSquare,
  Plug,
  Slack,
  Webhook,
  Zap,
  TestTube,
  Zap as ZapIcon,
} from "lucide-react";
import { toast } from "sonner";

import {
  connectIntegration,
  connectProvider,
  disconnectIntegration,
  fetchIntegrations,
  fetchProviders,
  getErrorMessage,
  sendTestIntegration,
  testIntegration,
  syncIntegration,
} from "@/lib/api";
import { hasRole, useAuthStore } from "@/lib/auth-store";
import type { Integration, ProviderInfo } from "@/types";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Card,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Separator } from "@/components/ui/separator";

import { ConnectIntegrationDialog } from "@/components/app/connect-integration-dialog";
import { WorkflowDiscovery } from "@/components/integrations/workflow-discovery";

interface ProviderCardProps {
  provider: ProviderInfo;
  connected: Integration | null;
  isAdmin: boolean;
  onConnect: () => void;
  onDisconnect: (integration: Integration) => void;
}

function ProviderCard({
  provider,
  connected,
  isAdmin,
  onConnect,
  onDisconnect,
}: ProviderCardProps) {
  const [workflowsOpen, setWorkflowsOpen] = useState(false);
  const [testing, setTesting] = useState(false);
  const [sendingTest, setSendingTest] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const queryClient = useQueryClient();

  // Detect if this is a delivery channel (notification provider) or workflow provider
  const isDeliveryChannel = ["slack", "email", "discord", "teams", "webhook"].includes(provider.type);
  const isWorkflowProvider = !isDeliveryChannel;

  const testMutation = useMutation({
    mutationFn: () => testIntegration(connected!.id),
    onSuccess: (result) => {
      toast[result.success ? "success" : "error"](result.message);
    },
    onError: (err) => {
      toast.error(getErrorMessage(err, "Test failed."));
    },
    onSettled: () => setTesting(false),
  });

  const sendTestMutation = useMutation({
    mutationFn: () => sendTestIntegration(connected!.id),
    onSuccess: (result) => {
      toast[result.success ? "success" : "error"](result.message ?? (result.success ? "Test sent!" : "Test failed"));
    },
    onError: (err) => {
      toast.error(getErrorMessage(err, "Test send failed."));
    },
    onSettled: () => setSendingTest(false),
  });

  const syncMutation = useMutation({
    mutationFn: () => syncIntegration(connected!.id),
    onSuccess: (result) => {
      toast.success(`Synced: ${result.workflows} workflows, ${result.executions} executions, ${result.events} events`);
    },
    onError: (err) => {
      toast.error(getErrorMessage(err, "Sync failed."));
    },
    onSettled: () => {
      setSyncing(false);
      queryClient.invalidateQueries({ queryKey: ["integrations"] });
      queryClient.invalidateQueries({ queryKey: ["integrations", connected!.id, "sync"] });
    },
  });

  const adminTitle = isAdmin ? undefined : "Requires an admin";

  // Determine the icon for the provider
  let Icon: React.ComponentType<{ className?: string }> = Plug;
  if (provider.type === "slack") Icon = Slack;
  else if (provider.type === "email") Icon = Mail;
  else if (provider.type === "discord") Icon = MessageSquare;
  else if (provider.type === "webhook") Icon = Webhook;
  else if (provider.type === "teams") Icon = MessageSquare;
  else if (["n8n", "make", "zapier", "temporal", "custom", "github"].includes(provider.type)) Icon = Zap;

  const renderStatus = () => {
    if (connected) {
      return <Badge variant="success">Connected</Badge>;
    }
    return <Badge variant="outline">Not connected</Badge>;
  };

  const renderHint = () => {
    if (!connected?.hint) return null;
    const label = provider.type === "slack" ? "Webhook" : "Credential";
    return (
      <p className="pt-1 font-mono text-xs text-white/44">
        {label} ••••{connected.hint}
      </p>
    );
  };

  return (
    <>
      <Card className="flex flex-col border-white/[0.08] !bg-[#0a0a0a]">
        <CardHeader className="flex-1">
          <div className="flex items-start justify-between gap-3">
            <span className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Icon className="size-5" />
            </span>
            {renderStatus()}
          </div>

          <CardTitle className="mt-3 text-white/90">
            {provider.name}
          </CardTitle>

          <CardDescription className="text-white/44">
            {provider.description}
          </CardDescription>

          {renderHint()}

          {connected && isWorkflowProvider && provider.capabilities?.workflowDiscovery && (
            <p className="pt-2 text-xs text-white/44">
              {provider.capabilities.executionHistory ? "Executions sync enabled" : "Discovery only"}
            </p>
          )}
        </CardHeader>

        <CardFooter>
          {!connected ? (
            <Button
              className="w-full"
              disabled={!isAdmin}
              title={adminTitle}
              onClick={onConnect}
            >
              <Plug className="size-4" />
              Connect
            </Button>
          ) : isDeliveryChannel ? (
            <div className="flex w-full gap-2">
              <Button
                variant="outline"
                className="flex-1"
                disabled={!isAdmin || testing}
                title={adminTitle}
                onClick={() => {
                  setTesting(true);
                  testMutation.mutate();
                }}
              >
                {testing && <Loader2 className="size-4 animate-spin mr-1" />}
                Test
              </Button>
              <Button
                variant="outline"
                className="flex-1"
                disabled={!isAdmin || sendingTest}
                title={adminTitle}
                onClick={() => {
                  setSendingTest(true);
                  sendTestMutation.mutate();
                }}
              >
                {sendingTest && <Loader2 className="size-4 animate-spin mr-1" />}
                Send test
              </Button>
              <Button
                variant="outline"
                className="flex-1 text-destructive hover:text-destructive"
                disabled={!isAdmin}
                title={adminTitle}
                onClick={() => onDisconnect(connected)}
              >
                Disconnect
              </Button>
            </div>
          ) : (
            <div className="flex w-full gap-2">
              {provider.capabilities?.workflowDiscovery && (
                <Button
                  variant="outline"
                  className="flex-1"
                  onClick={() => setWorkflowsOpen(true)}
                >
                  Workflows
                </Button>
              )}
              {provider.capabilities?.executionHistory && (
                <Button
                  variant="outline"
                  className="flex-1"
                  disabled={syncing}
                  onClick={() => {
                    setSyncing(true);
                    syncMutation.mutate();
                  }}
                >
                  {syncing && <Loader2 className="size-4 animate-spin mr-1" />}
                  Sync
                </Button>
              )}
              <Button
                variant="outline"
                className="flex-1 text-destructive hover:text-destructive"
                disabled={!isAdmin}
                title={adminTitle}
                onClick={() => onDisconnect(connected)}
              >
                Disconnect
              </Button>
            </div>
          )}
        </CardFooter>
      </Card>

      {/* n8n workflow discovery + monitoring */}
      {provider.type === "n8n" && connected && workflowsOpen && (
        <WorkflowDiscovery
          integrationId={connected.id}
          integrationName={connected.name}
          onClose={() => setWorkflowsOpen(false)}
        />
      )}
    </>
  );
}

function DisconnectDialog({
  integration,
  onOpenChange,
}: {
  integration: Integration | null;
  onOpenChange: (open: boolean) => void;
}) {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => disconnectIntegration(integration!.id),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["integrations"],
      });

      toast.success("Disconnected.");
      onOpenChange(false);
    },

    onError: (err) => {
      toast.error(
        getErrorMessage(err, "Could not disconnect."),
      );
    },
  });

  return (
    <Dialog open={!!integration} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            Disconnect {integration?.name}
          </DialogTitle>

          <DialogDescription>
            This deletes the stored credential. Notification nodes on this
            channel will fall back to an honest &ldquo;not delivered&rdquo;
            until you reconnect. This cannot be undone.
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
            onClick={() => mutation.mutate()}
            disabled={mutation.isPending}
          >
            {mutation.isPending && (
              <Loader2 className="size-4 animate-spin" />
            )}
            Disconnect
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default function IntegrationsPage() {
  const role = useAuthStore((s) => s.currentRole);
  const isAdmin = hasRole(role, "ADMIN");

  const [connectOpen, setConnectOpen] = useState(false);
  const [connectProviderType, setConnectProviderType] =
    useState<string | null>(null);

  const [disconnectTarget, setDisconnectTarget] =
    useState<Integration | null>(null);

  // Fetch connected integrations
  const integrationsQuery = useQuery({
    queryKey: ["integrations"],
    queryFn: fetchIntegrations,
  });

  // Fetch available providers from the catalog API
  const providersQuery = useQuery({
    queryKey: ["integration-providers"],
    queryFn: fetchProviders,
  });

  const connectedByType = new Map<string, Integration>(
    (integrationsQuery.data?.integrations ?? [])
      .filter((integration) => integration.status === "connected")
      .map((integration) => [integration.type, integration] as const),
  );

  function openConnect(providerType: string) {
    setConnectProviderType(providerType);
    setConnectOpen(true);
  }

  function closeConnect(open: boolean) {
    setConnectOpen(open);

    if (!open) {
      setConnectProviderType(null);
    }
  }

  // Render the provider catalog grouped by type
  function renderProvidersSection(title: string, providerTypes: string[]) {
    if (!providersQuery.data) return null;

    const sectionProviders = providersQuery.data.providers.filter((p) =>
      providerTypes.includes(p.type),
    );

    if (sectionProviders.length === 0) return null;

    return (
      <div className="space-y-4">
        <h2 className="text-sm font-medium uppercase tracking-wide text-white/60">
          {title}
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {sectionProviders.map((provider) => (
            <ProviderCard
              key={provider.type}
              provider={provider}
              connected={connectedByType.get(provider.type) ?? null}
              isAdmin={isAdmin}
              onConnect={() => openConnect(provider.type)}
              onDisconnect={setDisconnectTarget}
            />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <p className="mono-eyebrow">Automation</p>

        <h1 className="mt-1 text-xl font-semibold tracking-tight text-white/90">
          Integrations
        </h1>

        <p className="mt-1 text-sm text-white/44">
          Connect the services your workflows deliver to. Credentials are
          encrypted at rest and never shown again.
        </p>
      </div>

      {integrationsQuery.isError || providersQuery.isError ? (
        <p className="text-sm text-destructive">
          Could not load integrations. Please try again.
        </p>
      ) : integrationsQuery.isPending || providersQuery.isPending ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Skeleton className="h-44 w-full !bg-[#0a0a0a]" />
          <Skeleton className="h-44 w-full !bg-[#0a0a0a]" />
          <Skeleton className="h-44 w-full !bg-[#0a0a0a]" />
        </div>
      ) : (
        <>
          {/* Notifications (delivery channels) */}
          {renderProvidersSection("Notifications", [
            "slack",
            "email",
            "discord",
            "teams",
            "webhook",
          ])}

          {/* Workflow Platforms */}
          <div className="space-y-4 border-t border-white/5 pt-4">
            {renderProvidersSection("Workflow platforms", [
              "n8n",
              "make",
              "zapier",
              "github",
              "temporal",
              "custom",
            ])}
          </div>
        </>
      )}

      <ConnectIntegrationDialog
        open={connectOpen}
        onOpenChange={closeConnect}
        providerType={connectProviderType}
      />

      <DisconnectDialog
        integration={disconnectTarget}
        onOpenChange={(open) => {
          if (!open) {
            setDisconnectTarget(null);
          }
        }}
      />
    </div>
  );
}