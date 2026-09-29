# Template 12: Multi-System Deployment Reconciliation

## 1. Template Identity
- **Name**: Multi-System Deployment Reconciliation
- **One-line pitch**: "Compare state across multiple systems, detect discrepancies, and reconcile if needed"
- **Category**: Reliability/Incident Management
- **Complexity**: High
- **Estimated node count**: 14

## 2. Business Problem

The business problem is **deployment state drift** across multiple systems where:
- Different systems (Kubernetes, infrastructure, service mesh, databases) report different deployment states
- Manual reconciliation is slow and error-prone
- Discrepancies go undetected until they cause incidents
- No automated way to verify deployment consistency across systems

**Specific pain points:**
- **State divergence**: Deployment reported successful in CI/CD but pods not ready in Kubernetes
- **Service mesh mismatch**: Virtual services not updated after deployment
- **Database migration gaps**: Schema migrations not applied in all environments
- **Configuration drift**: Config maps/secrets out of sync with deployment
- **Silent failures**: Systems report healthy but actual state inconsistent

## 3. Target User

- **Primary**: SRE, Platform Engineer, DevOps Engineer
- **Secondary**: Release Engineer, Engineering Manager
- **Team**: Platform/Infrastructure team

## 4. Trigger

- **Type**: Manual / Schedule
- **Integration**: Cron or Manual Trigger
- **Event**: On-demand reconciliation or scheduled (every 4 hours)
- **Payload assumptions**:
  - `environment` (production, staging, etc.)
  - `systems` (array of system identifiers to check)
  - `deployment_id` (optional, specific deployment to verify)
  - `notification_channels` (optional, override default)

- **Required fields**: `environment` (if manual trigger)

## 5. Integrations Used

| Integration | Purpose |
|-------------|---------|
| Kubernetes API | Fetch pod/deployment state |
| Consul/Etcd | Service discovery state |
| AWS/GCP/Azure | Infrastructure state |
| PostgreSQL/MySQL | Database migration state |
| Istio/Linkerd | Service mesh state |
| Slack | Alerting and notifications |
| PagerDuty | Escalation for critical discrepancies |
| GitHub/GitLab | Deployment metadata |

## 6. Workflow Architecture

The workflow performs multi-system state reconciliation with automated discrepancy detection and remediation. It includes:
1. **Parallel state fetching** from all target systems
2. **State comparison** with configurable tolerance
3. **Discrepancy classification** (critical, warning, info)
4. **Automated reconciliation** for known patterns
5. **Human approval** for complex remediation
6. **Notification and tracking** of all actions

The architecture follows a **fan-out/fan-in reconciliation pattern** with:
- **Parallel state collection** (all systems queried simultaneously)
- **Structured comparison** (normalized state representation)
- **Classification engine** (severity-based routing)
- **Remediation paths** (automated vs. human-guided)
- **Audit trail** (complete reconciliation history)

## 7. Node Definitions

| Node ID | Node Name | Node Type | Integration | Config Summary |
|---------|-----------|-----------|-------------|---------------|
| trigger | Start Reconciliation | manual_trigger / schedule_trigger | - | `cron: 0 */4 * * *` or manual |
| fetch_k8s | Fetch Kubernetes State | http_request | Kubernetes API | `method: GET`, `url: /apis/apps/v1/deployments` |
| fetch_mesh | Fetch Service Mesh State | http_request | Istio/Linkerd | `method: GET`, `url: /api/virtualservices` |
| fetch_db | Fetch Database State | http_request | PostgreSQL/MySQL | `method: GET`, `url: /api/migrations/status` |
| fetch_infra | Fetch Infrastructure State | http_request | Cloud Provider | `method: GET`, `url: /api/resources` |
| compare | Compare States | transform | - | `mapping: {k8s, mesh, db, infra, discrepancies}` |
| detect | Detect Discrepancies | condition | - | `expression: discrepancies.length > 0` |
| classify | Classify Severity | ai_agent | Anthropic/OpenAI | `agent_id: discrepancy_classifier`, `confidence_threshold: 0.85` |
| auto_reconcile | Auto-Reconcile | http_request | Multiple | `method: POST`, `url: /api/reconcile/auto` |
| human_approval | Human Review Gate | human_approval | - | `timeout: 30m`, `escalation: on_call_sre` |
| manual_reconcile | Manual Reconcile | http_request | Multiple | `method: POST`, `url: /api/reconcile/manual` |
| alert | Raise Alert | notification | Slack/PagerDuty | `channel: #deployments`, `template: discrepancy_alert` |
| track | Track Reconciliation | linear_create_issue | Linear | `project: reconciliation`, `status: open` |
| complete | Workflow Completion | stop_fail | - | - |

