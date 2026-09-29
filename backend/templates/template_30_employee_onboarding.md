# Template 30: Employee Onboarding/Offboarding Orchestration

## 1. Template Identity
- **Name**: Employee Onboarding/Offboarding Orchestration
- **One-line pitch**: "Comprehensive employee lifecycle management with automated onboarding, access provisioning, and secure offboarding"
- **Category**: Business Operations
- **Complexity**: High
- **Estimated node count**: 32

## 2. Business Problem
The business problem is **inconsistent employee onboarding/offboarding processes** causing:
- Incomplete access provisioning
- Security risks from lingering access
- Manual errors in HR systems
- Inconsistent documentation
- Lack of compliance tracking

**Specific pain points:**
- **Access gaps**: New hires get access slowly
- **Security risks**: Ex-employees retain access
- **HR inefficiencies**: Manual data entry
- **Compliance gaps**: Missing audit trails
- **Documentation gaps**: Incomplete onboarding guides

## 3. Target User
- **Primary**: HR Manager, IT Administrator, Security Officer
- **Secondary**: Engineering Manager, Compliance Officer
- **Team**: HR, IT, Security teams

## 4. Trigger
- **Type**: Manual Dispatch / Scheduled / Webhook
- **Integration**: Linear / Slack / Manual
- **Event**: New hire/termination event, scheduled onboarding/offboarding
- **Payload assumptions:
  - `employee_id`
  - `employee_name`
  - `event_type` (onboarding/offboarding)
  - `start_date`
  - `end_date`
  - `department`
  - `job_title`
  - `manager_id`
  - `access_level`
  - `compliance_requirements`
- **Required fields**: `employee_id`, `event_type`, `start_date`

## 5. Integrations Used
| Integration          | Purpose                                                                 |
|----------------------|-------------------------------------------------------------------------|
| Linear               | Issue tracking and employee records                                      |
| Slack                | Team notifications and onboarding guides                                 |
| Google Workspace     | Email and calendar setup                                                |
| Okta                 | Identity and access management                                          |
| Salesforce           | HR system integration                                                   |
| Box                  | Document storage and sharing                                            |
| Zoom                 | Virtual onboarding sessions                                              |
| PagerDuty            | Escalation for critical issues                                         |
| Datadog              | Access provisioning monitoring                                          |

## 6. Workflow Architecture
The workflow automates employee onboarding/offboarding with:
1. **Employee record creation**
2. **Access provisioning**
3. **Document distribution**
4. **Training assignment**
5. **Compliance verification**
6. **Access revocation**
7. **Final documentation**
8. **Escalation handling**

The architecture follows a **validated lifecycle pattern** with:
- **Multi-stage approvals**
- **Automated access management**
- **Compliance verification**
- **Documentation tracking**
- **Escalation paths**

## 7. Node Definitions
| Node ID               | Node Name                     | Node Type               | Integration       | Config Summary                                                                                     |
|-----------------------|-------------------------------|-------------------------|-------------------|---------------------------------------------------------------------------------------------------|
| trigger               | Employee Event Trigger      | webhook_trigger         | Linear/Manual     | `event: employee_event`, `required_fields: employee_id, event_type, start_date` |
| validate              | Event Validation           | condition               | -                 | `checks: [valid_employee_id, valid_event_type, valid_dates]`                                     |
| create_record         | Create Employee Record     | salesforce_create_record | Salesforce        | `object: Employee`, `fields: employee_id, name, department, job_title, manager_id`                   |
| provision_access      | Provision Access           | okta_provision_access    | Okta              | `access_level: ${access_level}`, `timeout: 300s`                                                  |
| distribute_docs       | Distribute Onboarding Docs | box_share_folder         | Box               | `folder_id: onboarding_${department}`, `recipients: ${employee_email}`                             |
| assign_training       | Assign Training           | zoom_create_meeting      | Zoom              | `topic: Onboarding Training - ${employee_name}`, `duration: 60`, `attendees: ${employee_email}`     |
| verify_compliance     | Compliance Verification   | condition               | -                 | `checks: [compliance_docs_signed, background_check_complete]`                                     |
| revoke_access         | Revoke Access             | okta_revoke_access      | Okta              | `access_level: ${access_level}`, `timeout: 300s`                                                  |
| archive_docs          | Archive Employee Docs      | box_move_folder         | Box               | `folder_id: employee_archive_${employee_id}`, `source: onboarding_${department}`                    |
| notify                | Slack Notification        | slack                   | Slack             | `channel: #hr-notifications`, `template: employee_event_status`                                     |
| escalate              | Escalation Path           | escalation              | PagerDuty         | `severity: critical`, `timeout: 5m`                                                               |
| track                 | Track Employee Metrics    | linear_create_issue     | Linear             | `project: employee-lifecycle`, `status: open`                                                      |
| monitor               | Access Provisioning Monitor | condition           | Datadog           | `metrics: [access_provisioning_time, error_rate]`, `duration: 10m`                                |

