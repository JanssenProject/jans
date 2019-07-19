package org.gluu.oxd.server.introspection;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Back compatibility with AS 3.1.1 and earlier.
 *
 * @author yuriyz
 */
public interface BackCompatibleIntrospectionService {

    /**
     * Returns introspection response for specified token.
     *
     * @param p_authorization authorization token
     * @param p_token         token to introspect
     * @return introspection response
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    BackCompatibleIntrospectionResponse introspectToken(@HeaderParam("Authorization") String p_authorization, @FormParam("token") String p_token);
}
