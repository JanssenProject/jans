package io.jans.shibboleth.model.error;


public class TrustRelationshipUpdateFailed extends TrustError {
    
    private final TrustError cause;

    private TrustRelationshipUpdateFailed(TrustError cause) {

        super("");
        this.cause = cause;

        if (cause == null) {
            this.message = "Failed to update TrustRelationship";
        }else {
            this.message = "Failed to update TrustRelationship. " + cause.getMessage();
        }
    }

    public TrustError getCause() {

        return cause;
    }

    public static TrustRelationshipUpdateFailed because(TrustError error) {

        return new TrustRelationshipUpdateFailed(error);
    }
}
