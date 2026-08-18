/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.mds;

import static java.time.format.DateTimeFormatter.ISO_DATE;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import io.jans.fido2.exception.mds.MdsRateLimitedException;
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

	/**
	 * The FIDO Alliance metadata endpoint is fronted by a CDN that throttles the JDK's default
	 * {@code Java/<version>} User-Agent, which surfaces as an opaque HTTP 429. Identify the server
	 * properly instead of relying on that default.
	 */
	private static final String MDS_USER_AGENT = "Janssen-FIDO2";

	private static final String RETRY_AFTER_HEADER = "Retry-After";

	/** {@link HttpURLConnection} has no constant for 429 (Too Many Requests). */
	private static final int HTTP_TOO_MANY_REQUESTS = 429;

	private static final int MDS_CONNECT_TIMEOUT_MS = 10_000;

	private static final int MDS_READ_TIMEOUT_MS = 60_000;

	// Written by the asynchronous startup observer and the MDS3 update timer, read by request-handling
	// threads. Each refresh swaps in a new map rather than mutating the live one, so readers never
	// observe a partially populated TOC. Held in an AtomicReference rather than a volatile field: the
	// referent is a mutable Map, and volatile only publishes the reference safely, not the object.
	private final AtomicReference<Map<String, JsonNode>> tocEntries = new AtomicReference<>();

	// nextUpdate is written by the startup/timer refresh and read when persisting it; volatile keeps it
	// visible should a later refresh run on a different pool thread. LocalDate is immutable, so this is
	// safe to publish by reference.
	private volatile LocalDate nextUpdate;

	private MessageDigest digester;

	// Outcome of the most recent refresh, retained so it can be surfaced as MDS health. Until now a
	// failed download or parse was only written to the log and then discarded, which is why a stale or
	// broken MDS load is invisible to an administrator. Written by the startup observer and the MDS3
	// update timer, read by request threads, so both are volatile; OffsetDateTime and String are
	// immutable and therefore safe to publish by reference.
	private volatile OffsetDateTime lastSuccessfulRefresh;

	private volatile String lastRefreshError;

	/**
	 * Outcome of a single TOC download attempt, used to decide whether another attempt is worthwhile.
	 * Package-private so the retry control flow can be unit tested.
	 */
	enum TocDownloadOutcome {
		/** A freshly downloaded TOC was parsed and published. */
		PUBLISHED,
		/** No download was attempted because the cached TOC is still inside its validity window. */
		SKIPPED,
		/** The endpoint answered HTTP 429; it explicitly asked us to back off, so do not retry now. */
		RATE_LIMITED,
		/** The download failed for a reason that may well be transient. */
		FAILED
	}

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
		refreshTOCEntries(false);
	}

	private void refreshTOCEntries(boolean rejectExpired) {
		// Build the replacement map locally. Parsing a TOC involves certificate loading and JWS
		// verification, so assigning an empty map up front would leave concurrent readers seeing no
		// metadata for the whole parse — long enough for enforced attestation to reject valid
		// authenticators. Publish it in a single assignment once it is fully populated instead.
		Map<String, JsonNode> entries = Collections.synchronizedMap(new HashMap<String, JsonNode>());
		if (appConfiguration.getFido2Configuration().isDisableMetadataService()) {
			log.debug("SkipDownloadMds is enabled");
		} else {
			entries.putAll(parseTOCs(rejectExpired));
		}
		this.tocEntries.set(entries);
	}

	public void fetchMetadata() {
		fetchMetadata(false);
	}

	/**
	 * Fetches the MDS TOC blob, falling back to the copy cached in {@code jansDocument} when no fresh
	 * one could be published.
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

		// mdsDownloadStartupRetries counts *retries*, so the total number of downloads is one initial
		// attempt plus that many. A negative/zero setting degrades to the plain single-attempt behaviour.
		int maxAttempts = 1;
		if (retryWhenTocMissing && isTocContentMissing()) {
			maxAttempts = 1 + Math.max(0, appConfiguration.getFido2Configuration().getMdsDownloadStartupRetries());
			log.info("MDS TOC blob is missing at startup, will attempt the download up to {} time(s)", maxAttempts);
		}

		boolean publishedFreshToc = false;
		boolean retryWorthwhile = true;
		for (int attempt = 1; attempt <= maxAttempts && retryWorthwhile; attempt++) {
			TocDownloadOutcome outcome = fetchMetadataOnce();
			publishedFreshToc = outcome == TocDownloadOutcome.PUBLISHED;

			// Only a plain failure is worth another attempt: RATE_LIMITED means the endpoint explicitly
			// asked us to back off, and SKIPPED means the cached TOC is still current. Stop early too
			// once the blob is present, since the retry loop exists solely for the missing-TOC case.
			retryWorthwhile = !publishedFreshToc && outcome == TocDownloadOutcome.FAILED
					&& attempt < maxAttempts && isTocContentMissing();

			if (retryWorthwhile) {
				log.warn("Attempt {}/{} to download the MDS TOC failed", attempt, maxAttempts);
				sleepBeforeRetry();
			}
		}

		if (!publishedFreshToc) {
			publishCachedToc();
		}
	}

	/**
	 * Performs a single TOC download attempt, skipping it when the cached blob is still current.
	 * <p>
	 * package-private for testability (spied in TocServiceTest).
	 */
	TocDownloadOutcome fetchMetadataOnce() {
		LocalDate nextUpdateOn;
		try {
			nextUpdateOn = getNextUpdateDate();
		} catch (RuntimeException e) {
			// A missing or unreadable TOC document must not abort startup; treat it as due for download.
			log.warn("Unable to read the cached MDS TOC update date, treating the TOC as due: {}", e.getMessage());
			nextUpdateOn = null;
		}

		if (nextUpdateOn != null && nextUpdateOn.isAfter(LocalDate.now())) {
			log.info("The cached MDS TOC is current until {}, skipping the download", nextUpdateOn);
			return TocDownloadOutcome.SKIPPED;
		}

		return downloadAndPublishToc();
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

	/**
	 * Downloads the TOC and publishes its entries.
	 * <p>
	 * A failure is logged rather than propagated: it must not abort the {@link ApplicationInitialized}
	 * observer, and the caller falls back to the cached TOC.
	 *
	 * @return {@link TocDownloadOutcome#PUBLISHED} when a freshly downloaded TOC was published,
	 *         {@link TocDownloadOutcome#RATE_LIMITED} when the endpoint asked us to back off (which
	 *         must not be retried immediately), or {@link TocDownloadOutcome#FAILED} otherwise
	 */
	private TocDownloadOutcome downloadAndPublishToc() {
		// A new attempt supersedes the outcome of the previous one, so the error is cleared here, at the
		// attempt boundary — nowhere else. That lets whichever layer fails record its own reason and
		// lets the success stamp below be conditional on nothing having failed.
		this.lastRefreshError = null;
		try {
			MetadataServer metaDataServer = appConfiguration.getFido2Configuration().getMetadataServers().get(0);

			// as of now, we have only one metadata server, hence get(0), I cant envisage
			// why there will be multiple metadata servers
			log.info("Downloading the latest TOC from {}", metaDataServer.getUrl());
			boolean success = downloadMdsFromServer(new URL(metaDataServer.getUrl()));
			if (success) {
				refreshTOCEntries();
				// parseTOCs() swallows its own failures and returns an empty map, so the absence of a
				// recorded error is what distinguishes a genuine refresh from a download that produced
				// nothing usable. Only a genuine refresh may stamp nextUpdate: writing it after a failed
				// parse would persist the *previous* blob's date and make later refreshes skip the
				// download until that date passed.
				if (this.lastRefreshError != null) {
					log.error("The downloaded MDS TOC could not be parsed; not recording it as published");
					return TocDownloadOutcome.FAILED;
				}
				saveNextUpdateDateOfTheMDS();
				this.lastSuccessfulRefresh = OffsetDateTime.now(ZoneOffset.UTC);
				return TocDownloadOutcome.PUBLISHED;
			}
		} catch (IndexOutOfBoundsException e) {
			log.error("No FIDO2 metadata server is configured", e);
			recordRefreshError("No FIDO2 metadata server is configured");
		} catch (MalformedURLException e) {
			log.error("Error while parsing the FIDO alliance URL :", e);
			recordRefreshError("Malformed metadata server URL: " + e.getMessage());
		} catch (MdsRateLimitedException e) {
			log.warn("MDS TOC download was rate-limited{}, falling back to the cached TOC",
					e.getRetryAfterSeconds() == null ? "" : " (retry after " + e.getRetryAfterSeconds() + "s)");
			recordRefreshError("MDS TOC download was rate-limited (HTTP 429)"
					+ (e.getRetryAfterSeconds() == null ? "" : ", retry after " + e.getRetryAfterSeconds() + "s"));
			return TocDownloadOutcome.RATE_LIMITED;
		} catch (RuntimeException e) {
			log.error("MDS TOC download failed, falling back to the cached TOC: {}", e.getMessage(), e);
			recordRefreshError("MDS TOC download failed: " + e.getMessage());
		}
		return TocDownloadOutcome.FAILED;
	}

	/**
	 * Records why this refresh attempt failed, keeping the <em>first</em> failure of the attempt.
	 * <p>
	 * A failed download is followed by the cached-blob fallback, which parses the very blob the failed
	 * refresh was meant to replace and so tends to fail too. Letting that later failure win would
	 * report "can't parse the TOC document" when the real problem was that the endpoint was
	 * unreachable. The first failure is the root cause; the ones after it are consequences.
	 */
	private void recordRefreshError(String reason) {
		if (this.lastRefreshError == null) {
			this.lastRefreshError = reason;
		}
	}

	/**
	 * Publishes the TOC blob already stored in {@code jansDocument}, so that a failed or skipped
	 * download doesn't leave the server with no authenticator metadata at all.
	 * <p>
	 * The cached blob is used only while it is still inside its own {@code nextUpdate} validity window.
	 * An expired blob is rejected and the entry map is left empty, so enforced attestation keeps
	 * failing closed rather than validating against metadata the FIDO Alliance considers out of date.
	 */
	private void publishCachedToc() {
		refreshTOCEntries(true);

		Map<String, JsonNode> entries = this.tocEntries.get();
		if (entries == null || entries.isEmpty()) {
			log.warn("No usable MDS TOC is available; authenticator metadata can't be validated "
					+ "until the next successful download");
		} else {
			log.info("Published {} MDS TOC entries from the cached blob", entries.size());
		}
	}

	private Map<String, JsonNode> parseTOCs(boolean rejectExpired) {
		Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
		List<Map<String, JsonNode>> maps = new ArrayList<>();
		
		String mdsTocRootCertsFolder = fido2Configuration.getMdsCertsFolder();
		if (StringHelper.isEmpty(mdsTocRootCertsFolder)) {
			log.warn("Fido2 MDS cert and TOC properties should be set");
			recordRefreshError("Fido2 MDS cert and TOC properties are not set");
			return new HashMap<>();
		}
		log.info("Populating TOC certs entries from {}", mdsTocRootCertsFolder);

		try {
			Document mdsDocument = dbDocumentService.getDocumentByDisplayName("mdsTocsFolder");
			Pair<LocalDate, Map<String, JsonNode>> result = parseTOC(mdsTocRootCertsFolder, mdsDocument.getDocument());
			log.info("Get TOC {} entries with nextUpdate date {}", result.getSecond().size(), result.getFirst());

			maps.add(acceptParsedToc(result, rejectExpired));
		} catch (Exception e) {
			log.warn("Can't access document : {}", e.getMessage(), e);
			recordRefreshError("Can't read or parse the MDS TOC document: " + e.getMessage());
		}

		return mergeAndResolveDuplicateEntries(maps);
	}

	/**
	 * Decides whether a parsed TOC may be published.
	 * <p>
	 * When {@code rejectExpired} is set — i.e. we are falling back to the blob cached in the DB rather
	 * than using one we just downloaded — a TOC whose own {@code nextUpdate} has passed is discarded.
	 * Leaving the entry map empty keeps enforced attestation failing closed rather than validating
	 * against metadata the FIDO Alliance considers out of date. Package-private for unit testing.
	 *
	 * @return the parsed entries, or an empty map when the TOC is rejected
	 */
	Map<String, JsonNode> acceptParsedToc(Pair<LocalDate, Map<String, JsonNode>> parsedToc, boolean rejectExpired) {
		LocalDate tocNextUpdate = parsedToc.getFirst();
		if (rejectExpired && tocNextUpdate != null && tocNextUpdate.isBefore(LocalDate.now())) {
			log.warn("The cached MDS TOC expired on {}, refusing to use it", tocNextUpdate);
			return new HashMap<>();
		}
		return parsedToc.getSecond();
	}

	private Pair<LocalDate, Map<String, JsonNode>> parseTOC(String mdsTocRootCertsFolder, String content) {
		String decodedString = new String(base64Service.decode(content));
		return readEntriesFromTocJWT(decodedString, mdsTocRootCertsFolder, true);
	}

	/**
	 * CONF-21: add any configured per-endpoint {@code MetadataServer.rootCert} values to the set of
	 * trust anchors used to verify the MDS TOC JWS. The {@code rootCert} is a base64-encoded DER X.509
	 * certificate. This is additive — an empty/unset rootCert leaves the existing folder-based trust
	 * unchanged, and a malformed value is logged and skipped rather than breaking TOC verification.
	 * Package-private for unit testing.
	 */
	void addConfiguredMetadataServerRootCerts(List<X509Certificate> trustedCertificates) {
		List<MetadataServer> metadataServers = appConfiguration.getFido2Configuration().getMetadataServers();
		if (metadataServers == null || metadataServers.isEmpty()) {
			return;
		}
		for (MetadataServer metadataServer : metadataServers) {
			String rootCert = metadataServer.getRootCert();
			if (StringHelper.isEmpty(rootCert)) {
				continue;
			}
			try {
				X509Certificate cert = certificateService.getCertificate(rootCert);
				if (cert != null) {
					trustedCertificates.add(cert);
					log.info("Added per-endpoint MetadataServer rootCert as an additional MDS TOC trust anchor for {}",
							metadataServer.getUrl());
				}
			} catch (RuntimeException e) {
				log.warn("Failed to load configured MetadataServer.rootCert for {}; using mdsCertsFolder trust only: {}",
						metadataServer.getUrl(), e.getMessage());
			}
		}
	}

	private JWSVerifier resolveVerifier(JWSAlgorithm algorithm, String mdsTocRootCertsFolder,
			List<String> certificateChain) {
		List<X509Certificate> x509CertificateChain = certificateService.getCertificates(certificateChain);
		List<X509Certificate> x509TrustedCertificates = new ArrayList<>(
				certificateService.getCertificates(mdsTocRootCertsFolder));
		// CONF-21: honor a per-endpoint MetadataServer.rootCert as an ADDITIONAL TOC trust anchor, so a
		// conformance/test root can be trusted without modifying the shared production mdsCertsFolder.
		// Additive only: deployments that don't set rootCert are unaffected.
		addConfiguredMetadataServerRootCerts(x509TrustedCertificates);
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
		// Read the volatile field once: the startup observer or the MDS3 timer may swap in a new map
		// between the null check and the lookup.
		Map<String, JsonNode> entries = this.tocEntries.get();
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

	/**
	 * Number of authenticator metadata entries currently loaded in memory. Zero means attestation has
	 * no metadata to validate against — a common cause of a previously valid authenticator suddenly
	 * being rejected.
	 */
	public int getTocEntryCount() {
		// Read the volatile field once; a refresh may swap in a new map between the null check and size().
		Map<String, JsonNode> entries = this.tocEntries.get();
		return entries == null ? 0 : entries.size();
	}

	/**
	 * The {@code nextUpdate} declared by the TOC blob currently loaded in memory, or {@code null} when
	 * no blob has been parsed since startup.
	 * <p>
	 * Unlike {@link #getNextUpdateDate()} this reads the in-memory value rather than the document
	 * store, so it neither performs I/O nor throws {@link DocumentException} — which is what makes it
	 * usable from a health endpoint.
	 */
	public LocalDate getLoadedTocNextUpdate() {
		return nextUpdate;
	}

	/**
	 * When metadata was last downloaded and parsed successfully, in UTC, or {@code null} when no
	 * refresh has succeeded since startup.
	 */
	public OffsetDateTime getLastSuccessfulRefresh() {
		return lastSuccessfulRefresh;
	}

	/**
	 * Why the most recent refresh failed, or {@code null} when the last refresh succeeded.
	 */
	public String getLastRefreshError() {
		return lastRefreshError;
	}

	public boolean downloadMdsFromServer(URL metadataUrl) {
		byte[] sourceBytes = readTocBytes(metadataUrl);
		String encodedToc = base64Service.encodeToString(sourceBytes);

		// Verify the blob before it replaces the cached copy. persistTocDocument() overwrites the only
		// stored TOC, so persisting first and discovering the blob is unusable afterwards would destroy
		// metadata the server was successfully validating against — an endpoint that answers HTTP 200
		// with a truncated or wrongly-signed body would leave us with nothing. On rejection the caller
		// falls back to the cached blob, which is still intact.
		verifyDownloadedToc(encodedToc);

		return persistTocDocument(encodedToc);
	}

	/**
	 * Parses and verifies a freshly downloaded TOC, throwing if it cannot be used.
	 * <p>
	 * This does parse the blob a second time — {@link #refreshTOCEntries()} re-reads it from the
	 * document store once it is stored — but a TOC is downloaded once a day, and the cost buys the
	 * guarantee that a bad download can never replace a good cached one. Package-private for testing.
	 *
	 * @throws Fido2RuntimeException when the blob is malformed, or its signature does not verify
	 *         against the configured trust anchors
	 */
	void verifyDownloadedToc(String encodedToc) {
		String mdsTocRootCertsFolder = appConfiguration.getFido2Configuration().getMdsCertsFolder();
		if (StringHelper.isEmpty(mdsTocRootCertsFolder)) {
			throw new Fido2RuntimeException(
					"Fido2 MDS cert and TOC properties are not set, the downloaded TOC can't be verified");
		}
		parseTOC(mdsTocRootCertsFolder, encodedToc);
	}

	/**
	 * Reads the raw TOC blob from the configured metadata endpoint.
	 * <p>
	 * Non-HTTP sources (e.g. a {@code file:} mirror) are read as a plain stream, since they expose no
	 * status code or headers to inspect.
	 */
	private byte[] readTocBytes(URL metadataUrl) {
		try {
			URLConnection urlConnection = metadataUrl.openConnection();
			if (urlConnection instanceof HttpURLConnection) {
				return readTocBytesOverHttp(metadataUrl, (HttpURLConnection) urlConnection);
			}
			try (InputStream in = urlConnection.getInputStream()) {
				return IOUtils.toByteArray(in);
			}
		} catch (IOException e) {
			log.warn("Can't access document {}", metadataUrl, e);
			throw new Fido2RuntimeException("Can't access or open path: " + metadataUrl + e.getMessage(), e);
		}
	}

	/**
	 * Downloads the TOC over HTTP with an identifying User-Agent and bounded timeouts, and translates
	 * the response status into a typed failure.
	 * <p>
	 * HTTP 429 is reported as {@link MdsRateLimitedException} rather than a generic failure, because
	 * the endpoint is explicitly asking us to back off — callers that retry must not treat it as a
	 * transient glitch and immediately try again.
	 */
	private byte[] readTocBytesOverHttp(URL metadataUrl, HttpURLConnection connection) throws IOException {
		connection.setRequestProperty("User-Agent", MDS_USER_AGENT);
		connection.setConnectTimeout(MDS_CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(MDS_READ_TIMEOUT_MS);
		try {
			int responseCode = connection.getResponseCode();
			if (responseCode == HTTP_TOO_MANY_REQUESTS) {
				Integer retryAfterSeconds = parseRetryAfterSeconds(connection.getHeaderField(RETRY_AFTER_HEADER));
				log.warn("MDS TOC download from {} was rate-limited (HTTP 429){}", metadataUrl,
						retryAfterSeconds == null ? "" : ", Retry-After: " + retryAfterSeconds + "s");
				throw new MdsRateLimitedException(
						"MDS TOC download from " + metadataUrl + " was rate-limited (HTTP 429)", retryAfterSeconds);
			}
			if (responseCode != HttpURLConnection.HTTP_OK) {
				log.warn("Unexpected HTTP {} while downloading the MDS TOC from {}", responseCode, metadataUrl);
				throw new Fido2RuntimeException(
						"MDS TOC download from " + metadataUrl + " failed with HTTP " + responseCode);
			}

			try (InputStream in = connection.getInputStream()) {
				return IOUtils.toByteArray(in);
			}
		} finally {
			connection.disconnect();
		}
	}

	/**
	 * Parses a {@code Retry-After} header, which RFC 9110 allows to be either delta-seconds or an
	 * HTTP-date. Package-private for unit testing.
	 *
	 * @return the delay in seconds, or {@code null} when the header is absent or unparseable
	 */
	Integer parseRetryAfterSeconds(String retryAfterHeader) {
		if (StringHelper.isEmpty(retryAfterHeader)) {
			return null;
		}
		String value = retryAfterHeader.trim();
		try {
			return Math.max(0, Integer.parseInt(value));
		} catch (NumberFormatException e) {
			// Not delta-seconds; fall through and try the HTTP-date form.
		}
		try {
			ZonedDateTime retryAt = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
			long seconds = Duration.between(ZonedDateTime.now(retryAt.getZone()), retryAt).getSeconds();
			return (int) Math.max(0, Math.min(seconds, Integer.MAX_VALUE));
		} catch (DateTimeParseException e) {
			log.debug("Unparseable {} header value: {}", RETRY_AFTER_HEADER, value);
			return null;
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