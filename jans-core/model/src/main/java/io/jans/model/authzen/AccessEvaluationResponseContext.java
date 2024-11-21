package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;

/**
 * @author Yuriy Z
 */
public class AccessEvaluationResponseContext implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("reason_admin")
    private JsonNode reasonAdmin;

    @JsonProperty("reason_user")
    private JsonNode reasonUser;

    public AccessEvaluationResponseContext() {
    }

    public AccessEvaluationResponseContext(String id, JsonNode reasonAdmin, JsonNode reasonUser) {
        this.id = id;
        this.reasonAdmin = reasonAdmin;
        this.reasonUser = reasonUser;
    }

    public String getId() {
        return id;
    }

    public AccessEvaluationResponseContext setId(String id) {
        this.id = id;
        return this;
    }

    public JsonNode getReasonAdmin() {
        return reasonAdmin;
    }

    public AccessEvaluationResponseContext setReasonAdmin(JsonNode reasonAdmin) {
        this.reasonAdmin = reasonAdmin;
        return this;
    }

    public JsonNode getReasonUser() {
        return reasonUser;
    }

    public AccessEvaluationResponseContext setReasonUser(JsonNode reasonUser) {
        this.reasonUser = reasonUser;
        return this;
    }

    @Override
    public String toString() {
        return "AccessEvaluationResponseContext{" +
                "id='" + id + '\'' +
                ", reasonAdmin=" + reasonAdmin +
                ", reasonUser=" + reasonUser +
                '}';
    }
}
