package io.jans.configapi.plugin.link.rest;

import io.jans.configapi.plugin.link.util.Constants;
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

@ApplicationPath("/jans-link")
@OpenAPIDefinition(info = @Info(title = "Jans Config API - Jans Link Plugin", version = "1.0.0", contact = @Contact(name = "Gluu Support", url = "https://support.gluu.org", email = "xxx@gluu.org"),

license = @License(name = "Apache 2.0", url = "https://github.com/JanssenProject/jans/blob/main/LICENSE")),

tags = { @Tag(name = "Jans Link - Configuration")},

servers = { @Server(url = "https://jans.io/", description = "The Jans server") })

@SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(clientCredentials = @OAuthFlow(tokenUrl = "https://{op-hostname}/.../token", scopes = {
@OAuthScope(name = Constants.JANSLINK_CONFIG_READ_ACCESS, description = "View Jans link configuration related information"),
@OAuthScope(name = Constants.JANSLINK_CONFIG_WRITE_ACCESS, description = "Manage Jans link configuration related information")}
)))
public class ApiApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();

        classes.add(JansLinkConfigResource.class);
        
        return classes;
    }
}
