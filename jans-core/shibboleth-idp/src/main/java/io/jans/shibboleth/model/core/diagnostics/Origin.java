package io.jans.shibboleth.model.core.diagnostics;

import java.util.Objects;

import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.util.TrustResult;

public class Origin {

    private final String value;

    private Origin(String value) {

        this.value = value;
    }

    public static Origin of(String value) {

        if (value == null || value.isBlank()) {

            return new Origin("");
        }

        return new Origin(value);
    }

    public String getValue() {

        return value;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Origin origin = (Origin) o;
        
        return Objects.equals(value, origin.value);
    }

    @Override
    public int hashCode() {

        return value.hashCode();
    }

    @Override
    public String toString() {

        return value;
    }
}
