package io.jans.shibboleth.trust.shared;

/**
 * A required field/value was absent (null, blank, or otherwise missing). Neutral
 * across contexts — the merger of the two former per-context "required value"
 * errors — so it lives in the shared kernel.
 */
public class RequiredValueMissing extends DomainError {

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
