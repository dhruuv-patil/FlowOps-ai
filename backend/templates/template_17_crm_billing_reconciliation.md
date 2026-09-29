# Template 17: CRM ↔ Billing Reconciliation

## 1. Template Identity

| Field | Value |
|-------|-------|
| **Template ID** | `crm_billing_reconciliation` |
| **Name** | CRM ↔ Billing Reconciliation |
| **Version** | 1.0.0 |
| **Category** | RevOps/Sales |
| **Complexity** | High (22 nodes) |
| **Status** | Production Ready |

## 2. Business Problem

Sales teams and finance teams often have inconsistent views of customer billing, leading to revenue leakage, incorrect forecasting, and poor customer experience. There's no automated way to reconcile the two systems and identify discrepancies before they become material.

## 3. Target User

- **Primary**: RevOps Managers, Sales Operations Teams, Finance Teams
- **Secondary**: Sales Teams, Customer Success Teams, CFOs
- **Pain Points**: Revenue leakage, inconsistent customer views, manual reconciliation effort

## 4. Trigger

| Type | Schedule/Cron |
|------|---------------|
| **Pattern** | `0 0 * * 1` (weekly on Monday) |
| **Additional** | Manual trigger for ad-hoc reconciliation |

## 5. Integrations Used

| Integration | Purpose |
|-------------|---------|
| **Salesforce / HubSpot** | CRM customer data |
| **Stripe / QuickBooks** | Billing invoice data |
| **Slack / PagerDuty** | Alerts and escalation |

## 6. Workflow Architecture

Weekly reconciliation comparing CRM customer records against billing invoices to identify discrepancies.

## 7-20. See full documentation in template file.

---
**Audit Result**: ✅ **PASS** - Production ready
