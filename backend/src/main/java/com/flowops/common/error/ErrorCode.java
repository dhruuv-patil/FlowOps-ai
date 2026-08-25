package com.flowops.common.error;

import org.springframework.http.HttpStatus;

/**
 * The exhaustive M1 error catalogue (API contract §3.1).
 *
 * <p>Clients branch on the code, never on the message, so these names are part of
 * the wire contract: renaming one is a breaking change. The mirrored TypeScript
 * union lives in {@code frontend/types/index.ts}.
 *
 * <p>Messages are safe to show to end users: no stack trace, SQL, class name,
 * email address, token or password ever appears in one.
 */
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "The request body could not be read."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Content-Type must be application/json."),

    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
    /** Identical for an unknown email and a wrong password — see contract §3.2. */
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Incorrect email or password."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access token has expired."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Access token is invalid."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED,
            "Your session has expired. Please sign in again."),

    FORBIDDEN_ROLE(HttpStatus.FORBIDDEN, "You do not have permission to do that."),
    NO_ORGANIZATION_CONTEXT(HttpStatus.FORBIDDEN, "You do not belong to any organization."),

    /** Returned both for a nonexistent org and for a non-member — §2.1. */
    ORGANIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Organization not found."),
    /** Returned for a nonexistent workflow and for one owned by another org. */
    WORKFLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "Workflow not found."),
    /** A publish (or other transition) was blocked by graph validation errors. */
    WORKFLOW_INVALID(HttpStatus.UNPROCESSABLE_ENTITY,
            "The workflow has validation errors and cannot be published."),
    /** A run was requested for a workflow that has no published version yet. */
    WORKFLOW_NOT_PUBLISHED(HttpStatus.UNPROCESSABLE_ENTITY,
            "Publish a version of this workflow before running it."),
    /** Returned for a nonexistent execution and for one owned by another org. */
    EXECUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Execution not found."),
    /** Retry was requested for an execution that is not in a failed state. */
    EXECUTION_NOT_RETRYABLE(HttpStatus.CONFLICT,
            "Only a failed execution can be retried."),
    /** An approval decision was submitted for a node that is not awaiting one. */
    APPROVAL_NOT_PENDING(HttpStatus.CONFLICT,
            "This step is not awaiting an approval decision."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "That method is not supported for this path."),

    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "That email address is already registered."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please try again later."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    /**
     * Best-effort mapping used only by the {@code /error} fallback, which sees a
     * status code with no exception attached.
     */
    public static ErrorCode fromStatus(int status) {
        return switch (status) {
            case 400 -> MALFORMED_REQUEST;
            case 401 -> AUTHENTICATION_REQUIRED;
            case 403 -> FORBIDDEN_ROLE;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 429 -> RATE_LIMITED;
            default -> status >= 400 && status < 500 ? NOT_FOUND : INTERNAL_ERROR;
        };
    }
}
