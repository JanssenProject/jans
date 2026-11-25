package io.jans.shibboleth.trust.config;

/**
 * A trust relationship's free-text description. Absent and blank are the same thing here — both
 * normalise to empty — so there is no failure case and no {@code Result}.
 */
public record Description(String value) {

    public Description {

        value = value != null ? value.trim() : "";
    }

    public static Description of(String rawValue) {

        return new Description(rawValue);
    }

    @Override
    public String toString() {

        return value;
    }
}
