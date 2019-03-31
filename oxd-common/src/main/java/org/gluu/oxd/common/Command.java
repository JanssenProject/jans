/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.gluu.oxd.common.params.IParams;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */
@JsonPropertyOrder({"command", "params"})
public class Command implements Serializable {

//    private static final Logger LOG = LoggerFactory.getLogger(Command.class);

    @JsonProperty(value = "command")
    private CommandType commandType;
    @JsonProperty(value = "params")
    private JsonNode params;

    public Command() {
    }

    public Command(CommandType p_command) {
        commandType = p_command;
    }

    public Command(CommandType commandType, JsonNode params) {
        this.commandType = commandType;
        this.params = params;
    }

    public Command(CommandType commandType, IParams params) {
        this.commandType = commandType;
        this.params = JsonNodeFactory.instance.POJONode(params);
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public Command setCommandType(CommandType p_commandType) {
        commandType = p_commandType;
        return this;
    }

    public JsonNode getParams() {
        return params;
    }

    public Command setParams(JsonNode p_params) {
        params = p_params;
        return this;
    }

    public Command setParamsObject(IParams p_params) {
        params = JsonNodeFactory.instance.POJONode(p_params);
        return this;
    }

    public String paramsAsString() {
        return params != null ? params.toString() : "";
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Command");
        sb.append("{command=").append(commandType);
        sb.append(", params=").append(params);
        sb.append('}');
        return sb.toString();
    }
}
