/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.introspection.ws.rs;

import com.google.common.collect.Lists;
import io.jans.as.common.claims.Audience;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.uma.UmaScopeType;
import io.jans.as.model.util.Util;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AccessToken;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.external.ExternalIntrospectionService;
import io.jans.as.server.service.external.context.ExternalIntrospectionContext;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import io.jans.util.Pair;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;

import static io.jans.as.model.util.Util.escapeLog;
import static org.apache.commons.lang.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version September 30, 2021
 */
@Path("/introspection")
public class IntrospectionWebService {

    private static final Pair<AuthorizationGrant, Boolean> EMPTY = new Pair<>(null, false);

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
    private WebKeysConfiguration webKeysConfiguration;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectGet(@HeaderParam("Authorization") String authorization,
                                  @QueryParam("token") String token,
                                  @QueryParam("token_type_hint") String tokenTypeHint,
                                  @QueryParam("response_as_jwt") String responseAsJwt,
                                  @Context HttpServletRequest httpRequest,
                                  @Context HttpServletResponse httpResponse
    ) {
        return introspect(authorization, token, tokenTypeHint, responseAsJwt, httpRequest, httpResponse);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectPost(@HeaderParam("Authorization") String authorization,
                                   @FormParam("token") String token,
                                   @FormParam("token_type_hint") String tokenTypeHint,
                                   @FormParam("response_as_jwt") String responseAsJwt,
                                   @Context HttpServletRequest httpRequest,
                                   @Context HttpServletResponse httpResponse) {
        return introspect(authorization, token, tokenTypeHint, responseAsJwt, httpRequest, httpResponse);
    }

    private AuthorizationGrant validateAuthorization(String authorization, String token) throws UnsupportedEncodingException {
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
            log.error("Authorization grant is null.");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED, "Authorization grant is null.")).build());
        }

        final AbstractToken authorizationAccessToken = authorizationGrant.getAccessToken(tokenService.getToken(authorization));

        if ((authorizationAccessToken == null || !authorizationAccessToken.isValid()) && BooleanUtils.isFalse(pair.getSecond())) {
            log.error("Access token is not valid. Valid: {}, basicClientAuthentication: {}", (authorizationAccessToken != null && authorizationAccessToken.isValid()), pair.getSecond());
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED, "Access token is not valid")).build());
        }

        if (isTrue(appConfiguration.getIntrospectionAccessTokenMustHaveUmaProtectionScope()) &&
                !authorizationGrant.getScopesAsString().contains(UmaScopeType.PROTECTION.getValue())) { // #562 - make uma_protection optional
            final String reason = "access_token used to access introspection endpoint does not have uma_protection scope, however in oxauth configuration `checkUmaProtectionScopePresenceDuringIntrospection` is true";
            log.trace(reason);
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity(errorResponseFactory.errorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED, reason)).type(MediaType.APPLICATION_JSON_TYPE).build());
        }
        return authorizationGrant;
    }

    private Response introspect(String authorization, String token, String tokenTypeHint, String responseAsJwt, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("Introspect token, authorization: {}, token to introspect: {}, tokenTypeHint: {}", escapeLog(authorization), escapeLog(token), escapeLog(tokenTypeHint));
            }

            AuthorizationGrant authorizationGrant = validateAuthorization(authorization, token);

            if (StringUtils.isBlank(token)) {
                log.trace("Bad request: Token is blank.");
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, "")).build();
            }

            final io.jans.as.model.common.IntrospectionResponse response = new io.jans.as.model.common.IntrospectionResponse(false);

            final AuthorizationGrant grantOfIntrospectionToken = authorizationGrantList.getAuthorizationGrantByAccessToken(token);

            AbstractToken tokenToIntrospect = fillResponse(token, response, grantOfIntrospectionToken);
            JSONObject responseAsJsonObject = createResponseAsJsonObject(response, tokenToIntrospect);

            ExternalIntrospectionContext context = new ExternalIntrospectionContext(authorizationGrant, httpRequest, httpResponse, appConfiguration, attributeService);
            context.setGrantOfIntrospectionToken(grantOfIntrospectionToken);
            if (externalIntrospectionService.executeExternalModifyResponse(responseAsJsonObject, context)) {
                log.trace("Successfully run external introspection scripts.");
            } else {
                responseAsJsonObject = createResponseAsJsonObject(response, tokenToIntrospect);
                log.trace("Canceled changes made by external introspection script since method returned `false`.");
            }

            // Make scopes conform as required by spec, see #1499
            if (response.getScope() != null && !appConfiguration.getIntrospectionResponseScopesBackwardCompatibility()) {
                String scopes = StringUtils.join(response.getScope().toArray(), " ");
                responseAsJsonObject.put("scope", scopes);
            }
            if (Boolean.TRUE.toString().equalsIgnoreCase(responseAsJwt)) {
                return Response.status(Response.Status.OK).entity(createResponseAsJwt(responseAsJsonObject, grantOfIntrospectionToken)).build();
            }

            return Response.status(Response.Status.OK).entity(responseAsJsonObject.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();

        } catch (WebApplicationException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
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
            response.setAcrValues(grantOfIntrospectionToken.getAcrValues());
            response.setScope(grantOfIntrospectionToken.getScopes() != null ? grantOfIntrospectionToken.getScopes() : Lists.newArrayList()); // #433
            response.setClientId(grantOfIntrospectionToken.getClientId());
            response.setSub(grantOfIntrospectionToken.getSub());
            response.setUsername(grantOfIntrospectionToken.getUserId());
            response.setIssuer(appConfiguration.getIssuer());
            response.setAudience(grantOfIntrospectionToken.getClientId());

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

    private String createResponseAsJwt(JSONObject response, AuthorizationGrant grant) throws Exception {
        final JwtSigner jwtSigner = JwtSigner.newJwtSigner(appConfiguration, webKeysConfiguration, grant.getClient());
        final Jwt jwt = jwtSigner.newJwt();
        Audience.setAudience(jwt.getClaims(), grant.getClient());

        Iterator<String> keysIter = response.keys();
        while (keysIter.hasNext()) {
            String key = keysIter.next();
            Object value = response.opt(key);
            if (value != null) {
                try {
                    jwt.getClaims().setClaimObject(key, value, false);
                } catch (Exception e) {
                    log.error("Failed to put claims into jwt. Key: " + key + ", response: " + response.toString(), e);
                }
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("Response before signing: {}", jwt.getClaims().toJsonString());
        }

        return jwtSigner.sign().toString();
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
            if (grant != null && !grant.getClientId().equals(clientId)) {
                log.trace("Failed to match grant object clientId and client id provided during authentication.");
                return EMPTY;
            }
            return new Pair<>(grant, true);
        } else {
            if (log.isTraceEnabled())
                log.trace("Failed to perform basic authentication for client: {}", clientId);
        }
        return EMPTY;
    }

}
