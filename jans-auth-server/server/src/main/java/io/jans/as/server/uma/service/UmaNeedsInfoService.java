/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.service;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaNeedInfoResponse;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.service.external.ExternalUmaRptPolicyService;
import io.jans.as.server.uma.authorization.Claims;
import io.jans.as.server.uma.authorization.UmaAuthorizationContext;
import io.jans.as.server.uma.authorization.UmaAuthorizationContextBuilder;
import io.jans.as.server.uma.authorization.UmaPCT;
import io.jans.as.server.uma.authorization.UmaScriptByScope;
import io.jans.as.server.util.ServerUtil;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.uma.ClaimDefinition;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author yuriyz on 06/16/2017.
 */
@Stateless
@Named
public class UmaNeedsInfoService {

    @Inject
    private Logger log;
    @Inject
    private AppConfiguration appConfiguration;
    @Inject
    private UmaPermissionService permissionService;
    @Inject
    private UmaResourceService resourceService;
    @Inject
    private ExternalUmaRptPolicyService policyService;
    @Inject
    private UmaSessionService sessionService;

    public static Set<String> getScriptDNs(List<Scope> scopes) {
        HashSet<String> result = new HashSet<>();

        for (Scope scope : scopes) {
            List<String> authorizationPolicies = scope.getUmaAuthorizationPolicies();
            if (authorizationPolicies != null) {
                result.addAll(authorizationPolicies);
            }
        }

        return result;
    }

    public Map<UmaScriptByScope, UmaAuthorizationContext> checkNeedsInfo(Claims claims, Map<Scope, Boolean> requestedScopes,
                                                                         List<UmaPermission> permissions, UmaPCT pct, HttpServletRequest httpRequest,
                                                                         Client client) {

        Map<UmaScriptByScope, UmaAuthorizationContext> scriptMap = new HashMap<>();
        Map<String, String> ticketAttributes = new HashMap<>();

        List<ClaimDefinition> missedClaims = new ArrayList<>();

        UmaAuthorizationContextBuilder contextBuilder = new UmaAuthorizationContextBuilder(appConfiguration,
                resourceService, permissions, requestedScopes, claims, httpRequest,
                sessionService, permissionService, client);


        for (Scope scope : requestedScopes.keySet()) {
            List<String> authorizationPolicies = scope.getUmaAuthorizationPolicies();
            if (authorizationPolicies != null && !authorizationPolicies.isEmpty()) {
                for (String scriptDN : authorizationPolicies) {
                    CustomScriptConfiguration script = policyService.getScriptByDn(scriptDN);
                    if (script != null) {
                        UmaAuthorizationContext context = contextBuilder.build(script);
                        scriptMap.put(new UmaScriptByScope(scope, script), context);

                        List<ClaimDefinition> requiredClaims = policyService.getRequiredClaims(script, context);
                        if (requiredClaims != null && !requiredClaims.isEmpty()) {
                            for (ClaimDefinition definition : requiredClaims) {
                                if (!claims.has(definition.getName())) {
                                    missedClaims.add(definition);
                                }
                            }
                        }

                        String claimsGatheringScriptName = policyService.getClaimsGatheringScriptName(script, context);
                        if (StringUtils.isNotBlank(claimsGatheringScriptName)) {
                            ticketAttributes.put(UmaConstants.GATHERING_ID, constructGatheringScriptNameValue(ticketAttributes.get(UmaConstants.GATHERING_ID), claimsGatheringScriptName));
                        } else {
                            log.debug("External 'getClaimsGatheringScriptName' script method return null or blank value, script: {}", script.getName());
                        }
                    } else {
                        log.error("Unable to load UMA script dn: '{}'", scriptDN);
                    }
                }
            } else {
                log.trace("No policies defined for scope: {}, scopeDn: {}", scope.getId(), scope.getDn());
            }
        }

        if (!missedClaims.isEmpty()) {
            ticketAttributes.put(UmaPermission.PCT, pct.getCode());
            String newTicket = permissionService.changeTicket(permissions, ticketAttributes);

            UmaNeedInfoResponse needInfoResponse = new UmaNeedInfoResponse();
            needInfoResponse.setTicket(newTicket);
            needInfoResponse.setError("need_info");
            needInfoResponse.setRedirectUser(buildClaimsGatheringRedirectUri(scriptMap.values(), client, newTicket));
            needInfoResponse.setRequiredClaims(missedClaims);

            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).entity(ServerUtil.asJsonSilently(needInfoResponse)).build());
        }

        return scriptMap;
    }

    private String constructGatheringScriptNameValue(String existingValue, String claimsGatheringScriptName) {
        if (StringUtils.isBlank(existingValue)) {
            return claimsGatheringScriptName;
        }
        return existingValue + " " + claimsGatheringScriptName;
    }

    private String buildClaimsGatheringRedirectUri(Collection<UmaAuthorizationContext> contexts, Client client, String newTicket) {
        StringBuilder queryParametersBuilder = new StringBuilder();
        for (UmaAuthorizationContext context : contexts) {
            queryParametersBuilder.append(context.getRedirectUserParameters().buildQueryString()).append("&");
        }
        String queryParameters = queryParametersBuilder.toString();
        queryParameters = StringUtils.removeEnd(queryParameters, "&");

        String result = appConfiguration.getBaseEndpoint() + "/uma/gather_claims";
        if (StringUtils.isNotBlank(queryParameters)) {
            result += "?" + queryParameters;
        }
        result += "&client_id=" + client.getClientId() + "&ticket=" + newTicket;
        return result;
    }
}

