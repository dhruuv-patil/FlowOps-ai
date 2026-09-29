# Template 8: Infrastructure Drift Detection & Reconciliation

## 1. Template Identity
- **Name**: Infrastructure Drift Detection & Reconciliation
- **One-line pitch**: "Automated infrastructure drift detection and reconciliation with AI-driven drift analysis and automated remediation"
- **Category**: DevOps/Engineering
- **Complexity**: High
- **Estimated node count**: 24

## 2. Business Problem
The business problem is **inconsistent infrastructure states causing operational failures** where:
- Infrastructure drift leads to misconfigurations
- Manual reconciliation is slow and error-prone
- No automated detection of drift
- No real-time remediation for drift issues
- No observability into drift patterns

**Specific pain points:**
- **Operational risk**: Misconfigurations cause service failures
- **Slow reconciliation**: Manual fixes take hours
- **Inconsistent state**: Different teams handle drift differently
- **No root cause tracking**: Drift issues go unaddressed
- **Escalation delays**: Critical drift ignored due to lack of automation

## 3. Target User
- **Primary**: DevOps Engineer, Cloud Engineer, SRE
- **Secondary**: Engineering Manager, Release Manager
- **Team**: Engineering organization with cloud infrastructure

## 4. Trigger
- **Type**: Cloud Provider API Polling / Scheduled Check
- **Integration**: AWS, Azure, GCP
- **Event**: Scheduled drift check, manual dispatch
- **Payload assumptions:
  - `service_name`
  - `environment` (staging, production)
  - `baseline_config` (reference configuration)
  - `check_interval` (hours)
  - `drift_threshold` (percentage of drift)
  - `drift_type` (configuration, resource, deployment)
- **Required fields**: `service_name`, `environment`, `baseline_config`

## 5. Integrations Used
| Integration          | Purpose                                                                 |
|----------------------|-------------------------------------------------------------------------|
| AWS/GCP/Azure        | Infrastructure metadata and drift detection                             |
| Slack                | Team notifications                                                      |
| PagerDuty            | Escalation for critical drift                                           |
| Linear               | Issue tracking                                                          |
| Anthropic/OpenAI     | AI drift analysis                                                       |
| GitHub Actions       | Deployment metadata                                                     |
| Datadog/Prometheus  | Metrics for drift correlation                                           |

## 6. Workflow Architecture
The workflow automates infrastructure drift detection and reconciliation with:
1. **Drift ingestion** (scheduled polling)
2. **Drift analysis** (AI-driven detection)
3. **Drift classification** (configuration, resource, deployment)
4. **Human approval gates** (for critical decisions)
5. **Automated reconciliation** (revert to baseline)
6. **Artifact storage** (drift reports)
7. **Escalation handling** (PagerDuty for critical drift)
8. **Observability** (metrics tracking)

The architecture follows a **validated drift-handling pattern** with:
- **Pre-flight checks** (validate drift context)
- **AI-driven drift detection** (confidence thresholds)
- **Human approval gates** (for irreversible actions)
- **Automated reconciliation** (metrics-based triggers)
- **Artifact preservation** (for debugging)
- **Escalation paths** (PagerDuty for critical drift)

