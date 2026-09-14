"use client";

import { useState, useEffect, useCallback } from "react";
import { Loader2, RefreshCw, Eye, EyeOff, Zap } from "lucide-react";
import { toast } from "sonner";

import {
  testIntegration,
  discoverIntegrationWorkflows,
  monitorProviderWorkflow,
} from "@/lib/api";
import { getErrorMessage } from "@/lib/api";
import type { ExternalWorkflow, MonitoredWorkflowsResponse } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

interface WorkflowDiscoveryProps {
  integrationId: string;
  integrationName: string;
  onWorkflowsChange?: (workflows: ExternalWorkflow[]) => void;
  onClose: () => void;
}

export function WorkflowDiscovery({
  integrationId,
  integrationName,
  onWorkflowsChange,
  onClose,
}: WorkflowDiscoveryProps) {
  const [workflows, setWorkflows] = useState<ExternalWorkflow[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isTesting, setIsTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);
  const [searchQuery, setSearchQuery] = useState("");

  const loadWorkflows = useCallback(async () => {
    setIsLoading(true);
    try {
      const resp = await discoverIntegrationWorkflows(integrationId);
      setWorkflows(resp.workflows);
      onWorkflowsChange?.(resp.workflows);
    } catch (err) {
      toast.error(getErrorMessage(err, "Could not load workflows."));
    } finally {
      setIsLoading(false);
    }
  }, [integrationId, onWorkflowsChange]);

  useEffect(() => {
    loadWorkflows();
  }, [loadWorkflows]);

  const handleTestConnection = async () => {
    setIsTesting(true);
    setTestResult(null);
    try {
      const result = await testIntegration(integrationId);
      setTestResult(result);
      toast.success(result.success ? "Connection test passed." : `Test failed: ${result.message}`);
    } catch (err) {
      const msg = getErrorMessage(err, "Test failed");
      setTestResult({ success: false, message: msg });
      toast.error(msg);
    } finally {
      setIsTesting(false);
    }
  };

  const handleToggleMonitor = async (workflow: ExternalWorkflow, monitoring: boolean) => {
    try {
      await monitorProviderWorkflow(integrationId, workflow.id, monitoring);
      setWorkflows((prev) =>
        prev.map((w) =>
          w.id === workflow.id ? { ...w, monitoring } : w
        ),
      );
      toast.success(monitoring ? "Monitoring started." : "Monitoring stopped.");
    } catch (err) {
      toast.error(getErrorMessage(err, monitoring ? "Could not start monitoring." : "Could not stop monitoring."));
    }
  };

  const filteredWorkflows = workflows.filter(
    (w) =>
      w.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      w.id.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-4xl max-h-[80vh]">
        <DialogHeader>
          <DialogTitle>Workflows for {integrationName}</DialogTitle>
          <DialogDescription>
            Discovered workflows from your {integrationName} instance. Enable monitoring
            to sync execution telemetry and run anomaly detection.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {/* Test connection bar */}
          <div className="flex items-center gap-3">
            <Button variant="outline" onClick={handleTestConnection} disabled={isTesting}>
              {isTesting && <Loader2 className="size-4 animate-spin" />}
              Test Connection
            </Button>
            {testResult && (
              <Badge variant={testResult.success ? "success" : "destructive"}>
                {testResult.success ? "Connected" : "Failed"}
              </Badge>
            )}
            <Button variant="ghost" size="icon" onClick={loadWorkflows}>
              <RefreshCw className="size-4" />
            </Button>
          </div>

          {/* Search */}
          <div className="relative">
            <Input
              placeholder="Search workflows…"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9"
            />
            <Eye className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-white/44" />
          </div>

          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="size-6 animate-spin" />
            </div>
          ) : filteredWorkflows.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-8 text-white/44">
              <p>No workflows found.</p>
              <p className="text-sm">Click "Test Connection" then refresh to discover workflows.</p>
            </div>
          ) : (
            <Card>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Workflow</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Monitoring</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredWorkflows.map((wf) => (
                    <TableRow key={wf.id}>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <span className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                            <Zap className="size-4" />
                          </span>
                          <div>
                            <p className="font-medium text-white/90">{wf.name}</p>
                            <p className="text-xs text-white/44 font-mono">{wf.id}</p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant={wf.status === "active" ? "success" : "secondary"}>
                          {wf.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          variant={wf.monitoring ? "default" : "outline"}
                          size="sm"
                          onClick={() => handleToggleMonitor(wf, !wf.monitoring)}
                          className="w-28"
                        >
                          {wf.monitoring ? (
                            <>
                              <EyeOff className="size-3 mr-1" /> Stop
                            </>
                          ) : (
                            <>
                              <Eye className="size-3 mr-1" /> Monitor
                            </>
                          )}
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Card>
          )}
        </div>

        <DialogFooter>
          <Button variant="ghost" onClick={onClose}>
            Done
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

