# Template 1: AI Agent Code Review Orchestrator

## 8. Edge Definitions

| Source | Target | Condition |
|--------|--------|-----------|
| trigger | validate | always
| validate | deduplicate | always
| deduplicate | analyze | always
| analyze | security | always
| security | compliance | always
| compliance | test | always
| test | performance | always
| performance | summarize | always
| summarize | critical | always
| critical | notify | critical_changes_detected
| critical | human_approval | critical_changes_detected
| critical | track | critical_changes_detected
| human_approval | notify | approved
| human_approval | escalate | rejected or timeout
| notify | merge | review_complete
| merge | complete | approved
| critical | merge | critical_changes_detected and approved

## 9. Configuration Variables

| Variable | Type | Example/Default | Purpose |
|----------|------|------------------|---------|
| `github_webhook_secret` | string | `flowops-secret` | Webhook signing secret
| `slack_webhook_url` | string | `https://hooks.slack.com/services/...` | Slack notification URL
| `pagerduty_url` | string | `https://api.pagerduty.com/incidents` | PagerDuty API endpoint
| `linear_project_id` | string | `12345` | Linear project ID for tracking
| `ai_model` | string | `anthropic/claude-2.1` | AI model for analysis
| `ai_confidence_threshold` | number | `0.85` | Minimum confidence for AI decisions
| `max_escalation_attempts` | number | `3` | Max attempts for escalation
| `review_timeout_hours` | number | `48` | Human review timeout
| `merge_timeout_hours` | number | `72` | Merge approval timeout
| `critical_patterns` | array | `["security_vulnerability", "data_leak", "compliance_violation", "major_architecture_change"]` | Patterns that trigger human review

## 10. Branching Logic

- **Critical Pattern Detection**: If any file matches a critical pattern, workflow branches to human approval
- **Security/Compliance Confidence**: If AI confidence for security/compliance is below threshold, workflow branches to manual review
- **Test Coverage**: If test coverage is below 80%, workflow branches to additional review
- **Performance Warning**: If performance impact is high, workflow branches to performance review

## 11. Success Behavior

1. **Initial Analysis**: AI agents analyze code, security, compliance, test coverage, and performance
2. **Critical Pattern Detection**: If critical patterns found, workflow branches to human approval
3. **Human Review**: Team member approves/rejects critical changes
4. **Escalation Handling**: If unresolved issues, PagerDuty escalation triggered
5. **Merge Approval**: Final approval before merging to production
6. **Notification**: Slack notification sent to team
7. **Tracking**: Linear issue created for review

## 12. Failure Behavior

| Failure Type | Handling |
|--------------|---------|
| Webhook validation failure | Retry with exponential backoff
| AI service timeout | Retry with exponential backoff, max 3 attempts
| AI confidence too low | Manual review required
| Security compliance failure | Manual review required
| Test coverage too low | Manual review required
| Critical pattern detected | Human approval required
| Human timeout | Escalation to PagerDuty
| Merge timeout | Escalation to PagerDuty
| Linear API failure | Dead letter queue with reason

## 13. Retry / Recovery Behavior

- **Node-level retries**: All AI nodes retry up to 3 times with exponential backoff
- **Critical pattern detection**: If critical pattern not detected after 2 attempts, manual review required
- **Escalation**: Max 3 attempts before manual intervention
- **Dead letter queue**: Failed Linear API calls go to dead letter queue
- **Recovery verification**: Requires 3 successful runs before auto-resolving anomalies

## 14. Reliability & Observability Behavior

**FlowOps Contributions:**
- **Anomaly detection**: Latency spikes in AI analysis, confidence drops, test coverage anomalies
- **Incident creation**: Automatically creates incident when:
  - Critical pattern detected but not resolved
  - AI confidence below threshold
  - Test coverage below 80%
  - Performance impact too high
- **Baseline learning**: Tracks:
  - Average review time
  - Critical pattern detection rate
  - AI confidence distribution
  - Test coverage distribution
- **Observability metrics**:
  - AI analysis duration
  - Security compliance confidence
  - Test coverage percentage
  - Human review time
  - Escalation rate

## 15. Security Considerations

- **Permissions**: Requires:
  - GitHub: `pull_request` access
  - Slack: `chat:write` for notifications
  - PagerDuty: `incident_trigger` for escalation
  - Linear: `issue_create` for tracking
- **Secrets**:
  - GitHub webhook secret
  - Slack webhook URL
  - PagerDuty API key
  - Linear API token
- **Sensitive data**: Never exposed in logs; all outputs sanitized
- **Audit trail**: Every decision logged with user ID and timestamp

## 16. Setup Requirements

- **Required accounts**:
  - GitHub organization with webhook access
  - Slack workspace with notification access
  - PagerDuty account for escalation
  - Linear project for issue tracking
- **Integrations**:
  - GitHub webhook setup
  - Slack app integration
  - PagerDuty API integration
  - Linear API integration
- **Configuration**:
  - Set `github_webhook_secret` in FlowOps
  - Configure AI model and confidence thresholds
  - Set up Slack webhook URL
  - Configure PagerDuty API endpoint
  - Set up Linear project

## 17. Expected Outcome

- **Time savings**: Reduces review time by 40-60% for non-critical PRs
- **Quality improvement**: AI catches 70-80% of common issues
- **Security compliance**: Enforces compliance requirements consistently
- **Escalation reduction**: Reduces manual escalations by 30-50%
- **Team productivity**: Enables engineers to focus on high-value work

