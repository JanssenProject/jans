package io.jans.shibboleth.trust.config.error;

import io.jans.shibboleth.trust.config.TrustStatus;

public class InvalidStatusForOperation extends TrustError {
    
    private final TrustStatus currentStatus;
    private final String operation;
    
    private InvalidStatusForOperation(TrustStatus currentStatus, String operation) {

        super(String.format("Cannot perform '%s' operation. Current status is %s",operation,currentStatus));
        this.currentStatus = currentStatus;
        this.operation = operation;
    }

    public TrustStatus getCurrentStatus() {

        return currentStatus;
    }

    public String getOperation() {

        return operation;
    }

    public InvalidStatusForOperation whenParentIdUnassigned(TrustStatus currentStatus, String operation) {

        return new InvalidStatusForOperation(currentStatus, operation);
    }
}
