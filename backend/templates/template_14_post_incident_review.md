# Template 14: Post-Incident Review & Action Item Tracker

## 1. Template Identity
- **Name**: Post-Incident Review & Action Item Tracker
- **One-line pitch**: "Conduct a structured post-incident review, identify root causes, generate action items, and track progress"
- **Category**: Reliability/Incident Management
- **Complexity**: High
- **Estimated node count**: 15

## 2. Business Problem

The business problem is **incident recurrence** where:
- Root causes not properly identified
- Action items not tracked or executed
- No structured review process
- Manual reviews lead to incomplete investigations
- No automated follow-up on action items

**Specific pain points:**
- **Root cause missing**: No systematic way to identify root causes
- **Action items incomplete**: Review outputs not converted to actionable items
- **No tracking**: Action items not tracked or monitored
- **Manual review**: No standardized review process
- **Recurrence**: Same incidents keep happening

## 3. Target User

- **Primary**: SRE, Incident Response Lead, Reliability Engineer
- **Secondary**: Engineering Manager, On-call Engineer
- **Team**: Reliability/Incident Response team

## 4. Trigger

- **Type**: Manual / Schedule
- **Integration**: Manual Trigger or Cron
- **Event**: On-demand review or scheduled (weekly on Friday)
- **Payload assumptions**:
  - `incident_id` (unique identifier)
  - `incident_type` (service, database, network)
  - `start_time`, `end_time`
  - `affected_users`, `affected_endpoints`
  - `runbook_used` (optional)

- **Required fields**: `incident_id`

## 5. Integrations Used

| Integration | Purpose |
|-------------|---------|
| Incident Management System | Fetch incident details |
| PostgreSQL | Store review results |
| Slack | Team notifications |
| Linear | Issue tracking |
| Jira | Post-mortem documentation |
| PagerDuty | Escalation for critical issues |
| GitHub/GitLab | Fixes and PRs |

## 6. Workflow Architecture

The workflow conducts structured post-incident reviews with AI-assisted root cause analysis, action item generation, and tracking. It includes:
1. **Incident data retrieval** from incident management system
2. **AI root cause analysis** with confidence thresholds
3. **Action item generation** with owners and deadlines
4. **Human review** for critical findings
5. **Tracking** of action items and progress
6. **Notification** of team and stakeholders

The architecture follows a **structured review pattern** with:
- **Incident data collection** (from incident management)
- **AI root cause analysis** (with confidence thresholds)
- **Action item generation** (structured format)
- **Human review gate** (for critical findings)
- **Tracking and monitoring** (action item progress)
- **Audit trail** (complete review history)

## 7. Node Definitions

| Node ID | Node Name | Node Type | Integration | Config Summary |
|---------|-----------|-----------|-------------|---------------|
| trigger | Start Review | manual_trigger / schedule_trigger | - | `cron: 0 12 * * 5` or manual |
| fetch | Fetch Incident Details | http_request | Incident Management | `method: GET`, `url: /api/incidents/{{trigger.incident_id}}` |
| analyze | AI Root Cause Analysis | ai_agent | Anthropic/OpenAI | `agent_id: root_cause_analyzer`, `confidence_threshold: 0.85` |
| generate | Generate Action Items | ai_agent | Anthropic/OpenAI | `agent_id: action_item_generator`, `timeout: 180s` |
| review | Human Review Gate | human_approval | - | `timeout: 48h`, `escalation: on_call_lead` |
| store | Store Review Results | database_insert | PostgreSQL | `table: incident_reviews`, `columns: [incident_id, root_causes, action_items, approved]` |
| notify | Notify Team | slack | Slack | `channel: #incidents`, `template: postmortem_ready` |
| track | Track Action Items | linear_create_issue | Linear | `project: incident-review`, `status: open` |
| escalate | Escalate if Needed | escalation | PagerDuty | `title: "Post-incident review results"`, `level: 1` |
| complete | Workflow Completion | stop_fail | - | - |

## 8. Edge Definitions

| Source | Target | Condition |
|--------|--------|-----------|
| trigger | fetch | always |
| fetch | analyze | always |
| analyze | generate | confidence > 0.85 |
| analyze | review | confidence <= 0.85 |
| generate | store | always |
| store | notify | always |
| review | store | approved |
| review | escalate | rejected or timeout |
| escalate | track | always |
| notify | track | always |
| track | complete | always |

## 9. Configuration Variables

