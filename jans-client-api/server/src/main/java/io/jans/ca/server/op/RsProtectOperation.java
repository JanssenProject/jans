package io.jans.ca.server.op;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.jans.as.client.uma.UmaClientFactory;
import io.jans.as.client.uma.UmaResourceService;
import io.jans.as.model.uma.JsonLogic;
import io.jans.as.model.uma.JsonLogicNode;
import io.jans.as.model.uma.JsonLogicNodeParser;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.util.Util;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.RsProtectParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.RsProtectResponse;
import io.jans.ca.rs.protect.Condition;
import io.jans.ca.rs.protect.ResourceValidator;
import io.jans.ca.rs.protect.RsResource;
import io.jans.ca.rs.protect.resteasy.Key;
import io.jans.ca.rs.protect.resteasy.PatProvider;
import io.jans.ca.rs.protect.resteasy.ResourceRegistrar;
import io.jans.ca.rs.protect.resteasy.ServiceProvider;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.configuration.model.UmaResource;
import io.jans.ca.server.service.DiscoveryService;
import io.jans.ca.server.service.RpService;
import io.jans.ca.server.service.UmaTokenService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequestScoped
@Named
public class RsProtectOperation extends BaseOperation<RsProtectParams> {

    private static final Logger LOG = LoggerFactory.getLogger(RsProtectOperation.class);

    @Inject
    RpService rpService;
    @Inject
    UmaTokenService umaTokenService;
    @Inject
    OpClientFactoryImpl opClientFactory;
    @Inject
    DiscoveryService discoveryService;

