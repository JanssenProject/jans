/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa.validate;

import io.jans.as.client.BaseClient;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Invocation.Builder;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class SsaValidateClient extends BaseClient<SsaValidateRequest, SsaValidateResponse> {

    private static final Logger LOG = Logger.getLogger(SsaValidateClient.class);

    public SsaValidateClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.HEAD;
    }

    public SsaValidateResponse execSsaValidate(@NotNull String jti) {
        SsaValidateRequest ssaGetRequest = new SsaValidateRequest();
        ssaGetRequest.setJti(jti);
        setRequest(ssaGetRequest);
        return exec();
    }

    public SsaValidateResponse exec() {
        try {
            initClient();

            Builder clientRequest = webTarget.request();
            applyCookies(clientRequest);

            clientRequest.header("Content-Type", request.getContentType());
            clientRequest.header("jti", request.getJti());

            clientResponse = clientRequest.build(HttpMethod.HEAD).invoke();
            final SsaValidateResponse res = new SsaValidateResponse(clientResponse);
            setResponse(res);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}

