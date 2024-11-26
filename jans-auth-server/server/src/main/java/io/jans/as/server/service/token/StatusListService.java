package io.jans.as.server.service.token;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.as.server.service.DiscoveryService;
import io.jans.as.server.service.cluster.StatusIndexPoolService;
import io.jans.model.token.StatusIndexPool;
import io.jans.model.tokenstatus.StatusList;
import io.jans.model.tokenstatus.TokenStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static io.jans.as.model.config.Constants.CONTENT_TYPE_STATUSLIST_JSON;
import static io.jans.as.model.config.Constants.CONTENT_TYPE_STATUSLIST_JWT;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class StatusListService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private DiscoveryService discoveryService;

    @Inject
    private StatusIndexPoolService statusTokenPoolService;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    public Response requestStatusList(String acceptHeader) {
        log.debug("Attempting to request status_list, acceptHeader: {} ...", acceptHeader);

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.STATUS_LIST);

        try {
            final List<StatusIndexPool> pools = statusTokenPoolService.getAllPools();
            final StatusList statusList = join(pools);

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

    public StatusList join(List<StatusIndexPool> pools) {
        final int bitSize = appConfiguration.getStatusListBitSize();

        StatusList result = new StatusList(bitSize);
        for (StatusIndexPool pool : pools) {
            try {
                final String data = pool.getData();
                if (StringUtils.isBlank(data)) {
                    continue;
                }

                StatusList poolStatusList = StatusList.fromEncoded(data, bitSize);
                for (int i = 0; i < poolStatusList.getBitSetLength(); i++) {
                    int value = poolStatusList.get(i);
                    boolean isNotDefault = value != TokenStatus.VALID.getValue();
                    if (isNotDefault) {
                        result.set(i, value);
                    }
                }
            } catch (Exception e) {
                String msg = String.format("Failed to process status list from pool: %s, nodeId: %s", pool.getId(), pool.getNodeId());
                log.error(msg, e);
            }
        }
        return result;
    }

    public void addStatusClaimWithIndex(JsonWebResponse jwr, ExecutionContext executionContext) {
        if (!errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)) {
            log.trace("Skipped status claim addition because {} feature flag is disabled.", FeatureFlagType.STATUS_LIST.getValue());
            return;
        }

        final Integer index = executionContext.getStatusListIndex();
        if (index == null || index < 0) {
            return; // index is not set. It must be set into context to be saved in both entity bean and jwt consistently
        }

        final JSONObject indexAndUri = new JSONObject();
        indexAndUri.put("idx", index);
        indexAndUri.put("uri", getSub());

        final JSONObject statusList = new JSONObject();
        statusList.put("status_list", indexAndUri);

        jwr.getClaims().setClaim("status", statusList);
    }

    /**
     * Returns sub of status list. It must be equal in both issued token jwt ("uri") and status list jwt ("sub").
     *
     * @return Returns sub of status list
     */
    public String getSub() {
        return discoveryService.getTokenStatusListEndpoint();
    }

    public String createResponseJwt(JSONObject response) throws Exception {
        log.trace("Creating status list JWT response {} ...", response);

        final JwtSigner jwtSigner = newJwtSigner();
        final Jwt jwt = jwtSigner.newJwt();
        jwt.getHeader().setType(JwtType.STATUS_LIST_JWT);

        fillPayload(jwt, response);
        final String jwtString = jwtSigner.sign().toString();
        log.trace("Created status list JWT response {}", jwtString);
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
            log.error("Failed to put claims into status list jwt. Key: status_list, response: " + response.toString(), e);
        }

        if (log.isTraceEnabled()) {
            log.trace("Response before signing: {}", jwr.getClaims().toJsonString());
        }
    }
}
