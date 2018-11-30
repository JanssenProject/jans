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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.gluu.oxauth.fido2.cryptoutils.CoseService;
import org.gluu.oxauth.fido2.ctap.AttestationFormat;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.model.auth.AuthData;
import org.gluu.oxauth.fido2.model.entry.Fido2AuthenticationData;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationData;
import org.gluu.oxauth.fido2.service.AuthenticatorDataParser;
import org.gluu.oxauth.fido2.service.Base64Service;
import org.gluu.oxauth.fido2.service.DataMapperService;
import org.gluu.oxauth.fido2.service.verifier.CommonVerifiers;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class U2FAssertionFormatProcessor implements AssertionFormatProcessor {

    @Inject
    private Logger log;

    @Inject
    private CoseService coseService;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private AuthenticatorDataParser authenticatorDataParser;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private Base64Service base64Service;

    @Override
    public AttestationFormat getAttestationFormat() {
        return AttestationFormat.fido_u2f;
    }

    @Override
    public void process(String base64AuthenticatorData, String signature, String clientDataJson, Fido2RegistrationData registration,
            Fido2AuthenticationData authenticationEntity) {
        AuthData authData = authenticatorDataParser.parseAssertionData(base64AuthenticatorData);
        commonVerifiers.verifyUserPresent(authData);
        byte[] clientDataHash = DigestUtils.getSha256Digest().digest(base64Service.urlDecode(clientDataJson));

        try {
            JsonNode uncompressedECPointNode = dataMapperService.cborReadTree(base64Service.urlDecode(registration.getUncompressedECPoint()));
            PublicKey publicKey = coseService.createUncompressedPointFromCOSEPublicKey(uncompressedECPointNode);
            int coseCurveCode = coseService.getCodeCurve(uncompressedECPointNode);
            log.info("Uncompressed ECpoint node {}", uncompressedECPointNode.toString());
            log.info("Public key hex {}", Hex.encodeHexString(publicKey.getEncoded()));
            commonVerifiers.verifyAssertionSignature(authData, clientDataHash, signature, publicKey, registration.getSignatureAlgorithm());
            int counter = authenticatorDataParser.parseCounter(authData.getCounters());
            commonVerifiers.verifyCounter(registration.getCounter(), counter);
            registration.setCounter(counter);
        } catch (Exception e) {
            throw new Fido2RPRuntimeException("General server error " + e.getMessage());
        }
    }
}
