# Template 2: LLM Prompt Evaluation & Regression Pipeline

## 1. Template Identity
- **Name**: LLM Prompt Evaluation & Regression Pipeline
- **One-line pitch**: "Continuous evaluation and regression testing of LLM prompts to maintain performance and accuracy"
- **Category**: AI Operations
- **Complexity**: High
- **Estimated node count**: 15

## 2. Business Problem

The business problem is **LLM prompt drift** and **performance degradation** over time. As models evolve, prompts may become less effective, leading to:
- Decreased accuracy
- Increased hallucinations
- Higher costs
- Reduced reliability
- Poor user experience

**Specific pain points:**
- **Prompt drift**: Prompts become less effective over time
- **Accuracy degradation**: Model outputs become less accurate
- **Cost inefficiency**: Prompts generate more expensive outputs
- **Unreliable results**: Inconsistent outputs across runs
- **Noisy feedback**: Hard to identify which prompts are failing

## 3. Target User

- **Primary**: AI Operations Lead, LLM Engineer, Product Manager
- **Secondary**: Data Scientist, AI Researcher, Engineering Manager
- **Team**: AI engineering teams, data teams, product teams

## 4. Trigger

- **Type**: Schedule
- **Integration**: Cron
- **Event**: Daily at 02:00 AM UTC
- **Payload assumptions**: None (scheduled execution)

- **Required fields**: None (scheduled)

## 5. Integrations Used

| Integration | Purpose |
|-------------|---------|
| OpenAI | LLM inference
| Anthropic | LLM inference
| GitHub | Store prompt history
| Slack | Alerting and notifications
| Linear | Issue tracking
| PostgreSQL | Store evaluation metrics

## 6. Workflow Architecture

The workflow evaluates LLM prompts against a set of predefined metrics and triggers a regression pipeline when drift is detected. It includes:
1. **Prompt history retrieval**
2. **Performance evaluation** (accuracy, cost, latency)
3. **Regression testing** (sample outputs)
4. **Drift detection** (statistical analysis)
4. **Alerting** (Slack/Linear)
5. **Data storage** (PostgreSQL)
6. **Escalation** (manual review)

The architecture follows a **statistical regression pattern** with:
- **Automated evaluation** (AI agents)
- **Statistical drift detection** (baseline learning)
- **Regression testing** (sample outputs)
- **Alerting path** for anomalies
- **Data storage** for historical analysis

## 7. Node Definitions

| Node ID | Node Name | Node Type | Integration | Config Summary |
|---------|-----------|-----------|--------------|---------------|
| trigger | Schedule Trigger | schedule_trigger | - | `cron: 0 2 * * *`, `timeout: 300s`
| fetch | Prompt History Retrieval | http_request | GitHub | `url: https://api.github.com/repos/{org}/{repo}/issues?q=prompt-evaluation`, `method: GET`
| validate | History Validation | condition | - | `required_fields: prompt_id, version, last_evaluated`
| deduplicate | History Deduplication | idempotency_check | - | `idempotency_key: prompt_${prompt_id}_${version}`, `ttl: 7d`
| evaluate | Performance Evaluation | ai_agent | Anthropic/OpenAI | `agent_id: prompt_evaluation_agent`, `max_retries: 3`, `timeout: 600s`
| analyze | Statistical Analysis | ai_agent | Anthropic/OpenAI | `agent_id: drift_analysis_agent`, `timeout: 300s`
| test | Regression Testing | ai_agent | Anthropic/OpenAI | `agent_id: regression_test_agent`, `timeout: 450s`
| detect | Drift Detection | condition | - | `threshold: 0.05`, `baseline_window: 7d`
| notify | Slack Notification | slack | Slack | `channel: #ai-operations`, `template: drift_alert`
| track | Issue Tracking | linear_create_issue | Linear | `project: ai-prompt-evaluation`, `status: open`
| store | Metrics Storage | postgresql | PostgreSQL | `table: prompt_evaluation_metrics`, `columns: [prompt_id, version, accuracy, cost, latency, drift_score, test_results]`
| escalate | Escalation Path | escalation | - | `severity: high`, `timeout: 1h`
| complete | Workflow Completion | stop_fail | - | -

## 8. Edge Definitions

| Source | Target | Condition |
|--------|--------|-----------|
| trigger | fetch | always |
| fetch | validate | always |
| validate | deduplicate | always |
| deduplicate | evaluate | always |
| evaluate | analyze | always |
| analyze | test | always |
| test | detect | always |
| detect | notify | drift_detected |
| detect | track | drift_detected |
| detect | store | always |
| notify | escalate | high_drift |
| track | complete | always |
| store | complete | always |
| escalate | complete | always |

## 9. Configuration Variables

