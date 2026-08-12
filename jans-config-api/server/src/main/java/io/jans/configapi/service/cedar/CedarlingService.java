package io.jans.configapi.service.cedar;

import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.model.configuration.AssetMgtConfiguration;
import io.jans.core.cedarling.model.OpenIDConnectConfig;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.container.ResourceInfo;
import io.jans.core.cedarling.service.CedarlingAuthorizationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;

@ApplicationScoped
@Named
public class CedarlingService {

    @Inject
    Logger logger;

    @Inject
    ApiAppConfiguration apiAppConfiguration;

    @Inject
    CedarlingAuthorizationService cedarlingAuthorizationService;

    @Produces
    @ApplicationScoped
    public OpenIDConnectConfig getOpenIDConnectConfig() {
        OpenIDConnectConfig openIDConnectConfig = new OpenIDConnectConfig();
        openIDConnectConfig.setIssuer(apiAppConfiguration.getAuthIssuerUrl());
        return openIDConnectConfig;
    }

    public boolean authorize(String token, String issuer, ResourceInfo resourceInfo, String method, String path) {

        Map<String, String> tokens = new HashMap<>();
        tokens.put(CedarlingAuthorizationService.CEDARLING_JANS_ACCESS_TOKEN, token);

        Map<String, Object> context = new HashMap<>();
        java.util.List<String> actions = java.util.Arrays.stream(resourceInfo.getResourceMethod().getAnnotations())
                .map(a -> a.annotationType().getSimpleName()).collect(Collectors.toList());

        Map<String, Object> resource = new HashMap<>();
        resource.put("url", path);

        Map<String, Object> mapping = new HashMap<>();
        mapping.put("method", method);
        mapping.put("action", actions);
        mapping.put("issuer", issuer);

        resource.put("cedar_entity_mapping", mapping);

        return cedarlingAuthorizationService.authorize(tokens, method, resource, context);
    }
}
