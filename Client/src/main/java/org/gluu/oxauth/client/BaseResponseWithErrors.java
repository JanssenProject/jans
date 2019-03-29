/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.error.IErrorType;
import org.jboss.resteasy.client.ClientResponse;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/10/2012
 */

public abstract class BaseResponseWithErrors<T extends IErrorType> extends BaseResponse {

//    private static final Logger LOG = Logger.getLogger(BaseResponseWithErrors.class);

    private T errorType;
    private String errorDescription;
    private String errorUri;

    public BaseResponseWithErrors() {
        super();
    }

    public BaseResponseWithErrors(ClientResponse<String> clientResponse) {
        super(clientResponse);
        final String entity = getEntity();
        if (StringUtils.isNotBlank(entity)) {
            injectErrorIfExistSilently(entity);
        }
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String p_errorDescription) {
        errorDescription = p_errorDescription;
    }

    public T getErrorType() {
        return errorType;
    }

    public void setErrorType(T p_errorType) {
        errorType = p_errorType;
    }

    public String getErrorUri() {
        return errorUri;
    }

    public void setErrorUri(String p_errorUri) {
        errorUri = p_errorUri;
    }

    public abstract T fromString(String p_str);

    public void injectDataFromJson(String p_json) {
    }

    public void injectErrorIfExistSilently(JSONObject jsonObj) throws JSONException {
        if (jsonObj.has("error")) {
            errorType = fromString(jsonObj.getString("error"));
        }
        if (jsonObj.has("error_description")) {
            errorDescription = jsonObj.getString("error_description");
        }
        if (jsonObj.has("error_uri")) {
            errorUri = jsonObj.getString("error_uri");
        }
    }

    public void injectErrorIfExistSilently(String p_entity) {
        try {
            injectErrorIfExistSilently(new JSONObject(p_entity));
        } catch (JSONException e) {
            // ignore : it's ok to skip exception because entity string can be json array or just trash
        }
    }
}
