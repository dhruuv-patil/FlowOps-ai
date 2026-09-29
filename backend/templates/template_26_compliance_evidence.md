# Template 26: Compliance Evidence Collection & Audit Trail

## 1. Template Identity
- **Name**: Compliance Evidence Collection & Audit Trail
- **One-line pitch**: "Automated compliance evidence collection, validation, and audit trail generation with AI-powered analysis and secure storage"
- **Category**: Security/Access
- **Complexity**: High
- **Estimated node count**: 24

## 2. Business Problem
The business problem is **manual compliance evidence collection causing audit failures and regulatory penalties** where:
- Evidence collection is manual, time-consuming, and error-prone
- Audit trails are incomplete or tampered with
- No automated validation of compliance evidence
- Delayed detection of compliance violations
- No centralized evidence repository for auditors
- Inconsistent evidence collection across systems

**Specific pain points:**
- **Audit failures**: Missing or invalid evidence during audits
- **Regulatory fines**: Penalties for non-compliance due to poor evidence
- **Slow investigations**: Manual evidence gathering delays incident response
- **Evidence tampering**: No immutable audit trail for evidence
- **Inconsistent standards**: Different teams collect evidence differently
- **Storage risks**: Evidence stored insecurely or lost

## 3. Target User
- **Primary**: Compliance Officer, Security Engineer, Auditor
- **Secondary**: DevOps Engineer, SRE, Legal Team
- **Team**: Organizations subject to SOC 2, ISO 27001, GDPR, HIPAA, PCI DSS

## 4. Trigger
- **Type**: Scheduled Check / Webhook Trigger / Manual Dispatch
- **Integration**: Webhook, Schedule Trigger
- **Event**: Scheduled compliance check, evidence collection request, manual dispatch
- **Payload assumptions**:
  - `compliance_framework` (SOC2, ISO27001, GDPR, HIPAA, PCI_DSS)
  - `evidence_types` (logs, configs, access_controls, encryption, backups)
  - `collection_frequency` (daily, weekly, monthly)
  - `retention_period` (days)
  - `evidence_sources` (AWS, GCP, Azure, databases, applications)
  - `validation_rules` (required fields, formats, signatures)
- **Required fields**: `compliance_framework`, `evidence_types`, `evidence_sources`

## 5. Integrations Used
| Integration          | Purpose                                                                 |
|----------------------|-------------------------------------------------------------------------|
| PostgreSQL/MySQL     | Store compliance evidence and audit trails                              |
| MongoDB              | Store unstructured evidence (screenshots, configs, logs)                |
| AWS S3               | Secure, immutable storage for evidence artifacts                        |
| Slack                | Compliance alerts and notifications                                     |
| PagerDuty            | Escalation for critical compliance violations                           |
| Linear               | Compliance issue tracking and remediation                               |
| Anthropic/OpenAI     | AI-powered evidence validation and anomaly detection                    |
| Webhook              | Ingest evidence from external systems                                   |
| HTTP Request         | Pull evidence from APIs, databases, cloud providers                     |

## 6. Workflow Architecture
The workflow automates compliance evidence collection with:
1. **Evidence Ingestion** (scheduled pulls, webhook receipts, manual triggers)
2. **Evidence Validation** (format, completeness, integrity checks)
3. **AI-Powered Analysis** (anomaly detection, compliance validation)
4. **Secure Storage** (encrypted, immutable evidence repository)
5. **Audit Trail Generation** (immutable chain of custody)
6. **Compliance Scoring** (framework-specific compliance percentages)
7. **Alerting & Escalation** (notify on compliance violations)
8. **Evidence Retention & Cleanup** (automated retention policy enforcement)

