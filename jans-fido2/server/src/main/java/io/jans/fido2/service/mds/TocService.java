/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.mds;

import static java.time.format.DateTimeFormatter.ISO_DATE;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
import io.jans.service.cdi.async.Asynchronous;
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

	private static final String ADDED_TOC_ENTRY_MSG = "Added TOC entry: {} ";

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

	// tocEntries is populated by the asynchronous startup init()/fetchMetadata() (and the MDS3 update
	// timer) on a background thread, but read by request-handling threads. An AtomicReference gives it
	// safe cross-thread publication and lets each refresh atomically swap in a fully-built map.
	private final AtomicReference<Map<String, JsonNode>> tocEntries = new AtomicReference<>();

	// nextUpdate is written and read within the same fetchMetadata() flow; volatile keeps it visible
	// should a later refresh run on a different pool thread. LocalDate is immutable, so this is safe.
	private volatile LocalDate nextUpdate;
	private MessageDigest digester;

	/**
	 * Loads the MDS TOC at server startup.
	 * <p>
	 * Runs asynchronously (via the {@link Asynchronous} interceptor, which the container applies when
	 * it dispatches this observer) so the potentially slow download — including the retry loop when the
	 * TOC blob is missing — never blocks application initialization. The FIDO2 server keeps starting up
	 * while the TOC is fetched in the background; requests that need it before it's ready are already
	 * handled defensively (see {@link #getAuthenticatorsMetadata(String)}). We retry the download a few
	 * times only when the TOC blob is missing, because without it the server can't validate attestations.
	 */
	@Asynchronous
	public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
		fetchMetadata(true);
	}

	public void refreshTOCEntries() {
		Map<String, JsonNode> entries = Collections.synchronizedMap(new HashMap<>());
		if (appConfiguration.getFido2Configuration().isDisableMetadataService()) {
			log.debug("SkipDownloadMds is enabled");
		} else {
			entries.putAll(parseTOCs());
		}
		// Atomically publish the fully-built map so readers never observe a half-populated one.
		tocEntries.set(entries);
	}

	public void fetchMetadata() {
		fetchMetadata(false);
	}

	/**
	 * Fetches the MDS TOC blob.
	 *
	 * @param retryWhenTocMissing when {@code true} (server startup) and the MDS TOC blob is missing
	 *        from the DB, the download is retried a few times before giving up. A missing TOC blob
	 *        prevents the FIDO2 server from validating attestations, so it's worth a few extra
	 *        attempts to recover from a transient outage of the FIDO Alliance metadata service. When
	 *        the TOC is merely stale (present but past its {@code nextUpdate}) a single attempt is
	 *        made, matching the behaviour of the daily {@code MDS3UpdateTimer}.
	 */
	public void fetchMetadata(boolean retryWhenTocMissing) {
		if (appConfiguration.getFido2Configuration().isDisableMetadataService()) {
			log.debug("SkipDownloadMds is enabled");
			return;
		}

		int maxAttempts = 1;
		if (retryWhenTocMissing && isTocContentMissing()) {
			maxAttempts = Math.max(1, appConfiguration.getFido2Configuration().getMdsDownloadStartupRetries());
			log.info("MDS TOC blob is missing at startup, will attempt to download it up to {} time(s)", maxAttempts);
		}

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				fetchMetadataOnce();
			} catch (Exception e) {
				log.error("Attempt {}/{} to download the MDS TOC failed: {}", attempt, maxAttempts, e.getMessage(), e);
			}

			// Stop as soon as the TOC is available; only keep retrying while it's still missing.
			if (attempt >= maxAttempts || !isTocContentMissing()) {
				return;
			}

			sleepBeforeRetry();
		}
	}

	// package-private for testability (spied in TocServiceTest)
	void fetchMetadataOnce() {
		LocalDate nextUpdateOn = getNextUpdateDate();

		if (nextUpdateOn == null || nextUpdateOn.equals(LocalDate.now()) || nextUpdateOn.isBefore(LocalDate.now())) {
			log.info("Downloading the latest TOC from https://mds.fidoalliance.org/");
			MetadataServer metaDataServer;
			try {
				metaDataServer = appConfiguration.getFido2Configuration().getMetadataServers().get(0);
			} catch (IndexOutOfBoundsException e) {
				throw new Fido2RuntimeException("No FIDO2 metadata server is configured", e);
			}

			// as of now, we have only one metadata server, hence get(0), I cant envisage
			// why there will be multiple metadata servers
			URL metadataUrl;
			try {
				metadataUrl = new URL(metaDataServer.getUrl());
			} catch (MalformedURLException e) {
				throw new Fido2RuntimeException("Error while parsing the FIDO alliance URL: " + e.getMessage(), e);
			}

			boolean success = downloadMdsFromServer(metadataUrl);
			if (success) {
				refreshTOCEntries();
				saveNextUpdateDateOfTheMDS();
			}
		}
	}

	/**
	 * Returns {@code true} when the MDS TOC blob is absent or empty in the DB. This is the only
	 * condition under which the startup download is retried.
	 * <p>
	 * package-private for testability (spied/exercised in TocServiceTest).
	 */
	boolean isTocContentMissing() {
		try {
			Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
			String mdsTocFilesFolder = fido2Configuration.getMdsTocsFolder();

			List<Document> documents = dbDocumentService.getDocumentsByFilePath(mdsTocFilesFolder);
			if (documents == null || documents.isEmpty()) {
				return true;
			}
			return StringHelper.isEmpty(documents.get(0).getDocument());
		} catch (Exception e) {
			log.warn("Unable to determine whether the MDS TOC blob exists, treating it as missing: {}", e.getMessage());
			return true;
		}
	}

	// package-private for testability (stubbed in TocServiceTest to avoid real waits)
	void sleepBeforeRetry() {
		int retryIntervalSeconds = Math.max(1,
				appConfiguration.getFido2Configuration().getMdsDownloadStartupRetryInterval());
		log.info("Retrying MDS TOC download in {} second(s)", retryIntervalSeconds);
		try {
			Thread.sleep(retryIntervalSeconds * 1000L);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("MDS TOC download retry wait was interrupted");
		}
	}

	private Map<String, JsonNode> parseTOCs() {
		Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
		List<Map<String, JsonNode>> maps = new ArrayList<>();
		
		String mdsTocRootCertsFolder = fido2Configuration.getMdsCertsFolder();
		if (StringHelper.isEmpty(mdsTocRootCertsFolder)) {
			log.warn("Fido2 MDS cert and TOC properties should be set");
			return new HashMap<>();
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

	private Pair<LocalDate, Map<String, JsonNode>> parseTOC(String mdsTocRootCertsFolder, String content) {
		String decodedString = new String(base64Service.decode(content));
		return readEntriesFromTocJWT(decodedString, mdsTocRootCertsFolder, true);
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
		Map<String, JsonNode>[] a = new Map[maps.size()];
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
		Map<String, JsonNode> entries = tocEntries.get();
		if (entries == null) {
			log.warn("TOC entries map is null");
			return null;
		}
		JsonNode entry = entries.get(aaguid);
		if (entry == null) {
			log.warn("No entry found for AAGUID: {}", aaguid);
		}
		return entry;
	}

	public MessageDigest getDigester() {
		return digester;
	}

	public boolean downloadMdsFromServer(URL metadataUrl) {

		try (InputStream in = metadataUrl.openStream()) {
			byte[] sourceBytes = IOUtils.toByteArray(in);

			String encodedString = base64Service.encodeToString(sourceBytes);

			return persistTocDocument(encodedString);

		} catch (IOException e) {
			log.warn("Can't access document {}", metadataUrl, e);
			throw new Fido2RuntimeException("Can't access or open path: {}" + metadataUrl + e.getMessage(), e);
		}
	}

	private boolean persistTocDocument(String encodedString) {
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
			return (document.getDescription() == null || "mdsTocsFolder".equals(document.getDescription())) ? null : stringToLocalDate(document.getDescription());

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
		if (log.isDebugEnabled()) {
			log.debug("Attempting reading entries from JWT: {}", StringUtils.abbreviateMiddle(tocJwt, "...", 100));
		}

		JWSObject blobDecoded = parseJwt(tocJwt);
		JWSAlgorithm algorithm = blobDecoded.getHeader().getAlgorithm();
		List<String> headerCertificatesX5c = getHeaderCertificatesX5c(blobDecoded);

		verifyJwsSignature(blobDecoded, mdsTocRootCertsFolder, headerCertificatesX5c, algorithm);

		JsonNode toc = parseTocPayload(blobDecoded);

		if (loadGlobalVariables) {
			loadGlobalVariables(toc, algorithm);
		}

		JsonNode entriesNode = toc.get("entries");
		if (log.isDebugEnabled()) {
			log.debug("Legal header: {}", toc.get("legalHeader"));
			log.debug("Property 'no' value: {}. serialNo: {}", toc.get("no").asInt(), entriesNode.size());
		}

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
				processAaidEntry(entries, metadataEntryNode, aaid.get());
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
			log.info(ADDED_TOC_ENTRY_MSG, aaguid);
		} catch (Fido2RuntimeException e) {
			log.error(e.getMessage());
		}
	}

	private void processAaidEntry(Map<String, JsonNode> entries, JsonNode metadataEntryNode, String aaid) {
		try {
			certificateVerifier.verifyStatusAcceptable(aaid, metadataEntryNode);
			if (!metadataEntryNode.has("metadataStatement")) {
				log.warn("This entry doesn't contain metadataStatement");
			}
			entries.put(aaid, metadataEntryNode);
			log.info(ADDED_TOC_ENTRY_MSG, aaid);
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
				log.info(ADDED_TOC_ENTRY_MSG, keyIdentifier);
			}
		} catch (IOException e) {
			log.error("Failed to add attestationCertificateKeyIdentifiers to tocEntries: {}",
					attestationCertificateKeyIdentifiers);
		}
	}
}