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

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.google.safetynet.AttestationStatement;
import io.jans.fido2.google.safetynet.OfflineVerify;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.fido2.service.verifier.CommonVerifiers;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Attestation processor for attestations of fmt = android-safetynet
 *
 */
@ApplicationScoped
public class AndroidSafetyNetAttestationProcessor implements AttestationFormatProcessor {

    @Inject
    private Logger log;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private AttestationCertificateService attestationCertificateService;

    @Inject
    private Base64Service base64Service;

    @Override
    public AttestationFormat getAttestationFormat() {
        return AttestationFormat.android_safetynet;
    }

    @Override
    public void process(JsonNode attStmt, AuthData authData, Fido2RegistrationData credential, byte[] clientDataHash,
            CredAndCounterData credIdAndCounters) {

        commonVerifiers.verifyThatNonEmptyString(attStmt, "ver");
        String response = attStmt.get("response").asText();
        String aaguid = Hex.encodeHexString(authData.getAaguid());
        log.debug("Android safetynet payload {} {}", aaguid, new String(base64Service.decode(response)));

        X509TrustManager tm = attestationCertificateService.populateTrustManager(authData, null);
        AttestationStatement stmt;
        try {
            stmt = OfflineVerify.parseAndVerify(new String(base64Service.decode(response)), tm);
        } catch (Exception e) {
            throw new Fido2RuntimeException("Invalid safety net attestation " + e.getMessage());
        }

        if (stmt == null) {
            throw new Fido2RuntimeException("Invalid safety net attestation");
        }

        byte[] b1 = authData.getAuthDataDecoded();
        byte[] b2 = clientDataHash;
        byte[] buffer = ByteBuffer.allocate(b1.length + b2.length).put(b1).put(b2).array();
        byte[] hashedBuffer = DigestUtils.getSha256Digest().digest(buffer);
        byte[] nonce = stmt.getNonce();
        if (!Arrays.equals(hashedBuffer, nonce)) {
            throw new Fido2RuntimeException("Invalid safety net attestation");
        }

        if (!stmt.isCtsProfileMatch()) {
            throw new Fido2RuntimeException("Invalid safety net attestation");
        }

        Instant timestamp = Instant.ofEpochMilli(stmt.getTimestampMs());

        if (timestamp.isAfter(Instant.now())) {
            throw new Fido2RuntimeException("Invalid safety net attestation");
        }

        if (timestamp.isBefore(Instant.now().minus(1, ChronoUnit.MINUTES))) {
            throw new Fido2RuntimeException("Invalid safety net attestation");
        }

        credIdAndCounters.setAttestationType(getAttestationFormat().getFmt());
        credIdAndCounters.setCredId(base64Service.urlEncodeToString(authData.getCredId()));
        credIdAndCounters.setUncompressedEcPoint(base64Service.urlEncodeToString(authData.getCosePublicKey()));
    }
}
