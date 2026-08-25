package com.flowops.workflow.validation;

import java.util.List;

/**
 * The outcome of validating a graph. {@code valid} is true only when there are no
 * {@code ERROR}-severity issues; warnings do not block publishing.
 */
public record ValidationResult(boolean valid, List<ValidationIssue> issues) {

    public static ValidationResult of(List<ValidationIssue> issues) {
        boolean valid = issues.stream().noneMatch(i -> i.severity() == Severity.ERROR);
        return new ValidationResult(valid, issues);
    }
}
