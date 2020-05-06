package org.gluu.oxauth.fido2.service.mds;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Hex;
import org.gluu.oxauth.fido2.cryptoutils.CryptoUtils;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.model.auth.AuthData;
import org.gluu.oxauth.fido2.service.KeyStoreCreator;
import org.gluu.oxauth.fido2.service.verifier.CommonVerifiers;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.configuration.Fido2Configuration;
import org.gluu.service.cdi.event.ApplicationInitialized;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

@ApplicationScoped
public class AuthCertService {

	@Inject
	private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

	@Inject
	private KeyStoreCreator keyStoreCreator;

	@Inject
	private CryptoUtils cryptoUtils;

	@Inject
	private CommonVerifiers commonVerifiers;

	@Inject
	private MdsService mdsService;

	@Inject
	private LocalMdsService localMdsService;

	private List<X509Certificate> authenticatorCerts;

	public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
        Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
        if (fido2Configuration == null) {
            return;
        }

        String authenticatorCertsFolder = appConfiguration.getFido2Configuration().getAuthenticatorCertsFolder();
        this.authenticatorCerts = cryptoUtils.getCertificates(authenticatorCertsFolder);
	}

	private List<X509Certificate> getCertificates(JsonNode metadataNode) {
		if (metadataNode == null || !metadataNode.has("attestationRootCertificates")) {
			return Collections.emptyList();
		}

		ArrayNode node = (ArrayNode) metadataNode.get("attestationRootCertificates");
		Iterator<JsonNode> iter = node.elements();
		List<String> x509certificates = new ArrayList<>();
		while (iter.hasNext()) {
			JsonNode certNode = iter.next();
			x509certificates.add(certNode.asText());

		}

		return cryptoUtils.getCertificates(x509certificates);
	}

	public List<X509Certificate> getCertificates(AuthData authData) {
		String aaguid = Hex.encodeHexString(authData.getAaguid());

		JsonNode metadataForAuthenticator = localMdsService.getAuthenticatorsMetadata(aaguid);
		if (metadataForAuthenticator == null) {
			try {
				log.info("No metadata for authenticator {}. Attempting to contact MDS", aaguid);
				JsonNode metadata = mdsService.fetchMetadata(authData.getAaguid());
				commonVerifiers.verifyThatMetadataIsValid(metadata);
				localMdsService.registerAuthenticatorsMetadata(aaguid, metadata);
				metadataForAuthenticator = metadata;
	
				return getCertificates(metadataForAuthenticator);
			} catch (Fido2RPRuntimeException ex) {
				log.warn("Faield to get metadaa from Fido2 meta-data server");
			}
		}
		
		return authenticatorCerts;
	}

	public KeyStore getCertificationKeyStore(String aaguid, List<X509Certificate> certificates) {
		return keyStoreCreator.createKeyStore(aaguid, certificates);
	}

	public X509TrustManager populateTrustManager(AuthData authData) {
		String aaguid = Hex.encodeHexString(authData.getAaguid());
		List<X509Certificate> trustedCertificates = getCertificates(authData);
		KeyStore keyStore = getCertificationKeyStore(aaguid, trustedCertificates);

		TrustManagerFactory trustManagerFactory = null;
		try {
			trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);
			TrustManager[] tms = trustManagerFactory.getTrustManagers();

			return (X509TrustManager) tms[0];
		} catch (NoSuchAlgorithmException | KeyStoreException e) {
			log.error("Unrecoverable problem with the platform", e);
			return null;
		}
	}
}
