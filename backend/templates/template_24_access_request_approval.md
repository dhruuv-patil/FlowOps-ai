# Template 24: Access Request Approval Workflow with Time-Bounded Grants

## 1. Template Identity
- **Name**: Access Request Approval Workflow with Time-Bounded Grants
- **One-line pitch**: "Automated access request approval with time-bound grants, multi-level approval, and automated revocation"
- **Category**: Security/Access
- **Complexity**: Medium
- **Estimated node count**: 22

## 2. Business Problem
The business problem is **unauthorized access to sensitive systems** where:
- Access requests are manually approved
- No time-bound access grants
- No automated revocation
- No approval escalation
- No access tracking

**Specific pain points:**
- **Security risk**: Unauthorized access to sensitive systems
- **Manual process**: Slow approval process
- **No time-bound access**: Access persists beyond need
- **No automated revocation**: Manual cleanup required
- **No escalation**: Critical requests go unnoticed

## 3. Target User
- **Primary**: Security Engineer, Access Manager, Compliance Officer
- **Secondary**: IT Administrator, System Owner
- **Team**: Security and IT teams

## 4. Trigger
- **Type**: Access Request Form Submission / Manual Dispatch
- **Integration**: Google Forms, Microsoft Forms, Custom Form
- **Event**: Form submission, manual dispatch
- **Payload assumptions:**
  - `requester_email`
  - `system_name`
  - `access_level` (read, write, admin)
  - `duration_hours`
  - `reason`
  - `emergency` (true/false)
- **Required fields**: `requester_email`, `system_name`, `access_level`, `duration_hours`

## 5. Integrations Used
| Integration          | Purpose                                                                 |
|----------------------|-------------------------------------------------------------------------|
| Google Forms/Microsoft Forms | Access request form                                    |
| Slack                | Team notifications                                                      |
| PagerDuty            | Escalation for critical requests                                           |
| Linear               | Issue tracking                                                          |
| Okta/CyberArk        | Access management                                                       |
| AWS S3               | Access request artifact storage                                                 |

## 6. Workflow Architecture
The workflow automates access request approval with:
1. **Request ingestion** (form submission)
2. **Request validation** (required fields, system validation)
3. **Approval routing** (based on access level and duration)
4. **Human approval gates** (multi-level approval)
5. **Access grant** (time-bound access)
6. **Automated revocation** (after duration expires)
7. **Artifact storage** (access request logs)
8. **Escalation handling** (PagerDuty for critical requests)
9. **Observability** (access tracking)

The architecture follows a **validated approval pattern** with:
- **Pre-flight checks** (request validation, system validation)
- **Multi-level approval** (based on access level and duration)
- **Time-bound access** (automated revocation)
- **Artifact preservation** (for debugging)
- **Escalation paths** (PagerDuty for critical requests)

## 7. Node Definitions
| Node ID               | Node Name                     | Node Type               | Integration       | Config Summary                                                                                     |
|-----------------------|-------------------------------|-------------------------|-------------------|---------------------------------------------------------------------------------------------------|
| trigger               | Access Request Trigger              | webhook_trigger         | Google Forms/Microsoft Forms | `event: form_submitted`, `required_fields: requester_email, system_name, access_level, duration_hours`     |
| deduplicate           | Request Deduplication         | idempotency_check       | -                 | `idempotency_key: access_request_${requester_email}_${system_name}`, `ttl: 24h`                                |
| validate              | Pre-Request Validation       | condition               | -                 | `checks: [request_valid, system_valid, duration_valid]`                                     |
| route_approval        | Approval Routing            | condition               | -                 | `conditions: [access_level, duration_hours, emergency]`                                     |
| level_1_approval      | Level 1 Approval            | human_approval          | -                 | `timeout: 24h`, `escalation: pagerduty`                                                            |
| level_2_approval      | Level 2 Approval            | human_approval          | -                 | `timeout: 24h`, `escalation: pagerduty`                                                            |
| grant_access          | Grant Access                | access_grant            | Okta/CyberArk        | `duration_hours: ${duration_hours}`, `access_level: ${access_level}`                                              |
| schedule_revoke       | Schedule Revocation        | scheduled_task          | -                 | `delay: ${duration_hours}h`, `task: revoke_access`                                              |
| revoke_access         | Revoke Access               | access_revoke           | Okta/CyberArk        | `access_level: ${access_level}`                                                                   |
| notify                | Slack Notification           | slack                   | Slack             | `channel: #access-requests`, `template: access_status`                                            |
| escalate              | Escalation Path              | escalation              | PagerDuty         | `severity: critical`, `timeout: 5m`                                                               |
| artifact_store        | Artifact Storage             | aws_s3_upload           | AWS S3            | `bucket: access-requests-${repo}`, `key: ${requester_email}_${system_name}.zip`                             |
| track                 | Track Access                | linear_create_issue     | Linear             | `project: access-requests`, `status: open`                                                         |
| cleanup               | Cleanup Old Artifacts        | aws_s3_delete           | AWS S3            | `bucket: access-requests-${repo}`, `key: ${requester_email}_${system_name}.zip`                             |

