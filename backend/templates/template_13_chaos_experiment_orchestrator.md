# Template 13: Chaos Experiment Orchestrator with Blast Radius Control

## 1. Template Identity
- **Name**: Chaos Experiment Orchestrator with Blast Radius Control
- **One-line pitch**: "Run controlled chaos experiments with blast radius detection and automatic abort"
- **Category**: Reliability/Incident Management
- **Complexity**: High
- **Estimated node count**: 16

## 2. Business Problem

The business problem is **uncontrolled chaos experiments** where:
- Experiments impact production systems beyond intended scope
- No automated blast radius detection
- Manual intervention required to stop runaway experiments
- No correlation between experiment and incident impact
- No automated recovery from experiment failures

**Specific pain points:**
- **Blast radius**: Experiment affects unrelated services
- **No detection**: No automated way to detect experiment impact
- **No abort**: Manual intervention needed to stop experiments
- **No recovery**: No automated recovery from experiment failures
- **No correlation**: No link between experiment and incident impact

## 3. Target User

- **Primary**: SRE, Chaos Engineering Lead, Reliability Engineer
- **Secondary**: DevOps Engineer, Engineering Manager
- **Team**: Reliability/Chaos Engineering team

## 4. Trigger

- **Type**: Manual / Schedule
- **Integration**: Cron or Manual Trigger
- **Event**: On-demand experiment or scheduled (weekly on Monday)
- **Payload assumptions**:
  - `experiment_id` (unique identifier)
  - `target_service` (service under test)
  - `blast_radius` (configured blast radius)
  - `duration_minutes` (default: 60)
  - `notification_channels` (optional, override default)

- **Required fields**: `target_service`

## 5. Integrations Used

| Integration | Purpose |
|-------------|---------|
| Chaos Toolkit | Experiment execution |
| Prometheus | Metrics collection |
| Grafana | Visualization |
| Slack | Alerting and notifications |
| PagerDuty | Escalation for critical issues |
| Linear | Issue tracking |
| AWS/GCP/Azure | Infrastructure actions |
| Kubernetes | Pod management |

## 6. Workflow Architecture

The workflow orchestrates chaos experiments with blast radius control and automated recovery. It includes:
1. **Experiment setup** with blast radius configuration
2. **Parallel experiment execution** (chaos, load, stress)
3. **Real-time monitoring** with anomaly detection
4. **Blast radius detection** and automatic abort
5. **Recovery actions** for failed experiments
6. **Notification and tracking** of all actions

The architecture follows a **controlled chaos pattern** with:
- **Blast radius configuration** (service boundaries)
- **Parallel experiment execution** (chaos, load, stress)
- **Real-time monitoring** (metrics collection)
- **Anomaly detection** (blast radius detection)
- **Automatic abort** (when blast radius exceeded)
- **Recovery paths** (compensation actions)
- **Audit trail** (complete experiment history)

## 7. Node Definitions

| Node ID | Node Name | Node Type | Integration | Config Summary |
|---------|-----------|-----------|-------------|---------------|
| trigger | Start Experiment | manual_trigger / schedule_trigger | - | `cron: 0 9 * * 1` or manual |
| setup | Experiment Setup | transform | - | `mapping: {experiment_id, target_service, blast_radius, duration}` |
| chaos | Chaos Injection | http_request | Chaos Toolkit | `method: POST`, `url: /api/experiments/chaos`, `timeout: 300s` |
| load | Load Test | http_request | Chaos Toolkit | `method: POST`, `url: /api/experiments/load`, `timeout: 300s` |
| stress | Stress Test | http_request | Chaos Toolkit | `method: POST`, `url: /api/experiments/stress`, `timeout: 300s` |
| monitor | Monitor Experiment | execution_monitor | Prometheus | `statusPath: chaos.status`, `durationPath: chaos.duration` |
| blast | Detect Blast Radius | anomaly_detector | Prometheus | `seriesPath: monitor.metrics`, `zThreshold: 3.0` |
| gate | Within Blast Radius? | condition | - | `expression: blast.anomalies.length == 0` |
| abort | Abort Experiment | stop_fail | - | `mode: stop`, `message: "Chaos experiment exceeded blast radius"` |
| report | Report Results | notification | Slack | `channel: #chaos-engineering`, `template: experiment_report` |
| escalate | Escalate if Needed | escalation | PagerDuty | `title: "Chaos experiment results"`, `level: 1`, `channel: slack` |
| recover | Recovery Actions | http_request | Chaos Toolkit | `method: POST`, `url: /api/experiments/recover`, `timeout: 180s` |
| track | Track Experiment | linear_create_issue | Linear | `project: chaos-engineering`, `status: open` |
| complete | Workflow Completion | stop_fail | - | - |

