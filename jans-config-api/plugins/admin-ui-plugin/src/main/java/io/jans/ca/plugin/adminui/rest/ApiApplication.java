/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.plugin.adminui.rest;

import io.jans.ca.plugin.adminui.rest.auth.OAuth2Resource;
import io.jans.ca.plugin.adminui.rest.user.UserManagementResource;
import io.jans.ca.plugin.adminui.rest.license.LicenseResource;
import io.jans.ca.plugin.adminui.rest.logging.AuditLoggerResource;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.tags.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.servers.*;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Mougang T.Gasmyr
 *
 */
@ApplicationPath("/")
@OpenAPIDefinition(info = @Info(title = "Jans Config API - Admin-UI", version = "1.0.0", contact = @Contact(name = "Gluu Support", url = "https://support.gluu.org", email = "xxx@gluu.org"),

license = @License(name = "Apache 2.0", url = "https://github.com/JanssenProject/jans/blob/main/LICENSE")),

tags = { @Tag(name = "Admin UI - Role"),
        @Tag(name = "Admin UI - Permission"),
        @Tag(name = "Admin UI - Role-Permissions Mapping"),
        @Tag(name = "Admin UI - License") },

servers = { @Server(url = "https://jans.io/", description = "The Jans server") })

@SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(clientCredentials = @OAuthFlow(tokenUrl = "https://{op-hostname}/.../token", scopes = {
@OAuthScope(name = "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.read", description = "View admin user role related information"),
@OAuthScope(name = "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.write", description = "Manage admin user role related information"),
@OAuthScope(name = "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.read", description = "View admin permission related information"),
@OAuthScope(name = "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.write", description = "Manage admin permission related information"),
@OAuthScope(name = "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.readonly", description = "View role-permission mapping related information"),
@OAuthScope(name = "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.write", description = "Manage role-permission mapping related information"),
@OAuthScope(name = "https://jans.io/oauth/jans-auth-server/config/adminui/license.readonly", description = "Delete admin-ui license related information"),
@OAuthScope(name = "https://jans.io/oauth/jans-auth-server/config/adminui/license.write", description = "View admin-ui license related information")}
)))
public class ApiApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();

        // General
        classes.add(OAuth2Resource.class);
        classes.add(AuditLoggerResource.class);
        classes.add(LicenseResource.class);
        classes.add(UserManagementResource.class);
        return classes;
    }
}
