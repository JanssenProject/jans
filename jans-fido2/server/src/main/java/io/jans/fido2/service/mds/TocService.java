/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.mds;

import static java.time.format.DateTimeFormatter.ISO_DATE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
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
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.MetadataServer;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.DataMapperService;
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
	private DBDocumentService dbDocumentService;

	private Map<String, JsonNode> tocEntries;

	private LocalDate nextUpdate;
	private MessageDigest digester;

	public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
		fetchMetadata();
	}

	public void refreshTOCEntries() {
		this.tocEntries = Collections.synchronizedMap(new HashMap<String, JsonNode>());
		if (appConfiguration.getFido2Configuration().isDisableMetadataService()) {
			log.debug("SkipDownloadMds is enabled");
		} else {
			tocEntries.putAll(parseTOCs());
		}
	}

	public void fetchMetadata() {

		try {
			if (appConfiguration.getFido2Configuration().isDisableMetadataService()) {
				log.debug("SkipDownloadMds is enabled");
			} else {

				LocalDate nextUpdate = getNextUpdateDate();

				if (nextUpdate == null || nextUpdate.equals(LocalDate.now()) || nextUpdate.isBefore(LocalDate.now())) {
					log.info("Downloading the latest TOC from https://mds.fidoalliance.org/");
					MetadataServer metaDataServer = (MetadataServer) (appConfiguration.getFido2Configuration()
							.getMetadataServers().get(0));

					// as of now, we have only one metadata server, hence get(0), I cant envisage
					// why there will be multiple metadata servers
					boolean success = downloadMdsFromServer(new URL(metaDataServer.getUrl()));
					if (success) {
						refreshTOCEntries();
						saveNextUpdateDateOfTheMDS();
					}

				}
			}

		} catch (MalformedURLException e) {
			log.error("Error while parsing the FIDO alliance URL :", e);
			return;
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
			Document mdsDocument = dbDocumentService.getDocumentByDisplayName("mdsTocsFolder");
			Pair<LocalDate, Map<String, JsonNode>> result = parseTOC(mdsTocRootCertsFolder, mdsDocument.getDocument());
			log.info("Get TOC {} entries with nextUpdate date {}", result.getSecond().size(), result.getFirst());

			maps.add(result.getSecond());
		} catch (Exception e) {
			log.warn("Can't access document : {}", e.getMessage(), e);
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
		log.info("🔐 MedataUrlsProvider successfully loaded");
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

		try (InputStream in = metadataUrl.openStream()) {
			byte[] sourceBytes = IOUtils.toByteArray(in);

			String encodedString = base64Service.encodeToString(sourceBytes);

			try {
				Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
				String mdsTocFilesFolder = fido2Configuration.getMdsTocsFolder();

				Document document = dbDocumentService.getDocumentsByFilePath(mdsTocFilesFolder).get(0);
				document.setDocument(encodedString);
				document.setFilePath(mdsTocFilesFolder);
				dbDocumentService.updateDocument(document);
				return true;
			} catch (Exception e) {
				log.error("Failed to add new document of mdsTocFilesFolder", e);
				throw new DocumentException(e);
			}

		} catch (IOException e) {
			log.warn("Can't access document {}", metadataUrl, e);
			throw new Fido2RuntimeException("Can't access or open path: {}" + metadataUrl + e.getMessage(), e);
		}
	}

	public boolean saveNextUpdateDateOfTheMDS() {

		try {
			Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
			String mdsTocFilesFolder = fido2Configuration.getMdsTocsFolder();

			Document document = dbDocumentService.getDocumentsByFilePath(mdsTocFilesFolder).get(0);
			document.setDescription(localDateToString(nextUpdate));

			dbDocumentService.updateDocument(document);
			log.debug("TOC file updated.");
			return true;
		} catch (Exception e) {
			log.error("Failed to Save the nextUpdateDate of the MDS into jansDocument ", e);
			throw new DocumentException(e);
		}
	}

	public LocalDate getNextUpdateDate() {

		try {
			Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
			String mdsTocFilesFolder = fido2Configuration.getMdsTocsFolder();

			Document document = dbDocumentService.getDocumentsByFilePath(mdsTocFilesFolder).get(0);
			return (document.getDescription() == null || "mdsTocsFolder".equals(document.getDescription())) ? null : stringToLocalDate(document.getDescription().toString());

		} catch (Exception e) {
			log.error("Failed to get nextUpdateDate of the MDS from jansDocument ", e);
			throw new DocumentException(e);
		}
	}

	private LocalDate stringToLocalDate(String date) {
		return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
	}

	private String localDateToString(LocalDate date) {
		return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	private Pair<LocalDate, Map<String, JsonNode>> readEntriesFromTocJWT(String tocJwt, String mdsTocRootCertsFolder,
			boolean loadGlobalVariables) {
		log.debug("Attempting reading entries from JWT: {}", StringUtils.abbreviateMiddle(tocJwt, "...", 100));

		JWSObject blobDecoded = parseJwt(tocJwt);
		JWSAlgorithm algorithm = blobDecoded.getHeader().getAlgorithm();
		List<String> headerCertificatesX5c = getHeaderCertificatesX5c(blobDecoded);

		verifyJwsSignature(blobDecoded, mdsTocRootCertsFolder, headerCertificatesX5c, algorithm);

		JsonNode toc = parseTocPayload(blobDecoded);

		if (loadGlobalVariables) {
			loadGlobalVariables(toc, algorithm);
		}

		JsonNode entriesNode = toc.get("entries");
		log.debug("Legal header: {}", toc.get("legalHeader"));
		log.debug("Property 'no' value: {}. serialNo: {}", toc.get("no").asInt(), entriesNode.size());

		Map<String, JsonNode> entries = processMetadataEntries(entriesNode);

		LocalDate nextUpdateDate = LocalDate.parse(toc.get("nextUpdate").asText());
		return new Pair<>(nextUpdateDate, entries);
	}

	private JWSObject parseJwt(String tocJwt) {
		try {
			return JWSObject.parse(tocJwt);
		} catch (ParseException e) {
			throw new Fido2RuntimeException("Error when parsing TOC JWT: " + e.getMessage(), e);
		}
	}

	private List<String> getHeaderCertificatesX5c(JWSObject blobDecoded) {
		return blobDecoded.getHeader().getX509CertChain().stream().map(c -> base64Service.encodeToString(c.decode()))
				.collect(Collectors.toList());
	}

	private void verifyJwsSignature(JWSObject blobDecoded, String mdsTocRootCertsFolder,
			List<String> headerCertificatesX5c, JWSAlgorithm algorithm) {
		try {
			JWSVerifier verifier = resolveVerifier(algorithm, mdsTocRootCertsFolder, headerCertificatesX5c);
			if (!blobDecoded.verify(verifier)) {
				throw new Fido2RuntimeException("Unable to verify JWS object using algorithm: " + algorithm);
			}
		} catch (Exception e) {
			throw new Fido2RuntimeException(
					"Unable to verify JWS object using algorithm: " + algorithm + ", message: " + e.getMessage(), e);
		}
	}

	private JsonNode parseTocPayload(JWSObject blobDecoded) {
		try {
			return dataMapperService.readTree(blobDecoded.getPayload().toString());
		} catch (IOException e) {
			throw new Fido2RuntimeException("Error when reading JWT payload: " + e.getMessage(), e);
		}
	}

	private void loadGlobalVariables(JsonNode toc, JWSAlgorithm algorithm) {
		this.nextUpdate = LocalDate.parse(toc.get("nextUpdate").asText(), ISO_DATE);
		this.digester = resolveDigester(algorithm);
	}

	private Map<String, JsonNode> processMetadataEntries(JsonNode entriesNode) {
		Map<String, JsonNode> entries = new HashMap<>();

		for (JsonNode metadataEntryNode : entriesNode) {
			Optional<String> aaguid = Optional.ofNullable(metadataEntryNode.get("aaguid")).map(JsonNode::asText);
			Optional<String> aaid = Optional.ofNullable(metadataEntryNode.get("aaid")).map(JsonNode::asText);
			Optional<String> attestationCertificateKeyIdentifiers = Optional
					.ofNullable(metadataEntryNode.get("attestationCertificateKeyIdentifiers")).map(JsonNode::toString);

			if (aaguid.isPresent()) {
				processAaguidEntry(entries, metadataEntryNode, aaguid.get());
			} else if (aaid.isPresent()) {
				log.debug("TODO: handle aaid addition to tocEntries {}", aaid.get());
			} else if (attestationCertificateKeyIdentifiers.isPresent()) {
				processAttestationCertificateKeyIdentifiers(entries, entriesNode,
						attestationCertificateKeyIdentifiers.get());
			} else {
				log.debug("Null aaguid, aaid, attestationCertificateKeyIdentifiers - Added TOC entry with status {}",
						metadataEntryNode.get("statusReports").findValue("status"));
			}
		}

		return entries;
	}

	private void processAaguidEntry(Map<String, JsonNode> entries, JsonNode metadataEntryNode, String aaguid) {
		try {
			certificateVerifier.verifyStatusAcceptable(aaguid, metadataEntryNode);
			if (!metadataEntryNode.has("metadataStatement")) {
				log.warn("This entry doesn't contain metadataStatement");
			}
			entries.put(aaguid, metadataEntryNode);
			log.info("Added TOC entry: {} ", aaguid);
		} catch (Fido2RuntimeException e) {
			log.error(e.getMessage());
		}
	}

	private void processAttestationCertificateKeyIdentifiers(Map<String, JsonNode> entries, JsonNode entriesNode,
			String attestationCertificateKeyIdentifiers) {
		try {
			List<String> keyIdentifiersList = dataMapperService.readValue(attestationCertificateKeyIdentifiers,
					List.class);
			for (String keyIdentifier : keyIdentifiersList) {
				entries.put(keyIdentifier, entriesNode);
				log.info("Added TOC entry: {} ", keyIdentifier);
			}
		} catch (IOException e) {
			log.error("Failed to add attestationCertificateKeyIdentifiers to tocEntries: {}",
					attestationCertificateKeyIdentifiers);
		}
	}
}