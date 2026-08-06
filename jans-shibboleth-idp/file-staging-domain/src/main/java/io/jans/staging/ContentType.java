package io.jans.staging;

import java.util.Objects;

/**
 * The logical media type a file was uploaded as (e.g. {@code application/samlmetadata+xml}). Absence
 * is a first-class value — {@link #none()} — rather than {@code null}/{@code Optional}: an upload need
 * not assert a type.
 */
public final class ContentType {

    private static final ContentType NONE = new ContentType("");

    private final String value;

    private ContentType(String value) {

        this.value = value;
    }

    public static ContentType of(String value) {

        if (value == null || value.isBlank()) {

            return NONE;
        }
        return new ContentType(value.trim());
    }

    public static ContentType none() {

        return NONE;
    }

    public boolean isPresent() {

        return !value.isEmpty();
    }

    public String getValue() {

        return value;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {

            return true;
        }
        if (!(o instanceof ContentType)) {

            return false;
        }
        return value.equals(((ContentType) o).value);
    }

    @Override
    public int hashCode() {

        return Objects.hash(value);
    }

    @Override
    public String toString() {

        return value;
    }
}
