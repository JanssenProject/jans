/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.auth;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.util.CommonUtils;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.model.util.CertUtils;
import io.jans.as.model.util.HashUtil;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.external.ExternalDynamicClientRegistrationService;
import io.jans.as.server.service.external.context.DynamicClientRegistrationContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

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

    public boolean processMTLS(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain, Client client) throws Exception {
        log.debug("Trying to authenticate client {} via {} ...", client.getClientId(),
                client.getAuthenticationMethod());

        final String clientCertAsPem = httpRequest.getHeader("X-ClientCert");
        if (StringUtils.isBlank(clientCertAsPem)) {
            log.debug("Client certificate is missed in `X-ClientCert` header, client_id: {}.", client.getClientId());
            return false;
        }

        X509Certificate cert = CertUtils.x509CertificateFromPem(clientCertAsPem);
        if (cert == null) {
            log.debug("Failed to parse client certificate, client_id: {}.", client.getClientId());
            return false;
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

        if (client.getAuthenticationMethod() == AuthenticationMethod.TLS_CLIENT_AUTH) {
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

        if (client.getAuthenticationMethod() == AuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH) { // disable it
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

        final String clientCertAsPem = httpRequest.getHeader("X-ClientCert");
        if (StringUtils.isBlank(clientCertAsPem)) {
            log.debug("Client certificate is missed in `X-ClientCert` header");
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
