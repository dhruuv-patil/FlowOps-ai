"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import {
  connectIntegration,
  connectProvider,
  fetchProviders,
  getErrorMessage,
  getFieldErrors,
} from "@/lib/api";
import type { ProviderInfo } from "@/types";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

const SLACK_WEBHOOK_DOCS = "https://api.slack.com/messaging/webhooks";

interface ConnectIntegrationDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  providerType: string | null;
}

export function ConnectIntegrationDialog({
  open,
  onOpenChange,
  providerType,
}: ConnectIntegrationDialogProps) {
  const queryClient = useQueryClient();

  const [webhookUrl, setWebhookUrl] = useState("");
  const [selectedProvider, setSelectedProvider] =
    useState<ProviderInfo | null>(null);
  const [providerConfig, setProviderConfig] = useState<
    Record<string, string>
  >({});
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loadingProvider, setLoadingProvider] = useState(false);

  const isSlack = providerType === "slack";

  useEffect(() => {
    if (!open || !providerType || isSlack) {
      setSelectedProvider(null);
      setProviderConfig({});
      setErrors({});
      return;
    }

    let cancelled = false;

    async function loadProvider() {
      setLoadingProvider(true);
      setErrors({});

      try {
        const response = await fetchProviders();

        if (cancelled) return;

        const provider = response.providers.find(
          (item) => item.type === providerType,
        );

        if (!provider) {
          setSelectedProvider(null);
          setErrors({
            provider: `Provider "${providerType}" is not available.`,
          });
          return;
        }

        setSelectedProvider(provider);

        const initialConfig: Record<string, string> = {};

        for (const field of provider.credentialFields) {
          initialConfig[field.key] = "";
        }

        setProviderConfig(initialConfig);
      } catch (err) {
        if (!cancelled) {
          setErrors({
            provider: getErrorMessage(
              err,
              "Could not load provider configuration.",
            ),
          });
        }
      } finally {
        if (!cancelled) {
          setLoadingProvider(false);
        }
      }
    }

    void loadProvider();

    return () => {
      cancelled = true;
    };
  }, [open, providerType, isSlack]);

  const slackMutation = useMutation({
    mutationFn: () =>
      connectIntegration({
        type: "slack",
        webhookUrl: webhookUrl.trim(),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["integrations"] });
      toast.success("Slack connected.");
      setWebhookUrl("");
      setErrors({});
      onOpenChange(false);
    },
    onError: (err) => {
      const fieldErrors = getFieldErrors(err);

      if (Object.keys(fieldErrors).length > 0) {
        setErrors(fieldErrors);
      } else {
        toast.error(getErrorMessage(err, "Could not connect Slack."));
      }
    },
  });

  const providerMutation = useMutation({
    mutationFn: () => {
      if (!selectedProvider) {
        throw new Error("Provider configuration is not available.");
      }

      return connectProvider({
        type: selectedProvider.type,
        name: selectedProvider.name,
        config: providerConfig,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["integrations"] });

      toast.success(
        `${selectedProvider?.name ?? "Provider"} connected.`,
      );

      setProviderConfig({});
      setSelectedProvider(null);
      setErrors({});
      onOpenChange(false);
    },
    onError: (err) => {
      const fieldErrors = getFieldErrors(err);

      if (Object.keys(fieldErrors).length > 0) {
        setErrors(fieldErrors);
      } else {
        toast.error(
          getErrorMessage(
            err,
            `Could not connect ${
              selectedProvider?.name ?? "provider"
            }.`,
          ),
        );
      }
    },
  });

  function updateProviderField(key: string, value: string) {
    setProviderConfig((current) => ({
      ...current,
      [key]: value,
    }));

    setErrors((current) => {
      if (!current[key]) return current;

      const next = { ...current };
      delete next[key];
      return next;
    });
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    setErrors({});

    if (isSlack) {
      if (!webhookUrl.trim()) {
        setErrors({
          webhookUrl: "Paste your Slack incoming-webhook URL.",
        });
        return;
      }

      slackMutation.mutate();
      return;
    }

    if (!selectedProvider) {
      setErrors({
        provider: "Provider configuration is not available.",
      });
      return;
    }

    /*
     * CredentialField does not expose `required`, so we cannot infer
     * requiredness from the frontend catalog. The backend remains the
     * source of truth for credential validation.
     *
     * We only prevent submission when every credential value is empty.
     */
    const hasCredentials = selectedProvider.credentialFields.some(
      (field) => providerConfig[field.key]?.trim(),
    );

    if (!hasCredentials) {
      setErrors({
        provider: "Enter the required provider credentials.",
      });
      return;
    }

    providerMutation.mutate();
  }

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      setWebhookUrl("");
      setProviderConfig({});
      setSelectedProvider(null);
      setErrors({});
    }

    onOpenChange(nextOpen);
  }

  const isSubmitting =
    slackMutation.isPending || providerMutation.isPending;

  const title = isSlack
    ? "Connect Slack"
    : selectedProvider
      ? `Connect ${selectedProvider.name}`
      : providerType
        ? `Connect ${providerType}`
        : "Connect integration";

  const description = isSlack
    ? "Paste an incoming-webhook URL. Notification nodes set to the Slack channel will post to it during a run."
    : selectedProvider?.description ??
      "Enter the credentials required to connect this workflow provider.";

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        className="fixed left-1/2 top-1/2 flex h-[calc(100dvh-2rem)] max-h-[calc(100dvh-2rem)] w-[calc(100vw-2rem)] max-w-xl -translate-x-1/2 -translate-y-1/2 flex-col overflow-hidden rounded-xl border border-white/[0.08] bg-[#0a0a0a] p-0 text-white/90 shadow-2xl shadow-black/40 sm:h-[calc(100dvh-3rem)] sm:max-h-[calc(100dvh-3rem)]"
      >
        <form
          onSubmit={onSubmit}
          className="flex min-h-0 flex-1 flex-col"
        >
          <DialogHeader className="shrink-0 border-b border-white/[0.06] px-5 py-4">
            <DialogTitle className="text-white/90">
              {title}
            </DialogTitle>
            <DialogDescription className="text-white/35">
              {description}
            </DialogDescription>
          </DialogHeader>

          <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-5 py-4 [scrollbar-gutter:stable]">
            <div className="space-y-4">
              {isSlack ? (
                <div className="space-y-2">
                  <Label htmlFor="slack-webhook">
                    Incoming webhook URL
                  </Label>

                  <Input
                    id="slack-webhook"
                    autoFocus
                    type="url"
                    value={webhookUrl}
                    onChange={(e) => setWebhookUrl(e.target.value)}
                    placeholder="https://hooks.slack.com/services/T…/B…/…"
                    aria-invalid={!!errors.webhookUrl}
                  />

                  {errors.webhookUrl ? (
                    <p className="text-xs text-destructive">
                      {errors.webhookUrl}
                    </p>
                  ) : (
                    <p className="text-xs text-muted-foreground">
                      Create one in Slack under{" "}
                      <a
                        href={SLACK_WEBHOOK_DOCS}
                        target="_blank"
                        rel="noreferrer"
                        className="underline underline-offset-2 hover:text-foreground"
                      >
                        Incoming Webhooks
                      </a>
                      . Stored encrypted; never shown again.
                    </p>
                  )}
                </div>
              ) : loadingProvider ? (
                <div className="flex items-center justify-center py-10 text-sm text-muted-foreground">
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Loading provider configuration…
                </div>
              ) : errors.provider ? (
                <div className="rounded-lg border border-red-500/[0.12] bg-red-500/[0.035] px-3 py-2.5">
                  <p className="text-sm text-red-300/80">
                    {errors.provider}
                  </p>
                </div>
              ) : selectedProvider ? (
                <div className="space-y-4">
                  {selectedProvider.credentialFields.map((field) => (
                    <div key={field.key} className="space-y-2">
                      <Label htmlFor={`provider-${field.key}`}>
                        {field.label}
                      </Label>

                      <Input
                        id={`provider-${field.key}`}
                        type={field.secret ? "password" : "text"}
                        value={providerConfig[field.key] ?? ""}
                        onChange={(e) =>
                          updateProviderField(
                            field.key,
                            e.target.value,
                          )
                        }
                        placeholder={field.placeholder}
                        autoComplete="off"
                        aria-invalid={!!errors[field.key]}
                      />

                      {errors[field.key] ? (
                        <p className="text-xs text-destructive">
                          {errors[field.key]}
                        </p>
                      ) : field.help ? (
                        <p className="text-xs text-muted-foreground">
                          {field.help}
                        </p>
                      ) : null}
                    </div>
                  ))}
                </div>
              ) : null}
            </div>
          </div>

          <DialogFooter className="shrink-0 border-t border-white/[0.06] px-5 py-3">
            <Button
              type="button"
              variant="ghost"
              onClick={() => handleOpenChange(false)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>

            <Button
              type="submit"
              disabled={
                isSubmitting ||
                loadingProvider ||
                (!isSlack && !selectedProvider)
              }
            >
              {isSubmitting && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Connect
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}