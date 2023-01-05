/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import static io.jans.eleven.model.GenerateKeyRequestParam.EXPIRATION_TIME;
import static io.jans.eleven.model.GenerateKeyRequestParam.SIGNATURE_ALGORITHM;

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
public class GenerateKeyClient extends BaseClient<GenerateKeyRequest, GenerateKeyResponse> {

    public GenerateKeyClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
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
    	ResteasyClient resteasyClient = (ResteasyClient) ClientBuilder.newClient();
    	WebTarget webTarget = resteasyClient.target(url);

        Builder clientRequest = webTarget.request();

        clientRequest.header("Content-Type", getRequest().getMediaType());
        if (!Strings.isNullOrEmpty(getRequest().getAccessToken())) {
            clientRequest.header("Authorization", "Bearer " + getRequest().getAccessToken());
        }

        if (!Strings.isNullOrEmpty(getRequest().getSignatureAlgorithm())) {
            addRequestParam(SIGNATURE_ALGORITHM, getRequest().getSignatureAlgorithm());
        }
        if (getRequest().getExpirationTime() != null) {
            addRequestParam(EXPIRATION_TIME, getRequest().getExpirationTime());
        }

        // Call REST Service and handle response
        if (HttpMethod.POST.equals(request.getHttpMethod())) {
            clientResponse = clientRequest.buildPost(Entity.entity("{}", getRequest().getMediaType())).invoke();
        } else {
            clientResponse = clientRequest.buildGet().invoke();
        }

        try {
        	setResponse(new GenerateKeyResponse(clientResponse));
        } finally {
        	clientResponse.close();
        }

        return getResponse();
    }
}