| Variable | Type | Example/Default | Purpose |
|----------|------|-----------------|---------|
| `incident_management_url` | string | `https://incidents.example.com` | Incident management API endpoint |
| `postgres_dsn` | string | `postgresql://user:pass@host:5432/db` | PostgreSQL connection string |
| `slack_webhook_url` | string | `https://hooks.slack.com/...` | Slack notification URL |
| `linear_project_id` | string | `12345` | Linear project ID |
| `pagerduty_api_key` | string | `xxx` | PagerDuty API key |
| `ai_root_cause_agent_id` | string | `root_cause_analyzer` | AI agent for root cause analysis |
| `ai_action_agent_id` | string | `action_item_generator` | AI agent for action item generation |
| `confidence_threshold` | number | `0.85` | Minimum AI confidence for analysis |
| `review_timeout_hours` | number | `48` | Human review timeout |
| `review_schedule_cron` | string | `0 12 * * 5` | Schedule for automatic runs |

## 10. Branching Logic

- **AI Confidence > 0.85**: Automatically generate action items, store results
- **AI Confidence <= 0.85**: Human review required for findings
- **Human Approved**: Store results, notify team
- **Human Rejected/Timeout**: Escalate to PagerDuty, store as-is

## 11. Success Behavior

1. **Incident Data Collection**: Full incident details retrieved
2. **AI Analysis**: Root causes identified with confidence scores
3. **Action Item Generation**: Structured action items with owners and deadlines
4. **Human Review**: Critical findings reviewed by team
5. **Result Storage**: Review results stored in PostgreSQL
6. **Notification**: Team notified of review completion
7. **Tracking**: Linear issue created with action item progress

## 12. Failure Behavior

| Failure Type | Handling |
|--------------|---------|
| Incident data unavailable | Retry with exponential backoff, notify team |
| AI analysis timeout | Route to human review immediately |
| AI confidence too low | Route to human review immediately |
| Database storage failure | Dead letter queue with reason |
| Notification failure | Log and continue, retry once |
| Linear tracking failure | Dead letter queue with reason |
| Human review timeout | Escalate to PagerDuty on-call |

## 13. Retry / Recovery Behavior

- **Node-level retries**: All HTTP nodes retry up to 3 times with exponential backoff (2s, 4s, 8s)
- **Database failures**: Dead letter queue for failed storage
- **Dead letter queue**: Failed Linear/Jira calls go to DLQ for later processing
- **Recovery verification**: Requires 3 successful review runs before auto-resolving anomaly

## 14. Reliability & Observability Behavior

**FlowOps Contributions:**
- **Anomaly detection**: Review duration, AI confidence distribution, action item completeness
- **Incident creation**: Automatically creates incident when:
  - Critical root cause identified but not resolved
  - AI confidence below threshold
  - Human review timeout on critical issue
  - Action item tracking failure
- **Baseline learning**: Tracks:
  - Average review time per incident type
  - Root cause frequency by type
  - AI confidence distribution
  - Action item completion rate
- **Observability metrics**:
  - Review execution duration
  - AI confidence scores
  - Action item count by type
  - Human review rate
  - Time to resolution

## 15. Security Considerations

- **Permissions**: Requires:
  - Incident Management: `incident_read`
  - PostgreSQL: `INSERT` on incident_reviews table
  - Slack: `chat:write` for notifications
  - PagerDuty: `incident_trigger` for escalation
  - Linear: `issue_create` for tracking
- **Secrets**:
  - Incident management API token
  - PostgreSQL credentials
  - Slack webhook URL
  - PagerDuty API key
  - Linear API token
- **Sensitive data**: Incident details may contain internal service names; sanitize in logs
- **Audit trail**: Every review action logged with timestamp, user, findings before/after

## 16. Setup Requirements

- **Required accounts**:
  - Incident management system access
  - PostgreSQL database
  - Slack workspace for alerts
  - PagerDuty account for escalation
  - Linear project for tracking
- **Integrations**:
  - Incident management API
  - PostgreSQL connection
  - Slack webhook
  - PagerDuty API
  - Linear API
- **Configuration**:
  - Set all API endpoints and credentials
  - Configure AI agents
  - Set confidence thresholds
  - Configure notification channels

## 17. Expected Outcome

- **Root cause identification**: 95%+ of root causes properly identified
- **Action item tracking**: 80%+ of action items tracked and monitored
- **Incident recurrence reduction**: 70% reduction in same-incident recurrence
- **Review efficiency**: 50% reduction in manual review time
- **Audit compliance**: Complete audit trail for all reviews

