/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.web;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CoreUtils;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Created by yuriy on 8/30/2015.
 */
public class CommandService {

    public Command validate(String commandAsJson) {
        if (StringUtils.isBlank(commandAsJson)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Command object is blank").build());
        }
        try {
            Command command = CoreUtils.asCommand(commandAsJson);
            validate(command);
            return command;
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Command object json is invalid. Please check json format or whether it coresponds to oxd server convention.").build());
        }
    }

    public void validate(Command command) {
        if (command == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Command object is blank").build());
        }
        if (command.getCommandType() == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Command type is blank").build());
        }
    }
}