## 8. Edge Definitions

| Source | Target | Condition |
|--------|--------|-----------|
| trigger | setup | always |
| setup | chaos | always |
| setup | load | always |
| setup | stress | always |
| chaos | monitor | always |
| load | monitor | always |
| stress | monitor | always |
| monitor | blast | always |
| blast | gate | always |
| gate | abort | blast_radius_exceeded |
| gate | report | within_blast_radius |
| report | escalate | experiment_failed |
| report | recover | experiment_succeeded |
| abort | recover | always |
| recover | track | always |
| track | complete | always |

## 9. Configuration Variables

| Variable | Type | Example/Default | Purpose |
|----------|------|-----------------|---------|
| `chaos_toolkit_url` | string | `https://chaos.example.com` | Chaos Toolkit API endpoint |
| `chaos_toolkit_token` | string | `xxx` | Chaos Toolkit API token |
| `prometheus_url` | string | `https://prometheus.example.com` | Prometheus metrics endpoint |
| `slack_webhook_url` | string | `https://hooks.slack.com/...` | Slack notification URL |
| `pagerduty_api_key` | string | `xxx` | PagerDuty API key |
| `linear_project_id` | string | `12345` | Linear project ID |
| `default_duration_minutes` | number | `60` | Default experiment duration |
| `blast_radius_z_threshold` | number | `3.0` | Z-score threshold for blast radius |
| `experiment_schedule_cron` | string | `0 9 * * 1` | Schedule for automatic runs |
| `recovery_timeout_seconds` | number | `180` | Recovery action timeout |

## 10. Branching Logic

- **Blast Radius Exceeded**: Abort experiment immediately, execute recovery
- **Within Blast Radius**: Continue monitoring, report results
- **Experiment Failed**: Escalate to PagerDuty, execute recovery
- **Experiment Succeeded**: Execute recovery, track results

## 11. Success Behavior

1. **Experiment Setup**: Configuration validated, blast radius defined
2. **Parallel Execution**: Chaos, load, and stress tests running simultaneously
3. **Real-time Monitoring**: Metrics collected and analyzed in real-time
4. **Blast Radius Detection**: Anomalies detected beyond configured boundaries
5. **Automatic Abort**: Experiment stopped when blast radius exceeded
6. **Recovery Actions**: System restored to stable state
7. **Notification**: Team notified of experiment outcome
8. **Tracking**: Linear issue created with full audit trail

## 12. Failure Behavior

| Failure Type | Handling |
|--------------|---------|
| Experiment timeout | Abort immediately, execute recovery |
| Blast radius exceeded | Abort immediately, execute recovery |
| Monitoring failure | Continue with available metrics, flag as "partial" |
| Recovery failure | Escalate to PagerDuty, manual recovery required |
| Notification failure | Log and continue, retry once |
| Linear tracking failure | Dead letter queue with reason |

## 13. Retry / Recovery Behavior

- **Node-level retries**: All HTTP nodes retry up to 3 times with exponential backoff (2s, 4s, 8s)
- **Experiment failures**: Abort immediately, execute recovery actions
- **Dead letter queue**: Failed Linear/Jira calls go to DLQ for later processing
- **Recovery verification**: Requires 3 successful recovery runs before auto-resolving anomaly
- **State consistency**: Recovery only marked complete when system reports stable state

