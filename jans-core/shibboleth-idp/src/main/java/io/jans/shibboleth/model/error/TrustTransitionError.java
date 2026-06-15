package io.jans.shibboleth.model.error;

public class TrustTransitionError extends TrustError  {
    
    private TrustTransitionError(String message) {
        super(message);
    } 

    public static TrustError candidateRequired() {

        return new TrustTransitionError("TrustRelationship transition failed. TrustRelationship candidate is required");
    }

    public static TrustError rulesRequired() {

        return new TrustTransitionError("TrustRelationship transition failed. Rules are required");
    }
}
