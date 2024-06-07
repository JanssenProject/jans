package io.jans.as.server.service.token;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.service.DiscoveryService;
import io.jans.as.server.service.cluster.TokenPoolService;
import io.jans.model.token.TokenPool;
import io.jans.model.tokenstatus.StatusList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
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
    private StatusListIndexService statusListIndexService;

    @Inject
    private TokenPoolService tokenPoolService;

    public Response requestStatusList(String acceptHeader) {
        log.debug("Attempting to request token_status_list, acceptHeader: {} ...", acceptHeader);

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.TOKEN_STATUS_LIST);

        try {
            final List<TokenPool> pools = tokenPoolService.getAllTokenPools();
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

    private String createEntity(boolean isJsonRequested, StatusList statusList) throws IOException {
        if (isJsonRequested) {
            return asJson(statusList);
        }

        return ""; // todo JWT token
    }

    public String asJson(StatusList statusList) throws IOException {
        JSONObject jsonObject = new JSONObject(statusList.encodeAsJSON());
        return jsonObject.toString();
    }

    public StatusList join(List<TokenPool> pools) {
        final int bitSize = appConfiguration.getStatusListBitSize();

        StatusList result = new StatusList(bitSize);
        for (TokenPool pool : pools) {
            try {
                StatusList poolStatusList = StatusList.fromEncoded(pool.getData(), bitSize);
                for (int i = 0; i < poolStatusList.getBitSetLength(); i++) {
                    result.set(pool.getId() + i, poolStatusList.get(i));
                }

            } catch (Exception e) {
                String msg = String.format("Failed to process status list from pool: %s, nodeId: %s", pool.getId(), pool.getNodeId());
                log.error(msg, e);
            }
        }
        return result;
    }

    public void addStatusClaimWithIndex(JsonWebResponse jwr) {
        if (!errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.TOKEN_STATUS_LIST)) {
            log.trace("Skipped status claim addition because {} feature flag is disabled.", FeatureFlagType.TOKEN_STATUS_LIST.getValue());
            return;
        }

        final JSONObject indexAndUri = new JSONObject();
        indexAndUri.put("idx", statusListIndexService.nextIndex());
        indexAndUri.put("uri", discoveryService.getTokenStatusListEndpoint());

        final JSONObject statusList = new JSONObject();
        statusList.put("status_list", indexAndUri);

        jwr.getClaims().setClaim("status", statusList);
    }
}
