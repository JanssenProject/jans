package io.jans.shibboleth.model.error;

public class TrustRelationshipConsistencyFailure extends TrustError {
    
    private final TrustError cause;

    private TrustRelationshipConsistencyFailure(TrustError cause) {

        super("");
        this.cause = cause;
        if (cause == null) {
            this.message = "TrustRelationship consistency check failed";
        }else {
            this.message = "TrustRelationship consistency check failed. " + cause.getMessage();
        }
    }

    public TrustError getCause() {

        return cause;
    }

    public static final TrustRelationshipConsistencyFailure because(TrustError cause) {

        return new TrustRelationshipConsistencyFailure(cause);
    }
}
