package io.jans.as.server.token.ws.rs;

import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.token.HandleTokenFactory;
import io.jans.as.server.service.SessionIdService;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author Yuriy Z
 */
@Stateless
@Named
public class TokenInternalService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private SessionIdService sessionIdService;

    public String rotateDeviceSecret(SessionId sessionId, String actorToken) {
        if (BooleanUtils.isFalse(appConfiguration.getRotateDeviceSecret())) {
            return null;
        }

        String newDeviceSecret = HandleTokenFactory.generateDeviceSecret();

        final List<String> deviceSecrets = sessionId.getDeviceSecrets();
        deviceSecrets.remove(actorToken);
        deviceSecrets.add(newDeviceSecret);

        sessionIdService.updateSessionId(sessionId, false);

        return newDeviceSecret;
    }
}
