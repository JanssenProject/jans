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

package io.jans.fido2.service.verifier;

import java.security.PublicKey;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import io.jans.fido2.exception.Fido2CompromisedDevice;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.assertion.Response;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.service.AuthenticatorDataParser;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.util.DigestUtilService;
import io.jans.fido2.service.util.HexUtilService;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AssertionVerifier {

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

    @Inject
    private DigestUtilService digestUtilService;

    @Inject
    private HexUtilService hexUtilService;

    public void verifyAuthenticatorAssertionResponse(Response response, Fido2RegistrationData registration,
                                                     Fido2AuthenticationData authenticationEntity) {
        if (Strings.isNullOrEmpty(response.getAuthenticatorData()) ||
                Strings.isNullOrEmpty(response.getClientDataJSON()) ||
                Strings.isNullOrEmpty(response.getSignature())) {
            throw new Fido2RuntimeException("Authenticator data is invalid");
        }

        String base64AuthenticatorData = response.getAuthenticatorData();
        String clientDataJson = response.getClientDataJSON();
        String signature = response.getSignature();

        log.debug("Authenticator data {}", base64AuthenticatorData);
        

        process(base64AuthenticatorData, signature, clientDataJson, registration, authenticationEntity);
    }
    public void process(String base64AuthenticatorData, String signature, String clientDataJson, Fido2RegistrationData registration,
            Fido2AuthenticationData authenticationEntity) {
        AuthData authData = authenticatorDataParser.parseAssertionData(base64AuthenticatorData);
        commonVerifiers.verifyRpIdHash(authData, registration.getOrigin());

        log.debug("User verification option {}", authenticationEntity.getUserVerificationOption());
        userVerificationVerifier.verifyUserVerificationOption(authenticationEntity.getUserVerificationOption(), authData);

        byte[] clientDataHash = digestUtilService.sha256Digest(base64Service.urlDecode(clientDataJson));
 
        try {
            int counter = authenticatorDataParser.parseCounter(authData.getCounters());
            commonVerifiers.verifyCounter(registration.getCounter(), counter);
            registration.setCounter(counter);

            JsonNode uncompressedECPointNode = dataMapperService.cborReadTree(base64Service.urlDecode(registration.getUncompressedECPoint()));
            PublicKey publicKey = coseService.createUncompressedPointFromCOSEPublicKey(uncompressedECPointNode);

            log.debug("Uncompressed ECpoint node {}", uncompressedECPointNode);
            log.debug("EC Public key hex {}", hexUtilService.encodeHexString(publicKey.getEncoded()));
            log.debug("registration.getSignatureAlgorithm(): "+registration.getSignatureAlgorithm());
            
            authenticatorDataVerifier.verifyAssertionSignature(authData, clientDataHash, signature, publicKey,  registration.getSignatureAlgorithm());
           
        } catch (Fido2CompromisedDevice ex) {
        	throw ex;
        } catch (Exception ex) {
            throw new Fido2RuntimeException("Failed to check assertion", ex);
        }
    }
}
