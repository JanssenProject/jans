package org.xdi.oxauth.federation.ws.rs;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.wordnik.swagger.annotations.Api;

/**
 * Provides interface for Federation Data REST web services.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 08/10/2012
 */

@Path("/oxauth")
@Api(value = "/oxauth", description = "Federation Endpoint provides ability to send JOIN (to federation) requests.")

public interface FederationDataWS {
    @POST
    @Path("/federation")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestJoin(
            @FormParam("federation_id") String federationId,
            @FormParam("entity_type") String entityType,
            @FormParam("display_name") String displayName,
            @FormParam("op_id") String opId,
            @FormParam("domain") String domain,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("x509_url") String x509url,
            @FormParam("x509_pem") String x509pem,
            @Context HttpServletRequest request,
            @Context SecurityContext sec);
}
