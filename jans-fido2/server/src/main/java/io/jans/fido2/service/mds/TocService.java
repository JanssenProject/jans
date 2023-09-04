/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.mds;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;
import static java.time.format.DateTimeFormatter.ISO_DATE;
import java.net.URL;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.jans.fido2.exception.mds.MdsClientException;
import io.jans.fido2.model.mds.MdsGetEndpointResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.util.Pair;
import io.jans.util.StringHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;

/**
 * TOC is parsed and Hashmap containing JSON object of individual Authenticators is created.
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
    private FetchMdsProviderService fetchMdsProviderService;

    private Map<String, JsonNode> tocEntries;
    
    private LocalDate nextUpdate;
    private MessageDigest digester;

    public LocalDate getNextUpdateDate()
    {
    	return nextUpdate;
    }

    public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
        refresh();
		loadMetadataServiceExternalProvider();
    }

    public void refresh() {
		this.tocEntries = Collections.synchronizedMap(new HashMap<String, JsonNode>());
		if (appConfiguration.getFido2Configuration().isSkipDownloadMdsEnabled()) {
			log.debug("SkipDownloadMds is enabled");
		}  else {
			tocEntries.putAll(parseTOCs());
		}
	}
    
	private Map<String, JsonNode> parseTOCs() {
		Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
		if (fido2Configuration == null) {
			log.warn("Fido2 configuration not exists");
			return new HashMap<String, JsonNode>();
		}

		String mdsTocRootCertsFolder = fido2Configuration.getMdsCertsFolder();
		String mdsTocFilesFolder = fido2Configuration.getMdsTocsFolder();
		if (StringHelper.isEmpty(mdsTocRootCertsFolder) || StringHelper.isEmpty(mdsTocFilesFolder)) {
			log.warn("Fido2 MDS cert and TOC properties should be set");
			return new HashMap<String, JsonNode>();
		}
		log.info("Populating TOC entries from {}", mdsTocFilesFolder);

		Path path = FileSystems.getDefault().getPath(mdsTocFilesFolder);
		List<Map<String, JsonNode>> maps = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
			Iterator<Path> iter = directoryStream.iterator();
			while (iter.hasNext()) {
				Path filePath = iter.next();
				try {
					Pair<LocalDate, Map<String, JsonNode>> result = parseTOC(mdsTocRootCertsFolder, filePath);
					log.info("Get TOC {} entries with nextUpdate date {}", result.getSecond().size(),
							result.getFirst());

					maps.add(result.getSecond());
				} catch (IOException e) {
					log.warn("Can't access or open path: {}", filePath, e);
				} catch (ParseException e) {
					log.warn("Can't parse path: {}", filePath, e);
				}
			}
		} catch (Exception e) {
			log.warn("Something wrong with path", e);
		}

		return mergeAndResolveDuplicateEntries(maps);
	}

    private Map<String, JsonNode> parseTOC(String mdsTocRootCertFile, String mdsTocFileLocation) {
        try {
            return parseTOC(mdsTocRootCertFile, FileSystems.getDefault().getPath(mdsTocFileLocation)).getSecond();
        } catch (IOException e) {
            throw new Fido2RuntimeException("Unable to read TOC at " + mdsTocFileLocation, e);
        } catch (ParseException e) {
            throw new Fido2RuntimeException("Unable to parse TOC at " + mdsTocFileLocation, e);
        }
    }

	private Pair<LocalDate, Map<String, JsonNode>> parseTOC(String mdsTocRootCertsFolder, Path path)
			throws IOException, ParseException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			String tocJwt = reader.readLine();
			return readEntriesFromTocJWT(tocJwt, mdsTocRootCertsFolder, true);
		}
	}

    private JWSVerifier resolveVerifier(JWSAlgorithm algorithm, String mdsTocRootCertsFolder, List<String> certificateChain) {
        List<X509Certificate> x509CertificateChain = certificateService.getCertificates(certificateChain);
        List<X509Certificate> x509TrustedCertificates = certificateService.getCertificates(mdsTocRootCertsFolder);

        X509Certificate verifiedCert = certificateVerifier.verifyAttestationCertificates(x509CertificateChain, x509TrustedCertificates);
        //possible set of algos are : ES256, RS256, PS256, ED256
        // no support for ED256 in JOSE library
        
        if (JWSAlgorithm.ES256.equals(algorithm)) {
        	log.debug("resolveVerifier : ES256");
            try {
                return new ECDSAVerifier((ECPublicKey) verifiedCert.getPublicKey());
            } catch (JOSEException e) {
                throw new Fido2RuntimeException("Unable to create verifier for algorithm " + algorithm, e);
            }
        }
        else if (JWSAlgorithm.RS256.equals(algorithm) || JWSAlgorithm.PS256.equals(algorithm)) {
        	log.debug("resolveVerifier : RS256");
                return new RSASSAVerifier((RSAPublicKey) verifiedCert.getPublicKey());
           
        }
        else { 
            throw new Fido2RuntimeException("Don't know what to do with " + algorithm);
        }
    }

    private MessageDigest resolveDigester(JWSAlgorithm algorithm) {
    	// fix: algorithm RS256 added for https://github.com/GluuFederation/fido2/issues/16
        if (JWSAlgorithm.ES256.equals(algorithm) || JWSAlgorithm.RS256.equals(algorithm) ) {
            return DigestUtils.getSha256Digest();
        }
       else {
            throw new Fido2RuntimeException("Don't know what to do with " + algorithm);
        }
    }

    private Map<String, JsonNode> mergeAndResolveDuplicateEntries(List<Map<String, JsonNode>> maps) {
        Map<String, JsonNode> allEntries = new HashMap<>();
        Map<String, JsonNode> a[] = new Map[maps.size()];
        maps.toArray(a);

        allEntries.putAll(
                Stream.of(a).flatMap(m -> m.entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> {
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

		Path path = FileSystems.getDefault().getPath(mdsTocFilesFolder);
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
			Iterator<Path> iter = directoryStream.iterator();
			while (iter.hasNext()) {
				Path filePath = iter.next();
				try (InputStream in = metadataUrl.openStream()) {

					Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);

					log.info("TOC file updated.");
					return true;
				}
			}
		} catch (IOException e) {
			log.warn("Can't access or open path: {}", path, e);
		}
		return false;
	}

	private void loadMetadataServiceExternalProvider() {
		String metadataUrlsProvider = appConfiguration.getFido2Configuration().getMetadataUrlsProvider();
		if (metadataUrlsProvider != null && !metadataUrlsProvider.trim().isEmpty()) {
			log.debug("MetadataUrlsProvider found: {}", metadataUrlsProvider);
			try {
				MdsGetEndpointResponse mdsGetEndpointResponse = fetchMdsProviderService.fetchMdsV3Endpoints(metadataUrlsProvider);
				Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
				String mdsTocRootCertsFolder = fido2Configuration.getMdsCertsFolder();
				List<Map<String, JsonNode>> entryList = new ArrayList<>();
				for (String mdsUrl : mdsGetEndpointResponse.getResult()) {
					String blobJwt = fetchMdsProviderService.fetchMetadataBlob(mdsUrl);
					if (blobJwt == null) {
						continue;
					}
					try {
						Pair<LocalDate,Map<String, JsonNode>> dateMapPair = readEntriesFromTocJWT(blobJwt, mdsTocRootCertsFolder, false);
						entryList.add(dateMapPair.getSecond());
					} catch (Fido2RuntimeException e) {
						log.error(e.getMessage());
					}
				}
				this.tocEntries.putAll(mergeAndResolveDuplicateEntries(entryList));
				log.info("🔐 MedataUrlsProvider successfully loaded");

			} catch (MdsClientException e) {
				log.error(e.getMessage());
			}
		} else {
			log.debug("MetadataUrlsProvider not found");
		}
	}

	private Pair<LocalDate, Map<String, JsonNode>> readEntriesFromTocJWT(String tocJwt, String mdsTocRootCertsFolder, boolean loadGlobalVariables) {
		log.debug("Attempting reading entries from JWT: {}", StringUtils.abbreviateMiddle(tocJwt, "...", 100));
		JWSObject blobDecoded;
		try {
			blobDecoded = JWSObject.parse(tocJwt);
		} catch (ParseException e) {
			throw new Fido2RuntimeException("Error when parsing TOC JWT: " + e.getMessage(), e);
		}
		JWSAlgorithm algorithm = blobDecoded.getHeader().getAlgorithm();
		List<String> headerCertificatesX5c = blobDecoded.getHeader().getX509CertChain().stream()
				.map(c -> base64Service.encodeToString(c.decode()))
				.collect(Collectors.toList());
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
			throw new Fido2RuntimeException("Unable to verify JWS object using algorithm: " + algorithm + ", message: " + e.getMessage(), e);
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
				String attestationCertificateKeyIdentifiers = metadataEntryNode.get("attestationCertificateKeyIdentifiers").asText();
				log.debug("TODO: handle attestationCertificateKeyIdentifiers addition to tocEntries {}", attestationCertificateKeyIdentifiers);
			} else {
				log.debug("Null aaguid, aaid, attestationCertificateKeyIdentifiers - Added TOC entry with status {}", metadataEntryNode.get("statusReports").findValue("status"));
			}
		}

		LocalDate nextUpdateDate = LocalDate.parse(toc.get("nextUpdate").asText());
		return new Pair<>(nextUpdateDate, entries);
	}
}