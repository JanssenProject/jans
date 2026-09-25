package io.jans.shibboleth.trust.config.error;

public class InvalidUuidSyntax extends TrustError {

    private InvalidUuidSyntax(String message) {

        super(message);
    }

    public static InvalidUuidSyntax forValue(String value) {

        return new InvalidUuidSyntax("Invalid uuid syntax for value [" + value + "].");
    }
}
