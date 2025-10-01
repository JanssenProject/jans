/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.context;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

/**
 * This is interface for J2E context wrapper
 *
 * @author Yuriy Movchan 11/14/2014
 */
public interface WebContext {

    /**
     * Return a request parameter
     *
     * @param name
     * @return the request parameter
     */
    String getRequestParameter(String name);

    /**
     * Return all request parameters
     *
     * @return all request parameters
     */
    Map<String, String[]> getRequestParameters();

    /**
     * Return a request header
     *
     * @param name
     * @return the request header
     */
    String getRequestHeader(String name);

    /**
     * Save an attribute in session
     *
     * @param name
     * @param value
     */
    void setSessionAttribute(String name, Object value);

    /**
     * Get an attribute from session
     *
     * @param name
     * @return the session attribute
     */
    Object getSessionAttribute(String name);

    /**
     * Save an attribute in request
     *
     * @param name
     * @param value
     */
    void setRequestAttribute(final String name, final Object value);

    /**
     * Get an attribute from request
     *
     * @param name
     * @return the request attribute
     */
    Object getRequestAttribute(final String name);

    /**
     * Return the request method
     *
     * @return the request method
     */
    String getRequestMethod();

    /**
     * Write some content in the response
     *
     * @param content
     */
    void writeResponseContent(String content) throws IOException;

    /**
     * Add a header to the response
     *
     * @param name
     * @param value
     */
    void setResponseHeader(String name, String value);

    /**
     * Return the server name
     *
     * @return the server name
     */
    String getServerName();

    /**
     * Return the server port
     *
     * @return the server port
     */
    int getServerPort();

    /**
     * Return the scheme
     *
     * @return the scheme
     */
    String getScheme();

    /**
     * Return the full URL (with query string) the client used to request the server
     *
     * @return the Url
     */
    String getFullRequestURL();

    void sendRedirect(String location) throws IOException;

    HttpSession getSession(boolean create);

}
