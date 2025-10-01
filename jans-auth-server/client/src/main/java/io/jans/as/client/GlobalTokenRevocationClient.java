package io.jans.as.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.as.model.common.SubId;
import io.jans.as.model.revoke.GlobalTokenRevocationRequest;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import org.apache.log4j.Logger;

/**
 * @author Yuriy Z
 */
public class GlobalTokenRevocationClient extends BaseClient<GlobalTokenRevocationClientRequest, GlobalTokenRevocationResponse> {

    private static final Logger LOG = Logger.getLogger(GlobalTokenRevocationClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Constructs a client by providing a REST url where the global token revocation endpoint
     * is located.
     *
     * @param url global token revocation endpoint
     */
    public GlobalTokenRevocationClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public GlobalTokenRevocationResponse exec(GlobalTokenRevocationClientRequest request) {
        setRequest(request);
        return exec();
    }

    public GlobalTokenRevocationResponse exec() {
        initClient();

        Invocation.Builder clientRequest = webTarget.request();
        applyCookies(clientRequest);

        new ClientAuthnEnabler(clientRequest, requestForm).exec(request);

        clientRequest.header("Content-Type", request.getContentType());

        SubId subId = new SubId();
        subId.setFormat(getRequest().getFormat());
        subId.setId(getRequest().getId());

        GlobalTokenRevocationRequest model = new GlobalTokenRevocationRequest();
        model.setSubId(subId);

        try {
            clientResponse = clientRequest.buildPost(Entity.json(MAPPER.writeValueAsString(model))).invoke();

            final GlobalTokenRevocationResponse response = new GlobalTokenRevocationResponse(clientResponse);
            setResponse(response);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}
