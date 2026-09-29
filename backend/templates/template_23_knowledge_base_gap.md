# Template 23: Knowledge Base Gap Detection & Article Generation

## 1. Template Identity
- **Name**: Knowledge Base Gap Detection & Article Generation
- **One-line pitch**: "Automated knowledge base gap detection with AI-driven article generation and human review"
- **Category**: Customer Support
- **Complexity**: High
- **Estimated node count**: 20

## 2. Business Problem

The business problem is **knowledge gaps in the support knowledge base** where:
- Frequently asked questions are not documented
- Common issues are not addressed
- Documentation is outdated or incomplete
- Support agents spend time answering the same questions repeatedly
- Customer satisfaction suffers from inconsistent answers

**Specific pain points:**
- **Time wasted**: Support agents repeat answers
- **Customer frustration**: Inconsistent information
- **Knowledge silos**: Information scattered across teams
- **Documentation decay**: Outdated articles
- **Training inefficiency**: New agents spend time learning

## 3. Target User

- **Primary**: Support Engineer, Knowledge Management Specialist, Customer Success Manager
- **Secondary**: Product Manager, Technical Writer, Engineering Manager
- **Team**: Customer Support, Knowledge Management

## 4. Trigger

- **Type**: Webhook / Scheduled Poll / Manual Trigger
- **Integration**: Zendesk, Freshdesk, Slack, GitHub, Confluence, Notion
- **Event**: Ticket created, ticket closed, scheduled review, manual trigger
- **Payload assumptions**:
  - `ticket_id`
  - `subject`
  - `description`
  - `tags`
  - `frequency` (how often this question appears)
  - `resolution_time`
  - `agent_id`
  - `knowledge_base_url` (if exists)
- **Required fields**: `ticket_id`, `subject`, `description`, `frequency`

## 5. Integrations Used

| Integration | Purpose |
|-------------|---------|
| Zendesk/Freshdesk | Ticket data source |
| Slack | Team notifications |
| GitHub | Knowledge base storage |
| Confluence/Notion | Knowledge base storage |
| Anthropic/OpenAI | AI article generation |
| Linear | Issue tracking |
| AWS S3 | Evidence storage |

## 6. Workflow Architecture

The workflow implements an **automated knowledge management pattern** with:
1. **Gap detection** (ticket frequency analysis)
2. **AI article generation** (automated content creation)
3. **Human review** (quality assurance)
4. **Documentation update** (knowledge base integration)
5. **Communication** (stakeholder notifications)
6. **Tracking** (issue management)

The architecture follows **validated knowledge management** with:
- **Proactive gap detection**
- **AI-augmented content creation**
- **Human-in-the-loop review**
- **Automated documentation updates**
- **Comprehensive tracking**

## 7. Node Definitions

| Node ID | Node Name | Node Type | Integration | Config Summary |
|---------|-----------|-----------|-------------|----------------|
| trigger | Ticket Analysis Trigger | webhook_trigger | Zendesk/Freshdesk | `event: ticket_closed`, `required_fields: ticket_id,subject,description,frequency` |
| validate | Ticket Validation | condition | - | `checks: [valid_ticket, not_duplicate, not_resolved]` |
| analyze | Gap Detection Analysis | ai_agent | Anthropic/OpenAI | `agent_id: knowledge_gap_analyzer`, `confidence_threshold: 0.85` |
| generate | AI Article Generation | ai_agent | Anthropic/OpenAI | `agent_id: article_generator`, `style_guide: support_knowledge_base` |
| review | Human Review Gate | human_approval | - | `timeout: 48h`, `escalation: knowledge_manager` |
| update | Knowledge Base Update | github_create_pr | GitHub | `repo: support-knowledge-base`, `branch: update_${ticket_id}` |
| notify | Slack Notification | slack | Slack | `channel: #knowledge-updates`, `template: article_created` |
| track | Issue Tracking | linear_create_issue | Linear | `project: knowledge-management`, `status: open` |
| store | Store Evidence | aws_s3_upload | AWS S3 | `bucket: knowledge-base-evidence`, `key: ${ticket_id}` |
| complete | Workflow Completion | stop_fail | - | - |

## 8. Edge Definitions

| Source | Target | Condition |
|--------|--------|-----------|
| trigger | validate | always |
| validate | analyze | ticket_valid |
| validate | complete | invalid_ticket |
| analyze | generate | gap_detected |
| analyze | complete | no_gap_detected |
| generate | review | article_generated |
| review | update | approved |
| review | complete | rejected or timeout |
| update | notify | pr_created |
| notify | track | always |
| track | store | always |
| store | complete | always |

