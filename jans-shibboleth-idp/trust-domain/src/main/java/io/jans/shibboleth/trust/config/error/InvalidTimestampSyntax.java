package io.jans.shibboleth.trust.config.error;

public class InvalidTimestampSyntax extends TrustError {

    private InvalidTimestampSyntax(String message) {

        super(message);
    }

    public static InvalidTimestampSyntax forValue(String value) {

        return new InvalidTimestampSyntax("Invalid timestamp syntax for value [" + value + "].");
    }
}
