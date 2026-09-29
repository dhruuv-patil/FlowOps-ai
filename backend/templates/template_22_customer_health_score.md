# Customer Health Score Calculation & Alerting Workflow

## Template Metadata

**Template Name:** Customer Health Score Calculation & Alerting
**Version:** 1.0
**Author:** Claude Opus 4.5
**Last Updated:** 2026-09-29
**Workflow Type:** Business Intelligence & Monitoring
**Use Case Category:** Customer Success

## Description

This workflow calculates the **Customer Health Score (CHS)** for each customer based on predefined metrics and triggers alerts when the score falls below a predefined threshold. The CHS is a composite score derived from multiple data sources, including customer activity, support interactions, and usage patterns. Alerts are sent via supported integrations (e.g., Slack, Email, or internal notifications) to ensure proactive intervention.

## Use Case

- **Customer Success Teams:** Monitor customer health in real-time to identify at-risk customers.
- **Support Teams:** Trigger escalations for customers with declining health scores.
- **Product Teams:** Identify usage patterns and potential churn risks.
- **Business Intelligence:** Use CHS for predictive analytics and proactive engagement strategies.

## Key Features

- **Multi-Metric Calculation:** Aggregates scores from multiple dimensions (e.g., engagement, support interactions, usage frequency).
- **Dynamic Thresholds:** Configurable thresholds for alerting based on customer segment or business needs.
- **Integration Support:** Works with HTTP, MongoDB, PostgreSQL, GitHub, Slack, Email, and other supported integrations.
- **Alert Customization:** Supports customizable alert messages and escalation paths.
- **Audit Logging:** Comprehensive logging for monitoring and debugging.
- **Scalability:** Designed to handle large volumes of customer data efficiently.

## Supported Integrations

| Category          | Integration          | Description
|--------------------|---------------------|-------------
| **Data Sources**  | MongoDB             | Stores customer health data.
|                    | PostgreSQL          | Stores aggregated customer metrics.
|                    | HTTP (REST API)      | External data sources (e.g., usage analytics).
|                    | GitHub              | For tracking customer activity (e.g., PRs, issues).
| **Alerting**      | Slack              | Notifies teams via Slack channels.
|                    | Email               | Sends email alerts to stakeholders.
|                    | Webhook Deliverer   | Delivers alerts via HTTP webhooks.

## Prerequisites

- **Environment Variables:** `JWT_SECRET`, `MONGO_URI`, `POSTGRES_URI`, `SLACK_WEBHOOK_URL`, `EMAIL_SMTP_CONFIG`.
- **Database:** MongoDB and PostgreSQL databases configured with customer health data schemas.
- **API Keys:** Valid API keys for external integrations (e.g., GitHub, Slack).
- **Permissions:** Appropriate permissions for data access and alerting channels.
- **Dependencies:** Required libraries (e.g., `reactflow`, `axios`, `mongoose`, `bcrypt`, `nodemailer`).

## Workflow Overview

The workflow consists of the following phases:

1. **Data Collection:** Gather customer health data from MongoDB, PostgreSQL, and external APIs.
2. **Score Calculation:** Compute the CHS based on predefined metrics (e.g., engagement, support interactions, usage).
3. **Threshold Check:** Compare the CHS against configurable thresholds.
4. **Alert Trigger:** Send alerts via Slack, Email, or other supported integrations if thresholds are breached.
5. **Logging & Monitoring:** Log all workflow activities for auditing and debugging.

## Data Sources

| Source               | Data Type
|----------------------|----------
| MongoDB (Customer Data) | Customer engagement, support tickets, usage metrics.
| PostgreSQL (Metrics)  | Aggregated customer health metrics.
| GitHub API           | Customer activity (e.g., PRs, issues).
| External APIs        | Usage analytics, support logs.

## Input Parameters

| Parameter                     | Type       | Description
|-------------------------------|------------|-------------
| `customerId`                  | String     | Unique identifier for the customer.
| `threshold`                   | Number     | Minimum CHS threshold for alerts (e.g., 50).
| `alertSeverity`               | String     | Severity level for alerts (e.g., `high`, `medium`, `low`).
| `alertChannel`                | String     | Channel for alerts (e.g., `slack`, `email`).
| `metricsConfig`               | Object     | Configuration for scoring metrics (e.g., weights for engagement, support interactions).

## Output Parameters

| Parameter                     | Type       | Description
|-------------------------------|------------|-------------
| `customerHealthScore`         | Number     | Calculated CHS for the customer.
| `alertStatus`                 | Boolean    | `true` if an alert was triggered, `false` otherwise.
| `alertMessage`                | String     | Customized alert message.
| `logEntry`                    | Object     | Audit log entry for monitoring.

## Nodes

The workflow consists of the following nodes in React Flow JSON format:

```json
{
  "nodes": [
    {
      "id": "data-collection",
      "type": "data-collection",
      "data": {
        "label": "Data Collection",
        "description": "Gather customer health data from MongoDB, PostgreSQL, and external APIs."
      }
    },
    {
      "id": "score-calculation",
      "type": "score-calculation",
      "data": {
        "label": "Score Calculation",
        "description": "Compute CHS based on predefined metrics."
      }
    },
    {
      "id": "threshold-check",
      "type": "threshold-check",
      "data": {
        "label": "Threshold Check",
        "description": "Compare CHS against configurable thresholds."
      }
    },
    {
      "id": "alert-trigger",
      "type": "alert-trigger",
      "data": {
        "label": "Alert Trigger",
        "description": "Send alerts via Slack, Email, or other supported integrations."
      }
    },
    {
      "id": "logging",
      "type": "logging",
      "data": {
        "label": "Logging",
        "description": "Log workflow activities for auditing and debugging."
      }
    }
  ]
}
```

