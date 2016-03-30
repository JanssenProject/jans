package org.gluu.oxeleven.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Javier Rojas Blum
 * @version March 30, 2016
 */
@Path("/oxeleven")
public interface SignRestService {

    @POST
    @Path("/sign")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.APPLICATION_JSON})
    Response sign(
            @FormParam("signingInput") String signingInput,
            @FormParam("alias") String alias,
            @FormParam("signatureAlgorithm") String signatureAlgorithm);
}
