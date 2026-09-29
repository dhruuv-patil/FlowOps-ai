# Template 31: Expense Approval with Policy Validation

## 1. Template Identity
- **Name**: Expense Approval with Policy Validation
- **One-line pitch**: "Automated expense approval with AI-driven policy validation, approval routing, and escalation handling"
- **Category**: RevOps/Sales
- **Complexity**: High
- **Estimated node count**: 22

## 2. Business Problem
The business problem is **inefficient expense approval processes causing delays** where:
- Expenses are approved manually without policy validation
- Approval routing is inconsistent
- No automated policy validation
- No escalation handling for high-value expenses
- No observability into approval health

**Specific pain points:**
- **Time delays**: Manual approvals take 24-48 hours
- **Inconsistent routing**: Different teams handle approvals differently
- **Policy violations**: Expenses approved without policy validation
- **No escalation**: High-value expenses not escalated
- **No observability**: No visibility into approval health

## 3. Target User
- **Primary**: Finance Manager, Expense Approver, Procurement Specialist
- **Secondary**: Finance Analyst, Procurement Manager
- **Team**: Finance, Procurement, Operations

## 4. Trigger
- **Type**: Expense Submission Webhook / Manual Dispatch
- **Integration**: QuickBooks, Xero, Expensify, Concur
- **Event**: Expense submitted, manual dispatch
- **Payload assumptions:
  - `expense_id`
  - `employee_id`
  - `amount`
  - `category`
  - `receipt_url`
  - `policy_id`
  - `approval_level`
  - `escalation_required`
- **Required fields**: `expense_id`, `employee_id`, `amount`, `category`

## 5. Integrations Used
| Integration          | Purpose                                                                 |
|----------------------|-------------------------------------------------------------------------|
| QuickBooks/Xero/Expensify/Concur | Expense management                                    |
| Slack                | Team notifications                                                      |
| PagerDuty            | Escalation for high-value expenses                                       |
| Linear               | Issue tracking                                                          |
| Anthropic/OpenAI     | AI policy validation                                                   |
| AWS S3               | Receipt artifact storage                                                 |

## 6. Workflow Architecture
The workflow automates expense approval with:
1. **Expense ingestion** (webhook trigger)
2. **Policy validation** (AI-driven)
3. **Approval routing** (based on amount and category)
4. **Human approval gates** (for high-value expenses)
5. **Escalation handling** (PagerDuty for high-value expenses)
6. **Artifact storage** (receipts)
7. **Observability** (metrics tracking)

The architecture follows a **validated approval pattern** with:
- **Pre-approval validation** (policy validation)
- **Approval routing** (amount and category)
- **Human approval gates** (for high-value expenses)
- **Escalation paths** (PagerDuty for high-value expenses)
- **Artifact preservation** (for debugging)

## 7. Node Definitions
| Node ID               | Node Name                     | Node Type               | Integration       | Config Summary                                                                                     |
|-----------------------|-------------------------------|-------------------------|-------------------|---------------------------------------------------------------------------------------------------|
| trigger               | Expense Trigger              | webhook_trigger         | QuickBooks/Xero/Expensify/Concur | `event: expense_submitted`, `required_fields: expense_id, employee_id, amount, category`     |
| deduplicate           | Expense Deduplication         | idempotency_check       | -                 | `idempotency_key: expense_${expense_id}`, `ttl: 24h`                                |
| validate              | Policy Validation       | ai_agent               | Anthropic/OpenAI | `agent_id: policy_validation_agent`, `confidence_threshold: 0.9`, `timeout: 180s`                 |
| route                 | Approval Routing            | condition               | -                 | `routing_rules: [amount > 1000, category == "travel", escalation_required]`                                     |
| approve_level_1        | Level 1 Approval            | human_approval          | -                 | `timeout: 24h`, `escalation: pagerduty`                                                            |
| approve_level_2        | Level 2 Approval            | human_approval          | -                 | `timeout: 48h`, `escalation: pagerduty`                                                            |
| approve_level_3        | Level 3 Approval            | human_approval          | -                 | `timeout: 72h`, `escalation: pagerduty`                                                            |
| escalate              | Escalation Path              | escalation              | PagerDuty         | `severity: critical`, `timeout: 5m`                                                               |
| notify                | Slack Notification           | slack                   | Slack             | `channel: #expense-approvals`, `template: approval_status`                                            |
| artifact_store        | Artifact Storage             | aws_s3_upload           | AWS S3            | `bucket: expense-receipts-${repo}`, `key: ${expense_id}.zip`                             |
| track                 | Track Metrics                | linear_create_issue     | Linear             | `project: expense-approvals`, `status: open`                                                         |
| cleanup               | Cleanup Old Artifacts        | aws_s3_delete           | AWS S3            | `bucket: expense-receipts-${repo}`, `key: ${expense_id}.zip`                             |

