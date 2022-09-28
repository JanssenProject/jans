/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.config.Constants;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.List;

public class SsaClient extends BaseClient<SsaRequest, SsaResponse> {

    private static final Logger LOG = Logger.getLogger(SsaClient.class);

    public SsaClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public SsaResponse execSsaCreate(String accessToken, Long orgId, Long expirationDate, String description, String softwareId, List<String> softwareRoles, List<String> grantTypes) {
        setRequest(new SsaRequest());
        getRequest().setAccessToken(accessToken);
        getRequest().setOrgId(orgId);
        getRequest().setExpiration(expirationDate);
        getRequest().setDescription(description);
        getRequest().setSoftwareId(softwareId);
        getRequest().setSoftwareRoles(softwareRoles);
        getRequest().setGrantTypes(grantTypes);
        return exec();
    }

    public SsaResponse exec() {
        try {
            initClient();

            Builder clientRequest = webTarget.request();
            applyCookies(clientRequest);

            clientRequest.header("Content-Type", request.getContentType());
            if (StringUtils.isNotBlank(request.getAccessToken())) {
                clientRequest.header(Constants.AUTHORIZATION, "Bearer ".concat(request.getAccessToken()));
            }

            JSONObject requestBody = getRequest().getJSONParameters();
            clientResponse = clientRequest.buildPost(Entity.json(requestBody.toString(4))).invoke();
            final SsaResponse ssaResponse = new SsaResponse(clientResponse);
            ssaResponse.injectDataFromJson();
            setResponse(ssaResponse);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}