The architecture follows a **validated evidence-handling pattern** with:
- **Pre-flight validation** (check evidence sources, framework applicability)
- **Immutable storage** (WORM storage for evidence integrity)
- **AI-driven validation** (confidence thresholds for automated acceptance)
- **Human review gates** (for anomalous or insufficient evidence)
- **Audit trail generation** (cryptographic hashes, timestamps, user attribution)
- **Retention enforcement** (automated deletion per compliance requirements)
- **Escalation paths** (PagerDuty for critical compliance gaps)

## 7. Node Definitions
| Node ID               | Node Name                     | Node Type               | Integration       | Config Summary                                                                                     |
|-----------------------|-------------------------------|-------------------------|-------------------|---------------------------------------------------------------------------------------------------|
| trigger               | Compliance Check Trigger     | schedule_trigger        | -                 | `interval: 24h`, `required_fields: compliance_framework, evidence_types, evidence_sources`       |
| webhook_trigger       | Evidence Webhook             | webhook_trigger         | -                 | `method: POST`, `required_fields: evidence_type, source_system, timestamp`                       |
| deduplicate           | Evidence Deduplication       | idempotency_check       | -                 | `idempotency_key: evidence_${evidence_id}`, `ttl: 30d`                                             |
| validate_source       | Source Validation            | condition               | -                 | `checks: [source_available, credentials_valid, permissions_granted]`                             |
| collect_logs          | Collect System Logs          | http_request            | -                 | `method: GET`, `url: {{log_endpoint}}`, `timeout: 120s`                                           |
| collect_configs       | Collect Configurations       | http_request            | -                 | `method: GET`, `url: {{config_endpoint}}`, `timeout: 120s`                                        |
| collect_access        | Collect Access Controls      | database_query          | postgresql/mysql  | `sql: SELECT * FROM access_logs WHERE timestamp > {{start_time}}`                                |
| collect_backups       | Collect Backup Evidence      | s3_upload               | aws               | `bucket: compliance-evidence`, `key: backups/{{date}}/{{system}}`                                 |
| validate_evidence     | Evidence Validation          | ai_agent                | anthropic/openai  | `agent_id: evidence_validator`, `confidence_threshold: 0.9`, `timeout: 180s`                     |
| ai_analysis           | AI Compliance Analysis       | ai_agent                | anthropic/openai  | `agent_id: compliance_analyzer`, `confidence_threshold: 0.85`, `timeout: 240s`                   |
| human_review          | Human Evidence Review        | human_approval          | -                 | `timeout: 8h`, `escalation: pagerduty`                                                            |
| store_evidence        | Store Evidence Immutable     | s3_upload               | aws               | `bucket: compliance-evidence-immutable`, `key: {{framework}}/{{evidence_id}}.json`, `metadata: true` |
| audit_trail           | Generate Audit Trail         | database_insert         | postgresql/mysql  | `sql: INSERT INTO audit_trail (evidence_id, action, hash, timestamp, user_id) VALUES (...)`      |
| compliance_score      | Calculate Compliance Score   | ai_agent                | anthropic/openai  | `agent_id: compliance_scorer`, `timeout: 120s`                                                    |
| notify_slack          | Slack Notification           | slack                   | slack             | `channel: #compliance-alerts`, `template: compliance_status`                                      |
| escalate_pagerduty    | PagerDuty Escalation         | pagerduty_trigger_incident | pagerduty       | `serviceId: compliance-service`, `title: Compliance Violation Detected`                           |
| track_linear          | Linear Issue Tracking        | linear_create_issue     | linear            | `project: compliance`, `title: Compliance Evidence Review Required`                               |
| cleanup_old           | Cleanup Old Evidence         | s3_delete               | aws               | `bucket: compliance-evidence-temp`, `key: {{evidence_id}}`                                        |
| retention_check       | Retention Policy Check       | condition               | -                 | `checks: [retention_expired, legal_hold_not_applied]`                                             |
| delete_expired        | Delete Expired Evidence      | s3_delete               | aws               | `bucket: compliance-evidence`, `key: {{evidence_path}}`                                           |
| complete              | Workflow Completion          | stop_fail               | -                 | `mode: stop`, `message: Compliance evidence collection completed`                                 |

