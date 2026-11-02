/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.session.ws.rs;

import io.jans.as.model.util.Util;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.jans.as.server.util.ServerUtil.daemonThreadFactory;

/**
 * @author Yuriy Zabrovarnyy
 */
public class EndSessionUtils {

    private static final Logger log = LoggerFactory.getLogger(EndSessionUtils.class);

    private static final int BACKCHANNEL_TIMEOUT_MILLIS = 10_000;

    private EndSessionUtils() {
    }

    public static ExecutorService getExecutorService() {
        return Executors.newCachedThreadPool(daemonThreadFactory());
    }

    public static void callRpWithBackchannelUri(final String backchannelLogoutUri, String logoutToken) {
        log.debug("Calling RP with backchannel, backchannel_logout_uri: {}", backchannelLogoutUri);
        jakarta.ws.rs.client.Client client = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(createBackchannelEngine()).build();
        try (Response response = client.target(backchannelLogoutUri).request().post(Entity.form(new Form("logout_token", logoutToken)))) {
            log.debug("Backchannel RP response, status: {}, backchannel_logout_uri: {}", response.getStatus(), backchannelLogoutUri);
        } catch (Exception e) {
            log.error("Failed to call backchannel_logout_uri" + backchannelLogoutUri + ", message: " + e.getMessage(), e);
        } finally {
            client.close();
        }
    }

    private static ApacheHttpClient43Engine createBackchannelEngine() {
        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(BACKCHANNEL_TIMEOUT_MILLIS)
                .setConnectionRequestTimeout(BACKCHANNEL_TIMEOUT_MILLIS)
                .setSocketTimeout(BACKCHANNEL_TIMEOUT_MILLIS)
                .build();
        final ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(HttpClients.custom().setDefaultRequestConfig(requestConfig).build());
        engine.setFollowRedirects(true);
        return engine;
    }

    static boolean isHttpUrl(String url) {
        final String normalized = normalizeUrl(url);
        return normalized.startsWith("https://") || normalized.startsWith("http://");
    }

    static boolean isScriptableUrl(String url) {
        final String normalized = normalizeUrl(url);
        return normalized.startsWith("javascript:") || normalized.startsWith("vbscript:") || normalized.startsWith("data:");
    }

    // Browsers ignore control chars and whitespace around/inside the scheme, so strip them before checking.
    private static String normalizeUrl(String url) {
        return url == null ? "" : url.replaceAll("[\\x00-\\x20]", "").toLowerCase(Locale.ROOT);
    }

    public static String appendState(String uri, String state) {
        if (StringUtils.isBlank(state)) {
            return uri;
        }

        if (uri.contains("?")) {
            if (uri.contains("state=")) {
                return uri;
            } else {
                return uri + "&state=" + state;
            }
        } else {
            return uri + "?state=" + state;
        }
    }

    public static String appendSid(String logoutUri, String sid, String issuer) {
        if (logoutUri.contains("?")) {
            return logoutUri + "&sid=" + sid + "&iss=" + issuer;
        } else {
            return logoutUri + "?sid=" + sid + "&iss=" + issuer;
        }
    }

    public static String createFrontChannelHtml(Set<String> logoutUris, String postLogoutUrl, String state) {
        String iframes = "";
        for (String logoutUri : logoutUris) {
            if (!isHttpUrl(logoutUri)) {
                log.error("Skipping frontchannel_logout_uri with non-http(s) scheme: {}", Util.escapeLog(logoutUri));
                continue;
            }
            iframes = iframes + String.format("<iframe height=\"0\" width=\"0\" src=\"%s\" sandbox=\"allow-same-origin allow-scripts allow-popups allow-forms\"></iframe>",
                    StringEscapeUtils.escapeHtml4(logoutUri));
        }

        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>";

        if (!Util.isNullOrEmpty(postLogoutUrl) && isScriptableUrl(postLogoutUrl)) {
            log.error("Ignoring post_logout_redirect_uri with scriptable scheme: {}", Util.escapeLog(postLogoutUrl));
            postLogoutUrl = null;
        }

        if (!Util.isNullOrEmpty(postLogoutUrl)) {

            if (!Util.isNullOrEmpty(state)) {
                if (postLogoutUrl.contains("?")) {
                    postLogoutUrl += "&state=" + state;
                } else {
                    postLogoutUrl += "?state=" + state;
                }
            }

            html += "<script>" +
                    "window.onload=function() {" +
                    "window.location='" + StringEscapeUtils.escapeEcmaScript(postLogoutUrl) + "'" +
                    "}" +
                    "</script>";
        }

        html += "<title>Your logout was successful</title>" +
                "</head>" +
                "<body>" +
                "Your logout was successful.<br/>" +
                iframes;

        if (Util.isNullOrEmpty(postLogoutUrl)) {
            html += "<br/>" +
                    "<div style=\"display:flex;gap:12px;align-items:center;flex-wrap:wrap;\">" +
                    "<iframe src=\"https://ghbtns.com/github-btn.html?user=JanssenProject&amp;repo=jans&amp;type=star&amp;count=true&amp;size=large\" " +
                    "frameborder=\"0\" scrolling=\"0\" width=\"170\" height=\"30\" title=\"Star JanssenProject/jans on GitHub\"></iframe>" +
                    "<iframe src=\"https://ghbtns.com/github-btn.html?user=JanssenProject&amp;repo=jans&amp;type=watch&amp;count=true&amp;size=large&amp;v=2\" " +
                    "frameborder=\"0\" scrolling=\"0\" width=\"170\" height=\"30\" title=\"Watch JanssenProject/jans on GitHub\"></iframe>" +
                    "</div>";
        }

        html += "</body>" +
                "</html>";
        return html;
    }

}
