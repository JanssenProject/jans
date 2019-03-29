/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.uma;

import org.xdi.oxauth.model.uma.UmaScopeDescription;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * @author Yuriy Zabrovarnyy
 */

public interface UmaScopeService {

    @GET
    @Path("{id}")
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    UmaScopeDescription getScope(@PathParam("id") String id);
}