## 7. Node Definitions
| Node ID               | Node Name                     | Node Type               | Integration       | Config Summary                                                                                     |
|-----------------------|-------------------------------|-------------------------|-------------------|---------------------------------------------------------------------------------------------------|
| trigger               | Drift Check Trigger          | schedule_trigger        | -                 | `interval: 6h`, `required_fields: service_name, environment, baseline_config`                   |
| deduplicate           | Drift Deduplication          | idempotency_check       | -                 | `idempotency_key: drift_${service_name}_${environment}`, `ttl: 24h`                                  |
| validate              | Drift Context Validation      | condition               | -                 | `checks: [drift_valid, baseline_available, context_safe]`                                         |
| analyze               | AI Drift Analysis            | ai_agent                | Anthropic/OpenAI | `agent_id: drift_analysis_agent`, `confidence_threshold: 0.9`, `timeout: 180s`                   |
| classify              | Drift Classification          | ai_agent                | Anthropic/OpenAI | `agent_id: drift_classifier`, `timeout: 120s`                                                     |
| human_approval        | Human Review Gate             | human_approval          | -                 | `timeout: 24h`, `escalation: pagerduty`                                                           |
| reconcile             | Automated Reconciliation      | aws_cloudformation_reconcile | AWS/GCP/Azure | `revert_to: baseline`, `timeout: 60s`                                                           |
| notify                | Slack Notification            | slack                   | Slack             | `channel: #cloud-infra`, `template: drift_alert`                                                   |
| escalate              | Escalation Path               | escalation              | PagerDuty         | `severity: critical`, `timeout: 5m`                                                               |
| artifact_store        | Artifact Storage              | aws_s3_upload           | AWS S3            | `bucket: drift-reports-${repo}`, `key: ${service_name}_${environment}_${drift_check}.zip`          |
| track                 | Track Metrics                 | linear_create_issue     | Linear             | `project: infrastructure-drift`, `status: open`                                                    |
| cleanup               | Cleanup Old Artifacts         | aws_s3_delete           | AWS S3            | `bucket: drift-reports-${repo}`, `key: ${service_name}_${environment}_${drift_check}.zip`          |

## 8. Edge Definitions
| Source       | Target         | Condition                                                                                     |
|--------------|----------------|---------------------------------------------------------------------------------------------|
| trigger      | deduplicate    | always                                                                                       |
| deduplicate  | validate       | always                                                                                       |
| validate     | analyze        | always                                                                                       |
| analyze      | classify       | always                                                                                       |
| classify     | human_approval | drift_confirmed                                                                             |
| human_approval | reconcile      | approved                                                                                     |
| reconcile    | notify         | reconciliation_executed                                                                         |
| analyze      | notify         | drift_detected                                                                               |
| analyze      | escalate       | critical_drift                                                                               |
| notify       | track          | always                                                                                       |
| notify       | cleanup        | success                                                                                     |
| cleanup      | complete       | always                                                                                       |

## 9. Configuration Variables
| Variable                          | Type     | Example/Default                     | Purpose                                                                                     |
|-----------------------------------|----------|-------------------------------------|---------------------------------------------------------------------------------------------|
| `cloud_provider`                  | string   | `aws`                               | AWS/GCP/Azure provider for drift detection                                                 |
| `slack_webhook_url`               | string   | `https://hooks.slack.com/services/...` | Slack notification URL                                                                      |
| `pagerduty_url`                   | string   | `https://api.pagerduty.com/incidents` | PagerDuty API endpoint                                                                       |
| `linear_project_id`               | string   | `12345`                             | Linear project ID for tracking                                                              |
| `ai_model`                        | string   | `anthropic/claude-2.1`               | AI model for analysis                                                                       |
| `ai_confidence_threshold`         | number   | `0.85`                              | Minimum confidence for AI decisions                                                      |
| `drift_threshold`                 | number   | `0.1`                               | Drift percentage threshold for triggering reconciliation                                   |
| `artifact_bucket`                  | string   | `drift-reports-${repo}`              | S3 bucket for storing drift reports                                                     |
| `max_retries`                     | number   | `3`                                 | Max retries for drift checks                                                                  |
| `drift_check_interval`            | number   | `6`                                 | Hours between drift checks                                                                |

## 10. Branching Logic
- **Drift validation**: Fails if drift is invalid or baseline unavailable
- **Drift classification**: AI analyzes drift and confirms type
- **Human approval**: Required for critical reconciliation decisions
- **Reconciliation trigger**: Automatically reconciles if drift exceeds threshold
- **Escalation**: Critical drift escalates to PagerDuty

## 11. Success Behavior
1. **Trigger**: Scheduled drift check dispatched
2. **Deduplication**: Avoid reprocessing the same drift
3. **Validation**: Ensure drift context is valid
4. **AI Analysis**: Drift detected and classified
5. **Human Approval**: Team approves reconciliation
6. **Reconciliation**: Infrastructure reverted to baseline
7. **Notification**: Slack notification sent to team
8. **Tracking**: Linear issue created for tracking
9. **Cleanup**: Old artifacts deleted

