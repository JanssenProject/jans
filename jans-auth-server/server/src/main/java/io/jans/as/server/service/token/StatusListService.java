package io.jans.as.server.service.token;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.service.DiscoveryService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@Named
public class StatusListService {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private DiscoveryService discoveryService;

    @Inject
    private StatusListIndexService statusListIndexService;

    public Response requestStatusList(String acceptHeader) {
        log.debug("Attempting to request token_status_list, acceptHeader: {} ...", acceptHeader);

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.TOKEN_STATUS_LIST);

        // todo WIP
        throw new UnsupportedOperationException("WIP");
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