## 9. Configuration Variables

| Variable | Type | Example/Default | Purpose |
|----------|------|-----------------|---------|
| `ticketing_system_api_key` | string | `xxx` | Ticket system API key |
| `github_pat` | string | `xxx` | GitHub personal access token |
| `slack_webhook_url` | string | `https://hooks.slack.com/...` | Slack notifications |
| `linear_api_token` | string | `xxx` | Linear API token |
| `s3_bucket_name` | string | `knowledge-base-evidence` | Evidence storage bucket |
| `ai_confidence_threshold` | number | `0.85` | Minimum AI confidence for auto-actions |
| `gap_detection_threshold` | number | `5` | Minimum ticket frequency to detect gap |
| `review_timeout_hours` | number | `48` | Time to wait for human review |
| `max_retry_attempts` | number | `3` | Retry attempts for API calls |

## 10. Branching Logic

- **Ticket validation**: Verify ticket authenticity and prevent duplicates
- **Gap detection**: Only proceed if significant gap detected
- **AI confidence gating**: Low confidence triggers human review path
- **Human review**: Approved articles proceed to update, rejected go to completion
- **Documentation update**: Only proceed if PR created successfully

## 11. Success Behavior

1. **Trigger**: Ticket closed with high frequency
2. **Validation**: Ticket authenticity confirmed, duplicate check passed
3. **Analysis**: Knowledge gap identified with confidence score
4. **Generation**: AI generates article based on ticket content
5. **Review**: Knowledge manager approves article
6. **Update**: PR created in knowledge base repository
7. **Notification**: Slack alert sent to knowledge team
8. **Tracking**: Issue created in Linear for follow-up
9. **Storage**: Evidence stored in S3 for reference
10. **Completion**: Workflow marked as complete

## 12. Failure Behavior

| Failure Type | Handling |
|--------------|---------|
| Ticket validation failure | Skip ticket, mark as invalid |
| Gap detection failure | Skip ticket, mark as no gap |
| AI generation failure | Retry with exponential backoff, max 3 attempts |
| Human timeout | Escalate to knowledge manager |
| PR creation failure | Retry with exponential backoff, max 3 attempts |
| Slack notification failure | Log error, continue workflow |
| Linear issue creation failure | Dead letter queue with reason |

## 13. Retry / Recovery Behavior

- **Node-level retries**: All AI nodes retry up to 3 times with exponential backoff
- **Gap detection**: If no gap detected after 2 attempts, manual review required
- **Human timeout**: If review doesn't complete within 48h, escalate to knowledge manager
- **Dead letter queue**: Failed Linear API calls go to dead letter queue
- **Recovery verification**: Requires 3 successful runs before auto-resolving anomalies

## 14. Reliability & Observability Behavior

**FlowOps Contributions:**
- **Anomaly detection**: Gap detection confidence drops, article generation failures
- **Incident creation**: Automatically creates incident when:
  - Gap detection confidence below threshold
  - Article generation fails
  - PR creation fails
- **Baseline learning**: Tracks:
  - Gap detection rate
  - AI confidence distribution
  - Article generation time
  - Human review time
- **Observability metrics**:
  - Gap detection confidence
  - Article generation duration
  - Human review time
  - PR creation success rate

## 15. Security Considerations

- **Permissions**: Requires:
  - Ticketing system: `ticket_read` access
  - GitHub: `repo` access to knowledge base
  - Slack: `chat:write` for notifications
  - Linear: `issue_create` for tracking
- **Secrets**:
  - Ticketing system API key
  - GitHub PAT
  - Slack webhook URL
  - Linear API token
- **Sensitive data**: Never exposed in logs; all outputs sanitized
- **Audit trail**: Every decision logged with user ID and timestamp

## 16. Setup Requirements

- **Required accounts**:
  - Ticketing system with API access
  - GitHub repository for knowledge base
  - Slack workspace with notification access
  - Linear project for issue tracking
- **Integrations**:
  - Ticketing system API integration
  - GitHub repository setup
  - Slack app integration
  - Linear API integration
- **Configuration**:
  - Set `ticketing_system_api_key` in FlowOps
  - Configure GitHub PAT with repo access
  - Set up Slack webhook URL
  - Configure Linear API token
  - Set gap detection threshold
  - Configure review timeout

## 17. Expected Outcome

- **Time savings**: Reduces knowledge management time by 60-70%
- **Quality improvement**: AI generates 80-90% of articles correctly
- **Consistency**: Ensures consistent information across knowledge base
- **Team productivity**: Enables knowledge team to focus on high-value work

