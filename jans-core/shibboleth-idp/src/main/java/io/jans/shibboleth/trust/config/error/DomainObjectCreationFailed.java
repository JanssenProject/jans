package io.jans.shibboleth.trust.config.error;

import java.util.Objects;

public class DomainObjectCreationFailed extends TrustError {
    
    private final Class<?> targetClass;
    private final TrustError cause;

    private DomainObjectCreationFailed(Class<?> targetClass, String message, TrustError cause) {

        super("");
        this.targetClass = Objects.requireNonNull(targetClass);
        this.cause = cause;
        this.message = message != null ? message : "Failed to create " + targetClass.getSimpleName();
        this.message = cause != null ? this.message + " " + cause.getMessage() : this.message;
    }

    public TrustError getCause() {

        return cause;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + targetClass.getSimpleName() + " ]: " + message ;
    }

    public static DomainObjectCreationFailed forClass(Class<?> targetClass) {

        return new DomainObjectCreationFailed(targetClass, null, null);
    }

    public static DomainObjectCreationFailed forClassWithCause(Class<?> targetClass,TrustError cause) {

        return new DomainObjectCreationFailed(targetClass, null, cause);
    }

    public static DomainObjectCreationFailed withMessage(Class<?> targetClass,String message) {

        return new DomainObjectCreationFailed(targetClass, message,null);
    }

    public static DomainObjectCreationFailed withMessageAndCause(Class<?> targetClass, String message, TrustError cause) {

        return new DomainObjectCreationFailed(targetClass, message, cause);
    }
}