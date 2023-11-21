package io.jans.configapi.plugin.keycloak.idp.broker.rest;

import io.jans.configapi.plugin.keycloak.idp.broker.util.Constants;
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

@ApplicationPath(Constants.IDENTITY_PROVIDER)
@OpenAPIDefinition(info = @Info(title = "Jans Config API - Keycloak Identity Broker", version = "1.0.0", contact = @Contact(name = "Gluu Support", url = "https://support.gluu.org", email = "xxx@gluu.org"),

license = @License(name = "Apache 2.0", url = "https://github.com/JanssenProject/jans/blob/main/LICENSE")),

tags = { @Tag(name = "Jans - Keycloak SAML Identity Broker"), @Tag(name = "Jans - Keycloak Realm")  },

servers = { @Server(url = "https://jans.io/", description = "The Jans server") })

@SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(clientCredentials = @OAuthFlow(tokenUrl = "https://{op-hostname}/.../token", scopes = {
@OAuthScope(name = Constants.JANS_KC_CONFIG_READ_ACCESS, description = "View Jans Keycloak config related information"),
@OAuthScope(name = Constants.JANS_KC_CONFIG_WRITE_ACCESS, description = "Manage Jans Keycloak config related information"),
@OAuthScope(name = Constants.KC_REALM_READ_ACCESS, description = "View Keycloak realm related information"),
@OAuthScope(name = Constants.KC_REALM_WRITE_ACCESS, description = "Manage Keycloak realm related information"),
@OAuthScope(name = Constants.KC_SAML_IDP_WRITE_ACCESS, description = "View Keycloak SAML Identity Broker related information"),
@OAuthScope(name = Constants.KC_SAML_IDP_WRITE_ACCESS, description = "Manage Keycloak SAML Identity Broker related information")
}
)))
public class IdpApiApplication extends Application { 

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();

        classes.add(IdpResource.class);
        classes.add(KeycloakRealmResource.class);   
		
        return classes;
    }
}

