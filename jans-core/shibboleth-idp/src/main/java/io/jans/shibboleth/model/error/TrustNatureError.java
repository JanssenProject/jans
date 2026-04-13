package io.jans.shibboleth.model.error;


public class TrustNatureError extends TrustError {

    private TrustNatureError(String message) {
        
        super(message);
    }

    public static TrustNatureError required() {

        return new TrustNatureError("Trust nature is required");
    }
}