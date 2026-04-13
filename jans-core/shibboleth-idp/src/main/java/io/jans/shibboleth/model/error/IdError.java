package io.jans.shibboleth.model.error;


public class IdError extends TrustError {


    private IdError(String message) {

        super(message);
    }

    public static IdError notAssigned() {

        return new IdError("Id has not been assigned yet");
    }
    
}