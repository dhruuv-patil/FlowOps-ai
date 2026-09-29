# Template 4: AI-Driven Security Triage Agent

## 1. Template Identity
- **Name**: AI-Driven Security Triage Agent
- **One-line pitch**: "Automated security alert triage with AI analysis and human escalation for critical threats"
- **Category**: AI Operations
- **Complexity**: High
- **Estimated node count**: 17

## 2. Business Problem

The business problem is **security alert fatigue** where:
- Security teams overwhelmed by volume of alerts
- Critical threats buried in noise
- Inconsistent triage quality across analysts
- Slow response times for real incidents
- No systematic alert enrichment

**Specific pain points:**
- **Alert volume**: 1000+ alerts/day for mid-size org
- **False positives**: 80%+ of alerts are false positives
- **Burnout**: Analysts miss real threats due to fatigue
- **Inconsistent triage**: Different analysts, different standards
- **Slow MTTR**: Mean time to respond too high

## 3. Target User

- **Primary**: Security Operations Manager, SOC Lead
- **Secondary**: Security Analyst, CISO, Incident Response Lead
- **Team**: Security operations (5+ analysts)

## 4. Trigger

- **Type**: Webhook / Schedule
- **Integration**: Sentry / PagerDuty / GitHub Security / Custom SIEM
- **Event**: Security alert created, vulnerability detected, anomalous activity
- **Payload assumptions**:
  - `alert_id`, `alert_type`
  - `severity` (critical, high, medium, low)
  - `source` (sentry, pagerduty, github, custom)
  - `description`, `details`
  - `affected_systems`, `affected_users`
  - `timestamp`, `tags`
  - `raw_payload`

- **Required fields**: `alert_id`, `severity`, `source`

## 5. Integrations Used

| Integration | Purpose |
|-------------|---------|
| Sentry | Error/security alerts |
| PagerDuty | Incident escalation |
| GitHub Security | Code scanning alerts |
| Anthropic/OpenAI | AI triage analysis |
| Slack | Team notifications |
| Jira | Ticket tracking |
| Linear | Issue tracking |

## 6. Workflow Architecture

The workflow uses an AI agent to triage security alerts with automated enrichment and human escalation for critical threats. It includes:
1. **Alert ingestion** (multiple sources)
2. **Deduplication** (correlation)
3. **AI triage analysis** (risk assessment, context)
4. **Severity recalibration** (AI-adjusted severity)
5. **Automated enrichment** (threat intel, asset context)
6. **Routing** (auto-close, auto-assign, human review)
7. **Human escalation** for critical/high
8. **Incident creation** for validated threats
9. **Feedback loop** for model improvement

The architecture follows a **multi-source triage pattern** with:
- **Unified ingestion** (normalize sources)
- **AI triage** (risk + context)
- **Enrichment** (threat intel + asset data)
- **Tiered routing** (auto vs human)
- **Escalation path** for critical
- **Continuous learning** from analyst feedback

## 7. Node Definitions

| Node ID | Node Name | Node Type | Integration | Config Summary |
|---------|-----------|-----------|--------------|---------------|
| trigger | Security Alert Ingestion | webhook_trigger | Sentry/PagerDuty/GitHub | `event: alert_created`, `required_fields: alert_id,severity,source` |
| deduplicate | Alert Deduplication | idempotency_check | - | `idempotency_key: alert_${alert_id}`, `ttl: 24h`, `correlation_window: 1h` |
| normalize | Alert Normalization | transform | - | `schema: unified_alert_schema` |
| triage | AI Triage Analysis | ai_agent | Anthropic/OpenAI | `agent_id: security_triage_agent`, `confidence_threshold: 0.9`, `timeout: 180s` |
| enrich | Threat Intelligence Enrichment | http_request | - | `method: GET`, `url: https://api.threatintel.com/v1/lookup` |
| asset | Asset Context Enrichment | http_request | - | `method: GET`, `url: https://api.assetdb.com/v1/assets` |
| recalibrate | Severity Recalibration | condition | - | `rules: [ai_critical -> critical, ai_high -> high, ai_low + critical_source -> medium]` |
| route | Triage Routing | router | - | `routes: [critical:incident, high:human_review, medium:auto_assign, low:auto_close]` |
| incident | Create Incident | pagerduty_trigger_incident | PagerDuty | `severity: critical`, `service: security` |
| human_review | Human Review Gate | human_approval | - | `timeout: 2h`, `escalation: security_lead` |
| auto_assign | Auto-Assign Analyst | jira_create_issue | Jira | `project: SEC`, `assignee: round_robin` |
| auto_close | Auto-Close Alert | http_request | - | `method: POST`, `url: /api/alerts/close` |
| notify | Team Notification | slack | Slack | `channel: #sec-ops`, `template: triage_result` |
| track | Track Metrics | linear_create_issue | Linear | `project: security-triage`, `status: open` |
| feedback | Analyst Feedback | http_request | - | `method: POST`, `url: /api/feedback` |
| complete | Workflow Completion | stop_fail | - | -

