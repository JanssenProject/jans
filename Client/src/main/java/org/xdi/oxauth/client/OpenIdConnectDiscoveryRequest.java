/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import static org.xdi.oxauth.model.discovery.WebFingerParam.REL;
import static org.xdi.oxauth.model.discovery.WebFingerParam.REL_VALUE;
import static org.xdi.oxauth.model.discovery.WebFingerParam.RESOURCE;
import static org.xdi.oxauth.model.util.StringUtils.addQueryStringParam;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;

/**
 * @author Javier Rojas Blum Date: 01.28.2013
 */
public class OpenIdConnectDiscoveryRequest extends BaseRequest {

    private String resource;
    private String host;
    private String path;

    public OpenIdConnectDiscoveryRequest(String resource) throws URISyntaxException {
        this.resource = resource;

        if (StringUtils.isBlank(resource)) {
            throw new IllegalArgumentException("Resource cannot be null");
        }
        if (resource.startsWith("=") || resource.startsWith("@") || resource.startsWith("!")) { // XRI
            throw new UnsupportedOperationException("XRI is not supported"); // TODO: Add support for XRI
        } else if (resource.contains("@")) { // email
            this.host = resource.substring(resource.indexOf("@") + 1);
        } else {
            if (!resource.contains("://")) {
                // If the user input Identifier does not have an RFC 3986 "scheme" portion,
                // the string is interpreted as authority path-abempty [ "?" query ] [ "#" fragment ] of RFC 3986.
                // In this case, the https scheme is assumed, and the normalized URL will be formed by prefixing
                // https:// to the string.
                resource = "https://" + resource;
            }

            URI uri = new URI(resource);
            this.host = uri.getHost();
            if (uri.getPort() != -1) {
                this.host += ":" + uri.getPort();
            }
            if (StringUtils.isNotBlank(uri.getPath()) && !uri.getPath().equals(uri.getHost()) && !uri.getPath().equals("/")) {
                this.path = uri.getPath();
            }
        }
    }

    /**
     * Returns the Identifier of the target End-User that is the subject of the discovery request.
     *
     * @return The Identifier of the target End-User that is the subject of the discovery request.
     */
    public String getResource() {
        return resource;
    }

    /**
     * Sets the Identifier of the target End-User that is the subject of the discovery request.
     *
     * @param resource The Identifier of the target End-User that is the subject of the discovery request.
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * Returns the Server where a WebFinger service is hosted.
     *
     * @return The Server where a WebFinger service is hosted.
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the Server where a WebFinger service is hosted.
     *
     * @param host The Server where a WebFinger service is hosted.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * If the Issuer value contains a path component, any terminating / must be removed before
     * appending /.well-known/openid-configuration. Then the Client may make a new request using the path.
     *
     * @return The path component.
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path component.
     *
     * @param path The path component.
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns a query string with the parameters of the OpenID Connect Discovery request.
     * Any <code>null</code> or empty parameter will be omitted.
     *
     * @return A query string of parameters.
     */
    @Override
    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();

        try {
            addQueryStringParam(queryStringBuilder, RESOURCE, resource);
            addQueryStringParam(queryStringBuilder, REL, REL_VALUE);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return queryStringBuilder.toString();
    }
}