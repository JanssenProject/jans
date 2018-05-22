package org.xdi.oxd.client;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.ResponseStatus;

import java.io.Serializable;

/**
 * @author yuriyz
 */
@JsonPropertyOrder({"status", "data"})
public class CommandResponse2 implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(org.xdi.oxd.common.CommandResponse.class);

    public static final org.xdi.oxd.common.CommandResponse INTERNAL_ERROR_RESPONSE = org.xdi.oxd.common.CommandResponse.createInternalError();

    public static final org.xdi.oxd.common.CommandResponse OPERATION_IS_NOT_SUPPORTED = org.xdi.oxd.common.CommandResponse.createUnsupportedOperationError();

    @JsonProperty(value = "status")
    private ResponseStatus status;
    @JsonProperty(value = "data")
    private JsonNode data;

    public CommandResponse2() {
    }

    public CommandResponse2(ResponseStatus p_status) {
        status = p_status;
    }

    public CommandResponse2(ResponseStatus p_status, JsonNode p_data) {
        status = p_status;
        data = p_data;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public CommandResponse2 setStatus(ResponseStatus p_status) {
        status = p_status;
        return this;
    }

    public JsonNode getData() {
        return data;
    }

    public CommandResponse2 setData(JsonNode p_data) {
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

    public static CommandResponse2 ok() {
        return new CommandResponse2(ResponseStatus.OK);
    }

    public static CommandResponse2 error() {
        return new CommandResponse2(ResponseStatus.ERROR);
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CommandResponse2");
        sb.append("{status=").append(status);
        sb.append(", params=").append(data);
        sb.append('}');
        return sb.toString();
    }
}