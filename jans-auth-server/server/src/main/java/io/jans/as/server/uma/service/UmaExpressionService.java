/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.service;

import com.google.common.collect.Lists;
import io.jans.as.model.config.Constants;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.uma.JsonLogic;
import io.jans.as.model.uma.JsonLogicNode;
import io.jans.as.model.uma.JsonLogicNodeParser;
import io.jans.as.model.uma.UmaErrorResponseType;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.model.util.Util;
import io.jans.as.server.service.external.ExternalUmaRptPolicyService;
import io.jans.as.server.uma.authorization.UmaAuthorizationContext;
import io.jans.as.server.uma.authorization.UmaScriptByScope;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.BooleanUtils.isFalse;

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

    private static Map<String, String> scopeIdToDnMap(Map<UmaScriptByScope, UmaAuthorizationContext> scriptMap, List<String> scriptDNs) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<UmaScriptByScope, UmaAuthorizationContext> entry : scriptMap.entrySet()) {
            if (scriptDNs.contains(entry.getKey().getScope().getDn())) {
                result.put(entry.getKey().getScope().getId(), entry.getKey().getScope().getDn());
            }
        }
        return result;
    }

    private static Map<UmaScriptByScope, UmaAuthorizationContext> filterByScopeDns(Map<UmaScriptByScope, UmaAuthorizationContext> scriptMap, List<String> scopeDNs) {
        Map<UmaScriptByScope, UmaAuthorizationContext> result = new HashMap<>();
        for (Map.Entry<UmaScriptByScope, UmaAuthorizationContext> entry : scriptMap.entrySet()) {
            if (scopeDNs.contains(entry.getKey().getScope().getDn())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

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
                    throw errorResponseFactory.createWebApplicationException(Response.Status.FORBIDDEN, UmaErrorResponseType.FORBIDDEN_BY_POLICY, "Regular evaluation returns false, access FORBIDDEN.");
                }
            }
        }
    }

    private boolean evaluateByScopes(Map<UmaScriptByScope, UmaAuthorizationContext> scriptMap) {
        for (Map.Entry<UmaScriptByScope, UmaAuthorizationContext> entry : scriptMap.entrySet()) {
            final boolean result = policyService.authorize(entry.getKey().getScript(), entry.getValue());
            log.trace("Policy script inum: '{}' result: '{}'", entry.getKey().getScript().getInum(), result);
            if (!result) {
                log.trace("Stop authorization scriptMap execution, current script returns false, script inum: {}, scope: {}", entry.getKey().getScript().getInum(), entry.getKey().getScope());
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
                if (evaluateScopeExpressionInternal(scriptMap, permission, resource, scopeExpression, node, dataScopes, scopeIdToDnMap)) {
                    return; // expression returned true
                }
            } else {
                log.error("Scope size in JsonLogic object 'data' and in permission differs which is forbidden. Node data: {}, permissionDns: {}, result scopeIds: {}",
                        node, permission.getScopeDns(), scopeIdToDnMap);
                throw errorResponseFactory.createWebApplicationException(Response.Status.FORBIDDEN, UmaErrorResponseType.FORBIDDEN_BY_POLICY, "Scope size in JsonLogic object 'data' and in permission differs which is forbidden.");
            }
        } else {
            log.error("Failed to parse JsonLogic object, invalid expression: {}", scopeExpression);
            throw errorResponseFactory.createWebApplicationException(Response.Status.FORBIDDEN, UmaErrorResponseType.FORBIDDEN_BY_POLICY, "Failed to parse JsonLogic object, invalid expression: " + scopeExpression);
        }

        throw errorResponseFactory.createWebApplicationException(Response.Status.FORBIDDEN, UmaErrorResponseType.FORBIDDEN_BY_POLICY, Constants.UNKNOWN);
    }

    private boolean evaluateScopeExpressionInternal(Map<UmaScriptByScope, UmaAuthorizationContext> scriptMap, UmaPermission permission, UmaResource resource, String scopeExpression, JsonLogicNode node, List<String> dataScopes, Map<String, String> scopeIdToDnMap) {
        try {
            List<Boolean> evaluatedResults = new ArrayList<>();
            for (String scopeId : dataScopes) {
                log.trace("Evaluating scope result for scope: {}...", scopeId);
                boolean b = evaluateByScopes(filterByScopeDns(scriptMap, Lists.newArrayList(scopeIdToDnMap.get(scopeId))));
                log.trace("Evaluated scope result: {}, scope: {}", b, scopeId);
                evaluatedResults.add(b);
            }

            String rule = node.getRule().toString();
            final boolean result;
            if (evaluatedResults.isEmpty()) {
                result = JsonLogic.apply(rule);
            } else {
                result = JsonLogic.apply(rule, Util.asJsonSilently(evaluatedResults));
            }

            if (log.isTraceEnabled()) {
                log.trace("JsonLogic evaluation result: {}, rule: {}, data: {}", result, rule, Util.asJsonSilently(evaluatedResults));
            }
            if (result) {
                // access granted at this point but we have to remove scopes from permissions for which we got 'false' result
                removeFalseScopesFromPermission(permission, dataScopes, scopeIdToDnMap, evaluatedResults);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to evaluate jsonlogic expression. Expression: " + scopeExpression + ", resourceDn: " + resource.getDn(), e);
            throw errorResponseFactory.createWebApplicationException(Response.Status.FORBIDDEN, UmaErrorResponseType.FORBIDDEN_BY_POLICY, "Failed to evaluate jsonlogic expression.");
        }
        return false;
    }

    private void removeFalseScopesFromPermission(UmaPermission permission, List<String> dataScopes, Map<String, String> scopeIdToDnMap, List<Boolean> evaluatedResults) {
        if (!evaluatedResults.isEmpty() && permission.getScopeDns() != null) {

            List<String> newPermissionScopes = new ArrayList<>(permission.getScopeDns());

            for (int i = 0; i < evaluatedResults.size(); i++) {
                if (isFalse(evaluatedResults.get(i))) {
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
}
