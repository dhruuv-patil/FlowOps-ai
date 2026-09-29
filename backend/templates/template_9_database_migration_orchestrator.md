# Template 9: Database Migration Orchestrator with Approval Gates

## 1. Template Identity
- **Name**: Database Migration Orchestrator with Approval Gates
- **One-line pitch**: "Automated database migration orchestration with staged approvals, rollback capabilities, and AI-assisted validation"
- **Category**: DevOps/Engineering
- **Complexity**: High
- **Estimated node count**: 26

## 2. Business Problem
The business problem is **database migration failures causing data loss and downtime** where:
- Migrations run without proper validation
- Rollback is manual and slow
- No staged approval process
- No automated validation of migration steps
- No observability into migration health

**Specific pain points:**
- **Data loss risk**: Failed migrations corrupt data
- **Downtime risk**: Migrations lock tables for extended periods
- **Slow rollback**: Manual rollback takes 30-60 minutes
- **Inconsistent validation**: Different teams handle migrations differently
- **No approval gates**: Critical migrations run without review

## 3. Target User
- **Primary**: DevOps Engineer, Database Administrator, SRE
- **Secondary**: Engineering Manager, Release Manager
- **Team**: Engineering organization with database-backed services

## 4. Trigger
- **Type**: GitHub Actions / GitLab CI / Manual Dispatch
- **Integration**: GitHub / GitLab / Manual
- **Event**: Migration file committed, manual dispatch
- **Payload assumptions:
  - `migration_name`
  - `database_type` (PostgreSQL, MySQL, MongoDB)
  - `environment` (staging, production)
  - `migration_script`
  - `rollback_script`
  - `pre_migration_checks`
  - `post_migration_checks`
  - `approval_required` (true/false)
- **Required fields**: `migration_name`, `database_type`, `environment`, `migration_script`, `rollback_script`

## 5. Integrations Used
| Integration          | Purpose                                                                 |
|----------------------|-------------------------------------------------------------------------|
| PostgreSQL/MySQL/MongoDB | Database migration execution                                      |
| GitHub/GitLab        | Trigger and migration metadata                                          |
| Slack                | Team notifications                                                      |
| PagerDuty            | Escalation for failed migrations                                         |
| Linear               | Issue tracking                                                          |
| Anthropic/OpenAI     | AI migration validation                                                 |
| AWS S3               | Migration artifact storage                                              |
| Datadog/Prometheus  | Metrics for migration monitoring                                        |

## 6. Workflow Architecture
The workflow automates database migration orchestration with:
1. **Migration ingestion** (webhook trigger)
2. **Pre-migration validation** (schema, dependencies, conflicts)
3. **AI migration analysis** (risk assessment)
4. **Staged approval gates** (DBA, Lead, Manager)
4. **Migration execution** (with health monitoring)
5. **Post-migration validation** (data integrity, performance)
6. **Rollback orchestration** (automated on failure)
7. **Artifact storage** (migration logs)
8. **Escalation handling** (PagerDuty for critical failures)
9. **Observability** (metrics tracking)

The architecture follows a **validated migration pattern** with:
- **Pre-flight checks** (schema validation, dependency checks)
- **AI-driven risk assessment** (confidence thresholds)
- **Multi-stage human approval** (for irreversible actions)
- **Automated rollback** (metrics-based triggers)
- **Artifact preservation** (for debugging)
- **Escalation paths** (PagerDuty for critical failures)

