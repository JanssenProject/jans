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

package org.gluu.oxauth.fido2.certification;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Hex;
import org.gluu.oxauth.fido2.cryptoutils.CryptoUtils;
import org.gluu.oxauth.fido2.mds.MDSService;
import org.gluu.oxauth.fido2.service.AuthData;
import org.gluu.oxauth.fido2.service.CommonVerifiers;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Named
public class CertificationKeyStoreUtils {

    @Inject
    private Logger log;

    @Inject
    KeyStoreCreator keyStoreCreator;

    @Inject
    @Named("base64Decoder")
    private Base64.Decoder base64Decoder;

    @Inject
    CryptoUtils cryptoUtils;

    @Inject
    CommonVerifiers commonVerifiers;

    @Inject
    ObjectMapper mapper;

    @Inject
    MDSService mdsService;

    @Inject
    @Named("authenticatorsMetadata")
    Map<String, JsonNode> authenticatorsMetadata;

    List<X509Certificate> getCertificates(JsonNode metadataNode) {

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
        Map<String, JsonNode> aaguidMapOfMetadata = authenticatorsMetadata;

        JsonNode metadataForAuthenticator = aaguidMapOfMetadata.get(aaguid);
        if (metadataForAuthenticator == null) {
            log.info("No metadata for authenticator {}. Attempting to contact MDS", aaguid);
            JsonNode metadata = mdsService.fetchMetadata(authData.getAaguid());
            commonVerifiers.verifyThatMetadataIsValid(metadata);
            authenticatorsMetadata.put(aaguid, metadata);
            metadataForAuthenticator = metadata;
        }
        return getCertificates(metadataForAuthenticator);
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
            System.exit(1);
            return null;
        }
    }
}
