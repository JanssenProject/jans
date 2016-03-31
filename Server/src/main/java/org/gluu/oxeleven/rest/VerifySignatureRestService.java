package org.gluu.oxeleven.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.gluu.oxeleven.model.VerifySignatureRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version March 31, 2016
 */
@Path("/oxeleven")
public interface VerifySignatureRestService {

    @POST
    @Path("/verifySignature")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.APPLICATION_JSON})
    Response verifySignature(
            @FormParam(SIGNING_INPUT) String signingInput,
            @FormParam(SIGNATURE) String signature,
            @FormParam(ALIAS) String alias,
            @FormParam(SIGNATURE_ALGORITHM) String signatureAlgorithm);
}
