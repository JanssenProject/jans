package org.xdi.oxauth.auth.mtls;

import com.google.common.base.Strings;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.interception.MTLSInterception;
import org.xdi.oxauth.interception.MTLSInterceptionInterface;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;
import org.xdi.oxauth.model.ref.AuthenticatorReference;
import org.xdi.oxauth.model.ref.ClientReference;
import org.xdi.oxauth.model.util.CertUtils;
import org.xdi.oxauth.model.util.JwtUtil;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * @author Yuriy Zabrovarnyy
 */
@Interceptor
@MTLSInterception
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class MTLSInterceptor implements MTLSInterceptionInterface, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6153350621622208537L;
	private final static Logger log = LoggerFactory.getLogger(MTLSInterceptor.class);

	public MTLSInterceptor() {
		log.info("MTLS Interceptor loaded.");
	}

	@AroundInvoke
	public Object processMTLS(InvocationContext ctx) {
		log.debug("processMTLS...");
		try {
			HttpServletRequest httpRequest = (HttpServletRequest) ctx.getParameters()[0];
			HttpServletResponse httpResponse = (HttpServletResponse) ctx.getParameters()[1];
			FilterChain filterChain = (FilterChain) ctx.getParameters()[2];
			ClientReference client = (ClientReference) ctx.getParameters()[3];
			AuthenticatorReference authenticator = (AuthenticatorReference) ctx.getParameters()[4];
			AbstractCryptoProvider cryptoProvider = (AbstractCryptoProvider) ctx.getParameters()[5];
			final boolean result = processMTLS(httpRequest, httpResponse, filterChain, client, authenticator,
					cryptoProvider);
			ctx.proceed();
			return result;
		} catch (Exception e) {
			log.error("Failed to process MTLS.", e);
			return false;
		}
	}

	public boolean processMTLS(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
			FilterChain filterChain, ClientReference client, AuthenticatorReference authenticator,
			AbstractCryptoProvider cryptoProvider) throws Exception {

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
				authenticator.configureSessionClient();

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
					authenticator.configureSessionClient();

					filterChain.doFilter(httpRequest, httpResponse);
					return true;
				}
			}
		}
		return false;
	}
}
