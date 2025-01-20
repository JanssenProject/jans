package io.jans.configapi.plugin.lock.rest;

import io.jans.configapi.plugin.lock.util.Constants;
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

@ApplicationPath(Constants.LOCK)
@OpenAPIDefinition(info = @Info(title = "Jans Config API - Lock", version = "1.0.0", contact = @Contact(name = "Gluu Support", url = "https://support.gluu.org", email = "support@gluu.org"),

        license = @License(name = "Apache 2.0", url = "https://github.com/JanssenProject/jans/blob/main/LICENSE")),

        tags = { @Tag(name = "Lock - Configuration"),
                 @Tag(name = "Lock - Audit"),
                 @Tag(name = "Lock - Statistics")
                 },

        servers = { @Server(url = "https://jans.io/", description = "The Jans server") })

@SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(clientCredentials = @OAuthFlow(tokenUrl = "https://{op-hostname}/.../token", scopes = {
        @OAuthScope(name = Constants.LOCK_READ_ACCESS, description = "View Lock related information"),
        @OAuthScope(name = Constants.LOCK_WRITE_ACCESS, description = "View Lock related information"),
        @OAuthScope(name = Constants.LOCK_CONFIG_READ_ACCESS, description = "View Lock configuration related information"),
        @OAuthScope(name = Constants.LOCK_CONFIG_WRITE_ACCESS, description = "Manage Lock configuration related information"),
        @OAuthScope(name = Constants.LOCK_AUDIT_READ_ACCESS, description = "View Lock audit related information"),
        @OAuthScope(name = Constants.LOCK_AUDIT_WRITE_ACCESS, description = "View Lock audit related information"),
        @OAuthScope(name = Constants.LOCK_HEALTH_READ_ACCESS, description = "View Lock health related information"),
        @OAuthScope(name = Constants.LOCK_HEALTH_WRITE_ACCESS, description = "Manage Lock health related information"),
        @OAuthScope(name = Constants.LOCK_LOG_READ_ACCESS, description = "View Lock log related information"),
        @OAuthScope(name = Constants.LOCK_LOG_WRITE_ACCESS, description = "Manage Lock log health related information"),
        @OAuthScope(name = Constants.LOCK_TELEMETRY_READ_ACCESS, description = "View Lock telemetry related information"),
        @OAuthScope(name = Constants.LOCK_TELEMETRY_WRITE_ACCESS, description = "Manage Lock telemetry related information")

})))
public class ApiApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();

        classes.add(LockConfigResource.class);
        classes.add(AuditResource.class);

        return classes;
    }
}
