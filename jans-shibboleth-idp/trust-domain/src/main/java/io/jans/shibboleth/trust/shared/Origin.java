package io.jans.shibboleth.trust.shared;

/**
 * Where an activation report or worker claim came from. Absent and blank normalise to empty, so
 * every {@code Origin} is a total value with no failure case.
 */
public record Origin(String value) {

    public Origin {

        value = value == null || value.isBlank() ? "" : value;
    }

    public static Origin of(String value) {

        return new Origin(value);
    }

    @Override
    public String toString() {

        return value;
    }
}
