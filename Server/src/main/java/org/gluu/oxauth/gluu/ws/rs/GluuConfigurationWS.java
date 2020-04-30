/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.gluu.ws.rs;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.gluu.model.GluuAttribute;
import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.gluu.GluuConfiguration;
import org.gluu.oxauth.model.gluu.GluuErrorResponseType;
import org.gluu.oxauth.service.AttributeService;
import org.gluu.oxauth.service.ScopeService;
import org.gluu.oxauth.service.external.ExternalAuthenticationService;
import org.gluu.oxauth.util.ServerUtil;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Created by eugeniuparvan on 8/5/16.
 */
@Path("/.well-known/gluu-configuration")
public class GluuConfigurationWS {

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
        } catch (Throwable ex) {
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
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        try {
            for (Scope scope : scopeService.getAllScopesList()) {
                final Set<String> claimsList = new HashSet<String>();
                result.put(scope.getId(), claimsList);

                final List<String> claimIdList = scope.getOxAuthClaims();
                if (claimIdList != null && !claimIdList.isEmpty()) {
                    for (String claimDn : claimIdList) {
                        final GluuAttribute attribute = attributeService.getAttributeByDn(claimDn);
                        final String claimName = attribute.getOxAuthClaimName();
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
