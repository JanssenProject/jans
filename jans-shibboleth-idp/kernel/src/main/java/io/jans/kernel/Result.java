package io.jans.kernel;

/**
 * Outcome of a domain operation: a success carrying a value of type {@code T}, or a failure
 * carrying a {@link DomainError}. A single type serves every bounded context because every
 * context error extends {@code DomainError}.
 *
 * <p>A failure also carries a {@link FieldPath} saying <em>where</em> it happened. The error and the
 * path are deliberately separate: the error names the rule that failed and stays stable, while the
 * path is accumulated by callers as the failure travels outward, since only a caller knows the
 * location of the value it composed. {@link #at(String)} is how a caller contributes its segment,
 * and {@link #propagate()} carries error and path together into the caller's own result type.
 */
public final class Result<T> {

    private final T value;
    private final DomainError error;
    private final FieldPath path;
    private final boolean success;

    private Result(T value, DomainError error, FieldPath path, boolean success) {

        this.value = value;
        this.error = error;
        this.path = path;
        this.success = success;
    }

    public static <T> Result<T> success(T value) {

        return new Result<>(value, null, FieldPath.empty(), true);
    }

    public static <T> Result<T> failure(DomainError error) {

        return new Result<>(null, error, FieldPath.empty(), false);
    }

    public static <T> Result<T> failure(DomainError error, FieldPath path) {

        return new Result<>(null, error, path, false);
    }

    /**
     * Names the location of this value from the composing caller's point of view, prepending
     * {@code segment} to the failure's path. A success is returned unchanged, so the call composes
     * inline: {@code DisplayName.of(raw).at("displayName")}.
     */
    public Result<T> at(String segment) {

        return success ? this : new Result<>(null, error, path.prepend(segment), false);
    }

    /**
     * Names the location of a collection element, so a failure inside a collection stays
     * addressable: {@code attributes[2].displayName}.
     */
    public Result<T> at(String segment, int index) {

        return success ? this : new Result<>(null, error, path.prepend(segment, index), false);
    }

    /**
     * Re-types this failure as the caller's own result type, preserving both error and path. Use
     * this instead of {@code Result.failure(other.getError())}, which silently drops the path.
     */
    public <U> Result<U> propagate() {

        if (success) {

            throw new IllegalStateException("Cannot propagate a successful result as a failure");
        }

        return new Result<>(null, error, path, false);
    }

    public boolean isSuccess() {

        return success;
    }

    public boolean isFailure() {

        return !success;
    }

    public T getValue() {

        if (!success) {

            throw new IllegalStateException("Cannot get value from failed result with error '" + error + "'");
        }
        return value;
    }

    public DomainError getError() {

        if (success) {

            throw new IllegalStateException("Cannot get error from successful result");
        }
        return error;
    }

    /**
     * Where the failure occurred, as contributed by composing callers. Empty when no caller named a
     * location — typically a value object rejecting its own sole input.
     */
    public FieldPath getPath() {

        if (success) {

            throw new IllegalStateException("Cannot get error path from successful result");
        }
        return path;
    }
}
