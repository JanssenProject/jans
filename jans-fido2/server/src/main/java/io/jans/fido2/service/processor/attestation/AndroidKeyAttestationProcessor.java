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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import io.jans.fido2.androind.AndroidKeyUtils;
import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.fido2.service.verifier.AuthenticatorDataVerifier;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Attestation processor for attestations of fmt = android-key
 *
 */
@ApplicationScoped
public class AndroidKeyAttestationProcessor implements AttestationFormatProcessor {

    @Inject
    private Logger log;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private AuthenticatorDataVerifier authenticatorDataVerifier;

    @Inject
    private CertificateService certificateService;

    @Inject
    private CertificateVerifier certificateVerifier;

    @Inject
    private AndroidKeyUtils androidKeyUtils;

    @Inject
    private AttestationCertificateService attestationCertificateService;

    @Override
    public AttestationFormat getAttestationFormat() {
        return AttestationFormat.android_key;
    }

    @Override
    public void process(JsonNode attStmt, AuthData authData, Fido2RegistrationData credential, byte[] clientDataHash,
            CredAndCounterData credIdAndCounters) {

        log.debug("Android-key payload");

        Iterator<JsonNode> i = attStmt.get("x5c").elements();

        ArrayList<String> certificatePath = new ArrayList();
        while (i.hasNext()) {
            certificatePath.add(i.next().asText());
        }
        List<X509Certificate> certificates = certificateService.getCertificates(certificatePath);
        List<X509Certificate> trustAnchorCertificates = attestationCertificateService.getAttestationRootCertificates(authData, certificates);

        X509Certificate verifiedCert = certificateVerifier.verifyAttestationCertificates(certificates, trustAnchorCertificates);

        try {
            ASN1Sequence extensionData = androidKeyUtils.extractAttestationSequence(verifiedCert);
            int attestationVersion = AndroidKeyUtils.getIntegerFromAsn1(extensionData.getObjectAt(AndroidKeyUtils.ATTESTATION_VERSION_INDEX));
            int attestationSecurityLevel = AndroidKeyUtils
                    .getIntegerFromAsn1(extensionData.getObjectAt(AndroidKeyUtils.ATTESTATION_SECURITY_LEVEL_INDEX));
            int keymasterSecurityLevel = AndroidKeyUtils
                    .getIntegerFromAsn1(extensionData.getObjectAt(AndroidKeyUtils.KEYMASTER_SECURITY_LEVEL_INDEX));
            byte[] attestationChallenge = ((ASN1OctetString) extensionData.getObjectAt(AndroidKeyUtils.ATTESTATION_CHALLENGE_INDEX)).getOctets();

            if (!Arrays.equals(clientDataHash, attestationChallenge)) {
                throw new Fido2RuntimeException("Invalid android key attestation");
            }

            ASN1Encodable[] softwareEnforced = ((ASN1Sequence) extensionData.getObjectAt(AndroidKeyUtils.SW_ENFORCED_INDEX)).toArray();
            ASN1Encodable[] teeEnforced = ((ASN1Sequence) extensionData.getObjectAt(AndroidKeyUtils.TEE_ENFORCED_INDEX)).toArray();

        } catch (Exception e) {
            log.warn("Problem with android key", e);
            throw new Fido2RuntimeException("Problem with android key");
        }
        String signature = commonVerifiers.verifyBase64String(attStmt.get("sig"));
        authenticatorDataVerifier.verifyAttestationSignature(authData, clientDataHash, signature, verifiedCert, authData.getKeyType());

        // credIdAndCounters.setAttestationType(getAttestationFormat().getFmt());
        // credIdAndCounters.setCredId(base64Service.urlEncodeToString(authData.getCredId()));
        // credIdAndCounters.setUncompressedEcPoint(base64Service.urlEncodeToString(authData.getCOSEPublicKey()));
    }
}
