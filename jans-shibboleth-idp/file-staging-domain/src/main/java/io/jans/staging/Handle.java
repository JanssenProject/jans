package io.jans.staging;

import java.util.Objects;

/**
 * Durable, caller-owned location of a claimed file in the shared document store (its resolved path).
 * A staged-but-unclaimed file has {@link #none()}; a claimed file carries the path produced by
 * {@link Destination#resolve(Token)}.
 */
public final class Handle {

    private static final Handle NONE = new Handle("");

    private final String value;

    private Handle(String value) {

        this.value = value;
    }

    public static Handle of(String value) {

        return new Handle(value == null ? "" : value);
    }

    public static Handle none() {

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
        if (!(o instanceof Handle)) {

            return false;
        }
        return value.equals(((Handle) o).value);
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
