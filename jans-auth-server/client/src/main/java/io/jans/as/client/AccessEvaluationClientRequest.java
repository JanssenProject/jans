package io.jans.as.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.as.model.util.QueryBuilder;
import io.jans.model.authzen.AccessEvaluationRequest;
import org.apache.log4j.Logger;

/**
 * @author Yuriy Z
 */
public class AccessEvaluationClientRequest extends ClientAuthnRequest implements IsJsonRequest {

    private static final Logger LOG = Logger.getLogger(AccessEvaluationClientRequest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AccessEvaluationRequest request;

    public AccessEvaluationClientRequest() {
        setContentType("application/json");
    }

    public AccessEvaluationRequest getRequest() {
        return request;
    }

    public AccessEvaluationClientRequest setRequest(AccessEvaluationRequest request) {
        this.request = request;
        return this;
    }

    @Override
    public String getQueryString() {
        QueryBuilder builder = QueryBuilder.instance();
        if (request == null) {
            return builder.toString();
        }

        appendClientAuthnToQuery(builder);
        for (String key : getCustomParameters().keySet()) {
            builder.append(key, getCustomParameters().get(key));
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return "AccessEvaluationClientRequest{" +
                "request=" + request +
                "} " + super.toString();
    }

    @Override
    public String asJson() {
        try {
            return request != null ? MAPPER.writeValueAsString(request) : "";
        } catch (JsonProcessingException e) {
            LOG.error("Failed to serialize request", e);
            return "";
        }
    }
}