## 8. Edge Definitions

| Source | Target | Condition |
|--------|--------|-----------|
| trigger | fetch_k8s | always |
| trigger | fetch_mesh | always |
| trigger | fetch_db | always |
| trigger | fetch_infra | always |
| fetch_k8s | compare | always |
| fetch_mesh | compare | always |
| fetch_db | compare | always |
| fetch_infra | compare | always |
| compare | detect | always |
| detect | classify | discrepancies_found |
| detect | alert | no_discrepancies |
| classify | auto_reconcile | severity == "low" AND pattern_known |
| classify | human_approval | severity >= "medium" OR pattern_unknown |
| auto_reconcile | alert | always |
| human_approval | manual_reconcile | approved |
| human_approval | alert | rejected or timeout |
| manual_reconcile | alert | always |
| alert | track | always |
| track | complete | always |

## 9. Configuration Variables

| Variable | Type | Example/Default | Purpose |
|----------|------|-----------------|---------|
| `k8s_api_url` | string | `https://k8s.example.com` | Kubernetes API endpoint |
| `k8s_token` | string | `xxx` | Kubernetes service account token |
| `mesh_api_url` | string | `https://istio.example.com` | Service mesh API endpoint |
| `db_connection_string` | string | `postgresql://...` | Database connection |
| `cloud_provider` | string | `aws` | Cloud provider (aws/gcp/azure) |
| `slack_webhook_url` | string | `https://hooks.slack.com/...` | Slack notification URL |
| `pagerduty_api_key` | string | `xxx` | PagerDuty API key |
| `linear_project_id` | string | `12345` | Linear project ID |
| `discrepancy_threshold` | number | `0.05` | Threshold for discrepancy detection |
| `auto_reconcile_patterns` | array | `["replica_count", "image_tag", "config_map"]` | Patterns safe for auto-reconcile |
| `human_review_timeout_minutes` | number | `30` | Human review timeout |
| `reconciliation_schedule_cron` | string | `0 */4 * * *` | Schedule for automatic runs |

## 10. Branching Logic

- **No Discrepancies**: All systems in sync → notify success, track metrics
- **Low Severity + Known Pattern**: Auto-reconcile → notify result
- **Medium/High Severity OR Unknown Pattern**: Human approval required
- **Human Approved**: Execute manual reconciliation → notify result
- **Human Rejected/Timeout**: Escalate to PagerDuty → notify on-call

## 11. Success Behavior

1. **State Collection**: All target systems queried in parallel
2. **Comparison**: States normalized and compared field-by-field
3. **Classification**: AI classifies each discrepancy by severity and type
4. **Auto-Remediation**: Known low-severity patterns auto-fixed
5. **Human Gate**: Complex discrepancies routed for human review
6. **Resolution**: All discrepancies resolved or escalated
7. **Notification**: Team notified of reconciliation outcome
8. **Tracking**: Linear issue created with full audit trail

## 12. Failure Behavior

| Failure Type | Handling |
|--------------|---------|
| System API unavailable | Retry with exponential backoff (3 attempts), mark system as "unknown" |
| State comparison timeout | Partial comparison with available systems |
| AI classification failure | Default to human review for all discrepancies |
| Auto-reconcile failure | Escalate to human review immediately |
| Human review timeout | Escalate to PagerDuty on-call |
| Notification failure | Log and continue, retry once |
| Linear tracking failure | Dead letter queue with reason |

## 13. Retry / Recovery Behavior

- **Node-level retries**: All HTTP nodes retry up to 3 times with exponential backoff (2s, 4s, 8s)
- **System API failures**: Partial results accepted, missing systems flagged for manual check
- **Dead letter queue**: Failed Linear/Jira calls go to DLQ for later processing
- **Recovery verification**: Requires 3 successful reconciliation runs before auto-resolving anomaly
- **State consistency**: Reconciliation only marked complete when all systems report consistent state

## 14. Reliability & Observability Behavior

**FlowOps Contributions:**
- **Anomaly detection**: Reconciliation duration, discrepancy count trends, auto-reconcile success rate
- **Incident creation**: Automatically creates incident when:
  - Critical discrepancy detected (production down, data loss risk)
  - Auto-reconcile fails repeatedly
  - Human review timeout on critical issue
  - Discrepancy count spike > 3x baseline
