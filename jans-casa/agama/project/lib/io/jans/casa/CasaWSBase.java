package io.jans.casa;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.ParseException;

import io.jans.casa.conf.OIDCClientSettings;
import io.jans.casa.model.ApplicationConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.NetworkUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CasaWSBase {

    public static final String SCOPE_PREFIX = "https://jans.io/";

    private static final int TOKEN_EXP_GAP = 2000;

    // making it static prevents a serialization error...
    protected static final Logger logger = LoggerFactory.getLogger(CasaWSBase.class);

    private String basicAuthnHeader;
    private String serverBase;
    private String token;
    private long tokenExp;
    private String scope;
    private String apiBase;

    public CasaWSBase() throws IOException {
        this(false);
        setScope("https://jans.io/casa.2fa");
    }

    // constructor added to prevent serialization error...
    public CasaWSBase(boolean doHealthCheck) throws IOException {

        OIDCClientSettings clSettings = null;
        try {
            PersistenceEntryManager entryManager = CdiUtil.bean(PersistenceEntryManager.class);
            clSettings = entryManager.find(ApplicationConfiguration.class,
                    "ou=casa,ou=configuration,o=jans").getSettings().getOidcSettings().getClient();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new IOException("Unable to retrieve Casa configuration. Is Casa installed?");
        }

        serverBase = NetworkUtils.urlBeforeContextPath();
        apiBase = serverBase + "/jans-casa/rest";

        if (doHealthCheck) {
            ensureCasaAvailability();
        }

        logger.info("Agama calling Casa with client {}", clSettings.getClientId());
        String authz = clSettings.getClientId() + ":" + clSettings.getClientSecret();
        authz = new String(Base64.getEncoder().encode(authz.getBytes(UTF_8)), UTF_8);
        basicAuthnHeader = "Basic " + authz;

    }

    protected void setScope(String scope) {
        this.scope = scope;
    }

    protected String getApiBase() {
        return apiBase;
    }

    protected HTTPResponse sendRequest(HTTPRequest request, boolean checkOK, boolean withToken)
            throws IOException, ParseException {

        setTimeouts(request);
        if (withToken) {
            refreshToken();
            request.setAuthorization("Bearer " + token);
        }

        HTTPResponse r = request.send();
        if (checkOK) {
            r.ensureStatusCode(200);
        }
        return r;

    }

    protected String encode(String str) {
        return URLEncoder.encode(str, UTF_8);
    }

    private void ensureCasaAvailability() throws IOException {

        try {
            HTTPRequest request = new HTTPRequest(HTTPRequest.Method.GET,
                    new URL(serverBase + "/jans-casa/health-check"));
            sendRequest(request, true, false);
        } catch (Exception e) {
            logger.warn("Casa not installed or not running?");
            throw new IOException("Casa health-check request did not succeed", e);
        }

    }

    private void refreshToken() throws IOException {

        if (System.currentTimeMillis() < tokenExp - TOKEN_EXP_GAP)
            return;

        StringJoiner joiner = new StringJoiner("&");
        Map.of("grant_type", "client_credentials", "scope", scope)
                .forEach((k, v) -> joiner.add(k + "=" + v));

        logger.info("Calling token endpoint for scope {}", scope);

        HTTPRequest request = new HTTPRequest(HTTPRequest.Method.POST, new URL(serverBase + "/jans-auth/restv1/token"));
        setTimeouts(request);
        request.setQuery(joiner.toString());
        request.setAuthorization(basicAuthnHeader);

        try {
            Map<String, Object> jobj = request.send().getContentAsJSONObject();

            long exp = Long.parseLong(jobj.get("expires_in").toString()) * 1000;
            tokenExp = System.currentTimeMillis() + exp;
            token = jobj.get("access_token").toString();
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }

    }

    private void setTimeouts(HTTPRequest request) {
        request.setConnectTimeout(3500);
        request.setReadTimeout(3500);
    }

}
