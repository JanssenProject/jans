/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.service.external.context;

import java.util.HashMap;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds object required in custom scripts
 *
 * @author Yuriy Movchan  Date: 07/01/2015
 */

public class ExternalScriptContext {

    private static final Logger log = LoggerFactory.getLogger(ExternalScriptContext.class);

    private final Map<String, Object> contextVariables = new HashMap<>();

    protected HttpServletRequest httpRequest;
    protected final HttpServletResponse httpResponse;

    public ExternalScriptContext(HttpServletRequest httpRequest) {
        this(httpRequest, null);
    }

    public ExternalScriptContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;

        if (this.httpRequest == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                ExternalContext extCtx = facesContext.getExternalContext();
                if (extCtx != null) {
                    this.httpRequest = (HttpServletRequest) extCtx.getRequest();
                }
            }
        }
    }

    public Logger getLog() {
        return log;
    }

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    public HttpServletResponse getHttpResponse() {
        return httpResponse;
    }

    public String getIpAddress() {
        return httpRequest != null ? httpRequest.getRemoteAddr() : "";
    }

    public Map<String, Object> getContextVariables() {
        return contextVariables;
    }
}
