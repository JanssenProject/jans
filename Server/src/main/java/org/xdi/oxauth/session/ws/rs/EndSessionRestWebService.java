package org.xdi.oxauth.session.ws.rs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.xdi.oxauth.model.session.EndSessionRequestParam;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * @author Javier Rojas Date: 12.15.2011
 */
@Path("/oxauth")
@Api(value = "/oxauth", description = "End Session Endpoint - URL at the OP to which an RP can perform a redirect to request that the End-User be logged out at the OP")
public interface EndSessionRestWebService {

    @GET
    @Path("/end_session")
    @Produces({MediaType.TEXT_PLAIN})
    @ApiOperation(
            value = "End current Connect session.",
            notes = "End current Connect session.",
            response = Response.class,
            responseContainer = "JSON"
    )
    Response requestEndSession(
            @QueryParam(EndSessionRequestParam.ID_TOKEN_HINT)
            @ApiParam(value = "Previously issued ID Token passed to the logout endpoint as a hint about the End-User's current authenticated session with the Client. This is used as an indication of the identity of the End-User that the RP is requesting be logged out by the OP. The OP need not be listed as an audience of the ID Token when it is used as an id_token_hint value.", required = true)
            String idTokenHint,
            @QueryParam(EndSessionRequestParam.POST_LOGOUT_REDIRECT_URI)
            @ApiParam(value = "URL to which the RP is requesting that the End-User's User Agent be redirected after a logout has been performed. The value MUST have been previously registered with the OP, either using the post_logout_redirect_uris Registration parameter or via another mechanism. If supplied, the OP SHOULD honor this request following the logout.", required = false)
            String postLogoutRedirectUri,
            @QueryParam("session_id")
            @ApiParam(value = "Session ID", required = false)
            String sessionId,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse,
            @Context SecurityContext securityContext);
}