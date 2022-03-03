/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.context;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * This is wrapper and utility for J2E request and response
 *
 * @author Yuriy Movchan 11/14/2014
 */
public class J2EContext implements WebContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    /**
     * Build a J2E context from the current HTTP request
     *
     * @param request
     * @param response
     */
    public J2EContext(final HttpServletRequest request, final HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestParameter(final String name) {
        return this.request.getParameter(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String[]> getRequestParameters() {
        return this.request.getParameterMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestHeader(final String name) {
        return this.request.getHeader(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSessionAttribute(final String name, final Object value) {
        this.request.getSession().setAttribute(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getSessionAttribute(final String name) {
        return this.request.getSession().getAttribute(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRequestAttribute(final String name, final Object value) {
        this.request.setAttribute(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getRequestAttribute(final String name) {
        return this.request.getAttribute(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestMethod() {
        return this.request.getMethod();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeResponseContent(final String content) throws IOException {
        if (content != null) {
            this.response.getWriter().write(content);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResponseHeader(final String name, final String value) {
        this.response.setHeader(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServerName() {
        return this.request.getServerName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getServerPort() {
        return this.request.getServerPort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScheme() {
        return this.request.getScheme();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFullRequestURL() {
        StringBuffer requestURL = request.getRequestURL();
        String queryString = request.getQueryString();
        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?').append(queryString).toString();
        }
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        response.sendRedirect(location);
    }

    @Override
    public HttpSession getSession(boolean create) {
        return request.getSession(create);
    }

}