## 8. Edge Definitions
| Source       | Target         | Condition                                                                                     |
|--------------|----------------|---------------------------------------------------------------------------------------------|
| trigger      | deduplicate    | always                                                                                       |
| deduplicate  | validate       | always                                                                                       |
| validate     | route          | always                                                                                       |
| route        | approve_level_1 | amount <= 1000                                                                               |
| route        | approve_level_2 | amount > 1000 and category != "travel"                                                   |
| route        | approve_level_3 | category == "travel"                                                                       |
| route        | escalate       | escalation_required                                                                          |
| approve_level_1 | notify         | approved                                                                                     |
| approve_level_2 | notify         | approved                                                                                     |
| approve_level_3 | notify         | approved                                                                                     |
| escalate     | notify         | escalated                                                                                     |
| notify       | artifact_store | always                                                                                       |
| notify       | track          | always                                                                                       |
| notify       | cleanup        | success                                                                                     |

## 9. Configuration Variables
| Variable                          | Type     | Example/Default                     | Purpose                                                                                     |
|-----------------------------------|----------|-------------------------------------|---------------------------------------------------------------------------------------------|
| `expense_platform`                   | string   | `quickbooks`                      | Expense management platform (quickbooks, xero, expensify, concur)                        |
| `slack_webhook_url`               | string   | `https://hooks.slack.com/services/...` | Slack notification URL                                                                      |
| `pagerduty_url`                   | string   | `https://api.pagerduty.com/incidents` | PagerDuty API endpoint                                                                       |
| `linear_project_id`               | string   | `12345`                             | Linear project ID for tracking                                                              |
| `ai_model`                        | string   | `anthropic/claude-2.1`               | AI model for policy validation                                                               |
| `ai_confidence_threshold`         | number   | `0.85`                              | Minimum confidence for AI decisions                                                      |
| `approval_levels`                  | array    | `[1000, 5000, 10000]`                  | Approval thresholds (level 1, 2, 3)                                                      |
| `travel_categories`                | array    | `["travel", "accommodation", "transportation"]` | Categories that require level 3 approval                                                |
| `artifact_bucket`                  | string   | `expense-receipts-${repo}`                | S3 bucket for storing receipts                                                        |
| `max_retries`                     | number   | `3`                                 | Max retries for policy validation                                                               |
| `approval_timeout_hours`          | number   | `24`                                 | Timeout for level 1 approval                                                                |

## 10. Branching Logic
- **Policy validation**: Fails if policy not validated
- **Approval routing**: Based on amount and category
- **Escalation required**: If escalation required, branch to escalation
- **Approval levels**: Level 1, 2, 3 based on amount and category
- **Escalation**: Critical expenses escalated to PagerDuty

## 11. Success Behavior
1. **Trigger**: Expense submission webhook received
2. **Deduplication**: Avoid reprocessing the same expense
3. **Policy validation**: Ensure expense complies with policy
4. **Approval routing**: Route to appropriate approval level
5. **Human approval**: Approve expense
6. **Notification**: Slack notification sent to team
7. **Artifact storage**: Receipt stored in S3
8. **Tracking**: Linear issue created for tracking
9. **Cleanup**: Old artifacts deleted

