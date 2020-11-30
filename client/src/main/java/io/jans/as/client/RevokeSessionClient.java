/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import org.apache.log4j.Logger;

import javax.ws.rs.HttpMethod;

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
        new ClientAuthnEnabler(clientRequest).exec(request);

        clientRequest.header("Content-Type", request.getContentType());
        clientRequest.setHttpMethod(getHttpMethod());

        if (getRequest().getUserCriterionKey() != null) {
            clientRequest.formParameter("user_criterion_key", getRequest().getUserCriterionKey());
        }
        if (getRequest().getUserCriterionValue() != null) {
            clientRequest.formParameter("user_criterion_value", getRequest().getUserCriterionValue());
        }

        for (String key : getRequest().getCustomParameters().keySet()) {
            clientRequest.formParameter(key, getRequest().getCustomParameters().get(key));
        }

        try {
            clientResponse = clientRequest.post(String.class);

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
