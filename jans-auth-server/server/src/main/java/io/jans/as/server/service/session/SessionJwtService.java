package io.jans.as.server.service.session;

import com.mysql.cj.util.StringUtils;
import io.jans.as.common.claims.Audience;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.authorize.ws.rs.AuthzRequest;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.DiscoveryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class SessionJwtService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private ClientService clientService;

    @Inject
    private DiscoveryService discoveryService;

    @Inject
    private SessionStatusListIndexService sessionStatusListIndexService;

    public String createSessionJwt(AuthzRequest authzRequest, SessionId sessionUser, Client client) {
        if (!appConfiguration.isFeatureEnabled(FeatureFlagType.SESSION_STATUS_LIST)) {
            log.debug("Skip Session JWT created because session_status_list feature flag is not enabled");
            return "";
        }

        try {
            Integer sessionIndex = getOrGenerateSessionIndex(sessionUser);

            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm
                    .fromString(appConfiguration.getDefaultSignatureAlgorithm());
            String sessionJwtSignedResponseAlg = client.getAttributes().getSessionJwtSignedResponseAlg();
            if (StringUtils.isNullOrEmpty(sessionJwtSignedResponseAlg)
                    && SignatureAlgorithm.fromString(sessionJwtSignedResponseAlg) != null) {
                signatureAlgorithm = SignatureAlgorithm.fromString(sessionJwtSignedResponseAlg);
            }

            Date now = new Date();
            Calendar expirationCalendar = Calendar.getInstance();
            expirationCalendar.setTime(now);
            expirationCalendar.add(Calendar.SECOND, appConfiguration.getSessionIdLifetime());

            final JwtSigner jwtSigner = new JwtSigner(appConfiguration, webKeysConfiguration, signatureAlgorithm,
                    client.getClientId(), clientService.decryptSecret(client.getClientSecret()));
            final Jwt jwt = jwtSigner.newJwt();

            final JSONObject statusList = new JSONObject();
            statusList.put("idx", sessionIndex);
            statusList.put("uri", discoveryService.getSessionStatusListEndpoint());

            jwt.getClaims().setExpirationTime(expirationCalendar.getTime());
            jwt.getClaims().setIat(now);
            jwt.getClaims().setNbf(now);

            jwt.getClaims().setClaim("sid", sessionUser.getOutsideSid());
            jwt.getClaims().setClaim("jti", UUID.randomUUID().toString());
            jwt.getClaims().setClaim("status_list", statusList);

            Audience.setAudience(jwt.getClaims(), client);
            final String jwtString = jwtSigner.sign().toString();
            log.debug("Session JWT is successfully generated: {}", jwtString);
            return jwtString;
        } catch (Exception e) {
            log.error("Failed to create Session JWT for session {}", sessionUser.getId(), e);
            return "";
        }
    }

    private Integer getOrGenerateSessionIndex(SessionId sessionId) {
        Integer sessionIndex = sessionId.getPredefinedAttributes().getIndex();
        if (sessionIndex == null) {
            sessionIndex = sessionStatusListIndexService.next();
            sessionId.getPredefinedAttributes().setIndex(sessionIndex);
        }
        return sessionIndex;
    }
}
