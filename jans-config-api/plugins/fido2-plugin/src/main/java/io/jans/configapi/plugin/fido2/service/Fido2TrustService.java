/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.plugin.fido2.util.Fido2Util;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Proxies the read-only attestation and MDS diagnostics exposed by the FIDO2 server, so the Admin UI
 * can render an attestation and MDS status panel through the Config API.
 * <p>
 * GitHub Issue #14602
 */
@Singleton
public class Fido2TrustService {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";
    private static final String FIDO2_TRUST_BASE_URL = "/jans-fido2/restv1/trust";

    @Inject
    Logger log;

    @Inject
    Fido2Util fido2Util;

    public String getFido2TrustUrl() {
        return fido2Util.getIssuer() + FIDO2_TRUST_BASE_URL;
    }

    public String getAttestationConfigUrl() {
        return getFido2TrustUrl() + Constants.ATTESTATION_CONFIG;
    }

    /**
     * The attestation policy the FIDO2 server is applying.
     *
     * @param token bearer token to forward, may be null
     * @return attestation trust configuration
     */
    public JsonNode getAttestationConfig(String token) throws JsonProcessingException {
        log.info("Get Fido2 attestation trust configuration");

        return getTrustData(this.getAttestationConfigUrl(), buildHeaders(token));
    }

    private JsonNode getTrustData(String url, Map<String, String> headers) throws JsonProcessingException {
        log.debug("Fido2 trust data: url:{}, headers:{}", url, headers);

        if (StringUtils.isBlank(url)) {
            throw new WebApplicationException("Error while getting Fido2 trust data",
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        JsonNode jsonNode = fido2Util.executeGetRequest(url, headers, null);
        log.debug("Fido2 trust data: jsonNode:{}", jsonNode);

        return jsonNode;
    }

    private Map<String, String> buildHeaders(String token) {
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(token)) {
            headers.put(AUTHORIZATION, token);
        }
        return headers;
    }
}
