package com.flowops.integration.provider;

/**
 * High-level category for organizing provider integrations in the marketplace.
 */
public enum ProviderCategory {

    COMMUNICATION("Communication", "MessageSquare"),
    EMAIL("Email", "Mail"),
    DEVELOPER("Developer", "Code2"),
    DATABASES("Databases", "Database"),
    CLOUD("Cloud & Infra", "Cloud"),
    STORAGE("Storage", "HardDrive"),
    CRM("CRM & Sales", "Briefcase"),
    PROJECT_MANAGEMENT("Project Management", "Kanban"),
    PRODUCTIVITY("Productivity", "Sheet"),
    PAYMENTS("Payments & Commerce", "CreditCard"),
    MARKETING("Marketing", "Megaphone"),
    ANALYTICS("Analytics", "BarChart"),
    AI("AI & LLM", "Bot"),
    AUTOMATION("Workflow Platforms", "Zap"),
    UNIVERSAL("Generic & Universal", "Globe");

    private final String displayName;
    private final String icon;

    ProviderCategory(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String displayName() {
        return displayName;
    }

    public String icon() {
        return icon;
    }
}
