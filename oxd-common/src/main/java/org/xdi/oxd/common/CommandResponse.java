/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.node.POJONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */
@JsonPropertyOrder({"status", "data"})
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"status", "data"})
public class CommandResponse implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(CommandResponse.class);

    @JsonProperty(value = "status")
    @com.fasterxml.jackson.annotation.JsonProperty(value="status")
    private ResponseStatus status;
    @JsonProperty(value = "data")
    @com.fasterxml.jackson.annotation.JsonProperty(value="data")
    private JsonNode data;

    public CommandResponse() {
    }

    public CommandResponse(ResponseStatus p_status) {
        status = p_status;
    }

    public CommandResponse(ResponseStatus p_status, JsonNode p_data) {
        status = p_status;
        data = p_data;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public CommandResponse setStatus(ResponseStatus p_status) {
        status = p_status;
        return this;
    }

    public JsonNode getData() {
        return data;
    }

    public CommandResponse setData(JsonNode p_data) {
        data = p_data;
        return this;
    }

    public <T> T dataAsResponse(Class<T> p_class) {
        if (data != null && p_class != null) {
            final String asString = data.toString();
            try {
                return CoreUtils.createJsonMapper().readValue(asString, p_class);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            LOG.error("Unable to parse string to response, string: {}", asString);
        }
        return null;
    }

    public static CommandResponse ok() {
        return new CommandResponse(ResponseStatus.OK);
    }

    public static CommandResponse error() {
        return new CommandResponse(ResponseStatus.ERROR);
    }

    public static CommandResponse createErrorResponse(ErrorResponse p_error) {
        return CommandResponse.error().setData(new POJONode(p_error));
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        return "CommandResponse" +
                "{status=" + status +
                ", params=" + data +
                '}';
    }
}
