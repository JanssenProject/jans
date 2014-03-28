package org.xdi.oxauth.client.uma;

import org.xdi.oxauth.model.uma.ScopeDescription;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/04/2013
 */

public interface ScopeService {

    @GET
    @Path("{id}")
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public ScopeDescription getScope(@PathParam("id") String id);
}
