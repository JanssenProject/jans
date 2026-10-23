package io.jans.shibboleth.trust.config.error;

public class TrustTransitionError extends TrustError  {
    
    private TrustTransitionError(String message) {
        super(message);
    } 

    public static TrustError contextRequired() {

        return new TrustTransitionError("TrustRelationship transition failed. StateTransitionContext is required");
    }

    public static TrustError candidateRequired() {

        return new TrustTransitionError("TrustRelationship transition failed. Candidate in context is required");
    }
    
    public static TrustError rulesRequired() {

        return new TrustTransitionError("TrustRelationship transition failed. Rules are required");
    }
}
