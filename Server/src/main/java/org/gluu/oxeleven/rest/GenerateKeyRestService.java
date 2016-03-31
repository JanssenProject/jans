package org.gluu.oxeleven.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.gluu.oxeleven.model.GenerateKeyRequestParam.SIGNATURE_ALGORITHM;

/**
 * @author Javier Rojas Blum
 * @version March 31, 2016
 */
@Path("/oxeleven")
public interface GenerateKeyRestService {

    @POST
    @Path("/generateKey")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.APPLICATION_JSON})
    Response generateKey(
            @FormParam(SIGNATURE_ALGORITHM) String signatureAlgorithm);
}