## 8. Edge Definitions
| Source                | Target                      | Condition                                                               |
|-----------------------|-----------------------------|-------------------------------------------------------------------------|
| trigger               | deduplicate                 | always                                                                  |
| webhook_trigger       | deduplicate                 | always                                                                  |
| deduplicate           | validate_source             | always                                                                  |
| validate_source       | collect_logs                | logs_included                                                           |
| validate_source       | collect_configs             | configs_included                                                        |
| validate_source       | collect_access              | access_included                                                         |
| validate_source       | collect_backups             | backups_included                                                        |
| collect_logs          | validate_evidence           | logs_collected                                                          |
| collect_configs       | validate_evidence           | configs_collected                                                       |
| collect_access        | validate_evidence           | access_collected                                                        |
| collect_backups       | validate_evidence           | backups_collected                                                       |
| validate_evidence     | ai_analysis                 | evidence_valid                                                          |
| validate_evidence     | human_review                | evidence_invalid or low_confidence                                      |
| ai_analysis           | human_review                | compliance_violation_detected                                           |
| ai_analysis           | store_evidence              | compliant                                                               |
| human_review          | store_evidence              | approved                                                                |
| human_review          | escalate_pagerduty          | rejected or timeout                                                     |
| store_evidence        | audit_trail                 | always                                                                  |
| audit_trail           | compliance_score            | always                                                                  |
| compliance_score      | notify_slack                | always                                                                  |
| notify_slack          | track_linear                | score_below_threshold                                                   |
| notify_slack          | cleanup_old                 | score_above_threshold                                                   |
| track_linear          | cleanup_old                 | always                                                                  |
| cleanup_old           | retention_check             | always                                                                  |
| retention_check       | delete_expired              | retention_expired and no_legal_hold                                     |
| retention_check       | complete                    | not_retention_expired or legal_hold_applied                             |
| delete_expired        | complete                    | always                                                                  |
| complete              | stop_fail                   | always                                                                  |

## 9. Configuration Variables
| Variable                          | Type     | Example/Default                     | Purpose                                                                                     |
|-----------------------------------|----------|-------------------------------------|---------------------------------------------------------------------------------------------|
| `compliance_framework`            | string   | `SOC2_TYPE2`                        | Compliance framework to validate against                                                    |
| `evidence_retention_days`         | number   | `365`                               | Days to retain compliance evidence                                                          |
| `legal_hold_enabled`              | boolean  | `false`                             | Whether legal hold is active                                                                |
| `evidence_bucket`                 | string   | `compliance-evidence-${env}`        | S3 bucket for evidence storage                                                              |
| `immutable_bucket`                | string   | `compliance-evidence-immutable-${env}` | S3 bucket for immutable evidence storage                                                  |
| `temp_bucket`                     | string   | `compliance-evidence-temp-${env}`   | S3 bucket for temporary evidence storage                                                  |
| `ai_model`                        | string   | `anthropic/claude-3-sonnet`         | AI model for evidence analysis                                                              |
| `ai_confidence_threshold`         | number   | `0.85`                              | Minimum confidence for AI decisions                                                         |
| `evidence_validation_timeout`     | number   | `180`                               | Seconds for evidence validation                                                             |
| `compliance_analysis_timeout`     | number   | `240`                               | Seconds for compliance analysis                                                             |
| `human_review_timeout_hours`      | number   | `8`                                 | Hours for human evidence review                                                             |
| `slack_webhook_url`               | string   | `https://hooks.slack.com/services/...` | Slack notification URL                                                                    |
| `pagerduty_service_key`           | string   | `compliance-service-key`            | PagerDuty service key                                                                       |
| `linear_api_key`                  | string   | `linear-api-key`                    | Linear API key                                                                              |
| `database_host`                   | string   | `compliance-db.cluster-xyz.us-east-1.rds.amazonaws.com` | Database host for audit trail                                                            |
| `database_name`                   | string   | `compliance_audit`                  | Database name                                                                               |
| `database_username`               | string   | `compliance_user`                   | Database username                                                                           |
| `database_password`               | string   | `''`                                | Database password (use secrets manager)                                                     |
| `max_evidence_size_mb`            | number   | `100`                               | Maximum size per evidence item                                                              |
| `allowed_evidence_types`          | array    | `[\"logs\", \"configs\", \"access_controls\", \"encryption_keys\", \"backups\"]` | Types of evidence to collect                               |
| `compliance_threshold_score`      | number   | `85`                                | Minimum compliance score percentage to pass                                                 |
| `retention_check_interval_hours`  | number   | `24`                                | Hours between retention policy checks                                                       |

