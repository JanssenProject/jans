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

import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.gluu.oxauth.fido2.certification.CertificationKeyStoreUtils;
import org.gluu.oxauth.fido2.cryptoutils.AndroidKeyUtils;
import org.gluu.oxauth.fido2.cryptoutils.CryptoUtils;
import org.gluu.oxauth.fido2.ctap.AttestationFormat;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.model.auth.AuthData;
import org.gluu.oxauth.fido2.model.auth.CredAndCounterData;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationData;
import org.gluu.oxauth.fido2.service.CertificateValidator;
import org.gluu.oxauth.fido2.service.CommonVerifiers;
import org.gluu.oxauth.fido2.service.processors.AttestationFormatProcessor;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class AndroidKeyAttestationProcessor implements AttestationFormatProcessor {

    @Inject
    private Logger log;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private CryptoUtils cryptoUtils;

    @Inject
    private CertificateValidator certificateValidator;

    @Inject
    private AndroidKeyUtils androidKeyUtils;

    @Inject
    private CertificationKeyStoreUtils utils;

    @Override
    public AttestationFormat getAttestationFormat() {
        return AttestationFormat.android_key;
    }

    @Override
    public void process(JsonNode attStmt, AuthData authData, Fido2RegistrationData credential, byte[] clientDataHash,
            CredAndCounterData credIdAndCounters) {

        log.info("Android-key payload ");

        Iterator<JsonNode> i = attStmt.get("x5c").elements();

        ArrayList<String> certificatePath = new ArrayList();
        while (i.hasNext()) {
            certificatePath.add(i.next().asText());
        }
        List<X509Certificate> certificates = cryptoUtils.getCertificates(certificatePath);
        List<X509Certificate> trustAnchorCertificates = utils.getCertificates(authData);
        X509Certificate verifiedCert = certificateValidator.verifyAttestationCertificates(certificates, trustAnchorCertificates);
        ECPublicKey pubKey = (ECPublicKey) verifiedCert.getPublicKey();

        try {
            ASN1Sequence extensionData = androidKeyUtils.extractAttestationSequence(verifiedCert);
            int attestationVersion = AndroidKeyUtils.getIntegerFromAsn1(extensionData.getObjectAt(AndroidKeyUtils.ATTESTATION_VERSION_INDEX));
            int attestationSecurityLevel = AndroidKeyUtils
                    .getIntegerFromAsn1(extensionData.getObjectAt(AndroidKeyUtils.ATTESTATION_SECURITY_LEVEL_INDEX));
            int keymasterSecurityLevel = AndroidKeyUtils
                    .getIntegerFromAsn1(extensionData.getObjectAt(AndroidKeyUtils.KEYMASTER_SECURITY_LEVEL_INDEX));
            byte[] attestationChallenge = ((ASN1OctetString) extensionData.getObjectAt(AndroidKeyUtils.ATTESTATION_CHALLENGE_INDEX)).getOctets();

            if (!Arrays.equals(clientDataHash, attestationChallenge)) {
                throw new Fido2RPRuntimeException("Invalid android key attestation ");
            }

            ASN1Encodable[] softwareEnforced = ((ASN1Sequence) extensionData.getObjectAt(AndroidKeyUtils.SW_ENFORCED_INDEX)).toArray();
            ASN1Encodable[] teeEnforced = ((ASN1Sequence) extensionData.getObjectAt(AndroidKeyUtils.TEE_ENFORCED_INDEX)).toArray();

        } catch (Exception e) {
            log.warn("Problem with android key", e);
            throw new Fido2RPRuntimeException("Problem with android key");
        }
        String signature = commonVerifiers.verifyBase64String(attStmt.get("sig"));
        commonVerifiers.verifyAttestationSignature(authData, clientDataHash, signature, verifiedCert, authData.getKeyType());

        // credIdAndCounters.setAttestationType(getAttestationFormat().getFmt());
        // credIdAndCounters.setCredId(base64Service.urlEncodeToString(authData.getCredId()));
        // credIdAndCounters.setUncompressedEcPoint(base64Service.urlEncodeToString(authData.getCOSEPublicKey()));
    }
}
