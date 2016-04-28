/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import com.google.common.base.Strings;
import org.jboss.resteasy.client.ClientRequest;

import javax.ws.rs.HttpMethod;

import static org.gluu.oxeleven.model.GenerateKeyRequestParam.EXPIRATION_TIME;
import static org.gluu.oxeleven.model.GenerateKeyRequestParam.SIGNATURE_ALGORITHM;

/**
 * @author Javier Rojas Blum
 * @version April 27, 2016
 */
public class GenerateKeyClient extends BaseClient<GenerateKeyRequest, GenerateKeyResponse> {

    public GenerateKeyClient(String url) {
        super(url);
    }

    @Override
    public GenerateKeyRequest getRequest() {
        if (request instanceof GenerateKeyRequest) {
            return (GenerateKeyRequest) request;
        } else {
            return null;
        }
    }

    @Override
    public void setRequest(GenerateKeyRequest request) {
        super.request = request;
    }

    @Override
    public GenerateKeyResponse getResponse() {
        if (response instanceof GenerateKeyResponse) {
            return (GenerateKeyResponse) response;
        } else {
            return null;
        }
    }

    @Override
    public void setResponse(GenerateKeyResponse response) {
        super.response = response;
    }

    @Override
    public GenerateKeyResponse exec() throws Exception {
        clientRequest = new ClientRequest(url);
        clientRequest.header("Content-Type", getRequest().getMediaType());
        clientRequest.setHttpMethod(getRequest().getHttpMethod());

        if (!Strings.isNullOrEmpty(getRequest().getSignatureAlgorithm())) {
            addRequestParam(SIGNATURE_ALGORITHM, getRequest().getSignatureAlgorithm());
        }
        if (getRequest().getExpirationTime() != null) {
            addRequestParam(EXPIRATION_TIME, getRequest().getExpirationTime());
        }

        // Call REST Service and handle response
        if (HttpMethod.POST.equals(request.getHttpMethod())) {
            clientResponse = clientRequest.post(String.class);
        } else {
            clientResponse = clientRequest.get(String.class);
        }

        setResponse(new GenerateKeyResponse(clientResponse));

        return getResponse();
    }
}
