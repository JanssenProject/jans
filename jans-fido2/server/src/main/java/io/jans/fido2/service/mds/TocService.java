/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.mds;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.ArrayMap;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.OctetKeyPair;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.exception.mds.MdsClientException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.MetadataServer;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.Fido2Service;
import io.jans.fido2.service.app.ConfigurationFactory;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.service.document.store.exception.DocumentException;
import io.jans.service.document.store.model.Document;
import io.jans.service.document.store.service.DBDocumentService;
import io.jans.util.Pair;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ISO_DATE;

/**
 * TOC is parsed and Hashmap containing JSON object of individual Authenticators
 * is created.
 *
 */
@ApplicationScoped
public class TocService {

	@Inject
	private Logger log;

	@Inject
	private DataMapperService dataMapperService;

	@Inject
	private CertificateVerifier certificateVerifier;

	@Inject
	private CertificateService certificateService;

	@Inject
	private Base64Service base64Service;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private ConfigurationFactory configurationFactory;

	@Inject
	private FetchMdsProviderService fetchMdsProviderService;

	@Inject
	private DBDocumentService dbDocumentService;

	@Inject
	private Fido2Service fido2Service;

	private Map<String, JsonNode> tocEntries;

	private LocalDate nextUpdate;
	private MessageDigest digester;

	public LocalDate getNextUpdateDate() {
		return nextUpdate;
	}

