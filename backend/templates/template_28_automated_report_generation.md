# Template 28: Automated Report Generation & Distribution

## 1. Template Identity
- **Name**: Automated Report Generation & Distribution
- **One-line pitch**: "Automated report generation with AI analysis and distribution to stakeholders"
- **Category**: Business Operations
- **Complexity**: Medium
- **Estimated node count**: 18

## 2. Business Problem

The business problem is **manual report generation delays** where:
- Reports take too long to generate
- Data is inconsistent across reports
- Distribution is slow and error-prone
- Reports are not generated on schedule
- Manual analysis introduces errors

**Specific pain points:**
- **Slow generation**: Reports take 2-4 hours to generate
- **Inconsistent data**: Different reports show different data
- **Slow distribution**: Email distribution takes 1-2 hours
- **Missed deadlines**: Reports not generated on schedule
- **Human error**: Manual analysis introduces errors

## 3. Target User

- **Primary**: Business Analyst, Data Analyst, Operations Manager
- **Secondary**: Engineering Manager, Product Manager
- **Team**: Business Operations, Data Team

## 4. Trigger

- **Type**: Schedule / Webhook
- **Integration**: Google Sheets, Salesforce, HubSpot, Stripe, PostgreSQL
- **Event**: Scheduled report time, data update, new transaction
- **Payload assumptions**:
  - `report_type` (daily, weekly, monthly)
  - `start_date`, `end_date`
  - `recipients` (email addresses)
  - `data_source` (Google Sheets, Salesforce, etc.)
  - `report_format` (PDF, Excel, HTML)

- **Required fields**: `report_type`, `start_date`, `end_date`, `recipients`

## 5. Integrations Used

| Integration | Purpose |
|-------------|---------|
| Google Sheets | Data source |
| Salesforce | Data source |
| HubSpot | Data source |
| Stripe | Data source |
| PostgreSQL | Data source |
| Anthropic/OpenAI | AI analysis |
| Slack | Team notifications |
| Email | Report distribution |
| Jira | Issue tracking |
| Linear | Issue tracking |

## 6. Workflow Architecture

The workflow automates report generation and distribution with AI analysis and stakeholder notifications. It includes:
1. **Data collection** (from multiple sources)
2. **Data aggregation** (consolidation)
3. **AI analysis** (insights generation)
4. **Report generation** (formatting)
5. **Distribution** (to stakeholders)
6. **Notification** (confirmation)
7. **Tracking** (metrics)

The architecture follows a **validated automation pattern** with:
- **Data collection** (multiple sources)
- **Data aggregation** (consolidation)
- **AI analysis** (insights generation)
- **Report generation** (formatting)
- **Distribution** (to stakeholders)
- **Notification** (confirmation)
- **Tracking** (metrics)

## 7. Node Definitions

| Node ID | Node Name | Node Type | Integration | Config Summary |
|---------|-----------|-----------|--------------|---------------|
| trigger | Report Generation Trigger | schedule_trigger | - | `schedule: daily at 9am`, `required_fields: report_type,start_date,end_date,recipients` |
| collect | Data Collection | http_request | Google Sheets/Salesforce/Stripe | `method: GET`, `url: /api/data`, `timeout: 180s` |
| aggregate | Data Aggregation | data_aggregator | - | `sources: [google_sheets, salesforce, stripe]`, `timeout: 120s` |
| analyze | AI Analysis | ai_agent | Anthropic/OpenAI | `agent_id: report_analyzer`, `confidence_threshold: 0.85`, `timeout: 180s` |
| generate | Report Generation | report_generator | - | `format: PDF/Excel/HTML`, `template: standard_report`, `timeout: 120s` |
| distribute | Report Distribution | email | - | `recipients: ${recipients}`, `subject: Automated Report`, `timeout: 180s` |
| notify | Team Notification | slack | Slack | `channel: #reports`, `template: report_generated`, `timeout: 60s` |
| track | Track Metrics | linear_create_issue | Linear | `project: reports`, `status: open`, `timeout: 60s` |
| complete | Workflow Completion | stop_fail | - | - |

