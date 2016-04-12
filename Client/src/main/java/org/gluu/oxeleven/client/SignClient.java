/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import com.google.common.base.Strings;
import org.jboss.resteasy.client.ClientRequest;

import javax.ws.rs.HttpMethod;

import static org.gluu.oxeleven.model.SignRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version April 12, 2016
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

        if (!Strings.isNullOrEmpty(getRequest().getSigningInput())) {
            addRequestParam(SIGNING_INPUT, getRequest().getSigningInput());
        }
        if (!Strings.isNullOrEmpty(getRequest().getAlias())) {
            addRequestParam(ALIAS, getRequest().getAlias());
        }
        if (!Strings.isNullOrEmpty(getRequest().getSignatureAlgorithm())) {
            addRequestParam(SIGNATURE_ALGORITHM, getRequest().getSignatureAlgorithm());
        }

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
