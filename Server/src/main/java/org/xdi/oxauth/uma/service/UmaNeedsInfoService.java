package org.xdi.oxauth.uma.service;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.model.uma.ClaimDefinition;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.UmaScopeDescription;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.uma.authorization.Claims;
import org.xdi.oxauth.uma.authorization.UmaAuthorizationContext;
import org.xdi.oxauth.uma.authorization.UmaAuthorizationContextBuilder;
import org.xdi.oxauth.uma.authorization.UmaNeedInfoResponse;
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

    public static final String PARAM_GATHERING_ID = "gathering_id";
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
                                                                                  List<UmaPermission> permissions, HttpServletRequest httpRequest) {
        Set<String> scriptDNs = getScriptDNs(new ArrayList<UmaScopeDescription>(requestedScopes.keySet()));

        Map<CustomScriptConfiguration, UmaAuthorizationContext> scriptMap = new HashMap<CustomScriptConfiguration, UmaAuthorizationContext>();

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
                    context.addRedirectUserParam(PARAM_GATHERING_ID, claimsGatheringScriptName);
                } else {
                    log.error("External 'getClaimsGatheringScriptName' script method return null or blank value, script: " + script.getName());
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
            needInfoResponse.setRedirectUser(buildClaimsGatheringRedirectUri(scriptMap.values()));
            needInfoResponse.setRequiredClaims(missedClaims);

            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).entity(ServerUtil.asJsonSilently(needInfoResponse)).build());
        }

        return scriptMap;
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

    private String generateNewTicket(List<UmaPermission> permissions) {
        if (permissions.isEmpty()) {
            return permissionService.generateNewTicket();
        } else {
            return permissionService.changeTicket(permissions);
        }
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

