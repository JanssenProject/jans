/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.introspection.ws.rs;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xdi.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.xdi.oxauth.model.common.*;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.ldap.PairwiseIdentifier;
import org.xdi.oxauth.model.uma.UmaScopeType;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.PairwiseIdentifierService;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.util.ServerUtil;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/09/2013
 */
@Path("/introspection")
@Api(value = "/introspection", description = "The Introspection Endpoint is an OAuth 2 Endpoint that responds to " +
        "   HTTP GET and HTTP POST requests from token holders.  The endpoint " +
        "   takes a single parameter representing the token (and optionally " +
        "   further authentication) and returns a JSON document representing the meta information surrounding the token.")
public class IntrospectionWebService {

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
    private PairwiseIdentifierService pairwiseIdentifierService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "invalid_request\n" +
                    "The request is missing a required parameter, includes an unsupported parameter or parameter value, repeats the same parameter or is otherwise malformed.  The resource server SHOULD respond with the HTTP 400 (Bad Request) status code."),
            @ApiResponse(code = 500, message = "Introspection Internal Server Failed.")
    })
    public Response introspectGet(@HeaderParam("Authorization") String p_authorization,
                                  @QueryParam("token") String p_token,
                                  @QueryParam("token_type_hint") String tokenTypeHint
    ) {
        return introspect(p_authorization, p_token, tokenTypeHint);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectPost(@HeaderParam("Authorization") String p_authorization,
                                   @FormParam("token") String p_token,
                                   @FormParam("token_type_hint") String tokenTypeHint) {
        return introspect(p_authorization, p_token, tokenTypeHint);
    }

    private Response introspect(String p_authorization, String p_token, String tokenTypeHint) {
        try {
            log.trace("Introspect token, authorization: {}, token to introsppect: {}, tokenTypeHint:", p_authorization, p_token, tokenTypeHint);
            if (StringUtils.isNotBlank(p_authorization) && StringUtils.isNotBlank(p_token)) {
                final AuthorizationGrant authorizationGrant = getAuthorizationGrant(p_authorization, p_token);
                if (authorizationGrant != null) {
                    final AbstractToken authorizationAccessToken = authorizationGrant.getAccessToken(tokenService.getTokenFromAuthorizationParameter(p_authorization));
                    boolean isPat = authorizationGrant.getScopesAsString().contains(UmaScopeType.PROTECTION.getValue()); // #432
                    if (authorizationAccessToken != null && authorizationAccessToken.isValid() && isPat) {
                        final IntrospectionResponse response = new IntrospectionResponse(false);

                        final AuthorizationGrant grantOfIntrospectionToken = authorizationGrantList.getAuthorizationGrantByAccessToken(p_token);
                        if (grantOfIntrospectionToken != null) {
                            final AbstractToken tokenToIntrospect = grantOfIntrospectionToken.getAccessToken(p_token);
                            final User user = grantOfIntrospectionToken.getUser();

                            response.setActive(tokenToIntrospect.isValid());
                            response.setExpiresAt(ServerUtil.dateToSeconds(tokenToIntrospect.getExpirationDate()));
                            response.setIssuedAt(ServerUtil.dateToSeconds(tokenToIntrospect.getCreationDate()));
                            response.setAcrValues(tokenToIntrospect.getAuthMode());
                            response.setScopes(grantOfIntrospectionToken.getScopes() != null ? grantOfIntrospectionToken.getScopes() : new ArrayList<String>()); // #433
                            response.setClientId(grantOfIntrospectionToken.getClientId());
                            response.setSub(getSub(grantOfIntrospectionToken));
                            response.setUsername(user != null ? user.getAttribute("displayName") : null);
                            response.setIssuer(appConfiguration.getIssuer());
                            response.setAudience(grantOfIntrospectionToken.getClientId());

                            if (tokenToIntrospect instanceof AccessToken) {
                                AccessToken accessToken = (AccessToken) tokenToIntrospect;
                                response.setTokenType(accessToken.getTokenType() != null ? accessToken.getTokenType().getName() : TokenType.BEARER.getName());
                            }
                        } else {
                            log.error("Failed to find grant for access_token: " + p_token);
                        }
                        return Response.status(Response.Status.OK).entity(ServerUtil.asJson(response)).build();
                    } else {
                        log.error("Access token is not valid. Valid: " + (authorizationAccessToken != null && authorizationAccessToken.isValid()) + ", isPat:" + isPat);
                    }
                } else {
                    log.error("Authorization grant is null.");
                }

                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.ACCESS_DENIED)).build();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST)).build();
    }

    private String getSub(AuthorizationGrant grant) {
        final User user = grant.getUser();
        if (user == null) {
            log.trace("User is null for grant " + grant.getGrantId());
            return "";
        }
        final String subjectType = grant.getClient().getSubjectType();
        if (SubjectType.PAIRWISE.equals(SubjectType.fromString(subjectType))) {
            String sectorIdentifierUri = null;
            if (StringUtils.isNotBlank(grant.getClient().getSectorIdentifierUri())) {
                sectorIdentifierUri = grant.getClient().getSectorIdentifierUri();
            } else {
                sectorIdentifierUri = grant.getClient().getRedirectUris()[0];
            }

            String userInum = user.getAttribute("inum");

            try {
                PairwiseIdentifier pairwiseIdentifier = pairwiseIdentifierService.findPairWiseIdentifier(userInum, sectorIdentifierUri);
                if (pairwiseIdentifier == null) {
                    pairwiseIdentifier = new PairwiseIdentifier(sectorIdentifierUri);
                    pairwiseIdentifier.setId(UUID.randomUUID().toString());
                    pairwiseIdentifier.setDn(pairwiseIdentifierService.getDnForPairwiseIdentifier(pairwiseIdentifier.getId(), userInum));
                    pairwiseIdentifierService.addPairwiseIdentifier(userInum, pairwiseIdentifier);
                }
                return pairwiseIdentifier.getId();
            } catch (Exception e) {
                log.error("Failed to get sub claim. PairwiseIdentifierService failed to find pair wise identifier.", e);
                return "";
            }
        } else {
            return user.getAttribute(appConfiguration.getOpenidSubAttribute());
        }
    }

    private AuthorizationGrant getAuthorizationGrant(String authorization, String accessToken) throws UnsupportedEncodingException {
        AuthorizationGrant grant = tokenService.getAuthorizationGrantByPrefix(authorization, "Bearer ");
        if (grant == null) {
            grant = tokenService.getAuthorizationGrantByPrefix(authorization, "Basic ");
            if (grant != null) {
                return grant;
            }
            if (StringUtils.startsWithIgnoreCase(authorization, "Basic ")) {

                String encodedCredentials = authorization.substring("Basic ".length());

                String token = new String(Base64.decodeBase64(encodedCredentials), Util.UTF8_STRING_ENCODING);

                int delim = token.indexOf(":");

                if (delim != -1) {
                    String clientId = URLDecoder.decode(token.substring(0, delim), Util.UTF8_STRING_ENCODING);
                    String password = URLDecoder.decode(token.substring(delim + 1), Util.UTF8_STRING_ENCODING);
                    if (clientService.authenticate(clientId, password)) {
                        final AuthorizationGrant grantOfIntrospectionToken = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);
                        if (grantOfIntrospectionToken != null) {
                            if (!grantOfIntrospectionToken.getClientId().equals(clientId)) {
                                log.trace("Failed to match grant object clientId and client id provided during authentication.");
                                return null;
                            }
                            return authorizationGrantList.getAuthorizationGrantByAccessToken(encodedCredentials);
                        }
                    } else {
                        log.trace("Failed to perform basic authentication for client: " + clientId);
                    }

                }
            }
        }
        return grant;
    }

}
