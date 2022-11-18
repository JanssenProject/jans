/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa.get;

import io.jans.as.client.BaseClient;
import io.jans.as.model.config.Constants;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Invocation.Builder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class SsaGetClient extends BaseClient<SsaGetRequest, SsaGetResponse> {

    private static final Logger LOG = Logger.getLogger(SsaGetClient.class);

    public SsaGetClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.GET;
    }

    public SsaGetResponse execSsaGet(String accessToken, String jti, Long orgId) {
        SsaGetRequest ssaGetRequest = new SsaGetRequest();
        ssaGetRequest.setAccessToken(accessToken);
        ssaGetRequest.setJti(jti);
        ssaGetRequest.setOrgId(orgId);
        setRequest(ssaGetRequest);
        return exec();
    }

    public SsaGetResponse exec() {
        try {
            initClient();

            String uriWithParams = getUrl() + "?" + getRequest().getQueryString();
            Builder clientRequest = resteasyClient.target(uriWithParams).request();
            applyCookies(clientRequest);

            clientRequest.header("Content-Type", request.getContentType());
            if (StringUtils.isNotBlank(request.getAccessToken())) {
                clientRequest.header(Constants.AUTHORIZATION, "Bearer ".concat(request.getAccessToken()));
            }

            clientResponse = clientRequest.buildGet().invoke();
            final SsaGetResponse ssaGetResponse = new SsaGetResponse(clientResponse);
            ssaGetResponse.injectDataFromJson();
            setResponse(ssaGetResponse);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}

