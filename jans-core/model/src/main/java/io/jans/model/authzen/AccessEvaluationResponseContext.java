package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Yuriy Z
 */
public class AccessEvaluationResponseContext implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("code")
    private String code;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("error")
    private JsonNode error;

    @JsonProperty("reason_admin")
    private JsonNode reasonAdmin;

    @JsonProperty("reason_user")
    private JsonNode reasonUser;

    @JsonProperty("metadata")
    private JsonNode metadata;

    // dynamic fields from JSON will be stored here
    private final Map<String, Object> extra = new LinkedHashMap<>();

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

    public JsonNode getMetadata() {
        return metadata;
    }

    public AccessEvaluationResponseContext setMetadata(JsonNode metadata) {
        this.metadata = metadata;
        return this;
    }


    @JsonAnySetter
    public void putExtra(String key, Object value) {
        extra.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return extra;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public JsonNode getError() {
        return error;
    }

    public void setError(JsonNode error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "AccessEvaluationResponseContext{" +
                "id='" + id + '\'' +
                ", code=" + code +
                ", reason=" + reason +
                ", error=" + error +
                ", reasonAdmin=" + reasonAdmin +
                ", reasonUser=" + reasonUser +
                ", metadata=" + metadata +
                ", extra=" + extra +
                '}';
    }
}
