package io.jans.adapter.error;

import io.jans.kernel.RequiredValueMissing;

/**
 * The translations every bounded context shares, because the failures behind them are not any one
 * context's.
 *
 * <p>{@link RequiredValueMissing} is raised by every domain, and every context reports request
 * validation the same way, so their code, title, status and wording are fixed here rather than
 * restated per context: two contexts spelling the same failure differently is a broken contract for
 * any client that talks to both. A context's own error types get their translations in its own
 * {@link ErrorTranslation}.
 */
public final class KernelErrorCodes {

    /**
     * Reported when an error reaches the boundary with no registered translation. Seeing it in a
     * response means a context's {@link ErrorTranslation} is out of date, so it is a server-side
     * gap — 500 — rather than the caller's fault.
     */
    public static final ProblemTranslation UNEXPECTED = new ProblemTranslation(
        "unexpected_error", "Unexpected error", 500, "'%s' could not be processed");

    /**
     * A required value was absent. Field-scoped, so it is normally reported as a
     * {@link Violation} rather than as a whole problem.
     */
    public static final ProblemTranslation REQUIRED_VALUE_MISSING = new ProblemTranslation(
        "required_value_missing", "Required value missing", 400, "'%s' is required");

    /**
     * The request body carried one or more invalid fields — the problem that wraps a
     * {@link RequestValidationFailed}'s violations.
     */
    public static final ProblemTranslation VALIDATION_FAILED = new ProblemTranslation(
        "validation_failed", "Request validation failed", 400,
        "One or more fields of the request are invalid");

    private KernelErrorCodes() {
    }
}
