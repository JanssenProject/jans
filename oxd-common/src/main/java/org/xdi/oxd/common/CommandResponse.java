/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
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

    public CommandResponse setData(JsonNode p_data) {
        data = p_data;
        return this;
    }

    public static CommandResponse ok() {
        return new CommandResponse(ResponseStatus.OK);
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
