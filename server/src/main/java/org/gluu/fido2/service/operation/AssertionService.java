/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package org.gluu.fido2.service.operation;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.fido.model.entry.DeviceRegistration;
import org.gluu.fido2.ctap.UserVerification;
import org.gluu.fido2.exception.Fido2RPRuntimeException;
import org.gluu.fido2.model.auth.PublicKeyCredentialDescriptor;
import org.gluu.fido2.model.conf.AppConfiguration;
import org.gluu.fido2.model.entry.Fido2AuthenticationData;
import org.gluu.fido2.model.entry.Fido2AuthenticationEntry;
import org.gluu.fido2.model.entry.Fido2AuthenticationStatus;
import org.gluu.fido2.model.entry.Fido2RegistrationData;
import org.gluu.fido2.model.entry.Fido2RegistrationEntry;
import org.gluu.fido2.service.ChallengeGenerator;
import org.gluu.fido2.service.DataMapperService;
import org.gluu.fido2.service.persist.AuthenticationPersistenceService;
import org.gluu.fido2.service.persist.RegistrationPersistenceService;
import org.gluu.fido2.service.verifier.AssertionVerifier;
import org.gluu.fido2.service.verifier.CommonVerifiers;
import org.gluu.fido2.service.verifier.DomainVerifier;
import org.gluu.service.net.NetworkService;
import org.gluu.u2f.service.persist.DeviceRegistrationService;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class AssertionService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private DomainVerifier domainVerifier;

    @Inject
    private RegistrationPersistenceService registrationPersistenceService;

    @Inject
    private AuthenticationPersistenceService authenticationPersistenceService;
    
    @Inject
    private DeviceRegistrationService deviceRegistrationService;

    @Inject
    private AssertionVerifier assertionVerifier;

    @Inject
    private ChallengeGenerator challengeGenerator;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private CommonVerifiers commonVerifiers;
    
    @Inject
    private NetworkService networkService;

    /*
     * Requires mandatory parameters: username
     * Support non mandatory parameters: userVerification, documentDomain, extensions, timeout
     */
    public JsonNode options(JsonNode params) {
        log.debug("Assertion options {}", params);

        // Verify request parameters
        commonVerifiers.verifyAssertionOptions(params);

        // Get username
        String username = commonVerifiers.verifyThatFieldString(params, "username");

        // Create result object
        ObjectNode optionsResponseNode = dataMapperService.createObjectNode();

        // Put userVerification
        UserVerification userVerification = commonVerifiers.prepareUserVerification(params);
        optionsResponseNode.put("userVerification", userVerification.name());

        // Generate and put challenge
        String challenge = challengeGenerator.getChallenge();
        optionsResponseNode.put("challenge", challenge);
        log.debug("Put challenge {}", challenge);

        // Put RP
        String documentDomain = commonVerifiers.verifyRpDomain(params);
        log.debug("Put rpId {}", documentDomain);
        optionsResponseNode.put("rpId", documentDomain);

        // Put allowCredentials
        ArrayNode allowedCredentials = prepareAllowedCredentials(documentDomain, username);
        if (allowedCredentials.isEmpty()) {
            throw new Fido2RPRuntimeException("Can't find associated key(s). Username: " + username);
        }
        optionsResponseNode.set("allowCredentials", allowedCredentials);
        log.debug("Put allowedCredentials {}", allowedCredentials);

        // Put timeout
        int timeout = commonVerifiers.verifyTimeout(params);
        log.debug("Put timeout {}", timeout);
        optionsResponseNode.put("timeout", timeout);

        // Copy extensions
        if (params.hasNonNull("extensions")) {
        	JsonNode extensions = params.get("extensions");
            optionsResponseNode.set("extensions", extensions);
            log.debug("Put extensions {}", extensions);
        }

        optionsResponseNode.put("status", "ok");
        optionsResponseNode.put("errorMessage", "");

        Fido2AuthenticationData entity = new Fido2AuthenticationData();
        entity.setUsername(username);
        entity.setChallenge(challenge);
        entity.setDomain(documentDomain);
        entity.setUserVerificationOption(userVerification);
        entity.setStatus(Fido2AuthenticationStatus.pending);

        // Store original request
        entity.setAssertionRequest(params.toString());

        authenticationPersistenceService.save(entity);

        return optionsResponseNode;

    }

    public JsonNode verify(JsonNode params) {
        log.debug("authenticateResponse {}", params);

        // Verify if there are mandatory request parameters
        commonVerifiers.verifyBasicPayload(params);
        commonVerifiers.verifyAssertionType(params, "type");
        commonVerifiers.verifyThatFieldString(params, "rawId");

        String keyId = commonVerifiers.verifyThatFieldString(params, "id");

        // Get response
        JsonNode responseNode = params.get("response");

        // Verify userHandle
        if (responseNode.hasNonNull("userHandle")) {
            // This can be null for U2F authenticators
            String userHandle = commonVerifiers.verifyThatFieldString(params.get("response"), "userHandle");
        }

        // Verify client data
        JsonNode clientDataJSONNode = commonVerifiers.verifyClientJSON(responseNode);
        commonVerifiers.verifyClientJSONTypeIsGet(clientDataJSONNode);
        
        // Get challenge
        String challenge = commonVerifiers.getChallenge(clientDataJSONNode);

        // Find authentication entry
        Fido2AuthenticationEntry authenticationEntity = authenticationPersistenceService.findByChallenge(challenge).parallelStream().findFirst()
                .orElseThrow(() -> new Fido2RPRuntimeException(String.format("Can't find associated assertion request by challenge '%s'", challenge)));
        Fido2AuthenticationData authenticationData = authenticationEntity.getAuthenticationData();

        // Verify domain
        domainVerifier.verifyDomain(authenticationData.getDomain(), clientDataJSONNode);
        
        // Find registered public key
        Fido2RegistrationEntry registrationEntry = registrationPersistenceService.findByPublicKeyId(keyId)
                .orElseThrow(() -> new Fido2RPRuntimeException(String.format("Couldn't find the key by PublicKeyId '%s'", keyId)));
        Fido2RegistrationData registrationData = registrationEntry.getRegistrationData();

        assertionVerifier.verifyAuthenticatorAssertionResponse(responseNode, registrationData, authenticationData);

        // Store original response
        authenticationData.setAssertionResponse(params.toString());

        authenticationData.setStatus(Fido2AuthenticationStatus.authenticated);

        authenticationPersistenceService.update(authenticationEntity);

        // Create result object
        ObjectNode finishResponseNode = dataMapperService.createObjectNode();

        PublicKeyCredentialDescriptor credentialDescriptor = new PublicKeyCredentialDescriptor(registrationData.getType(), registrationData.getPublicKeyId());
        finishResponseNode.set("authenticatedCredentials", dataMapperService.convertValue(credentialDescriptor, JsonNode.class));

        finishResponseNode.put("status", "ok");
        finishResponseNode.put("errorMessage", "");

        return finishResponseNode;
    }

	private ArrayNode prepareAllowedCredentials(String documentDomain, String username) {
        List<DeviceRegistration> existingFidoRegistrations = deviceRegistrationService.findAllRegisteredByUsername(username, null);
        List<JsonNode> allowedFidoKeys = existingFidoRegistrations.parallelStream()
                .filter(f -> StringHelper.equals(documentDomain, networkService.getHost(f.getApplication())))
                .filter(f -> StringHelper.isNotEmpty(f.getKeyHandle()))
                .map(f -> dataMapperService.convertValue(
                        new PublicKeyCredentialDescriptor("public-key", new String[] {"usb", "ble", "nfc"}, f.getKeyHandle()),
                        JsonNode.class))
                .collect(Collectors.toList());

        List<Fido2RegistrationEntry> existingFido2Registrations = registrationPersistenceService.findAllRegisteredByUsername(username);
        List<JsonNode> allowedFido2Keys = existingFido2Registrations.parallelStream()
                .filter(f -> StringHelper.equals(documentDomain, f.getRegistrationData().getDomain()))
                .filter(f -> StringHelper.isNotEmpty(f.getRegistrationData().getPublicKeyId()))
                .map(f -> dataMapperService.convertValue(
                        new PublicKeyCredentialDescriptor(f.getRegistrationData().getType(), new String[] {"usb", "ble", "nfc"}, f.getRegistrationData().getPublicKeyId()),
                        JsonNode.class))
                .collect(Collectors.toList());

        ArrayNode allowedCredentials = dataMapperService.createArrayNode();
        allowedCredentials.addAll(allowedFidoKeys);
        allowedCredentials.addAll(allowedFido2Keys);

        return allowedCredentials;
	}

}
