/*
 Copyright (c) 2024, Gluu
 Author: Yuriy Z
 */

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.config.Constants;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.external.context.ExternalClientAuthnContext;
import io.jans.as.server.service.token.TokenService;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.client.ClientAuthnType;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.service.custom.script.CustomScriptManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Yuriy Z
 */
public class ClientAuthn implements ClientAuthnType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public Object authenticateClient(Object clientAuthnContext) {
        final ExternalClientAuthnContext context = (ExternalClientAuthnContext) clientAuthnContext;

        final HttpServletRequest request = context.getHttpRequest();
        final HttpServletResponse response = context.getHttpResponse();

        String authorization = request.getHeader(Constants.AUTHORIZATION);
        if (!StringUtils.startsWith(authorization, "Basic")) {
            context.sendUnauthorizedError();
            return null;
        }

        TokenService tokenService = CdiUtil.bean(TokenService.class);
        ClientService clientService = CdiUtil.bean(ClientService.class);

        String base64Token = tokenService.getBasicToken(authorization);
        String token = new String(Base64.decodeBase64(base64Token), StandardCharsets.UTF_8);

        int delim = token.indexOf(":");

        if (delim != -1) {
            String clientId = URLDecoder.decode(token.substring(0, delim), StandardCharsets.UTF_8);
            String clientSecret = URLDecoder.decode(token.substring(delim + 1), StandardCharsets.UTF_8);

            final boolean authenticated = clientService.authenticate(clientId, clientSecret);
            if (authenticated) {
                final Client client = clientService.getClient(clientId);
                scriptLogger.debug("Successfully performed basic client authentication, clientId: {}", clientId);
                return client;
            }
        }

        context.sendUnauthorizedError();
        return null;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized ClientAuthn Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized ClientAuthn Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed ClientAuthn Java custom script.");
        return false;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}
