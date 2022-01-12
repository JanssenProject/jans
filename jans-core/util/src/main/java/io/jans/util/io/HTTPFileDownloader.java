/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util.io;

import java.io.IOException;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import io.jans.util.EasySSLProtocolSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: Yuriy Movchan Date: 11.21.2010
 */
public final class HTTPFileDownloader {

    private HTTPFileDownloader() { }

    private static final Logger LOG = LoggerFactory.getLogger(HTTPFileDownloader.class);
    private static Protocol EASY_HTTPS;

    public static String getResource(String path, String contentType, String user, String password) {
        boolean isUseAuthentication = (user != null) && (password != null);

        if (!path.contains("://")) {
            path = "http://" + path;
        }
        String result = null;

        GetMethod method = new GetMethod(path);
        try {
            method.setRequestHeader("Accept", contentType);

            if (getEasyhttps() == null) {
                setEasyhttps(new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
            }
            Protocol.registerProtocol("https", getEasyhttps());

            final HttpClient httpClient;
            if (isUseAuthentication) {
                httpClient = createHttpClientWithBasicAuth(user, password);
            } else {
                httpClient = new HttpClient();
            }

            httpClient.executeMethod(method);
            if (method.getStatusCode() == HttpStatus.SC_OK) {
                result = method.getResponseBodyAsString();
            }
        } catch (IOException ex) {
            result = null;
            LOG.error(String.format("Failed to get resource %s", path), ex);
        } catch (Exception ex) {
            result = null;
            LOG.error(String.format("Failed to get resource %s", path), ex);
        } finally {
            method.releaseConnection();
        }

        return result;
    }

    private static HttpClient createHttpClientWithBasicAuth(String userid, String password) {
        Credentials credentials = new UsernamePasswordCredentials(userid, password);
        HttpClient httpClient = new HttpClient();
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        httpClient.getParams().setAuthenticationPreemptive(true);
        return httpClient;
    }

    public static void setEasyhttps(Protocol easyhttps) {
        HTTPFileDownloader.EASY_HTTPS = easyhttps;
    }

    public static Protocol getEasyhttps() {
        return EASY_HTTPS;
    }

}
