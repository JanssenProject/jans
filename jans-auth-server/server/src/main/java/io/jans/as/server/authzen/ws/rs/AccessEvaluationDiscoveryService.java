package io.jans.as.server.authzen.ws.rs;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.DiscoveryService;
import io.jans.as.server.service.LocalResponseCache;
import io.jans.as.server.service.external.ExternalAccessEvaluationDiscoveryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.json.JSONObject;
import org.slf4j.Logger;

import static io.jans.as.model.configuration.ConfigurationResponseClaim.ACCESS_EVALUATION_V1_ENDPOINT;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class AccessEvaluationDiscoveryService {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private LocalResponseCache localResponseCache;

    @Inject
    private ExternalAccessEvaluationDiscoveryService externalAccessEvaluationDiscoveryService;

    public JSONObject discovery(ExecutionContext context) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);

        final JSONObject cachedResponse = localResponseCache.getAccessEvaluationDiscoveryResponse();
        if (cachedResponse != null) {
            log.trace("Cached access evaluation discovery response returned.");
            return cachedResponse;
        }

        JSONObject jsonObj = createResponse();
        JSONObject clone = new JSONObject(jsonObj.toString());

        if (!externalAccessEvaluationDiscoveryService.modifyDiscovery(jsonObj, context)) {
            jsonObj = clone; // revert to original state if object was modified in script
        }

        localResponseCache.putAccessEvaluationDiscoveryResponse(jsonObj);
        return jsonObj;
    }

    private JSONObject createResponse() {
        JSONObject jsonObj = new JSONObject();
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION))
            jsonObj.put(ACCESS_EVALUATION_V1_ENDPOINT, DiscoveryService.getAccessEvaluationV1Endpoint(appConfiguration));

        return jsonObj;
    }
}
