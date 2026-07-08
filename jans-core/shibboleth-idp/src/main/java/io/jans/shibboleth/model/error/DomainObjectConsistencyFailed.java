package io.jans.shibboleth.model.error;

import java.util.Objects;

public class DomainObjectConsistencyFailed extends TrustError {
    
    private final Class<?> targetClass;
    private final TrustError cause;

    private DomainObjectConsistencyFailed(Class<?> targetClass, String message, TrustError cause) {

        super("");
        this.targetClass = Objects.requireNonNull(targetClass);
        this.cause = cause;
        this.message = message != null ? message : "Failed to create " + targetClass.getSimpleName();
        this.message = cause != null ? this.message + " " + cause.getMessage() : this.message;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + targetClass.getSimpleName() + " ]: " + message ;
    }

    public static DomainObjectConsistencyFailed forClass(Class<?> targetClass) {

        return new DomainObjectConsistencyFailed(targetClass, null, null);
    }

    public static DomainObjectConsistencyFailed forClassWithCause(Class<?> targetClass,TrustError cause) {

        return new DomainObjectConsistencyFailed(targetClass, null, cause);
    }

    public static DomainObjectConsistencyFailed withMessage(Class<?> targetClass,String message) {

        return new DomainObjectConsistencyFailed(targetClass, message,null);
    }

    public static DomainObjectConsistencyFailed withMessageAndCause(Class<?> targetClass, String message, TrustError cause) {

        return new DomainObjectConsistencyFailed(targetClass, message, cause);
    }
}