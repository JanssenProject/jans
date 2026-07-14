package io.jans.shibboleth.trust.config;


import io.jans.shibboleth.trust.config.error.*;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import java.util.Objects;

public class DisplayName {

    private final String value;

    private DisplayName(String value) {

        this.value = value;
    }

    public static Result<DisplayName> of(String rawValue) {

        if( rawValue == null || rawValue.trim().isEmpty() ) {

            return Result.failure(RequiredValueMissing.forField("rawValue"));
        }

        return Result.success(new DisplayName(rawValue.trim()));
    }

    public String getValue() {

        return value;
    }

    @Override
    public boolean equals(Object o) {

        if ( this == null ) return true;

        if ( o == null || getClass() != o.getClass() ) return false;
        
        DisplayName that = (DisplayName) o;
        return Objects.equals(value, that.value);
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