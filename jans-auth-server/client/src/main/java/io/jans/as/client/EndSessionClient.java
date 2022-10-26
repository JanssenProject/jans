/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.config.Constants;
import io.jans.as.model.session.EndSessionErrorResponseType;
import io.jans.as.model.session.EndSessionRequestParam;
import io.jans.as.model.session.EndSessionResponseParam;
import io.jans.as.model.util.QueryStringDecoder;
import io.jans.as.model.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Encapsulates functionality to make end session request calls to an
 * authorization server via REST Services.
 *
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
public class EndSessionClient extends BaseClient<EndSessionRequest, EndSessionResponse> {

    private static final Logger LOG = Logger.getLogger(EndSessionClient.class);

    /**
     * Constructs an end session client by providing an URL where the REST service is located.
     *
     * @param url The REST service location.
     */
    public EndSessionClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.GET;
    }

    /**
     * Executes the call to the REST Service requesting to end session and processes the response.
     *
     * @param idTokenHint           The issued ID Token.
     * @param postLogoutRedirectUri The URL to which the RP is requesting that the End-User's User-Agent be redirected
     *                              after a logout has been performed.
     * @param state                 The state.
     * @return The service response.
     */
    public EndSessionResponse execEndSession(String idTokenHint, String postLogoutRedirectUri, String state) {
        setRequest(new EndSessionRequest(idTokenHint, postLogoutRedirectUri, state));

        return exec();
    }

    /**
     * Executes the call to the REST Service and processes the response.
     *
     * @return The service response.
     */
    public EndSessionResponse exec() {
        // Prepare request parameters
        initClient();

        if (StringUtils.isNotBlank(getRequest().getIdTokenHint())) {
            addReqParam(EndSessionRequestParam.ID_TOKEN_HINT, getRequest().getIdTokenHint());
        }
        if (StringUtils.isNotBlank(getRequest().getPostLogoutRedirectUri())) {
            addReqParam(EndSessionRequestParam.POST_LOGOUT_REDIRECT_URI, getRequest().getPostLogoutRedirectUri());
        }
        if (StringUtils.isNotBlank(getRequest().getState())) {
            addReqParam(EndSessionRequestParam.STATE, getRequest().getState());
        }
        if (StringUtils.isNotBlank(getRequest().getSid())) {
            addReqParam(EndSessionRequestParam.SID, getRequest().getSid());
        }

        // Call REST Service and handle response
        try {
            Builder clientRequest = webTarget.request();
            applyCookies(clientRequest);

            clientRequest.accept(MediaType.TEXT_PLAIN);

            clientResponse = clientRequest.buildGet().invoke();
            int status = clientResponse.getStatus();

            setResponse(new EndSessionResponse(status));
            String entity = clientResponse.readEntity(String.class);
            getResponse().setEntity(entity);
            getResponse().setHeaders(clientResponse.getMetadata());
            if (clientResponse.getLocation() != null) {
                String location = clientResponse.getLocation().toString();
                getResponse().setLocation(location);

                int queryStringIndex = location.indexOf("?");
                if (queryStringIndex != -1) {
                    String queryString = location
                            .substring(queryStringIndex + 1);
                    Map<String, String> params = QueryStringDecoder.decode(queryString);
                    if (params.containsKey(EndSessionResponseParam.STATE)) {
                        getResponse().setState(params.get(EndSessionResponseParam.STATE));
                    }
                }
            }

            if (!Util.isNullOrEmpty(entity) && !entity.contains("<html>")) {
                JSONObject jsonObj = new JSONObject(entity);
                if (jsonObj.has(Constants.ERROR)) {
                    getResponse().setErrorType(EndSessionErrorResponseType.fromString(jsonObj.getString(Constants.ERROR)));
                }
                if (jsonObj.has(Constants.ERROR_DESCRIPTION)) {
                    getResponse().setErrorDescription(jsonObj.getString(Constants.ERROR_DESCRIPTION));
                }
                if (jsonObj.has(Constants.ERROR_URI)) {
                    getResponse().setErrorUri(jsonObj.getString(Constants.ERROR_URI));
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