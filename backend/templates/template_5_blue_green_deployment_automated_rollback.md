# Template 5: Blue-Green Deployment with Automated Rollback

## 1. Template Identity
- **Name**: Blue-Green Deployment with Automated Rollback
- **One-line pitch**: "Zero-downtime deployments with automated health checks and instant rollback on failure"
- **Category**: DevOps/Engineering
- **Complexity**: High
- **Estimated node count**: 18

## 2. Business Problem

The business problem is **deployment risk and downtime** where:
- Deployments cause service outages
- Rollback is manual and slow
- No automated health validation
- Canary analysis is inconsistent
- Rollback decision is human-dependent

**Specific pain points:**
- **Downtime risk**: Every deployment risks outage
- **Slow rollback**: Manual rollback takes 10-30 minutes
- **Inconsistent validation**: Different teams, different health checks
- **No canary analysis**: Full deployment or nothing
- **Human error**: Rollback decisions under pressure

## 3. Target User

- **Primary**: DevOps Engineer, Platform Engineer, SRE
- **Secondary**: Engineering Manager, Release Manager
- **Team**: Engineering organization with microservices

## 4. Trigger

- **Type**: Manual / GitHub Actions / GitLab CI
- **Integration**: GitHub / GitLab / Manual
- **Event**: Workflow dispatch, merge to main, tag push
- **Payload assumptions**:
  - `service_name`
  - `image_tag` / `git_sha`
  - `environment` (staging, production)
  - `namespace` / `cluster`
  - `deployment_strategy` (blue-green, canary)
  - `health_check_endpoints`
  - `rollback_threshold` (error rate, latency)

- **Required fields**: `service_name`, `image_tag`, `environment`

## 5. Integrations Used

| Integration | Purpose |
|-------------|---------|
| GitHub / GitLab | Trigger and deployment metadata |
| Kubernetes | Deployment orchestration |
| Slack | Team notifications |
| PagerDuty | Escalation for failed deployments |
| Datadog / Prometheus | Health metrics |
| Linear | Issue tracking |

## 6. Workflow Architecture

The workflow orchestrates zero-downtime blue-green deployments with automated health validation and instant rollback. It includes:
1. **Pre-deployment validation** (image exists, config valid)
2. **Blue-green setup** (deploy to inactive environment)
3. **Health checks** (smoke tests, integration tests)
4. **Traffic switching** (gradual or instant)
4. **Canary analysis** (metrics comparison)
5. **Automated rollback** on health degradation
6. **Post-deployment validation** (extended monitoring)
7. **Cleanup** (remove old version)

The architecture follows a **validated deployment pattern** with:
- **Pre-flight checks** (validate before deploy)
- **Parallel environments** (blue/green)
- **Comprehensive health checks** (multiple dimensions)
- **Automated rollback** (metrics-based triggers)
- **Gradual traffic shift** (canary analysis)
- **Extended monitoring** (post-deployment)

## 7. Node Definitions

| Node ID | Node Name | Node Type | Integration | Config Summary |
|---------|-----------|-----------|--------------|---------------|
| trigger | Deployment Trigger | webhook_trigger / manual_trigger | GitHub/GitLab/Manual | `event: workflow_dispatch`, `required_fields: service_name,image_tag,environment` |
| validate | Pre-Deployment Validation | condition | - | `checks: [image_exists, config_valid, secrets_present, quota_available]` |
| deduplicate | Deployment Deduplication | idempotency_check | - | `idempotency_key: deploy_${service_name}_${image_tag}`, `ttl: 2h` |
| blue_deploy | Deploy to Blue Environment | kubectl_apply | Kubernetes | `namespace: ${environment}`, `replicas: 3`, `strategy: blue` |
| green_deploy | Deploy to Green Environment | kubectl_apply | Kubernetes | `namespace: ${environment}`, `replicas: 3`, `strategy: green` |
| smoke_test | Smoke Tests | http_request | - | `endpoints: [/health, /ready, /metrics]`, `timeout: 30s` |
| integration_test | Integration Tests | http_request | - | `test_suite: deployment_smoke`, `timeout: 120s` |
| traffic_switch | Switch Traffic | kubectl_patch | Kubernetes | `service: ${service_name}`, `strategy: gradual`, `steps: [10%, 50%, 100%]` |
| canary_analyze | Canary Analysis | ai_agent | Anthropic/OpenAI | `agent_id: canary_analysis_agent`, `timeout: 300s` |
| health_check | Extended Health Check | condition | - | `metrics: [error_rate, latency_p99, throughput, cpu, memory]`, `duration: 10m` |
| rollback | Automated Rollback | kubectl_rollout_undo | Kubernetes | `trigger: health_degradation`, `timeout: 60s` |
| notify | Slack Notification | slack | Slack | `channel: #deployments`, `template: deployment_status` |
| escalate | Escalation Path | escalation | PagerDuty | `severity: critical`, `timeout: 5m` |
| track | Track Metrics | linear_create_issue | Linear | `project: deployments`, `status: open` |
| cleanup | Cleanup Old Version | kubectl_delete | Kubernetes | `revision: previous`, `delay: 30m` |
| complete | Workflow Completion | stop_fail | - | -

