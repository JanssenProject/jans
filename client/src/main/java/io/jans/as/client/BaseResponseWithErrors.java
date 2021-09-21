/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.config.Constants;
import io.jans.as.model.error.IErrorType;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.ClientResponse;
import org.json.JSONException;
import org.json.JSONObject;

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
        if (jsonObj.has(Constants.ERROR)) {
            errorType = fromString(jsonObj.getString(Constants.ERROR));
        }
        if (jsonObj.has(Constants.ERROR_DESCRIPTION)) {
            errorDescription = jsonObj.getString(Constants.ERROR_DESCRIPTION);
        }
        if (jsonObj.has(Constants.ERROR_URI)) {
            errorUri = jsonObj.getString(Constants.ERROR_URI);
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
