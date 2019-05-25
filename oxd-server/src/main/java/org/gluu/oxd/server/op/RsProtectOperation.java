package org.gluu.oxd.server.op;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.client.uma.UmaClientFactory;
import org.gluu.oxauth.client.uma.UmaResourceService;
import org.gluu.oxauth.model.uma.JsonLogicNodeParser;
import org.gluu.oxauth.model.uma.UmaMetadata;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.params.RsProtectParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.RsProtectResponse;
import org.gluu.oxd.rs.protect.Condition;
import org.gluu.oxd.rs.protect.RsResource;
import org.gluu.oxd.rs.protect.resteasy.Key;
import org.gluu.oxd.rs.protect.resteasy.PatProvider;
import org.gluu.oxd.rs.protect.resteasy.ResourceRegistrar;
import org.gluu.oxd.rs.protect.resteasy.ServiceProvider;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.model.UmaResource;
import org.gluu.oxd.server.service.Rp;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

public class RsProtectOperation extends BaseOperation<RsProtectParams> {

    private static final Logger LOG = LoggerFactory.getLogger(RsProtectOperation.class);

    protected RsProtectOperation(Command p_command, final Injector injector) {
        super(p_command, injector, RsProtectParams.class);
    }

    @Override
    public IOpResponse execute(final RsProtectParams params) throws Exception {
        validate(params);

        Rp site = getRp();

        PatProvider patProvider = new PatProvider() {
            @Override
            public String getPatToken() {
                return getUmaTokenService().getPat(params.getOxdId()).getToken();
            }

            @Override
            public void clearPat() {
                // do nothing
            }
        };

        ResourceRegistrar registrar = new ResourceRegistrar(patProvider, new ServiceProvider(site.getOpHost()));
        try {
            registrar.register(params.getResources());
        } catch (ClientResponseFailure e) {
            LOG.debug("Failed to register resource. Entity: " + e.getResponse().getEntity(String.class) + ", status: " + e.getResponse().getStatus(), e);
            if (e.getResponse().getStatus() == 400 || e.getResponse().getStatus() == 401) {
                LOG.debug("Try maybe PAT is lost on AS, force refresh PAT and re-try ...");
                getUmaTokenService().obtainPat(params.getOxdId()); // force to refresh PAT
                registrar.register(params.getResources());
            } else {
                throw e;
            }
        }

        persist(registrar, site);

        return new RsProtectResponse(site.getOxdId());
    }

    private void persist(ResourceRegistrar registrar, Rp site) throws IOException {
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

            site.getUmaProtectedResources().add(resource);
        }

        getRpService().update(site);
    }

    private void validate(RsProtectParams params) {
        if (params.getResources() == null || params.getResources().isEmpty()) {
            throw new HttpException(ErrorResponseCode.NO_UMA_RESOURCES_TO_PROTECT);
        }
        if (!org.gluu.oxd.rs.protect.ResourceValidator.isHttpMethodUniqueInPath(params.getResources())) {
            throw new HttpException(ErrorResponseCode.UMA_HTTP_METHOD_NOT_UNIQUE);
        }
        if (params.getResources() != null){
            for (RsResource resource : params.getResources()) {
                if (resource.getConditions() != null) {
                    for (Condition condition : resource.getConditions()) {
                        if (condition.getScopeExpression() != null) {
                            String json = condition.getScopeExpression().toString();
                            if (StringUtils.isNotBlank(json)) {
                                boolean nodeValid = JsonLogicNodeParser.isNodeValid(json);
                                LOG.trace("Scope expression validator - Valid: " + nodeValid + ", expression: " + json);
                                if (!nodeValid) {
                                    throw new HttpException(ErrorResponseCode.UMA_FAILED_TO_VALIDATE_SCOPE_EXPRESSION);
                                }
                            }
                        }
                    }
                }
            }
        }

        Rp rp = getRp();
        List<UmaResource> existingUmaResources = rp.getUmaProtectedResources();
        if (existingUmaResources != null && !existingUmaResources.isEmpty()) {
            if (params.getOverwrite() == null || !params.getOverwrite()) {
                throw new HttpException(ErrorResponseCode.UMA_PROTECTION_FAILED_BECAUSE_RESOURCES_ALREADY_EXISTS);
            } else {
                // remove existing resources, overwrite=true
                UmaMetadata discovery = getDiscoveryService().getUmaDiscoveryByOxdId(params.getOxdId());
                UmaResourceService resourceService = UmaClientFactory.instance().createResourceService(discovery, getHttpService().getClientExecutor());
                String pat = getUmaTokenService().getPat(params.getOxdId()).getToken();

                for (UmaResource resource : existingUmaResources) {
                    LOG.trace("Removing existing resource " + resource.getId() + " ...");
                    resourceService.deleteResource("Bearer " + pat, resource.getId());
                    LOG.trace("Removed existing resource " + resource.getId() + ".");
                }
                rp.getUmaProtectedResources().clear();
                getRpService().updateSilently(rp);
            }
        }
    }
}
