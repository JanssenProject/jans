package io.jans.shibboleth.model.error;

import io.jans.shibboleth.model.core.TrustNature;

public class OperationRestrictedToNature extends TrustError  {
    
    private static final String MSG_TEMPLATE = "Operation '%s' is only allowed for %s Trust Relationships. Current nature is %s.";
    private final String operationName;
    private final TrustNature requiredNature;
    private final TrustNature actualNature;

    public OperationRestrictedToNature(String operationName, TrustNature requiredNature, TrustNature actualNature) {

        super(String.format(MSG_TEMPLATE,operationName,requiredNature,actualNature));

        this.operationName = operationName;
        this.requiredNature = requiredNature;
        this.actualNature = actualNature;
    }

    public String getOperationName() {

        return operationName;
    }

    public TrustNature getRequiredNature() {

        return requiredNature;
    }

    public TrustNature getActualNature() {

        return actualNature;
    }
}
