package io.jans.adapter.error;

import io.jans.kernel.RequiredValueMissing;

/**
 * The codes every bounded context shares, because the errors behind them are the shared kernel's.
 *
 * <p>{@link RequiredValueMissing} is raised by every domain, so its code and wording are fixed here
 * rather than restated per context: two contexts spelling the same failure differently is a broken
 * contract for any client that talks to both. A context's own error types get their codes in its own
 * {@link ErrorTranslation}.
 */
public final class KernelErrorCodes {

    /**
     * Code reported when an error reaches the boundary with no registered translation. Seeing it in
     * a response means a context's {@link ErrorTranslation} is out of date.
     */
    public static final String UNEXPECTED = "unexpected_error";

    public static final String REQUIRED_VALUE_MISSING = "required_value_missing";

    /**
     * Wording for {@link #REQUIRED_VALUE_MISSING}, as a template taking the request-body field.
     */
    public static final String REQUIRED_VALUE_MISSING_TEMPLATE = "'%s' is required";

    private KernelErrorCodes() {
    }

    /**
     * Prose for an error no context registered, kept deliberately uninformative — an unregistered
     * error is one nobody has decided how to explain, so guessing would mislead.
     */
    public static String unexpectedMessage(String field) {

        return field.isEmpty()
            ? "The request could not be processed"
            : String.format("'%s' could not be processed", field);
    }
}