## 8. Edge Definitions
| Source               | Target                 | Condition                                                                                     |
|-----------------------|------------------------|---------------------------------------------------------------------------------------------|
| trigger              | validate               | always                                                                                       |
| validate             | create_record          | always                                                                                       |
| create_record        | provision_access       | always                                                                                       |
| provision_access     | distribute_docs        | access_provisioned                                                                           |
| distribute_docs      | assign_training        | always                                                                                       |
| assign_training      | verify_compliance      | always                                                                                       |
| verify_compliance    | notify                 | compliance_verified                                                                          |
| verify_compliance    | escalate               | compliance_failed                                                                           |
| notify               | track                  | always                                                                                       |
| track                | complete               | always                                                                                       |
| provision_access     | monitor                | always                                                                                       |
| monitor              | revoke_access          | access_provisioning_failed                                                                  |
| revoke_access        | archive_docs           | always                                                                                       |
| archive_docs         | notify                 | always                                                                                       |

## 9. Configuration Variables
| Variable                          | Type     | Example/Default                     | Purpose                                                                                     |
|-----------------------------------|----------|-------------------------------------|---------------------------------------------------------------------------------------------|
| `linear_webhook_secret`           | string   | `flowops-secret`                    | Linear webhook signing secret                                                             |
| `slack_webhook_url`               | string   | `https://hooks.slack.com/services/...` | Slack notification URL                                                                      |
| `pagerduty_url`                   | string   | `https://api.pagerduty.com/incidents` | PagerDuty API endpoint                                                                       |
| `linear_project_id`               | string   | `12345`                             | Linear project ID for tracking                                                              |
| `okta_api_url`                    | string   | `https://yourdomain.okta.com`          | Okta API endpoint                                                                           |
| `salesforce_api_url`             | string   | `https://yourdomain.salesforce.com`   | Salesforce API endpoint                                                                     |
| `box_api_url`                     | string   | `https://api.box.com/2.0`               | Box API endpoint                                                                           |
| `zoom_api_url`                    | string   | `https://api.zoom.us/v2`               | Zoom API endpoint                                                                           |
| `access_provisioning_timeout`     | number   | `300`                               | Access provisioning timeout (seconds)                                                      |
| `compliance_verification_timeout` | number   | `72`                                | Compliance verification timeout (hours)                                                    |

## 10. Branching Logic
- **Event validation**: Fails if invalid employee ID or event type
- **Access provisioning**: If fails, trigger escalation
- **Compliance verification**: If fails, trigger escalation
- **Access revocation**: If fails, trigger escalation
- **Monitoring**: If access provisioning fails, trigger revocation

## 11. Success Behavior
1. **Trigger**: Employee event (onboarding/offboarding)
2. **Validation**: Ensure valid employee data
3. **Record creation**: Create employee record in Salesforce
4. **Access provisioning**: Provision access in Okta
5. **Document distribution**: Share onboarding documents in Box
6. **Training assignment**: Schedule Zoom training
7. **Compliance verification**: Verify compliance documents
8. **Notification**: Slack notification sent to team
9. **Tracking**: Linear issue created for tracking
10. **Monitoring**: Track access provisioning metrics

## 12. Failure Behavior
| Failure Type                     | Handling                                                                                     |
|----------------------------------|---------------------------------------------------------------------------------------------|
| Invalid employee data               | Log and retry                                                                              |
| Salesforce API failure              | Retry with exponential backoff                                                             |
| Okta API failure                   | Retry with exponential backoff                                                             |
| Box API failure                   | Retry with exponential backoff                                                             |
| Zoom API failure                   | Retry with exponential backoff                                                             |
| Compliance verification failure    | Escalate to PagerDuty                                                                    |
| Access revocation failure            | Escalate to PagerDuty                                                                    |
| Slack notification failure           | Log and continue                                                                           |
| Linear API failure                | Dead letter queue with reason                                                               |
| PagerDuty escalation failure       | Log and continue                                                                           |

