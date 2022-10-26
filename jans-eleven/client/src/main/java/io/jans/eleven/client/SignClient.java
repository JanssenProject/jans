/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;

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
    	ResteasyClient resteasyClient = (ResteasyClient) ClientBuilder.newClient();
    	WebTarget webTarget = resteasyClient.target(url);

        Builder clientRequest = webTarget.request();

        clientRequest.header("Content-Type", getRequest().getMediaType());
        if (!Strings.isNullOrEmpty(getRequest().getAccessToken())) {
            clientRequest.header("Authorization", "Bearer " + getRequest().getAccessToken());
        }

        // Call REST Service and handle response
        if (HttpMethod.POST.equals(request.getHttpMethod())) {
        	String body = "{}";
            if (getRequest().getSignRequestParam() != null) {
                body = toPrettyJson(getRequest().getSignRequestParam());
            }
            clientResponse = clientRequest.buildPost(Entity.entity(body, getRequest().getMediaType())).invoke();
        } else {
            clientResponse = clientRequest.buildGet().invoke();
        }

        try {
        	setResponse(new SignResponse(clientResponse));
        } finally {
        	clientResponse.close();
        }

        return getResponse();
    }

}
