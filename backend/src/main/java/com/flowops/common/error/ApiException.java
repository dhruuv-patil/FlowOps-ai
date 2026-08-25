package com.flowops.common.error;

import java.util.List;

/**
 * The single exception type every deliberate API failure is expressed as.
 *
 * <p>Carrying an {@link ErrorCode} means the HTTP status and the wire code can
 * never drift apart: the handler reads both off the code.
 */
public class ApiException extends RuntimeException {

    private final transient ErrorCode code;
    private final transient List<FieldErrorDetail> fieldErrors;
    private final transient Long retryAfterSeconds;

    public ApiException(ErrorCode code) {
        this(code, code.defaultMessage(), null, null);
    }

    public ApiException(ErrorCode code, String message) {
        this(code, message, null, null);
    }

    public ApiException(ErrorCode code, String message, List<FieldErrorDetail> fieldErrors) {
        this(code, message, fieldErrors, null);
    }

    public ApiException(
            ErrorCode code,
            String message,
            List<FieldErrorDetail> fieldErrors,
            Long retryAfterSeconds) {
        super(message);
        this.code = code;
        this.fieldErrors = fieldErrors;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ErrorCode code() {
        return code;
    }

    /** Non-null only for {@link ErrorCode#VALIDATION_ERROR}. */
    public List<FieldErrorDetail> fieldErrors() {
        return fieldErrors;
    }

    /** Non-null only for {@link ErrorCode#RATE_LIMITED}; emitted as {@code Retry-After}. */
    public Long retryAfterSeconds() {
        return retryAfterSeconds;
    }

    /** A single-field validation failure raised outside Bean Validation. */
    public static ApiException validation(String field, String message) {
        return new ApiException(
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(new FieldErrorDetail(field, message)));
    }

    /** A throttled request. The wait is echoed in the message and in {@code Retry-After}. */
    public static ApiException rateLimited(long retryAfterSeconds) {
        return new ApiException(
                ErrorCode.RATE_LIMITED,
                "Too many attempts. Please try again in " + retryAfterSeconds + " seconds.",
                null,
                retryAfterSeconds);
    }
}
