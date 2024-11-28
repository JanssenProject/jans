package io.jans.as.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.model.authzen.AccessEvaluationResponse;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author Yuriy Z
 */
public class AccessEvaluationClientResponse extends BaseResponse {

    private static final Logger LOG = Logger.getLogger(AccessEvaluationClientResponse.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AccessEvaluationResponse response;

    public AccessEvaluationClientResponse(Response clientResponse) {
        super(clientResponse);
        injectDataFromJson(entity);
    }

    public void injectDataFromJson(String json) {
        if (StringUtils.isBlank(json)) {
            return;
        }

        try {
            response = MAPPER.readValue(json, AccessEvaluationResponse.class);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to read json: " + json, e);
        }
    }

    public AccessEvaluationResponse getResponse() {
        return response;
    }

    public AccessEvaluationClientResponse setResponse(AccessEvaluationResponse response) {
        this.response = response;
        return this;
    }

    @Override
    public String toString() {
        return "AccessEvaluationClientResponse{" +
                "response=" + response +
                "} " + super.toString();
    }
}
