/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import com.google.common.base.Strings;
import org.jboss.resteasy.client.ClientRequest;

import javax.ws.rs.HttpMethod;

import static org.gluu.oxeleven.model.VerifySignatureRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version April 18, 2016
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

        if (getRequest().getVerifySignatureRequestParam() != null) {
            clientRequest.body(getRequest().getMediaType(), getRequest().getVerifySignatureRequestParam());
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
