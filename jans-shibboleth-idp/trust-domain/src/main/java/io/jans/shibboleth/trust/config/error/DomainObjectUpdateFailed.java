package io.jans.shibboleth.trust.config.error;

import java.util.Objects;

import io.jans.shibboleth.trust.shared.DomainError;

public class DomainObjectUpdateFailed extends TrustError {
    
    private final Class<?> targetClass;
    private final DomainError cause;

    private DomainObjectUpdateFailed(Class<?> targetClass, String message, DomainError cause) {

        super("");
        this.targetClass = Objects.requireNonNull(targetClass);
        this.cause = cause;
        this.message = message != null ? message : "Failed to update " + targetClass.getSimpleName();
        this.message = cause != null ? this.message + " : " + cause.getMessage() : this.message;
    }

    public DomainError getCause() {

        return cause;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + targetClass.getSimpleName() + " ]: " + message ;
    }

    public static DomainObjectUpdateFailed forClass(Class<?> targetClass) {

        return new DomainObjectUpdateFailed(targetClass, null, null);
    }

    public static DomainObjectUpdateFailed forClassWithCause(Class<?> targetClass,DomainError cause) {

        return new DomainObjectUpdateFailed(targetClass, null, cause);
    }

    public static DomainObjectUpdateFailed withMessage(Class<?> targetClass,String message) {

        return new DomainObjectUpdateFailed(targetClass, message,null);
    }

    public static DomainObjectUpdateFailed withMessageAndCause(Class<?> targetClass, String message, DomainError cause) {

        return new DomainObjectUpdateFailed(targetClass, message, cause);
    }
}