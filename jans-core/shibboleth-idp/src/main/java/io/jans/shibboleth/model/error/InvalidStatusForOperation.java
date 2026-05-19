package io.jans.shibboleth.model.error;

import io.jans.shibboleth.model.core.TrustStatus;

public class InvalidStatusForOperation extends TrustError {
    
    private final TrustStatus currentStatus;
    private final String operation;
    
    public InvalidStatusForOperation(TrustStatus currentStatus, String operation) {

        super(String.format("Cannot perform '%s' operation. Current status is %s",operation,currentStatus));
        this.currentStatus = currentStatus;
        this.operation = operation;
    }

    public TrustStatus getCurrentStatus() {

        return currentStatus;
    }

}
