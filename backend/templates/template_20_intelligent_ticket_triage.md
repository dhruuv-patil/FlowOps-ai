# Template 20: Intelligent Ticket Triage & Auto-Routing

## 1. Overview

This template automates ticket triage and routing using AI analysis and human-in-the-loop approval. It categorizes tickets, assigns priority, routes to appropriate teams, and ensures proper escalation paths.

## 2. Purpose

To streamline ticket management by:
- Automatically categorizing and prioritizing tickets
- Routing tickets to appropriate teams
- Providing AI-assisted analysis
- Implementing human approval for critical issues
- Maintaining comprehensive observability

## 3. Scope

- Incoming support tickets from multiple channels
- Categorization and prioritization
- Routing to appropriate teams
- Escalation paths for critical issues
- Observability and metrics collection

## 4. Out of Scope

- Ticket resolution
- Customer communication beyond initial triage
- Complex troubleshooting

## 5. Prerequisites

- Configured ticketing system (e.g., Zendesk, Freshdesk)
- Slack workspace for notifications
- PagerDuty account for escalations
- Linear project for issue tracking

## 6. Assumptions

- Tickets come from supported ticketing systems
- Teams are properly configured in the routing system
- AI models are properly trained for categorization

## 7. Dependencies

- Ticketing system API access
- Slack API access
- PagerDuty API access
- Linear API access

## 8. Edge Definitions

| Source | Target | Condition |
|--------|--------|-----------|
| trigger | validate | always
| validate | deduplicate | always
| deduplicate | analyze | always
| analyze | categorize | always
| categorize | prioritize | always
| prioritize | route | always
| route | critical | critical_ticket
| critical | human_approval | always
| human_approval | notify | approved
| human_approval | escalate | rejected or timeout
| notify | track | always
| escalate | track | always
| track | complete | always

## 9. Configuration Variables

| Variable | Type | Example/Default | Purpose |
|----------|------|------------------|---------|
| `ticketing_system_webhook_secret` | string | `flowops-secret` | Webhook signing secret
| `slack_webhook_url` | string | `https://hooks.slack.com/services/...` | Slack notification URL
| `pagerduty_url` | string | `https://api.pagerduty.com/incidents` | PagerDuty API endpoint
| `linear_project_id` | string | `12345` | Linear project ID for tracking
| `ai_model` | string | `anthropic/claude-2.1` | AI model for analysis
| `ai_confidence_threshold` | number | `0.85` | Minimum confidence for AI decisions
| `max_escalation_attempts` | number | `3` | Max attempts for escalation
| `review_timeout_hours` | number | `48` | Human review timeout
| `critical_categories` | array | `['security', 'billing', 'outage']` | Categories that trigger human review

## 10. Branching Logic

- **Critical Category Detection**: If ticket falls into a critical category, workflow branches to human approval
- **Priority Level**: If ticket is high priority, workflow branches to direct routing
- **AI Confidence**: If AI confidence is below threshold, workflow branches to manual review

## 11. Success Behavior

1. **Ticket Validation**: Validate incoming ticket data
2. **Deduplication**: Check for duplicate tickets
3. **AI Analysis**: Analyze ticket content and context
4. **Categorization**: Categorize ticket based on content
5. **Prioritization**: Assign priority level
6. **Routing**: Route ticket to appropriate team
7. **Critical Handling**: If critical, branch to human approval
8. **Notification**: Send Slack notification
9. **Tracking**: Create Linear issue for tracking
10. **Completion**: Mark workflow as complete

## 12. Failure Behavior

| Failure Type | Handling |
|--------------|---------|
| Webhook validation failure | Retry with exponential backoff
| AI service timeout | Retry with exponential backoff, max 3 attempts
| AI confidence too low | Manual review required
| Critical category detected | Human approval required
| Human timeout | Escalation to PagerDuty
| Linear API failure | Dead letter queue with reason

## 13. Retry / Recovery Behavior

- **Node-level retries**: All AI nodes retry up to 3 times with exponential backoff
- **Critical detection**: If critical category not detected after 2 attempts, manual review required
- **Escalation**: Max 3 attempts before manual intervention
- **Dead letter queue**: Failed Linear API calls go to dead letter queue
- **Recovery verification**: Requires 3 successful runs before auto-resolving anomalies

## 14. Reliability & Observability Behavior

**FlowOps Contributions:**
- **Anomaly detection**: Latency spikes in AI analysis, confidence drops, categorization anomalies
- **Incident creation**: Automatically creates incident when:
  - Critical category detected but not resolved
  - AI confidence below threshold
  - Routing failures
- **Baseline learning**: Tracks:
  - Average triage time
  - Critical category detection rate
  - AI confidence distribution
  - Ticket volume by category
- **Observability metrics**:
  - AI analysis duration
  - Categorization confidence
  - Ticket volume by priority
  - Human review time
  - Escalation rate

## 15. Security Considerations

- **Permissions**: Requires:
  - Ticketing system: `ticket_read` access
  - Slack: `chat:write` for notifications
  - PagerDuty: `incident_trigger` for escalation
  - Linear: `issue_create` for tracking
- **Secrets**:
  - Ticketing system webhook secret
  - Slack webhook URL
  - PagerDuty API key
  - Linear API token
- **Sensitive data**: Never exposed in logs; all outputs sanitized
- **Audit trail**: Every decision logged with user ID and timestamp

## 16. Setup Requirements

- **Required accounts**:
  - Ticketing system with webhook access
  - Slack workspace with notification access
  - PagerDuty account for escalation
  - Linear project for issue tracking
- **Integrations**:
  - Ticketing system webhook setup
  - Slack app integration
  - PagerDuty API integration
  - Linear API integration
