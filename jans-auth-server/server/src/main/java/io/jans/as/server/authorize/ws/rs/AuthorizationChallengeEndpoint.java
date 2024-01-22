package io.jans.as.server.authorize.ws.rs;

import io.jans.as.model.util.QueryStringDecoder;
import io.jans.as.server.service.RequestParameterService;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The authorization challenge endpoint is a new endpoint defined by "OAuth 2.0 for First-Party Native Applications"
 * specification which the native application uses to obtain an authorization code.
 * The endpoint accepts the authorization request parameters defined in [RFC6749] for the authorization endpoint
 * as well as all applicable extensions defined for the authorization endpoint. Some examples of such extensions
 * include Proof Key for Code Exchange (PKCE) [RFC7636], Resource Indicators [RFC8707], and OpenID Connect [OpenID].
 * It is important to note that some extension parameters have meaning in a web context but don't have meaning in
 * a native mechanism (e.g. response_mode=query).
 *
 * @author Yuriy Z
 */
@Path("/authorization_challenge")
public class AuthorizationChallengeEndpoint {

    @Inject
    private RequestParameterService requestParameterService;

    @Inject
    private AuthorizationChallengeService authorizationChallengeService;

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public Response requestAuthorizationPost(
            @FormParam("client_id") String clientId,
            @FormParam("scope") String scope,
            @FormParam("acr_values") String acrValues,
            @FormParam("device_session") String deviceSession,
            @FormParam("use_device_session") String useDeviceSession,
            @FormParam("prompt") String prompt,
            @FormParam("state") String state,
            @FormParam("nonce") String nonce,
            @FormParam("code_challenge") String codeChallenge,
            @FormParam("code_challenge_method") String codeChallengeMethod,
            @FormParam("authorization_details") String authorizationDetails,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse) {

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setHttpMethod(HttpMethod.POST);
        authzRequest.setClientId(clientId);
        authzRequest.setScope(scope);
        authzRequest.setAcrValues(acrValues);
        authzRequest.setDeviceSession(deviceSession);
        authzRequest.setUseDeviceSession(Boolean.parseBoolean(useDeviceSession));
        authzRequest.setState(state);
        authzRequest.setNonce(nonce);
        authzRequest.setPrompt(prompt);
        authzRequest.setCustomParameters(requestParameterService.getCustomParameters(QueryStringDecoder.decode(httpRequest.getQueryString())));
        authzRequest.setHttpRequest(httpRequest);
        authzRequest.setHttpResponse(httpResponse);
        authzRequest.setCodeChallenge(codeChallenge);
        authzRequest.setCodeChallengeMethod(codeChallengeMethod);
        authzRequest.setAuthzDetailsString(authorizationDetails);

        return authorizationChallengeService.requestAuthorization(authzRequest);
    }
}