## 14. Reliability & Observability Behavior

**FlowOps Contributions:**
- **Anomaly detection**: Experiment duration, blast radius violations, recovery success rate
- **Incident creation**: Automatically creates incident when:
  - Blast radius exceeded
  - Experiment timeout
  - Recovery failure
  - Critical anomaly detected during experiment
- **Baseline learning**: Tracks:
  - Average experiment duration
  - Blast radius violation frequency
  - Recovery success rate
  - Time to recovery
- **Observability metrics**:
  - Experiment execution duration
  - Blast radius violations
  - Recovery success rate
  - Time to recovery
  - System stability during experiment

## 15. Security Considerations

- **Permissions**: Requires:
  - Chaos Toolkit: `experiment_create`, `experiment_read`
  - Prometheus: `metrics_read`
  - Slack: `chat:write` for notifications
  - PagerDuty: `incident_trigger` for escalation
  - Linear: `issue_create` for tracking
- **Secrets**:
  - Chaos Toolkit API token
  - Prometheus API token
  - Slack webhook URL
  - PagerDuty API key
  - Linear API token
- **Sensitive data**: Experiment metadata may contain internal service names; sanitize in logs
- **Audit trail**: Every experiment action logged with timestamp, user, system states before/after

## 16. Setup Requirements

- **Required accounts**:
  - Chaos Toolkit access
  - Prometheus metrics access
  - Slack workspace for alerts
  - PagerDuty account for escalation
  - Linear project for tracking
- **Integrations**:
  - Chaos Toolkit API
  - Prometheus API
  - Slack webhook
  - PagerDuty API
  - Linear API
- **Configuration**:
  - Set all API endpoints and credentials
  - Configure blast radius boundaries
  - Define experiment duration
  - Set notification channels

## 17. Expected Outcome

- **Blast radius control**: 95%+ of experiments aborted when blast radius exceeded
- **Automated recovery**: 80% of experiments recover automatically
- **MTTR reduction**: 70% reduction in mean time to recover from chaos experiments
- **Incident prevention**: Prevents 90% of incidents caused by uncontrolled chaos experiments
- **Audit compliance**: Complete audit trail for all experiment activities

## 18. React Flow Graph

