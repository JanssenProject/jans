package io.jans.shibboleth.trust.activation.error;

public class RequiredValueMissing extends ActivationError {

    private final String fieldName;

    private RequiredValueMissing(String fieldName) {

        super(String.format("Required value '%s' is missing", fieldName));
        this.fieldName = fieldName;
    }

    public String getFieldName() {

        return fieldName;
    }

    public static RequiredValueMissing forField(String fieldName) {

        return new RequiredValueMissing(fieldName);
    }
}