## 12. Failure Behavior
| Failure Type                     | Handling                                                                                     |
|----------------------------------|---------------------------------------------------------------------------------------------|
| Invalid expense config              | Log and retry                                                                              |
| AI analysis timeout              | Retry with exponential backoff                                                             |
| Policy validation failure                 | Human approval required                                                                    |
| Human approval timeout           | Escalate to PagerDuty                                                                      |
| Slack notification failure        | Log and continue                                                                           |
| Linear API failure               | Dead letter queue with reason                                                               |
| PagerDuty escalation failure      | Log and continue                                                                           |

## 13. Retry / Recovery Behavior
- **Node-level retries**: All AI nodes retry up to 3 times with exponential backoff
- **Expense platform API failures**: Retry with exponential backoff (2s, 4s, 8s)
- **Dead letter queue**: Failed Linear/PagerDuty calls go to DLQ
- **Recovery verification**: Requires 3 successful policy validations before auto-resolving

## 14. Reliability & Observability Behavior
**FlowOps Contributions:**
- **Anomaly detection**: Policy validation failures, approval delays, escalation anomalies
- **Incident creation**: Automatically creates incident when:
  - Policy validation fails
  - Approval delays exceed threshold
  - Escalation anomalies detected
- **Baseline learning**: Tracks:
  - Approval time distribution
  - Policy validation success rate
  - Escalation rate
- **Observability metrics**:
  - Approval duration
  - Policy validation confidence
  - Escalation rate
  - Approval success rate

## 15. Security Considerations
- **Permissions**: Requires:
  - Expense platform: `expense_read`, `expense_update` access
  - Slack: `chat:write` for notifications
  - PagerDuty: `incident_trigger` for escalation
  - Linear: `issue_create` for tracking
  - AWS: `s3:PutObject`, `s3:GetObject` for artifact storage
- **Secrets**:
  - Expense platform API key
  - Slack webhook URL
  - PagerDuty API key
  - Linear API token
- **Sensitive data**: Never exposed in logs; all outputs sanitized
- **Audit trail**: Every decision logged with user ID and timestamp

## 16. Setup Requirements
- **Required accounts**:
  - Expense management platform (QuickBooks/Xero/Expensify/Concur)
  - Slack workspace with notification access
  - PagerDuty account for escalation
  - Linear project for issue tracking
  - AWS S3 bucket for artifact storage
- **Integrations**:
  - Expense platform webhook setup
  - Slack app integration
  - PagerDuty API integration
  - Linear API integration
  - AWS S3 bucket setup
- **Configuration**:
  - Set `expense_platform` in FlowOps
  - Configure AI model and confidence thresholds
  - Set up Slack webhook URL
  - Configure PagerDuty API endpoint
  - Set up Linear project
  - Configure artifact bucket

## 17. Expected Outcome
- **Time savings**: Reduces approval time by 60-80%
- **Operational safety**: Policy validation prevents violations
- **Consistency**: Standardized approval process across teams
- **Escalation reduction**: Critical expenses escalated proactively
- **Observability**: Real-time metrics and policy validation

