package io.jans.shibboleth.trust.dto.error;

import io.jans.kernel.DomainError;
import io.jans.kernel.FieldPath;
import io.jans.kernel.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects the field-level failures of one request body, so a client learns about all of them at
 * once instead of one per round trip.
 *
 * <p>Mappers use it where they already hold both sides of the translation: the request field they
 * read, and the domain value they built from it.
 *
 * <pre>
 * Violations violations = Violations.create();
 *
 * DisplayName displayName = violations.take(DisplayName.of(request.getDisplayName()), "display_name");
 *
 * if (violations.any()) {
 *
 *     return violations.asFailure();
 * }
 *
 * return violations.completeWith(TrustRelationship.create(displayName, description, request.getNature()));
 * </pre>
 *
 * <p>{@link #take(Result, String)} returns {@code null} for a failed value, so every collected value
 * must be treated as unusable until {@link #any()} has been checked. That check is what makes the
 * nulls safe, and it is the reason accumulation stops before the domain call rather than trying to
 * proceed with holes in the input.
 */
public final class Violations {

    private final List<Violation> violations = new ArrayList<>();

    private Violations() {
    }

    public static Violations create() {

        return new Violations();
    }

    /**
     * Returns the value {@code result} carries, recording a violation against {@code dtoField} and
     * returning {@code null} if it failed instead.
     *
     * <p>{@code dtoField} is the outermost segment of the reported field name; any location the
     * result already carries is nested under it, so a failure inside a nested request object reads
     * as {@code assertion_consumer_service.location}.
     */
    public <T> T take(Result<T> result, String dtoField) {

        if (result.isSuccess()) {

            return result.getValue();
        }

        record(result.getError(), result.getPath().prepend(dtoField));
        return null;
    }

    /**
     * Returns the value {@code result} carries, recording a violation against whichever request
     * field the failure names and returning {@code null} if it failed instead. Use this where the
     * mapper cannot name the field itself — a failure raised inside a domain builder or aggregate.
     */
    public <T> T take(Result<T> result) {

        if (result.isSuccess()) {

            return result.getValue();
        }

        record(result.getError(), result.getPath());
        return null;
    }

    /**
     * Records a failure against the request field it names. A field-scoped error with no resolvable
     * field, and any error describing the request as a whole, is recorded against the request root.
     */
    public void record(DomainError error, FieldPath path) {

        String field = fieldOf(error, path);

        violations.add(new Violation(field, ErrorCodes.codeFor(error), ErrorCodes.messageFor(error, field)));
    }

    /**
     * Combines the location callers supplied with the field the error itself names. The supplied
     * location is the outer part; a field the error names nests inside it, so a domain value that
     * failed inside a collection element reads as {@code attributes[2].display_name}. The two agree
     * when a caller labelled the value it built directly, and then only one segment is emitted.
     */
    private static String fieldOf(DomainError error, FieldPath path) {

        String named = DtoFieldNames.resolve(error);

        if (path.isEmpty()) {

            return named;
        }

        if (named.isEmpty() || path.getLeaf().equals(named)) {

            return path.toString();
        }

        return path + "." + named;
    }

    /**
     * Hands a domain result back to the caller, converting a field-scoped domain failure into a
     * violation on the way. This is the terminal step of a mapper: the domain call may fail for
     * reasons that are not about any single field — a forbidden status transition, a nature
     * restriction — and those pass through unchanged for the transport layer to render as-is.
     */
    public <T> Result<T> completeWith(Result<T> domainResult) {

        return domainResult.isSuccess() ? domainResult : rejected(domainResult);
    }

    /**
     * Re-reports an already-failed domain result as the caller's own failure, re-typed. A
     * field-scoped error becomes a violation against the request field it names; anything else
     * passes through unchanged, path included.
     */
    public <T> Result<T> rejected(Result<?> failed) {

        DomainError error = failed.getError();

        if (!ErrorCodes.isFieldScoped(error)) {

            return failed.propagate();
        }

        record(error, failed.getPath());
        return asFailure();
    }

    public boolean any() {

        return !violations.isEmpty();
    }

    public boolean isEmpty() {

        return violations.isEmpty();
    }

    public List<Violation> asList() {

        return Collections.unmodifiableList(new ArrayList<>(violations));
    }

    /**
     * The collected violations as a failed {@link Result}, typed for the caller's return.
     */
    public <T> Result<T> asFailure() {

        return Result.failure(RequestValidationFailed.with(asList()));
    }
}