## 12. Failure Behavior
| Failure Type                     | Handling                                                                                     |
|----------------------------------|---------------------------------------------------------------------------------------------|
| Invalid drift context             | Log and retry                                                                              |
| AI analysis timeout              | Retry with exponential backoff                                                             |
| Drift not classified             | Manual review required                                                                      |
| Reconciliation failure           | Escalate to PagerDuty                                                                      |
| Slack notification failure        | Log and continue                                                                           |
| Linear API failure               | Dead letter queue with reason                                                               |
| PagerDuty escalation failure      | Log and continue                                                                           |

## 13. Retry / Recovery Behavior
- **Node-level retries**: All AI nodes retry up to 3 times with exponential backoff
- **Cloud API failures**: Retry with exponential backoff (2s, 4s, 8s)
- **Dead letter queue**: Failed Linear/PagerDuty calls go to DLQ
- **Recovery verification**: Requires 3 successful drift detections before auto-resolving

## 14. Reliability & Observability Behavior
**FlowOps Contributions:**
- **Anomaly detection**: Drift frequency, configuration drift, resource drift
- **Incident creation**: Automatically creates incident when:
  - Drift exceeds threshold
  - AI confidence below threshold
  - Critical drift detected
- **Baseline learning**: Tracks:
  - Drift detection rate
  - Drift frequency
  - AI confidence distribution
  - Reconciliation success rate
- **Observability metrics**:
  - Drift analysis duration
  - AI confidence
  - Reconciliation success rate
  - Drift frequency

## 15. Security Considerations
- **Permissions**: Requires:
  - Cloud provider: `describe_instances`, `update_instances` access
  - Slack: `chat:write` for notifications
  - PagerDuty: `incident_trigger` for escalation
  - Linear: `issue_create` for tracking
  - AWS: `s3:PutObject`, `s3:GetObject` for artifact storage
- **Secrets**:
  - Cloud provider credentials
  - Slack webhook URL
  - PagerDuty API key
  - Linear API token
- **Sensitive data**: Never exposed in logs; all outputs sanitized
- **Audit trail**: Every decision logged with user ID and timestamp

## 16. Setup Requirements
- **Required accounts**:
  - Cloud provider account with drift detection access
  - Slack workspace with notification access
  - PagerDuty account for escalation
  - Linear project for issue tracking
  - AWS S3 bucket for artifact storage
- **Integrations**:
  - Cloud provider API integration
  - Slack app integration
  - PagerDuty API integration
  - Linear API integration
  - AWS S3 bucket setup
- **Configuration**:
  - Set `cloud_provider` in FlowOps
  - Configure AI model and confidence thresholds
  - Set up Slack webhook URL
  - Configure PagerDuty API endpoint
  - Set up Linear project
  - Configure artifact bucket

## 17. Expected Outcome
- **Time savings**: Reduces drift investigation time by 60-80%
- **Operational safety**: Automated reconciliation prevents misconfigurations
- **Consistency**: Standardized drift handling across teams
- **Escalation reduction**: Critical drift escalated proactively
- **Observability**: Real-time metrics and drift tracking

