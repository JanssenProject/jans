package io.jans.shibboleth.trust.config;

import io.jans.shibboleth.trust.config.util.TrustResult;
import java.util.Objects;

public class Description {

    private final String value;

    private Description(String value) {

        this.value = value != null ? value.trim() : "";
    }

    public static Description of(String rawValue) {

        return new Description(rawValue);
    }

    public String getValue() {

        return value;
    }

    @Override
    public boolean equals(Object o) {

        if ( this == o ) return true;

        if ( o == null || getClass() != o.getClass() ) return false;
        
        Description that = (Description) o;
        return Objects.equals(value,that.value);
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