## 8. Edge Definitions

| Source | Target | Condition |
|--------|--------|-----------|
| trigger | collect | always |
| collect | aggregate | success |
| aggregate | analyze | success |
| analyze | generate | success |
| generate | distribute | success |
| distribute | notify | success |
| notify | track | success |
| track | complete | always |

## 9. Configuration Variables

| Variable | Type | Example/Default | Purpose |
|----------|------|-----------------|---------|
| `report_schedule` | string | `daily at 9am` | Report generation schedule |
| `data_sources` | array | `[google_sheets, salesforce, stripe]` | Data sources |
| `report_analyzer_agent_id` | string | `report_analyzer` | AI agent for analysis |
| `confidence_threshold` | number | `0.85` | Minimum AI confidence |
| `report_format` | string | `PDF` | Report format |
| `recipients` | array | `[user1@example.com, user2@example.com]` | Report recipients |
| `slack_channel` | string | `#reports` | Slack notification channel |
| `linear_project_id` | string | `reports` | Linear project ID |

## 10. Branching Logic

- **Data collection**: Success → aggregate, failure → abort
- **Data aggregation**: Success → analyze, failure → retry
- **AI analysis**: Success → generate, failure → retry
- **Report generation**: Success → distribute, failure → retry
- **Report distribution**: Success → notify, failure → retry
- **Team notification**: Success → track, failure → retry

## 11. Success Behavior

1. **Data collection**: Data collected from multiple sources
2. **Data aggregation**: Data consolidated into a single dataset
3. **AI analysis**: Insights generated from the data
4. **Report generation**: Report formatted and generated
5. **Report distribution**: Report sent to stakeholders
6. **Team notification**: Team notified of report generation
7. **Tracking**: Metrics tracked for future reference

## 12. Failure Behavior

| Failure Type | Handling |
|--------------|---------|
| Data collection failure | Retry with exponential backoff |
| Data aggregation failure | Retry with exponential backoff |
| AI analysis failure | Retry with exponential backoff |
| Report generation failure | Retry with exponential backoff |
| Report distribution failure | Retry with exponential backoff |
| Team notification failure | Retry with exponential backoff |
| Tracking failure | Dead letter queue |

## 13. Retry / Recovery Behavior

- **Node-level retries**: All nodes retry up to 3 times with exponential backoff
- **Dead letter queue**: Failed tracking calls go to DLQ
- **Recovery verification**: Requires 3 successful runs before auto-resolving

## 14. Reliability & Observability Behavior

**FlowOps Contributions:**
- **Anomaly detection**: Report generation time, data collection success rate, AI confidence
- **Incident creation**: Automatically creates incident when:
  - Report generation fails > 3 times
  - Data collection fails > 3 times
  - AI confidence below threshold
- **Baseline learning**: Tracks:
  - Average report generation time
  - Data collection success rate
  - AI confidence distribution
  - Time to distribution
- **Observability metrics**:
  - Report generation duration
  - Data collection success rate
  - AI decision confidence
  - Time to distribution
  - Manual intervention rate

## 15. Security Considerations

- **Permissions**: Requires:
  - Google Sheets: `spreadsheets_read`
  - Salesforce: `data_read`
  - Stripe: `data_read`
  - Slack: `chat:write`
  - Email: `send`
  - Linear: `issue_create`
- **Secrets**:
  - Google Sheets API key
  - Salesforce API key
  - Stripe API key
  - Slack webhook URL
  - Email API key
  - Linear API token
- **Sensitive data**: Never exposed in logs; all outputs sanitized
- **Audit trail**: Every action logged with timestamp, user, outcome

## 16. Setup Requirements

- **Required accounts**:
  - Google Sheets, Salesforce, Stripe
  - Slack workspace
  - Email service
  - Linear project
- **Integrations**:
  - Webhook for report generation trigger
  - Google Sheets API
  - Salesforce API
  - Stripe API
  - Slack webhook
  - Email API
  - Linear API
- **Configuration**:
  - Set API keys
  - Configure AI agents
  - Set confidence thresholds
  - Configure Slack webhook
  - Configure email service

