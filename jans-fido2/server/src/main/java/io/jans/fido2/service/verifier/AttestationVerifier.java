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

import java.io.IOException;

import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.service.AuthenticatorDataParser;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.processor.attestation.AttestationProcessorFactory;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class AttestationVerifier {

    @Inject
    private Logger log;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private AuthenticatorDataParser authenticatorDataParser;

    @Inject
    private Base64Service base64Service;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private AttestationProcessorFactory attestationProcessorFactory;

    public CredAndCounterData verifyAuthenticatorAttestationResponse(JsonNode response, Fido2RegistrationData credential) {
        if (!(response.hasNonNull("attestationObject") && response.hasNonNull("clientDataJSON"))) {
            throw new Fido2RuntimeException("Authenticator data is invalid");
        }

        JsonNode authenticatorResponse = response;
        String base64AuthenticatorData = authenticatorResponse.get("attestationObject").asText();
        String clientDataJson = authenticatorResponse.get("clientDataJSON").asText();
        byte[] authenticatorDataBuffer = base64Service.urlDecode(base64AuthenticatorData);

        CredAndCounterData credIdAndCounters = new CredAndCounterData();
        try {
            AuthData authData;
            if (authenticatorDataBuffer == null) {
                throw new Fido2RuntimeException("Attestation object is empty");
            }
            JsonNode authenticatorDataNode = dataMapperService.cborReadTree(authenticatorDataBuffer);
            if (authenticatorDataNode == null) {
                throw new Fido2RuntimeException("Attestation JSON is empty");
            }
            String fmt = commonVerifiers.verifyFmt(authenticatorDataNode, "fmt");
            log.debug("Authenticator data {} {}", fmt, authenticatorDataNode.toString());
            
            credential.setAttestationType(fmt);
            
            JsonNode attStmt = authenticatorDataNode.get("attStmt");
            commonVerifiers.verifyAuthStatement(attStmt);

            JsonNode authDataNode = authenticatorDataNode.get("authData");
            String authDataText = commonVerifiers.verifyAuthData(authDataNode);
            authData = authenticatorDataParser.parseAttestationData(authDataText);

            int counter = authenticatorDataParser.parseCounter(authData.getCounters());
            commonVerifiers.verifyCounter(counter);
            credIdAndCounters.setCounters(counter);

            byte[] clientDataHash = DigestUtils.getSha256Digest().digest(base64Service.urlDecode(clientDataJson));
            AttestationFormatProcessor attestationProcessor = attestationProcessorFactory.getCommandProcessor(fmt);
            attestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters);

            return credIdAndCounters;
        } catch (IOException ex) {
            throw new Fido2RuntimeException("Failed to parse and verify authenticator attestation response data", ex);
        }
    }

}