    @Override
    public IOpResponse execute(final RsProtectParams params, HttpServletRequest httpServletRequest) throws Exception {
        validate(params);

        Rp rp = getRp(params);

        PatProvider patProvider = new PatProvider() {
            @Override
            public String getPatToken() {
                return umaTokenService.getPat(params.getRpId()).getToken();
            }

            @Override
            public void clearPat() {
                // do nothing
            }
        };

        ResourceRegistrar registrar = opClientFactory.createResourceRegistrar(patProvider, new ServiceProvider(rp.getOpHost()));
        try {
            registrar.register(params.getResources());
        } catch (ClientErrorException e) {
            LOG.debug("Failed to register resource. Entity: " + e.getResponse().readEntity(String.class) + ", status: " + e.getResponse().getStatus(), e);
            if (e.getResponse().getStatus() == 400 || e.getResponse().getStatus() == 401) {
                LOG.debug("Try maybe PAT is lost on AS, force refresh PAT and re-try ...");
                umaTokenService.obtainPat(params.getRpId()); // force to refresh PAT
                registrar.register(params.getResources());
            } else {
                throw e;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }

        persist(registrar, rp);

        return new RsProtectResponse(rp.getRpId());
    }

    @Override
    public Class<RsProtectParams> getParameterClass() {
        return RsProtectParams.class;
    }

    @Override
    public String getReturnType() {
        return MediaType.APPLICATION_JSON;
    }

    private void persist(ResourceRegistrar registrar, Rp rp) throws IOException {
        Map<Key, RsResource> resourceMapCopy = registrar.getResourceMapCopy();

        for (Map.Entry<Key, String> entry : registrar.getIdMapCopy().entrySet()) {
            UmaResource resource = new UmaResource();
            resource.setId(entry.getValue());
            resource.setPath(entry.getKey().getPath());
            resource.setHttpMethods(entry.getKey().getHttpMethods());
            Set<String> scopes = Sets.newHashSet();
            Set<String> scopesForTicket = Sets.newHashSet();
            Set<String> scopeExpressions = Sets.newHashSet();

            RsResource rsResource = resourceMapCopy.get(entry.getKey());

            for (String httpMethod : entry.getKey().getHttpMethods()) {

                List<String> rsScopes = rsResource.scopes(httpMethod);
                if (rsScopes != null) {
                    scopes.addAll(rsScopes);
                }
                scopesForTicket.addAll(rsResource.getScopesForTicket(httpMethod));

                JsonNode scopeExpression = rsResource.getScopeExpression(httpMethod);
                if (scopeExpression != null) {
                    scopeExpressions.add(scopeExpression.toString());
                }
            }

            resource.setScopes(Lists.newArrayList(scopes));
            resource.setTicketScopes(Lists.newArrayList(scopesForTicket));
            resource.setScopeExpressions(Lists.newArrayList(scopeExpressions));

            if (rsResource.getIat() != null && rsResource.getIat() > 0) {
                resource.setIat(rsResource.getIat());
            }

            if (rsResource.getExp() != null && rsResource.getExp() > 0) {
                resource.setExp(rsResource.getExp());
            }
            rp.getUmaProtectedResources().add(resource);
        }

        rpService.update(rp);
    }

    private void validate(RsProtectParams params) {
        if (params.getResources() == null || params.getResources().isEmpty()) {
            throw new HttpException(ErrorResponseCode.NO_UMA_RESOURCES_TO_PROTECT);
        }
        if (!ResourceValidator.isHttpMethodUniqueInPath(params.getResources())) {
            throw new HttpException(ErrorResponseCode.UMA_HTTP_METHOD_NOT_UNIQUE);
        }
        if (params.getResources() != null) {
            for (RsResource resource : params.getResources()) {
                if (resource.getConditions() != null) {
                    for (Condition condition : resource.getConditions()) {
                        if (condition.getScopeExpression() != null) {
                            String json = condition.getScopeExpression().toString();
                            if (StringUtils.isNotBlank(json) && !json.equalsIgnoreCase("null")) {
                                boolean nodeValid = JsonLogicNodeParser.isNodeValid(json);
                                LOG.trace("Scope expression validator - Valid: " + nodeValid + ", expression: " + json);
                                if (!nodeValid) {
                                    throw new HttpException(ErrorResponseCode.UMA_FAILED_TO_VALIDATE_SCOPE_EXPRESSION);
                                }
                                validateScopeExpression(json);
                            }
                        }
                    }
                }
            }
        }

        Rp rp = getRp(params);
        List<UmaResource> existingUmaResources = rp.getUmaProtectedResources();
        if (existingUmaResources != null && !existingUmaResources.isEmpty()) {
            if (params.getOverwrite() == null || !params.getOverwrite()) {
                throw new HttpException(ErrorResponseCode.UMA_PROTECTION_FAILED_BECAUSE_RESOURCES_ALREADY_EXISTS);
            } else {
                // remove existing resources, overwrite=true
                UmaMetadata discovery = discoveryService.getUmaDiscoveryByRpId(params.getRpId());
                String pat = umaTokenService.getPat(params.getRpId()).getToken();
                UmaResourceService resourceService = UmaClientFactory.instance().createResourceService(discovery, getHttpService().getClientEngine());
                for (UmaResource resource : existingUmaResources) {

                    LOG.trace("Removing existing resource " + resource.getId() + " ...");
                    resourceService.deleteResource("Bearer " + pat, resource.getId());
                    LOG.trace("Removed existing resource " + resource.getId() + ".");
                }
                rp.getUmaProtectedResources().clear();
                rpService.updateSilently(rp);
            }
        }
    }

    public static void validateScopeExpression(String scopeExpression) {
        JsonLogicNode jsonLogicNode = JsonLogicNodeParser.parseNode(scopeExpression);
        try {
            Object scope = JsonLogic.applyObject(jsonLogicNode.getRule().toString(), Util.asJsonSilently(jsonLogicNode.getData()));
            if (scope == null || !jsonLogicNode.getData().contains(scope.toString())) {
                throw new HttpException(ErrorResponseCode.UMA_FAILED_TO_VALIDATE_SCOPE_EXPRESSION);
            }
        } catch (Exception e) {
            LOG.trace("The scope expression is invalid. Please check the documentation and make sure it is a valid JsonLogic expression.", e);
            throw new HttpException(ErrorResponseCode.UMA_FAILED_TO_VALIDATE_SCOPE_EXPRESSION);
        }
    }
}
