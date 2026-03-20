/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.client.RegisterRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.persistence.model.ClientAttributes;
import io.jans.as.server.model.registration.RegisterParamsValidator;
import io.jans.as.server.register.ws.rs.RegisterService;
import io.jans.as.server.service.external.ExternalDynamicClientRegistrationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * OAuth Client ID Metadata Document (CIMD) Service.
 * Handles URL-based client_id resolution per IETF draft spec.
 *
 * Fetched clients are persisted to the database with TTL-based expiry.
 * On TTL expiry, the client metadata is re-downloaded from the URL.
 *
 * @author Yuriy Z
 */
@ApplicationScoped
public class ClientIdMetadataService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ClientService clientService;

    @Inject
    private RegisterService registerService;

    @Inject
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;

    @Inject
    private RegisterParamsValidator registerParamsValidator;

    /**
     * Token endpoint authentication methods forbidden by the CIMD spec (Section 4.1).
     * These are all methods based on a shared symmetric secret, which cannot be
     * pre-established when using a URL as client_id.
     */
    static final Set<AuthenticationMethod> FORBIDDEN_AUTH_METHODS = EnumSet.of(
            AuthenticationMethod.CLIENT_SECRET_BASIC,
            AuthenticationMethod.CLIENT_SECRET_POST,
            AuthenticationMethod.CLIENT_SECRET_JWT
    );

    // ==================== Public API ====================

    /**
     * Check if client_id is a CIMD URL candidate.
     *
     * @param clientId the client_id to check
     * @return true if CIMD feature is enabled and client_id is a valid URL with allowed scheme
     */
    public boolean isCimdClientId(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            return false;
        }

        if (!isFeatureEnabled()) {
            return false;
        }

        try {
            URI uri = new URI(clientId);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }

            List<String> allowedSchemes = appConfiguration.getCimdSchemeAllowlist();
            return allowedSchemes != null && allowedSchemes.contains(scheme.toLowerCase());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Check if CIMD feature is enabled.
     *
     * @return true if CLIENT_ID_METADATA_DOCUMENT feature flag is enabled
     */
    public boolean isFeatureEnabled() {
        return appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENT_ID_METADATA_DOCUMENT);
    }

    /**
     * Fetch (or return persisted), validate, and return Client from CIMD URL.
     * Persists the client with a TTL. On TTL expiry, re-downloads the metadata.
     *
     * @param clientIdUrl the URL-based client_id
     * @return Client object populated from the CIMD document
     * @throws WebApplicationException if validation fails or fetch fails
     */
    public Client getClient(String clientIdUrl) {
        log.debug("Getting CIMD client for: {}", clientIdUrl);

        // Validate URL before any lookup (SSRF protection)
        validateClientIdUrl(clientIdUrl);

        // Derive a stable, safe id from the URL
        String id = computeId(clientIdUrl);
        String dn = clientService.buildClientDn(id);

        // Check if a valid (non-expired) persisted client exists
        Client existing = clientService.getClientByDn(dn);
        if (existing != null && existing.getAttributes() != null && existing.getAttributes().isCimdClient()) {
            if (!existing.getAttributes().isCimdExpired()) {
                log.debug("Returning persisted CIMD client for: {} (not yet expired)", clientIdUrl);
                existing.setClientId(clientIdUrl);
                return existing;
            }
            log.debug("Persisted CIMD client for {} has expired, re-downloading", clientIdUrl);
        }

        // Fetch document from the URL
        FetchResult result = doFetch(clientIdUrl);

        // Parse the metadata document via RegisterRequest (comprehensive RFC 7591 parsing)
        RegisterRequest registerRequest = parseRegisterRequest(result.body);

        // Validate that no forbidden symmetric-secret auth method is used (CIMD spec Section 4.1)
        validateTokenEndpointAuthMethod(registerRequest);

        // Validate redirect_uris using the same logic as DCR
        validateRedirectUris(registerRequest);

        // Build Client from RegisterRequest using the same DCR mapping path
        Client client = buildClientFromRequest(registerRequest, clientIdUrl);

        // Set the hash-based id as the persistent key (LDAP-safe)
        client.setClientId(id);
        client.setDn(dn);

        // Set CIMD attributes
        ClientAttributes attrs = client.getAttributes() != null ? client.getAttributes() : new ClientAttributes();
        attrs.setCimdClient(true);
        attrs.setCimdOriginalClientId(clientIdUrl);

        int ttlSeconds = calculateTtl(result.cacheControl);
        attrs.setCimdExpiresAt(System.currentTimeMillis() + (ttlSeconds * 1000L));
        client.setAttributes(attrs);

        // Execute external DCR script (allows server operators to customize/reject CIMD clients).
        // httpRequest is null since this is not an HTTP registration request.
        final boolean scriptResult = externalDynamicClientRegistrationService
                .executeExternalCreateClientMethods(registerRequest, client, null);
        if (!scriptResult) {
            throw badRequest("Client creation rejected by external script for: " + clientIdUrl);
        }

        // Persist (or update) the client
        if (existing != null) {
            clientService.merge(client);
            log.debug("Re-persisted (merged) CIMD client for: {}", clientIdUrl);
        } else {
            clientService.persist(client);
            log.debug("Persisted new CIMD client for: {}", clientIdUrl);
        }

        // Override the id-based clientId with the actual URL for the caller
        client.setClientId(clientIdUrl);

        return client;
    }

    // ==================== Validation ====================

    /**
     * Validate the client_id URL for SSRF and policy compliance.
     *
     * @param clientIdUrl the URL to validate
     * @throws WebApplicationException if validation fails
     */
    public void validateClientIdUrl(String clientIdUrl) {
        try {
            URI uri = new URI(clientIdUrl);

            // Scheme validation
            validateScheme(uri);

            // Domain allowlist/blocklist
            validateDomain(uri);

            // Private IP blocking
            if (Boolean.TRUE.equals(appConfiguration.getCimdBlockPrivateIp())) {
                validateNotPrivateIp(uri.getHost());
            }

        } catch (URISyntaxException e) {
            throw badRequest("Invalid client_id URL syntax: " + e.getMessage());
        }
    }

    void validateScheme(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw badRequest("Client ID URL must have a scheme");
        }

        List<String> allowedSchemes = appConfiguration.getCimdSchemeAllowlist();
        if (allowedSchemes == null || !allowedSchemes.contains(scheme.toLowerCase())) {
            throw badRequest("Invalid URL scheme. Only HTTPS allowed.");
        }
    }

    void validateDomain(URI uri) {
        String host = uri.getHost();
        if (StringUtils.isBlank(host)) {
            throw badRequest("Client ID URL must have a host");
        }

        List<String> blocklist = appConfiguration.getCimdDomainBlocklist();
        if (blocklist != null && blocklist.contains(host)) {
            throw badRequest("Domain is blocked: " + host);
        }

        List<String> allowlist = appConfiguration.getCimdDomainAllowlist();
        if (allowlist != null && !allowlist.isEmpty() && !allowlist.contains(host)) {
            throw badRequest("Domain not in allowlist: " + host);
        }
    }

    void validateNotPrivateIp(String host) {
        if (StringUtils.isBlank(host)) {
            throw badRequest("Cannot validate empty host");
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            if (isPrivateAddress(address)) {
                throw badRequest("Client ID URL resolves to private/local address");
            }
        } catch (UnknownHostException e) {
            throw badRequest("Cannot resolve client_id host: " + host);
        }
    }

    /**
     * Check if an IP address is private/internal.
     *
     * @param address the address to check
     * @return true if private/internal
     */
    public boolean isPrivateAddress(InetAddress address) {
        return address.isLoopbackAddress() ||
                address.isSiteLocalAddress() ||
                address.isLinkLocalAddress() ||
                address.isAnyLocalAddress();
    }

    /**
     * Validate redirect_uris from the CIMD document using the same rules as DCR.
     */
    void validateRedirectUris(RegisterRequest registerRequest) {
        final boolean valid = registerParamsValidator.validateRedirectUris(
                registerRequest.getGrantTypes(),
                registerRequest.getResponseTypes(),
                registerRequest.getApplicationType() != null ? registerRequest.getApplicationType() : ApplicationType.WEB,
                null,
                registerRequest.getRedirectUris(),
                registerRequest.getSectorIdentifierUri());
        if (!valid) {
            throw badRequest("Invalid redirect_uris in client metadata document");
        }
    }

    /**
     * Validate that the token_endpoint_auth_method is not a forbidden symmetric-secret method.
     * Per CIMD spec Section 4.1, client_secret_basic, client_secret_post, client_secret_jwt,
     * and any other symmetric-secret-based method MUST NOT be used.
     */
    void validateTokenEndpointAuthMethod(RegisterRequest registerRequest) {
        AuthenticationMethod method = registerRequest.getTokenEndpointAuthMethod();
        if (method != null && FORBIDDEN_AUTH_METHODS.contains(method)) {
            throw badRequest("token_endpoint_auth_method '" + method + "' is not allowed for CIMD clients. " +
                    "Symmetric secret-based methods (client_secret_basic, client_secret_post, client_secret_jwt) " +
                    "are forbidden per the Client ID Metadata Document specification.");
        }
    }

    // ==================== HTTP Fetching ====================

    protected FetchResult doFetch(String url) {

        try (jakarta.ws.rs.client.Client httpClient = ClientBuilder.newBuilder()
                .connectTimeout(appConfiguration.getCimdConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(appConfiguration.getCimdReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .build()) {
            Response response = httpClient.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            if (response.getStatus() != 200) {
                throw badRequest("Failed to fetch client metadata: HTTP " + response.getStatus() + " for client_id: " + url);
            }

            String body = response.readEntity(String.class);
            if (body == null) {
                throw badRequest("Empty response from client metadata URL");
            }

            if (body.length() > appConfiguration.getCimdMaxResponseSize()) {
                throw badRequest("Client metadata document exceeds maximum size");
            }

            String cacheControl = response.getHeaderString("Cache-Control");
            return new FetchResult(body, cacheControl);
        }
    }

    // ==================== Parsing ====================

    /**
     * Parse the CIMD metadata JSON into a RegisterRequest using RFC 7591 field mapping.
     */
    RegisterRequest parseRegisterRequest(String json) {
        try {
            return RegisterRequest.fromJson(json);
        } catch (Exception e) {
            log.error("Failed to parse CIMD metadata JSON: {}", e.getMessage());
            throw badRequest("Invalid client metadata JSON: " + e.getMessage());
        }
    }

    /**
     * Build a Client from a RegisterRequest using the same DCR mapping path (RegisterService).
     * This ensures all RFC 7591 fields are applied consistently with dynamic registration.
     */
    protected Client buildClientFromRequest(RegisterRequest registerRequest, String clientIdUrl) {
        try {
            Client client = new Client();
            registerService.updateClientFromRequestObject(client, registerRequest, false);
            client.setClientId(clientIdUrl);
            return client;
        } catch (Exception e) {
            log.error("Failed to apply CIMD metadata to client: {}", e.getMessage());
            throw badRequest("Failed to process client metadata: " + e.getMessage());
        }
    }

    /**
     * Parse metadata JSON and build a Client. Convenience wrapper used by tests.
     */
    protected Client parseToClient(String json, String clientIdUrl) {
        return buildClientFromRequest(parseRegisterRequest(json), clientIdUrl);
    }

    // ==================== TTL ====================

    protected int calculateTtl(String cacheControl) {
        int defaultTtl = appConfiguration.getCimdTtlMinutes() * 60;
        int maxTtl = appConfiguration.getCimdMaxTtlMinutes() * 60;

        if (StringUtils.isBlank(cacheControl)) {
            return defaultTtl;
        }

        // Parse max-age from Cache-Control header
        try {
            if (cacheControl.contains("max-age=")) {
                String maxAgeStr = cacheControl.replaceAll(".*max-age=(\\d+).*", "$1");
                int maxAge = Integer.parseInt(maxAgeStr);
                return Math.min(maxAge, maxTtl);
            }
        } catch (Exception e) {
            log.trace("Failed to parse Cache-Control header: " + cacheControl, e);
        }

        return defaultTtl;
    }

    // ==================== Helper Methods ====================

    /**
     * Compute a stable, LDAP-safe inum from a CIMD URL.
     * Uses SHA-256 hex digest to avoid special characters in LDAP DNs.
     */
    public static String computeId(String url) {
        return "cimd-" + DigestUtils.sha256Hex(url);
    }

    // ==================== Error Helpers ====================

    public WebApplicationException badRequest(String msg) {
        log.debug("CIMD validation error: {}", msg);
        return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"invalid_request\",\"error_description\":\"" + msg + "\"}")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
    }

    // ==================== Inner Classes ====================

    /**
     * Result of fetching a CIMD document.
     */
    public static class FetchResult {
        final String body;
        final String cacheControl;

        FetchResult(String body, String cacheControl) {
            this.body = body;
            this.cacheControl = cacheControl;
        }
    }
}
