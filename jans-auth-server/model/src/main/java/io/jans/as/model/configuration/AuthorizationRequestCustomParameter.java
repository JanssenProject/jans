/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.configuration;

import io.jans.util.exception.ConfigurationException;
import org.apache.commons.lang.StringUtils;

/**
 * @author Javier Rojas Blum
 * @version February 2, 2022
 */
public class AuthorizationRequestCustomParameter {

    private String paramName;
    private Boolean returnInResponse;

    private static final Boolean DEFAULT_RETURN_IN_RESPONSE = false;

    public String getParamName() {
        if (StringUtils.isEmpty(paramName)) {
            throw new ConfigurationException("The param name in a AuthorizationRequestCustomParameter is empty");
        }
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public Boolean getReturnInResponse() {
        if (returnInResponse == null) {
            returnInResponse = DEFAULT_RETURN_IN_RESPONSE;
        }
        return returnInResponse;
    }

    public void setReturnInResponse(Boolean returnInResponse) {
        this.returnInResponse = returnInResponse;
    }
}
