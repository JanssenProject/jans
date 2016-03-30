package org.gluu.oxeleven.client;

import org.jboss.resteasy.client.ClientRequest;

import javax.ws.rs.HttpMethod;

/**
 * @author Javier Rojas Blum
 * @version March 29, 2016
 */
public class SignClient extends BaseClient<SignRequest, SignResponse> {

    public SignClient(String url) {
        super(url);
    }

    @Override
    public SignRequest getRequest() {
        if (request instanceof SignRequest) {
            return (SignRequest) request;
        } else {
            return null;
        }
    }

    @Override
    public void setRequest(SignRequest request) {
        super.request = request;
    }

    @Override
    public SignResponse getResponse() {
        if (response instanceof SignResponse) {
            return (SignResponse) response;
        } else {
            return null;
        }
    }

    @Override
    public void setResponse(SignResponse response) {
        super.response = response;
    }

    @Override
    public SignResponse exec() throws Exception {
        clientRequest = new ClientRequest(url);
        clientRequest.header("Content-Type", getRequest().getMediaType());
        clientRequest.setHttpMethod(getRequest().getHttpMethod());

        addRequestParam("signingInput", getRequest().getSigningInput());
        addRequestParam("alias", getRequest().getAlias());
        addRequestParam("signatureAlgorithm", getRequest().getSignatureAlgorithm().getName());

        // Call REST Service and handle response
        if (HttpMethod.POST.equals(request.getHttpMethod())) {
            clientResponse = clientRequest.post(String.class);
        } else {
            clientResponse = clientRequest.get(String.class);
        }

        setResponse(new SignResponse(clientResponse));

        return getResponse();
    }
}
