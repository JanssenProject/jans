package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EvaluationOptions {

    public static final String EXECUTE_ALL = "execute_all";

    public static final String DENY_ON_FIRST_DENY = "deny_on_first_deny";

    public static final String PERMIT_ON_FIRST_PERMIT = "permit_on_first_permit";

    @JsonProperty("evaluations_semantic")
    private String evaluationsSemantic;

    public String getEvaluationsSemantic() {
        return evaluationsSemantic;
    }

    public void setEvaluationsSemantic(String evaluationsSemantic) {
        this.evaluationsSemantic = evaluationsSemantic;
    }

    @Override
    public String toString() {
        return "EvaluationOptions{" +
                "evaluationsSemantic='" + evaluationsSemantic + '\'' +
                '}';
    }
}
