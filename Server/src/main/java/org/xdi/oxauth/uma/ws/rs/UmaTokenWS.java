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
import org.xdi.oxauth.model.config.WebKeysConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.registration.Client;
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
import org.xdi.oxauth.uma.service.*;
import org.xdi.oxauth.util.ServerUtil;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.*;

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
    private UmaPctService pctService;

    @Inject
    private UmaResourceService resourceService;

    @Inject
    private UmaPermissionService permissionService;

    @Inject
    private UmaValidationService umaValidationService;

    @Inject
    private UmaScriptService umaScriptService;

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
            Map<UmaScopeDescription, Boolean> scopes = umaValidationService.validateScopes(scope, permissions);

            Claims claims = new Claims(idToken, pct);

            List<CustomScriptConfiguration> scripts = checkNeedsInfo(claims, scopes.keySet(), permissions);
            UmaAuthorizationContextBuilder contextBuilder = new UmaAuthorizationContextBuilder(attributeService, resourceService, permissions, scopes, claims, httpRequest);

            if (!scripts.isEmpty()) {
                for (CustomScriptConfiguration script : scripts) {
                    UmaAuthorizationContext authorizationContext = contextBuilder.build(script);
                    final boolean result = policyService.authorize(script, authorizationContext);
                    log.trace("Policy script inum: '{}' result: '{}'", script.getInum(), result);
                    if (!result) {
                        log.trace("Stop authorization scripts execution, current script returns false, script inum: " + script.getInum());

                        throw new UmaWebException(Response.Status.FORBIDDEN, errorResponseFactory, UmaErrorResponseType.FORBIDDEN_BY_POLICY);
                    }
                }
            } else {
                log.warn("There are no any policies that protects scopes. Scopes: " + UmaScopeService.asString(scopes.keySet()));
                log.warn("Access granted because there are no any protection. Make sure it is intentional behavior.");
            }

            log.trace("Access granted.");

            Client client = identity.getSetSessionClient().getClient();

            final boolean upgraded;
            if (rpt == null) {
                rpt = rptService.createRPTAndPersist(client.getClientId());
                upgraded = false;
            } else {
                upgraded = true;
            }

            updatePermissionsWithClientRequestedScope(permissions, scopes);

            rptService.addPermissionToRPT(rpt, permissions);

            pct = pctService.updateClaims(pct, idToken, claims, client.getClientId());

            UmaTokenResponse response = new UmaTokenResponse();
            response.setAccessToken(rpt.getCode());
            response.setUpgraded(upgraded);
            response.setTokenType("Bearer");
            response.setPct(pct.getCode());

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

    private void updatePermissionsWithClientRequestedScope(List<UmaPermission> permissions, Map<UmaScopeDescription, Boolean> scopes) {
        for (UmaPermission permission : permissions) {
            Set<String> scopeDns = new HashSet<String>(permission.getScopeDns());

            for (Map.Entry<UmaScopeDescription, Boolean> entry : scopes.entrySet()) {
                if (entry.getValue()) {
                    scopeDns.add(entry.getKey().getDn());
                }
            }

            permission.setScopeDns(new ArrayList<String>(scopeDns));
        }
    }

    private List<CustomScriptConfiguration> checkNeedsInfo(Claims claims, Set<UmaScopeDescription> requestedScopes, List<UmaPermission> permissions) {
        Set<String> scriptDNs = umaScriptService.getScriptDNs(new ArrayList<UmaScopeDescription>(requestedScopes));

        List<CustomScriptConfiguration> scripts = new ArrayList<CustomScriptConfiguration>();

        List<ClaimDefinition> missedClaims = new ArrayList<ClaimDefinition>();

        for (String scriptDN : scriptDNs) {
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
}
