/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.introspection.ws.rs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.authzdetails.AuthzDetails;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.uma.UmaScopeType;
import io.jans.as.model.util.Util;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AccessToken;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.IntrospectionService;
import io.jans.as.server.service.external.ExternalIntrospectionService;
import io.jans.as.server.service.external.context.ExternalIntrospectionContext;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.util.Pair;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static io.jans.as.model.util.Util.escapeLog;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version September 30, 2021
 */
@Path("/introspection")
public class IntrospectionWebService {

    private static final Pair<AuthorizationGrant, Boolean> EMPTY = new Pair<>(null, false);
    private static final ObjectMapper OBJECT_MAPPER = ServerUtil.createJsonMapper();

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

    @Inject
    private IntrospectionService introspectionService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectGet(@HeaderParam("Authorization") String authorization,
                                  @HeaderParam("Accept") String accept,
                                  @QueryParam("token") String token,
                                  @QueryParam("token_type_hint") String tokenTypeHint,
                                  @QueryParam("response_as_jwt") String responseAsJwt,
                                  @Context HttpServletRequest httpRequest,
                                  @Context HttpServletResponse httpResponse
    ) {
        return introspect(authorization, accept, token, tokenTypeHint, responseAsJwt, httpRequest, httpResponse);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectPost(@HeaderParam("Authorization") String authorization,
                                   @HeaderParam("Accept") String accept,
                                   @FormParam("token") String token,
                                   @FormParam("token_type_hint") String tokenTypeHint,
                                   @FormParam("response_as_jwt") String responseAsJwt,
                                   @Context HttpServletRequest httpRequest,
                                   @Context HttpServletResponse httpResponse) {
        return introspect(authorization, accept, token, tokenTypeHint, responseAsJwt, httpRequest, httpResponse);
    }

    private AuthorizationGrant validateAuthorization(String authorization, String token) throws UnsupportedEncodingException {
        try {
            final boolean skipAuthorization = isTrue(appConfiguration.getIntrospectionSkipAuthorization());
            log.trace("skipAuthorization: {}", skipAuthorization);
            if (skipAuthorization) {
                return null;
            }

            if (StringUtils.isBlank(authorization)) {
                log.trace("Bad request: Authorization header or token is blank.");
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, "")).build());
            }