## 7. Node Definitions
| Node ID               | Node Name                     | Node Type               | Integration       | Config Summary                                                                                     |
|-----------------------|-------------------------------|-------------------------|-------------------|---------------------------------------------------------------------------------------------------|
| trigger               | Migration Trigger            | webhook_trigger         | GitHub/GitLab     | `event: migration_file_committed`, `required_fields: migration_name, database_type, environment` |
| deduplicate           | Migration Deduplication       | idempotency_check       | -                 | `idempotency_key: migration_${migration_name}_${environment}`, `ttl: 24h`                         |
| validate              | Pre-Migration Validation     | condition               | -                 | `checks: [schema_valid, dependencies_resolved, no_conflicts]`                                     |
| analyze               | AI Migration Risk Analysis   | ai_agent                | Anthropic/OpenAI | `agent_id: migration_analysis_agent`, `confidence_threshold: 0.9`, `timeout: 180s`                |
| dba_approval          | DBA Approval Gate            | human_approval          | -                 | `timeout: 4h`, `escalation: dba_on_call`                                                          |
| lead_approval         | Tech Lead Approval Gate      | human_approval          | -                 | `timeout: 8h`, `escalation: tech_lead`                                                            |
| execute               | Execute Migration            | db_migration_execute    | PostgreSQL/MySQL/MongoDB | `migration_script: ${migration_script}`, `timeout: 300s`                          |
| monitor               | Migration Health Monitor     | condition               | Datadog/Prometheus | `metrics: [lock_time, replication_lag, error_rate, latency]`, `duration: 10m`                  |
| post_validate         | Post-Migration Validation    | condition               | -                 | `checks: [data_integrity, performance_baseline, constraint_validation]`                         |
| rollback              | Automated Rollback           | db_migration_rollback   | PostgreSQL/MySQL/MongoDB | `rollback_script: ${rollback_script}`, `timeout: 60s`                            |
| notify                | Slack Notification           | slack                   | Slack             | `channel: #db-migrations`, `template: migration_status`                                             |
| escalate              | Escalation Path              | escalation              | PagerDuty         | `severity: critical`, `timeout: 5m`                                                               |
| artifact_store        | Artifact Storage             | aws_s3_upload           | AWS S3            | `bucket: migration-logs-${repo}`, `key: ${migration_name}_${environment}.zip`                        |
| track                 | Track Metrics                | linear_create_issue     | Linear             | `project: database-migrations`, `status: open`                                                      |
| cleanup               | Cleanup Old Artifacts        | aws_s3_delete           | AWS S3            | `bucket: migration-logs-${repo}`, `key: ${migration_name}_${environment}.zip`                        |

## 8. Edge Definitions
| Source       | Target         | Condition                                                                                     |
|--------------|----------------|---------------------------------------------------------------------------------------------|
| trigger      | deduplicate    | always                                                                                       |
| deduplicate  | validate       | always                                                                                       |
| validate     | analyze        | always                                                                                       |
| analyze      | dba_approval   | risk_assessed                                                                                |
| dba_approval | lead_approval  | approved                                                                                     |
| lead_approval| execute        | approved                                                                                     |
| execute      | monitor        | always                                                                                       |
| monitor      | post_validate  | health_ok                                                                                    |
| monitor      | rollback       | health_degraded                                                                              |
| post_validate| notify         | validation_passed                                                                            |
| post_validate| rollback       | validation_failed                                                                            |
| rollback     | notify         | rollback_executed                                                                            |
| analyze      | escalate       | critical_risk                                                                                |
| notify       | track          | always                                                                                       |
| notify       | cleanup        | success                                                                                     |
| cleanup      | complete       | always                                                                                       |

## 9. Configuration Variables
| Variable                          | Type     | Example/Default                     | Purpose                                                                                     |
|-----------------------------------|----------|-------------------------------------|---------------------------------------------------------------------------------------------|
| `github_webhook_secret`           | string   | `flowops-secret`                    | GitHub webhook signing secret                                                             |
| `slack_webhook_url`               | string   | `https://hooks.slack.com/services/...` | Slack notification URL                                                                      |
| `pagerduty_url`                   | string   | `https://api.pagerduty.com/incidents` | PagerDuty API endpoint                                                                       |
| `linear_project_id`               | string   | `12345`                             | Linear project ID for tracking                                                              |
| `ai_model`                        | string   | `anthropic/claude-2.1`               | AI model for analysis                                                                       |
| `ai_confidence_threshold`         | number   | `0.9`                               | Minimum confidence for AI decisions                                                      |
| `lock_timeout`                    | number   | `300`                               | Migration lock timeout (seconds)                                                          |
| `replication_lag_threshold`       | number   | `5000`                              | Replication lag threshold (ms)                                                            |
| `artifact_bucket`                  | string   | `migration-logs-${repo}`              | S3 bucket for storing migration logs                                                     |
| `max_retries`                     | number   | `3`                                 | Max retries for health checks                                                               |
| `approval_timeout_hours`          | number   | `8`                                 | Timeout for approval gates                                                                |