## 8. Edge Definitions

| Source | Target | Condition |
|--------|--------|-----------|
| trigger | deduplicate | always |
| deduplicate | normalize | always |
| normalize | triage | always |
| triage | enrich | always |
| triage | asset | always |
| enrich | recalibrate | always |
| asset | recalibrate | always |
| recalibrate | route | always |
| route | incident | severity == critical |
| route | human_review | severity == high |
| route | auto_assign | severity == medium |
| route | auto_close | severity == low |
| human_review | incident | approved and critical |
| human_review | notify | rejected or timeout |
| auto_assign | notify | always |
| auto_close | notify | always |
| notify | track | always |
| track | feedback | always |
| feedback | complete | always |

## 9. Configuration Variables

| Variable | Type | Example/Default | Purpose |
|----------|------|-----------------|---------|
| `sentry_webhook_url` | string | `https://hooks.sentry.io/...` | Sentry webhook URL |
| `pagerduty_api_key` | string | `xxx` | PagerDuty API key |
| `github_security_webhook_url` | string | `https://hooks.github.com/...` | GitHub Security webhook URL |
| `threat_intel_api_key` | string | `xxx` | Threat intelligence API key |
| `asset_db_api_key` | string | `xxx` | Asset database API key |
| `slack_webhook_url` | string | `https://hooks.slack.com/...` | Slack notification URL |
| `jira_api_key` | string | `xxx` | Jira API key |
| `linear_project_id` | string | `12345` | Linear project ID |
| `triage_agent_id` | string | `security_triage_agent` | AI agent for triage |
| `confidence_threshold` | number | `0.9` | Minimum AI confidence |
| `correlation_window_hours` | number | `1` | Correlation window for deduplication |
| `auto_close_threshold` | number | `0.3` | Auto-close threshold for low severity |
| `human_review_timeout_hours` | number | `2` | Human review timeout |
| `max_retries` | number | `3` | Max retries for API calls |

## 10. Branching Logic

- **Severity Recalibration**: AI-adjusted severity determines routing
- **Critical Alerts**: AI confidence + severity > threshold → incident creation
- **High Severity**: AI confidence < threshold → human review
- **Medium/Low Severity**: Auto-assign/auto-close based on adjusted severity
- **Low Confidence**: If AI confidence < threshold, route to human review

## 11. Success Behavior

1. **Ingestion**: Alerts ingested from multiple sources
2. **Deduplication**: Duplicate alerts correlated and removed
3. **Normalization**: Alerts standardized to unified schema
4. **Triage**: AI analyzes risk, context, and confidence
5. **Enrichment**: Threat intel + asset context added
6. **Recalibration**: Severity adjusted based on AI analysis
7. **Routing**: Critical → incident, High → human review, Medium → auto-assign, Low → auto-close
8. **Incident Creation**: Critical alerts trigger PagerDuty incident
9. **Notification**: Team notified via Slack
10. **Tracking**: Metrics logged to Linear
11. **Feedback**: Analyst feedback collected for model improvement

## 12. Failure Behavior

| Failure Type | Handling |
|--------------|---------|
| Webhook validation failure | Retry with exponential backoff |
| Deduplication failure | Skip correlation, continue with raw alert |
| AI triage timeout | Retry with exponential backoff, max 3 attempts |
| Low AI confidence | Route to human review |
| API failure (threat intel/asset) | Use cached data, log warning |
| PagerDuty API failure | Dead letter queue with reason |
| Jira API failure | Dead letter queue with reason |
| Slack notification failure | Log and continue |
| Linear API failure | Dead letter queue with reason |
| Human review timeout | Escalate to security lead |

