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
@OpenAPIDefinition(info = @Info(title = "Jans Config API", version="OAS Version", contact =
@Contact(name = "Contact", url = "https://github.com/JanssenProject/jans/discussions"),

license = @License(name = "Apache 2.0", url = "https://github.com/JanssenProject/jans/blob/main/LICENSE")),

tags = { 
        @Tag(name = "Jans - SAML Identity Broker Configuration"), 
        @Tag(name = "Jans - SAML Identity Broker"), 
        @Tag(name = "Jans - SAML Identity Broker Realm")  },

servers = { @Server(url = "https://jans.io/", description = "The Jans server") })

@SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(clientCredentials = @OAuthFlow(tokenUrl = "https://{op-hostname}/.../token", scopes = {
@OAuthScope(name = Constants.JANS_IDP_CONFIG_READ_ACCESS, description = "View Jans Identity Broker config related information"),
@OAuthScope(name = Constants.JANS_IDP_CONFIG_WRITE_ACCESS, description = "Manage Jans Identity Broker config related information"),
@OAuthScope(name = Constants.JANS_IDP_REALM_READ_ACCESS, description = "View Identity Broker realm related information"),
@OAuthScope(name = Constants.JANS_IDP_REALM_WRITE_ACCESS, description = "Manage Identity Broker realm related information"),
@OAuthScope(name = Constants.JANS_IDP_SAML_READ_ACCESS, description = "View Identity Broker SAML Identity Broker related information"),
@OAuthScope(name = Constants.JANS_IDP_SAML_WRITE_ACCESS, description = "Manage Identity Broker SAML Identity Broker related information")
}
)))
public class IdpApiApplication extends Application { 

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();
        
        classes.add(IdpConfigResource.class); 
        classes.add(IdpResource.class);
        classes.add(IdpRealmResource.class);   
		
        return classes;
    }
}

