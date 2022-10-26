/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.jans.ws.rs;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.gluu.GluuConfiguration;
import io.jans.as.model.gluu.GluuErrorResponseType;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.util.ServerUtil;
import io.jans.model.GluuAttribute;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by eugeniuparvan on 8/5/16.
 */
@Path("/.well-known/jans-configuration")
public class JansConfigurationWS {

    @Inject
    private Logger log;

    @Inject
    private ScopeService scopeService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ExternalAuthenticationService externalAuthenticationService;

    @GET
    @Produces({"application/json"})
    public Response getConfiguration() {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.JANS_CONFIGURATION);
        try {
            final GluuConfiguration conf = new GluuConfiguration();

            conf.setIdGenerationEndpoint(appConfiguration.getIdGenerationEndpoint());
            conf.setIntrospectionEndpoint(appConfiguration.getIntrospectionEndpoint());
            conf.setAuthLevelMapping(createAuthLevelMapping());
            conf.setScopeToClaimsMapping(createScopeToClaimsMapping());

            // convert manually to avoid possible conflicts between resteasy
            // providers, e.g. jettison, jackson
            final String entity = ServerUtil.asPrettyJson(conf);
            log.trace("Gluu configuration: {}", entity);

            return Response.ok(entity).build();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getErrorResponse(GluuErrorResponseType.SERVER_ERROR)).build());
        }
    }

    public Map<Integer, Set<String>> createAuthLevelMapping() {
        Map<Integer, Set<String>> map = Maps.newHashMap();
        try {
            for (CustomScriptConfiguration script : externalAuthenticationService.getCustomScriptConfigurationsMap()) {
                String acr = script.getName();
                int level = script.getLevel();

                Set<String> acrs = map.get(level);
                if (acrs == null) {
                    acrs = Sets.newHashSet();
                    map.put(level, acrs);
                }
                acrs.add(acr);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return map;
    }

    private Map<String, Set<String>> createScopeToClaimsMapping() {
        Map<String, Set<String>> result = new HashMap<>();
        try {
            for (Scope scope : scopeService.getAllScopesList()) {
                final Set<String> claimsList = new HashSet<>();
                result.put(scope.getId(), claimsList);

                final List<String> claimIdList = scope.getClaims();
                if (claimIdList != null && !claimIdList.isEmpty()) {
                    for (String claimDn : claimIdList) {
                        final GluuAttribute attribute = attributeService.getAttributeByDn(claimDn);
                        final String claimName = attribute.getClaimName();
                        if (StringUtils.isNotBlank(claimName)) {
                            claimsList.add(claimName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }
}
