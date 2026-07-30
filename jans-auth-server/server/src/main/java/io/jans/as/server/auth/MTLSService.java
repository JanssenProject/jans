/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.auth;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.common.util.CommonUtils;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.model.util.CertUtils;
import io.jans.as.model.util.HashUtil;
import io.jans.as.model.util.SpiffeIdUtil;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.SpiffeBundleService;
import io.jans.as.server.service.external.ExternalDynamicClientRegistrationService;
import io.jans.as.server.service.external.context.DynamicClientRegistrationContext;
import io.jans.util.CoreCertUtil;
import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.security.PublicKey;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import static io.jans.as.model.register.RegisterRequestParam.TLS_CLIENT_AUTH_SUBJECT_DN;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class MTLSService {

    @Inject
    private Logger log;

    @Inject
    private Authenticator authenticator;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private SpiffeBundleService spiffeBundleService;

    public boolean processMTLS(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain, Client client) throws Exception {
        log.debug("Trying to authenticate client {} via {} ...", client.getClientId(),
                client.getAllAuthenticationMethods());

        final String clientCertAsPem = CoreCertUtil.getClientCert(httpRequest).getCert();
        if (StringUtils.isBlank(clientCertAsPem)) {
            log.debug("Client certificate is missed in `X-Forwarded-Client-Cert`, `X-Forwarded-Tls-Client-Cert` and `X-ClientCert` headers, client_id: {}.", client.getClientId());
            return false;
        }

        X509Certificate cert = CertUtils.x509CertificateFromPem(clientCertAsPem);
        if (cert == null) {
            log.debug("Failed to parse client certificate, client_id: {}.", client.getClientId());
            return false;
        }

        // SPIFFE X.509-SVIDs conventionally carry an empty/absent Subject DN (identity lives solely
        // in the URI SAN), so they must bypass the CN sanity check below entirely rather than fall
        // through to the registration script's isCertValidForClient interception hook.
        if (client.hasAuthenticationMethod(AuthenticationMethod.TLS_CLIENT_AUTH)
                && appConfiguration.isFeatureEnabled(FeatureFlagType.SPIFFE_CLIENT_AUTH)
                && StringUtils.isNotBlank(client.getAttributes().getSpiffeId())) {
            return processSpiffeX509Svid(httpRequest, httpResponse, filterChain, client, cert);
        }

        final String cn = CertUtils.getCN(cert);
        final String hashedCn = HashUtil.getHash(cn, SignatureAlgorithm.HS512);

        if ((StringUtils.isBlank(cn) || StringUtils.isBlank(hashedCn)) || (!cn.equals(client.getClientId()) && !hashedCn.equals(HashUtil.getHash(client.getClientId(), SignatureAlgorithm.HS512)))) {
            if (log.isTraceEnabled())
                log.trace("Client certificate CN does not match clientId. Invoke registration script's isCertValidForClient, CN: {}, clientId: {}, hashedCn: {}", cn, client.getClientId(), hashedCn);

            DynamicClientRegistrationContext context = new DynamicClientRegistrationContext(httpRequest, new JSONObject(), null, client);
            boolean result = externalDynamicClientRegistrationService.isCertValidForClient(cert, context);
            if (!result) {
                log.error("Reject request. isCertValidForClient returned false.");
                throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity(errorResponseFactory.getErrorAsJson(TokenErrorResponseType.INVALID_CLIENT, httpRequest.getParameter("state"), "")).build());
            }
        }

        if (client.hasAuthenticationMethod(AuthenticationMethod.TLS_CLIENT_AUTH)) {
            log.debug("Authenticating with tls_client_auth ...");

            final String subjectDn = client.getAttributes().getTlsClientAuthSubjectDn();
            if (StringUtils.isBlank(subjectDn)) {
                log.debug(
                        "SubjectDN is not set for client {} which is required to authenticate it via `tls_client_auth`.",
                        client.getClientId());
                return false;
            }

            // we check only `subjectDn`, the PKI certificate validation is performed by apache/httpd
            if (CertUtils.equalsRdn(subjectDn, cert.getSubjectDN().getName())) {
                log.debug("Client {} authenticated via `tls_client_auth`.", client.getClientId());
                authenticatedSuccessfully(client, httpRequest);

                filterChain.doFilter(httpRequest, httpResponse);
                return true;
            }

            log.debug("Client's subject dn: {}, cert subject dn: {}", subjectDn, cert.getSubjectDN().getName());
        }

        if (client.hasAuthenticationMethod(AuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH)) { // disable it
            log.debug("Authenticating with self_signed_tls_client_auth ...");
            final PublicKey publicKey = cert.getPublicKey();
            final byte[] encodedKey = publicKey.getEncoded();

            JSONObject jsonWebKeys = CommonUtils.getJwks(client);

            if (jsonWebKeys == null) {
                log.debug("Unable to load json web keys for client: {}, jwks_uri: {}, jks: {}", client.getClientId(),
                        client.getJwksUri(), client.getJwks());
                return false;
            }

            final JSONWebKeySet keySet = JSONWebKeySet.fromJSONObject(jsonWebKeys);
            for (JSONWebKey key : keySet.getKeys()) {
                if (ArrayUtils.isEquals(encodedKey,
                        cryptoProvider.getPublicKey(key.getKid(), jsonWebKeys, null).getEncoded())) {
                    log.debug("Client {} authenticated via `self_signed_tls_client_auth`, matched kid: {}.",
                            client.getClientId(), key.getKid());
                    authenticatedSuccessfully(client, httpRequest);

                    filterChain.doFilter(httpRequest, httpResponse);
                    return true;
                }
            }
        }
        log.debug("MTLS authentication failed.");
        return false;
    }

    /**
     * Validates a SPIFFE X.509-SVID client certificate, per draft-ietf-oauth-spiffe-client-auth:
     * exactly one URI SAN containing a valid SPIFFE ID, leaf certificate, digitalSignature key
     * usage, the SPIFFE ID matches (wildcard-aware) the client's registered {@code spiffe_id},
     * and the certificate is trusted by the admin-configured SPIFFE trust bundle for that trust
     * domain.
     */
    private boolean processSpiffeX509Svid(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain, Client client, X509Certificate cert) throws Exception {
        log.debug("Authenticating client {} via SPIFFE X.509-SVID ...", client.getClientId());

        final String registeredSpiffeId = client.getAttributes().getSpiffeId();

        final String presentedSpiffeId = CertUtils.getUniqueSpiffeUriSan(cert);
        if (presentedSpiffeId == null) {
            log.debug("Certificate does not have exactly one valid SPIFFE URI SAN, client_id: {}.", client.getClientId());
            return false;
        }

        if (!CertUtils.isLeafCertificate(cert)) {
            log.debug("SPIFFE X.509-SVID must be a leaf certificate (Basic Constraints CA must be FALSE), client_id: {}.", client.getClientId());
            return false;
        }

        if (!CertUtils.hasDigitalSignatureKeyUsage(cert)) {
            log.debug("SPIFFE X.509-SVID must have the digitalSignature key usage bit set, client_id: {}.", client.getClientId());
            return false;
        }

        if (!SpiffeIdUtil.matches(registeredSpiffeId, presentedSpiffeId)) {
            log.debug("Presented SPIFFE ID {} does not match registered spiffe_id {} for client {}.", presentedSpiffeId, registeredSpiffeId, client.getClientId());
            return false;
        }

        final String trustDomain = SpiffeIdUtil.trustDomainOf(presentedSpiffeId);
        final Set<TrustAnchor> trustAnchors = spiffeBundleService.getX509TrustAnchors(trustDomain);
        if (trustAnchors.isEmpty()) {
            log.debug("No SPIFFE trust anchors configured/available for trust domain: {}, client_id: {}.", trustDomain, client.getClientId());
            return false;
        }
        if (!isTrustedByAnyAnchor(cert, trustAnchors)) {
            log.debug("Failed to validate SPIFFE X.509-SVID against configured trust anchors for trust domain: {}, client_id: {}. " +
                            "Note: only the forwarded leaf certificate is validated (single-hop trust) - if your PKI uses an " +
                            "intermediate CA, either configure that intermediate as the trust anchor, or configure the reverse " +
                            "proxy to forward the full certificate chain.",
                    trustDomain, client.getClientId());
            return false;
        }

        log.debug("Client {} authenticated via SPIFFE X.509-SVID, spiffe_id: {}.", client.getClientId(), presentedSpiffeId);
        authenticatedSuccessfully(client, httpRequest);

        filterChain.doFilter(httpRequest, httpResponse);
        return true;
    }

    /**
     * Verifies that the leaf certificate's signature was produced by one of the given trust
     * anchors and that the leaf is currently valid. This is single-hop verification: only the
     * forwarded leaf certificate is available (the reverse proxy does not forward the full
     * chain), so a multi-level CA hierarchy must have its immediate issuing CA configured as the
     * trust anchor.
     */
    private boolean isTrustedByAnyAnchor(X509Certificate leaf, Set<TrustAnchor> anchors) {
        for (TrustAnchor anchor : anchors) {
            try {
                leaf.verify(anchor.getTrustedCert().getPublicKey());
                leaf.checkValidity();
                return true;
            } catch (Exception e) {
                log.trace("Certificate not trusted by anchor: {}", anchor.getTrustedCert().getSubjectDN(), e);
            }
        }
        return false;
    }

    private void authenticatedSuccessfully(Client client, HttpServletRequest httpRequest) {
        authenticator.configureSessionClient(client);

        List<Prompt> prompts = Prompt.fromString(httpRequest.getParameter(AuthorizeRequestParam.PROMPT), " ");
        if (prompts.contains(Prompt.LOGIN)) {
            return; // skip session authentication if we have prompt=login
        }

        SessionId sessionIdObject = sessionIdService.getSessionId(httpRequest);
        if (sessionIdObject == null || sessionIdObject.getState() != SessionIdState.AUTHENTICATED) {
            return;
        }

        authenticator.authenticateBySessionId(sessionIdObject);
    }

    public boolean processRegisterMTLS(HttpServletRequest httpRequest) {
        log.debug("Trying to authenticate client registration request via MTLS");

        String tlsClientAuthSubjectDn = null;
        try {
            String request = IOUtils.toString(httpRequest.getReader());
            JSONObject jsonObject = new JSONObject(request);
            tlsClientAuthSubjectDn = jsonObject.optString(TLS_CLIENT_AUTH_SUBJECT_DN.toString());
        } catch (Exception exception) {
            log.error("Error getting TLS_CLIENT_AUTH_SUBJECT_DN field from request registration body", exception);
        }

        final String clientCertAsPem = CoreCertUtil.getClientCert(httpRequest).getCert();
        if (StringUtils.isBlank(clientCertAsPem)) {
            log.debug("Client certificate is missed in `X-Forwarded-Client-Cert`, `X-Forwarded-Tls-Client-Cert` and `X-ClientCert` headers");
            return false;
        }

        X509Certificate cert = CertUtils.x509CertificateFromPem(clientCertAsPem);
        if (cert == null) {
            log.debug("Failed to parse client certificate");
            return false;
        }

        log.debug("MTLS client authentication tlsClientAuthSubjectDn = {}", tlsClientAuthSubjectDn);
        return CertUtils.equalsRdn(tlsClientAuthSubjectDn, cert.getSubjectDN().getName());
    }
}
