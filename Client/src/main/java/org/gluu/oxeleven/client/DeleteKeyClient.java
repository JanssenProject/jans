package org.gluu.oxeleven.client;

import org.jboss.resteasy.client.ClientRequest;

import javax.ws.rs.HttpMethod;

/**
 * @author Javier Rojas Blum
 * @version March 29, 2016
 */
public class DeleteKeyClient extends BaseClient<DeleteKeyRequest, DeleteKeyResponse> {

    public DeleteKeyClient(String url) {
        super(url);
    }

    @Override
    public DeleteKeyRequest getRequest() {
        if (request instanceof DeleteKeyRequest) {
            return (DeleteKeyRequest) request;
        } else {
            return null;
        }
    }

    @Override
    public void setRequest(DeleteKeyRequest request) {
        super.request = request;
    }

    @Override
    public DeleteKeyResponse getResponse() {
        if (response instanceof DeleteKeyResponse) {
            return (DeleteKeyResponse) response;
        } else {
            return null;
        }
    }

    @Override
    public void setResponse(DeleteKeyResponse response) {
        super.response = response;
    }

    @Override
    public DeleteKeyResponse exec() throws Exception {
        clientRequest = new ClientRequest(url);
        clientRequest.header("Content-Type", getRequest().getMediaType());
        clientRequest.setHttpMethod(getRequest().getHttpMethod());

        addRequestParam("alias", getRequest().getAlias());

        // Call REST Service and handle response
        if (HttpMethod.POST.equals(request.getHttpMethod())) {
            clientResponse = clientRequest.post(String.class);
        } else {
            clientResponse = clientRequest.get(String.class);
        }

        setResponse(new DeleteKeyResponse(clientResponse));

        return getResponse();
    }
}
