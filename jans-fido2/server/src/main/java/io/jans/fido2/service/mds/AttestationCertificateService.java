/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.mds;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Hex;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.KeyStoreCreator;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.service.cdi.event.ApplicationInitialized;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class AttestationCertificateService {

	@Inject
	private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

	@Inject
	private KeyStoreCreator keyStoreCreator;

	@Inject
	private CertificateService certificateService;

	@Inject
	private CommonVerifiers commonVerifiers;

	@Inject
	private MdsService mdsService;

	@Inject
	private LocalMdsService localMdsService;

    @Inject
    private DataMapperService dataMapperService;

	private Map<String, X509Certificate> rootCertificatesMap;

	public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
        Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
        if (fido2Configuration == null) {
            return;
        }

        String authenticatorCertsFolder = appConfiguration.getFido2Configuration().getAuthenticatorCertsFolder();
        this.rootCertificatesMap = certificateService.getCertificatesMap(authenticatorCertsFolder);
	}

	public List<X509Certificate> getAttestationRootCertificates(JsonNode metadataNode,
			List<X509Certificate> attestationCertificates) {
		JsonNode metaDataStatement = null;
		// incase of u2f-fido2 attestation
		if (metadataNode != null) {
			try {
				metaDataStatement = dataMapperService.readTree(metadataNode.get("metadataStatement").toPrettyString());
			} catch (IOException e) {
				log.error("Error parsing the metadata statement", e);
			}
		}

		if (metadataNode == null || metaDataStatement == null
				|| !metaDataStatement.has("attestationRootCertificates")) {
			List<X509Certificate> selectedRootCertificate = certificateService
					.selectRootCertificates(rootCertificatesMap, attestationCertificates);
			return selectedRootCertificate;
		}

		ArrayNode node = (ArrayNode) metaDataStatement.get("attestationRootCertificates");
		Iterator<JsonNode> iter = node.elements();
		List<String> x509certificates = new ArrayList<>();
		while (iter.hasNext()) {
			JsonNode certNode = iter.next();
			x509certificates.add(certNode.asText());
		}

		return certificateService.getCertificates(x509certificates);
	}

	public List<X509Certificate> getAttestationRootCertificates(AuthData authData, List<X509Certificate> attestationCertificates) {
		String aaguid = Hex.encodeHexString(authData.getAaguid());

		JsonNode metadataForAuthenticator = localMdsService.getAuthenticatorsMetadata(aaguid);
		if (metadataForAuthenticator == null) {
			try {
				log.info("No Local metadata for authenticator {}. Checking for metadata MDS3 blob", aaguid);
				JsonNode metadata = mdsService.fetchMetadata(authData.getAaguid());
				commonVerifiers.verifyThatMetadataIsValid(metadata);
				
				return getAttestationRootCertificates(metadata, attestationCertificates);
			} catch (Fido2RuntimeException ex) {
				log.warn("Failed to get metadata from Fido2 meta-data server");
				
				metadataForAuthenticator = dataMapperService.createObjectNode();
			}
		}

		return getAttestationRootCertificates(metadataForAuthenticator, attestationCertificates);
	}

	public X509TrustManager populateTrustManager(AuthData authData, List<X509Certificate> attestationCertificates) {
		String aaguid = Hex.encodeHexString(authData.getAaguid());
		List<X509Certificate> trustedCertificates = getAttestationRootCertificates(authData, attestationCertificates);
		if ((trustedCertificates == null) || (trustedCertificates.size() == 0)) {
			log.error("Failed to get trusted certificates");
			return null;
		}

		KeyStore keyStore = getCertificationKeyStore(aaguid, trustedCertificates);

		TrustManagerFactory trustManagerFactory = null;
		try {
			trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);
			TrustManager[] tms = trustManagerFactory.getTrustManagers();

			return (X509TrustManager) tms[0];
		} catch (NoSuchAlgorithmException | KeyStoreException e) {
			log.error("Failed to initialize trust manager", e);
			return null;
		}
	}

	private KeyStore getCertificationKeyStore(String aaguid, List<X509Certificate> certificates) {
		return keyStoreCreator.createKeyStore(aaguid, certificates);
	}

}