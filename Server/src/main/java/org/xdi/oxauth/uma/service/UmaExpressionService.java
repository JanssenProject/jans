package org.xdi.oxauth.uma.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;

import com.google.common.collect.Lists;

import org.gluu.oxauth.model.uma.JsonLogic;
import org.gluu.oxauth.model.uma.JsonLogicNode;
import org.gluu.oxauth.model.uma.JsonLogicNodeParser;
import org.gluu.oxauth.model.uma.UmaErrorResponseType;
import org.gluu.oxauth.model.uma.persistence.UmaPermission;
import org.gluu.oxauth.model.uma.persistence.UmaResource;
import org.gluu.oxauth.model.util.Util;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.service.external.ExternalUmaRptPolicyService;
import org.xdi.oxauth.uma.authorization.UmaAuthorizationContext;
import org.xdi.oxauth.uma.authorization.UmaScriptByScope;
import org.xdi.oxauth.uma.authorization.UmaWebException;

/**
 * @author yuriyz
 */
@Stateless
@Named
public class UmaExpressionService {

    @Inject
    private Logger log;
    @Inject
    private ExternalUmaRptPolicyService policyService;
    @Inject
    private ErrorResponseFactory errorResponseFactory;
    @Inject
    private UmaResourceService resourceService;
    @Inject

    private UmaPermissionService permissionService;

    public boolean isExpressionValid(String expression) {
        return JsonLogicNodeParser.isNodeValid(expression);
    }

    public void evaluate(Map<UmaScriptByScope, UmaAuthorizationContext> scriptMap, List<UmaPermission> permissions) {
        for (UmaPermission permission : permissions) {
            UmaResource resource = resourceService.getResourceById(permission.getResourceId());
            if (StringHelper.isNotEmpty(resource.getScopeExpression())) {
                evaluateScopeExpression(scriptMap, permission, resource);
            } else {
                if (!evaluateByScopes(filterByScopeDns(scriptMap, permission.getScopeDns()))) {
                    log.trace("Regular evaluation returns false, access FORBIDDEN.");
                    throw new UmaWebException(Response.Status.FORBIDDEN, errorResponseFactory, UmaErrorResponseType.FORBIDDEN_BY_POLICY);
                }
            }
        }
    }

    private boolean evaluateByScopes(Map<UmaScriptByScope, UmaAuthorizationContext> scriptMap) {
        for (Map.Entry<UmaScriptByScope, UmaAuthorizationContext> entry : scriptMap.entrySet()) {
            final boolean result = policyService.authorize(entry.getKey().getScript(), entry.getValue());
            log.trace("Policy script inum: '{}' result: '{}'", entry.getKey().getScript().getInum(), result);
            if (!result) {
                log.trace("Stop authorization scriptMap execution, current script returns false, script inum: " + entry.getKey().getScript().getInum() + ", scope: " + entry.getKey().getScope());
                return false;
            }
        }
        return true;
    }