            final Pair<AuthorizationGrant, Boolean> pair = getAuthorizationGrant(authorization, token);
            final AuthorizationGrant authorizationGrant = pair.getFirst();
            if (authorizationGrant == null) {
                log.debug("Authorization grant is null.");
                if (isTrue(pair.getSecond())) {
                    log.debug("Returned {\"active\":false}.");
                    throw new WebApplicationException(Response.status(Response.Status.OK)
                            .entity("{\"active\":false}")
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .build());
                }
                throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(errorResponseFactory.errorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED, "Authorization grant is null."))
                        .build());
            }

            final AbstractToken authorizationAccessToken = authorizationGrant.getAccessToken(tokenService.getToken(authorization));

            if ((authorizationAccessToken == null || !authorizationAccessToken.isValid()) && BooleanUtils.isFalse(pair.getSecond())) {
                log.error("Access token is not valid. Valid: {}, basicClientAuthentication: {}", (authorizationAccessToken != null && authorizationAccessToken.isValid()), pair.getSecond());
                throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED, "Access token is not valid")).build());
            }

            if (isTrue(appConfiguration.getIntrospectionAccessTokenMustHaveUmaProtectionScope()) &&
                    !authorizationGrant.getScopesAsString().contains(UmaScopeType.PROTECTION.getValue())) { // #562 - make uma_protection optional
                final String reason = "access_token used to access introspection endpoint does not have uma_protection scope, however in AS configuration `checkUmaProtectionScopePresenceDuringIntrospection` is true";
                log.trace(reason);
                throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity(errorResponseFactory.errorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED, reason)).type(MediaType.APPLICATION_JSON_TYPE).build());
            }
            introspectionService.validateIntrospectionScopePresence(authorizationGrant);
            return authorizationGrant;
        } catch (EntryPersistenceException e) {
            log.trace("Failed to find entry.", e);
            throw new WebApplicationException(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(errorResponseFactory.errorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED, "Authorization is not valid"))
                    .build());
        }
    }

    private Response introspect(String authorization, String accept,  String token, String tokenTypeHint, String responseAsJwt, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("Introspect token, authorization: {}, token to introspect: {}, tokenTypeHint: {}, accept: {}", escapeLog(authorization), escapeLog(token), escapeLog(tokenTypeHint), escapeLog(accept));
            }

            AuthorizationGrant authorizationGrant = validateAuthorization(authorization, token);

            if (StringUtils.isBlank(token)) {
                log.trace("Bad request: Token is blank.");
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, "")).build();
            }

            final IntrospectionResponse response = new IntrospectionResponse(false);

            final AuthorizationGrant grantOfIntrospectionToken = authorizationGrantList.getAuthorizationGrantByAccessToken(token);

            fillResponse(token, response, grantOfIntrospectionToken);
            JSONObject responseAsJsonObject = createResponseAsJsonObject(response, grantOfIntrospectionToken);

            ExternalIntrospectionContext context = new ExternalIntrospectionContext(authorizationGrant, httpRequest, httpResponse, appConfiguration, attributeService);
            context.setGrantOfIntrospectionToken(grantOfIntrospectionToken);
            if (externalIntrospectionService.executeExternalModifyResponse(responseAsJsonObject, context)) {
                log.trace("Successfully run external introspection scripts.");
            } else {
                responseAsJsonObject = createResponseAsJsonObject(response, grantOfIntrospectionToken);
                log.trace("Canceled changes made by external introspection script since method returned `false`.");
            }

            // Make scopes conform as required by spec, see #1499
            if (response.getScope() != null && !appConfiguration.getIntrospectionResponseScopesBackwardCompatibility()) {
                String scopes = StringUtils.join(response.getScope().toArray(), " ");
                responseAsJsonObject.put("scope", scopes);
            }

            // Response as JWT
            if (introspectionService.isJwtResponse(responseAsJwt, accept)) {
                String responseAsJwtEntity = introspectionService.createResponseAsJwt(responseAsJsonObject, grantOfIntrospectionToken);
                if (log.isTraceEnabled()) {
                    log.trace("Response jwt entity: {}", responseAsJwtEntity);
                }
                return Response.status(Response.Status.OK)
                        .entity(responseAsJwtEntity)
                        .type(Constants.APPLICATION_TOKEN_INTROSPECTION_JWT)
                        .build();
            }

            final String entity = responseAsJsonObject.toString();
            if (log.isTraceEnabled()) {
                log.trace("Response entity: {}", entity);
            }
            return Response.status(Response.Status.OK).entity(entity).type(MediaType.APPLICATION_JSON_TYPE).build();

        } catch (WebApplicationException e) {
            if (log.isTraceEnabled()) {
                log.trace(e.getMessage(), e);
            }
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    @Nullable
    private AbstractToken fillResponse(String token, IntrospectionResponse response, AuthorizationGrant grantOfIntrospectionToken) {
        AbstractToken tokenToIntrospect = null;
        if (grantOfIntrospectionToken != null) {
            tokenToIntrospect = grantOfIntrospectionToken.getAccessToken(token);

            response.setActive(tokenToIntrospect.isValid());
            response.setExpiresAt(ServerUtil.dateToSeconds(tokenToIntrospect.getExpirationDate()));
            response.setIssuedAt(ServerUtil.dateToSeconds(tokenToIntrospect.getCreationDate()));
            response.setAcr(grantOfIntrospectionToken.getAcrValues());
            response.setScope(grantOfIntrospectionToken.getScopes() != null ? grantOfIntrospectionToken.getScopes() : Lists.newArrayList()); // #433
            response.setClientId(grantOfIntrospectionToken.getClientId());
            response.setSub(grantOfIntrospectionToken.getSub());
            response.setUsername(grantOfIntrospectionToken.getUserId());
            response.setIssuer(appConfiguration.getIssuer());
            response.setAudience(grantOfIntrospectionToken.getClientId());
            response.setAuthTime(ServerUtil.dateToSeconds(grantOfIntrospectionToken.getAuthenticationTime()));

            final AuthzDetails authzDetails = grantOfIntrospectionToken.getAuthzDetails();
            if (!AuthzDetails.isEmpty(authzDetails)) {
                try {
                    JsonNode authorizationDetailsNode = OBJECT_MAPPER.readTree(authzDetails.asJsonString());
                    response.setAuthorizationDetails(authorizationDetailsNode);
                } catch (JsonProcessingException e) {
                    log.error(String.format("Failed to convert authorization_details %s", authzDetails.asJsonString()), e);
                }
            }

            if (tokenToIntrospect instanceof AccessToken) {
                AccessToken accessToken = (AccessToken) tokenToIntrospect;
                response.setTokenType(accessToken.getTokenType() != null ? accessToken.getTokenType().getName() : io.jans.as.model.common.TokenType.BEARER.getName());

                // DPoP
                if (StringUtils.isNotBlank(accessToken.getDpop())) {
                    response.setNotBefore(accessToken.getCreationDate().getTime());
                    HashMap<String, String> cnf = new HashMap<>();
                    cnf.put("jkt", accessToken.getDpop());
                    response.setCnf(cnf);
                }
            }
        } else {
            if (log.isDebugEnabled())
                log.debug("Failed to find grant for access_token: {}. Return 200 with active=false.", escapeLog(token));
        }
        return tokenToIntrospect;
    }

    private JSONObject createResponseAsJsonObject(IntrospectionResponse response, AuthorizationGrant grantOfIntrospectionToken) throws JSONException, IOException {
        final JSONObject result = new JSONObject(ServerUtil.asJson(response));

        if (log.isTraceEnabled()) {
            log.trace("grantOfIntrospectionToken: {}, x5ts256: {}", (grantOfIntrospectionToken != null), (grantOfIntrospectionToken != null ? grantOfIntrospectionToken.getX5ts256() : ""));
        }

        if (grantOfIntrospectionToken != null && StringUtils.isNotBlank(grantOfIntrospectionToken.getX5ts256())) {
            JSONObject cnf = result.optJSONObject("cnf");
            if (cnf == null) {
                cnf = new JSONObject();
                result.put("cnf", cnf);
            }

            cnf.put("x5t#S256", grantOfIntrospectionToken.getX5ts256());
        }

        return result;
    }

    /**
     * @return we return pair of authorization grant or otherwise true - if it's basic client authentication or false if it is not
     * @throws UnsupportedEncodingException when encoding is not supported
     */
    private Pair<AuthorizationGrant, Boolean> getAuthorizationGrant(String authorization, String accessToken) throws UnsupportedEncodingException {
        AuthorizationGrant grant = tokenService.getBearerAuthorizationGrant(authorization);
        if (grant != null) {
            final String authorizationAccessToken = tokenService.getBearerToken(authorization);
            final AbstractToken accessTokenObject = grant.getAccessToken(authorizationAccessToken);
            if (accessTokenObject != null && accessTokenObject.isValid()) {
                return new Pair<>(grant, false);
            } else {
                log.error("Access token is not valid: {}", authorizationAccessToken);
                return EMPTY;
            }
        }

        grant = tokenService.getBasicAuthorizationGrant(authorization);
        if (grant != null) {
            return new Pair<>(grant, false);
        }
        if (tokenService.isBasicAuthToken(authorization)) {
            return isBasicTokenValid(authorization, accessToken);
        }
        return EMPTY;
    }

    private Pair<AuthorizationGrant, Boolean> isBasicTokenValid(String authorization, String accessToken) throws UnsupportedEncodingException {
        String encodedCredentials = tokenService.getBasicToken(authorization);

        String token = new String(Base64.decodeBase64(encodedCredentials), StandardCharsets.UTF_8);

        int delim = token.indexOf(":");

        if (delim == -1) {
            return EMPTY;
        }

        String clientId = URLDecoder.decode(token.substring(0, delim), Util.UTF8_STRING_ENCODING);
        String password = URLDecoder.decode(token.substring(delim + 1), Util.UTF8_STRING_ENCODING);
        if (clientService.authenticate(clientId, password)) {
            AuthorizationGrant grant = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);
            if (BooleanUtils.isTrue(appConfiguration.getIntrospectionRestrictBasicAuthnToOwnTokens()) && grant != null && !grant.getClientId().equals(clientId)) {
                log.trace("Failed to match grant object clientId and client id provided during authentication.");
                return EMPTY;
            }
            return new Pair<>(grant, true);
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Failed to perform basic authentication for client: {}", clientId);
            }
        }
        return EMPTY;
    }

}
