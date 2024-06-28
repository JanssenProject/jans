package io.jans.as.client;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Invocation;
import org.apache.log4j.Logger;

/**
 * @author Yuriy Z
 */
public class StatusListClient extends BaseClient<StatusListRequest, StatusListResponse> {

    private static final Logger LOG = Logger.getLogger(StatusListClient.class);

    /**
     * Constructs a client for status list.
     *
     * @param url status list endpoint
     */
    public StatusListClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public StatusListResponse exec(StatusListRequest request) {
        setRequest(request);
        return exec();
    }

    public StatusListResponse exec() {
        initClient();

        Invocation.Builder clientRequest = webTarget.request();
        applyCookies(clientRequest);

        clientRequest.header("Content-Type", request.getContentType());

        try {
            clientResponse = clientRequest.buildGet().invoke();

            final StatusListResponse response = new StatusListResponse(clientResponse);
            setResponse(response);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}