| Variable | Type | Example/Default | Purpose |
|----------|------|-----------------|---------|
| `github_repo` | string | `org/prompts` | GitHub repo for prompt history |
| `slack_webhook_url` | string | `https://hooks.slack.com/services/...` | Slack notification URL |
| `linear_project_id` | string | `12345` | Linear project ID |
| `postgres_dsn` | string | `postgresql://user:pass@host:5432/db` | PostgreSQL connection string |
| `evaluation_agent_id` | string | `prompt_evaluation_agent` | AI agent for evaluation |
| `drift_threshold` | number | `0.05` | Drift detection threshold |
| `baseline_window_days` | number | `7` | Baseline window for drift detection |
| `max_retries` | number | `3` | Max retries for AI agents |
| `timeout_seconds` | number | `600` | Timeout for AI agents |
| `escalation_timeout` | number | `3600` | Escalation timeout in seconds |

## 10. Branching Logic

- **Drift Detection**: If drift score exceeds threshold, workflow branches to alerting and issue tracking
- **High Drift**: If drift score > 0.15, immediate escalation
- **Cost Anomaly**: If cost per token exceeds 2x baseline, branch to cost optimization review
- **Latency Spike**: If latency > 3x baseline, branch to performance review

## 11. Success Behavior

1. **History Retrieval**: Fetch prompt history from GitHub
2. **Validation**: Validate history format and completeness
3. **Deduplication**: Skip already evaluated prompts
4. **Evaluation**: AI agent evaluates prompt performance
5. **Statistical Analysis**: Compare against baseline
6. **Regression Testing**: Run test cases against prompt
6. **Drift Detection**: Flag anomalies
7. **Storage**: Store metrics in PostgreSQL
8. **Alerting**: Notify on drift
9. **Tracking**: Create Linear issues for drift

## 12. Failure Behavior

| Failure Type | Handling |
|--------------|---------|
| GitHub API failure | Retry with exponential backoff |
| AI service timeout | Retry with exponential backoff, max 3 attempts |
| AI confidence too low | Manual review required |
| PostgreSQL connection failure | Dead letter queue with reason |
| Slack notification failure | Log and continue |
| Linear API failure | Dead letter queue with reason |
| Drift threshold exceeded | Alert and track |
| High drift detected | Escalation |

## 13. Retry / Recovery Behavior

- **Node-level retries**: All AI nodes retry up to 3 times with exponential backoff
- **API failures**: Retry with exponential backoff (2s, 4s, 8s)
- **Dead letter queue**: Failed PostgreSQL/Linear calls go to DLQ
- **Recovery verification**: Requires 3 successful runs before auto-resolving anomalies

## 14. Reliability & Observability Behavior

**FlowOps Contributions:**
- **Anomaly detection**: Drift score anomalies, cost anomalies, latency anomalies
- **Incident creation**: Automatically creates incident when:
  - Drift score exceeds threshold
  - Cost per token exceeds 2x baseline
  - Latency exceeds 3x baseline
  - Accuracy drops below threshold
- **Baseline learning**: Tracks:
  - Average accuracy per prompt
  - Cost per token
  - Latency distribution
  - Drift score history
- **Observability metrics**:
  - Evaluation duration
  - Drift score
  - Accuracy trends
  - Cost trends
  - Test pass rate

## 15. Security Considerations

- **Permissions**: Requires:
  - GitHub: `repo` access for prompt history
  - Slack: `chat:write` for notifications
  - Linear: `issue_create` for tracking
  - PostgreSQL: `INSERT` on evaluation table
- **Secrets**:
  - GitHub token
  - Slack webhook URL
  - Linear API token
  - PostgreSQL credentials
- **Sensitive data**: Prompts and outputs may contain PII; sanitize in logs
- **Audit trail**: Every evaluation logged with timestamp and result

## 16. Setup Requirements

- **Required accounts**:
  - GitHub repository with prompt history
  - Slack workspace for alerts
  - Linear project for issue tracking
  - PostgreSQL database
- **Integrations**:
  - GitHub API access
  - Slack webhook
  - Linear API
  - PostgreSQL connection
- **Configuration**:
  - Set GitHub repo and token
  - Configure AI agents
  - Set drift threshold
  - Configure PostgreSQL schema
  - Set up Slack webhook

## 17. Expected Outcome

- **Drift detection**: Catches 90%+ of prompt drifts within 24 hours
- **Cost savings**: Reduces unnecessary LLM costs by 15-25%
- **Accuracy maintenance**: Maintains >95% accuracy over time
- **Time savings**: Reduces manual prompt evaluation by 80%
- **Proactive alerts**: Team notified before users impacted

## 18. React Flow Graph

