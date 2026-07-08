package io.jans.shibboleth.model.error;

import io.jans.shibboleth.model.core.TrustStatus;

public class OperationForbiddenFromStatus extends TrustError {
    
    private static final String MSG_TEMPLATE = "Operation '%s' cannot be performed when TrustRelationship status is %s";

    private final String operationName;
    private final TrustStatus forbiddenStatus;

    private OperationForbiddenFromStatus(String operationName, TrustStatus forbiddenStatus) {

        super(String.format(MSG_TEMPLATE,operationName,forbiddenStatus));
        this.operationName = operationName;
        this.forbiddenStatus = forbiddenStatus;
    }

    public String getOperationName() {

        return operationName;
    }

    public TrustStatus getForbiddenStatus() {

        return forbiddenStatus;
    }

    public static final OperationForbiddenFromStatus of(String operationName,TrustStatus forbiddenStatus) {

        return new OperationForbiddenFromStatus(operationName, forbiddenStatus);
    }
}
