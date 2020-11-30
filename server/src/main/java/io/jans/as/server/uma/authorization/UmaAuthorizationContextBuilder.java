/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.authorization;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.uma.service.UmaPermissionService;
import io.jans.as.server.uma.service.UmaResourceService;
import io.jans.as.server.uma.service.UmaSessionService;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author yuriyz on 06/06/2017.
 */
public class UmaAuthorizationContextBuilder {

    private final AttributeService attributeService;
    private final UmaResourceService resourceService;
    private final List<UmaPermission> permissions;
    private final Map<Scope, Boolean> scopes;
    private final Claims claims;
    private final HttpServletRequest httpRequest;
    private final AppConfiguration configuration;
    private final UmaSessionService sessionService;
    private final UserService userService;
    private final UmaPermissionService permissionService;
    private final Client client;

    public UmaAuthorizationContextBuilder(AppConfiguration configuration, AttributeService attributeService, UmaResourceService resourceService,
                                          List<UmaPermission> permissions, Map<Scope, Boolean> scopes,
                                          Claims claims, HttpServletRequest httpRequest,
                                          UmaSessionService sessionService, UserService userService, UmaPermissionService permissionService, Client client) {
        this.configuration = configuration;
        this.attributeService = attributeService;
        this.resourceService = resourceService;
        this.permissions = permissions;
        this.client = client;
        this.scopes = scopes;
        this.claims = claims;
        this.httpRequest = httpRequest;
        this.sessionService = sessionService;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    public UmaAuthorizationContext build(CustomScriptConfiguration script) {
        return new UmaAuthorizationContext(configuration, attributeService, scopes, getResources(), claims,
                script.getCustomScript().getDn(), httpRequest, script.getConfigurationAttributes(),
                sessionService, userService, permissionService, client);
    }

    public Set<String> getResourceIds() {
        Set<String> result = new HashSet<String>();
        for (UmaPermission permission : permissions) {
            result.add(permission.getResourceId());
        }
        return result;
    }

    public Set<UmaResource> getResources() {
        return resourceService.getResources(getResourceIds());
    }
}
