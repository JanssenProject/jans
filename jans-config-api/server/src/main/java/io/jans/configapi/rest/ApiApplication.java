/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest;

import io.jans.configapi.core.rest.BaseApiApplication;
import io.jans.configapi.rest.resource.auth.*;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.rest.health.ApiHealthCheck;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.tags.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.servers.*;

import java.util.HashSet;
import java.util.Set;
import jakarta.ws.rs.ApplicationPath;

/**
 * @author Mougang T.Gasmyr
 *
 */
@ApplicationPath("/api/v1")
@OpenAPIDefinition(info = @Info(title = "Jans Config API", contact =
@Contact(name = "Contact", url = "https://github.com/JanssenProject/jans/discussions"),

        license = @License(name = "License", url = "https://github" +
                ".com/JanssenProject/jans/blob/main/LICENSE"),

        version = "OAS Version"),

        tags = { @Tag(name = "Attribute"), @Tag(name = "Default Authentication Method"),
                @Tag(name = "Cache Configuration"), @Tag(name = "Cache Configuration – Memcached"),
                @Tag(name = "Cache Configuration – Redis"), @Tag(name = "Cache Configuration – in-Memory"),
                @Tag(name = "Cache Configuration – Native-Persistence"), @Tag(name = "Configuration – Properties"),
                @Tag(name = "Configuration – SMTP"), @Tag(name = "Configuration – Logging"),
                @Tag(name = "Configuration – JWK - JSON Web Key (JWK)"), @Tag(name = "Custom Scripts"),
                @Tag(name = "Database - LDAP configuration"), @Tag(name = "OAuth - OpenID Connect - Clients"),
                @Tag(name = "OAuth - UMA Resources"), @Tag(name = "OAuth - Scopes"),
                @Tag(name = "Agama - Configuration"), @Tag(name = "Agama"),
                @Tag(name = "Statistics - User"), @Tag(name = "Health - Check"), @Tag(name = "Server Stats"),
                @Tag(name = "Auth - Session Management"), @Tag(name = "Organization Configuration"),
                @Tag(name = "Auth Server Health - Check"), @Tag(name = "Plugins"),
                @Tag(name = "Configuration – Config API"), @Tag(name = "Client Authorization")},

        servers = { @Server(url = "https://jans.local.io", description = "The Jans server") })

@SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(clientCredentials = @OAuthFlow(tokenUrl = "https://{op-hostname}/.../token", scopes = {
        @OAuthScope(name = ApiAccessConstants.JANS_AUTH_CONFIG_READ_ACCESS, description = "View Auth Server properties related information"),
        @OAuthScope(name = ApiAccessConstants.JANS_AUTH_CONFIG_WRITE_ACCESS, description = "Manage Auth Server properties related information"),
        @OAuthScope(name = ApiAccessConstants.ATTRIBUTES_READ_ACCESS, description = "View attribute related information"),
        @OAuthScope(name = ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS, description = "Manage attribute related information"),
        @OAuthScope(name = ApiAccessConstants.ATTRIBUTES_DELETE_ACCESS, description = "Delete attribute related information"),
        @OAuthScope(name = ApiAccessConstants.ACRS_READ_ACCESS, description = "View ACRS related information"),
        @OAuthScope(name = ApiAccessConstants.ACRS_WRITE_ACCESS, description = "Manage ACRS related information"),
        @OAuthScope(name = ApiAccessConstants.DATABASE_LDAP_READ_ACCESS, description = "View LDAP database related information"),
        @OAuthScope(name = ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS, description = "Manage LDAP database related information"),
        @OAuthScope(name = ApiAccessConstants.DATABASE_LDAP_DELETE_ACCESS, description = "Delete LDAP database related information"),
        @OAuthScope(name = ApiAccessConstants.SCRIPTS_READ_ACCESS, description = "View cache scripts information"),
        @OAuthScope(name = ApiAccessConstants.SCRIPTS_WRITE_ACCESS, description = "Manage scripts related information"),
        @OAuthScope(name = ApiAccessConstants.SCRIPTS_DELETE_ACCESS, description = "Delete scripts related information"),
        @OAuthScope(name = ApiAccessConstants.CACHE_READ_ACCESS, description = "View cache related information"),
        @OAuthScope(name = ApiAccessConstants.CACHE_WRITE_ACCESS, description = "Manage cache related information"),
        @OAuthScope(name = ApiAccessConstants.SMTP_READ_ACCESS, description = "View SMTP related information"),
        @OAuthScope(name = ApiAccessConstants.SMTP_WRITE_ACCESS, description = "Manage SMTP related information"),
        @OAuthScope(name = ApiAccessConstants.SMTP_DELETE_ACCESS, description = "Delete SMTP related information"),
        @OAuthScope(name = ApiAccessConstants.LOGGING_READ_ACCESS, description = "View logging related information"),
        @OAuthScope(name = ApiAccessConstants.LOGGING_WRITE_ACCESS, description = "Manage logging related information"),
        @OAuthScope(name = ApiAccessConstants.JWKS_READ_ACCESS, description = "View JWKS related information"),
        @OAuthScope(name = ApiAccessConstants.JWKS_WRITE_ACCESS, description = "Manage JWKS related information"),
        @OAuthScope(name = ApiAccessConstants.JWKS_DELETE_ACCESS, description = "Delete JWKS related information"),
        @OAuthScope(name = ApiAccessConstants.OPENID_CLIENTS_READ_ACCESS, description = "View clients related information"),
        @OAuthScope(name = ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS, description = "Manage clients related information"),
        @OAuthScope(name = ApiAccessConstants.OPENID_CLIENTS_DELETE_ACCESS, description = "Delete clients related information"),
        @OAuthScope(name = ApiAccessConstants.SCOPES_READ_ACCESS, description = "View scope related information"),
        @OAuthScope(name = ApiAccessConstants.SCOPES_WRITE_ACCESS, description = "Manage scope related information"),
        @OAuthScope(name = ApiAccessConstants.SCOPES_DELETE_ACCESS, description = "Delete scope related information"),
        @OAuthScope(name = ApiAccessConstants.UMA_RESOURCES_READ_ACCESS, description = "View UMA Resource related information"),
        @OAuthScope(name = ApiAccessConstants.UMA_RESOURCES_WRITE_ACCESS, description = "Manage UMA Resource related information"),
        @OAuthScope(name = ApiAccessConstants.UMA_RESOURCES_DELETE_ACCESS, description = "Delete UMA Resource related information"),
        @OAuthScope(name = ApiAccessConstants.STATS_USER_READ_ACCESS, description = "View server with basic statistic"),
        @OAuthScope(name = ApiAccessConstants.ORG_CONFIG_READ_ACCESS, description = "View organization configuration information"),
        @OAuthScope(name = ApiAccessConstants.ORG_CONFIG_WRITE_ACCESS, description = "Manage organization configuration information"),
        @OAuthScope(name = ApiAccessConstants.AGAMA_READ_ACCESS, description = "View Agama Flow related information"),
        @OAuthScope(name = ApiAccessConstants.AGAMA_WRITE_ACCESS, description = "Manage Agama Flow related information"),
        @OAuthScope(name = ApiAccessConstants.AGAMA_DELETE_ACCESS, description = "Delete Agama Flow related information"),
        @OAuthScope(name = ApiAccessConstants.JANS_AUTH_SESSION_READ_ACCESS, description = "View Session related information"),
        @OAuthScope(name = ApiAccessConstants.JANS_AUTH_SESSION_DELETE_ACCESS, description = "Delete Session information"),
        @OAuthScope(name = ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, description = "Admin read scope"),
        @OAuthScope(name = ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS, description = "Admin write scope"),
        @OAuthScope(name = ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS, description = "Admin delete scope"),
        @OAuthScope(name = ApiAccessConstants.OPENID_READ_ACCESS, description = "View OpenID functionality"),
        @OAuthScope(name = ApiAccessConstants.OPENID_WRITE_ACCESS, description = "Manage OpenID functionality"),
        @OAuthScope(name = ApiAccessConstants.OPENID_DELETE_ACCESS, description = "Delete OpenID functionality"),
        @OAuthScope(name = ApiAccessConstants.UMA_READ_ACCESS, description = "View UMA functionality"),
        @OAuthScope(name = ApiAccessConstants.UMA_WRITE_ACCESS, description = "Manage UMA functionality"),
        @OAuthScope(name = ApiAccessConstants.UMA_DELETE_ACCESS, description = "Delete UMA functionality"),
        @OAuthScope(name = ApiAccessConstants.PLUGIN_READ_ACCESS, description = "View Plugin information"),
        @OAuthScope(name = ApiAccessConstants.CONFIG_READ_ACCESS, description = "View Config-API related configuration properties"),
        @OAuthScope(name = ApiAccessConstants.CONFIG_WRITE_ACCESS, description = "Manage Config-API related configuration properties"),
        @OAuthScope(name = ApiAccessConstants.CLIENT_AUTHORIZATIONS_READ_ACCESS, description = "View ClientAuthorizations"),
        @OAuthScope(name = ApiAccessConstants.CLIENT_AUTHORIZATIONS_DELETE_ACCESS, description = "Revoke ClientAuthorizations") }

)))
public class ApiApplication extends BaseApiApplication {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();

        // General
        classes = (HashSet) addCommonClasses((classes));
        
        classes.add(ApiHealthCheck.class);

        // oAuth Config
        classes.add(AcrsResource.class);
        classes.add(AttributesResource.class);
        classes.add(CacheConfigurationResource.class);
        classes.add(MessageConfigurationResource.class);
        classes.add(ClientsResource.class);
        classes.add(AuthConfigResource.class);
        classes.add(ConfigSmtpResource.class);
        classes.add(CustomScriptResource.class);
        classes.add(JwksResource.class);
        classes.add(LdapConfigurationResource.class);
        classes.add(LoggingResource.class);
        classes.add(ScopesResource.class);
        classes.add(UmaResourcesResource.class);
        classes.add(StatResource.class);
        classes.add(HealthCheckResource.class);
        classes.add(OrganizationResource.class);
        classes.add(AgamaResource.class);
        classes.add(AgamaDeploymentsResource.class);
        classes.add(SessionResource.class);
        classes.add(PluginResource.class);
        classes.add(ConfigApiResource.class);
        classes.add(ClientAuthResource.class);

        return classes;
    }
}
