package io.jans.as.server.service.logout;

import io.jans.as.common.claims.Audience;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.model.util.CertUtils;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.common.LogoutStatusJwt;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.DiscoveryService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.external.ExternalLogoutStatusJwtService;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.as.server.service.token.StatusListIndexService;
import io.jans.as.server.util.ServerUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class LogoutStatusJwtService {

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
    private StatusListIndexService statusListIndexService;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ExternalLogoutStatusJwtService externalLogoutStatusJwtService;

    public LogoutStatusJwt createLogoutStatusJwt(ExecutionContext context, AuthorizationGrant grant) {
        try {
            context.initFromGrantIfNeeded(grant);
            context.generateRandomTokenReferenceId();

            Integer lifetime = appConfiguration.getLogoutStatusJwtLifetime();
            int lifetimeFromScript = externalLogoutStatusJwtService.getLifetimeInSeconds(ExternalScriptContext.of(context));
            if (lifetimeFromScript > 0) {
                lifetime = lifetimeFromScript;
                log.trace("Override logout_status_jwt lifetime with value from script: {}", lifetimeFromScript);
            }

            LogoutStatusJwt logoutStatusJwt = new LogoutStatusJwt(lifetime);

            logoutStatusJwt.setSessionDn(grant.getSessionDn());
            logoutStatusJwt.setX5ts256(CertUtils.confirmationMethodHashS256(context.getCertAsPem()));
            logoutStatusJwt.setReferenceId(context.getTokenReferenceId());

            final String dpop = context.getDpop();
            if (org.apache.commons.lang3.StringUtils.isNoneBlank(dpop)) {
                logoutStatusJwt.setDpop(dpop);
            }

            Integer statusListIndex = null;
            if (errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)) {
                statusListIndex = statusListIndexService.next();
                context.setStatusListIndex(statusListIndex);
                logoutStatusJwt.setStatusListIndex(statusListIndex);
            }

            if (logoutStatusJwt.getExpiresIn() < 0) {
                log.trace("Failed to create logout status jwt with negative expiration time");
                return null;
            }

            final Client client = grant.getClient();
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm
                    .fromString(appConfiguration.getDefaultSignatureAlgorithm());
            String logoutStatusJwtSignedResponseAlg = client.getAttributes().getLogoutStatusJwtSignedResponseAlg();
            if (com.mysql.cj.util.StringUtils.isNullOrEmpty(logoutStatusJwtSignedResponseAlg)
                    && SignatureAlgorithm.fromString(logoutStatusJwtSignedResponseAlg) != null) {
                signatureAlgorithm = SignatureAlgorithm.fromString(logoutStatusJwtSignedResponseAlg);
            }

            final JwtSigner jwtSigner = new JwtSigner(appConfiguration, webKeysConfiguration, signatureAlgorithm,
                    client.getClientId(), clientService.decryptSecret(client.getClientSecret()));
            final Jwt jwt = jwtSigner.newJwt();

            String jti = fillPayload(jwt, statusListIndex, lifetime, grant.getSessionDn());
            logoutStatusJwt.setJti(jti);

            Audience.setAudience(jwt.getClaims(), client);

            boolean externalOk = externalLogoutStatusJwtService.modifyLogoutStatusJwtMethod(jwt, ExternalScriptContext.of(context));
            if (!externalOk) {
                final String reason = "External LogoutStatusJwt script forbids logout_status_jwt creation.";
                log.trace(reason);

                throw new WebApplicationException(Response
                        .status(Response.Status.FORBIDDEN)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .cacheControl(ServerUtil.cacheControl(true, false))
                        .header("Pragma", "no-cache")
                        .entity(errorResponseFactory.errorAsJson(TokenErrorResponseType.ACCESS_DENIED, reason))
                        .build());
            }

            final String jwtString = jwtSigner.sign().toString();
            if (log.isDebugEnabled())
                log.debug("Created Logout Status JWT: {}", jwtString + ", claims: " + jwtSigner.getJwt().getClaims().toJsonString());

            logoutStatusJwt.setCode(jwtString);

            return logoutStatusJwt;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create Logout Status JWT", e);
            return null;
        }
    }

    public String fillPayload(Jwt jwt, Integer index, Integer lifetime, String sessionDn) {
        Date now = new Date();
        Calendar expirationCalendar = Calendar.getInstance();
        expirationCalendar.setTime(now);
        expirationCalendar.add(Calendar.SECOND, lifetime);

        final JSONObject statusList = new JSONObject();
        statusList.put("idx", index);
        statusList.put("uri", discoveryService.getTokenStatusListEndpoint());

        jwt.getClaims().setExpirationTime(expirationCalendar.getTime());
        jwt.getClaims().setIat(now);
        jwt.getClaims().setNbf(now);

        final String jti = UUID.randomUUID().toString();
        // for now we don't need "sid" - left code because maybe later we change our mind
//        if (isNotBlank(sessionDn)) {
//            final SessionId sessionByDn = sessionIdService.getSessionByDn(sessionDn);
//            if (sessionByDn != null && isNotBlank(sessionByDn.getOutsideSid())) {
//                jwt.getClaims().setClaim("sid", sessionByDn.getOutsideSid());
//            } else {
//                log.trace("Failed to load session by DN {} or outside sid is blank.", sessionByDn);
//            }
//        }
        jwt.getClaims().setClaim("jti", jti);
        jwt.getClaims().setClaim("status_list", statusList);
        return jti;
    }
}