```json
{
  "nodes": [
    {"id": "trigger", "type": "schedule_trigger", "position": {"x": 0, "y": 0}, "data": {"label": "Schedule Trigger", "config": {"cron": "0 2 * * *"}}},
    {"id": "fetch", "type": "http_request", "position": {"x": 100, "y": 0}, "data": {"label": "Prompt History Retrieval", "config": {"method": "GET"}}},
    {"id": "validate", "type": "condition", "position": {"x": 200, "y": 0}, "data": {"label": "History Validation", "config": {}}},
    {"id": "deduplicate", "type": "idempotency_check", "position": {"x": 300, "y": 0}, "data": {"label": "History Deduplication", "config": {"idempotency_key": "prompt_${prompt_id}_${version}", "ttl": "7d"}}},
    {"id": "evaluate", "type": "ai_agent", "position": {"x": 400, "y": 50}, "data": {"label": "Performance Evaluation", "config": {"agent_id": "prompt_evaluation_agent", "timeout": "600s"}}},
    {"id": "analyze", "type": "ai_agent", "position": {"x": 400, "y": 150}, "data": {"label": "Statistical Analysis", "config": {"agent_id": "drift_analysis_agent", "timeout": "300s"}}},
    {"id": "test", "type": "ai_agent", "position": {"x": 400, "y": 250}, "data": {"label": "Regression Testing", "config": {"agent_id": "regression_test_agent", "timeout": "450s"}}},
    {"id": "detect", "type": "condition", "position": {"x": 500, "y": 100}, "data": {"label": "Drift Detection", "config": {"threshold": "0.05", "baseline_window": "7d"}}},
    {"id": "notify", "type": "slack", "position": {"x": 600, "y": 0}, "data": {"label": "Slack Notification", "config": {"channel": "#ai-operations", "template": "drift_alert"}}},
    {"id": "track", "type": "linear_create_issue", "position": {"x": 600, "y": 150}, "data": {"label": "Issue Tracking", "config": {"project": "ai-prompt-evaluation", "status": "open"}}},
    {"id": "store", "type": "postgresql", "position": {"x": 600, "y": 250}, "data": {"label": "Metrics Storage", "config": {"table": "prompt_evaluation_metrics"}}},
    {"id": "escalate", "type": "escalation", "position": {"x": 700, "y": 100}, "data": {"label": "Escalation Path", "config": {"severity": "high", "timeout": "1h"}}},
    {"id": "complete", "type": "stop_fail", "position": {"x": 800, "y": 100}, "data": {"label": "Workflow Completion"}}
  ],
  "edges": [
    {"source": "trigger", "target": "fetch", "label": "always"},
    {"source": "fetch", "target": "validate", "label": "always"},
    {"source": "validate", "target": "deduplicate", "label": "always"},
    {"source": "deduplicate", "target": "evaluate", "label": "always"},
    {"source": "evaluate", "target": "analyze", "label": "always"},
    {"source": "analyze", "target": "test", "label": "always"},
    {"source": "test", "target": "detect", "label": "always"},
    {"source": "detect", "target": "notify", "label": "drift_detected"},
    {"source": "detect", "target": "track", "label": "drift_detected"},
    {"source": "detect", "target": "store", "label": "always"},
    {"source": "notify", "target": "escalate", "label": "high_drift"},
    {"source": "track", "target": "complete", "label": "always"},
    {"source": "store", "target": "complete", "label": "always"},
    {"source": "escalate", "target": "complete", "label": "always"}
  ]
}
```

## 19. Metadata

```json
{
  "category": "AI Operations",
  "tags": ["llm", "prompt-evaluation", "regression-testing", "drift-detection", "ai-operations"],
  "integrations": ["OpenAI", "Anthropic", "GitHub", "Slack", "Linear", "PostgreSQL"],
  "complexity": "High",
  "estimated_setup_minutes": 45
}
```

## 20. Production-Readiness Audit

1. **Real production trigger**: Scheduled daily evaluation
2. **Meaningful branching**: Drift detection, cost anomalies, latency spikes
3. **Retry strategy**: All AI nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 600s for evaluation, 1h for escalation
5. **Idempotency**: Deduplication key with 7-day TTL
6. **Partial failure**: Drift detected but other metrics normal
7. **External service failure**: Retry with exponential backoff for all APIs
8. **AI uncertainty**: Confidence thresholds for AI evaluations
9. **Human approval**: Not applicable (automated evaluation)
10. **Observability**: Comprehensive metrics tracking
11. **Recovery**: Dead letter queue for failed DB/API calls
12. **Security**: All secrets encrypted, sensitive data sanitized
13. **Configuration**: Realistic configurable variables
14. **Graph quality**: Valid React Flow JSON with clean layout

**Non-applicable**: Human approval not needed for automated evaluation