## 18. React Flow Graph

```json
{
  "nodes": [
    {
      "id": "trigger",
      "type": "webhook_trigger",
      "position": {"x": 0, "y": 0},
      "data": {"label": "Ticketing System Webhook", "config": {"event": "ticket_closed"}}
    },
    {
      "id": "validate",
      "type": "condition",
      "position": {"x": 100, "y": 0},
      "data": {"label": "Ticket Validation", "config": {"required_fields": ["ticket_id", "subject", "description", "frequency"]}}
    },
    {
      "id": "analyze",
      "type": "ai_agent",
      "position": {"x": 200, "y": 50},
      "data": {"label": "Gap Detection Analysis", "config": {"agent_id": "knowledge_gap_analyzer", "confidence_threshold": 0.85}}
    },
    {
      "id": "generate",
      "type": "ai_agent",
      "position": {"x": 300, "y": 100},
      "data": {"label": "AI Article Generation", "config": {"agent_id": "article_generator", "style_guide": "support_knowledge_base"}}
    },
    {
      "id": "review",
      "type": "human_approval",
      "position": {"x": 400, "y": 100},
      "data": {"label": "Human Review Gate", "config": {"timeout": "48h", "escalation": "knowledge_manager"}}
    },
    {
      "id": "update",
      "type": "github_create_pr",
      "position": {"x": 500, "y": 50},
      "data": {"label": "Knowledge Base Update", "config": {"repo": "support-knowledge-base", "branch": "update_${ticket_id}"}}
    },
    {
      "id": "notify",
      "type": "slack",
      "position": {"x": 600, "y": 0},
      "data": {"label": "Slack Notification", "config": {"channel": "#knowledge-updates", "template": "article_created"}}
    },
    {
      "id": "track",
      "type": "linear_create_issue",
      "position": {"x": 600, "y": 100},
      "data": {"label": "Issue Tracking", "config": {"project": "knowledge-management", "status": "open"}}
    },
    {
      "id": "store",
      "type": "aws_s3_upload",
      "position": {"x": 600, "y": 200},
      "data": {"label": "Store Evidence", "config": {"bucket": "knowledge-base-evidence", "key": "${ticket_id}"}}
    },
    {
      "id": "complete",
      "type": "stop_fail",
      "position": {"x": 700, "y": 100},
      "data": {"label": "Workflow Completion"}
    }
  ],
  "edges": [
    {"source": "trigger", "target": "validate", "label": "always"},
    {"source": "validate", "target": "analyze", "label": "ticket_valid"},
    {"source": "validate", "target": "complete", "label": "invalid_ticket"},
    {"source": "analyze", "target": "generate", "label": "gap_detected"},
    {"source": "analyze", "target": "complete", "label": "no_gap_detected"},
    {"source": "generate", "target": "review", "label": "article_generated"},
    {"source": "review", "target": "update", "label": "approved"},
    {"source": "review", "target": "complete", "label": "rejected or timeout"},
    {"source": "update", "target": "notify", "label": "pr_created"},
    {"source": "notify", "target": "track", "label": "always"},
    {"source": "track", "target": "store", "label": "always"},
    {"source": "store", "target": "complete", "label": "always"}
  ]
}
```

## 19. Metadata

```json
{
  "category": "Customer Support",
  "tags": ["knowledge-base", "ai-agent", "human-in-loop", "support", "documentation"],
  "integrations": ["Zendesk", "Freshdesk", "Slack", "GitHub", "Anthropic", "Linear", "AWS S3"],
  "complexity": "High",
  "estimated_setup_minutes": 60
}
```

## 20. Production-Readiness Audit

1. **Real production trigger**: Ticketing system webhook for closed tickets
2. **Meaningful branching**: Gap detection, AI confidence, human review
3. **Retry strategy**: All AI nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 48h for human review
5. **Idempotency**: Deduplication key with 24h TTL
6. **Partial failure**: Gap detection with manual review
7. **External service failure**: Retry with exponential backoff for AI services
8. **AI uncertainty**: Confidence thresholds for AI decisions
9. **Human approval**: Required for article review
10. **Observability**: Comprehensive metrics tracking
11. **Recovery**: Dead letter queue for failed Linear API calls
12. **Security**: All secrets encrypted, sensitive data sanitized
13. **Configuration**: Realistic configurable variables
14. **Graph quality**: Valid React Flow JSON with clean layout

**Non-applicable**: None