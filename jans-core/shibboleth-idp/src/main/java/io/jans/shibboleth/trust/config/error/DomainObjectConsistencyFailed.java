package io.jans.shibboleth.trust.config.error;

import java.util.Objects;

import io.jans.shibboleth.trust.shared.DomainError;

public class DomainObjectConsistencyFailed extends TrustError {
    
    private final Class<?> targetClass;
    private final DomainError cause;

    private DomainObjectConsistencyFailed(Class<?> targetClass, String message, DomainError cause) {

        super("");
        this.targetClass = Objects.requireNonNull(targetClass);
        this.cause = cause;
        this.message = message != null ? message : "Consistency check failed for " + targetClass.getSimpleName() + ". ";
        this.message = cause != null ? this.message + " " + cause.getMessage() : this.message;
    }

    public DomainError getCause() {

        return cause;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + targetClass.getSimpleName() + " ]: " + message ;
    }

    public static DomainObjectConsistencyFailed forClass(Class<?> targetClass) {

        return new DomainObjectConsistencyFailed(targetClass, null, null);
    }

    public static DomainObjectConsistencyFailed forClassWithCause(Class<?> targetClass,DomainError cause) {

        return new DomainObjectConsistencyFailed(targetClass, null, cause);
    }

    public static DomainObjectConsistencyFailed withMessage(Class<?> targetClass,String message) {

        return new DomainObjectConsistencyFailed(targetClass, message,null);
    }

    public static DomainObjectConsistencyFailed withMessageAndCause(Class<?> targetClass, String message, DomainError cause) {

        return new DomainObjectConsistencyFailed(targetClass, message, cause);
    }
}