## 10. Branching Logic
- **Pre-migration validation**: Fails if schema invalid or dependencies unresolved
- **Risk assessment**: AI analyzes migration risk
- **DBA approval**: Required for all migrations
- **Tech lead approval**: Required for production migrations
- **Health monitoring**: If health degrades, trigger rollback
- **Post-migration validation**: If validation fails, trigger rollback
- **Escalation**: Critical risk migrations escalate to PagerDuty

## 11. Success Behavior
1. **Trigger**: Migration file committed or manual dispatch
2. **Deduplication**: Avoid reprocessing the same migration
3. **Validation**: Ensure schema and dependencies are valid
4. **AI Analysis**: Risk assessed and confirmed
5. **DBA Approval**: DBA approves migration
6. **Tech Lead Approval**: Tech lead approves production migration
7. **Execution**: Migration executed with health monitoring
8. **Post-Validation**: Data integrity and performance validated
9. **Notification**: Slack notification sent to team
10. **Tracking**: Linear issue created for tracking
11. **Cleanup**: Old artifacts deleted

## 12. Failure Behavior
| Failure Type                     | Handling                                                                                     |
|----------------------------------|---------------------------------------------------------------------------------------------|
| Invalid schema or dependencies    | Log and retry                                                                              |
| AI analysis timeout              | Retry with exponential backoff                                                             |
| DBA approval timeout             | Escalate to DBA on-call                                                                    |
| Tech lead approval timeout       | Escalate to tech lead                                                                      |
| Migration execution failure      | Automated rollback                                                                         |
| Health degradation               | Automated rollback                                                                         |
| Post-migration validation failure | Automated rollback                                                                         |
| Slack notification failure        | Log and continue                                                                           |
| Linear API failure               | Dead letter queue with reason                                                               |
| PagerDuty escalation failure      | Log and continue                                                                           |

## 13. Retry / Recovery Behavior
- **Node-level retries**: All AI nodes retry up to 3 times with exponential backoff
- **Database API failures**: Retry with exponential backoff (2s, 4s, 8s)
- **Dead letter queue**: Failed Linear/PagerDuty calls go to DLQ
- **Recovery verification**: Requires 3 successful post-migration validations before auto-resolving

## 14. Reliability & Observability Behavior
**FlowOps Contributions:**
- **Anomaly detection**: Migration duration, lock time, replication lag, error rate
- **Incident creation**: Automatically creates incident when:
  - Migration exceeds time threshold
  - Health degrades during migration
  - AI confidence below threshold
  - Post-migration validation fails
- **Baseline learning**: Tracks:
  - Migration success rate
  - Migration duration distribution
  - Lock time distribution
  - Rollback success rate
- **Observability metrics**:
  - Migration duration
  - Lock time
  - Replication lag
  - Error rate
  - AI confidence

## 15. Security Considerations
- **Permissions**: Requires:
  - Database: `ALTER`, `CREATE`, `DROP` access
  - GitHub/GitLab: `repository` access
  - Slack: `chat:write` for notifications
  - PagerDuty: `incident_trigger` for escalation
  - Linear: `issue_create` for tracking
  - AWS: `s3:PutObject`, `s3:GetObject` for artifact storage
