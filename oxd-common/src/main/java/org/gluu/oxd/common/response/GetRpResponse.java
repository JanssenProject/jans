package org.gluu.oxd.common.response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author yuriyz
 */
public class GetRpResponse implements IOpResponse {

    @JsonProperty(value = "node")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "node")
    private JsonNode node;

    public GetRpResponse() {
    }

    public GetRpResponse(JsonNode node) {
        this.node = node;
    }

    public JsonNode getNode() {
        return node;
    }

    public void setNode(JsonNode node) {
        this.node = node;
    }

    @Override
    public String toString() {
        return "GetRpResponse{" +
                "node='" + node + '\'' +
                '}';
    }
}
