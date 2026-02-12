package io.jans.as.server.authzen.ws.rs;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.LocalResponseCache;
import io.jans.as.server.service.external.ExternalAccessEvaluationDiscoveryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.Strings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import static io.jans.as.model.configuration.ConfigurationResponseClaim.*;

/**
 * AuthZEN PDP Metadata Discovery Service.
 * Builds response per AuthZEN spec.
 *
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
        if (!appConfiguration.isFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION)) {
            return jsonObj;
        }

        String baseEndpoint = Strings.CS.removeEnd(appConfiguration.getEndSessionEndpoint(), "/end_session");

        fillJsonObject(jsonObj, appConfiguration.getIssuer(), baseEndpoint);


        return jsonObj;
    }

    public static void fillJsonObject(JSONObject jsonObj, String issuer, String baseEndpoint) {
        jsonObj.put(AUTHZEN_POLICY_DECISION_POINT, issuer);
        jsonObj.put(AUTHZEN_ACCESS_EVALUATION_ENDPOINT, baseEndpoint + "/access/v1/evaluation");
        jsonObj.put(AUTHZEN_ACCESS_EVALUATIONS_ENDPOINT, baseEndpoint + "/access/v1/evaluations");
        jsonObj.put(AUTHZEN_SEARCH_SUBJECT_ENDPOINT, baseEndpoint + "/access/v1/search/subject");
        jsonObj.put(AUTHZEN_SEARCH_RESOURCE_ENDPOINT, baseEndpoint + "/access/v1/search/resource");
        jsonObj.put(AUTHZEN_SEARCH_ACTION_ENDPOINT, baseEndpoint + "/access/v1/search/action");

        // Capabilities - can be extended via custom script
        JSONArray capabilities = new JSONArray();
        jsonObj.put(AUTHZEN_CAPABILITIES, capabilities);

        // Keep legacy field for backward compatibility
        jsonObj.put(ACCESS_EVALUATION_V1_ENDPOINT, baseEndpoint + "/access/v1/evaluation");
    }
}
