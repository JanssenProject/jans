/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.mds;

import static java.time.format.DateTimeFormatter.ISO_DATE;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

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
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;

/**
 * @author Yuriy Movchan
 * @version May 08, 2020
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

    private Map<String, JsonNode> tocEntries;
    private MessageDigest digester;

    public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
        this.tocEntries = Collections.synchronizedMap(new HashMap<String, JsonNode>());
        tocEntries.putAll(parseTOCs());
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
                    log.info("Get TOC {} entries with nextUpdate date {}", result.getSecond().size(), result.getFirst());
                    
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

    private Pair<LocalDate, Map<String, JsonNode>> parseTOC(String mdsTocRootCertsFolder, Path path) throws IOException, ParseException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JWSObject jwsObject = JWSObject.parse(reader.readLine());

            List<String> certificateChain = jwsObject.getHeader().getX509CertChain().stream().map(c -> base64Service.encodeToString(c.decode()))
                    .collect(Collectors.toList());
            JWSAlgorithm algorithm = jwsObject.getHeader().getAlgorithm();

            try {
                JWSVerifier verifier = resolveVerifier(algorithm, mdsTocRootCertsFolder, certificateChain);
                if (!jwsObject.verify(verifier)) {
                    log.warn("Unable to verify JWS object using algorithm {} for file {}", algorithm, path);
                    return new Pair<LocalDate, Map<String,JsonNode>>(null, Collections.emptyMap());
                }
            } catch (Exception e) {
                log.warn("Unable to verify JWS object using algorithm {} for file {} {}", algorithm, path, e);
                return new Pair<LocalDate, Map<String,JsonNode>>(null, Collections.emptyMap());
            }

            String jwtPayload = jwsObject.getPayload().toString();
            JsonNode toc = dataMapperService.readTree(jwtPayload);
            log.debug("Legal header {}", toc.get("legalHeader"));

            ArrayNode entries = (ArrayNode) toc.get("entries");
            int numberOfEntries = toc.get("no").asInt();
            log.debug("Property 'no' value: {}. Number of entries: {}", numberOfEntries, entries.size());

            Iterator<JsonNode> iter = entries.elements();
            Map<String, JsonNode> tocEntries = new HashMap<>();
            while (iter.hasNext()) {
                JsonNode tocEntry = iter.next();
                if (tocEntry.hasNonNull("aaguid")) {
                    String aaguid = tocEntry.get("aaguid").asText();
                    log.info("Added TOC entry {} from {} with status {}", aaguid, path, tocEntry.get("statusReports").findValue("status"));
                    tocEntries.put(aaguid, tocEntry);
                }
            }
            
            String nextUpdateText = toc.get("nextUpdate").asText();

            LocalDate nextUpdateDate = LocalDate.parse(nextUpdateText);

            this.digester = resolveDigester(algorithm);
            
            return new Pair<LocalDate, Map<String,JsonNode>>(nextUpdateDate, tocEntries);
        }
    }

    private JWSVerifier resolveVerifier(JWSAlgorithm algorithm, String mdsTocRootCertsFolder, List<String> certificateChain) {
        List<X509Certificate> x509CertificateChain = certificateService.getCertificates(certificateChain);
        List<X509Certificate> x509TrustedCertificates = certificateService.getCertificates(mdsTocRootCertsFolder);

        X509Certificate verifiedCert = certificateVerifier.verifyAttestationCertificates(x509CertificateChain, x509TrustedCertificates);

        if (JWSAlgorithm.ES256.equals(algorithm)) {
            try {
                return new ECDSAVerifier((ECPublicKey) verifiedCert.getPublicKey());
            } catch (JOSEException e) {
                throw new Fido2RuntimeException("Unable to create verifier for algorithm " + algorithm, e);
            }
        } else {
            throw new Fido2RuntimeException("Don't know what to do with " + algorithm);
        }
    }

    private MessageDigest resolveDigester(JWSAlgorithm algorithm) {
        if (JWSAlgorithm.ES256.equals(algorithm)) {
            return DigestUtils.getSha256Digest();
        } else {
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

}
