package io.jans.shibboleth.model.error;

public class IdNotAssigned extends TrustError {
    
    public IdNotAssigned() {
        
        super("The specified id is not assigned");
    }

    public IdNotAssigned(String message) {

        super(message);
    }
}
