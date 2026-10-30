package io.jans.shibboleth.trust.config.error;

public class InvalidUriSyntax extends TrustError  {
    
    private InvalidUriSyntax(String message) {
        
        super(message);
    }

    public static InvalidUriSyntax forValue(String value) {

        return new InvalidUriSyntax("Invalid uri syntax for value [" + value + "].");
    }
}
