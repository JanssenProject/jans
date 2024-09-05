/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.operation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.entry.DeviceRegistration;
import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.ctap.AuthenticatorAttachment;
import io.jans.fido2.exception.Fido2CompromisedDevice;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.assertion.AssertionErrorResponseType;
import io.jans.fido2.model.auth.PublicKeyCredentialDescriptor;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.ChallengeGenerator;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.external.ExternalFido2Service;
import io.jans.fido2.service.external.context.ExternalFido2Context;
import io.jans.fido2.service.persist.AuthenticationPersistenceService;
import io.jans.fido2.service.persist.RegistrationPersistenceService;
import io.jans.fido2.service.persist.UserSessionIdService;
import io.jans.fido2.service.verifier.AssertionVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.DomainVerifier;
import io.jans.orm.model.fido2.*;
import io.jans.service.net.NetworkService;
import io.jans.u2f.service.persist.DeviceRegistrationService;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Core offering by the FIDO2 server, assertion is invoked upon authentication
 *
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
	private UserSessionIdService userSessionIdService;

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


    @Inject
    private ExternalFido2Service externalFido2InterceptionService;

    @Inject
    private Base64Service base64Service;

	@Inject
    private ErrorResponseFactory errorResponseFactory;

	@Context
	private HttpServletRequest httpRequest;
	@Context
	private HttpServletResponse httpResponse;

    /*
     * Requires mandatory parameters: username Support non mandatory parameters:
     * userVerification, documentDomain, extensions, timeout
     */
    public ObjectNode options(JsonNode params) {
        log.debug("Assertion options {}", params);

        // Apply external custom scripts
        ExternalFido2Context externalFido2InterceptionContext = new ExternalFido2Context(params, httpRequest, httpResponse);
        boolean externalInterceptContext = externalFido2InterceptionService.authenticateAssertionStart(params, externalFido2InterceptionContext);

        boolean superGluu = commonVerifiers.hasSuperGluu(params);
        boolean oneStep = commonVerifiers.isSuperGluuOneStepMode(params);

		// Verify request parameters
		String username = null;
		if (!(superGluu && oneStep)) {
			commonVerifiers.verifyAssertionOptions(params);

			// Get username
			username = commonVerifiers.verifyThatFieldString(params, "username");
		}

		// Create result object
		ObjectNode optionsResponseNode = dataMapperService.createObjectNode();

		// Put userVerification
		UserVerification userVerification = commonVerifiers.prepareUserVerification(params);
		optionsResponseNode.put("userVerification", userVerification.name());

		// Generate and put challenge
		String challenge = challengeGenerator.getAssertionChallenge();
		optionsResponseNode.put("challenge", challenge);
		log.debug("Put challenge {}", challenge);

		// Put RP
		String documentDomain = commonVerifiers.verifyRpDomain(params);
		log.debug("Put rpId {}", documentDomain);
		optionsResponseNode.put("rpId", documentDomain);

		String applicationId = documentDomain;
		if (superGluu && params.hasNonNull(CommonVerifiers.SUPER_GLUU_APP_ID)) {
			applicationId = params.get(CommonVerifiers.SUPER_GLUU_APP_ID).asText();
		}

		String requestedKeyHandle = null;
		if (superGluu && params.hasNonNull(CommonVerifiers.SUPER_GLUU_KEY_HANDLE)) {
			requestedKeyHandle = params.get(CommonVerifiers.SUPER_GLUU_KEY_HANDLE).asText();
		}

		// Put allowCredentials
		Pair<ArrayNode, String> allowedCredentialsPair = prepareAllowedCredentials(applicationId, username, requestedKeyHandle, superGluu);
		ArrayNode allowedCredentials = allowedCredentialsPair.getLeft();
		if (allowedCredentials.isEmpty()) {
			throw errorResponseFactory.badRequestException(AssertionErrorResponseType.KEYS_NOT_FOUND, "Can't find associated key(s). Username: " + username);
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

		String fidoApplicationId = allowedCredentialsPair.getRight();
		if (fidoApplicationId != null) {
			if (optionsResponseNode.hasNonNull("extensions")) {
				ObjectNode extensions = (ObjectNode) optionsResponseNode.get("extensions");
				extensions.put("appid", fidoApplicationId);
//			} else {
//				ObjectNode extensions = dataMapperService.createObjectNode();
//				extensions.put("appid", fidoApplicationId);
//				optionsResponseNode.set("extensions", extensions);
			}
		}

		optionsResponseNode.put("status", "ok");
		optionsResponseNode.put("errorMessage", "");

		Fido2AuthenticationData entity = new Fido2AuthenticationData();
		entity.setUsername(username);
		entity.setChallenge(challenge);
		entity.setDomain(documentDomain);
		entity.setUserVerificationOption(userVerification);
		entity.setStatus(Fido2AuthenticationStatus.pending);
		if (params.hasNonNull(CommonVerifiers.SUPER_GLUU_APP_ID)) {
			entity.setApplicationId(params.get(CommonVerifiers.SUPER_GLUU_APP_ID).asText());
		} else {
			entity.setApplicationId(documentDomain);
		}

		// Store original request
		entity.setAssertionRequest(params.toString());

		Fido2AuthenticationEntry authenticationEntity = authenticationPersistenceService.buildFido2AuthenticationEntry(entity, oneStep);
		if (params.hasNonNull("session_id")) {
			authenticationEntity.setSessionStateId(params.get("session_id").asText());
		}

		// Set expiration
		int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration();
		authenticationEntity.setExpiration(unfinishedRequestExpiration);

		authenticationPersistenceService.save(authenticationEntity);

		externalFido2InterceptionContext.addToContext(null, authenticationEntity);
		externalFido2InterceptionService.authenticateAssertionFinish(params, externalFido2InterceptionContext);

		return optionsResponseNode;
	}

	public ObjectNode generateOptions(JsonNode params) {
        log.debug("Generate assertion options: {}", params);

		// Create result object
		ObjectNode optionsResponseNode = dataMapperService.createObjectNode();

		// Put userVerification
		UserVerification userVerification = commonVerifiers.prepareUserVerification(params);
		optionsResponseNode.put("userVerification", userVerification.name());

		// Generate and put challenge
		String challenge = challengeGenerator.getAssertionChallenge();
		optionsResponseNode.put("challenge", challenge);
		log.debug("Put challenge {}", challenge);

		// Put RP
		String documentDomain = commonVerifiers.verifyRpDomain(params);
		log.debug("Put rpId {}", documentDomain);
		optionsResponseNode.put("rpId", documentDomain);

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

		Fido2AuthenticationData entity = new Fido2AuthenticationData();
		entity.setUsername(null);
		entity.setChallenge(challenge);
		entity.setDomain(documentDomain);
		entity.setUserVerificationOption(userVerification);
		entity.setStatus(Fido2AuthenticationStatus.pending);
		entity.setApplicationId(documentDomain);

		// Store original request
		entity.setAssertionRequest(params.toString());

		Fido2AuthenticationEntry authenticationEntity = authenticationPersistenceService.buildFido2AuthenticationEntry(entity, true);
		if (params.hasNonNull("session_id")) {
			authenticationEntity.setSessionStateId(params.get("session_id").asText());
		}

		// Set expiration
		int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration();
		authenticationEntity.setExpiration(unfinishedRequestExpiration);

		authenticationPersistenceService.save(authenticationEntity);

		return optionsResponseNode;
	}

	public ObjectNode verify(JsonNode params) {
		log.debug("authenticateResponse {}", params);

        // Apply external custom scripts
        ExternalFido2Context externalFido2InterceptionContext = new ExternalFido2Context(params, httpRequest, httpResponse);
        boolean externalInterceptContext = externalFido2InterceptionService.verifyAssertionStart(params, externalFido2InterceptionContext);

        boolean superGluu = commonVerifiers.hasSuperGluu(params);
        boolean oneStep = commonVerifiers.isSuperGluuOneStepMode(params);
        boolean cancelRequest = commonVerifiers.isSuperGluuCancelRequest(params);

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
		if (!superGluu) {
			commonVerifiers.verifyClientJSONTypeIsGet(clientDataJSONNode);
		}

		// Get challenge
		String challenge = commonVerifiers.getChallenge(clientDataJSONNode);

		// Find authentication entry
		Fido2AuthenticationEntry authenticationEntity = authenticationPersistenceService.findByChallenge(challenge, oneStep).parallelStream()
				.findFirst().orElseThrow(() -> new Fido2RuntimeException(
						String.format("Can't find associated assertion request by challenge '%s'", challenge)));
		Fido2AuthenticationData authenticationData = authenticationEntity.getAuthenticationData();

		// Verify domain
		domainVerifier.verifyDomain(authenticationData.getDomain(), clientDataJSONNode);

		// Find registered public key
		Fido2RegistrationEntry registrationEntry = registrationPersistenceService.findByPublicKeyId(keyId, authenticationEntity.getRpId())
				.orElseThrow(() -> new Fido2RuntimeException(String.format("Couldn't find the key by PublicKeyId '%s'", keyId)));
		Fido2RegistrationData registrationData = registrationEntry.getRegistrationData();

		// Set actual counter value. Note: Fido2 not update initial value in
		// Fido2RegistrationData to minimize DB updates
		registrationData.setCounter(registrationEntry.getCounter());

		try {
			assertionVerifier.verifyAuthenticatorAssertionResponse(responseNode, registrationData, authenticationData);
		} catch (Fido2CompromisedDevice ex) {
			registrationData.setStatus(Fido2RegistrationStatus.compromised);
			registrationPersistenceService.update(registrationEntry);

			throw ex;
		}

		// Store original response
		authenticationData.setAssertionResponse(params.toString());

        // Support cancel request
        if (cancelRequest) {
        	authenticationData.setStatus(Fido2AuthenticationStatus.canceled);
        } else {
        	authenticationData.setStatus(Fido2AuthenticationStatus.authenticated);

        	JsonNode responseDeviceData = responseNode.get("deviceData");
    		if (responseDeviceData != null && responseDeviceData.isTextual()) {
                try {
    				Fido2DeviceData deviceData = dataMapperService.readValue(
    						new String(base64Service.urlDecode(responseDeviceData.asText()), StandardCharsets.UTF_8),
    						Fido2DeviceData.class);
    	            
    	            boolean pushTokenUpdated = !StringHelper.equals(registrationEntry.getDeviceData().getPushToken(), deviceData.getPushToken());
    	            if (pushTokenUpdated) {
    	            	prepareForPushTokenChange(registrationEntry);
    	            }
                    registrationEntry.setDeviceData(deviceData);
                } catch (Exception ex) {
                    throw errorResponseFactory.invalidRequest(String.format("Device data is invalid: %s", responseDeviceData), ex);
                }
            }
        }

		// Set expiration
		int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getAuthenticationHistoryExpiration();
		authenticationEntity.setExpiration(unfinishedRequestExpiration);

		authenticationPersistenceService.update(authenticationEntity);

		// Store actual counter value in separate attribute. Note: Fido2 not update
		// initial value in Fido2RegistrationData to minimize DB updates
		registrationEntry.setCounter(registrationData.getCounter());
		registrationPersistenceService.update(registrationEntry);

        // If SessionStateId is not empty update session
		String sessionStateId = authenticationEntity.getSessionStateId();
        if (StringHelper.isNotEmpty(sessionStateId)) {
            log.debug("There is session id. Setting session id attributes");

            userSessionIdService.updateUserSessionIdOnFinishRequest(sessionStateId, registrationEntry.getUserInum(), registrationEntry, authenticationEntity, false, oneStep);
        }

		// Create result object
		ObjectNode finishResponseNode = dataMapperService.createObjectNode();

		PublicKeyCredentialDescriptor credentialDescriptor = new PublicKeyCredentialDescriptor(registrationData.getType(),
				registrationData.getPublicKeyId());
		finishResponseNode.set("authenticatedCredentials", dataMapperService.convertValue(credentialDescriptor, JsonNode.class));

		finishResponseNode.put("status", "ok");
		finishResponseNode.put("errorMessage", "");
		finishResponseNode.put("username", registrationData.getUsername());

		externalFido2InterceptionContext.addToContext(registrationEntry, authenticationEntity);
		externalFido2InterceptionService.verifyAssertionFinish(finishResponseNode, externalFido2InterceptionContext);

		return finishResponseNode;
	}

	private void prepareForPushTokenChange(Fido2RegistrationEntry registrationEntry) {
		Fido2DeviceNotificationConf deviceNotificationConf = registrationEntry.getDeviceNotificationConf();
		if (deviceNotificationConf == null) {
			return;
		}

		String snsEndpointArn = deviceNotificationConf.getSnsEndpointArn();
		if (StringHelper.isEmpty(snsEndpointArn)) {
			return;
		}
		
		deviceNotificationConf.setSnsEndpointArn(null);
		deviceNotificationConf.setSnsEndpointArnRemove(snsEndpointArn);
		List<String> snsEndpointArnHistory = deviceNotificationConf.getSnsEndpointArnHistory();
		if (snsEndpointArnHistory == null) {
			snsEndpointArnHistory = new ArrayList<>();
			deviceNotificationConf.setSnsEndpointArnHistory(snsEndpointArnHistory);
		}
		
		snsEndpointArnHistory.add(snsEndpointArn);
	}

	private Pair<ArrayNode, String> prepareAllowedCredentials(String documentDomain, String username, String requestedKeyHandle, boolean superGluu) {
		if (appConfiguration.isOldU2fMigrationEnabled()) {
	 		List<DeviceRegistration> existingFidoRegistrations = deviceRegistrationService.findAllRegisteredByUsername(username,
					documentDomain);
			if (existingFidoRegistrations.size() > 0) {
				deviceRegistrationService.migrateToFido2(existingFidoRegistrations, documentDomain, username);
			}
		}

		List<Fido2RegistrationEntry> existingFido2Registrations;
		if (superGluu && StringHelper.isNotEmpty(requestedKeyHandle)) {
			Fido2RegistrationEntry fido2RegistrationEntry = registrationPersistenceService.findByPublicKeyId(username, requestedKeyHandle, documentDomain).orElseThrow(() -> new Fido2RuntimeException(
						String.format("Can't find associated key '%s' for application '%s'", requestedKeyHandle, documentDomain)));
			existingFido2Registrations = Arrays.asList(fido2RegistrationEntry);
		} else {
			existingFido2Registrations = registrationPersistenceService.findByRpRegisteredUserDevices(username, superGluu ? documentDomain : null);
		}
		//  f.getRegistrationData().getAttenstationRequest() null check is added to maintain backward compatiblity with U2F devices when U2F devices are migrated to the FIDO2 server
		List<Fido2RegistrationEntry> allowedFido2Registrations = existingFido2Registrations.parallelStream()
				.filter(f -> StringHelper.isNotEmpty(f.getRegistrationData().getPublicKeyId())).collect(Collectors.toList());

		List<JsonNode> allowedFido2Keys =  new ArrayList<>(allowedFido2Registrations.size());
		allowedFido2Registrations.forEach((f) -> {
			log.debug("attestation request:" + f.getRegistrationData().getAttenstationRequest());
			String transports[];
			if (superGluu) {
				transports = new String[] { "net", "qr" };
			} else {
				transports = ((f.getRegistrationData().getAttestationType().equalsIgnoreCase(AttestationFormat.apple.getFmt())) || ( f.getRegistrationData().getAttenstationRequest() != null &&
						f.getRegistrationData().getAttenstationRequest().contains(AuthenticatorAttachment.PLATFORM.getAttachment())))

						? new String[] { "internal" }
						: new String[] { "usb", "ble", "nfc" };
			}
			PublicKeyCredentialDescriptor descriptor = new PublicKeyCredentialDescriptor(
					f.getRegistrationData().getType(), transports, f.getRegistrationData().getPublicKeyId());

			ObjectNode allowedFido2Key = dataMapperService.convertValue(descriptor, ObjectNode.class);
			allowedFido2Keys.add(allowedFido2Key);
		});

		Optional<Fido2RegistrationEntry> fidoRegistration = allowedFido2Registrations.parallelStream()
				.filter(f -> StringUtils.isNotEmpty(f.getRegistrationData().getApplicationId())).findAny();
		String applicationId = null;
		// applicationId should not be sent incase of pure fido2
		if (fidoRegistration.isPresent() && superGluu) {
			applicationId = fidoRegistration.get().getRegistrationData().getApplicationId();
		}

		ArrayNode allowedCredentials = dataMapperService.createArrayNode();
		allowedCredentials.addAll(allowedFido2Keys);

		return Pair.of(allowedCredentials, applicationId);
	}

}
