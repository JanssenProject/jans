package io.jans.shibboleth.model.error;

import java.util.Objects;

public class DomainObjectUpdateFailed extends TrustError {
    
    private final Class<?> targetClass;
    private final TrustError cause;

    private DomainObjectUpdateFailed(Class<?> targetClass, String message, TrustError cause) {

        super("");
        this.targetClass = Objects.requireNonNull(targetClass);
        this.cause = cause;
        this.message = message != null ? message : "Failed to update " + targetClass.getSimpleName();
        this.message = cause != null ? this.message + " : " + cause.getMessage() : this.message;
    }

    public TrustError getCause() {

        return cause;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + targetClass.getSimpleName() + " ]: " + message ;
    }

    public static DomainObjectUpdateFailed forClass(Class<?> targetClass) {

        return new DomainObjectUpdateFailed(targetClass, null, null);
    }

    public static DomainObjectUpdateFailed forClassWithCause(Class<?> targetClass,TrustError cause) {

        return new DomainObjectUpdateFailed(targetClass, null, cause);
    }

    public static DomainObjectUpdateFailed withMessage(Class<?> targetClass,String message) {

        return new DomainObjectUpdateFailed(targetClass, message,null);
    }

    public static DomainObjectUpdateFailed withMessageAndCause(Class<?> targetClass, String message, TrustError cause) {

        return new DomainObjectUpdateFailed(targetClass, message, cause);
    }
}