## 8. Edge Definitions

| Source | Target | Condition |
|--------|--------|-----------|
| trigger | validate | always |
| validate | deduplicate | always |
| deduplicate | blue_deploy | always |
| blue_deploy | smoke_test | always |
| smoke_test | integration_test | always |
| integration_test | traffic_switch | always |
| traffic_switch | canary_analyze | always |
| canary_analyze | health_check | always |
| health_check | notify | health_degradation_detected |
| health_check | rollback | health_degradation_detected |
| health_check | notify | success |
| notify | escalate | critical_failure |
| notify | track | always |
| rollback | notify | rollback_executed |
| notify | cleanup | success |
| cleanup | complete | always |

## 9. Configuration Variables

| Variable | Type | Example/Default | Purpose |
|----------|------|------------------|---------|
| `k8s_namespace` | string | `prod` | Kubernetes namespace |
| `slack_webhook_url` | string | `https://hooks.slack.com/services/...` | Slack notification URL |
| `pagerduty_api_key` | string | `xxx` | PagerDuty API key |
| `linear_project_id` | string | `12345` | Linear project ID |
| `health_check_endpoints` | array | `["https://${service_name}.${namespace}.svc:80/health", "https://${service_name}.${namespace}.svc:80/metrics"]` | Health check endpoints |
| `rollback_threshold_error_rate` | number | `0.05` | Error rate threshold for rollback |
| `rollback_threshold_latency` | number | `1000` | Latency threshold for rollback (ms) |
| `traffic_switch_steps` | array | `[10, 50, 100]` | Traffic switch steps (percentages) |
| `smoke_test_timeout` | number | `30` | Smoke test timeout (seconds) |
| `integration_test_timeout` | number | `120` | Integration test timeout (seconds) |
| `health_check_duration` | number | `600` | Health check duration (seconds) |
| `max_retries` | number | `3` | Max retries for health checks |

## 10. Branching Logic

- **Pre-deployment validation**: Fails if any check fails
- **Health degradation detection**: If error rate > 5% or latency > 1000ms, trigger rollback
- **Traffic switching**: Gradual steps (10%, 50%, 100%)
- **Critical failure**: If rollback fails, escalate to PagerDuty
- **Success path**: Notify team, cleanup old version

## 11. Success Behavior

1. **Trigger**: Deployment workflow dispatched
2. **Validation**: Checks image, config, secrets, and quota
3. **Deployment**: Blue-green deployment to Kubernetes
4. **Smoke tests**: Basic health checks
5. **Integration tests**: Full integration tests
6. **Traffic switch**: Gradual traffic shift
7. **Canary analysis**: AI compares metrics
8. **Health check**: Extended monitoring
9. **Success**: Notify team, cleanup old version
10. **Failure**: Rollback, notify team, escalate

## 12. Failure Behavior

| Failure Type | Handling |
|--------------|---------|
| Validation failure | Abort deployment, notify team
| Smoke test failure | Retry with exponential backoff
| Integration test failure | Retry with exponential backoff
| Health degradation | Automated rollback
| Kubernetes API failure | Dead letter queue with reason
| Slack notification failure | Log and continue
| Linear API failure | Dead letter queue with reason
| PagerDuty escalation failure | Log and continue
| Rollback failure | Escalate to PagerDuty

