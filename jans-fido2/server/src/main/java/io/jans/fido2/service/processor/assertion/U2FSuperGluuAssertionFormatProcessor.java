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

package io.jans.fido2.service.processor.assertion;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.service.AuthenticatorDataParser;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.processors.AssertionFormatProcessor;
import io.jans.fido2.service.verifier.AuthenticatorDataVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.UserVerificationVerifier;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *  Class which processes assertions of "fido2-u2f" fmt (attestation type)
 *
 */
@ApplicationScoped
public class U2FSuperGluuAssertionFormatProcessor implements AssertionFormatProcessor {

    @Inject
    private Logger log;

    @Inject
    private CoseService coseService;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private AuthenticatorDataVerifier authenticatorDataVerifier;

    @Inject
    private UserVerificationVerifier userVerificationVerifier;

    @Inject
    private AuthenticatorDataParser authenticatorDataParser;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private Base64Service base64Service;

    @Override
    public AttestationFormat getAttestationFormat() {
        return AttestationFormat.fido_u2f_super_gluu;
    }

    @Override
    public void process(String base64AuthenticatorData, String signature, String clientDataJson, Fido2RegistrationData registration,
            Fido2AuthenticationData authenticationEntity) {
        AuthData authData = authenticatorDataParser.parseAssertionData(base64AuthenticatorData);

//        String clientDataRaw = commonVerifiers.verifyClientRaw(response).asText();
        userVerificationVerifier.verifyUserPresent(authData);

        String clientDataJsonString = new String(base64Service.urlDecode(clientDataJson), StandardCharsets.UTF_8);
        
        byte[] clientDataHash = DigestUtils.getSha256Digest().digest(clientDataJsonString.getBytes(StandardCharsets.UTF_8));

        try {
            int counter = authenticatorDataParser.parseCounter(authData.getCounters());
            commonVerifiers.verifyCounter(registration.getCounter(), counter);
            registration.setCounter(counter);

            JsonNode uncompressedECPointNode = dataMapperService.cborReadTree(base64Service.urlDecode(registration.getUncompressedECPoint()));
            PublicKey publicKey = coseService.createUncompressedPointFromCOSEPublicKey(uncompressedECPointNode);
            log.debug("Uncompressed ECpoint node {}", uncompressedECPointNode.toString());
            log.debug("Public key hex {}", Hex.encodeHexString(publicKey.getEncoded()));

            authenticatorDataVerifier.verifyAssertionSignature(authData, clientDataHash, signature, publicKey, registration.getSignatureAlgorithm());
        } catch (Exception ex) {
            throw new Fido2RuntimeException("Failed to check U2F assertion", ex);
        }
    }
}
