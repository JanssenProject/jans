package org.gluu.oxeleven.client;

import com.google.common.base.Strings;
import org.jboss.resteasy.client.ClientRequest;

import javax.ws.rs.HttpMethod;

import static org.gluu.oxeleven.model.VerifySignatureRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version April 4, 2016
 */
public class VerifySignatureClient extends BaseClient<VerifySignatureRequest, VerifySignatureResponse> {

    public VerifySignatureClient(String url) {
        super(url);
    }

    @Override
    public VerifySignatureRequest getRequest() {
        if (request instanceof VerifySignatureRequest) {
            return (VerifySignatureRequest) request;
        } else {
            return null;
        }
    }

    @Override
    public void setRequest(VerifySignatureRequest request) {
        super.request = request;
    }

    @Override
    public VerifySignatureResponse getResponse() {
        if (response instanceof VerifySignatureResponse) {
            return (VerifySignatureResponse) response;
        } else {
            return null;
        }
    }

    @Override
    public void setResponse(VerifySignatureResponse response) {
        super.response = response;
    }

    @Override
    public VerifySignatureResponse exec() throws Exception {
        clientRequest = new ClientRequest(url);
        clientRequest.header("Content-Type", getRequest().getMediaType());
        clientRequest.setHttpMethod(getRequest().getHttpMethod());

        if (!Strings.isNullOrEmpty(getRequest().getSigningInput())) {
            addRequestParam(SIGNING_INPUT, getRequest().getSigningInput());
        }
        if (!Strings.isNullOrEmpty(getRequest().getSignature())) {
            addRequestParam(SIGNATURE, getRequest().getSignature());
        }
        if (!Strings.isNullOrEmpty(getRequest().getAlias())) {
            addRequestParam(ALIAS, getRequest().getAlias());
        }
        if (getRequest().getSignatureAlgorithm() != null) {
            addRequestParam(SIGNATURE_ALGORITHM, getRequest().getSignatureAlgorithm().getName());
        }

        // Call REST Service and handle response
        if (HttpMethod.POST.equals(request.getHttpMethod())) {
            clientResponse = clientRequest.post(String.class);
        } else {
            clientResponse = clientRequest.get(String.class);
        }

        setResponse(new VerifySignatureResponse(clientResponse));

        return getResponse();
    }
}
