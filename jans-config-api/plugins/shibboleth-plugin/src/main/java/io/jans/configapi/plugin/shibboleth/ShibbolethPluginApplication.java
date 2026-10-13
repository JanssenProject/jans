package io.jans.configapi.plugin.shibboleth;

import io.jans.configapi.plugin.shibboleth.rest.ShibbolethPluginConfigResource;
import io.jans.configapi.plugin.shibboleth.rest.ShibbolethResource;
import io.jans.configapi.plugin.shibboleth.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;

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

@ApplicationPath("/shibboleth")
@OpenAPIDefinition(info = @Info(title = "Jans Config API - Shibboleth IDP", version = "1.0.0", contact = @Contact(name = "Gluu Support", url = "https://support.gluu.org", email = "support@gluu.org"),

        license = @License(name = "Apache 2.0", url = "https://github.com/JanssenProject/jans/blob/main/LICENSE")),

        tags = { @Tag(name = "Shibboleth - Plugin Configuration"),
                @Tag(name = "Shibboleth - Trust Relationship") }, servers = {
                        @Server(url = "https://jans.io/", description = "The Jans server") })

@SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(clientCredentials = @OAuthFlow(tokenUrl = "https://{op-hostname}/.../token", scopes = {
        @OAuthScope(name = Constants.SHIBBOLETH_READ_ACCESS, description = "View Shibboleth related information"),
        @OAuthScope(name = Constants.SHIBBOLETH_WRITE_ACCESS, description = "Manage Shibboleth related information"),
        @OAuthScope(name = Constants.SHIBBOLETH_ADMIN_ACCESS, description = "Admin to manage Shibboleth related information"),
        @OAuthScope(name = Constants.SHIBBOLETH_CONFIG_READ_ACCESS, description = "View Shibboleth config related information"),
        @OAuthScope(name = Constants.SHIBBOLETH_CONFIG_WRITE_ACCESS, description = "Manage Shibboleth config related information"),
        @OAuthScope(name = Constants.SHIBBOLETH_CONFIG_ADMIN_ACCESS, description = "Admin to manage Shibboleth config related information"),
        @OAuthScope(name = Constants.SHIBBOLETH_TR_READ_ACCESS, description = "View Shibboleth trust relationship related information"),
        @OAuthScope(name = Constants.SHIBBOLETH_TR_WRITE_ACCESS, description = "Manage Shibboleth trust relationship related information"),
        @OAuthScope(name = Constants.SHIBBOLETH_TR_DELETE_ACCESS, description = "Manage Shibboleth trust relationship related information"),
        @OAuthScope(name = Constants.SHIBBOLETH_TR_ADMIN_ACCESS, description = "Admin to manage Shibboleth trust relationship related information"),
        @OAuthScope(name = ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, description = "Super admin for viewing application resource information"),
        @OAuthScope(name = ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS, description = "Super admin for updating application resource information"),
        @OAuthScope(name = ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS, description = "Super admin for deleting application resource information"), })))
public class ShibbolethPluginApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(ShibbolethPluginConfigResource.class);
        classes.add(ShibbolethResource.class);
        return classes;
    }
}
