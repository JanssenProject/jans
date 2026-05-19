package io.jans.shibboleth.model.error;

public class CannotBeNullOrBlank extends TrustError {
    
    private final String fieldName;

    public CannotBeNullOrBlank(String fieldName) {

        super(String.format("Field '%s' cannot be null or blank/empty",fieldName));
        this.fieldName = fieldName;
    }

    public String getFieldName() {

        return fieldName;
    }
}