	public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
		refresh();
		loadMetadataServiceExternalProvider();
	}

	public void refresh() {
		this.tocEntries = Collections.synchronizedMap(new HashMap<String, JsonNode>());
		if (appConfiguration.getFido2Configuration().isDisableMetadataService()) {
			log.debug("SkipDownloadMds is enabled");
		} else {
			tocEntries.putAll(parseTOCs());
		}
	}

	private Map<String, JsonNode> parseTOCs() {
		Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
		List<Map<String, JsonNode>> maps = new ArrayList<>();
		if (fido2Configuration == null) {
			log.warn("Fido2 configuration not exists");
			return new HashMap<String, JsonNode>();
		}

		String mdsTocRootCertsFolder = fido2Configuration.getMdsCertsFolder();
		if (StringHelper.isEmpty(mdsTocRootCertsFolder)) {
			log.warn("Fido2 MDS cert and TOC properties should be set");
			return new HashMap<String, JsonNode>();
		}
		log.info("Populating TOC certs entries from {}", mdsTocRootCertsFolder);

		try {
			Document tocRootCertsDocument = dbDocumentService.getDocumentByDisplayName("mdsTocsFolder");
			Pair<LocalDate, Map<String, JsonNode>> result = parseTOC(mdsTocRootCertsFolder, tocRootCertsDocument.getDocument());
			log.info("Get TOC {} entries with nextUpdate date {}", result.getSecond().size(), result.getFirst());

			maps.add(result.getSecond());
		} catch (Exception e) {
			log.warn("Can't access or open path: {}", e.getMessage(), e);
		} 

		return mergeAndResolveDuplicateEntries(maps);
	}

	private Pair<LocalDate, Map<String, JsonNode>> parseTOC(String mdsTocRootCertsFolder, String content)
			throws IOException, ParseException {
		String decodedString = new String(base64Service.decode(content));
		return readEntriesFromTocJWT(decodedString, mdsTocRootCertsFolder, true);
	}

	private Pair<LocalDate, Map<String, JsonNode>> parseTOC(String mdsTocRootCertsFolder, Path path)
			throws IOException, ParseException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			String tocJwt = reader.readLine();
			return readEntriesFromTocJWT(tocJwt, mdsTocRootCertsFolder, true);
		}
	}

	private JWSVerifier resolveVerifier(JWSAlgorithm algorithm, String mdsTocRootCertsFolder,
			List<String> certificateChain) {
		List<X509Certificate> x509CertificateChain = certificateService.getCertificates(certificateChain);
		List<X509Certificate> x509TrustedCertificates = certificateService.getCertificates(mdsTocRootCertsFolder);
		List<String> enabledFidoAlgorithms = appConfiguration.getFido2Configuration().getEnabledFidoAlgorithms();

		X509Certificate verifiedCert = certificateVerifier.verifyAttestationCertificates(x509CertificateChain,
				x509TrustedCertificates);
		// possible set of algos are : ES256, RS256, PS256, ED256, ED25519
		// no support for ED256 in JOSE library

		if (!(enabledFidoAlgorithms.contains(algorithm.getName())
				|| enabledFidoAlgorithms.contains(Curve.Ed25519.getName()))) {
			throw new Fido2RuntimeException("Unable to create a verifier for algorithm " + algorithm
					+ " as it is not supported. Add this algorithm in the FIDO2 configuration to support it.");
		}

		if (JWSAlgorithm.ES256.equals(algorithm)) {
			log.debug("resolveVerifier : ES256");
			try {
				return new ECDSAVerifier((ECPublicKey) verifiedCert.getPublicKey());
			} catch (JOSEException e) {
				throw new Fido2RuntimeException("Unable to create verifier for algorithm " + algorithm, e);
			}
		} else if (JWSAlgorithm.RS256.equals(algorithm) || JWSAlgorithm.PS256.equals(algorithm)) {
			log.debug("resolveVerifier : RS256");
			return new RSASSAVerifier((RSAPublicKey) verifiedCert.getPublicKey());
		} else if (JWSAlgorithm.EdDSA.equals(algorithm)
				&& ((OctetKeyPair) verifiedCert.getPublicKey()).getCurve().equals(Curve.Ed25519)) {
			log.debug("resolveVerifier : Ed25519");
			try {
				return new Ed25519Verifier((OctetKeyPair) verifiedCert.getPublicKey());
			} catch (JOSEException e) {
				throw new Fido2RuntimeException("Error during resolving Ed25519 verifier " + e.getMessage());
			}
		} else {
			throw new Fido2RuntimeException("Don't know what to do with " + algorithm);
		}
	}

	private MessageDigest resolveDigester(JWSAlgorithm algorithm) {
		// fix: algorithm RS256 added for
		// https://github.com/GluuFederation/fido2/issues/16
		if (JWSAlgorithm.ES256.equals(algorithm) || JWSAlgorithm.RS256.equals(algorithm)) {
			return DigestUtils.getSha256Digest();
		} else if (JWSAlgorithm.EdDSA.equals(algorithm)) {
			return DigestUtils.getSha512Digest();
		} else {
			throw new Fido2RuntimeException("Don't know what to do with " + algorithm);
		}
	}

	private Map<String, JsonNode> mergeAndResolveDuplicateEntries(List<Map<String, JsonNode>> maps) {
		Map<String, JsonNode> allEntries = new HashMap<>();
		Map<String, JsonNode> a[] = new Map[maps.size()];
		maps.toArray(a);

		allEntries.putAll(Stream.of(a).flatMap(m -> m.entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> {
					log.warn("Duplicate values {} {}", v1, v2);

					LocalDate dateV1 = getDate(v1);
					LocalDate dateV2 = getDate(v2);

					JsonNode result;
					if (dateV1.isAfter(dateV2)) {
						result = v1;
					} else {
						result = v2;
					}

					log.debug("Selected value {} ", result);

					return result;
				})));

		return allEntries;
	}

	private LocalDate getDate(JsonNode node) {
		JsonNode dateNode = node.get("timeOfLastStatusChange");
		LocalDate date;
		if (dateNode != null) {
			date = LocalDate.parse(dateNode.asText(), ISO_DATE);
		} else {
			date = LocalDate.now();
		}

		return date;
	}

	public JsonNode getAuthenticatorsMetadata(String aaguid) {

		return tocEntries.get(aaguid);
	}

	public MessageDigest getDigester() {
		return digester;
	}

	public boolean downloadMdsFromServer(URL metadataUrl) {

		Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();

		String mdsTocFilesFolder = fido2Configuration.getMdsTocsFolder();
		try {
			List<Document> documents = dbDocumentService.getDocumentsByFilePath(mdsTocFilesFolder);
			for (Document document : documents) {
				dbDocumentService.removeDocument(document);
			}
		} catch (Exception e) {
			log.error("Failed to remove old document of mdsTocFilesFolder", e);
			throw new DocumentException(e);
		}

		try (InputStream in = metadataUrl.openStream()) {
			byte[] sourceBytes = IOUtils.toByteArray(in);

			String encodedString = base64Service.encodeToString(sourceBytes);

			Document document = new Document();
			document.setFileName("mdsToc");
			document.setDescription("MDS TOC JWT file");
			document.setService("Fido2 MDS");
			document.setFilePath(mdsTocFilesFolder);
			try {
				document.setDocument(encodedString);
				document.setInum(dbDocumentService.generateInumForNewDocument());
				document.setDn(dbDocumentService.getDnForDocument(document.getInum()));
				document.setEnabled(true);
				dbDocumentService.addDocument(document);
			} catch (Exception e) {
				log.error("Failed to add new document of mdsTocFilesFolder", e);
				throw new DocumentException(e);
			}

			log.info("TOC file updated.");
			return true;
		} catch (IOException e) {
			log.warn("Can't access or open path: {}", metadataUrl, e);
			throw new Fido2RuntimeException("Can't access or open path: {}" + metadataUrl + e.getMessage(), e);
		}
	}

	private void loadMetadataServiceExternalProvider() {
		List<MetadataServer> metadataServers = appConfiguration.getFido2Configuration().getMetadataServers();
		Map<String, List<String>> updatedmetadataServers = new HashMap<>();
		if (metadataServers != null && !metadataServers.isEmpty()) {
			log.debug("metadataServers found: {}", metadataServers.size());
			try {
				for (MetadataServer metadataServer : metadataServers) {
					String blobJWT = fetchMdsProviderService.fetchMdsV3Endpoints(metadataServer.getUrl());
					Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
					String mdsTocRootCertsFolder = fido2Configuration.getMdsCertsFolder();
					List<String> documentsId = saveMetadataServerCertsInDB(metadataServer.getUrl(), blobJWT);
					updatedmetadataServers.put(metadataServer.getUrl(), documentsId);
					List<Map<String, JsonNode>> entryList = new ArrayList<>();
					try {
						Pair<LocalDate, Map<String, JsonNode>> dateMapPair = readEntriesFromTocJWT(blobJWT,
								mdsTocRootCertsFolder, false);
						entryList.add(dateMapPair.getSecond());
					} catch (Fido2RuntimeException e) {
						log.error(e.getMessage());
					}
					this.tocEntries.putAll(mergeAndResolveDuplicateEntries(entryList));
					log.info("🔐 MedataUrlsProvider successfully loaded");
				}

				List<MetadataServer> metadataServerList = new ArrayList<>();

				for (String metadataserverurl : updatedmetadataServers.keySet()) {
					MetadataServer metadataServer = new MetadataServer();
					metadataServer.setUrl(metadataserverurl);
					metadataServer.setCertificateDocumentInum(updatedmetadataServers.get(metadataserverurl));
					metadataServerList.add(metadataServer);
				}

				AppConfiguration updateAppConfiguration = configurationFactory.getAppConfiguration();
				updateAppConfiguration.getFido2Configuration().setMetadataServers(metadataServerList);
				fido2Service.merge(updateAppConfiguration);

			} catch (MdsClientException e) {
				log.error(e.getMessage());
			}
		} else {
			log.debug("MetadataUrlsProvider not found");
		}
	}

	public List<String> saveMetadataServerCertsInDB(String metadataServer, String blobJWT) {
		List<String> result = new ArrayList<>();
		log.debug("Attempting reading entries from JWT: {}", StringUtils.abbreviateMiddle(blobJWT, "...", 100));
		JWSObject blobDecoded;
		try {
			blobDecoded = JWSObject.parse(blobJWT);
		} catch (ParseException e) {
			throw new Fido2RuntimeException("Error when parsing TOC JWT: " + e.getMessage(), e);
		}
		List<String> headerCertificatesX5c = blobDecoded.getHeader().getX509CertChain().stream()
				.map(c -> base64Service.encodeToString(c.decode())).collect(Collectors.toList());
		int index = 0;
		if (!headerCertificatesX5c.isEmpty()) {
			List<Document> oldCerts = dbDocumentService.searchDocuments(metadataServer, 100);
			for (Document certDoc : oldCerts) {
				try {
					dbDocumentService.removeDocument(certDoc);
				} catch (Exception e) {
					log.error("Failed to remove document file[ath:'" + certDoc.getFilePath() + "' : ", e);
					throw new DocumentException(e);
				}
			}

			for (String cert : headerCertificatesX5c) {
				Document document = new Document();
				document.setFileName(metadataServer + "_" + (index++));
				document.setDescription("metadata certificate for " + metadataServer);
				document.setService("Fido2 MDS");
				try {
					document.setDocument(cert);
					document.setInum(dbDocumentService.generateInumForNewDocument());
					document.setDn(dbDocumentService.getDnForDocument(document.getInum()));
					document.setEnabled(true);
					dbDocumentService.addDocument(document);
					result.add(document.getInum());
				} catch (Exception e) {
					log.error("Failed to add document for  '" + document.getFileName() + ", message: " + e.getMessage(),
							e);
					throw new DocumentException(e);
				}
			}
		}
		return result;
	}

	private Pair<LocalDate, Map<String, JsonNode>> readEntriesFromTocJWT(String tocJwt, String mdsTocRootCertsFolder,
			boolean loadGlobalVariables) {
		log.debug("Attempting reading entries from JWT: {}", StringUtils.abbreviateMiddle(tocJwt, "...", 100));
		JWSObject blobDecoded;
		try {
			blobDecoded = JWSObject.parse(tocJwt);
		} catch (ParseException e) {
			throw new Fido2RuntimeException("Error when parsing TOC JWT: " + e.getMessage(), e);
		}
		JWSAlgorithm algorithm = blobDecoded.getHeader().getAlgorithm();
		List<String> headerCertificatesX5c = blobDecoded.getHeader().getX509CertChain().stream()
				.map(c -> base64Service.encodeToString(c.decode())).collect(Collectors.toList());
		// If the x5u attribute is present in the JWT Header then
		// if (jwsObject.getHeader().getX509CertURL() != null) {
		// 1. The FIDO Server MUST verify that the URL specified by the x5u attribute
		// has the same web-origin as the URL used to download the metadata BLOB from.
		// The FIDO Server SHOULD ignore the file if the web-origin differs (in order to
		// prevent loading objects from arbitrary sites).
		// 2. The FIDO Server MUST download the certificate (chain) from the URL
		// specified by the x5u attribute [JWS]. The certificate chain MUST be verified
		// to properly chain to the metadata BLOB signing trust anchor according to
		// [RFC5280]. All certificates in the chain MUST be checked for revocation
		// according to [RFC5280].
		// 3. The FIDO Server SHOULD ignore the file if the chain cannot be verified or
		// if one of the chain certificates is revoked.

		// the chain should be retrieved from the x5c attribute.
		// else if (certificateChain.isEmpty()) {
		// The FIDO Server SHOULD ignore the file if the chain cannot be verified or if
		// one of the chain certificates is revoked.
		// } else {
		log.info("Metadata BLOB signing trust anchor is considered the BLOB signing certificate chain");
		// Metadata BLOB signing trust anchor is considered the BLOB signing certificate
		// chain.
		// Verify the signature of the Metadata BLOB object using the BLOB signing
		// certificate chain (as determined by the steps above). The FIDO Server SHOULD
		// ignore the file if the signature is invalid. It SHOULD also ignore the file
		// if its number (no) is less or equal to the number of the last Metadata BLOB
		// object cached locally.
		// }
		try {
			JWSVerifier verifier = resolveVerifier(algorithm, mdsTocRootCertsFolder, headerCertificatesX5c);
			if (!blobDecoded.verify(verifier)) {
				throw new Fido2RuntimeException("Unable to verify JWS object using algorithm: " + algorithm);
			}
		} catch (Exception e) {
			throw new Fido2RuntimeException(
					"Unable to verify JWS object using algorithm: " + algorithm + ", message: " + e.getMessage(), e);
		}

		JsonNode toc;
		try {
			toc = dataMapperService.readTree(blobDecoded.getPayload().toString());
		} catch (IOException e) {
			throw new Fido2RuntimeException("Error when read JWT payload: " + e.getMessage(), e);
		}
		if (loadGlobalVariables) {
			this.nextUpdate = LocalDate.parse(toc.get("nextUpdate").asText(), ISO_DATE);
			this.digester = resolveDigester(algorithm);
		}

		JsonNode entriesNode = toc.get("entries");
		log.debug("Legal header: {}", toc.get("legalHeader"));
		// The serial number of this UAF Metadata BLOB Payload. Serial numbers MUST be
		// consecutive and strictly monotonic, i.e. the successor BLOB will have a no
		// value exactly incremented by one.
		log.debug("Property 'no' value: {}. serialNo: {}", toc.get("no").asInt(), entriesNode.size());

		Map<String, JsonNode> entries = new HashMap<>();
		for (JsonNode metadataEntryNode : entriesNode) {
			if (metadataEntryNode.hasNonNull("aaguid")) {
				String aaguid = metadataEntryNode.get("aaguid").asText();
				try {
					certificateVerifier.verifyStatusAcceptable(aaguid, metadataEntryNode);
					if (!metadataEntryNode.has("metadataStatement")) {
						log.warn("This entry doesn't contains metadataStatement");
						continue;
					}
					entries.put(aaguid, metadataEntryNode);
					log.info("Added TOC entry: {} ", aaguid);
				} catch (Fido2RuntimeException e) {
					log.error(e.getMessage());
				}
			} else if (metadataEntryNode.hasNonNull("aaid")) {
				String aaid = metadataEntryNode.get("aaid").asText();
				log.debug("TODO: handle aaid addition to tocEntries {}", aaid);
			} else if (metadataEntryNode.hasNonNull("attestationCertificateKeyIdentifiers")) {
				// FIDO U2F authenticators do not support AAID nor AAGUID, but they use
				// attestation certificates dedicated to a single authenticator model.
				String attestationCertificateKeyIdentifiers = metadataEntryNode
						.get("attestationCertificateKeyIdentifiers").toString();
				try {

					List<String> attestationCertificateKeyIdentifiersList = dataMapperService
							.readValue(attestationCertificateKeyIdentifiers, List.class);
					for (String attestationCertificateKeyIdentifier : attestationCertificateKeyIdentifiersList) {
						entries.put(attestationCertificateKeyIdentifier, entriesNode);
						log.info("Added TOC entry: {} ", attestationCertificateKeyIdentifier);
					}
				} catch (IOException e) {
					log.error("Failed to add attestationCertificateKeyIdentifiers addition to tocEntries :"
							+ attestationCertificateKeyIdentifiers);
					continue;
				}
			} else {
				log.debug("Null aaguid, aaid, attestationCertificateKeyIdentifiers - Added TOC entry with status {}",
						metadataEntryNode.get("statusReports").findValue("status"));
			}
		}

		LocalDate nextUpdateDate = LocalDate.parse(toc.get("nextUpdate").asText());
		return new Pair<>(nextUpdateDate, entries);
	}
}