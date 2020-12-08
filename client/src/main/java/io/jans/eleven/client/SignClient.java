/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import javax.ws.rs.HttpMethod;

import org.jboss.resteasy.client.ClientRequest;

import com.google.common.base.Strings;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2017
 */
public class SignClient extends BaseClient<SignRequest, SignResponse> {

    public SignClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
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
        if (!Strings.isNullOrEmpty(getRequest().getAccessToken())) {
            clientRequest.header("Authorization", "Bearer " + getRequest().getAccessToken());
        }

        if (getRequest().getSignRequestParam() != null) {
            clientRequest.body(getRequest().getMediaType(), toPrettyJson(getRequest().getSignRequestParam()));
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
