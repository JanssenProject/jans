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

package org.gluu.oxauth.fido2.service.verifier;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.model.auth.AuthData;
import org.gluu.oxauth.fido2.model.auth.CredAndCounterData;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationData;
import org.gluu.oxauth.fido2.service.AuthenticatorDataParser;
import org.gluu.oxauth.fido2.service.Base64Service;
import org.gluu.oxauth.fido2.service.DataMapperService;
import org.gluu.oxauth.fido2.service.processors.AttestationFormatProcessor;
import org.gluu.oxauth.fido2.service.processors.impl.AttestationProcessorFactory;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class AuthenticatorAttestationVerifier {

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
        JsonNode authenticatorResponse = response;
        String base64AuthenticatorData = authenticatorResponse.get("attestationObject").asText();
        String clientDataJson = authenticatorResponse.get("clientDataJSON").asText();
        byte[] authenticatorDataBuffer = base64Service.urlDecode(base64AuthenticatorData);
        CredAndCounterData credIdAndCounters = new CredAndCounterData();
        try {
            AuthData authData;
            JsonNode authenticatorDataNode = dataMapperService.cborReadTree(authenticatorDataBuffer);
            String fmt = commonVerifiers.verifyFmt(authenticatorDataNode.get("fmt"));
            log.info("Authenticator data {} {}", fmt, authenticatorDataNode.toString());
            credential.setAttestationType(fmt);
            JsonNode authDataNode = authenticatorDataNode.get("authData");
            String authDataText = commonVerifiers.verifyAuthData(authDataNode);
            JsonNode attStmt = authenticatorDataNode.get("attStmt");

            authData = authenticatorDataParser.parseAttestationData(authDataText);
            int counter = authenticatorDataParser.parseCounter(authData.getCounters());
            commonVerifiers.verifyCounter(counter);
            credIdAndCounters.setCounters(counter);
            byte[] clientDataHash = DigestUtils.getSha256Digest().digest(base64Service.urlDecode(clientDataJson));
            AttestationFormatProcessor attestationProcessor = attestationProcessorFactory.getCommandProcessor(fmt);
            attestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters);
            return credIdAndCounters;
        } catch (IOException e) {
            throw new Fido2RPRuntimeException("Problem with processing authenticator data");
        }
    }
}
