package org.gluu.oxauth.uma.authorization;

import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.uma.persistence.UmaPermission;
import org.gluu.oxauth.model.uma.persistence.UmaResource;
import org.gluu.oxauth.service.AttributeService;
import org.gluu.oxauth.service.UserService;
import org.gluu.oxauth.uma.service.UmaPermissionService;
import org.gluu.oxauth.uma.service.UmaResourceService;
import org.gluu.oxauth.uma.service.UmaSessionService;
import org.oxauth.persistence.model.Scope;

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