    private void evaluateScopeExpression(Map<UmaScriptByScope, UmaAuthorizationContext> scriptMap, UmaPermission permission, UmaResource resource) {
        String scopeExpression = resource.getScopeExpression();
        JsonLogicNode node = JsonLogicNodeParser.parseNode(scopeExpression);
        if (node != null) {
            log.trace("Evaluating scope expression ...");

            // validate scopes, all must be present
            List<String> dataScopes = node.getDataCopy();
            Map<String, String> scopeIdToDnMap = scopeIdToDnMap(scriptMap, permission.getScopeDns());
            if (dataScopes.size() == scopeIdToDnMap.size()) {
                try {
                    List<Boolean> evaluatedResults = new ArrayList<Boolean>();
                    for (String scopeId : dataScopes) {
                        log.trace("Evaluating scope result for scope: " + scopeId + " ...");
                        boolean b = evaluateByScopes(filterByScopeDns(scriptMap, Lists.newArrayList(scopeIdToDnMap.get(scopeId))));
                        log.trace("Evaluated scope result: " + b + ", scope: " + scopeId);
                        evaluatedResults.add(b);
                    }

                    String rule = node.getRule().toString();
                    final boolean result;
                    if (evaluatedResults.isEmpty()) {
                        result = JsonLogic.apply(rule);
                    } else {
                        result = JsonLogic.apply(rule, Util.asJsonSilently(evaluatedResults));
                    }

                    log.trace("JsonLogic evaluation result: " + result + ", rule: " + rule + ", data:" + Util.asJsonSilently(evaluatedResults));
                    if (result) {
                        // access granted at this point but we have to remove scopes from permissions for which we got 'false' result
                        removeFalseScopesFromPermission(permission, dataScopes, scopeIdToDnMap, evaluatedResults);
                        return; // expression returned true;
                    }
                } catch (Exception e) {
                    log.error("Failed to evaluate jsonlogic expression. Expression: " + scopeExpression + ", resourceDn: " + resource.getDn());
                }
            } else {
                log.error("Scope size in JsonLogic object 'data' and in permission differs which is forbidden. Node data: " + node +
                        ", permissionDns: " + permission.getScopeDns() + ", result scopeIds: " + scopeIdToDnMap);
            }
        } else {
            log.error("Failed to parse JsonLogic object, invalid expression: " + scopeExpression);
        }
        throw new UmaWebException(Response.Status.FORBIDDEN, errorResponseFactory, UmaErrorResponseType.FORBIDDEN_BY_POLICY);
    }

    private void removeFalseScopesFromPermission(UmaPermission permission, List<String> dataScopes, Map<String, String> scopeIdToDnMap, List<Boolean> evaluatedResults) {
        if (!evaluatedResults.isEmpty() && permission.getScopeDns() != null) {

            List<String> newPermissionScopes = new ArrayList<String>(permission.getScopeDns());

            for (int i = 0; i < evaluatedResults.size(); i++) {
                if (!evaluatedResults.get(i)) {
                    String dnToRemove = scopeIdToDnMap.get(dataScopes.get(i));
                    newPermissionScopes.remove(dnToRemove);
                }
            }

            if (newPermissionScopes.size() < permission.getScopeDns().size()) {
                permission.setScopeDns(newPermissionScopes);

                permissionService.mergeSilently(permission);
            }
        }
    }

    private static Map<String, String> scopeIdToDnMap(Map<UmaScriptByScope, UmaAuthorizationContext> scriptMap, List<String> scriptDNs) {
        Map<String, String> result = new HashMap<String, String>();
        for (Map.Entry<UmaScriptByScope, UmaAuthorizationContext> entry : scriptMap.entrySet()) {
            if (scriptDNs.contains(entry.getKey().getScope().getDn())) {
                result.put(entry.getKey().getScope().getId(), entry.getKey().getScope().getDn());
            }
        }
        return result;
    }

    private static Map<UmaScriptByScope, UmaAuthorizationContext> filterByScopeDns(Map<UmaScriptByScope, UmaAuthorizationContext> scriptMap, List<String> scopeDNs) {
        Map<UmaScriptByScope, UmaAuthorizationContext> result = new HashMap<UmaScriptByScope, UmaAuthorizationContext>();
        for (Map.Entry<UmaScriptByScope, UmaAuthorizationContext> entry : scriptMap.entrySet()) {
            if (scopeDNs.contains(entry.getKey().getScope().getDn())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private static Map<UmaScriptByScope, UmaAuthorizationContext> filterByScopeId(Map<UmaScriptByScope, UmaAuthorizationContext> scriptMap, String scopeId) {
        Map<UmaScriptByScope, UmaAuthorizationContext> result = new HashMap<UmaScriptByScope, UmaAuthorizationContext>();
        for (Map.Entry<UmaScriptByScope, UmaAuthorizationContext> entry : scriptMap.entrySet()) {
            if (entry.getKey().getScope().getId().equals(scopeId)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
