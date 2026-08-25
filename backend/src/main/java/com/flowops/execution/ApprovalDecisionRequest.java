package com.flowops.execution;

import jakarta.validation.constraints.NotNull;

/**
 * Body for resolving a Human Approval node: {@code approved} true routes the run
 * down the {@code approved} port, false down {@code rejected}. {@code note} is an
 * optional reason recorded in the run log.
 */
public record ApprovalDecisionRequest(@NotNull Boolean approved, String note) {
}
