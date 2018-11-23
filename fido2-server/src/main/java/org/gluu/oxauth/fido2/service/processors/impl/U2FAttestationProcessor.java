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

package org.gluu.oxauth.fido2.service.processors.impl;

import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.cryptoutils.CoseService;
import org.gluu.oxauth.fido2.cryptoutils.CryptoUtils;
import org.gluu.oxauth.fido2.ctap.AttestationFormat;
import org.gluu.oxauth.fido2.model.auth.AuthData;
import org.gluu.oxauth.fido2.model.auth.CredAndCounterData;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationData;
import org.gluu.oxauth.fido2.service.Base64Service;
import org.gluu.oxauth.fido2.service.CertificateSelector;
import org.gluu.oxauth.fido2.service.CertificateValidator;
import org.gluu.oxauth.fido2.service.CommonVerifiers;
import org.gluu.oxauth.fido2.service.processors.AttestationFormatProcessor;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class U2FAttestationProcessor implements AttestationFormatProcessor {

    @Inject
    private Logger log;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private AttestationProcessorFactory attestationProcessorFactory;

    @Inject
    private CertificateSelector certificateSelector;

    @Inject
    private CertificateValidator certificateValidator;

    @Inject
    private CoseService coseService;

    @Inject
    private Base64Service base64Service;

    @Inject
    private CryptoUtils cryptoUtils;

    @Override
    public AttestationFormat getAttestationFormat() {
        return AttestationFormat.fido_u2f;
    }

    @Override
    public void process(JsonNode attStmt, AuthData authData, Fido2RegistrationData registration, byte[] clientDataHash,
            CredAndCounterData credIdAndCounters) {
        int alg = -7;

        String signature = commonVerifiers.verifyBase64String(attStmt.get("sig"));
        commonVerifiers.verifyAAGUIDZeroed(authData);
        commonVerifiers.verifyUserPresent(authData);
        commonVerifiers.verifyRpIdHash(authData, registration.getDomain());

        if (attStmt.hasNonNull("x5c")) {
            Iterator<JsonNode> i = attStmt.get("x5c").elements();
            ArrayList<String> certificatePath = new ArrayList();
            while (i.hasNext()) {
                certificatePath.add(i.next().asText());
            }
            List<X509Certificate> certificates = cryptoUtils.getCertificates(certificatePath);

            // certificateValidator.saveCertificate(certificate);

            credIdAndCounters.setSignatureAlgorithm(alg);
            List<X509Certificate> trustAnchorCertificates = certificateSelector.selectRootCertificate(certificates.get(0));
            Certificate verifiedCert = certificateValidator.verifyAttestationCertificates(certificates, trustAnchorCertificates);
            commonVerifiers.verifyU2FAttestationSignature(authData, clientDataHash, signature, verifiedCert, alg);

        } else if (attStmt.hasNonNull("ecdaaKeyId")) {
            String ecdaaKeyId = attStmt.get("ecdaaKeyId").asText();
            throw new UnsupportedOperationException("TODO");
        } else {
            PublicKey publicKey = coseService.getPublicKeyFromUncompressedECPoint(authData.getCOSEPublicKey());
            commonVerifiers.verifyPackedSurrogateAttestationSignature(authData.getAuthDataDecoded(), clientDataHash, signature, publicKey, alg);
        }
        credIdAndCounters.setAttestationType(getAttestationFormat().getFmt());
        credIdAndCounters.setCredId(base64Service.urlEncodeToString(authData.getCredId()));
        credIdAndCounters.setUncompressedEcPoint(base64Service.urlEncodeToString(authData.getCOSEPublicKey()));
    }
}
