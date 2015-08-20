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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
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

    @POST
    @Path("/command")
    @Produces({MediaType.APPLICATION_JSON})
    public Response execute(@FormParam("request") Command command, @Context HttpServletRequest httpRequest) {
        try {
            // todo log command
            validate(command);
            return Response.ok().entity(CoreUtils.asJson(execute(command))).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Command object is blank").build());
        }
    }

    private void validate(Command command) {
        if (command == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Command object is blank").build());
        }
        // todo

    }

    private CommandResponse execute(Command command) {
        return processor.process(command);
    }
}
