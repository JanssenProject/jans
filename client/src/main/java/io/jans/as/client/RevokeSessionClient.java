/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;

import org.apache.log4j.Logger;

/**
 * @author Yuriy Zabrovarnyy
 */
public class RevokeSessionClient extends BaseClient<RevokeSessionRequest, RevokeSessionResponse>{

    private static final Logger LOG = Logger.getLogger(RevokeSessionClient.class);

    /**
     * Constructs a token client by providing a REST url where the token service
     * is located.
     *
     * @param url The REST Service location.
     */
    public RevokeSessionClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public RevokeSessionResponse exec(RevokeSessionRequest request) {
        setRequest(request);
        return exec();
    }

    public RevokeSessionResponse exec() {
        initClientRequest();

        Builder clientRequest = webTarget.request();
        applyCookies(clientRequest);

        new ClientAuthnEnabler(clientRequest, requestForm).exec(request);

        clientRequest.header("Content-Type", request.getContentType());
//        clientRequest.setHttpMethod(getHttpMethod());

        if (getRequest().getUserCriterionKey() != null) {
            requestForm.param("user_criterion_key", getRequest().getUserCriterionKey());
        }
        if (getRequest().getUserCriterionValue() != null) {
            requestForm.param("user_criterion_value", getRequest().getUserCriterionValue());
        }

        for (String key : getRequest().getCustomParameters().keySet()) {
            requestForm.param(key, getRequest().getCustomParameters().get(key));
        }

        try {
            clientResponse = clientRequest.buildPost(Entity.form(requestForm)).invoke();

            final RevokeSessionResponse response = new RevokeSessionResponse(clientResponse);
            setResponse(response);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}
