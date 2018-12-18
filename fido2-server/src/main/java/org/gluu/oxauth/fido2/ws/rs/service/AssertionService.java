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
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.model.entry.Fido2AuthenticationData;
import org.gluu.oxauth.fido2.model.entry.Fido2AuthenticationEntry;
import org.gluu.oxauth.fido2.model.entry.Fido2AuthenticationStatus;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationData;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationEntry;
import org.gluu.oxauth.fido2.persist.AuthenticationPersistenceService;
import org.gluu.oxauth.fido2.persist.RegistrationPersistenceService;
import org.gluu.oxauth.fido2.service.Base64Service;
import org.gluu.oxauth.fido2.service.ChallengeGenerator;
import org.gluu.oxauth.fido2.service.DataMapperService;
import org.gluu.oxauth.fido2.service.verifier.AuthenticatorAssertionVerifier;
import org.gluu.oxauth.fido2.service.verifier.ChallengeVerifier;
import org.gluu.oxauth.fido2.service.verifier.CommonVerifiers;
import org.gluu.oxauth.fido2.service.verifier.DomainVerifier;
import org.slf4j.Logger;
import org.xdi.oxauth.model.configuration.AppConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ApplicationScoped
public class AssertionService {

    @Inject
    private Logger log;

    @Inject
    private ChallengeVerifier challengeVerifier;

    @Inject
    private DomainVerifier domainVerifier;

    @Inject
    private RegistrationPersistenceService registrationsRepository;

    @Inject
    private AuthenticationPersistenceService authenticationsRepository;

    @Inject
    private AuthenticatorAssertionVerifier authenticatorAuthorizationVerifier;

    @Inject
    private ChallengeGenerator challengeGenerator;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private Base64Service base64Service;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private AppConfiguration appConfiguration;

    public JsonNode options(JsonNode params) {
        log.info("options {}", params);
        return assertionOptions(params);
    }

    public JsonNode verify(JsonNode params) {
        log.info("authenticateResponse {}", params);
        ObjectNode authenticateResponseNode = dataMapperService.createObjectNode();
        JsonNode response = params.get("response");

        commonVerifiers.verifyBasicPayload(params);
        String keyId = commonVerifiers.verifyThatString(params, "id");
        commonVerifiers.verifyAssertionType(params.get("type"));
        commonVerifiers.verifyThatString(params, "rawId");
        JsonNode userHandle = params.get("response").get("userHandle");
        if (userHandle != null && params.get("response").hasNonNull("userHandle")) {
            // This can be null for U2F authenticators
            commonVerifiers.verifyThatString(userHandle);
        }

        JsonNode clientDataJSONNode;
        try {
            clientDataJSONNode = dataMapperService
                    .readTree(new String(base64Service.urlDecode(params.get("response").get("clientDataJSON").asText()), Charset.forName("UTF-8")));
        } catch (IOException e) {
            throw new Fido2RPRuntimeException("Can't parse message");
        } catch (Exception e) {
            throw new Fido2RPRuntimeException("Invalid assertion data");
        }

        commonVerifiers.verifyClientJSON(clientDataJSONNode);
        commonVerifiers.verifyClientJSONTypeIsGet(clientDataJSONNode);

        String clientDataChallenge = clientDataJSONNode.get("challenge").asText();
        String clientDataOrigin = clientDataJSONNode.get("origin").asText();

        Fido2AuthenticationEntry authenticationEntity = authenticationsRepository.findByChallenge(clientDataChallenge).parallelStream().findFirst()
                .orElseThrow(() -> new Fido2RPRuntimeException(String.format("Can't find matching request by challenge '%s'", clientDataChallenge)));
        
        Fido2AuthenticationData authenticationData = authenticationEntity.getAuthenticationData();

        // challengeVerifier.verifyChallenge(authenticationEntity.getChallenge(),
        // challenge, clientDataChallenge);
        domainVerifier.verifyDomain(authenticationData.getDomain(), clientDataOrigin);

        Fido2RegistrationEntry registrationEntry = registrationsRepository.findByPublicKeyId(keyId)
                .orElseThrow(() -> new Fido2RPRuntimeException(String.format("Couldn't find the key by PublicKeyId '%s'", keyId)));
        Fido2RegistrationData registrationData = registrationEntry.getRegistrationData();

        authenticatorAuthorizationVerifier.verifyAuthenticatorAssertionResponse(response, registrationData, authenticationData);

        authenticationData.setW3cAuthenticatorAssertionResponse(response.toString());
        authenticationData.setStatus(Fido2AuthenticationStatus.authenticated);

        authenticationsRepository.update(authenticationEntity);

        authenticateResponseNode.put("status", "ok");
        authenticateResponseNode.put("errorMessage", "");
        return authenticateResponseNode;
    }