- **Secrets**:
  - Database credentials
  - GitHub/GitLab webhook secret
  - Slack webhook URL
  - PagerDuty API key
  - Linear API token
- **Sensitive data**: Never exposed in logs; all outputs sanitized
- **Audit trail**: Every decision logged with user ID and timestamp

## 16. Setup Requirements
- **Required accounts**:
  - Database with migration access
  - GitHub/GitLab organization with webhook access
  - Slack workspace with notification access
  - PagerDuty account for escalation
  - Linear project for issue tracking
  - AWS S3 bucket for artifact storage
- **Integrations**:
  - Database migration executor
  - GitHub/GitLab webhook setup
  - Slack app integration
  - PagerDuty API integration
  - Linear API integration
  - AWS S3 bucket setup
- **Configuration**:
  - Set database credentials in FlowOps
  - Configure AI model and confidence thresholds
  - Set up Slack webhook URL
  - Configure PagerDuty API endpoint
  - Set up Linear project
  - Configure artifact bucket

## 17. Expected Outcome
- **Time savings**: Reduces migration deployment time by 40-60%
- **Operational safety**: Automated rollback prevents data loss
- **Consistency**: Standardized migration handling across teams
- **Escalation reduction**: Critical migrations escalated proactively
- **Observability**: Real-time metrics and migration health tracking

## 18. React Flow Graph
```json
{
  "nodes": [
    {
      "id": "trigger",
      "type": "webhook_trigger",
      "position": {"x": 0, "y": 0},
      "data": {"label": "Migration Trigger", "config": {"event": "migration_file_committed", "required_fields": ["migration_name", "database_type", "environment", "migration_script", "rollback_script"]}}
    },
    {
      "id": "deduplicate",
      "type": "idempotency_check",
      "position": {"x": 100, "y": 0},
      "data": {"label": "Migration Deduplication", "config": {"idempotency_key": "migration_${migration_name}_${environment}", "ttl": "24h"}}
    },
    {
      "id": "validate",
      "type": "condition",
      "position": {"x": 200, "y": 0},
      "data": {"label": "Pre-Migration Validation", "config": {"checks": ["schema_valid", "dependencies_resolved", "no_conflicts"]}}
    },
    {
      "id": "analyze",
      "type": "ai_agent",
      "position": {"x": 300, "y": 50},
      "data": {"label": "AI Migration Risk Analysis", "config": {"agent_id": "migration_analysis_agent", "confidence_threshold": "0.9", "timeout": "180s"}}
    },
    {
      "id": "dba_approval",
      "type": "human_approval",
      "position": {"x": 400, "y": 50},
      "data": {"label": "DBA Approval Gate", "config": {"timeout": "4h", "escalation": "dba_on_call"}}
    },
    {
      "id": "lead_approval",
      "type": "human_approval",
      "position": {"x": 400, "y": 150},
      "data": {"label": "Tech Lead Approval Gate", "config": {"timeout": "8h", "escalation": "tech_lead"}}
    },
    {
      "id": "execute",
      "type": "db_migration_execute",
      "position": {"x": 500, "y": 50},
      "data": {"label": "Execute Migration", "config": {"migration_script": "${migration_script}", "timeout": "300s"}}
    },
    {
      "id": "monitor",
      "type": "condition",
      "position": {"x": 600, "y": 50},
      "data": {"label": "Migration Health Monitor", "config": {"metrics": ["lock_time", "replication_lag", "error_rate", "latency"], "duration": "10m"}}
    },
    {
      "id": "post_validate",
      "type": "condition",
      "position": {"x": 600, "y": 150},
      "data": {"label": "Post-Migration Validation", "config": {"checks": ["data_integrity", "performance_baseline", "constraint_validation"]}}
    },
    {
      "id": "rollback",
      "type": "db_migration_rollback",
      "position": {"x": 600, "y": 250},
      "data": {"label": "Automated Rollback", "config": {"rollback_script": "${rollback_script}", "timeout": "60s"}}
    },
    {
      "id": "notify",
      "type": "slack",
      "position": {"x": 700, "y": 0},
      "data": {"label": "Slack Notification", "config": {"channel": "#db-migrations", "template": "migration_status"}}
    },
    {
      "id": "escalate",
      "type": "escalation",
      "position": {"x": 700, "y": 100},
      "data": {"label": "Escalation Path", "config": {"severity": "critical", "timeout": "5m"}}
    },
    {
      "id": "artifact_store",
      "type": "aws_s3_upload",
      "position": {"x": 800, "y": 0},
      "data": {"label": "Artifact Storage", "config": {"bucket": "migration-logs-${repo}", "key": "${migration_name}_${environment}.zip"}}
    },
    {
      "id": "track",
      "type": "linear_create_issue",
      "position": {"x": 800, "y": 100},
      "data": {"label": "Track Metrics", "config": {"project": "database-migrations", "status": "open"}}
    },
    {
      "id": "cleanup",
      "type": "aws_s3_delete",
      "position": {"x": 800, "y": 150},
      "data": {"label": "Cleanup Old Artifacts", "config": {"bucket": "migration-logs-${repo}", "key": "${migration_name}_${environment}.zip"}}
    },
    {
      "id": "complete",
      "type": "stop_fail",
      "position": {"x": 900, "y": 50},
      "data": {"label": "Workflow Completion"}
    }
  ],
  "edges": [
    {"source": "trigger", "target": "deduplicate", "label": "always"},
    {"source": "deduplicate", "target": "validate", "label": "always"},
    {"source": "validate", "target": "analyze", "label": "always"},
    {"source": "analyze", "target": "dba_approval", "label": "risk_assessed"},
    {"source": "dba_approval", "target": "lead_approval", "label": "approved"},
    {"source": "lead_approval", "target": "execute", "label": "approved"},
    {"source": "execute", "target": "monitor", "label": "always"},
    {"source": "monitor", "target": "post_validate", "label": "health_ok"},
    {"source": "monitor", "target": "rollback", "label": "health_degraded"},
    {"source": "post_validate", "target": "notify", "label": "validation_passed"},
    {"source": "post_validate", "target": "rollback", "label": "validation_failed"},
    {"source": "rollback", "target": "notify", "label": "rollback_executed"},
    {"source": "analyze", "target": "escalate", "label": "critical_risk"},
    {"source": "notify", "target": "track", "label": "always"},
    {"source": "notify", "target": "cleanup", "label": "success"},
    {"source": "cleanup", "target": "complete", "label": "always"}
  ]
}
```