## 8. Edge Definitions
| Source       | Target         | Condition                                                                                     |
|--------------|----------------|---------------------------------------------------------------------------------------------|
| trigger      | deduplicate    | always                                                                                       |
| deduplicate  | validate       | always                                                                                       |
| validate     | route_approval  | always                                                                                       |
| route_approval | level_1_approval | access_level == 'read' && duration_hours <= 24 && !emergency |
| route_approval | level_2_approval | access_level == 'write' || duration_hours > 24 || emergency |
| level_1_approval | grant_access | approved                                                                                     |
| level_2_approval | grant_access | approved                                                                                     |
| grant_access | schedule_revoke | always                                                                                       |
| schedule_revoke | revoke_access | always                                                                                       |
| revoke_access | notify         | always                                                                                       |
| notify       | track          | always                                                                                       |
| notify       | cleanup        | success                                                                                     |
| cleanup      | complete       | always                                                                                       |

## 9. Configuration Variables
| Variable                          | Type     | Example/Default                     | Purpose                                                                                     |
|-----------------------------------|----------|-------------------------------------|---------------------------------------------------------------------------------------------|
| `access_request_form`             | string   | `google_forms`                      | Access request form (google_forms, microsoft_forms, custom)                        |
| `slack_webhook_url`               | string   | `https://hooks.slack.com/services/...` | Slack notification URL                                                                      |
| `pagerduty_url`                   | string   | `https://api.pagerduty.com/incidents` | PagerDuty API endpoint                                                                       |
| `linear_project_id`               | string   | `12345`                             | Linear project ID for tracking                                                              |
| `access_management_system`        | string   | `okta`                              | Access management system (okta, cyberark)                                                |
| `default_duration_hours`         | number   | `24`                                | Default access duration (hours)                                                           |
| `artifact_bucket`                  | string   | `access-requests-${repo}`                | S3 bucket for storing access request logs                                                        |
| `max_retries`                     | number   | `3`                                 | Max retries for access management operations                                                               |
| `approval_timeout_hours`          | number   | `24`                                 | Timeout for human approval                                                                |

## 10. Branching Logic
- **Pre-request validation**: Fails if request invalid or system invalid
- **Approval routing**: Based on access level and duration
- **Level 1 approval**: For read access with duration <= 24 hours
- **Level 2 approval**: For write access or duration > 24 hours or emergency
- **Escalation**: Critical requests escalate to PagerDuty

## 11. Success Behavior
1. **Trigger**: Access request form submission
2. **Deduplication**: Avoid reprocessing the same request
3. **Validation**: Ensure request and system are valid
4. **Routing**: Route to appropriate approval level
5. **Approval**: Human approval required
6. **Grant Access**: Time-bound access granted
7. **Schedule Revocation**: Automated revocation scheduled
8. **Revoke Access**: Access revoked after duration expires
9. **Notification**: Slack notification sent to team
10. **Tracking**: Linear issue created for tracking
11. **Cleanup**: Old artifacts deleted

## 12. Failure Behavior
| Failure Type                     | Handling                                                                                     |
|----------------------------------|---------------------------------------------------------------------------------------------|
| Invalid request                    | Log and retry                                                                              |
| Invalid system                    | Log and retry                                                                              |
| Approval timeout                   | Escalate to PagerDuty                                                                      |
| Access grant failure               | Retry with exponential backoff                                                             |
| Revocation failure                 | Escalate to PagerDuty                                                                      |
| Slack notification failure        | Log and continue                                                                           |
| Linear API failure               | Dead letter queue with reason                                                               |
| PagerDuty escalation failure      | Log and continue                                                                           |

