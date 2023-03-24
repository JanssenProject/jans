/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

/*
 * Copyright (c) 2018 Mastercard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jans.fido2.service.processor.attestation;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Hex;
import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.fido2.service.verifier.AuthenticatorDataVerifier;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Attestation processor for attestations of fmt = packed
 *
 */
@ApplicationScoped
public class PackedAttestationProcessor implements AttestationFormatProcessor {

    @Inject
    private Logger log;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private AuthenticatorDataVerifier authenticatorDataVerifier;

    @Inject
    private CertificateVerifier certificateVerifier;

    @Inject
    private CoseService coseService;

    @Inject
    private Base64Service base64Service;

    @Inject
    private AttestationCertificateService attestationCertificateService;

    @Inject
    private CertificateService certificateService;

    @Override
    public AttestationFormat getAttestationFormat() {
        return AttestationFormat.packed;
    }

    @Override
    public void process(JsonNode attStmt, AuthData authData, Fido2RegistrationData registration, byte[] clientDataHash,
            CredAndCounterData credIdAndCounters) {
        int alg = commonVerifiers.verifyAlgorithm(attStmt.get("alg"), authData.getKeyType());
        String signature = commonVerifiers.verifyBase64String(attStmt.get("sig"));


        if (attStmt.hasNonNull("x5c")) {
            List<X509Certificate> attestationCertificates = getAttestationCertificates(attStmt);

            X509TrustManager tm = attestationCertificateService.populateTrustManager(authData, attestationCertificates);
            if ((tm == null) || (tm.getAcceptedIssuers().length == 0)) {
                throw new Fido2RuntimeException(
                        "Packed full attestation but no certificates in metadata for authenticator " + Hex.encodeHexString(authData.getAaguid()));
            }

            credIdAndCounters.setSignatureAlgorithm(alg);

            X509Certificate verifiedCert = certificateVerifier.verifyAttestationCertificates(attestationCertificates, Arrays.asList(tm.getAcceptedIssuers()));

            authenticatorDataVerifier.verifyPackedAttestationSignature(authData.getAuthDataDecoded(), clientDataHash, signature, verifiedCert, alg);

            if (certificateVerifier.isSelfSigned(verifiedCert)) {
                throw new Fido2RuntimeException("Self signed certificate");
            }
        } else if (attStmt.hasNonNull("ecdaaKeyId")) {
            String ecdaaKeyId = attStmt.get("ecdaaKeyId").asText();
            throw new UnsupportedOperationException(ecdaaKeyId + " is not supported");
        } else {
            PublicKey publicKey = coseService.getPublicKeyFromUncompressedECPoint(authData.getCosePublicKey());
            authenticatorDataVerifier.verifyPackedSurrogateAttestationSignature(authData.getAuthDataDecoded(), clientDataHash, signature, publicKey, alg);
        }
        credIdAndCounters.setAttestationType(getAttestationFormat().getFmt());
        credIdAndCounters.setCredId(base64Service.urlEncodeToString(authData.getCredId()));
        credIdAndCounters.setUncompressedEcPoint(base64Service.urlEncodeToString(authData.getCosePublicKey()));
    }

	private List<X509Certificate> getAttestationCertificates(JsonNode attStmt) {
		Iterator<JsonNode> i = attStmt.get("x5c").elements();
		ArrayList<String> certificatePath = new ArrayList<String>();
		while (i.hasNext()) {
		    certificatePath.add(i.next().asText());
		}
		List<X509Certificate> certificates = certificateService.getCertificates(certificatePath);

		return certificates;
	}

}
