"use client";

import { useState, useEffect } from "react";
import { Loader2, RefreshCw, AlertCircle, CheckCircle, XCircle } from "lucide-react";
import { toast } from "sonner";

import { syncIntegration, fetchSyncStatus } from "@/lib/api";
import { getErrorMessage } from "@/lib/api";
import type { SyncStatusResponse } from "@/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";

interface IntegrationStatusProps {
  integrationId: string;
  integrationName: string;
}

export function IntegrationStatus({ integrationId, integrationName }: IntegrationStatusProps) {
  const [status, setStatus] = useState<SyncStatusResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSyncing, setIsSyncing] = useState(false);

  const loadStatus = async () => {
    try {
      const resp = await fetchSyncStatus(integrationId);
      setStatus(resp);
    } catch (err) {
      toast.error(getErrorMessage(err, "Could not load sync status."));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadStatus();
  }, [integrationId]);

  const handleSync = async () => {
    setIsSyncing(true);
    try {
      const result = await syncIntegration(integrationId);
      toast.success(
        `Sync complete: ${result.workflows} workflows, ${result.executions} executions, ${result.events} events.`,
      );
      await loadStatus();
    } catch (err) {
      toast.error(getErrorMessage(err, "Sync failed."));
    } finally {
      setIsSyncing(false);
    }
  };

  const statusConfig = {
    HEALTHY: { label: "Healthy", variant: "success" as const, icon: CheckCircle },
    FAILED: { label: "Failed", variant: "destructive" as const, icon: AlertCircle },
    NEW: { label: "Never synced", variant: "secondary" as const, icon: XCircle },
  } as const;

  const currentStatus = status?.status ?? "NEW";
  const config = statusConfig[currentStatus as keyof typeof statusConfig] ?? statusConfig.NEW;
  const StatusIcon = config.icon;

  return (
    <Card className="!bg-[#0a0a0a] border-white/[0.08]">
      <CardHeader>
        <CardTitle className="text-white/90">{integrationName} — Sync Status</CardTitle>
        <CardDescription className="text-white/44">
          Manual sync and automatic scheduler status
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <StatusIcon className={`size-5 ${config.variant === "success" ? "text-green-400" : config.variant === "destructive" ? "text-red-400" : "text-white/44"}`} />
            <Badge variant={config.variant}>{config.label}</Badge>
          </div>
          <Button
            onClick={handleSync}
            disabled={isSyncing}
            className="w-fit"
          >
            {isSyncing && <Loader2 className="size-4 animate-spin" />}
            Sync Now
          </Button>
        </div>

        <Separator />

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <p className="text-xs text-white/44 mb-1">Last Sync</p>
            <p className="font-mono text-sm text-white/90">
              {status?.lastSyncAt
                ? new Date(status.lastSyncAt).toLocaleString()
                : "—"}
            </p>
          </div>
          <div>
            <p className="text-xs text-white/44 mb-1">Last Successful Sync</p>
            <p className="font-mono text-sm text-white/90">
              {status?.lastSuccessfulSyncAt
                ? new Date(status.lastSuccessfulSyncAt).toLocaleString()
                : "—"}
            </p>
          </div>
        </div>

        {status?.error && (
          <div className="p-3 rounded-md bg-red-500/10 border border-red-500/20">
            <p className="text-xs text-white/44 mb-1">Last Error</p>
            <p className="text-sm text-red-400 font-mono">{status.error}</p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}