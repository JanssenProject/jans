package io.jans.as.server.service.external;

import io.jans.as.common.model.registration.Client;
import io.jans.as.server.auth.Authenticator;
import io.jans.as.server.service.external.context.ExternalClientAuthnContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.client.ClientAuthnType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Client Authentication service responsible for external script interaction.
 *
 * @author Yuriy Z
 */
@ApplicationScoped
public class ExternalClientAuthnService extends ExternalScriptService {

    @Inject
    private Authenticator authenticator;

    public ExternalClientAuthnService() {
        super(CustomScriptType.CLIENT_AUTHN);
    }

    public Client externalAuthenticateClient(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException {
        final List<CustomScriptConfiguration> scripts = getCustomScriptConfigurations();
        if (scripts == null || scripts.isEmpty()) {
            log.trace("Unable to perform client authentication by custom script because there is no `client_authn` scripts.");
            return null;
        }

        for (CustomScriptConfiguration script : scripts) {
            final Client client = externalAuthenticateClient(script, servletRequest, servletResponse);
            if (client != null) {
                log.trace("Client {} authenticated successfully by custom script {}.", getClientId(client), script.getName());
                return client;
            }
        }

        log.trace("All `client_authn` scripts returned false.");
        return null;
    }

    private Client externalAuthenticateClient(CustomScriptConfiguration customScript, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {

        ClientAuthnType script = (ClientAuthnType) customScript.getExternalType();

        log.trace("Executing external 'authenticateClient' method, script name: {}, requestParameters: {}",
                customScript.getName(), servletRequest.getParameterMap());

        ExternalClientAuthnContext context = new ExternalClientAuthnContext(servletRequest, servletResponse);

        Client client = null;

        try {
            client = (Client) script.authenticateClient(context);
            if (client != null) {
                authenticator.configureSessionClient(client);
            }
        } catch (Exception e) {
            log.error("Failed to run external 'authenticateClient' method of script " + customScript.getName(), e);
            client = null;
        }

        log.trace("Executed external 'authenticateClient' method, client {}, script name: {}, requestParameters: {}",
                getClientId(client), customScript.getName(), servletRequest.getParameterMap());
        return client;
    }


    private static String getClientId(Client client) {
        return client != null ? client.getClientId() : "null";
    }
}
