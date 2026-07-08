package io.jans.shibboleth.model.error;

public class InvalidUriSyntax extends TrustError  {
    
    private InvalidUriSyntax(String message) {
        
        super(message);
    }

    public static InvalidUriSyntax forValue(String value) {

        return new InvalidUriSyntax("Invalid uri syntax for value [" + value + "].");
    }
}
