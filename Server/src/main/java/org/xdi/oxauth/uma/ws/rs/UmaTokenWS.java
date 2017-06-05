/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import com.wordnik.swagger.annotations.Api;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.model.uma.ClaimDefinition;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.config.WebKeysConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.token.JsonWebResponse;
import org.xdi.oxauth.model.token.JwtSigner;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.UmaTokenResponse;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.UmaScopeDescription;
import org.xdi.oxauth.security.Identity;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.external.ExternalUmaAuthorizationPolicyService;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.uma.authorization.*;
import org.xdi.oxauth.uma.service.UmaPermissionService;
import org.xdi.oxauth.uma.service.UmaRptService;
import org.xdi.oxauth.uma.service.UmaValidationService;
import org.xdi.oxauth.util.ServerUtil;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The endpoint at which the RP asks for token.
 */
@Path("/uma/token")
@Api(value = "/uma/token", description = "UMA Token endpoint.")
public class UmaTokenWS {

    @Inject
    private Logger log;

    @Inject
    private Identity identity;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private UmaRptService rptService;

    @Inject
    private UmaPermissionService permissionService;

    @Inject
    private UmaValidationService umaValidationService;

    @Inject
    private UmaAuthorizationService umaAuthorizationService;

    @Inject
    private ClientService clientService;

    @Inject
    private TokenService tokenService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private LdapEntryManager ldapEntryManager;

    @Inject
    private ExternalUmaAuthorizationPolicyService policyService;

    @Inject
    private AttributeService attributeService;

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response requestRpt(
            @FormParam("grant_type")
            String grantType,
            @FormParam("ticket")
            String ticket,
            @FormParam("claim_token")
            String claimToken,
            @FormParam("claim_token_format")
            String claimTokenFormat,
            @FormParam("pct")
            String pctCode,
            @FormParam("rpt")
            String rptCode,
            @FormParam("scope")
            String scope,
            @Context HttpServletRequest httpRequest) {
        try {
            log.trace("requestRpt grant_type: {}, ticket: {}, claim_token: {}, claim_token_format: {}, pct: {}, rpt: {}, scope: {}"
                    , grantType, ticket, claimToken, claimTokenFormat, pctCode, rptCode, scope);

            umaValidationService.validateGrantType(grantType);
            List<UmaPermission> permissions = umaValidationService.validateTicket(ticket);
            Jwt idToken = umaValidationService.validateClaimToken(claimToken, claimTokenFormat);
            UmaPCT pct = umaValidationService.validatePct(pctCode);
            UmaRPT rpt = umaValidationService.validateRPT(rptCode);
            List<UmaScopeDescription> scopes = umaValidationService.validateScopes(scope, permissions);

            Claims claims = new Claims(idToken, pct);

            List<CustomScriptConfiguration> scripts = checkNeedsInfo(claims, scopes, permissions);

            boolean granted = false;

            if (scripts.isEmpty()) {
                granted = true;
            } else {
                for (CustomScriptConfiguration script : scripts) {
                    UmaAuthorizationContext context = new UmaAuthorizationContext(attributeService, );
                    final boolean result = policyService.authorize(script, context);
                    log.trace("Policy script inum: '{}' result: '{}'", script.getInum(), result);
                }
            }
            // todo uma2 schedule for remove ?
            final AuthorizationGrant grant = null;//umaValidationService.assertHasAuthorizationScope(authorization);

//            final UmaRPT rpt = authorizeRptPermission(authorization, rptAuthorizationRequest, httpRequest, grant);

            UmaTokenResponse response = new UmaTokenResponse();

            return Response.ok(ServerUtil.asJson(response)).build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }
        }

