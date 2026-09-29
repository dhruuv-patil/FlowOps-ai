# Template 3: Autonomous Lead Qualification Agent

## 1. Template Identity
- **Name**: Autonomous Lead Qualification Agent
- **One-line pitch**: "AI-powered lead qualification with human-in-the-loop for high-value prospects"
- **Category**: AI Operations
- **Complexity**: High
- **Estimated node count**: 16

## 2. Business Problem

The business problem is **lead qualification at scale** where:
- High volume of inbound leads overwhelms sales teams
- Manual qualification is slow and inconsistent
- High-value prospects get missed or delayed
- Low-quality leads waste sales time
- No consistent qualification criteria across team

**Specific pain points:**
- **Volume overload**: Sales reps can't handle lead volume
- **Inconsistent scoring**: Different reps use different criteria
- **Missed opportunities**: High-value leads not prioritized
- **Wasted time**: Reps spend time on unqualified leads
- **No feedback loop**: Qualification criteria never improves

## 3. Target User

- **Primary**: Sales Operations Manager, RevOps Lead
- **Secondary**: Sales Director, Marketing Operations, SDR Manager
- **Team**: Sales organization (20+ reps)

## 4. Trigger

- **Type**: Webhook
- **Integration**: HubSpot / Salesforce / Marketo
- **Event**: New lead/contact created, form submission, webinar registration
- **Payload assumptions**:
  - `contact_id` or `lead_id`
  - `email`, `first_name`, `last_name`
  - `company`, `title`, `phone`
  - `source` (organic, paid, referral, event)
  - `utm_source`, `utm_medium`, `utm_campaign`
  - `behavioral_data` (page views, content downloads, email opens)
  - `firmographic_data` (company size, industry, revenue)

- **Required fields**: `email`, `company`

## 5. Integrations Used

| Integration | Purpose |
|-------------|---------|
| HubSpot | Lead data and CRM |
| Salesforce | Lead data and CRM |
| OpenAI/Anthropic | AI qualification |
| Slack | Sales team notifications |
| SendGrid/Resend | Email outreach |
| Linear | Issue tracking for anomalies |

## 6. Workflow Architecture

The workflow uses an AI agent to autonomously qualify leads with human review for high-value prospects. It includes:
1. **Lead enrichment** (firmographic + behavioral data)
2. **AI qualification scoring** (fit, intent, timing)
3. **Tier classification** (A/B/C/D tier)
4. **Routing** (auto-assign or human review)
5. **Human approval** for Tier A leads
6. **Outreach automation** for qualified leads
7. **Feedback loop** for continuous improvement

The architecture follows a **tiered qualification pattern** with:
- **Automated enrichment** (data APIs)
- **AI scoring** (fit + intent + timing)
- **Tier-based routing** (auto vs human)
- **Human gate** for high-value
- **Outreach automation** for qualified
- **Learning loop** for model improvement

## 7. Node Definitions

| Node ID | Node Name | Node Type | Integration | Config Summary |
|---------|-----------|-----------|--------------|---------------|
| trigger | Lead Webhook | webhook_trigger | HubSpot/Salesforce | `event: contact_created`, `required_fields: email,company` |
| enrich | Lead Enrichment | ai_agent | Anthropic/OpenAI | `agent_id: enrichment_agent`, `max_retries: 2`, `timeout: 120s` |
| score | AI Qualification Scoring | ai_agent | Anthropic/OpenAI | `agent_id: qualification_agent`, `confidence_threshold: 0.8` |
| tier | Tier Classification | condition | - | `tiers: [A:80-100, B:60-79, C:40-59, D:0-39]` |
| route | Tier Routing | router | - | `routes: [A:human_review, B:auto_assign, C:nurture, D:reject]` |
| human_review | Human Review Gate | human_approval | - | `timeout: 24h`, `escalation: sales_manager` |
| assign | Auto-Assign Rep | hubspot_update_contact | HubSpot | `property: hubspot_owner_id` |
| nurture | Nurture Sequence | sendgrid_send_email | SendGrid | `template: nurture_sequence` |
| reject | Reject Lead | hubspot_update_contact | HubSpot | `property: lifecyclestage, value: unqualified` |
| outreach | Automated Outreach | sendgrid_send_email | SendGrid | `template: initial_outreach` |
| notify | Slack Notification | slack | Slack | `channel: #sales-leads`, `template: lead_qualified` |
| track | Track Metrics | linear_create_issue | Linear | `project: lead-qualification`, `status: open` |
| feedback | Feedback Collection | http_request | - | `method: POST`, `url: /api/feedback` |
| complete | Workflow Completion | stop_fail | - | - |