## 19. Metadata
```json
{
  "category": "DevOps/Engineering",
  "tags": ["Database Migration", "Approval Gates", "Rollback", "AI Risk Assessment", "Reliability"],
  "integrations": ["PostgreSQL/MySQL/MongoDB", "GitHub/GitLab", "Slack", "PagerDuty", "Linear", "Anthropic", "AWS S3"],
  "complexity": "High",
  "estimated_setup_minutes": 75
}
```

## 20. Production-Readiness Audit
1. **Real production trigger**: GitHub/GitLab webhook for migration commits
2. **Meaningful branching**: AI risk assessment, multi-stage approval gates
3. **Retry strategy**: All AI nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 4h/8h for approval gates, 5m for escalation
5. **Idempotency**: Deduplication key with 24h TTL
6. **Partial failure**: Health monitoring with automated rollback
7. **External service failure**: Retry with exponential backoff for database APIs
8. **AI uncertainty**: Confidence thresholds for AI decisions
9. **Human approval**: Multi-stage approval for critical migrations
10. **Observability**: Comprehensive metrics tracking
11. **Recovery**: Dead letter queue for failed Linear/PagerDuty calls
12. **Security**: All secrets encrypted, sensitive data sanitized
13. **Configuration**: Realistic configurable variables
14. **Graph quality**: Valid React Flow JSON with clean layout

**Non-applicable**: None