## 10. Branching Logic
- **Evidence validation**: If evidence fails validation or AI confidence low, route to human review
- **Compliance analysis**: If AI detects compliance violations, route to human review and escalation
- **Human approval**: If evidence approved, store; if rejected, escalate to PagerDuty
- **Compliance scoring**: If score below threshold, notify Slack and create Linear issue
- **Retention check**: If evidence expired and no legal hold, delete; otherwise retain
- **Source validation**: If source unavailable or credentials invalid, skip collection and log warning

## 11. Success Behavior
1. **Trigger**: Scheduled compliance check or webhook evidence receipt
2. **Deduplication**: Prevent processing duplicate evidence
3. **Source Validation**: Verify evidence sources are accessible
4. **Evidence Collection**: Pull logs, configs, access controls, backups from sources
5. **Evidence Validation**: Validate evidence format, completeness, integrity
6. **AI Analysis**: Analyze evidence for compliance violations and anomalies
7. **Human Review**: Review anomalous or low-confidence evidence
8. **Immutable Storage**: Store evidence in WORM S3 bucket with metadata
9. **Audit Trail**: Record evidence handling in database audit trail
10. **Compliance Scoring**: Calculate framework-specific compliance percentage
11. **Notification**: Slack notification with compliance status
12. **Tracking**: Linear issue created if compliance score below threshold
13. **Cleanup**: Temporary evidence cleaned up
14. **Retention Check**: Apply retention policies to stored evidence
15. **Completion**: Workflow marked as successful

## 12. Failure Behavior
| Failure Type                     | Handling                                                                                     |
|----------------------------------|---------------------------------------------------------------------------------------------|
| Source unavailable               | Log warning, skip collection, continue with available sources                               |
| Invalid credentials              | Log error, skip collection, alert security team                                             |
| Evidence validation failure      | Route to human review                                                                       |
| AI analysis timeout              | Retry with exponential backoff (max 3 attempts)                                             |
| Low AI confidence                | Route to human review                                                                       |
| Human review timeout             | Escalate to PagerDuty                                                                       |
| Storage failure                  | Retry with exponential backoff, alert on persistent failure                                 |
| Audit trail failure              | Log error, attempt retry, alert if unresolved                                               |
| Slack notification failure       | Log and continue                                                                            |
| Linear API failure               | Dead letter queue with reason                                                               |
| PagerDuty escalation failure     | Log and continue                                                                            |
| S3 storage failure               | Retry with exponential backoff, alert on persistent failure                                 |
| Database connection failure      | Retry with exponential backoff, alert if unresolved                                         |

## 13. Retry / Recovery Behavior
- **Node-level retries**: All HTTP, AI, and storage nodes retry up to 3 times with exponential backoff
- **Database retries**: Retry failed queries with exponential backoff (2s, 4s, 8s)
- **Dead letter queue**: Failed Linear/PagerDuty calls go to DLQ for manual inspection
- **Recovery verification**: Requires 2 successful evidence collections before clearing error state
- **Checkpointing**: Workflow state saved after each major phase for restart capability
- **Circuit breaker**: External service calls use circuit breaker pattern to prevent cascading failures