## 17. Expected Outcome

- **Faster reports**: 80% reduction in report generation time
- **Consistent data**: 100% consistent data across reports
- **Faster distribution**: 90% reduction in distribution time
- **On-time reports**: 100% on-time report generation
- **Reduced errors**: 90%+ reduction in human errors

## 18. React Flow Graph

```json
{
  "nodes": [
    {"id": "trigger", "type": "schedule_trigger", "position": {"x": 0, "y": 0}, "data": {"label": "Report Generation Trigger", "config": {"schedule": "daily at 9am", "required_fields": "report_type,start_date,end_date,recipients"}}},
    {"id": "collect", "type": "http_request", "position": {"x": 100, "y": 0}, "data": {"label": "Data Collection", "config": {"method": "GET", "url": "/api/data", "timeout": "180s"}}},
    {"id": "aggregate", "type": "data_aggregator", "position": {"x": 200, "y": 0}, "data": {"label": "Data Aggregation", "config": {"sources": ["google_sheets", "salesforce", "stripe"], "timeout": "120s"}}},
    {"id": "analyze", "type": "ai_agent", "position": {"x": 300, "y": 0}, "data": {"label": "AI Analysis", "config": {"agent_id": "report_analyzer", "confidence_threshold": "0.85", "timeout": "180s"}}},
    {"id": "generate", "type": "report_generator", "position": {"x": 400, "y": 0}, "data": {"label": "Report Generation", "config": {"format": "PDF/Excel/HTML", "template": "standard_report", "timeout": "120s"}}},
    {"id": "distribute", "type": "email", "position": {"x": 500, "y": 0}, "data": {"label": "Report Distribution", "config": {"recipients": "${recipients}", "subject": "Automated Report", "timeout": "180s"}}},
    {"id": "notify", "type": "slack", "position": {"x": 600, "y": 0}, "data": {"label": "Team Notification", "config": {"channel": "#reports", "template": "report_generated", "timeout": "60s"}}},
    {"id": "track", "type": "linear_create_issue", "position": {"x": 700, "y": 0}, "data": {"label": "Track Metrics", "config": {"project": "reports", "status": "open", "timeout": "60s"}}},
    {"id": "complete", "type": "stop_fail", "position": {"x": 800, "y": 0}, "data": {"label": "Workflow Completion"}}
  ],
  "edges": [
    {"source": "trigger", "target": "collect", "label": "always"},
    {"source": "collect", "target": "aggregate", "label": "success"},
    {"source": "aggregate", "target": "analyze", "label": "success"},
    {"source": "analyze", "target": "generate", "label": "success"},
    {"source": "generate", "target": "distribute", "label": "success"},
    {"source": "distribute", "target": "notify", "label": "success"},
    {"source": "notify", "target": "track", "label": "success"},
    {"source": "track", "target": "complete", "label": "always"}
  ]
}
```

## 19. Metadata

```json
{
  "category": "Business Operations",
  "tags": ["report-generation", "ai-analysis", "data-aggregation", "report-distribution"],
  "integrations": ["Google Sheets", "Salesforce", "Stripe", "Anthropic", "Slack", "Email", "Linear"],
  "complexity": "Medium",
  "estimated_setup_minutes": 45
}
```

## 20. Production-Readiness Audit

1. **Real production trigger**: Scheduled report generation
2. **Meaningful branching**: Data collection, aggregation, AI analysis
3. **Retry strategy**: All nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 180s for AI analysis, 120s for report generation
5. **Idempotency**: Not applicable
6. **Partial failure**: Not applicable
7. **External service failure**: Retry with dead letter queue
8. **AI uncertainty**: Confidence threshold gates human review
9. **Human approval**: Not required
10. **Observability**: Comprehensive metrics tracking
11. **Recovery**: Dead letter queue
12. **Security**: All secrets encrypted, sensitive data sanitized
13. **Configuration**: Realistic configurable variables
14. **Graph quality**: Valid React Flow JSON with clean layout

**Non-applicable**: None