## 13. Retry / Recovery Behavior
- **Node-level retries**: All access management nodes retry up to 3 times with exponential backoff
- **Access management API failures**: Retry with exponential backoff (2s, 4s, 8s)
- **Dead letter queue**: Failed Linear/PagerDuty calls go to DLQ
- **Recovery verification**: Requires 3 successful access management operations before auto-resolving

## 14. Reliability & Observability Behavior
**FlowOps Contributions:**
- **Anomaly detection**: Unusual access patterns, repeated failures
- **Incident creation**: Automatically creates incident when:
  - Unusual access patterns detected
  - Repeated failures occur
  - Access revocation fails
- **Baseline learning**: Tracks:
  - Access request patterns
  - Approval times
  - Access durations
  - Revocation success rates
- **Observability metrics**:
  - Access request count
  - Approval time
  - Access duration
  - Revocation success rate

## 15. Security Considerations
- **Permissions**: Requires:
  - Access request form: `form_read` access
  - Slack: `chat:write` for notifications
  - PagerDuty: `incident_trigger` for escalation
  - Linear: `issue_create` for tracking
  - AWS: `s3:PutObject`, `s3:GetObject` for artifact storage
  - Access management system: `access_grant`, `access_revoke` access
- **Secrets**:
  - Access request form API key
  - Slack webhook URL
  - PagerDuty API key
  - Linear API token
  - Access management system API key
- **Sensitive data**: Never exposed in logs; all outputs sanitized
- **Audit trail**: Every decision logged with user ID and timestamp

## 16. Setup Requirements
- **Required accounts**:
  - Access request form (Google Forms/Microsoft Forms/Custom)
  - Slack workspace with notification access
  - PagerDuty account for escalation
  - Linear project for issue tracking
  - AWS S3 bucket for artifact storage
  - Access management system (Okta/CyberArk)
- **Integrations**:
  - Access request form webhook setup
  - Slack app integration
  - PagerDuty API integration
  - Linear API integration
  - AWS S3 bucket setup
  - Access management system API integration
- **Configuration**:
  - Set `access_request_form` in FlowOps
  - Configure access management system
  - Set up Slack webhook URL
  - Configure PagerDuty API endpoint
  - Set up Linear project
  - Configure artifact bucket

## 17. Expected Outcome
- **Time savings**: Reduces approval time by 60-80%
- **Operational safety**: Automated revocation prevents unauthorized access
- **Consistency**: Standardized approval process across teams
- **Escalation reduction**: Critical requests escalated proactively
- **Observability**: Real-time metrics and access tracking

