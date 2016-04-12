/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import org.jboss.resteasy.client.ClientRequest;

import javax.ws.rs.HttpMethod;

/**
 * @author Javier Rojas Blum
 * @version April 12, 2016
 */
public class JwksClient extends BaseClient<JwksRequest, JwksResponse> {

    public JwksClient(String url) {
        super(url);
    }

    @Override
    public JwksRequest getRequest() {
        if (request instanceof JwksRequest) {
            return (JwksRequest) request;
        } else {
            return null;
        }
    }

    @Override
    public void setRequest(JwksRequest request) {
        super.request = request;
    }

    @Override
    public JwksResponse getResponse() {
        if (response instanceof JwksResponse) {
            return (JwksResponse) response;
        } else {
            return null;
        }
    }

    @Override
    public void setResponse(JwksResponse response) {
        super.response = response;
    }

    @Override
    public JwksResponse exec() throws Exception {
        clientRequest = new ClientRequest(url);
        clientRequest.header("Content-Type", getRequest().getMediaType());
        clientRequest.setHttpMethod(getRequest().getHttpMethod());

        if (getRequest().getJwks() != null) {
            clientRequest.body(getRequest().getMediaType(), getRequest().getJwks());
        }

        // Call REST Service and handle response
        if (HttpMethod.POST.equals(request.getHttpMethod())) {
            clientResponse = clientRequest.post(String.class);
        } else {
            clientResponse = clientRequest.get(String.class);
        }

        setResponse(new JwksResponse(clientResponse));

        return getResponse();
    }
}
