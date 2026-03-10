package io.jans.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoreCertUtil {

    private static final Logger LOG = LoggerFactory.getLogger(CoreCertUtil.class);

    public static final String HEADER_XFCC_CERT = "X-Forwarded-Client-Cert";
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

        // First try to parse XFCC header
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

        // Use legacy header if XFCC is not present, parsing failed, or XFCC has no cert
        headerValue = request.getHeader(HEADER_CLIENT_CERT);
        if (headerValue != null && !headerValue.isEmpty()) {
            // If we have XFCC metadata (hash, subject, uri) but no cert, combine with legacy cert
            if (clientCert != null) {
                return new ClientCert(clientCert.getHash(), headerValue, clientCert.getSubject(), clientCert.getUri());
            }
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