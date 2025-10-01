package io.jans.plugin.demo.rest;

import com.spl.plugin.demo.util.Constants;
import com.spl.plugin.demo.util.Utils;
import io.jans.as.common.model.registration.Client;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

@Path("demo")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DemoResource {    
    
    @Inject
    Logger log;
    
    @Inject
    Utils utils;
        
    @GET
	@Path(Constants.ISSUER)
    public Response getIssuer() {
        log.debug(" DemoResource::getIssuer() - utils = "+utils+"\n\n");
        String issuer = utils.getIssuer();
        log.debug(" DemoResource::getIssuer() - issuer = "+issuer+"\n\n");
        return Response.ok(issuer).build();
    }
    
    @GET
    @Path(Constants.CLIENT + Constants.INUM_PATH)
    public Response getClient(@PathParam(Constants.INUM) @NotNull String inum) {
        log.debug("\n DemoResource::getClient() - log = "+log+" ,inum = "+inum+"\n\n");
        Client client = utils.getClient(inum);
        log.debug(" DemoResource::getClient() - client = "+client+"\n\n");
        return Response.ok(client).build();
    }       

}
