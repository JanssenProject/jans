/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static io.jans.as.model.configuration.ConfigurationResponseClaim.AUTH_LEVEL_MAPPING;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.ID_GENERATION_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.INTROSPECTION_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.PAR_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.REQUIRE_PAR;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.SCOPE_TO_CLAIMS_MAPPING;

/**
 * Created by eugeniuparvan on 8/12/16.
 *
 * @version December 26, 2016
 */
public class GluuConfigurationClient extends BaseClient<GluuConfigurationRequest, GluuConfigurationResponse> {

    private static final Logger LOG = Logger.getLogger(GluuConfigurationClient.class);

    public GluuConfigurationClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.GET;
    }

    public GluuConfigurationResponse execGluuConfiguration() {
        initClient();

        setRequest(new GluuConfigurationRequest());

        Builder clientRequest = webTarget.request();
        applyCookies(clientRequest);

        // Prepare request parameters
        clientRequest.header("Content-Type", MediaType.APPLICATION_JSON);

        // Call REST Service and handle response
        try {
            clientResponse = clientRequest.buildGet().invoke();

            setResponse(new GluuConfigurationResponse());

            String entity = clientResponse.readEntity(String.class);
            getResponse().setEntity(entity);
            getResponse().setHeaders(clientResponse.getMetadata());
            getResponse().setStatus(clientResponse.getStatus());

            if (StringUtils.isNotBlank(entity)) {
                JSONObject jsonObj = new JSONObject(entity);

                if (jsonObj.has(ID_GENERATION_ENDPOINT)) {
                    getResponse().setIdGenerationEndpoint(jsonObj.getString(ID_GENERATION_ENDPOINT));
                }
                if (jsonObj.has(INTROSPECTION_ENDPOINT)) {
                    getResponse().setIntrospectionEndpoint(jsonObj.getString(INTROSPECTION_ENDPOINT));
                }
                if (jsonObj.has(PAR_ENDPOINT)) {
                    getResponse().setParEndpoint(jsonObj.getString(PAR_ENDPOINT));
                }
                if (jsonObj.has(REQUIRE_PAR)) {
                    getResponse().setRequirePar(jsonObj.getBoolean(REQUIRE_PAR));
                }
                if (jsonObj.has(AUTH_LEVEL_MAPPING)) {
                    getResponse().setAuthLevelMapping(mapJsonToAuthLevelMapping(jsonObj.getJSONObject(AUTH_LEVEL_MAPPING)));
                }
                if (jsonObj.has(SCOPE_TO_CLAIMS_MAPPING)) {
                    getResponse().setScopeToClaimsMapping(mapJsonToScopeToClaimsMapping(jsonObj.getJSONObject(SCOPE_TO_CLAIMS_MAPPING)));
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }

    private Map<Integer, Set<String>> mapJsonToAuthLevelMapping(JSONObject jsonObject) {
        Map<Integer, Set<String>> authLevelMapping = new HashMap<>();

        Iterator<?> keys = jsonObject.keys();
        while (keys.hasNext()) {
            try {
                String key = (String) keys.next();
                Integer level = new Integer(key);

                authLevelMapping.put(level, new HashSet<String>());

                JSONArray jsonArray = jsonObject.getJSONArray(key);
                for (int i = 0; i < jsonArray.length(); i++)
                    authLevelMapping.get(level).add(jsonArray.getString(i));
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return authLevelMapping;
    }

    private Map<String, Set<String>> mapJsonToScopeToClaimsMapping(JSONObject jsonObject) {
        Map<String, Set<String>> scopeToClaimsMapping = new HashMap<>();
        Iterator<?> keys = jsonObject.keys();
        while (keys.hasNext()) {
            try {
                String scope = (String) keys.next();

                scopeToClaimsMapping.put(scope, new HashSet<String>());

                JSONArray jsonArray = jsonObject.getJSONArray(scope);
                for (int i = 0; i < jsonArray.length(); i++)
                    scopeToClaimsMapping.get(scope).add(jsonArray.getString(i));
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return scopeToClaimsMapping;
    }
}