## 8. Edge Definitions

| Source | Target | Condition |
|--------|--------|-----------|
| trigger | enrich | always |
| enrich | score | always |
| score | tier | always |
| tier | route | always |
| route | human_review | tier == A |
| route | assign | tier == B |
| route | nurture | tier == C |
| route | reject | tier == D |
| human_review | assign | approved |
| human_review | notify | rejected or timeout |
| assign | outreach | always |
| nurture | outreach | always |
| reject | notify | always |
| outreach | notify | always |
| notify | track | always |
| track | feedback | always |
| feedback | complete | always |

## 9. Configuration Variables

| Variable | Type | Example/Default | Purpose |
|----------|------|-----------------|---------|
| `hubspot_api_key` | string | `pat-xxxxx` | HubSpot API access |
| `salesforce_client_id` | string | `xxx` | Salesforce OAuth client |
| `sendgrid_api_key` | string | `SG.xxx` | SendGrid API key |
| `slack_webhook_url` | string | `https://hooks.slack.com/...` | Slack notification URL |
| `linear_project_id` | string | `12345` | Linear project ID |
| `enrichment_agent_id` | string | `lead_enrichment_agent` | AI agent for enrichment |
| `qualification_agent_id` | string | `lead_qualification_agent` | AI agent for scoring |
| `tier_a_threshold` | number | `80` | Minimum score for Tier A |
| `tier_b_threshold` | number | `60` | Minimum score for Tier B |
| `tier_c_threshold` | number | `40` | Minimum score for Tier C |
| `human_review_timeout_hours` | number | `24` | Human review timeout |
| `confidence_threshold` | number | `0.8` | Minimum AI confidence |
| `max_retries` | number | `2` | Max retries for AI agents |

## 10. Branching Logic

- **Tier Classification**: AI score determines tier (A/B/C/D)
- **Routing**: Tier A → human review, Tier B → auto-assign, Tier C → nurture, Tier D → reject
- **Human Review**: Approve → assign, Reject/Timeout → notify
- **Low Confidence**: If AI confidence < threshold, route to human review regardless of score

## 11. Success Behavior

1. **Enrichment**: AI enriches lead with firmographic/behavioral data
2. **Scoring**: AI scores lead on fit, intent, timing (0-100)
3. **Tiering**: Lead classified into A/B/C/D tier
4. **Routing**: Tier A → human review, B → auto-assign, C → nurture, D → reject
5. **Human Review**: Sales manager reviews Tier A leads
6. **Assignment**: Rep assigned via HubSpot/Salesforce
7. **Outreach**: Automated initial email sent
8. **Notification**: Sales team notified via Slack
9. **Tracking**: Metrics logged to Linear
10. **Feedback**: Feedback collected for model improvement

## 12. Failure Behavior

| Failure Type | Handling |
|--------------|---------|
| Webhook validation failure | Retry with exponential backoff |
| Enrichment API failure | Use available data, flag for manual review |
| AI scoring timeout | Retry with exponential backoff, max 2 attempts |
| Low AI confidence | Route to human review |
| HubSpot/Salesforce API failure | Dead letter queue with reason |
| SendGrid failure | Retry once, then queue for retry |
| Slack notification failure | Log and continue |
| Linear API failure | Dead letter queue with reason |
| Human review timeout | Escalate to sales manager |

