# Template 16: Inbound Lead Enrichment, Scoring & Routing

## Overview
This template handles inbound lead enrichment, scoring, and intelligent routing to the appropriate sales team or channel based on lead quality and business rules.

## Supported Integrations
- **CRM**: HubSpot, Salesforce
- **Payment**: Stripe
- **Communication**: Slack
- **Authentication**: OAuth2 (Generic Provider)

## 20-Section React Flow JSON Structure
```json
{
  "id": "template_16",
  "name": "Inbound Lead Enrichment, Scoring & Routing",
  "version": "1.0",
  "description": "Enriches inbound leads with public/private data, scores them, and routes to appropriate sales channels",
  "nodes": [
    {
      "id": "start",
      "type": "start",
      "data": {
        "label": "Start Flow"
      }
    },
    {
      "id": "lead_inbound",
      "type": "http",
      "data": {
        "label": "Receive Inbound Lead",
        "method": "POST",
        "path": "/api/leads/inbound",
        "description": "Endpoint for receiving inbound leads via webhook or API call",
        "retryStrategy": {
          "maxRetries": 3,
          "backoff": {
            "type": "exponential",
            "base": 1000,
            "max": 5000
          }
        },
        "timeout": 30000,
        "idempotencyKey": "${request.headers['X-Idempotency-Key']}"
      }
    },
    {
      "id": "validate_inbound",
      "type": "validation",
      "data": {
        "label": "Validate Inbound Lead",
        "description": "Validate required fields and schema",
        "validations": [
          {
            "field": "email",
            "required": true,
            "type": "email"
          },
          {
            "field": "source",
            "required": true,
            "allowedValues": ["website", "form", "chatbot", "social", "email"],
            "description": "Lead source channel"
          },
          {
            "field": "company",
            "required": false,
            "type": "string"
          }
        ],
        "errorHandling": {
          "invalid": "reject",
          "missing": "reject"
        }
      }
    },
    {
      "id": "enrich_public_data",
      "type": "enrichment",
      "data": {
        "label": "Enrich with Public Data",
        "description": "Append public data (company info, tech stack, etc.)",
        "providers": [
          {
            "type": "api",
            "name": "Clearbit",
            "endpoint": "https://api.clearbit.com/v2/companies/enrich",
            "method": "POST",
            "headers": {
              "Authorization": "Bearer ${CLEARBIT_API_KEY}"
            },
            "retryStrategy": {
              "maxRetries": 2,
              "backoff": {
                "type": "exponential",
                "base": 500
              }
            },
            "timeout": 15000
          }
        ],
        "idempotencyKey": "${request.headers['X-Idempotency-Key']}-public"
      }
    },
    {
      "id": "enrich_crm_data",
      "type": "enrichment",
      "data": {
        "label": "Enrich with CRM Data",
        "description": "Append CRM-specific data (existing contacts, deals, etc.)",
        "providers": [
          {
            "type": "hubspot",
            "name": "HubSpot CRM",
            "endpoint": "https://api.hubapi.com/crm/v3/objects/contacts",
            "method": "POST",
            "retryStrategy": {
              "maxRetries": 3,
              "backoff": {
                "type": "exponential",
                "base": 1000
              }
            },
            "timeout": 20000
          },
          {
            "type": "salesforce",
            "name": "Salesforce CRM",
            "endpoint": "https://${SF_INSTANCE}.salesforce.com/services/data/v56.0/sobjects/Lead",
            "method": "POST",
            "retryStrategy": {
              "maxRetries": 3,
              "backoff": {
                "type": "exponential",
                "base": 1000
              }
            },
            "timeout": 20000
          }
        ],
        "idempotencyKey": "${request.headers['X-Idempotency-Key']}-crm"
      }
    },
    {
      "id": "enrich_payment_data",
      "type": "enrichment",
      "data": {
        "label": "Enrich with Payment Data",
        "description": "Check Stripe for payment history and intent",
        "providers": [
          {
            "type": "stripe",
            "name": "Stripe API",
            "endpoint": "https://api.stripe.com/v1/customers",
            "method": "GET",
            "headers": {
              "Authorization": "Bearer ${STRIPE_API_KEY}"
            },
            "retryStrategy": {
              "maxRetries": 2,
              "backoff": {
                "type": "exponential",
                "base": 500
              }
            },
            "timeout": 10000
          }
        ],
        "idempotencyKey": "${request.headers['X-Idempotency-Key']}-payment"
      }
    },
    {
      "id": "calculate_score",
      "type": "scoring",
      "data": {
        "label": "Calculate Lead Score",
        "description": "Score lead based on enrichment data and business rules",
        "rules": [
          {
            "field": "company_size",
            "score": 10,
            "thresholds": {
              "small": 1,
              "medium": 5,
              "large": 10
            }
          },
          {
            "field": "tech_stack",
            "score": 5,
            "matchingTech": ["AWS", "Azure", "GCP", "Kubernetes"],
            "scorePerMatch": 1
          },
          {
            "field": "payment_intent",
            "score": 20,
            "hasIntent": true
          },
          {
            "field": "source",
            "score": {
              "website": 5,
              "form": 10,
              "chatbot": 15,
              "social": 8,
              "email": 12
            }
          },
          {
            "field": "engagement_score",
            "score": 10,
            "thresholds": {
              "low": 0,
              "medium": 5,
              "high": 10
            }
          }
        ],
        "maxScore": 100,
        "defaultScore": 30
      }
    },
    {
      "id": "classify_lead",
      "type": "classification",
      "data": {
        "label": "Classify Lead",
        "description": "Classify lead into business categories",
        "categories": [
          {
            "name": "Hot Prospect",
            "criteria": {
              "score": {"gt": 80},
              "companySize": {"gte": 50},
              "paymentIntent": true
            }
          },
          {
            "name": "Warm Prospect",
            "criteria": {
              "score": {"gte": 50, "lt": 80},
              "companySize": {"gte": 10}
            }
          },
          {
            "name": "Cold Prospect",
            "criteria": {
              "score": {"lt": 50}
            }
          },
          {
            "name": "Marketing Lead",
            "criteria": {
              "source": {"in": ["social", "website"]},
              "score": {"lt": 30}
            }
          }
        ],
        "defaultCategory": "Cold Prospect"
      }
    },
    {
      "id": "determine_routing",
      "type": "routing",
      "data": {
        "label": "Determine Routing",
        "description": "Route lead based on classification and business rules",
        "rules": [
          {
            "category": "Hot Prospect",
            "priority": "high",
            "destinations": [
              {
                "type": "sales_team",
                "team": "enterprise_sales",
                "slack_channel": "#enterprise-sales"
              },
              {
                "type": "email",
                "template": "hot_prospect_followup"
              }
            ]
          },
          {
            "category": "Warm Prospect",
            "priority": "medium",
            "destinations": [
              {
                "type": "sales_team",
                "team": "account_executives",
                "slack_channel": "#account-executives"
              },
              {
                "type": "email",
                "template": "warm_prospect_followup"
              }
            ]
          },
          {
            "category": "Cold Prospect",
            "priority": "low",
            "destinations": [
              {
                "type": "marketing",
                "campaign": "nurture_cold_leads"
              },
              {
                "type": "email",
                "template": "cold_prospect_nurture"
              }
            ]
          },
          {
            "category": "Marketing Lead",
            "priority": "low",
            "destinations": [
              {
                "type": "marketing",
                "campaign": "lead_nurturing"
              },
              {
                "type": "slack",
                "channel": "#marketing-leads"
              }
            ]
          }
        ],
        "fallbackDestination": {
          "type": "slack",
          "channel": "#lead-routing-fallback"
        }
      }
    },
    {
      "id": "notify_sales_team",
      "type": "notification",
      "data": {
        "label": "Notify Sales Team",
        "description": "Notify appropriate sales team via Slack",
        "providers": [
          {
            "type": "slack",
            "endpoint": "https://slack.com/api/chat.postMessage",
            "method": "POST",
            "headers": {
              "Authorization": "Bearer ${SLACK_BOT_TOKEN}"
            },
            "template": "{ "text": "New lead from ${source}: ${name} (${email})", \n                      "blocks": [\n                        { "type": "section", "text": { "type": "mrkdwn", "text": "*Lead Details:*" } },\n                        { "type": "divider" },\n                        { "type": "section", "fields": [\n                          { "type": "mrkdwn", "text": "*Score*:", "short": true },\n                          { "type": "mrkdwn", "text": "${score}/100", "short": true }\n                        ] },
                        { "type": "section", "fields": [\n                          { "type": "mrkdwn", "text": "*Category*:", "short": true },\n                          { "type": "mrkdwn", "text": "${category}", "short": true }\n                        ] },
                        { "type": "actions", "elements": [\n                          { "type": "button", "text": { "type": "plain_text", "text": "View in CRM" }, \n                            "url": "${crm_url}", "style": "primary" }\n                        ] }\n                      ] }\n                    "}",
            "retryStrategy": {
              "maxRetries": 2,
              "backoff": {
                "type": "exponential",
                "base": 500
              }
            },
            "timeout": 10000
          }
        ],
        "idempotencyKey": "${request.headers['X-Idempotency-Key']}-notify"
      }
    },
    {
      "id": "update_crm",
      "type": "crm_update",
      "data": {
        "label": "Update CRM",
        "description": "Update CRM with enriched lead data",
        "providers": [
          {
            "type": "hubspot",
            "endpoint": "https://api.hubapi.com/crm/v3/objects/contacts/${contactId}",
            "method": "PATCH",
            "retryStrategy": {
              "maxRetries": 3,
              "backoff": {
                "type": "exponential",
                "base": 1000
              }
            },
            "timeout": 20000
          },
          {
            "type": "salesforce",
            "endpoint": "https://${SF_INSTANCE}.salesforce.com/services/data/v56.0/sobjects/Lead/${leadId}",
            "method": "PATCH",
            "retryStrategy": {
              "maxRetries": 3,
              "backoff": {
                "type": "exponential",
                "base": 1000
              }
            },
            "timeout": 20000
          }
        ],
        "idempotencyKey": "${request.headers['X-Idempotency-Key']}-crm-update"
      }
    },
    {
      "id": "send_followup_email",
      "type": "email",
      "data": {
        "label": "Send Followup Email",
        "description": "Send personalized followup email based on lead category",
        "templates": {
          "hot_prospect_followup": "hot_prospect_template.html",
          "warm_prospect_followup": "warm_prospect_template.html",
          "cold_prospect_nurture": "cold_prospect_template.html"
        },
        "retryStrategy": {
          "maxRetries": 2,
          "backoff": {
            "type": "exponential",
            "base": 500
          }
        },
        "timeout": 15000
      }
    },
    {
      "id": "track_engagement",
      "type": "analytics",
      "data": {
        "label": "Track Engagement",
        "description": "Track lead engagement metrics",
        "metrics": [
          "first_contact_time",
          "response_time",
          "conversion_rate",
          "engagement_score"
        ],
        "providers": [
          {
            "type": "google_analytics",
            "endpoint": "https://www.google-analytics.com/mp/collect",
            "method": "POST",
            "headers": {
              "Content-Type": "application/json"
            },
            "retryStrategy": {
              "maxRetries": 2,
              "backoff": {
                "type": "exponential",
                "base": 500
              }
            },
            "timeout": 10000
          }
        ],
        "idempotencyKey": "${request.headers['X-Idempotency-Key']}-engagement"
      }
    },
    {
      "id": "handle_failure",
      "type": "error_handling",
      "data": {
        "label": "Handle Failure",
        "description": "Handle various failure scenarios",
        "rules": [
          {
            "errorType": "validation_error",
            "action": "reject",
            "message": "Lead validation failed: ${error.message}"
          },
          {
            "errorType": "enrichment_failure",
            "action": "retry",
            "maxRetries": 2,
            "backoff": {
              "type": "exponential",
              "base": 1000
            },
            "fallback": "notify_admin"
          },
          {
            "errorType": "routing_failure",
            "action": "fallback_routing",
            "fallbackChannel": "#lead-routing-fallback"
          },
          {
            "errorType": "timeout",
            "action": "retry",
            "maxRetries": 1,
            "backoff": {
              "type": "fixed",
              "delay": 5000
            },
            "fallback": "notify_admin"
          },
          {
            "errorType": "idempotency_conflict",
            "action": "skip",
            "message": "Duplicate processing detected"
          }
        ],
        "adminNotification": {
          "slackChannel": "#lead-processing-errors",
          "template": "{ "text": "Lead processing error: ${error.type} - ${error.message}", \n                      "blocks": [\n                        { "type": "section", "text": { "type": "mrkdwn", "text": "*Lead Details:*" } },\n                        { "type": "divider" },\n                        { "type": "section", "fields": [\n                          { "type": "mrkdwn", "text": "*Lead ID*:", "short": true },\n                          { "type": "mrkdwn", "text": "${lead.id}", "short": true }\n                        ] },
                        { "type": "section", "fields": [\n                          { "type": "mrkdwn", "text": "*Error Type*:", "short": true },\n                          { "type": "mrkdwn", "text": "${error.type}", "short": true }\n                        ] }\n                      ] }\n                    "}"
        }
      }
    },
    {
      "id": "log_observability",
      "type": "observability",
      "data": {
        "label": "Log Observability Data",
        "description": "Log comprehensive observability data",
        "metrics": [
          "processing_time",
          "enrichment_time",
          "scoring_time",
          "routing_time",
          "error_rate"
        ],
        "logs": [
          "lead_processed",
          "enrichment_results",
          "routing_decision",
          "notification_sent"
        ],
        "providers": [
          {
            "type": "structured_logging",
            "endpoint": "https://api.logging-service.com/v1/logs",
            "method": "POST",
            "retryStrategy": {
              "maxRetries": 2,
              "backoff": {
                "type": "exponential",
                "base": 500
              }
            },
            "timeout": 10000
          },
          {
            "type": "metrics",
            "endpoint": "https://api.metrics-service.com/v1/metrics",
            "method": "POST",
            "retryStrategy": {
              "maxRetries": 2,
              "backoff": {
                "type": "exponential",
                "base": 500
              }
            },
            "timeout": 10000
          }
        ]
      }
    },
    {
      "id": "complete_flow",
      "type": "end",
      "data": {
        "label": "Flow Complete"
      }
    }
  ],
  "edges": [
    {
      "source": "start",
      "target": "lead_inbound",
      "label": "Receive Lead"
    },
    {
      "source": "lead_inbound",
      "target": "validate_inbound",
      "label": "Validate"
    },
    {
      "source": "validate_inbound",
      "target": "enrich_public_data",
      "label": "Enrich Public"
    },
    {
      "source": "validate_inbound",
      "target": "handle_failure",
      "label": "Validation Failed",
      "error": true
    },
    {
      "source": "enrich_public_data",
      "target": "enrich_crm_data",
      "label": "Enrich CRM"
    },
    {
      "source": "enrich_public_data",
      "target": "handle_failure",
      "label": "Public Enrichment Failed",
      "error": true
    },
    {
      "source": "enrich_crm_data",
      "target": "enrich_payment_data",
      "label": "Enrich Payment"
    },
    {
      "source": "enrich_crm_data",
      "target": "handle_failure",
      "label": "CRM Enrichment Failed",
      "error": true
    },
    {
      "source": "enrich_payment_data",
      "target": "calculate_score",
      "label": "Calculate Score"
    },
    {
      "source": "enrich_payment_data",
      "target": "handle_failure",
      "label": "Payment Enrichment Failed",
      "error": true
    },
    {
      "source": "calculate_score",
      "target": "classify_lead",
      "label": "Classify"
    },
    {
      "source": "classify_lead",
      "target": "determine_routing",
      "label": "Route"
    },
    {
      "source": "determine_routing",
      "target": "notify_sales_team",
      "label": "Notify"
    },
    {
      "source": "determine_routing",
      "target": "update_crm",
      "label": "Update CRM"
    },
    {
      "source": "notify_sales_team",
      "target": "send_followup_email",
      "label": "Send Email"
    },
    {
      "source": "update_crm",
      "target": "send_followup_email",
      "label": "Send Email"
    },
    {
      "source": "send_followup_email",
      "target": "track_engagement",
      "label": "Track Engagement"
    },
    {
      "source": "track_engagement",
      "target": "log_observability",
      "label": "Log Observability"
    },
    {
      "source": "log_observability",
      "target": "complete_flow",
      "label": "Complete"
    },
    {
      "source": "handle_failure",
      "target": "log_observability",
      "label": "Log Error",
      "error": true
    },
    {
      "source": "handle_failure",
      "target": "complete_flow",
      "label": "Complete with Error",
      "error": true
    }
  ],
  "reliability": {
    "circuitBreaker": {
      "enabled": true,
      "failureThreshold": 5,
      "resetTimeout": 300000,
      "allowedCallsInHalfOpen": 1
    },
    "retryPolicy": {
      "default": {
        "maxRetries": 3,
        "backoff": {
          "type": "exponential",
          "base": 1000,
          "max": 5000
        }
      },
      "critical": {
        "maxRetries": 5,
        "backoff": {
          "type": "exponential",
          "base": 2000,
          "max": 10000
        }
      }
    },
    "timeoutPolicy": {
      "default": 30000,
      "critical": 60000
    },
    "idempotency": {
      "enabled": true,
      "keyPrefix": "lead_"
    },
    "observability": {
      "metrics": [
        "processing_time",
        "error_rate",
        "retry_count",
        "success_rate"
      ],
      "logs": [
        "lead_processed",
        "enrichment_results",
        "routing_decision",
        "notification_sent",
        "error_occurred"
      ],
      "traces": [
        "lead_flow_trace"
      ]
    }
  },
  "flowopsDifferentiators": {
    "reliability": {
      "description": "Implements comprehensive retry strategies with exponential backoff, circuit breakers, and idempotency keys for all external integrations",
      "features": [
        "Per-step retry policies",
        "Circuit breaker pattern",
        "Idempotency key generation",
        "Comprehensive timeout handling"
      ]
    },
    "observability": {
      "description": "Provides detailed logging, metrics, and tracing for all critical operations",
      "features": [
        "Structured logging",
        "Comprehensive metrics collection",
        "Distributed tracing",
        "Error classification and handling"
      ]
    },
    "scalability": {
      "description": "Designed to handle high volumes of leads with parallel processing capabilities",
      "features": [
        "Non-blocking integrations",
        "Asynchronous processing",
        "Resource-efficient design"
      ]
    }
  }
}

## Key Features

1. **Comprehensive Enrichment**: Combines public data (Clearbit), CRM data (HubSpot/Salesforce), and payment data (Stripe)
2. **Intelligent Scoring**: Multi-factor scoring system with configurable weights
3. **Smart Routing**: Dynamic routing based on lead classification and business rules
4. **Production-Grade Reliability**: 
   - Circuit breakers for external integrations
   - Comprehensive retry strategies
   - Idempotency keys for all operations
   - Timeout handling at all stages
5. **Observability**: 
   - Structured logging for all operations
   - Comprehensive metrics collection
   - Distributed tracing
6. **Error Handling**: 
   - Graceful degradation
   - Fallback mechanisms
   - Admin notifications for critical errors
7. **Supported Integrations**: Only uses officially supported integrations (HubSpot, Salesforce, Stripe, Slack)

## Usage Instructions

1. Deploy this template to your FlowOps environment
2. Configure required API keys in your environment variables
3. Set up webhook endpoints for inbound lead reception
4. Configure Slack channels and email templates
5. Monitor the flow using FlowOps observability dashboard

## Reliability Patterns

- **Retry Strategies**: Exponential backoff for all external API calls
- **Circuit Breakers**: Prevent cascading failures in external integrations
- **Idempotency**: All operations support idempotency keys
- **Timeouts**: Strict timeouts for all external calls
- **Fallbacks**: Multiple fallback mechanisms for routing and notifications

## Observability Features

- **Structured Logging**: All operations logged with context
- **Metrics Collection**: Processing times, error rates, success rates
- **Distributed Tracing**: End-to-end flow tracing
- **Error Classification**: Detailed error categorization and handling

## Supported Integrations List

| Integration      | Type          | Purpose                          | Configuration Required          |
|------------------|---------------|---------------------------------|---------------------------------|
| HubSpot          | CRM           | Lead enrichment & updates       | API Key                          |
| Salesforce       | CRM           | Lead enrichment & updates       | Instance URL, API Key            |
| Clearbit         | Data Enrichment| Public company data             | API Key                          |
| Stripe           | Payment       | Payment intent checking          | API Key                          |
| Slack            | Notification  | Team notifications              | Bot Token                        |
| Google Analytics | Analytics     | Engagement tracking             | Measurement ID                    |

## Failure Handling

The template implements comprehensive failure handling:

1. **Validation Failures**: Immediate rejection with detailed error message
2. **Enrichment Failures**: Automatic retry with exponential backoff, then fallback to admin notification
3. **Routing Failures**: Fallback to generic routing channel
4. **Timeouts**: Automatic retry with increased timeout, then fallback
5. **Idempotency Conflicts**: Graceful skip with logging
6. **Critical Errors**: Immediate admin notification

## Production Readiness

This template is production-ready with:

- Comprehensive error handling
- Reliable retry mechanisms
- Observability at every stage
- Support for high volume processing
- Secure integration with supported providers
- Configurable business rules

## Next Steps

1. Review the template structure and integrations
2. Configure required API keys and credentials
3. Set up monitoring and alerting for the flow
4. Test with sample leads before production deployment
5. Monitor performance and adjust scoring rules as needed