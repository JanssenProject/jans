/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import com.google.common.collect.Lists;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.AuthorizationRequestCustomParameter;
import io.jans.as.model.util.Util;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.model.security.Identity;
import io.jans.util.Pair;
import io.jans.util.StringHelper;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version February 2, 2022
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
            AuthorizeRequestParam.AUTHORIZATION_DETAILS,
            AuthorizeRequestParam.LOGIN_HINT,
            AuthorizeRequestParam.ACR_VALUES,
            AuthorizeRequestParam.REQUEST,
            AuthorizeRequestParam.REQUEST_URI,
            AuthorizeRequestParam.ORIGIN_HEADERS,
            AuthorizeRequestParam.CODE_CHALLENGE,
            AuthorizeRequestParam.CODE_CHALLENGE_METHOD,
            AuthorizeRequestParam.DPOP_JKT,
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
        if (isTrue(appConfiguration.getSessionIdRequestParameterEnabled())) {
            allowedParameters.add(AuthorizeRequestParam.SESSION_ID);
        }
        return allowedParameters;
    }

    public Map<String, String> getAllowedParameters(@Nonnull final Map<String, String> requestParameterMap) {
        Set<AuthorizationRequestCustomParameter> authorizationRequestCustomAllowedParameters = appConfiguration.getAuthorizationRequestCustomAllowedParameters();
        if (authorizationRequestCustomAllowedParameters == null) {
            authorizationRequestCustomAllowedParameters = new HashSet<>(0);
        }

        final Map<String, String> result = new HashMap<>();
        if (requestParameterMap.isEmpty()) {
            return result;
        }

        final List<String> allAllowed = getAllAllowedParameters();
        final Set<Map.Entry<String, String>> set = requestParameterMap.entrySet();
        for (Map.Entry<String, String> entry : set) {
            if (allAllowed.contains(entry.getKey())
                    || authorizationRequestCustomAllowedParameters.stream()
                    .filter(o -> StringUtils.isNotBlank(o.getParamName()) && o.getParamName().equals(entry.getKey())).findFirst().isPresent()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public Map<String, String> getCustomParameters(@Nonnull final Map<String, String> requestParameterMap) {
        return getCustomParameters(requestParameterMap, false);
    }

    public Map<String, String> getCustomParameters(@Nonnull final Map<String, String> requestParameterMap, boolean onlyReturnInResponseParams) {
        Set<AuthorizationRequestCustomParameter> authorizationRequestCustomAllowedParameters = appConfiguration.getAuthorizationRequestCustomAllowedParameters();

        final Map<String, String> result = new HashMap<>();
        if (authorizationRequestCustomAllowedParameters == null) {
            return result;
        }

        if (!requestParameterMap.isEmpty()) {
            final Set<Map.Entry<String, String>> set = requestParameterMap.entrySet();
            for (Map.Entry<String, String> entry : set) {

                if (onlyReturnInResponseParams && authorizationRequestCustomAllowedParameters.stream()
                        .filter(o -> StringUtils.isNotBlank(o.getParamName())
                                && o.getParamName().equals(entry.getKey())
                                && o.getReturnInResponse()).findFirst().isPresent()) {
                    result.put(entry.getKey(), entry.getValue());
                } else if (!onlyReturnInResponseParams && authorizationRequestCustomAllowedParameters.stream()
                        .filter(o -> StringUtils.isNotBlank(o.getParamName())
                                && o.getParamName().equals(entry.getKey())).findFirst().isPresent()) {
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
            final String value = entry.getValue();
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

    private void putInMap(Map<String, String> map, String name) {
        if (map == null) {
            return;
        }

        String value = getParameterValue(name);

        map.put(name, value);
    }

    public String getParameterValue(String name) {
        Pair<String, String> valueWithType = getParameterValueWithType(name);
        if (valueWithType == null) {
            return null;
        }

        return valueWithType.getFirst();
    }

    public Pair<String, String> getParameterValueWithType(String name) {
        String value = null;
        String clazz = null;
        final Object o = identity.getWorkingParameter(name);
        if (o instanceof String) {
            value = (String) o;
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

        return new Pair<>(value, clazz);
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
     *
     * @param jwtRequest       JWT processing
     * @param customParameters Custom parameters used in the authorization flow.
     */
    public void getCustomParameters(JwtAuthorizationRequest jwtRequest, Map<String, String> customParameters) {
        Set<AuthorizationRequestCustomParameter> authorizationRequestCustomAllowedParameters = appConfiguration.getAuthorizationRequestCustomAllowedParameters();

        if (authorizationRequestCustomAllowedParameters == null) {
            return;
        }

        JSONObject jsonPayload = jwtRequest.getJsonPayload();
        for (AuthorizationRequestCustomParameter customParam : authorizationRequestCustomAllowedParameters) {
            if (jsonPayload.has(customParam.getParamName())) {
                customParameters.put(customParam.getParamName(), jsonPayload.getString(customParam.getParamName()));
            }
        }
    }

    public Map<String, String> getCustomParameters(HttpServletRequest request) {
        Map<String, String> customParameters = new HashMap<>();
        addCustomParameters(request, customParameters);
        return customParameters;
    }

    public void addCustomParameters(HttpServletRequest request, Map<String, String> customParameters) {
        Set<AuthorizationRequestCustomParameter> authorizationRequestCustomAllowedParameters = appConfiguration
                .getAuthorizationRequestCustomAllowedParameters();

        if (authorizationRequestCustomAllowedParameters == null) {
            log.trace("Skipped custom parameters because 'authorizationRequestCustomAllowedParameters' AS configuration is not set.");
            return;
        }

        final Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            final String parameterName = parameterNames.nextElement();
            if (!authorizationRequestCustomAllowedParameters.stream().map(AuthorizationRequestCustomParameter::getParamName).collect(toList()).contains(parameterName)) {
                log.trace("Skipped '{}' as custom parameter (not defined in 'authorizationRequestCustomAllowedParameters')", parameterName);
                continue;
            }

            final String parameterValue = request.getParameter(parameterName);
            if (StringUtils.isNotBlank(parameterValue)) {
                customParameters.put(parameterName, parameterValue);
            }
        }

        log.trace("Custom parameters: {}", customParameters);
    }

    public void putCustomParametersIntoSession(SessionId sessionId, HttpServletRequest httpRequest) {
        putCustomParametersIntoSession(sessionId, getCustomParameters(httpRequest));
    }

    public void putCustomParametersIntoSession(SessionId sessionId, Map<String, String> customParameters) {
        if (sessionId == null || customParameters == null) {
            return;
        }

        putCustomParametersIntoSession(sessionId.getSessionAttributes(), customParameters);
    }

    public void putCustomParametersIntoSession(Map<String, String> sessionAttributes, Map<String, String> customParameters) {
        if (sessionAttributes == null || customParameters == null) {
            return;
        }

        for (Map.Entry<String, String> entry : customParameters.entrySet()) {
            sessionAttributes.put("custom_" + entry.getKey(), entry.getValue());
        }
    }
}
