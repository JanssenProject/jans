/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.fido2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.plugin.fido2.util.Fido2Util;
import io.jans.model.net.HttpServiceResponse;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Proxies the native app readiness check of the FIDO2 server, so it can be run with Config API
 * authentication. Nothing is cached here: the result cache, the rate limits and the audit live in the FIDO2
 * server, next to the sandboxed fetcher.
 * <p>
 * The caller picks a configured relying party by id and nothing else. The id becomes one URL-encoded path
 * segment under the FIDO2 server's own address, and the request body is built here, so no URL, host, port or
 * path from the caller reaches the FIDO2 server.
 */
@Singleton
public class Fido2ReadinessService {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";
    private static final String RETRY_AFTER = "Retry-After";
    private static final String FIDO2_RP_BASE_URL = "/jans-fido2/restv1" + Constants.RP + "/";
    private static final String READINESS = "/readiness";
    private static final String UNKNOWN = "unknown";
    private static final int MAX_PRINCIPAL_LENGTH = 128;

    /** What the FIDO2 server answered: its status, the JSON body if any, and its {@code Retry-After}. */
    public static final class Outcome {
        private final int status;
        private final JsonNode body;
        private final String retryAfter;

        Outcome(int status, JsonNode body, String retryAfter) {
            this.status = status;
            this.body = body;
            this.retryAfter = retryAfter;
        }

        public int getStatus() {
            return status;
        }

        public JsonNode getBody() {
            return body;
        }

        public String getRetryAfter() {
            return retryAfter;
        }
    }

    @Inject
    Logger log;

    @Inject
    Fido2Util fido2Util;

    public String getReadinessUrl(String rpConfigId) {
        return fido2Util.getIssuer() + FIDO2_RP_BASE_URL + encodeSegment(rpConfigId) + READINESS;
    }

    /**
     * Runs the readiness check for one configured relying party.
     *
     * @param rpConfigId  the RP id exactly as configured in {@code requestedParties}
     * @param recheck     skip the FIDO2 server's cached result (it is still rate limited there)
     * @param token       the caller's {@code Authorization} header value to forward, may be null
     * @param requestedBy the authenticated caller, forwarded for the FIDO2 server's audit, may be null
     * @return the FIDO2 server's status, body and {@code Retry-After}, unchanged
     */
    public Outcome check(String rpConfigId, boolean recheck, String token, String requestedBy)
            throws JsonProcessingException {
        if (StringUtils.isBlank(rpConfigId)) {
            throw new WebApplicationException("The relying party id is required",
                    Response.Status.BAD_REQUEST.getStatusCode());
        }
        String url = getReadinessUrl(rpConfigId);
        log.info("Fido2 native app readiness check, recheck:{}", recheck);

        HttpServiceResponse serviceResponse = fido2Util.executePost(url, buildHeaders(token, requestedBy),
                "{\"recheck\":" + recheck + "}");
        HttpResponse httpResponse = serviceResponse == null ? null : serviceResponse.getHttpResponse();
        if (httpResponse == null || httpResponse.getStatusLine() == null) {
            throw new WebApplicationException("Error while running the Fido2 native app readiness check",
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        int status = httpResponse.getStatusLine().getStatusCode();
        String retryAfter = httpResponse.getFirstHeader(RETRY_AFTER) == null ? null
                : httpResponse.getFirstHeader(RETRY_AFTER).getValue();
        return new Outcome(status, parse(httpResponse.getEntity()), retryAfter);
    }

    private static JsonNode parse(HttpEntity entity) throws JsonProcessingException {
        if (entity == null) {
            return null;
        }
        try {
            String content = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            return StringUtils.isBlank(content) ? null : Jackson.asJsonNode(content);
        } catch (IOException ex) {
            throw new WebApplicationException("Unable to read upstream response entity", ex);
        }
    }

    private Map<String, String> buildHeaders(String token, String requestedBy) {
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(token)) {
            headers.put(AUTHORIZATION, token);
        }
        headers.put(Constants.REQUESTED_BY_HEADER, sanitize(requestedBy));
        return headers;
    }

    /** Percent-encodes everything but the RFC 3986 unreserved characters, so {@code :} and {@code /} survive as data. */
    static String encodeSegment(String value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;
            boolean unreserved = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~';
            if (unreserved) {
                out.append((char) c);
            } else {
                out.append('%').append(Character.toUpperCase(Character.forDigit(c >> 4, 16)))
                        .append(Character.toUpperCase(Character.forDigit(c & 0xf, 16)));
            }
        }
        return out.toString();
    }

    /** A header value of plain characters only, so a caller-supplied label cannot inject headers or log lines. */
    static String sanitize(String value) {
        if (StringUtils.isBlank(value)) {
            return UNKNOWN;
        }
        String cut = value.length() > MAX_PRINCIPAL_LENGTH ? value.substring(0, MAX_PRINCIPAL_LENGTH) : value;
        StringBuilder out = new StringBuilder(cut.length());
        for (int i = 0; i < cut.length(); i++) {
            char c = cut.charAt(i);
            boolean plain = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '.' || c == '-' || c == '_' || c == '@' || c == ':';
            out.append(plain ? c : '_');
        }
        return out.toString();
    }
}