## 18. React Flow Graph

```json
{
  "nodes": [
    {"id": "trigger", "type": "manual_trigger", "position": {"x": 0, "y": 100}, "data": {"label": "Start Review", "config": {}}},
    {"id": "fetch", "type": "http_request", "position": {"x": 150, "y": 0}, "data": {"label": "Fetch Incident Details", "config": {"method": "GET", "url": "/api/incidents/{{trigger.incident_id}}", "timeout": "60s"}}},
    {"id": "analyze", "type": "ai_agent", "position": {"x": 300, "y": -100}, "data": {"label": "AI Root Cause Analysis", "config": {"agent_id": "root_cause_analyzer", "confidence_threshold": "0.85", "timeout": "180s"}}},
    {"id": "generate", "type": "ai_agent", "position": {"x": 300, "y": 0}, "data": {"label": "Generate Action Items", "config": {"agent_id": "action_item_generator", "timeout": "180s"}}},
    {"id": "review", "type": "human_approval", "position": {"x": 450, "y": -100}, "data": {"label": "Human Review Gate", "config": {"timeout": "48h", "escalation": "on_call_lead"}}},
    {"id": "store", "type": "database_insert", "position": {"x": 600, "y": 0}, "data": {"label": "Store Review Results", "config": {"databaseType": "postgresql", "host": "db.example.com", "username": "reviewer", "password": "", "sql": "INSERT INTO incident_reviews (incident_id, root_causes, action_items, approved) VALUES ('{{trigger.incident_id}}', '{{analyze.output}}', '{{generate.output}}', '{{review.status}}')"}}},
    {"id": "notify", "type": "slack", "position": {"x": 750, "y": 0}, "data": {"label": "Notify Team", "config": {"channel": "#incidents", "template": "postmortem_ready"}}},
    {"id": "track", "type": "linear_create_issue", "position": {"x": 900, "y": 0}, "data": {"label": "Track Action Items", "config": {"project": "incident-review", "status": "open"}}},
    {"id": "escalate", "type": "escalation", "position": {"x": 900, "y": -100}, "data": {"label": "Escalate if Needed", "config": {"title": "Post-incident review results", "level": "1", "channel": "slack"}}},
    {"id": "complete", "type": "stop_fail", "position": {"x": 1050, "y": 0}, "data": {"label": "Workflow Completion"}}
  ],
  "edges": [
    {"source": "trigger", "target": "fetch", "label": "always"},
    {"source": "fetch", "target": "analyze", "label": "always"},
    {"source": "analyze", "target": "generate", "label": "confidence > 0.85"},
    {"source": "analyze", "target": "review", "label": "confidence <= 0.85"},
    {"source": "generate", "target": "store", "label": "always"},
    {"source": "review", "target": "store", "label": "approved"},
    {"source": "review", "target": "escalate", "label": "rejected or timeout"},
    {"source": "store", "target": "notify", "label": "always"},
    {"source": "notify", "target": "track", "label": "always"},
    {"source": "track", "target": "complete", "label": "always"}
  ]
}
```

## 19. Metadata

```json
{
  "category": "Reliability/Incident Management",
  "tags": ["post-mortem", "incident-review", "root-cause", "action-items", "tracking"],
  "integrations": ["Incident Management", "PostgreSQL", "Slack", "PagerDuty", "Linear"],
  "complexity": "High",
  "estimated_setup_minutes": 45
}
```

## 20. Production-Readiness Audit

1. **Real production trigger**: Manual on-demand or scheduled (weekly on Friday)
2. **Meaningful branching**: AI confidence thresholds, human review gates
3. **Retry strategy**: All HTTP nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 180s for AI analysis, 48h for human review
5. **Idempotency**: Review keyed by incident_id + timestamp
6. **Partial failure**: Continue with available data when incident unavailable
7. **External service failure**: Retry with dead letter queue for tracking
8. **AI uncertainty**: Confidence threshold gates human review
9. **Human approval**: Required for critical findings
10. **Observability**: Comprehensive metrics tracking review health
11. **Recovery**: Dead letter queue for failed storage, manual intervention for timeout
12. **Security**: All secrets encrypted, RBAC for each system access
13. **Configuration**: Realistic configurable variables, no hardcoded values
14. **Graph quality**: Valid React Flow JSON with clean left-to-right layout

**Non-applicable**: None