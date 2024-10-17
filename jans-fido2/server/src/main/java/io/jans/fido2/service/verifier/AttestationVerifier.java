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

import com.google.common.base.Strings;
import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.attestation.Response;
import io.jans.fido2.model.conf.AttestationMode;
import io.jans.fido2.model.error.ErrorResponseFactory;
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

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AppConfiguration appConfiguration;

    public CredAndCounterData verifyAuthenticatorAttestationResponse(Response response, Fido2RegistrationData credential) {
        if (Strings.isNullOrEmpty(response.getAttestationObject()) || Strings.isNullOrEmpty(response.getClientDataJSON())) {
            throw errorResponseFactory.invalidRequest("Authenticator data is invalid");
        }

        String base64AuthenticatorData = response.getAttestationObject();
        String clientDataJson = response.getClientDataJSON();
        byte[] authenticatorDataBuffer = base64Service.urlDecode(base64AuthenticatorData);

        CredAndCounterData credIdAndCounters = new CredAndCounterData();
        try {
            AuthData authData;
            if (authenticatorDataBuffer == null) {
                throw errorResponseFactory.invalidRequest("Attestation object is empty");
            }
            JsonNode authenticatorDataNode = dataMapperService.cborReadTree(authenticatorDataBuffer);
            if (authenticatorDataNode == null) {
                throw errorResponseFactory.invalidRequest("Attestation JSON is empty");
            }
            String fmt = commonVerifiers.verifyFmt(authenticatorDataNode, "fmt");
            log.debug("Authenticator data {} {}", fmt, authenticatorDataNode);
            
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
            log.debug("attestationProcessor : "+attestationProcessor.getClass());

            if (AttestationMode.DISABLED.getValue().equals(appConfiguration.getFido2Configuration().getAttestationMode())) {
                log.warn("SkipValidateMdsInAttestation is enabled");
            } else {
                if (AttestationMode.ENFORCED.getValue().equals(appConfiguration.getFido2Configuration().getAttestationMode()) && fmt.equals(AttestationFormat.none.getFmt())) {
                    throw new Fido2RuntimeException("Unauthorized to perform this action");
                }
                else {
                	log.debug("Invoking Format Processers");
                    attestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters);
                }
            }
            //get flags buffer
            byte[] flagsBuffer = authData.getFlags();
            credIdAndCounters.setBackupEligibilityFlag(authenticatorDataParser.verifyBackupEligibility(flagsBuffer));
            credIdAndCounters.setBackupStateFlag(authenticatorDataParser.verifyBackupState(flagsBuffer));
            
            credIdAndCounters.setAttestedCredentialDataFlag(authenticatorDataParser.verifyAtFlag(flagsBuffer));
            
            credIdAndCounters.setUserPresentFlag(authenticatorDataParser.verifyUPFlag(flagsBuffer));
            credIdAndCounters.setUserVerifiedFlag(authenticatorDataParser.verifyUVFlag(flagsBuffer));
            log.debug("credIdAndCounters : "+credIdAndCounters.toString());
            return credIdAndCounters;
        } catch (IOException ex) {
            throw errorResponseFactory.invalidRequest("Failed to parse and verify authenticator attestation response data", ex);
        }
    }

}

