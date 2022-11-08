/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa.revoke;

import io.jans.as.client.BaseClient;
import io.jans.as.model.config.Constants;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Invocation.Builder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class SsaRevokeClient extends BaseClient<SsaRevokeRequest, SsaRevokeResponse> {

    private static final Logger LOG = Logger.getLogger(SsaRevokeClient.class);

    public SsaRevokeClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.DELETE;
    }

    public SsaRevokeResponse execSsaRevoke(String accessToken, String jti, Long orgId) {
        SsaRevokeRequest req = new SsaRevokeRequest();
        req.setAccessToken(accessToken);
        req.setJti(jti);
        req.setOrgId(orgId);
        setRequest(req);
        return exec();
    }

    public SsaRevokeResponse exec() {
        try {
            initClient();

            String uriWithParams = getUrl() + "?" + getRequest().getQueryString();
            Builder clientRequest = resteasyClient.target(uriWithParams).request();
            applyCookies(clientRequest);

            clientRequest.header("Content-Type", request.getContentType());
            if (StringUtils.isNotBlank(request.getAccessToken())) {
                clientRequest.header(Constants.AUTHORIZATION, "Bearer ".concat(request.getAccessToken()));
            }

            clientResponse = clientRequest.buildDelete().invoke();
            final SsaRevokeResponse res = new SsaRevokeResponse(clientResponse);
            setResponse(res);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}

