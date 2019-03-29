/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.introspection.ws.rs;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.gluu.oxauth.model.common.*;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.uma.UmaScopeType;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxauth.service.AttributeService;
import org.gluu.oxauth.service.ClientService;
import org.gluu.oxauth.service.external.ExternalIntrospectionService;
import org.gluu.oxauth.service.external.context.ExternalIntrospectionContext;
import org.gluu.oxauth.service.token.TokenService;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.util.Pair;
import org.slf4j.Logger;
import org.gluu.oxauth.model.common.User;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

/**
 * @author Yuriy Zabrovarnyy
 * @version June 30, 2018
 */
@Path("/introspection")
@Api(value = "/introspection", description = "The Introspection Endpoint is an OAuth 2 Endpoint that responds to " +
        "   HTTP GET and HTTP POST requests from token holders.  The endpoint " +
        "   takes a single parameter representing the token (and optionally " +
        "   further authentication) and returns a JSON document representing the meta information surrounding the token.")
public class IntrospectionWebService {

    private static final Pair<AuthorizationGrant, Boolean> EMPTY = new Pair<AuthorizationGrant, Boolean>(null, false);

    @Inject
    private Logger log;
    @Inject
    private AppConfiguration appConfiguration;
    @Inject
    private TokenService tokenService;
    @Inject
    private ErrorResponseFactory errorResponseFactory;
    @Inject
    private AuthorizationGrantList authorizationGrantList;
    @Inject
    private ClientService clientService;
    @Inject
    private ExternalIntrospectionService externalIntrospectionService;
    @Inject
    private AttributeService attributeService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "invalid_request\n" +
                    "The request is missing a required parameter, includes an unsupported parameter or parameter value, repeats the same parameter or is otherwise malformed.  The resource server SHOULD respond with the HTTP 400 (Bad Request) status code."),
            @ApiResponse(code = 500, message = "Introspection Internal Server Failed.")
    })
    public Response introspectGet(@HeaderParam("Authorization") String p_authorization,
                                  @QueryParam("token") String p_token,
                                  @QueryParam("token_type_hint") String tokenTypeHint,
                                  @Context HttpServletRequest httpRequest,
                                  @Context HttpServletResponse httpResponse
    ) {
        return introspect(p_authorization, p_token, tokenTypeHint, httpRequest, httpResponse);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectPost(@HeaderParam("Authorization") String p_authorization,
                                   @FormParam("token") String p_token,
                                   @FormParam("token_type_hint") String tokenTypeHint,
                                   @Context HttpServletRequest httpRequest,
                                   @Context HttpServletResponse httpResponse) {
        return introspect(p_authorization, p_token, tokenTypeHint, httpRequest, httpResponse);
    }

    private Response introspect(String p_authorization, String p_token, String tokenTypeHint, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            log.trace("Introspect token, authorization: {}, token to introsppect: {}, tokenTypeHint:", p_authorization, p_token, tokenTypeHint);
            if (StringUtils.isNotBlank(p_authorization) && StringUtils.isNotBlank(p_token)) {
                final Pair<AuthorizationGrant, Boolean> pair = getAuthorizationGrant(p_authorization, p_token);
                final AuthorizationGrant authorizationGrant = pair.getFirst();
                if (authorizationGrant != null) {
                    final AbstractToken authorizationAccessToken = authorizationGrant.getAccessToken(tokenService.getTokenFromAuthorizationParameter(p_authorization));

                    if ((authorizationAccessToken != null && authorizationAccessToken.isValid()) || pair.getSecond()) {
                        if (ServerUtil.isTrue(appConfiguration.getIntrospectionAccessTokenMustHaveUmaProtectionScope())) { // #562 - make uma_protection optional
                            if (!authorizationGrant.getScopesAsString().contains(UmaScopeType.PROTECTION.getValue())) {
                                log.trace("access_token used to access introspection endpoint does not have uma_protection scope, however in oxauth configuration `checkUmaProtectionScopePresenceDuringIntrospection` is true");
                                return Response.status(Response.Status.UNAUTHORIZED).entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED) + " access_token does not have uma_protection scope which is required by OP configuration.").build();
                            }
                        }
                        final IntrospectionResponse response = new IntrospectionResponse(false);

                        final AuthorizationGrant grantOfIntrospectionToken = authorizationGrantList.getAuthorizationGrantByAccessToken(p_token);
                        AbstractToken tokenToIntrospect = null;
                        if (grantOfIntrospectionToken != null) {
                            tokenToIntrospect = grantOfIntrospectionToken.getAccessToken(p_token);
                            final User user = grantOfIntrospectionToken.getUser();

                            response.setActive(tokenToIntrospect.isValid());
                            response.setExpiresAt(ServerUtil.dateToSeconds(tokenToIntrospect.getExpirationDate()));
                            response.setIssuedAt(ServerUtil.dateToSeconds(tokenToIntrospect.getCreationDate()));
                            response.setAcrValues(grantOfIntrospectionToken.getAcrValues());
                            response.setScope(grantOfIntrospectionToken.getScopes() != null ? grantOfIntrospectionToken.getScopes() : new ArrayList<String>()); // #433
                            response.setClientId(grantOfIntrospectionToken.getClientId());
                            response.setSub(grantOfIntrospectionToken.getSub());
                            response.setUsername(user != null ? user.getAttribute("displayName") : null);
                            response.setIssuer(appConfiguration.getIssuer());
                            response.setAudience(grantOfIntrospectionToken.getClientId());

                            if (tokenToIntrospect instanceof AccessToken) {
                                AccessToken accessToken = (AccessToken) tokenToIntrospect;
                                response.setTokenType(accessToken.getTokenType() != null ? accessToken.getTokenType().getName() : TokenType.BEARER.getName());
                            }
                        } else {
                            log.error("Failed to find grant for access_token: " + p_token + ". Return 200 with active=false.");
                        }
                        JSONObject responseAsJsonObject = createResponseAsJsonObject(response, tokenToIntrospect);

                        ExternalIntrospectionContext context = new ExternalIntrospectionContext(authorizationGrant, httpRequest, httpResponse, appConfiguration, attributeService);
                        if (externalIntrospectionService.executeExternalModifyResponse(responseAsJsonObject, context)) {
                            log.trace("Successfully run extenal introspection scripts.");
                        } else {
                            responseAsJsonObject = createResponseAsJsonObject(response, tokenToIntrospect);
                            log.trace("Canceled changes made by external introspection script since method returned `false`.");
                        }

                        return Response.status(Response.Status.OK).entity(responseAsJsonObject.toString()).build();
                    } else {
                        log.error("Access token is not valid. Valid: " + (authorizationAccessToken != null && authorizationAccessToken.isValid()));
                        return Response.status(Response.Status.UNAUTHORIZED).entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED)).build();
                    }
                } else {
                    log.error("Authorization grant is null.");
                    return Response.status(Response.Status.UNAUTHORIZED).entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED)).build();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST)).build();
    }

    private static JSONObject createResponseAsJsonObject(IntrospectionResponse response, AbstractToken tokenToIntrospect) throws JSONException, IOException {
        final JSONObject result = new JSONObject(ServerUtil.asJson(response));
        if (tokenToIntrospect != null && StringUtils.isNotBlank(tokenToIntrospect.getX5ts256())) {
            final JSONObject cnf = new JSONObject();
            cnf.put("x5t#S256", tokenToIntrospect.getX5ts256());
            result.put("cnf", cnf);
        }

        return result;
    }

    private Pair<AuthorizationGrant, Boolean> getAuthorizationGrant(String authorization, String accessToken) throws UnsupportedEncodingException {
        AuthorizationGrant grant = tokenService.getAuthorizationGrantByPrefix(authorization, "Bearer ");
        if (grant != null) {
            final String authorizationAccessToken = authorization.substring("Bearer ".length());
            final AbstractToken accessTokenObject = grant.getAccessToken(authorizationAccessToken);
            if (accessTokenObject != null && accessTokenObject.isValid()) {
                return new Pair<AuthorizationGrant, Boolean>(grant, false);
            } else {
                log.error("Access token is not valid: " + authorizationAccessToken);
                return EMPTY;
            }
        }

        grant = tokenService.getAuthorizationGrantByPrefix(authorization, "Basic ");
        if (grant != null) {
            return new Pair<AuthorizationGrant, Boolean>(grant, false);
        }
        if (StringUtils.startsWithIgnoreCase(authorization, "Basic ")) {

            String encodedCredentials = authorization.substring("Basic ".length());

            String token = new String(Base64.decodeBase64(encodedCredentials), Util.UTF8_STRING_ENCODING);

            int delim = token.indexOf(":");

            if (delim != -1) {
                String clientId = URLDecoder.decode(token.substring(0, delim), Util.UTF8_STRING_ENCODING);
                String password = URLDecoder.decode(token.substring(delim + 1), Util.UTF8_STRING_ENCODING);
                if (clientService.authenticate(clientId, password)) {
                    grant = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);
                    if (grant != null && !grant.getClientId().equals(clientId)) {
                        log.trace("Failed to match grant object clientId and client id provided during authentication.");
                        return EMPTY;
                    }
                    return new Pair<AuthorizationGrant, Boolean>(grant, true);
                } else {
                    log.trace("Failed to perform basic authentication for client: " + clientId);
                }
            }
        }
        return EMPTY;
    }

}
