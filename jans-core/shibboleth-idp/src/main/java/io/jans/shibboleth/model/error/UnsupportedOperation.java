package io.jans.shibboleth.model.error;


public class UnsupportedOperation extends TrustError  {
    
    private UnsupportedOperation(String message) {
        
        super(message);
    }

    public static UnsupportedOperation withMessage(String message) {

        return new UnsupportedOperation(message);
    }
}