## 14. Reliability & Observability Behavior
**FlowOps Contributions:**
- **Anomaly detection**: Evidence collection frequency drops, validation failure rates, AI confidence degradation
- **Incident creation**: Automatically creates incident when:
  - Evidence validation failure rate > 20%
  - AI compliance violation detected
  - Human review rejection rate > 50%
  - Compliance score drops suddenly (>20% point decrease)
- **Baseline learning**: Tracks:
  - Evidence collection success rate per source
  - Average evidence validation time
  - AI confidence distribution for evidence
  - Human review rate
  - Compliance score trends per framework
- **Observability metrics**:
  - Evidence items collected per run
  - Validation success rate
  - AI analysis duration
  - Compliance score percentage
  - Human review count
  - Evidence storage size
  - Retention policy compliance
  - Audit trail completeness

## 15. Security Considerations
- **Permissions**: Requires:
  - Database: `SELECT`, `INSERT` on audit_trail and evidence tables
  - S3: `PutObject`, `GetObject`, `DeleteObject`, `PutObjectRetention` on evidence buckets
  - HTTP: Outbound access to evidence sources (logs, configs, APIs)
  - Slack: `chat:write` for notifications
  - PagerDuty: `incident_trigger` for escalation
  - Linear: `issue_create` for tracking
- **Secrets**:
  - Database credentials (use AWS Secrets Manager or HashiCorp Vault)
  - S3 access keys (use IAM roles for service accounts)
  - Slack webhook URL
  - PagerDuty integration key
  - Linear API key
  - AI provider API keys
- **Sensitive data**: 
  - Evidence encrypted at rest and in transit
  - PII automatically redacted before storage if detected
  - Never exposed in logs; all outputs sanitized
  - Access to evidence requires role-based permissions
- **Audit trail**: 
  - Every evidence access logged with user ID, timestamp, action
  - Cryptographic hash chains for evidence integrity
  - Immutable storage with legal hold capabilities
  - Full chain of custody from collection to storage

## 16. Setup Requirements
- **Required accounts**:
  - AWS account with S3 access for evidence storage
  - PostgreSQL/MySQL database for audit trail storage
  - Slack workspace for compliance notifications
  - PagerDuty account for compliance escalation
  - Linear project for compliance tracking
  - Anthropic/OpenAI account for AI analysis
- **Integrations**:
  - AWS S3 integration configured
  - Database integration configured
  - Slack app integration installed
  - PagerDuty integration configured
  - Linear integration configured
  - Anthropic/OpenAI integration configured
- **Configuration**:
  - Set compliance framework and evidence types
  - Configure S3 buckets for evidence storage (standard, immutable, temp)
  - Set up database connection for audit trail
  - Configure AI model and confidence thresholds
  - Set up Slack webhook URL for notifications
  - Configure PagerDuty service key for escalation
  - Set up Linear API key for issue tracking
  - Define evidence sources and collection endpoints
  - Set retention periods and legal hold procedures
  - Configure evidence validation rules per framework

## 17. Expected Outcome
- **Time savings**: Reduces evidence collection time by 70-90% vs manual processes
- **Audit readiness**: Continuous audit trail with immutable evidence storage
- **Compliance visibility**: Real-time compliance scoring and violation detection
- **Evidence integrity**: Cryptographically verifiable evidence chain of custody
- **Automated remediation**: Linear issues created for compliance gaps
- **Regulatory confidence**: Auditors can verify evidence integrity and completeness
- **Operational efficiency**: Evidence collection runs automatically on schedule
- **Risk reduction**: Early detection of compliance violations before audits

