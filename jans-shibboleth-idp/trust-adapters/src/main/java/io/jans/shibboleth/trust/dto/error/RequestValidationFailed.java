package io.jans.shibboleth.trust.dto.error;

import io.jans.kernel.DomainError;

import java.util.Collections;
import java.util.List;

/**
 * A request body failed validation, carrying one {@link Violation} per offending field.
 *
 * <p>This is the boundary's own error, not a domain error: it exists so the field-level detail
 * survives inside a {@code Result} until the transport layer can render it. The transport layer
 * reads {@link #getViolations()} onto a problem response's {@code violations} array rather than
 * re-deriving anything from {@link #getMessage()}.
 */
public final class RequestValidationFailed extends DomainError {

    private final List<Violation> violations;

    private RequestValidationFailed(List<Violation> violations) {

        super(violations.size() == 1
            ? "Request validation failed: 1 invalid field"
            : String.format("Request validation failed: %d invalid fields", violations.size()));

        this.violations = Collections.unmodifiableList(violations);
    }

    public static RequestValidationFailed with(List<Violation> violations) {

        if (violations == null || violations.isEmpty()) {

            throw new IllegalArgumentException("RequestValidationFailed requires at least one violation");
        }

        return new RequestValidationFailed(violations);
    }

    public List<Violation> getViolations() {

        return violations;
    }
}
