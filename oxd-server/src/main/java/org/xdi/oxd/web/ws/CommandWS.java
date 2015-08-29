package org.xdi.oxd.web.ws;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.server.GuiceModule;
import org.xdi.oxd.server.Processor;
import org.xdi.oxd.web.CommandService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 20/08/2015
 */

@Path("/rest")
public class CommandWS {

    private static final Logger LOG = LoggerFactory.getLogger(CommandWS.class);

    private final Processor processor;

    public CommandWS() {
        final Injector injector = Guice.createInjector(new GuiceModule());
        processor = new Processor(injector);
    }

    @GET
    @Path("/command")
    @Produces({MediaType.APPLICATION_JSON})
    public Response get(@QueryParam("request") String commandAsJson, @Context HttpServletRequest httpRequest) {
        return execute(commandAsJson);
    }

    @POST
    @Path("/command")
    @Produces({MediaType.APPLICATION_JSON})
    public Response post(@FormParam("request") String commandAsJson, @Context HttpServletRequest httpRequest) {
        return execute(commandAsJson);
    }

    private Response execute(String commandAsJson) {
        try {
            LOG.debug("Request, command: " + commandAsJson);
            Command command = new CommandService().validate(commandAsJson);

            String response = CoreUtils.asJson(execute(command));
            LOG.debug("Response, command: " + command + "\n, response: " + response);
            return Response.ok().entity(response).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Command object is blank").build());
        }
    }

    public CommandResponse execute(Command command) {
        return processor.process(command);
    }
}