## 13. Retry / Recovery Behavior

- **Node-level retries**: All AI nodes retry up to 3 times with exponential backoff
- **API failures**: Retry with exponential backoff (2s, 4s, 8s)
- **Dead letter queue**: Failed PagerDuty/Jira calls go to DLQ
- **Recovery verification**: Requires 3 successful runs before auto-resolving anomalies

## 14. Reliability & Observability Behavior

**FlowOps Contributions:**
- **Anomaly detection**: False positive rate, MTTR spikes, triage accuracy
- **Incident creation**: Automatically creates incident when:
  - AI confidence + severity > threshold
  - Threat intelligence confirms critical risk
  - Asset context indicates high impact
- **Baseline learning**: Tracks:
  - False positive rate
  - MTTR by severity
  - Triage accuracy
  - Threat intelligence enrichment rate
- **Observability metrics**:
  - Alert triage accuracy
  - Time to triage
  - False positive rate
  - Incident creation rate
  - Analyst workload balance

## 15. Security Considerations

- **Permissions**: Requires:
  - Sentry: `alerts` read
  - PagerDuty: `incident_trigger`
  - GitHub: `security_events` read
  - Threat intelligence: `lookup`
  - Asset DB: `asset_read`
  - Slack: `chat:write`
  - Jira: `issue_create`
  - Linear: `issue_create`
- **Secrets**:
  - Sentry webhook URL
  - PagerDuty API key
  - Threat intelligence API key
  - Asset DB API key
  - Slack webhook URL
  - Jira API key
  - Linear API token
- **Sensitive data**: Never exposed in logs; all outputs sanitized
- **Audit trail**: Every triage decision logged with timestamp, confidence, severity

## 16. Setup Requirements

- **Required accounts**:
  - Sentry/PagerDuty/GitHub Security
  - Threat intelligence provider
  - Asset database
  - Slack workspace
  - Jira project
  - Linear project
- **Integrations**:
  - Webhook for alert ingestion
  - Threat intelligence API
  - Asset database API
  - Slack webhook
  - Jira API
  - Linear API
- **Configuration**:
  - Set webhook URLs
  - Configure AI agents
  - Set triage thresholds
  - Configure enrichment APIs
  - Set up Slack webhook

## 17. Expected Outcome

- **Reduced MTTR**: 50% faster triage for critical alerts
- **Lower false positives**: 90%+ reduction in manual review
- **Improved analyst productivity**: 60%+ fewer manual reviews
- **Better incident response**: 80%+ of incidents handled within SLA
- **Continuous improvement**: Model learns from analyst feedback

## 18. React Flow Graph

