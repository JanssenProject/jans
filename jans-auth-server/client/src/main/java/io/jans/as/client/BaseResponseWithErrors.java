/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.config.Constants;
import io.jans.as.model.error.IErrorType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/10/2012
 */

public abstract class BaseResponseWithErrors<T extends IErrorType> extends BaseResponse {

    private T errorType;
    private String errorDescription;
    private String errorUri;

    private Map<String, List<String>> claimMap;

    protected BaseResponseWithErrors() {
        super();
    }

    @SuppressWarnings("java:S1874")
    protected BaseResponseWithErrors(Response clientResponse) {
        super(clientResponse);
        claimMap = new HashMap<>();
        final String entity = getEntity();
        if (StringUtils.isNotBlank(entity)) {
            injectErrorIfExistSilently(entity);
        }
    }

    public Map<String, List<String>> getClaimMap() {
        return claimMap;
    }

    public Map<String, String> getClaims() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : claimMap.entrySet()) {
            final boolean hasValue = entry.getValue() != null && !entry.getValue().isEmpty();
            if (hasValue) {
                result.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return result;
    }

    public void setClaimMap(Map<String, List<String>> claims) {
        this.claimMap = claims;
    }

    public List<String> getClaim(String claimName) {
        if (claimMap.containsKey(claimName)) {
            return claimMap.get(claimName);
        }

        return null;
    }

    @Nullable
    public String getFirstClaim(@NotNull String claimName) {
        final List<String> values = getClaim(claimName);
        return !values.isEmpty() ? values.get(0) : null;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public T getErrorType() {
        return errorType;
    }

    public void setErrorType(T errorType) {
        this.errorType = errorType;
    }

    public String getErrorUri() {
        return errorUri;
    }

    public void setErrorUri(String errorUri) {
        this.errorUri = errorUri;
    }

    public abstract T fromString(String str);

    public void injectDataFromJson(String json) {
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

    public void injectErrorIfExistSilently(String entity) {
        try {
            injectErrorIfExistSilently(new JSONObject(entity));
        } catch (JSONException e) {
            // ignore : it's ok to skip exception because entity string can be json array or just trash
        }
    }

    @Override
    public String toString() {
        return "BaseResponseWithErrors{" +
                "claimMap=" + claimMap +
                "errorType=" + errorType +
                ", errorDescription='" + errorDescription + '\'' +
                ", errorUri='" + errorUri + '\'' +
                "} " + super.toString();
    }
}
