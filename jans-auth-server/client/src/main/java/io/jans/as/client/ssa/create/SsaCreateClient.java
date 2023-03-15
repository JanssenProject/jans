/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa.create;

import io.jans.as.client.BaseClient;
import io.jans.as.model.config.Constants;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.List;

public class SsaCreateClient extends BaseClient<SsaCreateRequest, SsaCreateResponse> {

    private static final Logger LOG = Logger.getLogger(SsaCreateClient.class);

    public SsaCreateClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public SsaCreateResponse execSsaCreate(String accessToken, String orgId, Long expirationDate, String description,
                                           String softwareId, List<String> softwareRoles, List<String> grantTypes,
                                           Boolean oneTimeUse, Boolean rotateSsa) {
        SsaCreateRequest ssaCreateRequest = new SsaCreateRequest();
        ssaCreateRequest.setAccessToken(accessToken);
        ssaCreateRequest.setOrgId(orgId);
        ssaCreateRequest.setExpiration(expirationDate);
        ssaCreateRequest.setDescription(description);
        ssaCreateRequest.setSoftwareId(softwareId);
        ssaCreateRequest.setSoftwareRoles(softwareRoles);
        ssaCreateRequest.setGrantTypes(grantTypes);
        ssaCreateRequest.setOneTimeUse(oneTimeUse);
        ssaCreateRequest.setRotateSsa(rotateSsa);
        setRequest(ssaCreateRequest);
        return exec();
    }

    public SsaCreateResponse exec() {
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
            final SsaCreateResponse ssaCreateResponse = new SsaCreateResponse(clientResponse);
            ssaCreateResponse.injectDataFromJson();
            setResponse(ssaCreateResponse);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}

