package io.jans.staging.error;

/**
 * The claim destination is not a well-formed absolute path within the caller's layout (blank,
 * relative, or containing a traversal segment). Maps to HTTP 400.
 */
public final class InvalidDestination extends StagingError {

    private final String value;

    private InvalidDestination(String value) {

        super("Invalid destination: '" + value + "'");
        this.value = value;
    }

    public String getValue() {

        return value;
    }

    public static InvalidDestination forValue(String value) {

        return new InvalidDestination(value);
    }
}
