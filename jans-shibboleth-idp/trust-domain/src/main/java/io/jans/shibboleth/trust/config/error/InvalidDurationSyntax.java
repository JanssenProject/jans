package io.jans.shibboleth.trust.config.error;

public class InvalidDurationSyntax extends TrustError {

    private InvalidDurationSyntax(String message) {

        super(message);
    }

    public static InvalidDurationSyntax forValue(String value) {

        return new InvalidDurationSyntax("Invalid duration syntax for value [" + value + "].");
    }
}
