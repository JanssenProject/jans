package org.gluu.oxauth.auth;

import com.google.common.base.Strings;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.jwk.JSONWebKey;
import org.gluu.oxauth.model.jwk.JSONWebKeySet;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.util.CertUtils;
import org.gluu.oxauth.model.util.JwtUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class MTLSService {

    private final static Logger log = LoggerFactory.getLogger(MTLSService.class);

    public boolean processMTLS(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain,
                               Client client, Authenticator authenticator, AbstractCryptoProvider cryptoProvider) throws Exception {
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

        if (client.getAuthenticationMethod() == AuthenticationMethod.TLS_CLIENT_AUTH) {

            final String subjectDn = client.getAttributes().getTlsClientAuthSubjectDn();
            if (StringUtils.isBlank(subjectDn)) {
                log.debug(
                        "SubjectDN is not set for client {} which is required to authenticate it via `tls_client_auth`.",
                        client.getClientId());
                return false;
            }

            // we check only `subjectDn`, the PKI certificate validation is performed by
            // apache/httpd
            if (subjectDn.equals(cert.getSubjectDN().getName())) {
                log.debug("Client {} authenticated via `tls_client_auth`.", client.getClientId());
                authenticator.configureSessionClient(client);

                filterChain.doFilter(httpRequest, httpResponse);
                return true;
            }
        }

        if (client.getAuthenticationMethod() == AuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH) { // disable it
            // temporarily
            final PublicKey publicKey = cert.getPublicKey();
            final byte[] encodedKey = publicKey.getEncoded();

            JSONObject jsonWebKeys = Strings.isNullOrEmpty(client.getJwks())
                    ? JwtUtil.getJSONWebKeys(client.getJwksUri())
                    : new JSONObject(client.getJwks());

            if (jsonWebKeys == null) {
                log.debug("Unable to load json web keys for client: {}, jwks_uri: {}, jks: {}", client.getClientId(),
                        client.getJwksUri(), client.getJwks());
                return false;
            }

            final JSONWebKeySet keySet = JSONWebKeySet.fromJSONObject(jsonWebKeys);
            for (JSONWebKey key : keySet.getKeys()) {
                if (ArrayUtils.isEquals(encodedKey,
                        cryptoProvider.getPublicKey(key.getKid(), jsonWebKeys).getEncoded())) {
                    log.debug("Client {} authenticated via `self_signed_tls_client_auth`, matched kid: {}.",
                            client.getClientId(), key.getKid());
                    authenticator.configureSessionClient(client);

                    filterChain.doFilter(httpRequest, httpResponse);
                    return true;
                }
            }
        }
        return false;
    }
}