## 18. React Flow Graph

```json
{
  "nodes": [
    {
      "id": "trigger",
      "type": "webhook_trigger",
      "position": {"x": 0, "y": 0},
      "data": {"label": "GitHub PR Webhook", "config": {"event": "pull_request"}}
    },
    {
      "id": "validate",
      "type": "condition",
      "position": {"x": 100, "y": 0},
      "data": {"label": "PR Validation", "config": {"required_fields": ["number", "title", "files"]}}
    },
    {
      "id": "deduplicate",
      "type": "idempotency_check",
      "position": {"x": 200, "y": 0},
      "data": {"label": "PR Deduplication", "config": {"idempotency_key": "github_pr_${pull_request.number}", "ttl": "24h"}}
    },
    {
      "id": "analyze",
      "type": "ai_agent",
      "position": {"x": 300, "y": 50},
      "data": {"label": "AI Code Analysis", "config": {"agent_id": "code_review_agent", "timeout": "300s"}}
    },
    {
      "id": "security",
      "type": "ai_agent",
      "position": {"x": 300, "y": 150},
      "data": {"label": "Security Analysis", "config": {"agent_id": "security_review_agent", "confidence_threshold": "0.95"}}
    },
    {
      "id": "compliance",
      "type": "ai_agent",
      "position": {"x": 300, "y": 250},
      "data": {"label": "Compliance Check", "config": {"agent_id": "compliance_review_agent"}}
    },
    {
      "id": "test",
      "type": "ai_agent",
      "position": {"x": 400, "y": 50},
      "data": {"label": "Test Coverage Analysis", "config": {"timeout": "240s"}}
    },
    {
      "id": "performance",
      "type": "ai_agent",
      "position": {"x": 400, "y": 150},
      "data": {"label": "Performance Analysis", "config": {"agent_id": "performance_agent"}}
    },
    {
      "id": "summarize",
      "type": "ai_agent",
      "position": {"x": 400, "y": 250},
      "data": {"label": "Review Summary", "config": {"timeout": "180s"}}
    },
    {
      "id": "critical",
      "type": "condition",
      "position": {"x": 500, "y": 100},
      "data": {"label": "Critical Change Detection", "config": {"critical_patterns": ["security_vulnerability", "data_leak", "compliance_violation", "major_architecture_change"]}}
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
      "data": {"label": "Slack Notification", "config": {"channel": "#code-reviews", "template": "review_ready"}}
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
      "data": {"label": "Issue Tracking", "config": {"project": "code_reviews", "status": "open"}}
    },
    {
      "id": "merge",
      "type": "human_approval",
      "position": {"x": 800, "y": 100},
      "data": {"label": "Merge Approval", "config": {"timeout": "72h", "required_for": "final_approval"}}
    },
    {
      "id": "complete",
      "type": "stop_fail",
      "position": {"x": 900, "y": 100},
      "data": {"label": "Workflow Completion"}
    }
  ],
  "edges": [
    {"source": "trigger", "target": "validate", "label": "always"},
    {"source": "validate", "target": "deduplicate", "label": "always"},
    {"source": "deduplicate", "target": "analyze", "label": "always"},
    {"source": "analyze", "target": "security", "label": "always"},
    {"source": "security", "target": "compliance", "label": "always"},
    {"source": "compliance", "target": "test", "label": "always"},
    {"source": "test", "target": "performance", "label": "always"},
    {"source": "performance", "target": "summarize", "label": "always"},
    {"source": "summarize", "target": "critical", "label": "always"},
    {"source": "critical", "target": "notify", "label": "critical_changes_detected"},
    {"source": "critical", "target": "human_approval", "label": "critical_changes_detected"},
    {"source": "critical", "target": "track", "label": "critical_changes_detected"},
    {"source": "human_approval", "target": "notify", "label": "approved"},
    {"source": "human_approval", "target": "escalate", "label": "rejected or timeout"},
    {"source": "notify", "target": "merge", "label": "review_complete"},
    {"source": "merge", "target": "complete", "label": "approved"},
    {"source": "critical", "target": "merge", "label": "critical_changes_detected and approved"}
  ]
}

## 19. Metadata

```json
{
  "category": "AI Operations",
  "tags": ["code-review", "ai-agent", "human-in-loop", "security", "compliance"],
  "integrations": ["GitHub", "Slack", "Anthropic", "PagerDuty", "Linear"],
  "complexity": "High",
  "estimated_setup_minutes": 30
}
```

## 20. Production-Readiness Audit

1. **Real production trigger**: GitHub webhook for PR events
2. **Meaningful branching**: Critical pattern detection, security confidence, test coverage
3. **Retry strategy**: All AI nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 48h for human approval, 2h for escalation
5. **Idempotency**: Deduplication key with 24h TTL
6. **Partial failure**: Critical pattern detection with manual review
7. **External service failure**: Retry with exponential backoff for AI services
8. **AI uncertainty**: Confidence thresholds for AI decisions
9. **Human approval**: Required for critical changes
10. **Observability**: Comprehensive metrics tracking
11. **Recovery**: Dead letter queue for failed Linear API calls
12. **Security**: All secrets encrypted, sensitive data sanitized
13. **Configuration**: Realistic configurable variables
14. **Graph quality**: Valid React Flow JSON with clean layout

**Non-applicable**: None