package io.jans.shibboleth.trust.shared;

/**
 * Outcome of a domain operation: a success carrying a value of type {@code T},
 * or a failure carrying a {@link DomainError}. A single type serves both
 * bounded contexts because every context error extends {@code DomainError}.
 */
public final class Result<T> {

    private final T value;
    private final DomainError error;
    private final boolean success;

    private Result(T value, DomainError error, boolean success) {

        this.value = value;
        this.error = error;
        this.success = success;
    }

    public static <T> Result<T> success(T value) {

        return new Result<>(value, null, true);
    }

    public static <T> Result<T> failure(DomainError error) {

        return new Result<>(null, error, false);
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
}