    private JsonNode assertionOptions(JsonNode params) {
        log.info("assertionOptions {}", params);
        String username = params.get("username").asText();
        String userVerification = "required";

        if (params.hasNonNull("authenticatorSelection")) {
            JsonNode authenticatorSelector = params.get("authenticatorSelection");
            if (authenticatorSelector.hasNonNull("userVerification")) {
                userVerification = commonVerifiers.verifyUserVerification(authenticatorSelector.get("userVerification"));
            }
        }

        log.info("Options {} ", username);

        ObjectNode assertionOptionsResponseNode = dataMapperService.createObjectNode();
        List<Fido2RegistrationEntry> registrationEntries = registrationsRepository.findAllByUsername(username);
        if (registrationEntries.isEmpty()) {
            throw new Fido2RPRuntimeException("No record of registration. Have you registered");
        }
        
        List<Fido2RegistrationData> registrations = registrationEntries.parallelStream().map(f -> f.getRegistrationData()).collect(Collectors.toList());

        String challenge = challengeGenerator.getChallenge();
        assertionOptionsResponseNode.put("challenge", challenge);

        ObjectNode credentialUserEntityNode = assertionOptionsResponseNode.putObject("user");
        credentialUserEntityNode.put("name", username);

        ArrayNode publicKeyCredentialDescriptors = assertionOptionsResponseNode.putArray("allowCredentials");

        for (Fido2RegistrationData registration : registrations) {
            if (StringUtils.isEmpty(registration.getPublicKeyId())) {
                throw new Fido2RPRuntimeException("Can't find associated key. Have you registered");
            }
            ObjectNode publicKeyCredentialDescriptorNode = publicKeyCredentialDescriptors.addObject();
            publicKeyCredentialDescriptorNode.put("type", "public-key");
            ArrayNode authenticatorTransportNode = publicKeyCredentialDescriptorNode.putArray("transports");
            authenticatorTransportNode.add("usb").add("ble").add("nfc");
            publicKeyCredentialDescriptorNode.put("id", registration.getPublicKeyId());
        }

        if (!foundPublicKeys) {
            throw new Fido2RPRuntimeException("Can't find associated key. Have you registered");
        }

        assertionOptionsResponseNode.put("userVerification", userVerification);
        
        if (params.hasNonNull("extensions")) {
            assertionOptionsResponseNode.set("extensions", params.get("extensions"));
        }

        String host;
        try {
            host = new URL(appConfiguration.getIssuer()).getHost();
        } catch (MalformedURLException e) {
            host = appConfiguration.getIssuer();
        }

        Fido2AuthenticationData authenticationData = new Fido2AuthenticationData();
        authenticationData.setUsername(username);
        authenticationData.setChallenge(challenge);
        authenticationData.setDomain(host);
        authenticationData.setW3cCredentialRequestOptions(assertionOptionsResponseNode.toString());
        authenticationData.setUserVerificationOption(userVerification);
        authenticationData.setStatus(Fido2AuthenticationStatus.pending);

        authenticationsRepository.save(authenticationData);

        assertionOptionsResponseNode.put("status", "ok");
        assertionOptionsResponseNode.put("errorMessage", "");

        return assertionOptionsResponseNode;
    }
}
