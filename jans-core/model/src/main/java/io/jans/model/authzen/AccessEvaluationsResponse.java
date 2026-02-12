package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * AuthZEN Batch Evaluations Response.
 * Response for POST /access/v1/evaluations endpoint.
 *
 * @author Yuriy Z
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessEvaluationsResponse {

    @JsonProperty("evaluations")
    private List<AccessEvaluationResponse> evaluations;

    public AccessEvaluationsResponse() {
        // empty
    }

    public AccessEvaluationsResponse(List<AccessEvaluationResponse> evaluations) {
        this.evaluations = evaluations;
    }

    public List<AccessEvaluationResponse> getEvaluations() {
        return evaluations;
    }

    public AccessEvaluationsResponse setEvaluations(List<AccessEvaluationResponse> evaluations) {
        this.evaluations = evaluations;
        return this;
    }

    @Override
    public String toString() {
        return "AccessEvaluationsResponse{" +
                "evaluations=" + evaluations +
                '}';
    }
}
