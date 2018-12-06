/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.xdi.oxauth.model.config.WebKeysConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.UmaTokenResponse;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.UmaScopeDescription;
import org.xdi.oxauth.security.Identity;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.external.ExternalUmaRptPolicyService;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.uma.authorization.Claims;
import org.xdi.oxauth.uma.authorization.UmaAuthorizationContext;
import org.xdi.oxauth.uma.authorization.UmaPCT;
import org.xdi.oxauth.uma.authorization.UmaRPT;
import org.xdi.oxauth.uma.authorization.UmaScriptByScope;
import org.xdi.oxauth.uma.authorization.UmaWebException;
import org.xdi.oxauth.util.ServerUtil;

/**
 * UMA Token Service
 */
@Named
@Stateless
public class UmaTokenService {

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
    private UmaPermissionService permissionService;
    @Inject
    private UmaValidationService umaValidationService;
    @Inject
    private ClientService clientService;
    @Inject
    private TokenService tokenService;
    @Inject
    private AppConfiguration appConfiguration;
    @Inject
    private WebKeysConfiguration webKeysConfiguration;
    @Inject
    private UmaNeedsInfoService umaNeedsInfoService;
    @Inject
    private ExternalUmaRptPolicyService policyService;
    @Inject
    private UmaExpressionService expressionService;

    public Response requestRpt(
            String grantType,
            String ticket,
            String claimToken,
            String claimTokenFormat,
            String pctCode,
            String rptCode,
            String scope,
            HttpServletRequest httpRequest) {
        try {
            log.trace("requestRpt grant_type: {}, ticket: {}, claim_token: {}, claim_token_format: {}, pct: {}, rpt: {}, scope: {}"
                    , grantType, ticket, claimToken, claimTokenFormat, pctCode, rptCode, scope);

            umaValidationService.validateGrantType(grantType);
            List<UmaPermission> permissions = umaValidationService.validateTicket(ticket);
            Jwt idToken = umaValidationService.validateClaimToken(claimToken, claimTokenFormat);
            UmaPCT pct = umaValidationService.validatePct(pctCode);
            UmaRPT rpt = umaValidationService.validateRPT(rptCode);
            Map<UmaScopeDescription, Boolean> scopes = umaValidationService.validateScopes(scope, permissions);
            Client client = identity.getSessionClient().getClient();

            if (client != null && client.isDisabled()) {
                throw new UmaWebException(Response.Status.FORBIDDEN, errorResponseFactory, UmaErrorResponseType.DISABLED_CLIENT);
            }

            pct = pctService.updateClaims(pct, idToken, client.getClientId(), permissions); // creates new pct if pct is null in request
            Claims claims = new Claims(idToken, pct, claimToken);

            Map<UmaScriptByScope, UmaAuthorizationContext> scriptMap = umaNeedsInfoService.checkNeedsInfo(claims, scopes, permissions, pct, httpRequest, client);

            if (!scriptMap.isEmpty()) {
                expressionService.evaluate(scriptMap, permissions);
            } else {
                log.warn("There are no any policies that protects scopes. Scopes: " + UmaScopeService.asString(scopes.keySet()) + ". Configuration property umaGrantAccessIfNoPolicies: " + appConfiguration.getUmaGrantAccessIfNoPolicies());

                if (appConfiguration.getUmaGrantAccessIfNoPolicies() != null && appConfiguration.getUmaGrantAccessIfNoPolicies()) {
                    log.warn("Access granted because there are no any protection. Make sure it is intentional behavior.");
                } else {
                    log.warn("Access denied because there are no any protection. Make sure it is intentional behavior.");
                    throw new UmaWebException(Response.Status.FORBIDDEN, errorResponseFactory, UmaErrorResponseType.FORBIDDEN_BY_POLICY);
                }
            }

            log.trace("Access granted.");

            updatePermissionsWithClientRequestedScope(permissions, scopes);
            addPctToPermissions(permissions, pct);

            final boolean upgraded;
            if (rpt == null) {
                rpt = rptService.createRPTAndPersist(client, permissions);
                upgraded = false;
            } else {
                rptService.addPermissionToRPT(rpt, permissions);
                upgraded = true;
            }

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
        throw new UmaWebException(Response.Status.INTERNAL_SERVER_ERROR, errorResponseFactory, UmaErrorResponseType.SERVER_ERROR);
    }

    private void addPctToPermissions(List<UmaPermission> permissions, UmaPCT pct) {
        for (UmaPermission p : permissions) {
            p.getAttributes().put(UmaPermission.PCT, pct.getCode());
            permissionService.mergeSilently(p);
        }
    }

    private void updatePermissionsWithClientRequestedScope(List<UmaPermission> permissions, Map<UmaScopeDescription, Boolean> scopes) {
        log.trace("Updating permissions with requested scopes ...");
        for (UmaPermission permission : permissions) {
            Set<String> scopeDns = new HashSet<String>(permission.getScopeDns());

            for (Map.Entry<UmaScopeDescription, Boolean> entry : scopes.entrySet()) {
                log.trace("Updating permissions with scope: " + entry.getKey().getId() + ", isRequestedScope: " + entry.getValue() + ", permisson: " + permission.getDn());
                scopeDns.add(entry.getKey().getDn());
            }

            permission.setScopeDns(new ArrayList<String>(scopeDns));
        }
    }
}