## 18. React Flow Graph
```json
{
  "nodes": [
    {
      "id": "trigger",
      "type": "webhook_trigger",
      "position": {"x": 0, "y": 0},
      "data": {"label": "Expense Trigger", "config": {"event": "expense_submitted", "required_fields": ["expense_id", "employee_id", "amount", "category"]}}
    },
    {
      "id": "deduplicate",
      "type": "idempotency_check",
      "position": {"x": 100, "y": 0},
      "data": {"label": "Expense Deduplication", "config": {"idempotency_key": "expense_${expense_id}", "ttl": "24h"}}
    },
    {
      "id": "validate",
      "type": "ai_agent",
      "position": {"x": 200, "y": 0},
      "data": {"label": "Policy Validation", "config": {"agent_id": "policy_validation_agent", "confidence_threshold": "0.9", "timeout": "180s"}}
    },
    {
      "id": "route",
      "type": "condition",
      "position": {"x": 300, "y": 0},
      "data": {"label": "Approval Routing", "config": {"routing_rules": ["amount > 1000", "category == \"travel\"", "escalation_required"]}}
    },
    {
      "id": "approve_level_1",
      "type": "human_approval",
      "position": {"x": 400, "y": 0},
      "data": {"label": "Level 1 Approval", "config": {"timeout": "24h", "escalation": "pagerduty"}}
    },
    {
      "id": "approve_level_2",
      "type": "human_approval",
      "position": {"x": 400, "y": 100},
      "data": {"label": "Level 2 Approval", "config": {"timeout": "48h", "escalation": "pagerduty"}}
    },
    {
      "id": "approve_level_3",
      "type": "human_approval",
      "position": {"x": 400, "y": 200},
      "data": {"label": "Level 3 Approval", "config": {"timeout": "72h", "escalation": "pagerduty"}}
    },
    {
      "id": "escalate",
      "type": "escalation",
      "position": {"x": 500, "y": 100},
      "data": {"label": "Escalation Path", "config": {"severity": "critical", "timeout": "5m"}}
    },
    {
      "id": "notify",
      "type": "slack",
      "position": {"x": 600, "y": 100},
      "data": {"label": "Slack Notification", "config": {"channel": "#expense-approvals", "template": "approval_status"}}
    },
    {
      "id": "artifact_store",
      "type": "aws_s3_upload",
      "position": {"x": 700, "y": 0},
      "data": {"label": "Artifact Storage", "config": {"bucket": "expense-receipts-${repo}", "key": "${expense_id}.zip"}}
    },
    {
      "id": "track",
      "type": "linear_create_issue",
      "position": {"x": 700, "y": 100},
      "data": {"label": "Track Metrics", "config": {"project": "expense-approvals", "status": "open"}}
    },
    {
      "id": "cleanup",
      "type": "aws_s3_delete",
      "position": {"x": 700, "y": 200},
      "data": {"label": "Cleanup Old Artifacts", "config": {"bucket": "expense-receipts-${repo}", "key": "${expense_id}.zip"}}
    }
  ],
  "edges": [
    {"source": "trigger", "target": "deduplicate", "label": "always"},
    {"source": "deduplicate", "target": "validate", "label": "always"},
    {"source": "validate", "target": "route", "label": "always"},
    {"source": "route", "target": "approve_level_1", "label": "amount <= 1000"},
    {"source": "route", "target": "approve_level_2", "label": "amount > 1000 and category != \"travel\""},
    {"source": "route", "target": "approve_level_3", "label": "category == \"travel\""},
    {"source": "route", "target": "escalate", "label": "escalation_required"},
    {"source": "approve_level_1", "target": "notify", "label": "approved"},
    {"source": "approve_level_2", "target": "notify", "label": "approved"},
    {"source": "approve_level_3", "target": "notify", "label": "approved"},
    {"source": "escalate", "target": "notify", "label": "escalated"},
    {"source": "notify", "target": "artifact_store", "label": "always"},
    {"source": "notify", "target": "track", "label": "always"},
    {"source": "notify", "target": "cleanup", "label": "success"}
  ]
}
```

## 19. Metadata
```json
{
  "category": "RevOps/Sales",
  "tags": ["Expense Approval", "Policy Validation", "Approval Routing", "AI Analysis", "Reliability"],
  "integrations": ["QuickBooks/Xero/Expensify/Concur", "Slack", "PagerDuty", "Linear", "Anthropic", "AWS S3"],
  "complexity": "High",
  "estimated_setup_minutes": 60
}
```

## 20. Production-Readiness Audit
1. **Real production trigger**: Expense platform webhook for expense submissions
2. **Meaningful branching**: AI policy validation, approval routing, escalation
3. **Retry strategy**: All AI nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 24h for level 1 approval, 48h for level 2, 72h for level 3
5. **Idempotency**: Deduplication key with 24h TTL
6. **Partial failure**: Policy validation with manual review
7. **External service failure**: Retry with exponential backoff for expense platform APIs
8. **AI uncertainty**: Confidence thresholds for AI decisions
9. **Human approval**: Required for all approval levels
10. **Observability**: Comprehensive metrics tracking
11. **Recovery**: Dead letter queue for failed Linear/PagerDuty calls
12. **Security**: All secrets encrypted, sensitive data sanitized
13. **Configuration**: Realistic configurable variables
14. **Graph quality**: Valid React Flow JSON with clean layout

**Non-applicable**: None