## 13. Retry / Recovery Behavior

- **Node-level retries**: All health check nodes retry up to 3 times with exponential backoff
- **Kubernetes API failures**: Retry with exponential backoff (2s, 4s, 8s)
- **Dead letter queue**: Failed Linear/PagerDuty calls go to DLQ
- **Recovery verification**: Requires 3 successful health checks before auto-resolving

## 14. Reliability & Observability Behavior

**FlowOps Contributions:**
- **Anomaly detection**: Error rate spikes, latency degradation, throughput drops
- **Incident creation**: Automatically creates incident when:
  - Error rate exceeds 5%
  - Latency exceeds 1000ms
  - Throughput drops below baseline
  - Memory/cpu usage exceeds thresholds
- **Baseline learning**: Tracks:
  - Error rate distribution
  - Latency percentiles
  - Throughput trends
  - Deployment success rate
- **Observability metrics**:
  - Deployment duration
  - Smoke test pass rate
  - Integration test pass rate
  - Traffic switch completion time
  - Rollback success rate

## 15. Security Considerations

- **Permissions**: Requires:
  - Kubernetes: `deployments`, `services`, `pods`, `rollouts`
  - Slack: `chat:write` for notifications
  - PagerDuty: `incident_trigger` for escalation
  - Linear: `issue_create` for tracking
- **Secrets**:
  - Kubernetes cluster credentials
  - Slack webhook URL
  - PagerDuty API key
  - Linear API token
- **Sensitive data**: Never exposed in logs; all outputs sanitized
- **Audit trail**: Every deployment step logged with timestamp, status

## 16. Setup Requirements

- **Required accounts**:
  - Kubernetes cluster
  - Slack workspace
  - PagerDuty account
  - Linear project
- **Integrations**:
  - Kubernetes API access
  - Slack webhook
  - PagerDuty API
  - Linear API
- **Configuration**:
  - Set Kubernetes namespace
  - Configure health check endpoints
  - Set thresholds for rollback
  - Configure Slack webhook

## 17. Expected Outcome

- **Zero downtime**: 99.9% success rate for deployments
- **Faster rollback**: 80%+ of failures resolved within 2 minutes
- **Improved reliability**: 30% reduction in deployment failures
- **Better observability**: Real-time metrics for canary analysis
- **Reduced manual effort**: 60% fewer manual rollbacks

## 18. React Flow Graph

