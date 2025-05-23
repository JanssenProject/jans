package io.jans.as.server.service.session;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.as.server.service.DiscoveryService;
import io.jans.model.token.SessionStatusIndexPool;
import io.jans.model.tokenstatus.StatusList;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static io.jans.as.model.config.Constants.CONTENT_TYPE_STATUSLIST_JSON;
import static io.jans.as.model.config.Constants.CONTENT_TYPE_STATUSLIST_JWT;
import static io.jans.as.server.service.token.StatusListService.join;

/**
 * @author Yuriy Z
 */
public class SessionStatusListService {

        @Inject
        private Logger log;

        @Inject
        private AppConfiguration appConfiguration;

        @Inject
        private ErrorResponseFactory errorResponseFactory;

        @Inject
        private DiscoveryService discoveryService;

        @Inject
        private SessionStatusIndexPoolService sessionStatusIndexService;

        @Inject
        private WebKeysConfiguration webKeysConfiguration;

        public Response requestStatusList(String acceptHeader) {
            log.debug("Attempting to request session_status_list, acceptHeader: {} ...", acceptHeader);

            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.SESSION_STATUS_LIST);

            try {
                final List<SessionStatusIndexPool> pools = sessionStatusIndexService.getAllPools();
                final StatusList statusList = join(pools, appConfiguration.getSessionStatusListBitSize(), log);

                final boolean isJsonRequested = CONTENT_TYPE_STATUSLIST_JSON.equalsIgnoreCase(acceptHeader);

                final String entity = createEntity(isJsonRequested, statusList);
                final String responseType = isJsonRequested ? CONTENT_TYPE_STATUSLIST_JSON : CONTENT_TYPE_STATUSLIST_JWT;

                if (log.isTraceEnabled()) {
                    log.trace("Response entity {}, responseType {}", entity, responseType);
                }

                return Response.status(Response.Status.OK).entity(entity).type(responseType).build();
            } catch (WebApplicationException e) {
                if (log.isTraceEnabled()) {
                    log.trace(e.getMessage(), e);
                }
                throw e;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).build();
            }
        }

        private String createEntity(boolean isJsonRequested, StatusList statusList) throws Exception {
            JSONObject jsonObject = new JSONObject(statusList.encodeAsJSON());

            if (isJsonRequested) {
                return jsonObject.toString();
            }

            return createResponseJwt(jsonObject);
        }

        /**
         * Returns sub of session status list. It must be equal in both issued token jwt ("uri") and status list jwt ("sub").
         *
         * @return Returns sub of session status list
         */
        public String getSub() {
            return discoveryService.getSessionStatusListEndpoint();
        }

        public String createResponseJwt(JSONObject response) throws Exception {
            log.trace("Creating session status list JWT response {} ...", response);

            final JwtSigner jwtSigner = newJwtSigner();
            final Jwt jwt = jwtSigner.newJwt();
            jwt.getHeader().setType(JwtType.STATUS_LIST_JWT);

            fillPayload(jwt, response);
            final String jwtString = jwtSigner.sign().toString();
            log.trace("Created session status list JWT response {}", jwtString);
            return jwtString;
        }

        private JwtSigner newJwtSigner() {
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(appConfiguration.getDefaultSignatureAlgorithm());
            if (appConfiguration.getStatusListResponseJwtSignatureAlgorithm() != null) {
                signatureAlgorithm = SignatureAlgorithm.fromString(appConfiguration.getStatusListResponseJwtSignatureAlgorithm());
            }

            return new JwtSigner(appConfiguration, webKeysConfiguration, signatureAlgorithm, "", null);
        }

        public void fillPayload(JsonWebResponse jwr, JSONObject response) throws InvalidJwtException {
            final int lifetime = appConfiguration.getStatusListResponseJwtLifetime();

            final Calendar calendar = Calendar.getInstance();
            final Date issuedAt = calendar.getTime();
            calendar.add(Calendar.SECOND, lifetime);
            final Date expiration = calendar.getTime();

            jwr.getClaims().setExpirationTime(expiration);
            jwr.getClaims().setIat(issuedAt);
            jwr.getClaims().setNbf(issuedAt);
            jwr.getClaims().setClaim("ttl", lifetime);
            jwr.getClaims().setClaim("sub", getSub());

            try {
                jwr.getClaims().setClaim("status_list", response);
            } catch (Exception e) {
                log.error("Failed to put claims into session status list jwt. Key: status_list, response: " + response.toString(), e);
            }

            if (log.isTraceEnabled()) {
                log.trace("Response before signing: {}", jwr.getClaims().toJsonString());
            }
        }
}
