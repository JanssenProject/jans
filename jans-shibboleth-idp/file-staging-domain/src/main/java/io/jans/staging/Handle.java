package io.jans.staging;

/**
 * An opaque reference to stored content. Absence is a first-class value — {@link #none()} — rather
 * than {@code null}/{@code Optional}.
 */
public record Handle(String value) {

    private static final Handle NONE = new Handle("");

    public Handle {

        value = value == null ? "" : value;
    }

    public static Handle of(String value) {

        return new Handle(value);
    }

    public static Handle none() {

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
