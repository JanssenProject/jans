/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import static io.jans.eleven.model.DeleteKeyRequestParam.KEY_ID;

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
public class DeleteKeyClient extends BaseClient<DeleteKeyRequest, DeleteKeyResponse> {

    public DeleteKeyClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
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
    	ResteasyClient resteasyClient = (ResteasyClient) ClientBuilder.newClient();
    	WebTarget webTarget = resteasyClient.target(url);

        Builder clientRequest = webTarget.request();

        addRequestParam(KEY_ID, getRequest().getAlias());

        clientRequest.header("Content-Type", getRequest().getMediaType());
        if (!Strings.isNullOrEmpty(getRequest().getAccessToken())) {
            clientRequest.header("Authorization", "Bearer " + getRequest().getAccessToken());
        }

        // Call REST Service and handle response
        if (HttpMethod.POST.equals(request.getHttpMethod())) {
        	clientResponse = clientRequest.buildPost(Entity.entity("{}", getRequest().getMediaType())).invoke();
        } else {
            clientResponse = clientRequest.buildGet().invoke();
        }

        try {
        	setResponse(new DeleteKeyResponse(clientResponse));
        } finally {
        	clientResponse.close();
        }

        return getResponse();
    }
}
