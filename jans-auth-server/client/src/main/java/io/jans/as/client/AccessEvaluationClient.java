package io.jans.as.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import org.apache.log4j.Logger;

/**
 * @author Yuriy Z
 */
public class AccessEvaluationClient extends BaseClient<AccessEvaluationClientRequest, AccessEvaluationClientResponse> {

    private static final Logger LOG = Logger.getLogger(AccessEvaluationClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public AccessEvaluationClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public AccessEvaluationClientResponse exec(AccessEvaluationClientRequest request) {
        setRequest(request);
        return exec();
    }

    public AccessEvaluationClientResponse exec() {
        initClient();

        Invocation.Builder clientRequest = webTarget.request();

        new ClientAuthnEnabler(clientRequest, requestForm).exec(request);

        clientRequest.header("Content-Type", request.getContentType());

        try {
            String jsonString = MAPPER.writeValueAsString(request.getRequest());
            clientResponse = clientRequest.buildPost(Entity.json(jsonString)).invoke();

            final AccessEvaluationClientResponse response = new AccessEvaluationClientResponse(clientResponse);
            setResponse(response);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}