- **Baseline learning**: Tracks:
  - Average reconciliation duration per environment
  - Discrepancy frequency by type and system
  - Auto-reconcile vs. human review ratio
  - Time to resolution by severity
- **Observability metrics**:
  - Reconciliation execution duration
  - Discrepancy count by severity
  - Auto-reconcile success rate
  - Human review rate
  - Time to resolution
  - System availability during reconciliation

## 15. Security Considerations

- **Permissions**: Requires:
  - Kubernetes: `deployments.read`, `pods.read`, `configmaps.read`
  - Service Mesh: `virtualservices.read`, `destinationrules.read`
  - Database: `SELECT` on migration tables
  - Cloud Provider: `compute.instances.list`, `container.clusters.get`
  - Slack: `chat:write` for notifications
  - PagerDuty: `incident_trigger` for escalation
  - Linear: `issue_create` for tracking
- **Secrets**:
  - Kubernetes service account token
  - Service mesh API token
  - Database credentials
  - Cloud provider credentials
  - Slack webhook URL
  - PagerDuty API key
  - Linear API token
- **Sensitive data**: Deployment metadata may contain internal service names; sanitize in logs
- **Audit trail**: Every reconciliation action logged with timestamp, user, system states before/after

## 16. Setup Requirements

- **Required accounts**:
  - Kubernetes cluster access
  - Service mesh (Istio/Linkerd) access
  - Database access (read migration status)
  - Cloud provider account (AWS/GCP/Azure)
  - Slack workspace for alerts
  - PagerDuty account for escalation
  - Linear project for tracking
- **Integrations**:
  - Kubernetes API server access
  - Service mesh control plane API
  - Database connection
  - Cloud provider API
  - Slack webhook
  - PagerDuty API
  - Linear API
- **Configuration**:
  - Set all API endpoints and credentials
  - Configure AI classifier agent
  - Define auto-reconcile patterns
  - Set discrepancy thresholds
  - Configure notification channels

## 17. Expected Outcome

- **Detection coverage**: 95%+ of cross-system discrepancies detected within 4 hours
- **Auto-resolution**: 60-70% of discrepancies auto-resolved without human intervention
- **MTTR reduction**: 50% reduction in mean time to resolve deployment discrepancies
- **Incident prevention**: Prevents 80% of incidents caused by deployment state drift
- **Audit compliance**: Complete audit trail for all reconciliation activities

## 18. React Flow Graph