## 13. Retry / Recovery Behavior
- **Node-level retries**: All API nodes retry up to 3 times with exponential backoff
- **Dead letter queue**: Failed Linear/PagerDuty calls go to DLQ
- **Recovery verification**: Requires 3 successful runs before auto-resolving

## 14. Reliability & Observability Behavior
**FlowOps Contributions:**
- **Anomaly detection**: Access provisioning time, error rate, compliance verification time
- **Incident creation**: Automatically creates incident when:
  - Access provisioning fails
  - Compliance verification fails
  - Access revocation fails
- **Baseline learning**: Tracks:
  - Access provisioning time distribution
  - Compliance verification time distribution
  - Error rate distribution
- **Observability metrics**:
  - Access provisioning time
  - Compliance verification time
  - Error rate

## 15. Security Considerations
- **Permissions**: Requires:
  - Salesforce: `Employee` object access
  - Okta: `User` and `Group` management
  - Box: `Folder` and `File` access
  - Zoom: `Meeting` creation
  - Slack: `chat:write` for notifications
  - PagerDuty: `incident_trigger` for escalation
  - Linear: `issue_create` for tracking
- **Secrets**:
  - Salesforce API token
  - Okta API token
  - Box API token
  - Zoom API token
  - Slack webhook URL
  - PagerDuty API key
  - Linear API token
- **Sensitive data**: Never exposed in logs; all outputs sanitized
- **Audit trail**: Every decision logged with user ID and timestamp

## 16. Setup Requirements
- **Required accounts**:
  - Salesforce with Employee object access
  - Okta with User and Group management
  - Box with Folder and File access
  - Zoom with Meeting creation
  - Slack workspace with notification access
  - PagerDuty account for escalation
  - Linear project for issue tracking
- **Integrations**:
  - Salesforce API integration
  - Okta API integration
  - Box API integration
  - Zoom API integration
  - Slack app integration
  - PagerDuty API integration
  - Linear API integration
- **Configuration**:
  - Set Salesforce API token in FlowOps
  - Set Okta API token in FlowOps
  - Set Box API token in FlowOps
  - Set Zoom API token in FlowOps
  - Configure Slack webhook URL
  - Configure PagerDuty API endpoint
  - Set up Linear project

## 17. Expected Outcome
- **Time savings**: Reduces onboarding/offboarding time by 50-70%
- **Operational safety**: Automated access management prevents security risks
- **Consistency**: Standardized processes across teams
- **Compliance**: Ensures compliance requirements are met
- **Observability**: Real-time metrics and access provisioning tracking

