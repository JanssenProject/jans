package io.jans.shibboleth.model.error;


public class EntityIdError extends TrustError {

    private EntityIdError(String message) {

        super(message);
    }

    public static EntityIdError cannotBeNullOrBlank() {

        return new EntityIdError("Entity ID cannot be null or blank");
    }
}