## Connections

The workflow connections define the data flow between nodes:

```json
{
  "edges": [
    {
      "id": "edge-data-collection",
      "source": "data-collection",
      "target": "score-calculation",
      "type": "data",
      "label": "Customer Data"
    },
    {
      "id": "edge-score-calculation",
      "source": "score-calculation",
      "target": "threshold-check",
      "type": "data",
      "label": "CHS"
    },
    {
      "id": "edge-threshold-check",
      "source": "threshold-check",
      "target": "alert-trigger",
      "type": "control",
      "label": "Alert Triggered?"
    },
    {
      "id": "edge-alert-trigger",
      "source": "alert-trigger",
      "target": "logging",
      "type": "data",
      "label": "Alert Log"
    },
    {
      "id": "edge-logging",
      "source": "logging",
      "target": "score-calculation",
      "type": "data",
      "label": "Audit Log"
    }
  ]
}
```

## Error Handling

- **Data Collection Failures:** Retry logic with exponential backoff for transient failures (e.g., database timeouts).
- **Score Calculation Errors:** Fallback to default metrics if critical data is missing.
- **Alerting Failures:** Log errors and retry once before marking the workflow as failed.
- **Logging Errors:** Ensure logs are written to a fallback location if the primary logging system fails.

## Retry Logic

- **Max Retries:** 3 retries for transient failures (e.g., database timeouts, API rate limits).
- **Backoff Strategy:** Exponential backoff (e.g., 1s, 2s, 4s) between retries.
- **Timeout:** 10 seconds for external API calls.

## Security Considerations

- **Authentication:** Use JWT for API authentication where required.
- **Data Encryption:** Encrypt sensitive data (e.g., customer IDs, API keys) at rest and in transit.
- **Access Control:** Restrict access to workflow nodes based on user roles (e.g., admin, analyst).
- **Audit Logging:** Log all sensitive operations for compliance and debugging.

## Performance Optimization

- **Caching:** Cache frequently accessed data (e.g., customer metrics) to reduce database load.
- **Parallel Processing:** Use async/await for concurrent data collection and scoring.
- **Batch Processing:** Process large datasets in batches to avoid timeouts.
- **Optimized Queries:** Use indexed queries and efficient database queries.

## Monitoring & Logging

- **Logging:** Use structured logging (e.g., JSON logs) for all workflow activities.
- **Metrics:** Track key performance indicators (e.g., latency, success rate).
- **Alerts:** Monitor workflow health via Slack or email alerts.
- **Dashboard:** Provide a dashboard for real-time monitoring of workflow performance.

## Alerting Strategy

- **Severity Levels:** High, Medium, Low alerts based on CHS thresholds.
- **Notification Channels:** Slack, Email, and internal webhooks.
- **Escalation Paths:** Auto-escalate high-severity alerts to senior support teams.
- **Customizable Messages:** Support dynamic alert messages based on customer segment.

## Scalability

- **Horizontal Scaling:** Designed to scale horizontally for large customer bases.
- **Load Balancing:** Use load balancers for distributed workflow execution.
- **Partitioning:** Partition data by customer segment for efficient querying.
- **Database Sharding:** Shard MongoDB and PostgreSQL databases for scalability.

## Maintenance & Updates

- **Versioning:** Maintain version control for workflow updates.
- **Deprecation Policy:** Deprecate old workflow versions with clear migration paths.
- **Documentation:** Update documentation for new features and changes.
- **Testing:** Run regression tests after major updates to ensure compatibility.

--- 

**React Flow JSON for Visualization:**

```json
{
  "nodes": [
    {
      "id": "data-collection",
      "position": { "x": 100, "y": 100 },
      "data": {
        "text": "Data Collection",
        "style": {"backgroundColor": "#4CAF50"}
      }
    },
    {
      "id": "score-calculation",
      "position": { "x": 300, "y": 100 },
      "data": {
        "text": "Score Calculation",
        "style": {"backgroundColor": "#FF9800"}
      }
    },
    {
      "id": "threshold-check",
      "position": { "x": 500, "y": 100 },
      "data": {
        "text": "Threshold Check",
        "style": {"backgroundColor": "#FF5722"}
      }
    },
    {
      "id": "alert-trigger",
      "position": { "x": 700, "y": 100 },
      "data": {
        "text": "Alert Trigger",
        "style": {"backgroundColor": "#9C27B0"}
      }
    },
    {
      "id": "logging",
      "position": { "x": 900, "y": 100 },
      "data": {
        "text": "Logging",
        "style": {"backgroundColor": "#607D8B"}
      }
    }
  ],
  "edges": [
    {
      "id": "edge-data-collection",
      "source": "data-collection",
      "target": "score-calculation",
      "type": "solid",
      "style": {"strokeWidth": 2}
    },
    {
      "id": "edge-score-calculation",
      "source": "score-calculation",
      "target": "threshold-check",
      "type": "dashed",
      "style": {"strokeWidth": 2}
    },
    {
      "id": "edge-threshold-check",
      "source": "threshold-check",
      "target": "alert-trigger",
      "type": "solid",
      "style": {"strokeWidth": 2}
    },
    {
      "id": "edge-alert-trigger",
      "source": "alert-trigger",
      "target": "logging",
      "type": "dashed",
      "style": {"strokeWidth": 2}
    },
    {
      "id": "edge-logging",
      "source": "logging",
      "target": "data-collection",
      "type": "dotted",
      "style": {"strokeWidth": 2}
    }
  ]
}
```