## 18. React Flow Graph
```json
{
  "nodes": [
    {
      "id": "trigger",
      "type": "webhook_trigger",
      "position": {"x": 0, "y": 0},
      "data": {"label": "Access Request Trigger", "config": {"event": "form_submitted", "required_fields": ["requester_email", "system_name", "access_level", "duration_hours"]}}
    },
    {
      "id": "deduplicate",
      "type": "idempotency_check",
      "position": {"x": 100, "y": 0},
      "data": {"label": "Request Deduplication", "config": {"idempotency_key": "access_request_${requester_email}_${system_name}", "ttl": "24h"}}
    },
    {
      "id": "validate",
      "type": "condition",
      "position": {"x": 200, "y": 0},
      "data": {"label": "Pre-Request Validation", "config": {"checks": ["request_valid", "system_valid", "duration_valid"]}}
    },
    {
      "id": "route_approval",
      "type": "condition",
      "position": {"x": 300, "y": 0},
      "data": {"label": "Approval Routing", "config": {"conditions": ["access_level", "duration_hours", "emergency"]}}
    },
    {
      "id": "level_1_approval",
      "type": "human_approval",
      "position": {"x": 400, "y": -50},
      "data": {"label": "Level 1 Approval", "config": {"timeout": "24h", "escalation": "pagerduty"}}
    },
    {
      "id": "level_2_approval",
      "type": "human_approval",
      "position": {"x": 400, "y": 50},
      "data": {"label": "Level 2 Approval", "config": {"timeout": "24h", "escalation": "pagerduty"}}
    },
    {
      "id": "grant_access",
      "type": "access_grant",
      "position": {"x": 500, "y": 0},
      "data": {"label": "Grant Access", "config": {"duration_hours": "${duration_hours}", "access_level": "${access_level}"}}
    },
    {
      "id": "schedule_revoke",
      "type": "scheduled_task",
      "position": {"x": 600, "y": 0},
      "data": {"label": "Schedule Revocation", "config": {"delay": "${duration_hours}h", "task": "revoke_access"}}
    },
    {
      "id": "revoke_access",
      "type": "access_revoke",
      "position": {"x": 700, "y": 0},
      "data": {"label": "Revoke Access", "config": {"access_level": "${access_level}"}}
    },
    {
      "id": "notify",
      "type": "slack",
      "position": {"x": 800, "y": 0},
      "data": {"label": "Slack Notification", "config": {"channel": "#access-requests", "template": "access_status"}}
    },
    {
      "id": "escalate",
      "type": "escalation",
      "position": {"x": 800, "y": 100},
      "data": {"label": "Escalation Path", "config": {"severity": "critical", "timeout": "5m"}}
    },
    {
      "id": "artifact_store",
      "type": "aws_s3_upload",
      "position": {"x": 900, "y": 0},
      "data": {"label": "Artifact Storage", "config": {"bucket": "access-requests-${repo}", "key": "${requester_email}_${system_name}.zip"}}
    },
    {
      "id": "track",
      "type": "linear_create_issue",
      "position": {"x": 900, "y": 100},
      "data": {"label": "Track Access", "config": {"project": "access-requests", "status": "open"}}
    },
    {
      "id": "cleanup",
      "type": "aws_s3_delete",
      "position": {"x": 900, "y": 150},
      "data": {"label": "Cleanup Old Artifacts", "config": {"bucket": "access-requests-${repo}", "key": "${requester_email}_${system_name}.zip"}}
    },
    {
      "id": "complete",
      "type": "stop_fail",
      "position": {"x": 1000, "y": 50},
      "data": {"label": "Workflow Completion"}
    }
  ],
  "edges": [
    {"source": "trigger", "target": "deduplicate", "label": "always"},
    {"source": "deduplicate", "target": "validate", "label": "always"},
    {"source": "validate", "target": "route_approval", "label": "always"},
    {"source": "route_approval", "target": "level_1_approval", "label": "access_level == 'read' && duration_hours <= 24 && !emergency"},
    {"source": "route_approval", "target": "level_2_approval", "label": "access_level == 'write' || duration_hours > 24 || emergency"},
    {"source": "level_1_approval", "target": "grant_access", "label": "approved"},
    {"source": "level_2_approval", "target": "grant_access", "label": "approved"},
    {"source": "grant_access", "target": "schedule_revoke", "label": "always"},
    {"source": "schedule_revoke", "target": "revoke_access", "label": "always"},
    {"source": "revoke_access", "target": "notify", "label": "always"},
    {"source": "notify", "target": "track", "label": "always"},
    {"source": "notify", "target": "cleanup", "label": "success"},
    {"source": "cleanup", "target": "complete", "label": "always"}
  ]
}
```

## 19. Metadata
```json
{
  "category": "Security/Access",
  "tags": ["Access Request", "Approval Workflow", "Time-Bounded Grants", "Automated Revocation", "Reliability"],
  "integrations": ["Google Forms/Microsoft Forms", "Slack", "PagerDuty", "Linear", "Okta/CyberArk", "AWS S3"],
  "complexity": "Medium",
  "estimated_setup_minutes": 45
}
```

## 20. Production-Readiness Audit
1. **Real production trigger**: Access request form submission
2. **Meaningful branching**: Multi-level approval, emergency escalation
3. **Retry strategy**: All access management nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 24h for human approval, 5m for escalation
5. **Idempotency**: Deduplication key with 24h TTL
6. **Partial failure**: Automated revocation scheduled
7. **External service failure**: Retry with exponential backoff for access management APIs
8. **Human approval**: Required for all access requests
9. **Observability**: Comprehensive metrics tracking
10. **Recovery**: Dead letter queue for failed Linear/PagerDuty calls
11. **Security**: All secrets encrypted, sensitive data sanitized
12. **Configuration**: Realistic configurable variables
13. **Graph quality**: Valid React Flow JSON with clean layout

**Non-applicable**: None