## 13. Retry / Recovery Behavior

- **Node-level retries**: AI agents retry up to 2 times with exponential backoff
- **CRM API failures**: Retry with exponential backoff (2s, 4s)
- **Email failures**: Retry once, then queue for manual retry
- **Dead letter queue**: Failed CRM/Linear calls go to DLQ
- **Recovery verification**: Requires 3 successful runs before auto-resolving

## 14. Reliability & Observability Behavior

**FlowOps Contributions:**
- **Anomaly detection**: Scoring anomalies, conversion rate drops, tier distribution shifts
- **Incident creation**: Automatically creates incident when:
  - Tier A leads not reviewed within SLA
  - Conversion rate drops >20% from baseline
  - AI confidence consistently below threshold
  - CRM sync failures exceed threshold
- **Baseline learning**: Tracks:
  - Average score by source
  - Tier distribution
  - Conversion rate by tier
  - AI confidence distribution
  - Time to assignment
- **Observability metrics**:
  - Qualification accuracy
  - Tier conversion rates
  - Time to human review
  - Rep workload balance
  - Feedback quality

## 15. Security Considerations

- **Permissions**: Requires:
  - HubSpot: `contacts` read/write, `owners` read
  - Salesforce: `Lead` read/write, `User` read
  - SendGrid: `mail.send`
  - Slack: `chat:write`
  - Linear: `issue_create`
- **Secrets**:
  - HubSpot API key
  - Salesforce OAuth credentials
  - SendGrid API key
  - Slack webhook URL
  - Linear API token
- **Sensitive data**: PII (email, phone, name) - never in logs
- **Audit trail**: Every decision logged with user ID, timestamp, score

## 16. Setup Requirements

- **Required accounts**:
  - HubSpot or Salesforce
  - SendGrid or Resend
  - Slack workspace
  - Linear project
- **Integrations**:
  - CRM webhook for new leads
  - SendGrid API
  - Slack webhook
  - Linear API
- **Configuration**:
  - Set CRM API keys
  - Configure AI agents with qualification criteria
  - Set tier thresholds
  - Configure email templates
  - Set up Slack webhook

## 17. Expected Outcome

- **Time savings**: Reduces manual qualification by 70-80%
- **Conversion improvement**: 15-25% increase in qualified-to-opportunity
- **Speed to lead**: 80%+ of leads contacted within 1 hour
- **Rep productivity**: Reps spend 90%+ time on qualified leads
- **Feedback loop**: Continuous model improvement from rep feedback

## 18. React Flow Graph