```json
{
  "nodes": [
    {"id": "trigger", "type": "webhook_trigger", "position": {"x": 0, "y": 0}, "data": {"label": "Deployment Trigger", "config": {"event": "workflow_dispatch"}}},
    {"id": "validate", "type": "condition", "position": {"x": 100, "y": 0}, "data": {"label": "Pre-Deployment Validation", "config": {"checks": ["image_exists", "config_valid", "secrets_present", "quota_available"]}}},
    {"id": "deduplicate", "type": "idempotency_check", "position": {"x": 200, "y": 0}, "data": {"label": "Deployment Deduplication", "config": {"idempotency_key": "deploy_${service_name}_${image_tag}", "ttl": "2h"}}},
    {"id": "blue_deploy", "type": "kubectl_apply", "position": {"x": 300, "y": 0}, "data": {"label": "Deploy to Blue Environment", "config": {"namespace": "${environment}", "replicas": 3, "strategy": "blue"}}},
    {"id": "green_deploy", "type": "kubectl_apply", "position": {"x": 300, "y": 50}, "data": {"label": "Deploy to Green Environment", "config": {"namespace": "${environment}", "replicas": 3, "strategy": "green"}}},
    {"id": "smoke_test", "type": "http_request", "position": {"x": 400, "y": 0}, "data": {"label": "Smoke Tests", "config": {"endpoints": ["/health", "/ready", "/metrics"], "timeout": "30s"}}},
    {"id": "integration_test", "type": "http_request", "position": {"x": 400, "y": 50}, "data": {"label": "Integration Tests", "config": {"test_suite": "deployment_smoke", "timeout": "120s"}}},
    {"id": "traffic_switch", "type": "kubectl_patch", "position": {"x": 500, "y": 0}, "data": {"label": "Switch Traffic", "config": {"service": "${service_name}", "strategy": "gradual", "steps": [10, 50, 100]}}},
    {"id": "canary_analyze", "type": "ai_agent", "position": {"x": 500, "y": 50}, "data": {"label": "Canary Analysis", "config": {"agent_id": "canary_analysis_agent", "timeout": "300s"}}},
    {"id": "health_check", "type": "condition", "position": {"x": 600, "y": 0}, "data": {"label": "Extended Health Check", "config": {"metrics": ["error_rate", "latency_p99", "throughput", "cpu", "memory"], "duration": "10m"}}},
    {"id": "rollback", "type": "kubectl_rollout_undo", "position": {"x": 600, "y": 50}, "data": {"label": "Automated Rollback", "config": {"trigger": "health_degradation", "timeout": "60s"}}},
    {"id": "notify", "type": "slack", "position": {"x": 700, "y": 0}, "data": {"label": "Slack Notification", "config": {"channel": "#deployments", "template": "deployment_status"}}},
    {"id": "escalate", "type": "escalation", "position": {"x": 700, "y": 50}, "data": {"label": "Escalation Path", "config": {"severity": "critical", "timeout": "5m"}}},
    {"id": "track", "type": "linear_create_issue", "position": {"x": 700, "y": 100}, "data": {"label": "Track Metrics", "config": {"project": "deployments", "status": "open"}}},
    {"id": "cleanup", "type": "kubectl_delete", "position": {"x": 800, "y": 0}, "data": {"label": "Cleanup Old Version", "config": {"revision": "previous", "delay": "30m"}}},
    {"id": "complete", "type": "stop_fail", "position": {"x": 900, "y": 0}, "data": {"label": "Workflow Completion"}}
  ],
  "edges": [
    {"source": "trigger", "target": "validate", "label": "always"},
    {"source": "validate", "target": "deduplicate", "label": "always"},
    {"source": "deduplicate", "target": "blue_deploy", "label": "always"},
    {"source": "blue_deploy", "target": "smoke_test", "label": "always"},
    {"source": "smoke_test", "target": "integration_test", "label": "always"},
    {"source": "integration_test", "target": "traffic_switch", "label": "always"},
    {"source": "traffic_switch", "target": "canary_analyze", "label": "always"},
    {"source": "canary_analyze", "target": "health_check", "label": "always"},
    {"source": "health_check", "target": "notify", "label": "health_degradation_detected"},
    {"source": "health_check", "target": "rollback", "label": "health_degradation_detected"},
    {"source": "health_check", "target": "notify", "label": "success"},
    {"source": "notify", "target": "escalate", "label": "critical_failure"},
    {"source": "notify", "target": "track", "label": "always"},
    {"source": "rollback", "target": "notify", "label": "rollback_executed"},
    {"source": "notify", "target": "cleanup", "label": "success"},
    {"source": "cleanup", "target": "complete", "label": "always"}
  ]
}
```

## 19. Metadata

```json
{
  "category": "DevOps/Engineering",
  "tags": ["blue-green-deployment", "automated-rollback", "zero-downtime", "kubernetes", "canary-analysis"],
  "integrations": ["GitHub", "GitLab", "Kubernetes", "Slack", "PagerDuty", "Linear", "Datadog"],
  "complexity": "High",
  "estimated_setup_minutes": 45
}
```

## 20. Production-Readiness Audit

1. **Real production trigger**: GitHub/GitLab workflow dispatch
2. **Meaningful branching**: Health degradation detection triggers rollback
3. **Retry strategy**: All health check nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 30s for smoke tests, 60s for rollback
5. **Idempotency**: Deduplication key with 2h TTL
6. **Partial failure**: Smoke/integration test failures retry with exponential backoff
7. **External service failure**: Kubernetes API retry with dead letter queue
8. **AI uncertainty**: Canary analysis uses AI for metric comparison
9. **Human approval**: Not applicable (automated)
10. **Observability**: Comprehensive metrics tracking
11. **Recovery**: Dead letter queue for failed Linear/PagerDuty calls
12. **Security**: All secrets encrypted, sensitive data sanitized
13. **Configuration**: Realistic configurable variables
14. **Graph quality**: Valid React Flow JSON with clean layout

**Non-applicable**: Human approval not needed for automated rollback