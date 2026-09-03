package io.jans.staging;

/**
 * The logical media type a file was uploaded as (e.g. {@code application/samlmetadata+xml}). Absence
 * is a first-class value — {@link #none()} — rather than {@code null}/{@code Optional}: an upload need
 * not assert a type.
 */
public record ContentType(String value) {

    private static final ContentType NONE = new ContentType("");

    public ContentType {

        value = value == null ? "" : value.trim();
    }

    public static ContentType of(String value) {

        if (value == null || value.isBlank()) {

            return NONE;
        }
        return new ContentType(value);
    }

    public static ContentType none() {

        return NONE;
    }

    public boolean isPresent() {

        return !value.isEmpty();
    }

    @Override
    public String toString() {

        return value;
    }
}
