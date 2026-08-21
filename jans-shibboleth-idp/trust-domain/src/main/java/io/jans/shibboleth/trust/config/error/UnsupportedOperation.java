package io.jans.shibboleth.trust.config.error;


public class UnsupportedOperation extends TrustError  {
    
    private UnsupportedOperation(String message) {
        
        super(message);
    }

    public static UnsupportedOperation withMessage(String message) {

        return new UnsupportedOperation(message);
    }
}
