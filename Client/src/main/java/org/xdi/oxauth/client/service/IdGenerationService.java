/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.xdi.oxauth.model.common.Id;

/**
 * Id generation service.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/06/2013
 */
public interface IdGenerationService {

    /**
     * Generates id.
     *
     * @param p_prefix id prefix (e.g. @!1111)
     * @param p_type id type (e.g. people, oclient)
     * @param p_authorization (uma authorization, e.g. Bearer <rpt token>)
     * @return generated id
     */
    @GET
    @Path("/{prefix}/{type}/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Id generateId(@PathParam("prefix") String p_prefix, @PathParam("type") String p_type, @HeaderParam("Authorization") String p_authorization);
}
