/*
 * Copyright (c) 2018 Mastercard
 * Copyright (c) 2018 Gluu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.gluu.oxauth.fido2.mds;

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
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.digest.DigestUtils;
import org.gluu.oxauth.fido2.cryptoutils.CryptoUtils;
import org.gluu.oxauth.fido2.service.CertificateValidator;
import org.gluu.oxauth.fido2.service.Fido2RPRuntimeException;
import org.slf4j.Logger;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.util.StringHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.api.client.util.Value;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;

@Named
@ApplicationScoped
public class MDSTOCHandler {

    @Inject
    private Logger log;

    @Inject
    private ObjectMapper om;

    @Inject
    @Named("tocEntries")
    private Map<String, JsonNode> tocEntries;

    @Inject
    private TOCEntryDigester tocEntryDigester;

    @Inject
    private CertificateValidator certificateValidator;

    @Inject
    private CryptoUtils cryptoUtils;

    @Inject
    @Named("base64Encoder")
    private Base64.Encoder base64Encoder;

    @Inject
    private AppConfiguration appConfiguration;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) throws Exception {
        tocEntries.putAll(parseTOCs());
    }

    private Map<String, JsonNode> parseTOCs() {
        String mdsTocRootFileLocation = appConfiguration.getFido2Configuration().getMdsTocRootFileLocation();
        String mdsTocFilesFolder = appConfiguration.getFido2Configuration().getMdsTocFilesFolder();
//        String mdsTocFileLocation = appConfiguration.getFido2Configuration().getMdsTocFileLocation();
        if (StringHelper.isEmpty(mdsTocRootFileLocation) || StringHelper.isEmpty(mdsTocFilesFolder)) {
            log.warn("Fido2 MDS properties should be set");
            return new HashMap<String, JsonNode>();
        }

        log.info("Populating TOC entries from {}", mdsTocFilesFolder);

        Path path = FileSystems.getDefault().getPath(mdsTocFilesFolder);
        DirectoryStream<Path> directoryStream = null;
        List<Map<String, JsonNode>> maps = new ArrayList<>();
        try {
            directoryStream = Files.newDirectoryStream(path);
            Iterator<Path> iter = directoryStream.iterator();
            while (iter.hasNext()) {
                Path filePath = iter.next();
                try {
                    maps.add(parseTOC(mdsTocRootFileLocation, filePath));
                } catch (IOException e) {
                    log.warn("Can't access or open path " + path);
                } catch (ParseException e) {
                    log.warn("Can't parse path " + path);
                }
            }
        } catch (IOException e) {
            log.warn("Something wrong with path ", e);
        } finally {
            if (directoryStream != null) {
                try {
                    directoryStream.close();
                } catch (IOException e) {
                    log.warn("Something wrong with directory stream", e);
                }
            }
        }
        return mergeAndResolveDuplicateEntries(maps);

    }

    private Map<String, JsonNode> parseTOC(String mdsTocRootFileLocation, String mdsTocFileLocation) {
        try {
            return parseTOC(mdsTocRootFileLocation, FileSystems.getDefault().getPath(mdsTocFileLocation));
        } catch (IOException e) {
            throw new Fido2RPRuntimeException("Unable to read TOC at " + mdsTocFileLocation);
        } catch (ParseException e) {
            throw new Fido2RPRuntimeException("Unable to parse TOC at " + mdsTocFileLocation);
        }
    }

    private Map<String, JsonNode> parseTOC(String mdsTocRootFileLocation, Path path) throws IOException, ParseException {
        BufferedReader reader = null;
        try {
            reader = Files.newBufferedReader(path);
            JWSObject jwsObject = JWSObject.parse(reader.readLine());

            List<String> certificateChain = jwsObject.getHeader().getX509CertChain().stream().map(c -> base64Encoder.encodeToString(c.decode()))
                    .collect(Collectors.toList());
            JWSAlgorithm algorithm = jwsObject.getHeader().getAlgorithm();

            try {
                JWSVerifier verifier = resolveVerifier(algorithm, mdsTocRootFileLocation, certificateChain);
                if (!jwsObject.verify(verifier)) {
                    log.warn("Unable to verify JWS object using algorithm {} for file {}", algorithm, path);
                    return Collections.emptyMap();
                }
            } catch (JOSEException e) {
                log.warn("Unable to verify JWS object using algorithm {} for file {} {} ", algorithm, path, e.getMessage());
                return Collections.emptyMap();
            } catch (Fido2RPRuntimeException ex) {
                log.warn("Unable to verify JWS object using algorithm {} for file {} {}", algorithm, path, ex.getMessage());
                return Collections.emptyMap();
            }
            tocEntryDigester.setDigester(resolveDigester(algorithm));
            String jwtPayload = jwsObject.getPayload().toString();
            JsonNode toc = om.readTree(jwtPayload);
            log.info("Legal header {}", toc.get("legalHeader"));
            ArrayNode entries = (ArrayNode) toc.get("entries");
            int numberOfEntries = toc.get("no").asInt();
            log.info("Number of entries {} {}", numberOfEntries, entries.size());
            Iterator<JsonNode> iter = entries.elements();
            Map<String, JsonNode> tocEntries = new HashMap<>();
            while (iter.hasNext()) {
                JsonNode tocEntry = iter.next();
                log.info("{} {}", path, tocEntry.get("aaguid").asText());
                tocEntries.put(tocEntry.get("aaguid").asText(), tocEntry);
            }
            return tocEntries;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("Unable to close reader {}", path);
                }
            }
        }

    }

    private JWSVerifier resolveVerifier(JWSAlgorithm algorithm, String mdsTocRootFileLocation, List<String> certificateChain) {
        Path path = FileSystems.getDefault().getPath(mdsTocRootFileLocation);

        List<X509Certificate> x509CertificateChain = cryptoUtils.getCertificates(certificateChain);
        List<X509Certificate> x509TrustedCertificates = new ArrayList<>();
        try {
            x509TrustedCertificates.add(cryptoUtils.getCertificate(Files.newInputStream(path)));
        } catch (IOException e) {
            throw new Fido2RPRuntimeException("Unable to read the root cert " + path);
        }
        X509Certificate verifiedCert = certificateValidator.verifyAttestationCertificates(x509CertificateChain, x509TrustedCertificates);

        if (JWSAlgorithm.ES256.equals(algorithm)) {
            JWSVerifier verifier;
            try {
                verifier = new ECDSAVerifier((ECPublicKey) verifiedCert.getPublicKey());
                return verifier;
            } catch (JOSEException e) {
                throw new Fido2RPRuntimeException("Unable to create verifier for algorithm " + algorithm);
            }
        } else {
            throw new Fido2RPRuntimeException("Don't know what to do with " + algorithm);
        }
    }

    private MessageDigest resolveDigester(JWSAlgorithm algorithm) {
        if (JWSAlgorithm.ES256.equals(algorithm)) {
            return DigestUtils.getSha256Digest();
        } else {
            throw new Fido2RPRuntimeException("Don't know what to do with " + algorithm);
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
                    log.warn("Selected value {} ", result);
                    return result;
                })));
        return allEntries;
    }

    private LocalDate getDate(JsonNode n) {
        JsonNode dateNode = n.get("timeOfLastStatusChange");
        LocalDate date;
        if (dateNode != null) {
            date = LocalDate.parse(dateNode.asText(), ISO_DATE);
        } else {
            date = LocalDate.now();
        }
        return date;
    }

}
