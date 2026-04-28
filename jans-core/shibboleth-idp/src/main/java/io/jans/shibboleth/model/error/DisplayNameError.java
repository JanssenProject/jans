package io.jans.shibboleth.model.error;


public class DisplayNameError extends TrustError {


    private DisplayNameError(String message) {
        
        super(message);
    }

    public static DisplayNameError cannotBeNullOrBlank() {

        return new DisplayNameError("Display name cannot be null or blank");
    }

    public static DisplayNameError required() {

        return new DisplayNameError("Display name is required");
    }
}