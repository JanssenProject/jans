package io.jans.kernel;

import java.util.Objects;

/**
 * A required value was absent (null, blank, or otherwise missing). Neutral across contexts, so it
 * lives in the shared kernel.
 *
 * <p>The error names the type that required the value, and — only when that type has more than one
 * field — which of its fields. A single-field value object needs no field name because its type
 * <em>is</em> the field: {@code RequiredValueMissing.of(DisplayName.class)}. A type with several
 * fields names the one that was missing: {@code forField(TrustRelationship.class, "displayName")}.
 *
 * <p>Neither form describes where the value sits in a larger structure — a value object cannot know
 * that. Location is carried separately by {@link FieldPath}, contributed by composing callers.
 */
public class RequiredValueMissing extends DomainError {

    private final Class<?> owner;
    private final String fieldName;

    private RequiredValueMissing(Class<?> owner, String fieldName) {

        super(fieldName.isEmpty()
            ? String.format("%s requires a value", owner.getSimpleName())
            : String.format("%s requires a value for '%s'", owner.getSimpleName(), fieldName));

        this.owner = owner;
        this.fieldName = fieldName;
    }

    /**
     * The value required by a single-field type, whose type is itself the field.
     */
    public static RequiredValueMissing of(Class<?> owner) {

        return new RequiredValueMissing(Objects.requireNonNull(owner, "owner"), "");
    }

    /**
     * A named field required by a type that has several.
     */
    public static RequiredValueMissing forField(Class<?> owner, String fieldName) {

        Objects.requireNonNull(owner, "owner");

        if (fieldName == null || fieldName.trim().isEmpty()) {

            throw new IllegalArgumentException(
                "Field name cannot be null or blank for " + owner.getSimpleName() + "; use of(Class) instead");
        }

        return new RequiredValueMissing(owner, fieldName);
    }

    public Class<?> getOwner() {

        return owner;
    }

    /**
     * The missing field's name, or the empty string when the owning type is itself the field.
     */
    public String getFieldName() {

        return fieldName;
    }

    public boolean namesField() {

        return !fieldName.isEmpty();
    }
}