        log.error("Failed to handle request to UMA Token Endpoint.");
        throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
    }

    private List<CustomScriptConfiguration> checkNeedsInfo(Claims claims, List<UmaScopeDescription> requestedScopes, List<UmaPermission> permissions) {
        Set<String> authorizationPolicyDNs = umaAuthorizationService.getAuthorizationPolicyDNs(requestedScopes);

        List<CustomScriptConfiguration> scripts = new ArrayList<CustomScriptConfiguration>();

        List<ClaimDefinition> missedClaims = new ArrayList<ClaimDefinition>();

        for (String scriptDN : authorizationPolicyDNs) {
            CustomScriptConfiguration script = policyService.getAuthorizationPolicyByDn(scriptDN);

            if (script != null) {
                List<ClaimDefinition> requiredClaims = policyService.getRequiredClaims(script);
                if (requiredClaims != null && !requiredClaims.isEmpty()) {
                    for (ClaimDefinition definition : requiredClaims) {
                        if (!claims.has(definition.getName())) {
                            missedClaims.add(definition);
                        }
                    }
                }
            } else {
                log.error("Unable to load UMA script dn: '{}'", scriptDN);
            }
        }

        if (!missedClaims.isEmpty()) {
            String newTicket = generateNewTicket(permissions);

            UmaNeedInfoResponse needInfoResponse = new UmaNeedInfoResponse();
            needInfoResponse.setTicket(newTicket);
            needInfoResponse.setError("need_info");
            needInfoResponse.setRedirectUser(appConfiguration.getBaseEndpoint() + "/uma/gather_claims");
            needInfoResponse.setRequiredClaims(missedClaims);

            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).entity(ServerUtil.asJsonSilently(needInfoResponse)).build());
        }

        return scripts;
    }

    private String generateNewTicket(List<UmaPermission> permissions) {
        if (permissions.isEmpty()) {
            return permissionService.generateNewTicket();
        } else {
            return permissionService.changeTicket(permissions);
        }
    }

    private UmaRPT authorizeRptPermission(String authorization,
                                          HttpServletRequest httpRequest,
                                          AuthorizationGrant grant) {
//        UmaRPT rpt;
//        if (Util.isNullOrEmpty(rptAuthorizationRequest.getRpt())) {
//            rpt = rptService.createRPT(authorization);
//        } else {
//            rpt = rptService.getRPTByCode(rptAuthorizationRequest.getRpt());
//        }
//
//        // Validate RPT
//        try {
//            umaValidationService.validateRPT(rpt);
//        } catch (WebApplicationException e) {
//            // according to latest UMA spec ( dated 2015-02-23 https://docs.kantarainitiative.org/uma/draft-uma-core.html)
//            // it's up to implementation whether to create new RPT for each request or pass back requests RPT.
//            // Here we decided to pass back new RPT if request's RPT in invalid.
//            rpt = rptService.getRPTByCode(rptAuthorizationRequest.getRpt());
//        }

//        final List<UmaPermission> permissions = permissionService.getPermissionByTicket(rptAuthorizationRequest.getTicket());
//
//        umaValidationService.validatePermissions(permissions);
//
//        boolean allowToAdd = true;
//        for (UmaPermission permission : permissions) {
//            if (!umaAuthorizationService.allowToAddPermission(grant, rpt, permission, httpRequest, rptAuthorizationRequest.getClaims())) {
//                allowToAdd = false;
//                break;
//            }
//        }
//
//        if (allowToAdd) {
//            for (UmaPermission permission : permissions) {
//                rptService.addPermissionToRPT(rpt, permission);
//                invalidateTicket(permission);
//            }
//            return rpt;
//        }

        // throw not authorized exception
        throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.NOT_AUTHORIZED_PERMISSION)).build());
    }

    private void invalidateTicket(UmaPermission permission) {
        try {
            permission.setStatus("invalidated"); // invalidate ticket and persist
            ldapEntryManager.merge(permission);
        } catch (Exception e) {
            log.error("Failed to invalidate ticket: " + permission.getTicket() + ". " + e.getMessage(), e);
        }
    }

    private JsonWebResponse createJwr(UmaRPT rpt, String authorization, List<String> gluuAccessTokenScopes) throws Exception {
        final AuthorizationGrant grant = tokenService.getAuthorizationGrant(authorization);

        JwtSigner jwtSigner = JwtSigner.newJwtSigner(appConfiguration, webKeysConfiguration, grant.getClient());
        Jwt jwt = jwtSigner.newJwt();

        jwt.getClaims().setExpirationTime(rpt.getExpirationDate());
        jwt.getClaims().setIssuedAt(rpt.getCreationDate());

        if (!gluuAccessTokenScopes.isEmpty()) {
            jwt.getClaims().setClaim("scopes", gluuAccessTokenScopes);
        }

        return jwtSigner.sign();
    }

//    UmaRPT rpt = rptService.createRPT(authorization);
//
//    String rptResponse = rpt.getCode();
//    final Boolean umaRptAsJwt = appConfiguration.getUmaRptAsJwt();
//    if (umaRptAsJwt != null && umaRptAsJwt) {
//        rptResponse = createJwr(rpt, authorization, Lists.<String>newArrayList()).asString();
//    }
}
