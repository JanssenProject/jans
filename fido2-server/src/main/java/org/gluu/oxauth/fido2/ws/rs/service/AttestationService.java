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

package org.gluu.oxauth.fido2.ws.rs.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.ctap.AttestationConveyancePreference;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.model.auth.CredAndCounterData;
import org.gluu.oxauth.fido2.model.cert.PublicKeyCredentialDescriptor;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationData;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationEntry;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationStatus;
import org.gluu.oxauth.fido2.persist.AuthenticationPersistenceService;
import org.gluu.oxauth.fido2.persist.RegistrationPersistenceService;
import org.gluu.oxauth.fido2.service.Base64Service;
import org.gluu.oxauth.fido2.service.ChallengeGenerator;
import org.gluu.oxauth.fido2.service.DataMapperService;
import org.gluu.oxauth.fido2.service.verifier.AuthenticatorAttestationVerifier;
import org.gluu.oxauth.fido2.service.verifier.ChallengeVerifier;
import org.gluu.oxauth.fido2.service.verifier.CommonVerifiers;
import org.gluu.oxauth.fido2.service.verifier.DomainVerifier;
import org.slf4j.Logger;
import org.xdi.oxauth.model.configuration.AppConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ApplicationScoped
public class AttestationService {

    @Inject
    private Logger log;

    @Inject
    private AuthenticationPersistenceService authenticationsRepository;

    @Inject
    private RegistrationPersistenceService registrationsRepository;

    @Inject
    private AuthenticatorAttestationVerifier authenticatorAttestationVerifier;

    @Inject
    private ChallengeVerifier challengeVerifier;

    @Inject
    private DomainVerifier domainVerifier;

    @Inject
    private ChallengeGenerator challengeGenerator;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private Base64Service base64Service;

    @Inject
    private AppConfiguration appConfiguration;

    public JsonNode options(JsonNode params) {
        log.info("options {}", params);
        return createNewRegistration(params);
    }

    public JsonNode verify(JsonNode params) {
        log.info("registerResponse {}", params);
        // JsonNode request = params.get("request");

        commonVerifiers.verifyBasicPayload(params);
        commonVerifiers.verifyBase64UrlString(params, "type");
        JsonNode response = params.get("response");
        JsonNode clientDataJSONNode = null;
        try {
            if (!params.get("response").hasNonNull("clientDataJSON")) {
                throw new Fido2RPRuntimeException("Client data JSON is missing");
            }
            clientDataJSONNode = dataMapperService
                    .readTree(new String(base64Service.urlDecode(params.get("response").get("clientDataJSON").asText()), Charset.forName("UTF-8")));
            if (clientDataJSONNode == null) {
                throw new Fido2RPRuntimeException("Client data JSON is empty");
            }
        } catch (IOException e) {
            throw new Fido2RPRuntimeException("Can't parse message");
        }

        commonVerifiers.verifyClientJSON(clientDataJSONNode);
        commonVerifiers.verifyClientJSONTypeIsCreate(clientDataJSONNode);
        String keyId = commonVerifiers.verifyBase64UrlString(params, "id");

        String clientDataChallenge = base64Service
                .urlEncodeToStringWithoutPadding(base64Service.urlDecode(clientDataJSONNode.get("challenge").asText()));
        log.info("Challenge {}", clientDataChallenge);
        // String clientDataOrigin = clientDataJSONNode.get("origin").asText();

        List<Fido2RegistrationEntry> registrationEntries = registrationsRepository.findAllByChallenge(clientDataChallenge);
        Fido2RegistrationEntry credentialEntryFound = registrationEntries.parallelStream().findAny()
                .orElseThrow(() -> new Fido2RPRuntimeException("Can't find request with matching challenge and domain"));
        
        Fido2RegistrationData credentialFound = credentialEntryFound.getRegistrationData();

        domainVerifier.verifyDomain(credentialFound.getDomain(), clientDataJSONNode.get("origin").asText());
        CredAndCounterData attestationData = authenticatorAttestationVerifier.verifyAuthenticatorAttestationResponse(response, credentialFound);

        credentialFound.setUncompressedECPoint(attestationData.getUncompressedEcPoint());
        credentialFound.setStatus(Fido2RegistrationStatus.registered);
        credentialFound.setW3cAuthenticatorAttenstationResponse(response.toString());
        credentialFound.setSignatureAlgorithm(attestationData.getSignatureAlgorithm());
        credentialFound.setCounter(attestationData.getCounters());
        if (attestationData.getCredId() != null) {
            credentialFound.setPublicKeyId(attestationData.getCredId());
        } else {
            credentialFound.setPublicKeyId(keyId);
        }
        credentialFound.setType("public-key");
        registrationsRepository.update(credentialEntryFound);

        ((ObjectNode) params).put("errorMessage", "");
        ((ObjectNode) params).put("status", "ok");
        return params;
    }

