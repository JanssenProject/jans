package org.xdi.oxauth.uma.service;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.model.uma.ClaimDefinition;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.UmaScopeDescription;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.uma.authorization.*;
import org.xdi.oxauth.util.ServerUtil;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

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
    private AttributeService attributeService;

    @Inject
    private UmaResourceService resourceService;

    @Inject
    private ExternalUmaRptPolicyService policyService;

    public Map<CustomScriptConfiguration, UmaAuthorizationContext> checkNeedsInfo(Claims claims, Map<UmaScopeDescription, Boolean> requestedScopes,
                                                                                  List<UmaPermission> permissions, UmaPCT pct, HttpServletRequest httpRequest) {
        Set<String> scriptDNs = getScriptDNs(new ArrayList<UmaScopeDescription>(requestedScopes.keySet()));

        Map<CustomScriptConfiguration, UmaAuthorizationContext> scriptMap = new HashMap<CustomScriptConfiguration, UmaAuthorizationContext>();
        Map<String, String> ticketAttributes = new HashMap<String, String>();

        List<ClaimDefinition> missedClaims = new ArrayList<ClaimDefinition>();

        UmaAuthorizationContextBuilder contextBuilder = new UmaAuthorizationContextBuilder(attributeService, resourceService, permissions, requestedScopes, claims, httpRequest);

        for (String scriptDN : scriptDNs) {
            CustomScriptConfiguration script = policyService.getScriptByDn(scriptDN);

            if (script != null) {
                UmaAuthorizationContext context = contextBuilder.build(script);
                scriptMap.put(script, context);

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
                    context.addRedirectUserParam(UmaConstants.GATHERING_ID, claimsGatheringScriptName);
                    ticketAttributes.put(UmaConstants.GATHERING_ID, constructGatheringValue(ticketAttributes.get(UmaConstants.GATHERING_ID), claimsGatheringScriptName));
                } else {
                    log.error("External 'getClaimsGatheringScriptName' script method return null or blank value, script: " + script.getName());
                }
            } else {
                log.error("Unable to load UMA script dn: '{}'", scriptDN);
            }
        }

        if (!missedClaims.isEmpty()) {
            ticketAttributes.put(UmaPermission.PCT_DN, pct.getDn());
            String newTicket = permissionService.changeTicket(permissions, ticketAttributes);

            UmaNeedInfoResponse needInfoResponse = new UmaNeedInfoResponse();
            needInfoResponse.setTicket(newTicket);
            needInfoResponse.setError("need_info");
            needInfoResponse.setRedirectUser(buildClaimsGatheringRedirectUri(scriptMap.values()));
            needInfoResponse.setRequiredClaims(missedClaims);

            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).entity(ServerUtil.asJsonSilently(needInfoResponse)).build());
        }

        return scriptMap;
    }

    private String constructGatheringValue(String existingValue, String claimsGatheringScriptName) {
        if (StringUtils.isBlank(existingValue)) {
            return claimsGatheringScriptName;
        }
        return existingValue + " " + claimsGatheringScriptName;
    }

    private String buildClaimsGatheringRedirectUri(Collection<UmaAuthorizationContext> contexts) {
        String queryParameters = "";

        for (UmaAuthorizationContext context : contexts) {
            Map<String, Set<String>> paramMap = context.getRedirectUserParam();
            for (Map.Entry<String, Set<String>> param : paramMap.entrySet()) {
                Set<String> values = param.getValue();
                if (StringUtils.isNotBlank(param.getKey()) && values != null && !values.isEmpty()) {
                    for (String value : values) {
                        if (StringUtils.isNotBlank(value)) {
                            try {
                                queryParameters += param.getKey() + "=" + URLEncoder.encode(value, "UTF-8") + "&";
                            } catch (UnsupportedEncodingException e) {
                                log.error("Failed to encode value: " + value + ", scriptId: " + context.getScriptDn(), e);
                            }
                        }
                    }
                }
            }
        }
        StringUtils.removeEnd(queryParameters, "&");

        String result = appConfiguration.getBaseEndpoint() + "/uma/gather_claims" + queryParameters;
        if (StringUtils.isNotBlank(queryParameters)) {
            result += "?" + queryParameters;
        }
        return result;
    }

    public static Set<String> getScriptDNs(List<UmaScopeDescription> scopes) {
        HashSet<String> result = new HashSet<String>();

        for (UmaScopeDescription scope : scopes) {
            List<String> authorizationPolicies = scope.getAuthorizationPolicies();
            if (authorizationPolicies != null) {
                result.addAll(authorizationPolicies);
            }
        }

        return result;
    }
}

