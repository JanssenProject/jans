/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.common.Parameters;
import org.xdi.oxauth.model.session.EndSessionErrorResponseType;
import org.xdi.oxauth.model.session.EndSessionRequestParam;
import org.xdi.oxauth.model.session.EndSessionResponseParam;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Encapsulates functionality to make end session request calls to an
 * authorization server via REST Services.
 *
 * @author Javier Rojas Blum
 * @version 0.9 January 28, 2015
 */
public class EndSessionClient extends BaseClient<EndSessionRequest, EndSessionResponse> {

    private static final String mediaType = MediaType.TEXT_PLAIN;

    /**
     * Constructs an end session client by providing an URL where the REST service is located.
     *
     * @param url The REST service location.
     */
    public EndSessionClient(String url) {
        super(url);
    }

    @Override
    public EndSessionRequest getRequest() {
        if (request instanceof EndSessionRequest) {
            return (EndSessionRequest) request;
        } else {
            return null;
        }
    }

    @Override
    public void setRequest(EndSessionRequest request) {
        super.request = request;
    }

    @Override
    public EndSessionResponse getResponse() {
        if (response instanceof EndSessionResponse) {
            return (EndSessionResponse) response;
        } else {
            return null;
        }
    }

    @Override
    public void setResponse(EndSessionResponse response) {
        super.response = response;
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
        initClientRequest();
        clientRequest.accept(mediaType);
        clientRequest.setHttpMethod(getHttpMethod());

        if (StringUtils.isNotBlank(getRequest().getIdTokenHint())) {
            clientRequest.queryParameter(EndSessionRequestParam.ID_TOKEN_HINT, getRequest().getIdTokenHint());
        }
        if (StringUtils.isNotBlank(getRequest().getPostLogoutRedirectUri())) {
            clientRequest.queryParameter(EndSessionRequestParam.POST_LOGOUT_REDIRECT_URI, getRequest().getPostLogoutRedirectUri());
        }
        if (StringUtils.isNotBlank(getRequest().getState())) {
            clientRequest.queryParameter(EndSessionRequestParam.STATE, getRequest().getState());
        }
        if (StringUtils.isNotBlank(getRequest().getSessionId())) {
            clientRequest.queryParameter(Parameters.SESSION_ID.getParamName(), getRequest().getSessionId());
        }

        // Call REST Service and handle response
        try {
            clientResponse = clientRequest.get(String.class);
            int status = clientResponse.getStatus();

            setResponse(new EndSessionResponse(status));
            String entity = clientResponse.getEntity(String.class);
            getResponse().setEntity(entity);
            getResponse().setHeaders(clientResponse.getHeaders());
            if (clientResponse.getLocation() != null) {
                String location = clientResponse.getLocation().getHref();
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

            if (entity != null && !entity.equals("")) {
                try {
                    JSONObject jsonObj = new JSONObject(entity);
                    if (jsonObj.has("error")) {
                        getResponse().setErrorType(EndSessionErrorResponseType.fromString(jsonObj.getString("error")));
                    }
                    if (jsonObj.has("error_description")) {
                        getResponse().setErrorDescription(jsonObj.getString("error_description"));
                    }
                    if (jsonObj.has("error_uri")) {
                        getResponse().setErrorUri(jsonObj.getString("error_uri"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();

                    // http based logout : http://openid.net/specs/openid-connect-logout-1_0.html
                    getResponse().setHtmlPage(entity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}