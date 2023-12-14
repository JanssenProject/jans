/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.operation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.fido2.ctap.AttestationConveyancePreference;
import io.jans.fido2.ctap.AuthenticatorAttachment;
import io.jans.fido2.ctap.CoseEC2Algorithm;
import io.jans.fido2.ctap.CoseRSAAlgorithm;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.attestation.AttestationErrorResponseType;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.auth.PublicKeyCredentialDescriptor;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.RequestedParty;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.ChallengeGenerator;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.external.ExternalFido2Service;
import io.jans.fido2.service.external.context.ExternalFido2Context;
import io.jans.fido2.service.persist.RegistrationPersistenceService;
import io.jans.fido2.service.persist.UserSessionIdService;
import io.jans.fido2.service.verifier.AttestationVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.DomainVerifier;
import io.jans.orm.model.fido2.*;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Core offering by the FIDO2 server, attestation is invoked upon enrollment
 *
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class AttestationService {

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private RegistrationPersistenceService registrationPersistenceService;

	@Inject
	private AttestationVerifier attestationVerifier;

	@Inject
	private UserSessionIdService userSessionIdService;

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
    private ExternalFido2Service externalFido2InterceptionService;

	@Inject
    private ErrorResponseFactory errorResponseFactory;

	@Context
	private HttpServletRequest httpRequest;
	@Context
	private HttpServletResponse httpResponse;

    /*
     * Requires mandatory parameters: username, displayName, attestation Support non
     * mandatory parameters: authenticatorSelection, documentDomain, extensions,
     * timeout
     */
    public ObjectNode options(JsonNode params) {

        log.debug("Attestation options {}", params);

        // Apply external custom scripts
        ExternalFido2Context externalFido2InterceptionContext = new ExternalFido2Context(params, httpRequest, httpResponse);
        boolean externalInterceptContext = externalFido2InterceptionService.registerAttestationStart(params, externalFido2InterceptionContext);

        // Verify request parameters
        commonVerifiers.verifyAttestationOptions(params);

		boolean oneStep = commonVerifiers.isSuperGluuOneStepMode(params);

		// Create result object
		ObjectNode optionsResponseNode = dataMapperService.createObjectNode();

		// Put attestation
		AttestationConveyancePreference attestationConveyancePreference = commonVerifiers
				.verifyAttestationConveyanceType(params);
		optionsResponseNode.put("attestation", attestationConveyancePreference.toString());
		log.debug("Put attestation {}", attestationConveyancePreference);

		// Put authenticatorSelection
		ObjectNode authenticatorSelectionNode = prepareAuthenticatorSelection(params);
		optionsResponseNode.set("authenticatorSelection", authenticatorSelectionNode);
		log.debug("Put authenticatorSelection {}", authenticatorSelectionNode);

		// Generate and put challenge
		String challenge = challengeGenerator.getAttestationChallenge();
		optionsResponseNode.put("challenge", challenge);
		log.debug("Put challenge {}", challenge);

		// Put pubKeyCredParams
		ArrayNode credentialParametersNode = preparePublicKeyCredentialSelection();
		optionsResponseNode.set("pubKeyCredParams", credentialParametersNode);
		log.debug("Put pubKeyCredParams {}", credentialParametersNode);

		// Put RP
		String documentDomain = commonVerifiers.verifyRpDomain(params);
		ObjectNode credentialRpEntityNode = createRpDomain(documentDomain);
		if (credentialRpEntityNode != null) {
			optionsResponseNode.set("rp", credentialRpEntityNode);
			log.debug("Put rp {}", credentialRpEntityNode);
		}

		// Put user
		String userId = generateUserId();
		String username = params.get("username").asText();
		String displayName = params.get("displayName").asText();

		ObjectNode credentialUserEntityNode = createUserCredentials(userId, username, displayName);
		optionsResponseNode.set("user", credentialUserEntityNode);
		log.debug("Put user {}", credentialUserEntityNode);

		// Put excludeCredentials
		if (!oneStep) {
			ArrayNode excludedCredentials = prepareExcludeCredentials(documentDomain, username);
			optionsResponseNode.set("excludeCredentials", excludedCredentials);
			log.debug("Put excludeCredentials {}", excludedCredentials);
		}

		// Copy extensions
		if (params.hasNonNull("extensions")) {
			JsonNode extensions = params.get("extensions");
			optionsResponseNode.set("extensions", extensions);
			log.debug("Put extensions {}", extensions);
		}
		// incase of Apple's Touch ID and Window's Hello; timeout,status and error message cause a NotAllowedError on the browser, so skipping these attributes
		if (params.hasNonNull("authenticatorAttachment")) {
			if (AuthenticatorAttachment.CROSS_PLATFORM.getAttachment().equals(authenticatorSelectionNode.get("authenticatorAttachment").asText())) {
				// Put timeout
				int timeout = commonVerifiers.verifyTimeout(params);
				log.debug("Put timeout {}", timeout);
				optionsResponseNode.put("timeout", timeout);

				optionsResponseNode.put("status", "ok");
				optionsResponseNode.put("errorMessage", "");
			}
		}
		
		// Store request in DB
		Fido2RegistrationData entity = new Fido2RegistrationData();
		entity.setUsername(username);
		entity.setUserId(userId);
		entity.setChallenge(challenge);
		entity.setDomain(documentDomain);
		entity.setStatus(Fido2RegistrationStatus.pending);
		if (params.hasNonNull(CommonVerifiers.SUPER_GLUU_APP_ID)) {
			entity.setApplicationId(params.get(CommonVerifiers.SUPER_GLUU_APP_ID).asText());
		} else {
			entity.setApplicationId(documentDomain);
		}

		// Store original requests
		entity.setAttenstationRequest(params.toString());

		Fido2RegistrationEntry registrationEntry = registrationPersistenceService.buildFido2RegistrationEntry(entity, oneStep);
		if (params.hasNonNull("session_id")) {
			registrationEntry.setSessionStateId(params.get("session_id").asText());
		}

		// Set expiration
		int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration();
        registrationEntry.setExpiration(unfinishedRequestExpiration);

		registrationPersistenceService.save(registrationEntry);

		log.debug("Saved in DB");

		externalFido2InterceptionContext.addToContext(registrationEntry, null);
		externalFido2InterceptionService.registerAttestationFinish(params, externalFido2InterceptionContext);

		return optionsResponseNode;
	}

	public ObjectNode verify(JsonNode params) {
		log.debug("Attestation verify {}", params);

        // Apply external custom scripts
        ExternalFido2Context externalFido2InterceptionContext = new ExternalFido2Context(params, httpRequest, httpResponse);
        boolean externalInterceptContext = externalFido2InterceptionService.verifyAttestationStart(params, externalFido2InterceptionContext);

        boolean superGluu = commonVerifiers.hasSuperGluu(params);
        boolean oneStep = commonVerifiers.isSuperGluuOneStepMode(params);
        boolean cancelRequest = commonVerifiers.isSuperGluuCancelRequest(params);

		// Verify if there are mandatory request parameters
		commonVerifiers.verifyBasicPayload(params);
		commonVerifiers.verifyAssertionType(params, "type");

		// Get response
		JsonNode responseNode = params.get("response");

		// Verify client data
		JsonNode clientDataJSONNode = commonVerifiers.verifyClientJSON(responseNode);
		if (!superGluu) {
			commonVerifiers.verifyClientJSONTypeIsCreate(clientDataJSONNode);
		}

		// Get challenge
		String challenge = commonVerifiers.getChallenge(clientDataJSONNode);

		// Find registration entry
		Fido2RegistrationEntry registrationEntry = registrationPersistenceService.findByChallenge(challenge, oneStep)
				.parallelStream().findAny().orElseThrow(() ->
					errorResponseFactory.badRequestException(AttestationErrorResponseType.INVALID_CHALLENGE, String.format("Can't find associated attestation request by challenge '%s'", challenge)));
		Fido2RegistrationData registrationData = registrationEntry.getRegistrationData();

		// Verify domain
		domainVerifier.verifyDomain(registrationData.getDomain(), clientDataJSONNode);

		// Verify authenticator attestation response
		CredAndCounterData attestationData = attestationVerifier.verifyAuthenticatorAttestationResponse(responseNode,
				registrationData);

		registrationData.setUncompressedECPoint(attestationData.getUncompressedEcPoint());
		registrationData.setSignatureAlgorithm(attestationData.getSignatureAlgorithm());
		registrationData.setCounter(attestationData.getCounters());

		String keyId = commonVerifiers.verifyCredentialId(attestationData, params);

		registrationData.setPublicKeyId(keyId);
		registrationData.setType("public-key");
		registrationData.setAttestationType(attestationData.getAttestationType());

        // Support cancel request
        if (cancelRequest) {
        	registrationData.setStatus(Fido2RegistrationStatus.canceled);
        } else {
        	registrationData.setStatus(Fido2RegistrationStatus.registered);
        }

		// Store original response
		registrationData.setAttenstationResponse(params.toString());

		// Set actual counter value. Note: Fido2 not update initial value in
		// Fido2RegistrationData to minimize DB updates
		registrationData.setCounter(registrationEntry.getCounter());

		JsonNode responseDeviceData = responseNode.get("deviceData");
		if (responseDeviceData != null && responseDeviceData.isTextual()) {
            try {
				Fido2DeviceData deviceData = dataMapperService.readValue(
						new String(base64Service.urlDecode(responseDeviceData.asText()), StandardCharsets.UTF_8),
						Fido2DeviceData.class);
                registrationEntry.setDeviceData(deviceData);
            } catch (Exception ex) {
                throw errorResponseFactory.invalidRequest(String.format("Device data is invalid: %s", responseDeviceData), ex);
            }
        }

        registrationEntry.setPublicKeyId(registrationData.getPublicKeyId());

        int publicKeyIdHash = registrationPersistenceService.getPublicKeyIdHash(registrationData.getPublicKeyId());
        registrationEntry.setPublicKeyIdHash(publicKeyIdHash);

        // Get sessionId before cleaning it from registration entry
        String sessionStateId = registrationEntry.getSessionStateId();
        registrationEntry.setSessionStateId(null);

        // Set expiration for one_step entry
        if (oneStep) {
            int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration();
        	registrationEntry.setExpiration(unfinishedRequestExpiration);
        } else {
        	registrationEntry.clearExpiration();
        }

		registrationPersistenceService.update(registrationEntry);

		// If sessionStateId is not empty update session
        if (StringHelper.isNotEmpty(sessionStateId)) {
            log.debug("There is session id. Setting session id attributes");

            userSessionIdService.updateUserSessionIdOnFinishRequest(sessionStateId, registrationEntry.getUserInum(), registrationEntry, true, oneStep);
        }

		// Create result object
		ObjectNode finishResponseNode = dataMapperService.createObjectNode();

		PublicKeyCredentialDescriptor credentialDescriptor = new PublicKeyCredentialDescriptor(
				registrationData.getType(), registrationData.getPublicKeyId());
		finishResponseNode.set("createdCredentials",
				dataMapperService.convertValue(credentialDescriptor, JsonNode.class));

		finishResponseNode.put("status", "ok");
		finishResponseNode.put("errorMessage", "");

		externalFido2InterceptionContext.addToContext(registrationEntry, null);
		externalFido2InterceptionService.verifyAttestationFinish(params, externalFido2InterceptionContext);

		return finishResponseNode;
	}

	private ObjectNode prepareAuthenticatorSelection(JsonNode params) {

		// default is cross platform
		AuthenticatorAttachment authenticatorAttachment = AuthenticatorAttachment.CROSS_PLATFORM;
		UserVerification userVerification = UserVerification.preferred;

		Boolean requireResidentKey = false;

		if (params.hasNonNull("authenticatorSelection")) {
			log.debug("params.hasNonNull(\"authenticatorSelection\")");
			JsonNode authenticatorSelectionNodeParameter = params.get("authenticatorSelection");
			authenticatorAttachment = commonVerifiers
					.verifyAuthenticatorAttachment(authenticatorSelectionNodeParameter.get("authenticatorAttachment"));
			userVerification = commonVerifiers
					.verifyUserVerification(authenticatorSelectionNodeParameter.get("userVerification"));
			requireResidentKey = commonVerifiers
					.verifyRequireResidentKey(authenticatorSelectionNodeParameter.get("requireResidentKey"));
		}

		ObjectNode authenticatorSelectionNode = dataMapperService.createObjectNode();
		if (authenticatorAttachment != null) {
			authenticatorSelectionNode.put("authenticatorAttachment", authenticatorAttachment.getAttachment());
		}

		if (requireResidentKey != null) {
			authenticatorSelectionNode.put("requireResidentKey", requireResidentKey);
		}
		if (userVerification != null) {
			authenticatorSelectionNode.put("userVerification", userVerification.toString());
		}

		return authenticatorSelectionNode;
	}

	private ArrayNode preparePublicKeyCredentialSelection() {
		List<String> requestedCredentialTypes = appConfiguration.getFido2Configuration().getRequestedCredentialTypes();

		ArrayNode credentialParametersNode = dataMapperService.createArrayNode();
		if ((requestedCredentialTypes == null) || requestedCredentialTypes.isEmpty()) {
			// Add default requested credential types

			// FIDO2 RS256
			ObjectNode credentialParametersNodeRS256 = credentialParametersNode.addObject();
			credentialParametersNodeRS256.arrayNode().addObject();
			credentialParametersNodeRS256.put("type", "public-key");
			credentialParametersNodeRS256.put("alg", CoseRSAAlgorithm.RS256.getNumericValue());

			// FIDO2 ES256
			ObjectNode credentialParametersNodeES256 = credentialParametersNode.addObject();
			credentialParametersNodeES256.arrayNode().addObject();
			credentialParametersNodeES256.put("type", "public-key");
			credentialParametersNodeES256.put("alg", CoseEC2Algorithm.ES256.getNumericValue());
		} else {
			for (String requestedCredentialType : requestedCredentialTypes) {
				CoseRSAAlgorithm coseRSAAlgorithm = null;
				try {
					coseRSAAlgorithm = CoseRSAAlgorithm.valueOf(requestedCredentialType);
				} catch (IllegalArgumentException ex) {
				}

				if (coseRSAAlgorithm != null) {
					ObjectNode credentialParametersNodeRS256 = credentialParametersNode.addObject();
					credentialParametersNodeRS256.arrayNode().addObject();
					credentialParametersNodeRS256.put("type", "public-key");
					credentialParametersNodeRS256.put("alg", coseRSAAlgorithm.getNumericValue());
					break;
				}
			}

			for (String requestedCredentialType : requestedCredentialTypes) {
				CoseEC2Algorithm coseEC2Algorithm = null;
				try {
					coseEC2Algorithm = CoseEC2Algorithm.valueOf(requestedCredentialType);
				} catch (IllegalArgumentException ex) {
				}

				if (coseEC2Algorithm != null) {
					ObjectNode credentialParametersNodeRS256 = credentialParametersNode.addObject();
					credentialParametersNodeRS256.arrayNode().addObject();
					credentialParametersNodeRS256.put("type", "public-key");
					credentialParametersNodeRS256.put("alg", coseEC2Algorithm.getNumericValue());
					break;
				}
			}
		}

		return credentialParametersNode;
	}

	private ObjectNode createRpDomain(String documentDomain) {
		List<RequestedParty> requestedParties = appConfiguration.getFido2Configuration().getRequestedParties();

		if ((requestedParties == null) || requestedParties.isEmpty()) {
			// Add entry for default RP
			ObjectNode credentialRpEntityNode = dataMapperService.createObjectNode();
			credentialRpEntityNode.put("name", appConfiguration.getIssuer());
			credentialRpEntityNode.put("id", documentDomain);
		} else {
			for (RequestedParty requestedParty : requestedParties) {
				for (String domain : requestedParty.getDomains()) {
					if (StringHelper.equalsIgnoreCase(documentDomain, domain)) {
						// Add entry for supported RP
						ObjectNode credentialRpEntityNode = dataMapperService.createObjectNode();
						credentialRpEntityNode.put("name", requestedParty.getName());
						credentialRpEntityNode.put("id", documentDomain);

						return credentialRpEntityNode;
					}
				}
			}
		}

		return null;
	}

	public String generateUserId() {
		byte[] buffer = new byte[32];
		new SecureRandom().nextBytes(buffer);

		return base64Service.urlEncodeToString(buffer);
	}

	private ObjectNode createUserCredentials(String userId, String username, String displayName) {
		ObjectNode credentialUserEntityNode = dataMapperService.createObjectNode();
		credentialUserEntityNode.put("id", userId);
		credentialUserEntityNode.put("name", username);
		credentialUserEntityNode.put("displayName", displayName);

		return credentialUserEntityNode;
	}

	private ArrayNode prepareExcludeCredentials(String documentDomain, String username) {
		List<Fido2RegistrationEntry> existingRegistrations = registrationPersistenceService
				.findByRpRegisteredUserDevices(username, documentDomain);
		List<JsonNode> excludedKeys = existingRegistrations.parallelStream()
				.filter(f -> StringHelper.isNotEmpty(f.getRegistrationData().getPublicKeyId()))
				.map(f -> dataMapperService.convertValue(new PublicKeyCredentialDescriptor(
						f.getRegistrationData().getType(), new String[] { "usb", "ble", "nfc", "internal", "net", "qr" },
						f.getRegistrationData().getPublicKeyId()), JsonNode.class))
				.collect(Collectors.toList());

		ArrayNode excludedCredentials = dataMapperService.createArrayNode();
		excludedCredentials.addAll(excludedKeys);

		return excludedCredentials;
	}

}