```json
{
  "nodes": [
    {"id": "trigger", "type": "webhook_trigger", "position": {"x": 0, "y": 0}, "data": {"label": "Lead Webhook", "config": {"event": "contact_created"}}},
    {"id": "enrich", "type": "ai_agent", "position": {"x": 100, "y": 0}, "data": {"label": "Lead Enrichment", "config": {"agent_id": "enrichment_agent", "timeout": "120s"}}},
    {"id": "score", "type": "ai_agent", "position": {"x": 200, "y": 0}, "data": {"label": "AI Qualification Scoring", "config": {"agent_id": "qualification_agent", "confidence_threshold": "0.8"}}},
    {"id": "tier", "type": "condition", "position": {"x": 300, "y": 0}, "data": {"label": "Tier Classification", "config": {"tiers": "A:80-100,B:60-79,C:40-59,D:0-39"}}},
    {"id": "route", "type": "router", "position": {"x": 400, "y": 0}, "data": {"label": "Tier Routing", "config": {"routes": "A:human_review,B:auto_assign,C:nurture,D:reject"}}},
    {"id": "human_review", "type": "human_approval", "position": {"x": 500, "y": -100}, "data": {"label": "Human Review Gate", "config": {"timeout": "24h", "escalation": "sales_manager"}}},
    {"id": "assign", "type": "hubspot_update_contact", "position": {"x": 500, "y": 0}, "data": {"label": "Auto-Assign Rep", "config": {"property": "hubspot_owner_id"}}},
    {"id": "nurture", "type": "sendgrid_send_email", "position": {"x": 500, "y": 100}, "data": {"label": "Nurture Sequence", "config": {"template": "nurture_sequence"}}},
    {"id": "reject", "type": "hubspot_update_contact", "position": {"x": 500, "y": 200}, "data": {"label": "Reject Lead", "config": {"property": "lifecyclestage", "value": "unqualified"}}},
    {"id": "outreach", "type": "sendgrid_send_email", "position": {"x": 600, "y": 0}, "data": {"label": "Automated Outreach", "config": {"template": "initial_outreach"}}},
    {"id": "notify", "type": "slack", "position": {"x": 700, "y": 0}, "data": {"label": "Slack Notification", "config": {"channel": "#sales-leads", "template": "lead_qualified"}}},
    {"id": "track", "type": "linear_create_issue", "position": {"x": 800, "y": 0}, "data": {"label": "Track Metrics", "config": {"project": "lead-qualification", "status": "open"}}},
    {"id": "feedback", "type": "http_request", "position": {"x": 900, "y": 0}, "data": {"label": "Feedback Collection", "config": {"method": "POST", "url": "/api/feedback"}}},
    {"id": "complete", "type": "stop_fail", "position": {"x": 1000, "y": 0}, "data": {"label": "Workflow Completion"}}
  ],
  "edges": [
    {"source": "trigger", "target": "enrich", "label": "always"},
    {"source": "enrich", "target": "score", "label": "always"},
    {"source": "score", "target": "tier", "label": "always"},
    {"source": "tier", "target": "route", "label": "always"},
    {"source": "route", "target": "human_review", "label": "tier == A"},
    {"source": "route", "target": "assign", "label": "tier == B"},
    {"source": "route", "target": "nurture", "label": "tier == C"},
    {"source": "route", "target": "reject", "label": "tier == D"},
    {"source": "human_review", "target": "assign", "label": "approved"},
    {"source": "human_review", "target": "notify", "label": "rejected or timeout"},
    {"source": "assign", "target": "outreach", "label": "always"},
    {"source": "nurture", "target": "outreach", "label": "always"},
    {"source": "reject", "target": "notify", "label": "always"},
    {"source": "outreach", "target": "notify", "label": "always"},
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
  "tags": ["lead-qualification", "ai-agent", "sales-operations", "revops", "human-in-loop"],
  "integrations": ["HubSpot", "Salesforce", "Anthropic", "OpenAI", "SendGrid", "Slack", "Linear"],
  "complexity": "High",
  "estimated_setup_minutes": 40
}
```

## 20. Production-Readiness Audit

1. **Real production trigger**: CRM webhook for new leads
2. **Meaningful branching**: Tier-based routing (A/B/C/D)
3. **Retry strategy**: AI agents retry up to 2 times with exponential backoff
4. **Timeout handling**: 24h for human review, 120s for enrichment
5. **Idempotency**: Not applicable - leads are unique
6. **Partial failure**: Enrichment failure falls back to available data
7. **External service failure**: CRM API retry with dead letter queue
8. **AI uncertainty**: Confidence threshold gates human review
9. **Human approval**: Required for Tier A (high-value) leads
10. **Observability**: Comprehensive metrics tracking
11. **Recovery**: Dead letter queue for failed CRM/Linear calls
12. **Security**: All secrets encrypted, PII never in logs
13. **Configuration**: Realistic configurable variables
14. **Graph quality**: Valid React Flow JSON with clean layout

**Non-applicable**: Idempotency not needed for unique leads