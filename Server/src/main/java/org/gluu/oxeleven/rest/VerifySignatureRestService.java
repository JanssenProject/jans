package org.gluu.oxeleven.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Javier Rojas Blum
 * @version March 30, 2016
 */
@Path("/oxeleven")
public interface VerifySignatureRestService {

    @POST
    @Path("/verifySignature")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.APPLICATION_JSON})
    Response verifySignature(
            @FormParam("signingInput") String signingInput,
            @FormParam("signature") String signature,
            @FormParam("alias") String alias,
            @FormParam("signatureAlgorithm") String signatureAlgorithm);
}
