/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
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
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
     * Per-URL locks to serialize concurrent fetch+persist operations for the same client_id.
     * Prevents duplicate-write failures when multiple requests arrive simultaneously for the
     * same uncached (or expired) URL-based client_id.
     */
    private final ConcurrentHashMap<String, Object> fetchLocks = new ConcurrentHashMap<>();

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

        // Serialize concurrent fetch+persist for the same client_id to avoid duplicate writes.
        Object lock = fetchLocks.computeIfAbsent(id, k -> new Object());
        synchronized (lock) {
            // Re-check after acquiring lock — another thread may have already fetched and persisted.
            Client reChecked = clientService.getClientByDn(dn);
            if (reChecked != null && reChecked.getAttributes() != null && reChecked.getAttributes().isCimdClient()) {
                if (!reChecked.getAttributes().isCimdExpired()) {
                    log.debug("Returning CIMD client fetched by concurrent thread for: {}", clientIdUrl);
                    reChecked.setClientId(clientIdUrl);
                    return reChecked;
                }
            }

            // Fetch document from the URL
            FetchResult result = doFetch(clientIdUrl);

            // Parse the metadata document via RegisterRequest (comprehensive RFC 7591 parsing)
            RegisterRequest registerRequest = parseRegisterRequest(result.body);

            // Validate that the document contains client_id matching the fetch URL (§4.1)
            validateClientIdBinding(registerRequest, clientIdUrl);

            // Validate that client_secret / client_secret_expires_at are absent (§4.1)
            validateNoClientSecret(registerRequest);

            // Validate that no forbidden symmetric-secret auth method is used (§4.1)
            validateTokenEndpointAuthMethod(registerRequest);

            // Validate redirect_uris using the same logic as DCR
            validateRedirectUris(registerRequest);

            // Build Client from RegisterRequest using the same DCR mapping path
            Client client = buildClientFromRequest(registerRequest, clientIdUrl);

            // Set the hash-based id as the persistent key (safe)
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
            final Client existingAtLockTime = reChecked != null ? reChecked : existing;
            if (existingAtLockTime != null) {
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

            // §3: structural constraints on the client_id URL
            validateUrlStructure(uri);

            // Private IP blocking
            if (Boolean.TRUE.equals(appConfiguration.getCimdBlockPrivateIp())) {
                validateNotPrivateIp(uri.getHost());
            }

        } catch (URISyntaxException e) {
            throw badRequest("Invalid client_id URL syntax: " + e.getMessage());
        }
    }

    /**
     * Enforce §3 structural constraints on the client_id URL:
     * <ul>
     *   <li>MUST contain a path component</li>
     *   <li>MUST NOT contain single-dot or double-dot path segments</li>
     *   <li>MUST NOT contain a fragment component</li>
     *   <li>MUST NOT contain a username or password</li>
     * </ul>
     */
    void validateUrlStructure(URI uri) {
        // MUST contain a path component
        String path = uri.getPath();
        if (StringUtils.isBlank(path)) {
            throw badRequest("Client ID URL must contain a path component");
        }

        // MUST NOT contain single-dot or double-dot path segments
        for (String segment : path.split("/")) {
            if (".".equals(segment) || "..".equals(segment)) {
                throw badRequest("Client ID URL must not contain dot or double-dot path segments");
            }
        }

        // MUST NOT contain a fragment component
        if (uri.getFragment() != null) {
            throw badRequest("Client ID URL must not contain a fragment component");
        }

        // MUST NOT contain a username or password
        if (uri.getUserInfo() != null) {
            throw badRequest("Client ID URL must not contain a username or password");
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

        // Normalize to lowercase for case-insensitive comparison
        String normalizedHost = host.toLowerCase();

        List<String> blocklist = appConfiguration.getCimdDomainBlocklist();
        if (blocklist != null && blocklist.contains(normalizedHost)) {
            throw badRequest("Domain is blocked: " + normalizedHost);
        }

        List<String> allowlist = appConfiguration.getCimdDomainAllowlist();
        if (allowlist != null && !allowlist.isEmpty() && !allowlist.contains(normalizedHost)) {
            throw badRequest("Domain not in allowlist: " + normalizedHost);
        }
    }

    /**
     * Resolve all IP addresses for the host and reject if any resolves to a private/loopback address.
     * Checking all records (not just the first) mitigates DNS round-robin evasion.
     * Note: a small DNS-rebinding window remains between this check and the actual fetch;
     * for stronger protection, configure a network-level egress firewall.
     */
    void validateNotPrivateIp(String host) {
        if (StringUtils.isBlank(host)) {
            throw badRequest("Cannot validate empty host");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isPrivateAddress(address)) {
                    throw badRequest("Client ID URL resolves to private/local address");
                }
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
     * Validate that the metadata document contains a {@code client_id} field (§4.1 MUST)
     * and that its value matches the URL used to fetch it (simple string comparison).
     * client_id is read from the raw JSON object since RegisterRequest does not expose it
     * as a parsed field (it is a server-assigned value in normal DCR).
     */
    void validateClientIdBinding(RegisterRequest registerRequest, String clientIdUrl) {
        if (registerRequest.getJsonObject() == null) {
            throw badRequest("Client metadata document must contain a client_id property");
        }
        String documentClientId = registerRequest.getJsonObject().optString("client_id", null);
        if (documentClientId == null) {
            throw badRequest("Client metadata document must contain a client_id property");
        }
        if (!documentClientId.equals(clientIdUrl)) {
            throw badRequest("client_id in metadata document does not match the URL used to fetch it. " +
                    "Expected: " + clientIdUrl + ", got: " + documentClientId);
        }
    }

    /**
     * Validate that the metadata document does not include {@code client_secret} or
     * {@code client_secret_expires_at} (§4.1 MUST NOT). These properties are meaningless
     * for CIMD clients since no shared secret can be pre-established.
     */
    void validateNoClientSecret(RegisterRequest registerRequest) {
        if (registerRequest.getJsonObject() == null) {
            return;
        }
        if (registerRequest.getJsonObject().has("client_secret")) {
            throw badRequest("client_secret must not be present in a client metadata document");
        }
        if (registerRequest.getJsonObject().has("client_secret_expires_at")) {
            throw badRequest("client_secret_expires_at must not be present in a client metadata document");
        }
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
                // §4: MUST NOT automatically follow HTTP redirects
                .property(ResteasyClientBuilder.PROPERTY_FOLLOW_REDIRECTS, false)
                .build()) {

            Response response = httpClient.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            if (response.getStatus() != 200) {
                throw badRequest("Failed to fetch client metadata: HTTP " + response.getStatus() + " for client_id: " + url);
            }

            // Stream the body and enforce the byte limit before buffering the full payload.
            // readEntity(String.class) would buffer the entire response first; streaming avoids
            // unbounded heap allocation from oversized or malicious responses.
            int maxBytes = appConfiguration.getCimdMaxResponseSize();
            String body;
            try (InputStream is = response.readEntity(InputStream.class)) {
                byte[] bytes = is.readNBytes(maxBytes + 1);
                if (bytes.length > maxBytes) {
                    throw badRequest("Client metadata document exceeds maximum size");
                }
                if (bytes.length == 0) {
                    throw badRequest("Empty response from client metadata URL");
                }
                body = new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw badRequest("Failed to read client metadata response: " + e.getMessage());
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

        // Honour no-store and no-cache directives: treat as TTL=0 so the entry is
        // considered immediately expired and re-fetched on the next request.
        String lowerCacheControl = cacheControl.toLowerCase();
        if (lowerCacheControl.contains("no-store") || lowerCacheControl.contains("no-cache")) {
            return 0;
        }

        // Parse max-age from Cache-Control header
        try {
            if (cacheControl.contains("max-age=")) {
                String maxAgeStr = cacheControl.replaceAll(".*max-age=(\\d+).*", "$1");
                int maxAge = Integer.parseInt(maxAgeStr);
                return Math.min(maxAge, maxTtl);
            }
        } catch (Exception e) {
            log.trace("Failed to parse Cache-Control header: {}", cacheControl);
            log.trace(e.getMessage(), e);
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
        // Use JsonStringEncoder to properly escape the message, preventing JSON injection
        // from exception text or untrusted input that may contain quotes or backslashes.
        char[] escaped = JsonStringEncoder.getInstance().quoteAsString(msg);
        String body = "{\"error\":\"invalid_request\",\"error_description\":\"" + new String(escaped) + "\"}";
        return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity(body)
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