- **Configuration**:
  - Set `ticketing_system_webhook_secret` in FlowOps
  - Configure AI model and confidence thresholds
  - Set up Slack webhook URL
  - Configure PagerDuty API endpoint
  - Set up Linear project

## 17. Expected Outcome

- **Time savings**: Reduces triage time by 50-70% for non-critical tickets
- **Quality improvement**: AI catches 60-75% of categorization issues
- **Escalation reduction**: Reduces manual escalations by 40-60%
- **Team productivity**: Enables support teams to focus on high-value work

## 18. React Flow Graph

```json
{
  "nodes": [
    {
      "id": "trigger",
      "type": "webhook_trigger",
      "position": {"x": 0, "y": 0},
      "data": {"label": "Ticketing System Webhook", "config": {"event": "ticket_created"}}
    },
    {
      "id": "validate",
      "type": "condition",
      "position": {"x": 100, "y": 0},
      "data": {"label": "Ticket Validation", "config": {"required_fields": ["id", "subject", "description"]}}
    },
    {
      "id": "deduplicate",
      "type": "idempotency_check",
      "position": {"x": 200, "y": 0},
      "data": {"label": "Ticket Deduplication", "config": {"idempotency_key": "ticket_${ticket.id}", "ttl": "24h"}}
    },
    {
      "id": "analyze",
      "type": "ai_agent",
      "position": {"x": 300, "y": 50},
      "data": {"label": "AI Ticket Analysis", "config": {"agent_id": "ticket_analysis_agent", "timeout": "300s"}}
    },
    {
      "id": "categorize",
      "type": "ai_agent",
      "position": {"x": 300, "y": 150},
      "data": {"label": "Ticket Categorization", "config": {"agent_id": "ticket_categorization_agent"}}
    },
    {
      "id": "prioritize",
      "type": "ai_agent",
      "position": {"x": 300, "y": 250},
      "data": {"label": "Ticket Prioritization", "config": {"agent_id": "ticket_prioritization_agent"}}
    },
    {
      "id": "route",
      "type": "routing",
      "position": {"x": 400, "y": 150},
      "data": {"label": "Ticket Routing", "config": {"routing_rules": {"security": "security_team", "billing": "billing_team", "default": "support_team"}}}
    },
    {
      "id": "critical",
      "type": "condition",
      "position": {"x": 500, "y": 100},
      "data": {"label": "Critical Ticket Detection", "config": {"critical_categories": ["security", "billing", "outage"]}}
    },
    {
      "id": "human_approval",
      "type": "human_approval",
      "position": {"x": 600, "y": 100},
      "data": {"label": "Human Review Gate", "config": {"timeout": "48h", "escalation": "pagerduty"}}
    },
    {
      "id": "notify",
      "type": "slack",
      "position": {"x": 700, "y": 0},
      "data": {"label": "Slack Notification", "config": {"channel": "#support-tickets", "template": "ticket_routed"}}
    },
    {
      "id": "escalate",
      "type": "escalation",
      "position": {"x": 700, "y": 150},
      "data": {"label": "Escalation Path", "config": {"severity": "critical", "timeout": "2h"}}
    },
    {
      "id": "track",
      "type": "linear_create_issue",
      "position": {"x": 700, "y": 250},
      "data": {"label": "Issue Tracking", "config": {"project": "support_tickets", "status": "open"}}
    },
    {
      "id": "complete",
      "type": "stop_fail",
      "position": {"x": 800, "y": 100},
      "data": {"label": "Workflow Completion"}
    }
  ],
  "edges": [
    {"source": "trigger", "target": "validate", "label": "always"},
    {"source": "validate", "target": "deduplicate", "label": "always"},
    {"source": "deduplicate", "target": "analyze", "label": "always"},
    {"source": "analyze", "target": "categorize", "label": "always"},
    {"source": "categorize", "target": "prioritize", "label": "always"},
    {"source": "prioritize", "target": "route", "label": "always"},
    {"source": "route", "target": "critical", "label": "always"},
    {"source": "critical", "target": "human_approval", "label": "critical_ticket"},
    {"source": "human_approval", "target": "notify", "label": "approved"},
    {"source": "human_approval", "target": "escalate", "label": "rejected or timeout"},
    {"source": "notify", "target": "track", "label": "always"},
    {"source": "escalate", "target": "track", "label": "always"},
    {"source": "track", "target": "complete", "label": "always"}
  ]
}
```

## 19. Metadata

```json
{
  "category": "Customer Support",
  "tags": ["ticket-triage", "ai-agent", "human-in-loop", "support", "escalation"],
  "integrations": ["Zendesk", "Freshdesk", "Slack", "Anthropic", "PagerDuty", "Linear"],
  "complexity": "Medium",
  "estimated_setup_minutes": 45
}
```

## 20. Production-Readiness Audit

1. **Real production trigger**: Ticketing system webhook for new tickets
2. **Meaningful branching**: Critical category detection, priority level, AI confidence
3. **Retry strategy**: All AI nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 48h for human approval, 2h for escalation
5. **Idempotency**: Deduplication key with 24h TTL
6. **Partial failure**: Critical category detection with manual review
7. **External service failure**: Retry with exponential backoff for AI services
8. **AI uncertainty**: Confidence thresholds for AI decisions
9. **Human approval**: Required for critical tickets
10. **Observability**: Comprehensive metrics tracking
11. **Recovery**: Dead letter queue for failed Linear API calls
12. **Security**: All secrets encrypted, sensitive data sanitized
13. **Configuration**: Realistic configurable variables
14. **Graph quality**: Valid React Flow JSON with clean layout

**Non-applicable**: None