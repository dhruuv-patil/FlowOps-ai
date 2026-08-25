package com.flowops.common.error;

/** One Bean Validation violation, keyed by the bare camelCase JSON field name. */
public record FieldErrorDetail(String field, String message) {
}