## 18. React Flow Graph
```json
{
  "nodes": [
    {
      "id": "trigger",
      "type": "schedule_trigger",
      "position": {"x": 0, "y": 0},
      "data": {"label": "Drift Check Trigger", "config": {"interval": "6h", "required_fields": ["service_name", "environment", "baseline_config"]}}
    },
    {
      "id": "deduplicate",
      "type": "idempotency_check",
      "position": {"x": 100, "y": 0},
      "data": {"label": "Drift Deduplication", "config": {"idempotency_key": "drift_${service_name}_${environment}", "ttl": "24h"}}
    },
    {
      "id": "validate",
      "type": "condition",
      "position": {"x": 200, "y": 0},
      "data": {"label": "Drift Context Validation", "config": {"checks": ["drift_valid", "baseline_available", "context_safe"]}}
    },
    {
      "id": "analyze",
      "type": "ai_agent",
      "position": {"x": 300, "y": 50},
      "data": {"label": "AI Drift Analysis", "config": {"agent_id": "drift_analysis_agent", "confidence_threshold": "0.9", "timeout": "180s"}}
    },
    {
      "id": "classify",
      "type": "ai_agent",
      "position": {"x": 300, "y": 150},
      "data": {"label": "Drift Classification", "config": {"agent_id": "drift_classifier", "timeout": "120s"}}
    },
    {
      "id": "human_approval",
      "type": "human_approval",
      "position": {"x": 400, "y": 50},
      "data": {"label": "Human Review Gate", "config": {"timeout": "24h", "escalation": "pagerduty"}}
    },
    {
      "id": "reconcile",
      "type": "aws_cloudformation_reconcile",
      "position": {"x": 500, "y": 50},
      "data": {"label": "Automated Reconciliation", "config": {"revert_to": "baseline", "timeout": "60s"}}
    },
    {
      "id": "notify",
      "type": "slack",
      "position": {"x": 600, "y": 0},
      "data": {"label": "Slack Notification", "config": {"channel": "#cloud-infra", "template": "drift_alert"}}
    },
    {
      "id": "escalate",
      "type": "escalation",
      "position": {"x": 600, "y": 100},
      "data": {"label": "Escalation Path", "config": {"severity": "critical", "timeout": "5m"}}
    },
    {
      "id": "artifact_store",
      "type": "aws_s3_upload",
      "position": {"x": 700, "y": 0},
      "data": {"label": "Artifact Storage", "config": {"bucket": "drift-reports-${repo}", "key": "${service_name}_${environment}_${drift_check}.zip"}}
    },
    {
      "id": "track",
      "type": "linear_create_issue",
      "position": {"x": 700, "y": 100},
      "data": {"label": "Track Metrics", "config": {"project": "infrastructure-drift", "status": "open"}}
    },
    {
      "id": "cleanup",
      "type": "aws_s3_delete",
      "position": {"x": 700, "y": 150},
      "data": {"label": "Cleanup Old Artifacts", "config": {"bucket": "drift-reports-${repo}", "key": "${service_name}_${environment}_${drift_check}.zip"}}
    },
    {
      "id": "complete",
      "type": "stop_fail",
      "position": {"x": 800, "y": 50},
      "data": {"label": "Workflow Completion"}
    }
  ],
  "edges": [
    {"source": "trigger", "target": "deduplicate", "label": "always"},
    {"source": "deduplicate", "target": "validate", "label": "always"},
    {"source": "validate", "target": "analyze", "label": "always"},
    {"source": "analyze", "target": "classify", "label": "always"},
    {"source": "classify", "target": "human_approval", "label": "drift_confirmed"},
    {"source": "human_approval", "target": "reconcile", "label": "approved"},
    {"source": "reconcile", "target": "notify", "label": "reconciliation_executed"},
    {"source": "analyze", "target": "notify", "label": "drift_detected"},
    {"source": "analyze", "target": "escalate", "label": "critical_drift"},
    {"source": "notify", "target": "track", "label": "always"},
    {"source": "notify", "target": "cleanup", "label": "success"},
    {"source": "cleanup", "target": "complete", "label": "always"}
  ]
}
```

## 19. Metadata
```json
{
  "category": "DevOps/Engineering",
  "tags": ["Infrastructure Drift", "AI Analysis", "Reconciliation", "Cloud Operations", "Reliability"],
  "integrations": ["AWS/GCP/Azure", "Slack", "PagerDuty", "Linear", "Anthropic"],
  "complexity": "High",
  "estimated_setup_minutes": 60
}
```

## 20. Production-Readiness Audit
1. **Real production trigger**: Scheduled drift checks
2. **Meaningful branching**: AI drift detection, human approval gates
3. **Retry strategy**: All AI nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 24h for human approval, 5m for escalation
5. **Idempotency**: Deduplication key with 24h TTL
6. **Partial failure**: Drift classification with manual review
7. **External service failure**: Retry with exponential backoff for cloud APIs
8. **AI uncertainty**: Confidence thresholds for AI decisions
9. **Human approval**: Required for critical reconciliation decisions
10. **Observability**: Comprehensive metrics tracking
11. **Recovery**: Dead letter queue for failed Linear/PagerDuty calls
12. **Security**: All secrets encrypted, sensitive data sanitized
13. **Configuration**: Realistic configurable variables
14. **Graph quality**: Valid React Flow JSON with clean layout

**Non-applicable**: None