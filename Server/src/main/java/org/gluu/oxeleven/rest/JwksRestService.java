package org.gluu.oxeleven.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.gluu.oxeleven.model.JwksRequestParam.ALIAS_LIST;

/**
 * @author Javier Rojas Blum
 * @version March 31, 2016
 */
@Path("/oxeleven")
public interface JwksRestService {

    @POST
    @Path("/jwks")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.APPLICATION_JSON})
    Response sign(
            @FormParam(ALIAS_LIST) List<String> aliasList);
}