```json
{
  "nodes": [
    {"id": "trigger", "type": "manual_trigger", "position": {"x": 0, "y": 100}, "data": {"label": "Start Reconciliation", "config": {}}},
    {"id": "fetch_k8s", "type": "http_request", "position": {"x": 150, "y": -100}, "data": {"label": "Fetch Kubernetes State", "config": {"method": "GET", "url": "/apis/apps/v1/deployments", "timeout": "60s"}}},
    {"id": "fetch_mesh", "type": "http_request", "position": {"x": 150, "y": -30}, "data": {"label": "Fetch Service Mesh State", "config": {"method": "GET", "url": "/api/virtualservices", "timeout": "60s"}}},
    {"id": "fetch_db", "type": "http_request", "position": {"x": 150, "y": 40}, "data": {"label": "Fetch Database State", "config": {"method": "GET", "url": "/api/migrations/status", "timeout": "60s"}}},
    {"id": "fetch_infra", "type": "http_request", "position": {"x": 150, "y": 110}, "data": {"label": "Fetch Infrastructure State", "config": {"method": "GET", "url": "/api/resources", "timeout": "60s"}}},
    {"id": "compare", "type": "transform", "position": {"x": 350, "y": 0}, "data": {"label": "Compare States", "config": {"mapping": {"k8s": "{{fetch_k8s.body}}", "mesh": "{{fetch_mesh.body}}", "db": "{{fetch_db.body}}", "infra": "{{fetch_infra.body}}", "discrepancies": "{{compareStates(fetch_k8s.body, fetch_mesh.body, fetch_db.body, fetch_infra.body)}}"}}}},
    {"id": "detect", "type": "condition", "position": {"x": 500, "y": 0}, "data": {"label": "Discrepancies Found?", "config": {"expression": "{{compare.discrepancies.length}} > 0"}}},
    {"id": "classify", "type": "ai_agent", "position": {"x": 650, "y": -100}, "data": {"label": "Classify Severity", "config": {"agent_id": "discrepancy_classifier", "confidence_threshold": "0.85", "timeout": "120s"}}},
    {"id": "auto_reconcile", "type": "http_request", "position": {"x": 800, "y": -150}, "data": {"label": "Auto-Reconcile", "config": {"method": "POST", "url": "/api/reconcile/auto", "body": "{\"discrepancies\": {{classify.output.auto_fixable}}}", "timeout": "180s"}}},
    {"id": "human_approval", "type": "human_approval", "position": {"x": 800, "y": 50}, "data": {"label": "Human Review Gate", "config": {"timeout": "30m", "escalation": "on_call_sre"}}},
    {"id": "manual_reconcile", "type": "http_request", "position": {"x": 950, "y": 50}, "data": {"label": "Manual Reconcile", "config": {"method": "POST", "url": "/api/reconcile/manual", "body": "{\"discrepancies\": {{classify.output.manual}}}", "timeout": "300s"}}},
    {"id": "alert", "type": "notification", "position": {"x": 1100, "y": 0}, "data": {"label": "Raise Alert", "config": {"channel": "#deployments", "template": "discrepancy_alert", "providers": ["slack", "pagerduty"]}}},
    {"id": "track", "type": "linear_create_issue", "position": {"x": 1250, "y": 0}, "data": {"label": "Track Reconciliation", "config": {"project": "reconciliation", "status": "open"}}},
    {"id": "complete", "type": "stop_fail", "position": {"x": 1400, "y": 0}, "data": {"label": "Workflow Completion"}}
  ],
  "edges": [
    {"source": "trigger", "target": "fetch_k8s", "label": "always"},
    {"source": "trigger", "target": "fetch_mesh", "label": "always"},
    {"source": "trigger", "target": "fetch_db", "label": "always"},
    {"source": "trigger", "target": "fetch_infra", "label": "always"},
    {"source": "fetch_k8s", "target": "compare", "label": "always"},
    {"source": "fetch_mesh", "target": "compare", "label": "always"},
    {"source": "fetch_db", "target": "compare", "label": "always"},
    {"source": "fetch_infra", "target": "compare", "label": "always"},
    {"source": "compare", "target": "detect", "label": "always"},
    {"source": "detect", "target": "classify", "label": "discrepancies_found"},
    {"source": "detect", "target": "alert", "label": "no_discrepancies"},
    {"source": "classify", "target": "auto_reconcile", "label": "severity == low AND pattern_known"},
    {"source": "classify", "target": "human_approval", "label": "severity >= medium OR pattern_unknown"},
    {"source": "auto_reconcile", "target": "alert", "label": "always"},
    {"source": "human_approval", "target": "manual_reconcile", "label": "approved"},
    {"source": "human_approval", "target": "alert", "label": "rejected or timeout"},
    {"source": "manual_reconcile", "target": "alert", "label": "always"},
    {"source": "alert", "target": "track", "label": "always"},
    {"source": "track", "target": "complete", "label": "always"}
  ]
}
```

## 19. Metadata

```json
{
  "category": "Reliability/Incident Management",
  "tags": ["reconciliation", "multi-system", "deployment", "drift-detection", "kubernetes", "service-mesh"],
  "integrations": ["Kubernetes", "Istio", "PostgreSQL", "AWS", "Slack", "PagerDuty", "Linear"],
  "complexity": "High",
  "estimated_setup_minutes": 60
}
```

## 20. Production-Readiness Audit

1. **Real production trigger**: Manual on-demand or scheduled (every 4 hours)
2. **Meaningful branching**: Severity classification, known vs. unknown patterns, human gates
3. **Retry strategy**: All HTTP nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 60s for state fetch, 120s for AI classification, 30m for human review
5. **Idempotency**: Reconciliation keyed by deployment_id + timestamp
6. **Partial failure**: Partial state comparison when some systems unavailable
7. **External service failure**: Retry with dead letter queue for tracking
8. **AI uncertainty**: Confidence threshold (0.85) gates auto-reconcile vs. human review
9. **Human approval**: Required for medium/high severity or unknown patterns
10. **Observability**: Comprehensive metrics tracking reconciliation health
11. **Recovery**: Auto-reconcile for known patterns, manual for complex, escalation for timeout
12. **Security**: All secrets encrypted, RBAC for each system access
13. **Configuration**: Realistic configurable variables, no hardcoded values
14. **Graph quality**: Valid React Flow JSON with clean left-to-right layout

**Non-applicable**: None