/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.rest;

import org.gluu.oxeleven.model.SignRequestParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.gluu.oxeleven.model.SignRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version April 19, 2016
 */
@Path("/oxeleven")
public interface SignRestService {

    @POST
    @Path("/sign")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    Response sign(SignRequestParam signRequestParam);
}
