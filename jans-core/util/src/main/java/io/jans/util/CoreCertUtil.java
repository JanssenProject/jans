package io.jans.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoreCertUtil {

    private static final Logger LOG = LoggerFactory.getLogger(CoreCertUtil.class);

    public static final String HEADER_XFCC_CERT = "X-Forwarded-Client-Cert";
    public static final String HEADER_XFTCC_CERT = "X-Forwarded-Tls-Client-Cert";
    public static final String HEADER_CLIENT_CERT = "X-ClientCert";

    public static final Pattern PARAM_XFCC_HASH = Pattern.compile("Hash=([^;]+)");
    public static final Pattern PARAM_XFCC_SUBJECT = Pattern.compile("Subject=\"([^\"]+)\"");
    public static final Pattern PARAM_XFCC_CERT = Pattern.compile("Cert=\"([^\"]+)\"");
    public static final Pattern PARAM_XFCC_URI = Pattern.compile("URI=([^;]*)");

    private final static ClientCert MISSING_CERT_HEADERS = new ClientCert(null, null, null, null);

    public static ClientCert getClientCert(HttpServletRequest request) {
        if (request == null) {
            return MISSING_CERT_HEADERS;
        }

        // 1. First try to parse XFCC header (Envoy/Istio)
        String headerValue = request.getHeader(HEADER_XFCC_CERT);
        ClientCert clientCert = null;

        if (headerValue != null && !headerValue.isEmpty()) {
            try {
                clientCert = parseXfccHeader(headerValue);
                if (clientCert != null && clientCert.getCert() != null && !clientCert.getCert().isEmpty()) {
                    return clientCert;
                }
            } catch (Exception e) {
                LOG.error("Failed to parse client cert: " + headerValue, e);
            }
        }

        // 2. Try XFTCC header (Traefik) - raw base64 without PEM delimiters
        // Note: We don't mix XFCC metadata with XFTCC cert to avoid combining data from potentially different certificates
        headerValue = request.getHeader(HEADER_XFTCC_CERT);
        if (headerValue != null && !headerValue.isEmpty()) {
            String cert = parseXftccHeader(headerValue);
            if (cert != null) {
                return new ClientCert(null, cert, null, null);
            }
        }

        // 3. Use legacy header if neither XFCC nor XFTCC worked
        // Note: We don't mix XFCC metadata with legacy cert to avoid combining data from potentially different certificates
        headerValue = request.getHeader(HEADER_CLIENT_CERT);
        if (headerValue != null && !headerValue.isEmpty()) {
            return new ClientCert(null, headerValue, null, null);
        }

        // Return XFCC metadata even without cert (hash/subject may still be useful)
        if (clientCert != null) {
            return clientCert;
        }

        return MISSING_CERT_HEADERS;
    }

    public static ClientCert parseXfccHeader(String headerValue) {
        if (headerValue == null || headerValue.isEmpty()) {
            return null;
        }

        String hash = extract(headerValue, PARAM_XFCC_HASH);
        String subject = extract(headerValue, PARAM_XFCC_SUBJECT);

        String uri = extract(headerValue, PARAM_XFCC_URI);

        String cert = extract(headerValue, PARAM_XFCC_CERT);
        if (cert != null) {
            try {
                cert = URLDecoder.decode(cert, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                LOG.warn("Failed to decode certificate from XFCC header: {}", e.getMessage());
                return null;
            }
        }

        return new ClientCert(hash, cert, subject, uri);
    }

    /**
     * Parses the X-Forwarded-Tls-Client-Cert header (Traefik format).
     * The header contains base64-encoded certificate without PEM delimiters.
     * Traefik &lt;2.9.4 URL-encodes this header, while newer versions send raw base64.
     *
     * @param headerValue base64 string (possibly URL-encoded)
     * @return PEM-formatted certificate or null if invalid
     */
    public static String parseXftccHeader(String headerValue) {
        if (headerValue == null || headerValue.trim().isEmpty()) {
            return null;
        }

        String base64Content = headerValue.trim();

        // URL-decode if needed (Traefik <2.9.4 URL-encodes this header)
        // Check for URL-encoded characters (%, +encoded as %2B, etc.)
        if (base64Content.contains("%")) {
            try {
                base64Content = URLDecoder.decode(base64Content, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                LOG.warn("Failed to URL-decode X-Forwarded-Tls-Client-Cert header, trying as raw base64: {}", e.getMessage());
                // Continue with original value - it might be raw base64 with literal % (unlikely but safe)
                base64Content = headerValue.trim();
            }
        }

        // Validate it's valid base64 by attempting decode
        try {
            Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            LOG.error("Failed to decode X-Forwarded-Tls-Client-Cert header as base64: {}", e.getMessage());
            return null;
        }

        // Convert to PEM format
        return "-----BEGIN CERTIFICATE-----\n" + base64Content + "\n-----END CERTIFICATE-----";
    }

    private static String extract(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static class ClientCert {
        private final String hash;
        private final String cert;
        private final String subject;
        private final String uri;

        public ClientCert(String hash, String cert, String subject, String uri) {
            this.hash = hash;
            this.cert = cert;
            this.subject = subject;
            this.uri = uri;
        }

        public String getHash() {
            return hash;
        }

        public String getCert() {
            return cert;
        }

        public String getSubject() {
            return subject;
        }

        public String getUri() {
            return uri;
        }
    }
}