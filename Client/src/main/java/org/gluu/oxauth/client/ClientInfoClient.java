/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.gluu.oxauth.model.common.AuthorizationMethod;
import org.gluu.oxauth.model.userinfo.UserInfoErrorResponseType;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates functionality to make client info request calls to an authorization server via REST Services.
 *
 * @author Javier Rojas Blum
 * @version December 26, 2016
 */
public class ClientInfoClient extends BaseClient<ClientInfoRequest, ClientInfoResponse> {

    private static final Logger LOG = Logger.getLogger(ClientInfoClient.class);

    /**
     * Constructs an Client Info client by providing a REST url where the service is located.
     *
     * @param url The REST Service location.
     */
    public ClientInfoClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        if (getRequest().getAuthorizationMethod() == null
                || getRequest().getAuthorizationMethod() == AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD
                || getRequest().getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER) {
            return HttpMethod.POST;
        } else { // AuthorizationMethod.URL_QUERY_PARAMETER
            return HttpMethod.GET;
        }
    }

    /**
     * Executes the call to the REST Service and processes the response.
     *
     * @param accessToken The access token obtained from the oxAuth authorization request.
     * @return The service response.
     */
    public ClientInfoResponse execClientInfo(String accessToken) {
        setRequest(new ClientInfoRequest(accessToken));

        return exec();
    }

    public ClientInfoResponse exec() {
        initClientRequest();
        return _exec();
    }


    @Deprecated
    public ClientInfoResponse exec(ClientExecutor p_executor) {
        clientRequest = new ClientRequest(getUrl(), p_executor);
        return _exec();
    }


    /**
     * Executes the call to the REST Service and processes the response.
     *
     * @return The service response.
     */
    private ClientInfoResponse _exec() {
        // Prepare request parameters
        clientRequest.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        clientRequest.setHttpMethod(getHttpMethod());

        if (getRequest().getAuthorizationMethod() == null
                || getRequest().getAuthorizationMethod() == AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD) {
            if (StringUtils.isNotBlank(getRequest().getAccessToken())) {
                clientRequest.header("Authorization", "Bearer " + getRequest().getAccessToken());
            }
        } else if (getRequest().getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER) {
            if (StringUtils.isNotBlank(getRequest().getAccessToken())) {
                clientRequest.formParameter("access_token", getRequest().getAccessToken());
            }
        } else if (getRequest().getAuthorizationMethod() == AuthorizationMethod.URL_QUERY_PARAMETER) {
            if (StringUtils.isNotBlank(getRequest().getAccessToken())) {
                clientRequest.queryParameter("access_token", getRequest().getAccessToken());
            }
        }

        // Call REST Service and handle response
        try {
            if (getRequest().getAuthorizationMethod() == null
                    || getRequest().getAuthorizationMethod() == AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD
                    || getRequest().getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER) {
                clientResponse = clientRequest.post(String.class);
            } else {  //AuthorizationMethod.URL_QUERY_PARAMETER
                clientResponse = clientRequest.get(String.class);
            }

            int status = clientResponse.getStatus();

            setResponse(new ClientInfoResponse(status));

            String entity = clientResponse.getEntity(String.class);
            getResponse().setEntity(entity);
            getResponse().setHeaders(clientResponse.getMetadata());
            if (StringUtils.isNotBlank(entity)) {
                try {
                    JSONObject jsonObj = new JSONObject(entity);

                    if (jsonObj.has("error")) {
                        getResponse().setErrorType(UserInfoErrorResponseType.fromString(jsonObj.getString("error")));
                        jsonObj.remove("error");
                    }
                    if (jsonObj.has("error_description")) {
                        getResponse().setErrorDescription(jsonObj.getString("error_description"));
                        jsonObj.remove("error_description");
                    }
                    if (jsonObj.has("error_uri")) {
                        getResponse().setErrorUri(jsonObj.getString("error_uri"));
                        jsonObj.remove("error_uri");
                    }

                    for (Iterator<String> iterator = jsonObj.keys(); iterator.hasNext(); ) {
                        String key = iterator.next();
                        List<String> values = new ArrayList<String>();

                        JSONArray jsonArray = jsonObj.optJSONArray(key);
                        if (jsonArray != null) {
                            for (int i = 0; i < jsonArray.length(); i++) {
                                String value = jsonArray.optString(i);
                                if (value != null) {
                                    values.add(value);
                                }
                            }
                        } else {
                            String value = jsonObj.optString(key);
                            if (value != null) {
                                values.add(value);
                            }
                        }

                        getResponse().getClaims().put(key, values);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}