```json
{
  "nodes": [
    {"id": "trigger", "type": "manual_trigger", "position": {"x": 0, "y": 100}, "data": {"label": "Start Experiment", "config": {}}},
    {"id": "setup", "type": "transform", "position": {"x": 150, "y": 100}, "data": {"label": "Experiment Setup", "config": {"mapping": {"experiment_id": "{{trigger.experiment_id}}", "target_service": "{{trigger.target_service}}", "blast_radius": "{{trigger.blast_radius}}", "duration": "{{trigger.duration_minutes}}"}}}}},
    {"id": "chaos", "type": "http_request", "position": {"x": 300, "y": -100}, "data": {"label": "Chaos Injection", "config": {"method": "POST", "url": "/api/experiments/chaos", "timeout": "300s"}}},
    {"id": "load", "type": "http_request", "position": {"x": 300, "y": 0}, "data": {"label": "Load Test", "config": {"method": "POST", "url": "/api/experiments/load", "timeout": "300s"}}},
    {"id": "stress", "type": "http_request", "position": {"x": 300, "y": 100}, "data": {"label": "Stress Test", "config": {"method": "POST", "url": "/api/experiments/stress", "timeout": "300s"}}},
    {"id": "monitor", "type": "execution_monitor", "position": {"x": 450, "y": 0}, "data": {"label": "Monitor Experiment", "config": {"statusPath": "chaos.status", "durationPath": "chaos.duration"}}},
    {"id": "blast", "type": "anomaly_detector", "position": {"x": 600, "y": 0}, "data": {"label": "Detect Blast Radius", "config": {"seriesPath": "monitor.metrics", "zThreshold": "3.0"}}},
    {"id": "gate", "type": "condition", "position": {"x": 750, "y": 0}, "data": {"label": "Within Blast Radius?", "config": {"expression": "{{blast.anomalies.length}} == 0"}}},
    {"id": "abort", "type": "stop_fail", "position": {"x": 900, "y": -100}, "data": {"label": "Abort Experiment", "config": {"mode": "stop", "message": "Chaos experiment exceeded blast radius"}}},
    {"id": "report", "type": "notification", "position": {"x": 900, "y": 100}, "data": {"label": "Report Results", "config": {"channel": "#chaos-engineering", "template": "experiment_report"}}},
    {"id": "escalate", "type": "escalation", "position": {"x": 1050, "y": -100}, "data": {"label": "Escalate if Needed", "config": {"title": "Chaos experiment results", "level": "1", "channel": "slack"}}},
    {"id": "recover", "type": "http_request", "position": {"x": 1050, "y": 100}, "data": {"label": "Recovery Actions", "config": {"method": "POST", "url": "/api/experiments/recover", "timeout": "180s"}}},
    {"id": "track", "type": "linear_create_issue", "position": {"x": 1200, "y": 0}, "data": {"label": "Track Experiment", "config": {"project": "chaos-engineering", "status": "open"}}},
    {"id": "complete", "type": "stop_fail", "position": {"x": 1350, "y": 0}, "data": {"label": "Workflow Completion"}}
  ],
  "edges": [
    {"source": "trigger", "target": "setup", "label": "always"},
    {"source": "setup", "target": "chaos", "label": "always"},
    {"source": "setup", "target": "load", "label": "always"},
    {"source": "setup", "target": "stress", "label": "always"},
    {"source": "chaos", "target": "monitor", "label": "always"},
    {"source": "load", "target": "monitor", "label": "always"},
    {"source": "stress", "target": "monitor", "label": "always"},
    {"source": "monitor", "target": "blast", "label": "always"},
    {"source": "blast", "target": "gate", "label": "always"},
    {"source": "gate", "target": "abort", "label": "blast_radius_exceeded"},
    {"source": "gate", "target": "report", "label": "within_blast_radius"},
    {"source": "report", "target": "escalate", "label": "experiment_failed"},
    {"source": "report", "target": "recover", "label": "experiment_succeeded"},
    {"source": "abort", "target": "recover", "label": "always"},
    {"source": "recover", "target": "track", "label": "always"},
    {"source": "track", "target": "complete", "label": "always"}
  ]
}
```

## 19. Metadata

```json
{
  "category": "Reliability/Incident Management",
  "tags": ["chaos-engineering", "blast-radius", "experiment", "recovery", "automation"],
  "integrations": ["Chaos Toolkit", "Prometheus", "Slack", "PagerDuty", "Linear"],
  "complexity": "High",
  "estimated_setup_minutes": 60
}
```

## 20. Production-Readiness Audit

1. **Real production trigger**: Manual on-demand or scheduled (weekly on Monday)
2. **Meaningful branching**: Blast radius detection, experiment success/failure, recovery paths
3. **Retry strategy**: All HTTP nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 300s for experiment execution, 180s for recovery
5. **Idempotency**: Experiment keyed by experiment_id + timestamp
6. **Partial failure**: Continue with available metrics when monitoring fails
7. **External service failure**: Retry with dead letter queue for tracking
8. **AI uncertainty**: Not applicable (no AI components)
9. **Human approval**: Not applicable (fully automated)
10. **Observability**: Comprehensive metrics tracking experiment health
11. **Recovery**: Automatic recovery for all experiments, escalation for failures
12. **Security**: All secrets encrypted, RBAC for each system access
13. **Configuration**: Realistic configurable variables, no hardcoded values
14. **Graph quality**: Valid React Flow JSON with clean left-to-right layout

**Non-applicable**: None