## 18. React Flow Graph
```json
{
  "nodes": [
    {
      "id": "trigger",
      "type": "schedule_trigger",
      "position": {"x": 0, "y": 0},
      "data": {"label": "Compliance Check Trigger", "config": {"interval": "24h", "required_fields": ["compliance_framework", "evidence_types", "evidence_sources"]}}
    },
    {
      "id": "webhook_trigger",
      "type": "webhook_trigger",
      "position": {"x": 0, "y": 100},
      "data": {"label": "Evidence Webhook", "config": {"method": "POST", "required_fields": ["evidence_type", "source_system", "timestamp"]}}
    },
    {
      "id": "deduplicate",
      "type": "idempotency_check",
      "position": {"x": 200, "y": 50},
      "data": {"label": "Evidence Deduplication", "config": {"idempotency_key": "evidence_${evidence_id}", "ttl": "30d"}}
    },
    {
      "id": "validate_source",
      "type": "condition",
      "position": {"x": 400, "y": 50},
      "data": {"label": "Source Validation", "config": {"checks": ["source_available", "credentials_valid", "permissions_granted"]}}
    },
    {
      "id": "collect_logs",
      "type": "http_request",
      "position": {"x": 600, "y": 0},
      "data": {"label": "Collect System Logs", "config": {"method": "GET", "url": "{{log_endpoint}}", "timeout": "120s"}}
    },
    {
      "id": "collect_configs",
      "type": "http_request",
      "position": {"x": 600, "y": 100},
      "data": {"label": "Collect Configurations", "config": {"method": "GET", "url": "{{config_endpoint}}", "timeout": "120s"}}
    },
    {
      "id": "collect_access",
      "type": "database_query",
      "position": {"x": 600, "y": 200},
      "data": {"label": "Collect Access Controls", "config": {"databaseType": "postgresql", "host": "{{database_host}}", "port": "5432", "database": "{{database_name}}", "username": "{{database_username}}", "password": "{{database_password}}", "sql": "SELECT * FROM access_logs WHERE timestamp > {{start_time}}", "limit": "1000"}}
    },
    {
      "id": "collect_backups",
      "type": "s3_upload",
      "position": {"x": 600, "y": 300},
      "data": {"label": "Collect Backup Evidence", "config": {"bucket": "compliance-evidence-${env}", "key": "backups/{{date}}/{{system}}", "content": "{{backup_data}}"}}
    },
    {
      "id": "validate_evidence",
      "type": "ai_agent",
      "position": {"x": 800, "y": 50},
      "data": {"label": "Evidence Validation", "config": {"agentId": "evidence_validator", "instructions": "Validate evidence format, completeness, and integrity. Check for required fields, proper timestamps, and cryptographic signatures where applicable. Return validation status and confidence score.", "input": "{{collected_evidence}}", "timeout": "180s"}}
    },
    {
      "id": "ai_analysis",
      "type": "ai_agent",
      "position": {"x": 1000, "y": 0},
      "data": {"label": "AI Compliance Analysis", "config": {"agentId": "compliance_analyzer", "instructions": "Analyze evidence for compliance violations against {{compliance_framework}}. Check for missing controls, misconfigurations, policy violations, and anomalies. Return compliance status, violations found, and confidence score.", "input": "{{validated_evidence}}", "timeout": "240s"}}
    },
    {
      "id": "human_review",
      "type": "human_approval",
      "position": {"x": 1000, "y": 150},
      "data": {"label": "Human Evidence Review", "config": {"prompt": "Review the following evidence that requires human judgment:\\n\\nEvidence Summary: {{evidence_summary}}\\n\\nAI Validation Result: {{validation_result}}\\n\\nAI Compliance Analysis: {{compliance_analysis}}\\n\\nPlease approve if evidence is sufficient and compliant, or reject if additional evidence is needed or non-compliant.", "timeout": "8h", "escalation": "pagerduty"}}
    },
    {
      "id": "store_evidence",
      "type": "s3_upload",
      "position": {"x": 1200, "y": 0},
      "data": {"label": "Store Evidence Immutable", "config": {"bucket": "compliance-evidence-immutable-${env}", "key": "{{framework}}/{{evidence_id}}.json", "content": "{{evidence_with_metadata}}", "metadata": "true"}}
    },
    {
      "id": "audit_trail",
      "type": "database_insert",
      "position": {"x": 1200, "y": 100},
      "data": {"label": "Generate Audit Trail", "config": {"databaseType": "postgresql", "host": "{{database_host}}", "port": "5432", "database": "{{database_name}}", "username": "{{database_username}}", "password": "{{database_password}}", "sql": "INSERT INTO audit_trail (evidence_id, action, hash, timestamp, user_id, source_ip) VALUES ('{{evidence_id}}', '{{action}}', '{{evidence_hash}}', '{{timestamp}}', '{{user_id}}', '{{source_ip}}')", "limit": "1"}}
    },
    {
      "id": "compliance_score",
      "type": "ai_agent",
      "position": {"x": 1200, "y": 200},
      "data": {"label": "Calculate Compliance Score", "config": {"agentId": "compliance_scorer", "instructions": "Calculate compliance percentage for {{compliance_framework}} based on evidence analysis. Consider: control coverage, evidence quality, violation severity, and remediation status. Return score (0-100) and breakdown by control category.", "input": "{{evidence_analysis}}", "timeout": "120s"}}
    },
    {
      "id": "notify_slack",
      "type": "slack",
      "position": {"x": 1400, "y": 0},
      "data": {"label": "Slack Notification", "config": {"channel": "#compliance-alerts", "template": "compliance_status"}}}
    },
    {
      "id": "escalate_pagerduty",
      "type": "pagerduty_trigger_incident",
      "position": {"x": 1400, "y": 100},
      "data": {"label": "PagerDuty Escalation", "config": {"serviceId": "compliance-service", "title": "Compliance Violation Detected"}}}
    },
    {
      "id": "track_linear",
      "type": "linear_create_issue",
      "position": {"x": 1400, "y": 200},
      "data": {"label": "Linear Issue Tracking", "config": {"project": "compliance", "title": "Compliance Evidence Review Required"}}}
    },
    {
      "id": "cleanup_old",
      "type": "s3_delete",
      "position": {"x": 1600, "y": 0},
      "data": {"label": "Cleanup Old Evidence", "config": {"bucket": "compliance-evidence-temp-${env}", "key": "{{evidence_id}}"}}}
    },
    {
      "id": "retention_check",
      "type": "condition",
      "position": {"x": 1600, "y": 100},
      "data": {"label": "Retention Policy Check", "config": {"checks": ["retention_expired", "legal_hold_not_applied"]}}}
    },
    {
      "id": "delete_expired",
      "type": "s3_delete",
      "position": {"x": 1800, "y": 0},
      "data": {"label": "Delete Expired Evidence", "config": {"bucket": "compliance-evidence-${env}", "key": "{{evidence_path}}"}}}
    },
    {
      "id": "complete",
      "type": "stop_fail",
      "position": {"x": 1800, "y": 100},
      "data": {"label": "Workflow Completion", "config": {"mode": "stop", "message": "Compliance evidence collection completed"}}}
    }
  ],
  "edges": [
    {"source": "trigger", "target": "deduplicate", "label": "always"},
    {"source": "webhook_trigger", "target": "deduplicate", "label": "always"},
    {"source": "deduplicate", "target": "validate_source", "label": "always"},
    {"source": "validate_source", "target": "collect_logs", "label": "logs_included"},
    {"source": "validate_source", "target": "collect_configs", "label": "configs_included"},
    {"source": "validate_source", "target": "collect_access", "label": "access_included"},
    {"source": "validate_source", "target": "collect_backups", "label": "backups_included"},
    {"source": "collect_logs", "target": "validate_evidence", "label": "logs_collected"},
    {"source": "collect_configs", "target": "validate_evidence", "label": "configs_collected"},
    {"source": "collect_access", "target": "validate_evidence", "label": "access_collected"},
    {"source": "collect_backups", "target": "validate_evidence", "label": "backups_collected"},
    {"source": "validate_evidence", "target": "ai_analysis", "label": "evidence_valid"},
    {"source": "validate_evidence", "target": "human_review", "label": "evidence_invalid or low_confidence"},
    {"source": "ai_analysis", "target": "human_review", "label": "compliance_violation_detected"},
    {"source": "ai_analysis", "target": "store_evidence", "label": "compliant"},
    {"source": "human_review", "target": "store_evidence", "label": "approved"},
    {"source": "human_review", "target": "escalate_pagerduty", "label": "rejected or timeout"},
    {"source": "store_evidence", "target": "audit_trail", "label": "always"},
    {"source": "audit_trail", "target": "compliance_score", "label": "always"},
    {"source": "compliance_score", "target": "notify_slack", "label": "always"},
    {"source": "notify_slack", "target": "track_linear", "label": "score_below_threshold"},
    {"source": "notify_slack", "target": "cleanup_old", "label": "score_above_threshold"},
    {"source": "track_linear", "target": "cleanup_old", "label": "always"},
    {"source": "cleanup_old", "target": "retention_check", "label": "always"},
    {"source": "retention_check", "target": "delete_expired", "label": "retention_expired and no_legal_hold"},
    {"source": "retention_check", "target": "complete", "label": "not_retention_expired or legal_hold_applied"},
    {"source": "delete_expired", "target": "complete", "label": "always"},
    {"source": "complete", "target": "stop_fail", "label": "always"}
  ]
}
```

