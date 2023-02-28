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

import java.security.PublicKey;

import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.ctap.AuthenticatorAttachment;
import io.jans.fido2.exception.Fido2CompromisedDevice;
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
 * Class which processes assertions of "packed" fmt (attestation type)
 *
 */
@ApplicationScoped
public class PackedAssertionFormatProcessor implements AssertionFormatProcessor {

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
        return AttestationFormat.packed;
    }

    @Override
    public void process(String base64AuthenticatorData, String signature, String clientDataJson, Fido2RegistrationData registration,
            Fido2AuthenticationData authenticationEntity) {
        AuthData authData = authenticatorDataParser.parseAssertionData(base64AuthenticatorData);
        commonVerifiers.verifyRpIdHash(authData, registration.getDomain());

        log.debug("User verification option {}", authenticationEntity.getUserVerificationOption());
        userVerificationVerifier.verifyUserVerificationOption(authenticationEntity.getUserVerificationOption(), authData);

        byte[] clientDataHash = DigestUtils.getSha256Digest().digest(base64Service.urlDecode(clientDataJson));

        try {
            int counter = authenticatorDataParser.parseCounter(authData.getCounters());
            commonVerifiers.verifyCounter(registration.getCounter(), counter);
            registration.setCounter(counter);

            JsonNode uncompressedECPointNode = dataMapperService.cborReadTree(base64Service.urlDecode(registration.getUncompressedECPoint()));
            PublicKey publicKey = coseService.createUncompressedPointFromCOSEPublicKey(uncompressedECPointNode);

            log.debug("Uncompressed ECpoint node {}", uncompressedECPointNode.toString());
            log.debug("EC Public key hex {}", Hex.encodeHexString(publicKey.getEncoded()));
            // apple algorithm = -7
            // windows hello algorithm is -257
            log.debug("registration.getSignatureAlgorithm(): "+registration.getSignatureAlgorithm());
            log.debug("Platform authenticator: "+ String.valueOf(registration.getAttenstationRequest().contains(AuthenticatorAttachment.PLATFORM.getAttachment()) ? -7 : registration.getSignatureAlgorithm()));
            authenticatorDataVerifier.verifyAssertionSignature(authData, clientDataHash, signature, publicKey, registration.getAttenstationRequest().contains(AuthenticatorAttachment.PLATFORM.getAttachment()) ? -7 : registration.getSignatureAlgorithm());
           
        } catch (Fido2CompromisedDevice ex) {
        	throw ex;
        } catch (Exception ex) {
            throw new Fido2RuntimeException("Failed to check packet assertion", ex);
        }
    }

}
