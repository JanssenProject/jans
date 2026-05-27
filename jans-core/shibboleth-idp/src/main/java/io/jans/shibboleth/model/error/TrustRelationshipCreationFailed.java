package io.jans.shibboleth.model.error;

public class TrustRelationshipCreationFailed extends TrustError  {
    
    private final TrustError cause;

    private TrustRelationshipCreationFailed(TrustError cause) {

        super("");
        this.cause = cause;
        if (cause == null) {
            this.message = "Failed to create TrustRelationship";
        }else {
            this.message = "Failed to create TrustRelationship. " + cause.getMessage();
        }
    }

    public TrustError getCause() {

        return cause;
    }

    public static TrustRelationshipCreationFailed because(TrustError cause) {

        return new TrustRelationshipCreationFailed(cause);
    }

}