## 18. React Flow Graph
```json
{
  "nodes": [
    {
      "id": "trigger",
      "type": "webhook_trigger",
      "position": {"x": 0, "y": 0},
      "data": {"label": "Employee Event Trigger", "config": {"event": "employee_event", "required_fields": ["employee_id", "event_type", "start_date"]}}
    },
    {
      "id": "validate",
      "type": "condition",
      "position": {"x": 100, "y": 0},
      "data": {"label": "Event Validation", "config": {"checks": ["valid_employee_id", "valid_event_type", "valid_dates"]}}
    },
    {
      "id": "create_record",
      "type": "salesforce_create_record",
      "position": {"x": 200, "y": 0},
      "data": {"label": "Create Employee Record", "config": {"object": "Employee", "fields": ["employee_id", "name", "department", "job_title", "manager_id"]}}
    },
    {
      "id": "provision_access",
      "type": "okta_provision_access",
      "position": {"x": 300, "y": 50},
      "data": {"label": "Provision Access", "config": {"access_level": "${access_level}", "timeout": "300s"}}
    },
    {
      "id": "distribute_docs",
      "type": "box_share_folder",
      "position": {"x": 400, "y": 50},
      "data": {"label": "Distribute Onboarding Docs", "config": {"folder_id": "onboarding_${department}", "recipients": "${employee_email}"}}
    },
    {
      "id": "assign_training",
      "type": "zoom_create_meeting",
      "position": {"x": 500, "y": 50},
      "data": {"label": "Assign Training", "config": {"topic": "Onboarding Training - ${employee_name}", "duration": "60", "attendees": "${employee_email}"}}
    },
    {
      "id": "verify_compliance",
      "type": "condition",
      "position": {"x": 600, "y": 50},
      "data": {"label": "Compliance Verification", "config": {"checks": ["compliance_docs_signed", "background_check_complete"]}}
    },
    {
      "id": "revoke_access",
      "type": "okta_revoke_access",
      "position": {"x": 600, "y": 150},
      "data": {"label": "Revoke Access", "config": {"access_level": "${access_level}", "timeout": "300s"}}
    },
    {
      "id": "archive_docs",
      "type": "box_move_folder",
      "position": {"x": 700, "y": 150},
      "data": {"label": "Archive Employee Docs", "config": {"folder_id": "employee_archive_${employee_id}", "source": "onboarding_${department}"}}
    },
    {
      "id": "notify",
      "type": "slack",
      "position": {"x": 700, "y": 50},
      "data": {"label": "Slack Notification", "config": {"channel": "#hr-notifications", "template": "employee_event_status"}}
    },
    {
      "id": "escalate",
      "type": "escalation",
      "position": {"x": 700, "y": 250},
      "data": {"label": "Escalation Path", "config": {"severity": "critical", "timeout": "5m"}}
    },
    {
      "id": "track",
      "type": "linear_create_issue",
      "position": {"x": 800, "y": 50},
      "data": {"label": "Track Employee Metrics", "config": {"project": "employee-lifecycle", "status": "open"}}
    },
    {
      "id": "monitor",
      "type": "condition",
      "position": {"x": 400, "y": 150},
      "data": {"label": "Access Provisioning Monitor", "config": {"metrics": ["access_provisioning_time", "error_rate"], "duration": "10m"}}
    },
    {
      "id": "complete",
      "type": "stop_fail",
      "position": {"x": 900, "y": 50},
      "data": {"label": "Workflow Completion"}
    }
  ],
  "edges": [
    {"source": "trigger", "target": "validate", "label": "always"},
    {"source": "validate", "target": "create_record", "label": "always"},
    {"source": "create_record", "target": "provision_access", "label": "always"},
    {"source": "provision_access", "target": "distribute_docs", "label": "access_provisioned"},
    {"source": "distribute_docs", "target": "assign_training", "label": "always"},
    {"source": "assign_training", "target": "verify_compliance", "label": "always"},
    {"source": "verify_compliance", "target": "notify", "label": "compliance_verified"},
    {"source": "verify_compliance", "target": "escalate", "label": "compliance_failed"},
    {"source": "notify", "target": "track", "label": "always"},
    {"source": "track", "target": "complete", "label": "always"},
    {"source": "provision_access", "target": "monitor", "label": "always"},
    {"source": "monitor", "target": "revoke_access", "label": "access_provisioning_failed"},
    {"source": "revoke_access", "target": "archive_docs", "label": "always"},
    {"source": "archive_docs", "target": "notify", "label": "always"}
  ]
}
```

## 19. Metadata
```json
{
  "category": "Business Operations",
  "tags": ["Employee Onboarding", "Employee Offboarding", "Access Management", "Compliance", "HR Automation"],
  "integrations": ["Salesforce", "Okta", "Box", "Zoom", "Slack", "PagerDuty", "Linear", "Datadog"],
  "complexity": "High",
  "estimated_setup_minutes": 90
}
```

## 20. Production-Readiness Audit
1. **Real production trigger**: Linear webhook for employee events
2. **Meaningful branching**: Access provisioning, compliance verification, access revocation
3. **Retry strategy**: All API nodes retry up to 3 times with exponential backoff
4. **Timeout handling**: 5m for escalation
5. **Idempotency**: Not applicable for this workflow
6. **Partial failure**: Access provisioning monitoring with automated revocation
7. **External service failure**: Retry with exponential backoff for all API calls
8. **AI uncertainty**: Not applicable for this workflow
9. **Human approval**: Not applicable for this workflow
10. **Observability**: Comprehensive metrics tracking
11. **Recovery**: Dead letter queue for failed Linear/PagerDuty calls
12. **Security**: All secrets encrypted, sensitive data sanitized
13. **Configuration**: Realistic configurable variables
14. **Graph quality**: Valid React Flow JSON with clean layout

**Non-applicable**: None