```json
{
  "nodes": [
    {"id": "trigger", "type": "webhook_trigger", "position": {"x": 0, "y": 0}, "data": {"label": "Security Alert Ingestion", "config": {"event": "alert_created"}}},
    {"id": "deduplicate", "type": "idempotency_check", "position": {"x": 100, "y": 0}, "data": {"label": "Alert Deduplication", "config": {"idempotency_key": "alert_${alert_id}", "ttl": "24h", "correlation_window": "1h"}}},
    {"id": "normalize", "type": "transform", "position": {"x": 200, "y": 0}, "data": {"label": "Alert Normalization", "config": {"schema": "unified_alert_schema"}}},
    {"id": "triage", "type": "ai_agent", "position": {"x": 300, "y": 50}, "data": {"label": "AI Triage Analysis", "config": {"agent_id": "security_triage_agent", "confidence_threshold": "0.9", "timeout": "180s"}}},
    {"id": "enrich", "type": "http_request", "position": {"x": 300, "y": 150}, "data": {"label": "Threat Intelligence Enrichment", "config": {"method": "GET", "url": "https://api.threatintel.com/v1/lookup"}}},
    {"id": "asset", "type": "http_request", "position": {"x": 300, "y": 250}, "data": {"label": "Asset Context Enrichment", "config": {"method": "GET", "url": "https://api.assetdb.com/v1/assets"}}},
    {"id": "recalibrate", "type": "condition", "position": {"x": 400, "y": 100}, "data": {"label": "Severity Recalibration", "config": {"rules": "ai_critical -> critical, ai_high -> high, ai_low + critical_source -> medium"}}},
    {"id": "route", "type": "router", "position": {"x": 500, "y": 100}, "data": {"label": "Triage Routing", "config": {"routes": "critical:incident, high:human_review, medium:auto_assign, low:auto_close"}}},
    {"id": "incident", "type": "pagerduty_trigger_incident", "position": {"x": 600, "y": 0}, "data": {"label": "Create Incident", "config": {"severity": "critical", "service": "security"}}},
    {"id": "human_review", "type": "human_approval", "position": {"x": 600, "y": 100}, "data": {"label": "Human Review Gate", "config": {"timeout": "2h", "escalation": "security_lead"}}},
    {"id": "auto_assign", "type": "jira_create_issue", "position": {"x": 600, "y": 200}, "data": {"label": "Auto-Assign Analyst", "config": {"project": "SEC", "assignee": "round_robin"}}},
    {"id": "auto_close", "type": "http_request", "position": {"x": 600, "y": 300}, "data": {"label": "Auto-Close Alert", "config": {"method": "POST", "url": "/api/alerts/close"}}},
    {"id": "notify", "type": "slack", "position": {"x": 700, "y": 0}, "data": {"label": "Team Notification", "config": {"channel": "#sec-ops", "template": "triage_result"}}},
    {"id": "track", "type": "linear_create_issue", "position": {"x": 700, "y": 150}, "data": {"label": "Track Metrics", "config": {"project": "security-triage", "status": "open"}}},
    {"id": "feedback", "type": "http_request", "position": {"x": 700, "y": 250}, "data": {"label": "Analyst Feedback", "config": {"method": "POST", "url": "/api/feedback"}}},
    {"id": "complete", "type": "stop_fail", "position": {"x": 800, "y": 100}, "data": {"label": "Workflow Completion"}}
  ],
  "edges": [
    {"source": "trigger", "target": "deduplicate", "label": "always"},
    {"source": "deduplicate", "target": "normalize", "label": "always"},
    {"source": "normalize", "target": "triage", "label": "always"},
    {"source": "triage", "target": "enrich", "label": "always"},
    {"source": "triage", "target": "asset", "label": "always"},
    {"source": "enrich", "target": "recalibrate", "label": "always"},
    {"source": "asset", "target": "recalibrate", "label": "always"},
    {"source": "recalibrate", "target": "route", "label": "always"},
    {"source": "route", "target": "incident", "label": "severity == critical"},
    {"source": "route", "target": "human_review", "label": "severity == high"},
    {"source": "route", "target": "auto_assign", "label": "severity == medium"},
    {"source": "route", "target": "auto_close", "label": "severity == low"},
    {"source": "human_review", "target": "incident", "label": "approved and critical"},
    {"source": "human_review", "target": "notify", "label": "rejected or timeout"},
    {"source": "auto_assign", "target": "notify", "label": "always"},
    {"source": "auto_close", "target": "notify", "label": "always"},
    {"source": "notify", "target": "track", "label": "always"},
    {"source": "track", "target": "feedback", "label": "always"},
    {"source": "feedback", "target": "complete", "label": "always"}
  ]
}
```

## 19. Metadata

```json
{
  "category": "AI Operations",
  "tags": ["security-triage", "ai-agent", "threat-intel", "incident-response", "human-in-loop"],
  "integrations": ["Sentry", "PagerDuty", "GitHub", "Anthropic", "Slack", "Jira", "Linear", "ThreatIntel", "AssetDB"],
  "complexity": "High",
  "estimated_setup_minutes": 50
}
```

## 20. Production-Readiness Audit

1. **Real production trigger**: Webhook for security alerts
2. **Meaningful branching**: AI-adjusted severity routing
3. **Retry strategy**: All API nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 180s for AI triage, 2h for human review
5. **Deduplication**: Correlation window with 24h TTL
6. **Partial failure**: Enrichment fallback to available data
7. **External service failure**: Retry with dead letter queue
8. **AI uncertainty**: Confidence threshold gates human review
9. **Human approval**: Required for high-severity alerts
10. **Observability**: Comprehensive metrics tracking
11. **Recovery**: Dead letter queue for failed PagerDuty/Jira calls
12. **Security**: All secrets encrypted, sensitive data sanitized
13. **Configuration**: Realistic configurable variables
14. **Graph quality**: Valid React Flow JSON with clean layout

**Non-applicable**: None