/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest;

import io.jans.configapi.configuration.ObjectMapperContextResolver;
import io.jans.configapi.rest.resource.auth.*;
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
import jakarta.ws.rs.core.Application;

/**
 * @author Mougang T.Gasmyr
 *
 */
@ApplicationPath("/api/v1")
@OpenAPIDefinition(info = @Info(title = "Jans Config API", version = "1.0.0", contact = @Contact(name = "Gluu Support", url = "https://support.gluu.org", email = "xxx@gluu.org"),

        license = @License(name = "Apache 2.0", url = "https://github.com/JanssenProject/jans/blob/main/LICENSE")),

        tags = { @Tag(name = "Attribute"), @Tag(name = "Default Authentication Method"),
                @Tag(name = "Cache Configuration – Memcached"), @Tag(name = "Cache Configuration – Redis"),
                @Tag(name = "Cache Configuration – in-Memory"), @Tag(name = "Cache Configuration – Native-Persistence"),
                @Tag(name = "Configuration – Properties"), @Tag(name = "Fido2 - Configuration"),
                @Tag(name = "Configuration – SMTP"), @Tag(name = "Configuration – Logging"),
                @Tag(name = "Configuration – JWK - JSON Web Key (JWK)"), @Tag(name = "Custom Scripts"),
                @Tag(name = "Database - LDAP configuration"), @Tag(name = "Database - Couchbase configuration"),
                @Tag(name = "OAuth - OpenID Connect - Clients"), @Tag(name = "OAuth - UMA Resources"),
                @Tag(name = "OAuth - Scopes"), @Tag(name = "Configuration – Agama Flow"),
                @Tag(name = "Statistics - User"), @Tag(name = "Health - Check"), @Tag(name = "Server Stats"),
                @Tag(name = "Auth - Session Management"),
                @Tag(name = "Organization Configuration"),
                @Tag(name = "Auth Server Health - Check") },

        servers = { @Server(url = "https://jans.io/", description = "The Jans server") })

@SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(clientCredentials = @OAuthFlow(tokenUrl = "https://{op-hostname}/.../token", scopes = {
        @OAuthScope(name = "https://jans.io/oauth/jans-auth-server/config/properties.readonly", description = "View Auth Server properties related information"),
        @OAuthScope(name = "https://jans.io/oauth/jans-auth-server/config/properties.write", description = "Manage Auth Server properties related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/attributes.readonly", description = "View attribute related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/attributes.write", description = "Manage attribute related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/attributes.delete", description = "Delete attribute related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/acrs.readonly", description = "View ACRS related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/acrs.write", description = "Manage ACRS related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/database/ldap.readonly", description = "View LDAP database related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/database/ldap.write", description = "Manage LDAP database related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/database/ldap.delete", description = "Delete LDAP database related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/database/couchbase.readonly", description = "View Couchbase database information"),
        @OAuthScope(name = "https://jans.io/oauth/config/database/couchbase.write", description = "Manage Couchbase database related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/database/couchbase.delete", description = "Delete Couchbase database related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/scripts.readonly", description = "View cache scripts information"),
        @OAuthScope(name = "https://jans.io/oauth/config/scripts.write", description = "Manage scripts related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/scripts.delete", description = "Delete scripts related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/cache.readonly", description = "View cache related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/cache.write", description = "Manage cache related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/smtp.readonly", description = "View SMTP related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/smtp.write", description = "Manage SMTP related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/smtp.delete", description = "Delete SMTP related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/logging.readonly", description = "View logging related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/logging.write", description = "Manage logging related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/jwks.readonly", description = "View JWKS related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/jwks.write", description = "Manage JWKS related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/openid/clients.readonly", description = "View clients related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/openid/clients.write", description = "Manage clients related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/openid/clients.delete", description = "Delete clients related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/scopes.readonly", description = "View scope related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/scopes.write", description = "Manage scope related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/scopes.delete", description = "Delete scope related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/uma/resources.readonly", description = "View UMA Resource related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/uma/resources.write", description = "Manage UMA Resource related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/uma/resources.delete", description = "Delete UMA Resource related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/database/sql.readonly", description = "View SQL database related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/database/sql.write", description = "Manage SQL database related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/database/sql.delete", description = "Delete SQL database related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/stats.readonly", description = "View server with basic statistic"),
        @OAuthScope(name = "https://jans.io/oauth/config/organization.readonly", description = "View organization configuration information"),
        @OAuthScope(name = "https://jans.io/oauth/config/organization.write", description = "Manage organization configuration information"),
        @OAuthScope(name = "https://jans.io/oauth/config/user.readonly", description = "View user related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/user.write", description = "Manage user related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/user.delete", description = "Delete user related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/agama.readonly", description = "View Agama Flow related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/agama.write", description = "Manage Agama Flow related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/agama.delete", description = "Delete Agama Flow related information"),
        @OAuthScope(name = "https://jans.io/oauth/jans-auth-server/session.readonly", description = "View Session related information"),
        @OAuthScope(name = "https://jans.io/oauth/jans-auth-server/session.delete", description = "Delete Session information") }

)))
public class ApiApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();

        // General
        classes.add(ObjectMapperContextResolver.class);
        classes.add(ApiHealthCheck.class);

        // oAuth Config
        classes.add(AcrsResource.class);
        classes.add(AttributesResource.class);
        classes.add(CacheConfigurationResource.class);
        classes.add(ClientsResource.class);
        classes.add(ConfigResource.class);
        classes.add(ConfigSmtpResource.class);
        classes.add(CouchbaseConfigurationResource.class);
        classes.add(CustomScriptResource.class);
        classes.add(JwksResource.class);
        classes.add(LdapConfigurationResource.class);
        classes.add(LoggingResource.class);
        classes.add(ScopesResource.class);
        classes.add(UmaResourcesResource.class);
        classes.add(StatResource.class);
        classes.add(HealthCheckResource.class);
        classes.add(OrganizationResource.class);
        classes.add(SqlConfigurationResource.class);
        classes.add(AgamaResource.class);
        classes.add(SessionResource.class);

        return classes;
    }
}