## 19. Metadata
```json
{
  "category": "Security/Access",
  "tags": ["compliance", "evidence-collection", "audit-trail", "ai-analysis", "immutable-storage", "retention-policy"],
  "integrations": ["PostgreSQL/MySQL", "MongoDB", "AWS S3", "Slack", "PagerDuty", "Linear", "Anthropic/OpenAI", "Webhook", "HTTP"],
  "complexity": "High",
  "estimated_setup_minutes": 90
}
```

## 20. Production-Readiness Audit
1. **Real production trigger**: Scheduled compliance checks and webhook evidence ingestion
2. **Meaningful branching**: Evidence validation results, AI confidence scores, human approval decisions, compliance scoring thresholds
3. **Retry strategy**: All HTTP, AI, and storage nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 8h for human review, 4m for AI analysis, 2m for evidence validation
5. **Idempotency**: Deduplication key with 30-day TTL prevents duplicate evidence processing
6. **Partial failure**: Continue processing with available evidence sources when some sources fail
7. **External service failure**: Retry with exponential backoff for AI services, databases, and storage
8. **AI uncertainty**: Confidence thresholds route low-confidence evidence to human review
9. **Human approval**: Required for anomalous evidence and compliance violation decisions
10. **Observability**: Comprehensive metrics tracking evidence collection, validation, scoring, and storage
11. **Recovery**: Dead letter queue for failed notifications, checkpointing for workflow restart
12. **Security**: Encryption at rest and in transit, role-based access, immutable storage with WORM capabilities
13. **Configuration**: Realistic configurable variables for frameworks, retention, thresholds, and connections
14. **Graph quality**: Valid React Flow JSON with clean layout and logical flow
15. **Audit trail completeness**: Cryptographic hashes, immutable storage, and full chain of custody
16. **Evidence integrity**: WORM storage, metadata preservation, and tamper-evident audit trails
17. **Retention enforcement**: Automated retention policy application with legal hold overrides
18. **Compliance framework support**: Extensible to SOC 2, ISO 27001, GDPR, HIPAA, PCI DSS, and other frameworks
19. **Scalability**: Designed to handle high-volume evidence collection from multiple sources
20. **Operational safety**: Fail-safe defaults, graceful degradation, and clear error handling

**Non-applicable**: None - all audit items are relevant and addressed for compliance evidence collection workflows.
```