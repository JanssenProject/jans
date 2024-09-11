/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.operation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import io.jans.entry.DeviceRegistration;
import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.ctap.AuthenticatorAttachment;
import io.jans.fido2.exception.Fido2CompromisedDevice;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.assertion.*;
import io.jans.fido2.model.common.AttestationOrAssertionResponse;
import io.jans.fido2.model.common.PublicKeyCredentialDescriptor;
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
import io.jans.fido2.service.util.CommonUtilService;
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
    public AssertionOptionsResponse options(AssertionOptions assertionOptions) {
        log.debug("Assertion options {}", CommonUtilService.toJsonNode(assertionOptions));

        // Apply external custom scripts
        ExternalFido2Context externalFido2InterceptionContext = new ExternalFido2Context(CommonUtilService.toJsonNode(assertionOptions), httpRequest, httpResponse);
        boolean externalInterceptContext = externalFido2InterceptionService.authenticateAssertionStart(CommonUtilService.toJsonNode(assertionOptions), externalFido2InterceptionContext);


		// Verify request parameters
		String username = assertionOptions.getUsername();//commonVerifiers.verifyThatFieldString(params, "username");
		

		// Create result object
		//ObjectNode optionsResponseNode = dataMapperService.createObjectNode();
		AssertionOptionsResponse assertionOptionsResponse = new AssertionOptionsResponse();

		// Put userVerification
		UserVerification userVerification = commonVerifiers.prepareUserVerification(assertionOptions.getUserVerification());
		assertionOptionsResponse.setUserVerification(userVerification.name());

		// Generate and put challenge
		String challenge = challengeGenerator.getAssertionChallenge();
		assertionOptionsResponse.setChallenge(challenge);
		log.debug("Put challenge {}", challenge);

		// Put RP
		String documentDomain = commonVerifiers.verifyRpDomain(assertionOptions.getDocumentDomain(),appConfiguration.getIssuer());
		assertionOptionsResponse.setRpId(documentDomain);
		log.debug("Put rpId {}", documentDomain);


		String applicationId = documentDomain;
		
		// Put allowCredentials
		Pair<List<PublicKeyCredentialDescriptor>, String> allowedCredentialsPair = prepareAllowedCredentials(applicationId, username);
		List<PublicKeyCredentialDescriptor> allowedCredentials = allowedCredentialsPair.getLeft();
		if (allowedCredentials.isEmpty()) {
			throw errorResponseFactory.badRequestException(AssertionErrorResponseType.KEYS_NOT_FOUND, "Can't find associated key(s). Username: " + username);
		}
		assertionOptionsResponse.setAllowCredentials(allowedCredentials);
		allowedCredentials.stream().forEach(ele -> log.debug("Put allowedCredentials {}", ele.toString()));
		log.debug("Put allowedCredentials {}", allowedCredentials);

		// Put timeout
		long timeout = commonVerifiers.verifyTimeout(assertionOptions.getTimeout());
		assertionOptionsResponse.setTimeout(timeout);
		log.debug("Put timeout {}", timeout);

		// Copy extensions
		if (assertionOptions.getExtensions() != null) {
			assertionOptionsResponse.setExtensions(assertionOptions.getExtensions());
			log.debug("Put extensions {}", assertionOptions.getExtensions());
		}

		String fidoApplicationId = allowedCredentialsPair.getRight();
		if (fidoApplicationId != null) {
			if (assertionOptions.getExtensions() != null) {
				ObjectNode extensions = (ObjectNode) assertionOptions.getExtensions();
				extensions.put("appid", fidoApplicationId);
//			} else {
//				ObjectNode extensions = dataMapperService.createObjectNode();
//				extensions.put("appid", fidoApplicationId);
//				optionsResponseNode.set("extensions", extensions);
			}
		}
		assertionOptionsResponse.setStatus("ok");
		assertionOptionsResponse.setErrorMessage("");

		Fido2AuthenticationData entity = new Fido2AuthenticationData();
		entity.setUsername(username);
		entity.setChallenge(challenge);
		entity.setDomain(documentDomain);
		entity.setUserVerificationOption(userVerification);
		entity.setStatus(Fido2AuthenticationStatus.pending);
		entity.setApplicationId(documentDomain);
		

		// Store original request
		entity.setAssertionRequest(CommonUtilService.toJsonNode(assertionOptions).toString());

		Fido2AuthenticationEntry authenticationEntity = authenticationPersistenceService.buildFido2AuthenticationEntry(entity);
		if (!Strings.isNullOrEmpty(assertionOptions.getSessionId())) {
			authenticationEntity.setSessionStateId(assertionOptions.getSessionId());
		}

		// Set expiration
		int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration();
		authenticationEntity.setExpiration(unfinishedRequestExpiration);

		authenticationPersistenceService.save(authenticationEntity);

		externalFido2InterceptionContext.addToContext(null, authenticationEntity);
		externalFido2InterceptionService.authenticateAssertionFinish(CommonUtilService.toJsonNode(assertionOptions), externalFido2InterceptionContext);

		return assertionOptionsResponse;
	}

	public AsserOptGenerateResponse generateOptions(AssertionOptionsGenerate assertionOptionsGenerate) throws JsonProcessingException {
        log.debug("Generate assertion options: {}", CommonUtilService.toJsonNode(assertionOptionsGenerate));

		// Create result object
		AsserOptGenerateResponse asserOptGenerateResponse = new AsserOptGenerateResponse();
		//ObjectNode optionsResponseNode = dataMapperService.createObjectNode();

		// Put userVerification
		UserVerification userVerification = commonVerifiers.prepareUserVerification(assertionOptionsGenerate.getUserVerification());
		asserOptGenerateResponse.setUserVerification(userVerification.name());

		// Generate and put challenge
		String challenge = challengeGenerator.getAssertionChallenge();
		asserOptGenerateResponse.setChallenge(challenge);
		log.debug("Put challenge {}", challenge);

		// Put RP
		String documentDomain = commonVerifiers.verifyRpDomain(assertionOptionsGenerate.getDocumentDomain(), appConfiguration.getIssuer());
		asserOptGenerateResponse.setRpId(documentDomain);
		log.debug("Put rpId {}", documentDomain);

		// Put timeout
		long timeout = commonVerifiers.verifyTimeout(assertionOptionsGenerate.getTimeout());
		asserOptGenerateResponse.setTimeout(timeout);
		log.debug("Put timeout {}", timeout);

		// Copy extensions
		if (assertionOptionsGenerate.getExtensions() != null) {
			JsonNode extensions = assertionOptionsGenerate.getExtensions();
			asserOptGenerateResponse.setExtensions(extensions);
			log.debug("Put extensions {}", extensions);
		}
		asserOptGenerateResponse.setStatus("ok");

		Fido2AuthenticationData entity = new Fido2AuthenticationData();
		entity.setUsername(null);
		entity.setChallenge(challenge);
		entity.setDomain(documentDomain);
		entity.setUserVerificationOption(userVerification);
		entity.setStatus(Fido2AuthenticationStatus.pending);
		entity.setApplicationId(documentDomain);

		// Store original request
		entity.setAssertionRequest(CommonUtilService.toJsonNode(assertionOptionsGenerate).toString());

		Fido2AuthenticationEntry authenticationEntity = authenticationPersistenceService.buildFido2AuthenticationEntry(entity);
		if (!Strings.isNullOrEmpty(assertionOptionsGenerate.getSessionId())) {
			authenticationEntity.setSessionStateId(assertionOptionsGenerate.getSessionId());
		}

		// Set expiration
		int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration();
		authenticationEntity.setExpiration(unfinishedRequestExpiration);

		authenticationPersistenceService.save(authenticationEntity);

		return asserOptGenerateResponse;
	}

	public AttestationOrAssertionResponse verify(AssertionResult assertionResult) {
		log.debug("authenticateResponse {}", CommonUtilService.toJsonNode(assertionResult));

        // Apply external custom scripts
        ExternalFido2Context externalFido2InterceptionContext = new ExternalFido2Context(CommonUtilService.toJsonNode(assertionResult), httpRequest, httpResponse);
        boolean externalInterceptContext = externalFido2InterceptionService.verifyAssertionStart(CommonUtilService.toJsonNode(assertionResult), externalFido2InterceptionContext);


		// Verify if there are mandatory request parameters
		commonVerifiers.verifyBasicPayload(assertionResult);
		commonVerifiers.verifyAssertionType(assertionResult.getType());
		commonVerifiers.verifyNullOrEmptyString(assertionResult.getRawId());

		String keyId = commonVerifiers.verifyNullOrEmptyString(assertionResult.getId());

		// Get response
		Response response = assertionResult.getResponse();
		if(response == null) {
			throw errorResponseFactory.invalidRequest("The response parameter is null.");
		}
		// Verify client data
		JsonNode clientJsonNode = commonVerifiers.verifyClientJSON(response.getClientDataJSON());
		

		// Get challenge
		String challenge = commonVerifiers.getChallenge(clientJsonNode);

		// Find authentication entry
		Fido2AuthenticationEntry authenticationEntity = authenticationPersistenceService.findByChallenge(challenge).parallelStream()
				.findFirst().orElseThrow(() -> new Fido2RuntimeException(
						String.format("Can't find associated assertion request by challenge '%s'", challenge)));
		Fido2AuthenticationData authenticationData = authenticationEntity.getAuthenticationData();

		// Verify domain
		domainVerifier.verifyDomain(authenticationData.getDomain(), clientJsonNode);

		// Find registered public key
		Fido2RegistrationEntry registrationEntry = registrationPersistenceService.findByPublicKeyId(keyId, authenticationEntity.getRpId())
				.orElseThrow(() -> new Fido2RuntimeException(String.format("Couldn't find the key by PublicKeyId '%s'", keyId)));
		Fido2RegistrationData registrationData = registrationEntry.getRegistrationData();

		// Set actual counter value. Note: Fido2 not update initial value in
		// Fido2RegistrationData to minimize DB updates
		registrationData.setCounter(registrationEntry.getCounter());

		try {
			assertionVerifier.verifyAuthenticatorAssertionResponse(response, registrationData, authenticationData);
		} catch (Fido2CompromisedDevice ex) {
			registrationData.setStatus(Fido2RegistrationStatus.compromised);
			registrationPersistenceService.update(registrationEntry);

			throw ex;
		}

		// Store original response
		authenticationData.setAssertionResponse(CommonUtilService.toJsonNode(assertionResult).toString());

		authenticationData.setStatus(Fido2AuthenticationStatus.authenticated);

		//TODO: CHeck with Yuriy Ack if this should be here
		String deviceDataStr = response.getDeviceData();
		if (!Strings.isNullOrEmpty(deviceDataStr)) {
			try {
				Fido2DeviceData deviceData = dataMapperService.readValue(
						new String(base64Service.urlDecode(deviceDataStr), StandardCharsets.UTF_8),
						Fido2DeviceData.class);

				boolean pushTokenUpdated = !StringHelper.equals(registrationEntry.getDeviceData().getPushToken(),
						deviceData.getPushToken());
				if (pushTokenUpdated) {
					prepareForPushTokenChange(registrationEntry);
				}
				registrationEntry.setDeviceData(deviceData);
			} catch (Exception ex) {
				throw errorResponseFactory.invalidRequest(String.format("Device data is invalid: %s", deviceDataStr),
						ex);
			}
		}

		// Set expiration
		int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getMetadataRefreshInterval();
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

            userSessionIdService.updateUserSessionIdOnFinishRequest(sessionStateId, registrationEntry.getUserInum(), registrationEntry, authenticationEntity, false);
        }

		// Create result object
        AttestationOrAssertionResponse assertionResultResponse = new AttestationOrAssertionResponse();

		PublicKeyCredentialDescriptor credentialDescriptor = new PublicKeyCredentialDescriptor(registrationData.getPublicKeyId());
		assertionResultResponse.setCredentials(credentialDescriptor);
		assertionResultResponse.setStatus("ok");
		assertionResultResponse.setErrorMessage("");
		assertionResultResponse.setUsername(registrationData.getUsername());

		externalFido2InterceptionContext.addToContext(registrationEntry, authenticationEntity);
		externalFido2InterceptionService.verifyAssertionFinish(CommonUtilService.toJsonNode(assertionResultResponse), externalFido2InterceptionContext);

		return assertionResultResponse;
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

	private Pair<List<PublicKeyCredentialDescriptor>, String> prepareAllowedCredentials(String documentDomain,
			String username) {
		if (appConfiguration.isOldU2fMigrationEnabled()) {
			List<DeviceRegistration> existingFidoRegistrations = deviceRegistrationService
					.findAllRegisteredByUsername(username, documentDomain);
			if (existingFidoRegistrations.size() > 0) {
				deviceRegistrationService.migrateToFido2(existingFidoRegistrations, documentDomain, username);
			}
		}

		List<Fido2RegistrationEntry> existingFido2Registrations;

		// TODO: incase of a bug, this the second argument should have been null, see
		// old code to understand
		existingFido2Registrations = registrationPersistenceService.findByRpRegisteredUserDevices(username,
				documentDomain);

		// f.getRegistrationData().getAttenstationRequest() null check is added to
		// maintain backward compatiblity with U2F devices when U2F devices are migrated
		// to the FIDO2 server
		List<Fido2RegistrationEntry> allowedFido2Registrations = existingFido2Registrations.parallelStream()
				.filter(f -> StringHelper.isNotEmpty(f.getRegistrationData().getPublicKeyId()))
				.collect(Collectors.toList());

		List<PublicKeyCredentialDescriptor> allowedFido2Keys = new ArrayList<>(allowedFido2Registrations.size());
		allowedFido2Registrations.forEach((f) -> {
			log.debug("attestation request:" + f.getRegistrationData().getAttestationRequest());
			String transports[];

			transports = ((f.getRegistrationData().getAttestationType()
					.equalsIgnoreCase(AttestationFormat.apple.getFmt()))
					|| (f.getRegistrationData().getAttestationRequest() != null && f.getRegistrationData()
							.getAttestationRequest().contains(AuthenticatorAttachment.PLATFORM.getAttachment())))

									? new String[] { "internal" }
									: new String[] { "usb", "ble", "nfc" };

			PublicKeyCredentialDescriptor descriptor = new PublicKeyCredentialDescriptor(transports,
					f.getRegistrationData().getPublicKeyId());

			allowedFido2Keys.add(descriptor);
		});

		Optional<Fido2RegistrationEntry> fidoRegistration = allowedFido2Registrations.parallelStream()
				.filter(f -> StringUtils.isNotEmpty(f.getRegistrationData().getApplicationId())).findAny();
		String applicationId = null;

		// applicationId should not be sent incase of pure fido2
		applicationId = fidoRegistration.get().getRegistrationData().getApplicationId();

		return Pair.of(allowedFido2Keys, applicationId);
	}

}
