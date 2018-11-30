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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Hex;
import org.gluu.oxauth.fido2.certification.CertificationKeyStoreUtils;
import org.gluu.oxauth.fido2.cryptoutils.CoseService;
import org.gluu.oxauth.fido2.cryptoutils.CryptoUtils;
import org.gluu.oxauth.fido2.ctap.AttestationFormat;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.model.auth.AuthData;
import org.gluu.oxauth.fido2.model.auth.CredAndCounterData;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationData;
import org.gluu.oxauth.fido2.service.Base64Service;
import org.gluu.oxauth.fido2.service.CertificateValidator;
import org.gluu.oxauth.fido2.service.processors.AttestationFormatProcessor;
import org.gluu.oxauth.fido2.service.verifier.CommonVerifiers;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class PackedAttestationProcessor implements AttestationFormatProcessor {

    @Inject
    private Logger log;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private CertificateValidator certificateValidator;

    @Inject
    private CoseService coseService;

    @Inject
    private Base64Service base64Service;

    @Inject
    private CertificationKeyStoreUtils utils;

    @Inject
    private CryptoUtils cryptoUtils;

    @Override
    public AttestationFormat getAttestationFormat() {
        return AttestationFormat.packed;
    }

    @Override
    public void process(JsonNode attStmt, AuthData authData, Fido2RegistrationData registration, byte[] clientDataHash,
            CredAndCounterData credIdAndCounters) {
        int alg = commonVerifiers.verifyAlgorithm(attStmt.get("alg"), authData.getKeyType());
        String signature = commonVerifiers.verifyBase64String(attStmt.get("sig"));

        X509TrustManager tm = utils.populateTrustManager(authData);

        if (attStmt.hasNonNull("x5c")) {
            if (tm.getAcceptedIssuers().length == 0) {
                throw new Fido2RPRuntimeException(
                        "Packed full attestation but no certificates in metadata for authenticator " + Hex.encodeHexString(authData.getAaguid()));
            }

            Iterator<JsonNode> i = attStmt.get("x5c").elements();
            ArrayList<String> certificatePath = new ArrayList();
            while (i.hasNext()) {
                certificatePath.add(i.next().asText());
            }
            List<X509Certificate> certificates = cryptoUtils.getCertificates(certificatePath);
            credIdAndCounters.setSignatureAlgorithm(alg);

            X509Certificate verifiedCert = certificateValidator.verifyAttestationCertificates(certificates, Arrays.asList(tm.getAcceptedIssuers()));

            PublicKey verifiedKey = verifiedCert.getPublicKey();
            commonVerifiers.verifyPackedAttestationSignature(authData.getAuthDataDecoded(), clientDataHash, signature, verifiedCert, alg);
            try {
                verifiedCert.verify(verifiedKey);
                throw new Fido2RPRuntimeException("Self signed certificate ");
            } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {

            }

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
