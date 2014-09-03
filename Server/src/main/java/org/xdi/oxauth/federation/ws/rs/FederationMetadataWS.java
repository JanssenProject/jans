package org.xdi.oxauth.federation.ws.rs;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.wordnik.swagger.annotations.Api;

/**
 * Provides interface for Federation Metadata REST web services.
 * <p/>
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/09/2012
 */
@Path("/oxauth")
@Api(value="/oxauth", description = "Federation Metadata Endpoint provided metadata information about federation.")
public interface FederationMetadataWS {

    @GET
    @Path("/federationmetadata")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestMetadata(
            @QueryParam("federation_id") String federationId,
            @QueryParam("signed") String signed,
            @Context HttpServletRequest request,
            @Context SecurityContext sec);
}
