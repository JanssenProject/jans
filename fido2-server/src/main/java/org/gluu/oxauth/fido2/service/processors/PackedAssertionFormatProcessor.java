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

package org.gluu.oxauth.fido2.service.processors;

import java.security.PublicKey;
import java.util.Base64;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.gluu.oxauth.fido2.cryptoutils.COSEHelper;
import org.gluu.oxauth.fido2.ctap.AttestationFormat;
import org.gluu.oxauth.fido2.ctap.UserVerification;
import org.gluu.oxauth.fido2.model.entry.Fido2AuthenticationData;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationData;
import org.gluu.oxauth.fido2.service.AuthData;
import org.gluu.oxauth.fido2.service.AuthenticatorDataParser;
import org.gluu.oxauth.fido2.service.CommonVerifiers;
import org.gluu.oxauth.fido2.service.Fido2RPRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Named
public class PackedAssertionFormatProcessor implements AssertionFormatProcessor {

    @Inject
    private Logger log;

    @Inject
    private COSEHelper uncompressedECPointHelper;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private AuthenticatorDataParser authenticatorDataParser;

    @Inject
    @Named("cborMapper")
    private ObjectMapper cborMapper;

    @Inject
    @Named("base64UrlDecoder")
    private Base64.Decoder base64UrlDecoder;

    @Override
    public AttestationFormat getAttestationFormat() {
        return AttestationFormat.packed;
    }

    public void process(String base64AuthenticatorData, String signature, String clientDataJson, Fido2RegistrationData registration,
            Fido2AuthenticationData authenticationEntity) {
        AuthData authData = authenticatorDataParser.parseAssertionData(base64AuthenticatorData);
        commonVerifiers.verifyRpIdHash(authData, registration.getDomain());

        log.info("User verification option {}", authenticationEntity.getUserVerificationOption());
        if (UserVerification.valueOf(authenticationEntity.getUserVerificationOption()) == UserVerification.required) {
            commonVerifiers.verifyRequiredUserPresent(authData);
        }
        if (UserVerification.valueOf(authenticationEntity.getUserVerificationOption()) == UserVerification.preferred) {
            commonVerifiers.verifyPreferredUserPresent(authData);
        }
        if (UserVerification.valueOf(authenticationEntity.getUserVerificationOption()) == UserVerification.discouraged) {
            commonVerifiers.verifyDiscouragedUserPresent(authData);
        }

        byte[] clientDataHash = DigestUtils.getSha256Digest().digest(base64UrlDecoder.decode(clientDataJson));

        try {

            JsonNode uncompressedECPointNode = cborMapper.readTree(base64UrlDecoder.decode(registration.getUncompressedECPoint()));
            PublicKey publicKey = uncompressedECPointHelper.createUncompressedPointFromCOSEPublicKey(uncompressedECPointNode);

            log.info("Uncompressed ECpoint node {}", uncompressedECPointNode.toString());
            log.info("EC Public key hex {}", Hex.encodeHexString(publicKey.getEncoded()));

            commonVerifiers.verifyAssertionSignature(authData, clientDataHash, signature, publicKey, registration.getSignatureAlgorithm());
            int counter = authenticatorDataParser.parseCounter(authData.getCounters());
            commonVerifiers.verifyCounter(registration.getCounter(), counter);
            registration.setCounter(counter);
        } catch (Exception e) {
            throw new Fido2RPRuntimeException("General server error " + e.getMessage());
        }
    }
}
