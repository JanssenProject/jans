/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import com.google.common.collect.Lists;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.Util;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.model.security.Identity;
import io.jans.util.Pair;
import io.jans.util.StringHelper;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * 
 * @version October 7, 2019
 */
@Stateless
@Named
public class RequestParameterService {

	// use only "acr" instead of "acr_values" #334
    private static final List<String> ALLOWED_PARAMETER = Collections.unmodifiableList(Arrays.asList(
            AuthorizeRequestParam.SCOPE,
            AuthorizeRequestParam.RESPONSE_TYPE,
            AuthorizeRequestParam.CLIENT_ID,
            AuthorizeRequestParam.REDIRECT_URI,
            AuthorizeRequestParam.STATE,
            AuthorizeRequestParam.RESPONSE_MODE,
            AuthorizeRequestParam.NONCE,
            AuthorizeRequestParam.DISPLAY,
            AuthorizeRequestParam.PROMPT,
            AuthorizeRequestParam.MAX_AGE,
            AuthorizeRequestParam.UI_LOCALES,
            AuthorizeRequestParam.ID_TOKEN_HINT,
            AuthorizeRequestParam.LOGIN_HINT,
            AuthorizeRequestParam.ACR_VALUES,
            AuthorizeRequestParam.REQUEST,
            AuthorizeRequestParam.REQUEST_URI,
            AuthorizeRequestParam.ORIGIN_HEADERS,
            AuthorizeRequestParam.CODE_CHALLENGE,
            AuthorizeRequestParam.CODE_CHALLENGE_METHOD,
            AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS,
            AuthorizeRequestParam.CLAIMS,
            AuthorizeRequestParam.AUTH_REQ_ID,
            AuthorizeRequestParam.SID,
            DeviceAuthorizationService.SESSION_USER_CODE));

    @Inject
    private Logger log;

    @Inject
    private Identity identity;

    @Inject
    private AppConfiguration appConfiguration;

    private List<String> getAllAllowedParameters() {
        List<String> allowedParameters = Lists.newArrayList(ALLOWED_PARAMETER);
        if (appConfiguration.getSessionIdRequestParameterEnabled()) {
            allowedParameters.add(AuthorizeRequestParam.SESSION_ID);
        }
        return allowedParameters;
    }

    public Map<String, String> getAllowedParameters(@Nonnull final Map<String, String> requestParameterMap) {
        Set<String> authorizationRequestCustomAllowedParameters = appConfiguration.getAuthorizationRequestCustomAllowedParameters();
        if (authorizationRequestCustomAllowedParameters == null) {
        	authorizationRequestCustomAllowedParameters = new HashSet<String>(0);
        }

        final Map<String, String> result = new HashMap<String, String>();
        if (requestParameterMap.isEmpty()) {
            return result;
        }

        final List<String> allAllowed = getAllAllowedParameters();
        final Set<Map.Entry<String, String>> set = requestParameterMap.entrySet();
        for (Map.Entry<String, String> entry : set) {
            if (allAllowed.contains(entry.getKey()) || authorizationRequestCustomAllowedParameters.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public Map<String, String> getCustomParameters(@Nonnull final Map<String, String> requestParameterMap) {
        Set<String> authorizationRequestCustomAllowedParameters = appConfiguration.getAuthorizationRequestCustomAllowedParameters();

        final Map<String, String> result = new HashMap<String, String>();
        if (authorizationRequestCustomAllowedParameters == null) {
        	return result;
        }

        if (!requestParameterMap.isEmpty()) {
            final Set<Map.Entry<String, String>> set = requestParameterMap.entrySet();
            for (Map.Entry<String, String> entry : set) {
                if (authorizationRequestCustomAllowedParameters.contains(entry.getKey())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return result;
    }

    public String parametersAsString(final Map<String, String> parameterMap) throws UnsupportedEncodingException {
        final StringBuilder sb = new StringBuilder();
        final Set<Entry<String, String>> set = parameterMap.entrySet();
        for (Map.Entry<String, String> entry : set) {
            final String value = (String) entry.getValue();
            if (StringUtils.isNotBlank(value)) {
                sb.append(entry.getKey()).append("=").append(URLEncoder.encode(value, Util.UTF8_STRING_ENCODING)).append("&");
            }
        }

        String result = sb.toString();
        if (result.endsWith("&")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public Map<String, String> getParametersMap(List<String> extraParameters, final Map<String, String> parameterMap) {
        final List<String> allowedParameters = getAllAllowedParameters();

        if (extraParameters != null) {
            for (String extraParameter : extraParameters) {
                putInMap(parameterMap, extraParameter);
            }

            allowedParameters.addAll(extraParameters);
        }

        parameterMap.entrySet().removeIf(entry -> !allowedParameters.contains(entry.getKey()));
        return parameterMap;
    }

    private void putInMap(Map<String, String> map, String p_name) {
        if (map == null) {
            return;
        }

        String value = getParameterValue(p_name);

        map.put(p_name, value);
    }

    public String getParameterValue(String p_name) {
        Pair<String, String> valueWithType = getParameterValueWithType(p_name);
        if (valueWithType == null) {
            return null;
        }

        return valueWithType.getFirst();
    }

    public Pair<String, String> getParameterValueWithType(String p_name) {
        String value = null;
        String clazz = null;
        final Object o = identity.getWorkingParameter(p_name);
        if (o instanceof String) {
            final String s = (String) o;
            value = s;
            clazz = String.class.getName();
        } else if (o instanceof Integer) {
            final Integer i = (Integer) o;
            value = i.toString();
            clazz = Integer.class.getName();
        } else if (o instanceof Boolean) {
            final Boolean b = (Boolean) o;
            value = b.toString();
            clazz = Boolean.class.getName();
        }

        return new Pair<String, String>(value, clazz);
    }

    public Object getTypedValue(String stringValue, String type) {
        if (StringHelper.equals(Boolean.class.getName(), type)) {
            return Boolean.valueOf(stringValue);
        } else if (StringHelper.equals(Integer.class.getName(), type)) {
            return Integer.valueOf(stringValue);
        }

        return stringValue;
    }

    /**
     * Process a JWT Request instance and update Custom Parameters according to custom parameters sent.
     * @param jwtRequest JWT processing
     * @param customParameters Custom parameters used in the authorization flow.
     */
    public void getCustomParameters(JwtAuthorizationRequest jwtRequest, Map<String, String> customParameters) {
        Set<String> authorizationRequestCustomAllowedParameters = appConfiguration.getAuthorizationRequestCustomAllowedParameters();

        if (authorizationRequestCustomAllowedParameters == null) {
            return;
        }

        JSONObject jsonPayload = jwtRequest.getJsonPayload();
        for (String customParam : authorizationRequestCustomAllowedParameters) {
            if (jsonPayload.has( customParam )) {
                customParameters.put(customParam, jsonPayload.getString(customParam));
            }
        }
    }
}
