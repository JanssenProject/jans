package org.xdi.oxd.license.client;

import org.xdi.oxd.license.client.data.License;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/09/2014
 */

@Path("/rest")
public interface GenerateWS {

    @GET
    @Path("/generate")
    @Produces(MediaType.APPLICATION_JSON)
    License generate();
}
