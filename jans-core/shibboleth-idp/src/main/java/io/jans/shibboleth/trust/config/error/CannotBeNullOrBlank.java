package io.jans.shibboleth.trust.config.error;

public class CannotBeNullOrBlank extends TrustError {
    
    private final String fieldName;

    private CannotBeNullOrBlank(String fieldName) {

        super(String.format("Field '%s' cannot be null or blank/empty",fieldName));
        this.fieldName = fieldName;
    }

    public String getFieldName() {

        return fieldName;
    }

    public static CannotBeNullOrBlank forField(String fieldName) {

        return new CannotBeNullOrBlank(fieldName);
    }
}
