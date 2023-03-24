/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa.jwtssa;

import io.jans.as.client.BaseClient;
import io.jans.as.model.config.Constants;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Invocation.Builder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class SsaGetJwtClient extends BaseClient<SsaGetJwtRequest, SsaGetJwtResponse> {

    private static final Logger log = Logger.getLogger(SsaGetJwtClient.class);

    public SsaGetJwtClient(String url) {
        super(url + "/jwt");
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.GET;
    }

    public SsaGetJwtResponse execGetJwtSsa(@NotNull String accessToken, @NotNull String jti) {
        SsaGetJwtRequest ssaGetRequest = new SsaGetJwtRequest();
        ssaGetRequest.setJti(jti);
        ssaGetRequest.setAccessToken(accessToken);
        setRequest(ssaGetRequest);
        return exec();
    }

    public SsaGetJwtResponse exec() {
        try {
            initClient();
            String uriWithParams = getUrl() + "?" + getRequest().getQueryString();
            Builder clientRequest = resteasyClient.target(uriWithParams).request();
            applyCookies(clientRequest);

            clientRequest.header("Content-Type", request.getContentType());
            if (StringUtils.isNotBlank(request.getAccessToken())) {
                clientRequest.header(Constants.AUTHORIZATION, "Bearer ".concat(request.getAccessToken()));
            }

            clientResponse = clientRequest.build(getHttpMethod()).invoke();
            final SsaGetJwtResponse res = new SsaGetJwtResponse(clientResponse);
            res.injectDataFromJson();
            setResponse(res);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}