    private JsonNode createNewRegistration(JsonNode params) {
        commonVerifiers.verifyOptions(params);
        String username = params.get("username").asText();
        String displayName = params.get("displayName").asText();

        String documentDomain;
        String host;
        if (params.hasNonNull("documentDomain")) {
            documentDomain = params.get("documentDomain").asText();
        } else {
            documentDomain = appConfiguration.getIssuer();
        }

        try {
            host = new URL(documentDomain).getHost();
        } catch (MalformedURLException e) {
            host = documentDomain;
            // throw new Fido2RPRuntimeException(e.getMessage());
        }

        String authenticatorSelection;
        if (params.hasNonNull("authenticatorSelection")) {
            authenticatorSelection = params.get("authenticatorSelection").asText();
        } else {
            authenticatorSelection = "";
        }

        log.info("Options {} {} {}", username, displayName, documentDomain);
        AttestationConveyancePreference attestationConveyancePreference = commonVerifiers.verifyAttestationConveyanceType(params);

        String credentialType = params.hasNonNull("credentialType") ? params.get("credentialType").asText("public-key") : "public-key";
        ObjectNode credentialCreationOptionsNode = dataMapperService.createObjectNode();

        String challenge = challengeGenerator.getChallenge();
        credentialCreationOptionsNode.put("challenge", challenge);
        log.info("Challenge {}", challenge);
        ObjectNode credentialRpEntityNode = credentialCreationOptionsNode.putObject("rp");
        credentialRpEntityNode.put("name", "Mastercard RP");
        credentialRpEntityNode.put("id", documentDomain);

        ObjectNode credentialUserEntityNode = credentialCreationOptionsNode.putObject("user");
        byte[] buffer = new byte[32];
        new SecureRandom().nextBytes(buffer);
        String userId = base64Service.urlEncodeToString(buffer);
        credentialUserEntityNode.put("id", userId);
        credentialUserEntityNode.put("name", username);
        credentialUserEntityNode.put("displayName", displayName);
        credentialCreationOptionsNode.put("attestation", attestationConveyancePreference.toString());
        ArrayNode credentialParametersArrayNode = credentialCreationOptionsNode.putArray("pubKeyCredParams");
        ObjectNode credentialParametersNode = credentialParametersArrayNode.addObject();
        if ("public-key".equals(credentialType)) {
            credentialParametersNode.put("type", "public-key");
            credentialParametersNode.put("alg", -7);
        }
        if ("FIDO".equals(credentialType)) {
            credentialParametersNode.put("type", "FIDO");
            credentialParametersNode.put("alg", -7);
        }
        credentialCreationOptionsNode.set("authenticatorSelection", params.get("authenticatorSelection"));

        List<Fido2RegistrationEntry> existingRegistrations = registrationsRepository.findAllByUsername(username);
        List<JsonNode> excludedKeys = existingRegistrations.parallelStream()
                .filter(f -> (Fido2RegistrationStatus.registered.equals(f.getRegistrationData().getStatus())))
                .map(f -> dataMapperService.convertValue(
                        new PublicKeyCredentialDescriptor(f.getRegistrationData().getType(), f.getRegistrationData().getPublicKeyId()),
                        JsonNode.class))
                .collect(Collectors.toList());

        ArrayNode excludedCredentials = credentialCreationOptionsNode.putArray("excludeCredentials");
        excludedCredentials.addAll(excludedKeys);
        credentialCreationOptionsNode.put("status", "ok");
        credentialCreationOptionsNode.put("errorMessage", "");
        Fido2RegistrationData entity = new Fido2RegistrationData();
        entity.setUsername(username);
        entity.setUserId(userId);
        entity.setChallenge(challenge);
        entity.setDomain(host);
        entity.setStatus(Fido2RegistrationStatus.registered);
        entity.setW3cCredentialCreationOptions(credentialCreationOptionsNode.toString());
        entity.setAttestationConveyancePreferenceType(attestationConveyancePreference);
        registrationsRepository.save(entity);
        return credentialCreationOptionsNode;
    }
}
