package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Z
 */
public class AccessEvaluationResponse {

    public static final AccessEvaluationResponse FALSE = new AccessEvaluationResponse(false, null);

    public static final AccessEvaluationResponse TRUE = new AccessEvaluationResponse(true, null);

    @JsonProperty("decision")
    private boolean decision = false;

    @JsonProperty("context")
    private AccessEvaluationResponseContext context;

    public AccessEvaluationResponse() {
    }

    public AccessEvaluationResponse(boolean decision, AccessEvaluationResponseContext context) {
        this.decision = decision;
        this.context = context;
    }

    public AccessEvaluationResponseContext getContext() {
        return context;
    }

    public AccessEvaluationResponse setContext(AccessEvaluationResponseContext context) {
        this.context = context;
        return this;
    }

    public boolean isDecision() {
        return decision;
    }

    public AccessEvaluationResponse setDecision(boolean decision) {
        this.decision = decision;
        return this;
    }

    @Override
    public String toString() {
        return "AccessEvaluationResponse{" +
                "decision=" + decision +
                ", context=" + context +
                '}';
    }
}
