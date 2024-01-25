package io.jans.configapi.plugin.saml.rest;

import io.jans.configapi.core.configuration.ObjectMapperContextResolver;
import io.jans.configapi.core.rest.BaseApiApplication;
import io.jans.configapi.plugin.saml.util.Constants;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.tags.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.servers.*;

import jakarta.ws.rs.ApplicationPath;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/kc")
@OpenAPIDefinition(info = @Info(title = "Jans Config API - SAML", version="OAS Version", contact = @Contact(name = "Gluu Support", url = "https://support.gluu.org", email = "xxx@gluu.org"),

license = @License(name = "Apache 2.0", url = "https://github.com/JanssenProject/jans/blob/main/LICENSE")),

tags = { @Tag(name = "SAML - Configuration"),
@Tag(name = "SAML - Trust Relationship"),
@Tag(name = "SAML - Identity Broker"),
},

servers = { @Server(url = "https://jans.io/", description = "The Jans server") })

@SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(clientCredentials = @OAuthFlow(tokenUrl = "https://{op-hostname}/.../token", scopes = {
@OAuthScope(name = Constants.SAML_READ_ACCESS, description = "View SAML related information"),
@OAuthScope(name = Constants.SAML_WRITE_ACCESS, description = "Manage SAML related information"),
@OAuthScope(name = Constants.SAML_CONFIG_READ_ACCESS, description = "View SAML configuration related information"),
@OAuthScope(name = Constants.SAML_CONFIG_WRITE_ACCESS, description = "Manage SAML configuration related information")
}
)))
public class ApiApplication extends BaseApiApplication {
    
    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();

        // General
        classes = (HashSet) addCommonClasses((classes));
        
        // General Application level class
        classes.add(ObjectMapperContextResolver.class);
                
        classes.add(SamlConfigResource.class);
        classes.add(TrustRelationshipResource.class);
        classes.add(IdpResource.class);
				
        return classes;
    }
}
