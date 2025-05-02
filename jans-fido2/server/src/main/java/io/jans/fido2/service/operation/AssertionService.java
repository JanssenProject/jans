/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import io.jans.fido2.exception.Fido2CompromisedDevice;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.assertion.AsserOptGenerateResponse;
import io.jans.fido2.model.assertion.AssertionErrorResponseType;
import io.jans.fido2.model.assertion.AssertionOptions;
import io.jans.fido2.model.assertion.AssertionOptionsGenerate;
import io.jans.fido2.model.assertion.AssertionOptionsResponse;
import io.jans.fido2.model.assertion.AssertionResult;
import io.jans.fido2.model.assertion.Response;
import io.jans.fido2.model.common.AttestationOrAssertionResponse;
import io.jans.fido2.model.common.PublicKeyCredentialDescriptor;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.ChallengeGenerator;
import io.jans.fido2.service.external.ExternalFido2Service;
import io.jans.fido2.service.external.context.ExternalFido2Context;
import io.jans.fido2.service.persist.AuthenticationPersistenceService;
import io.jans.fido2.service.persist.RegistrationPersistenceService;
import io.jans.fido2.service.persist.UserSessionIdService;
import io.jans.fido2.service.util.CommonUtilService;
import io.jans.fido2.service.verifier.AssertionVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.DomainVerifier;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2AuthenticationEntry;
import io.jans.orm.model.fido2.Fido2AuthenticationStatus;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.orm.model.fido2.Fido2RegistrationStatus;
import io.jans.orm.model.fido2.UserVerification;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;

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
	private UserSessionIdService userSessionIdService;

	@Inject
	private AssertionVerifier assertionVerifier;

	@Inject
	private ChallengeGenerator challengeGenerator;

	@Inject
	private CommonVerifiers commonVerifiers;

	@Inject
	private ExternalFido2Service externalFido2InterceptionService;

	@Inject
	private ErrorResponseFactory errorResponseFactory;

	@Context
	private HttpServletRequest httpRequest;
	@Context
	private HttpServletResponse httpResponse;

	/*
	 * Requires mandatory parameters: username Support non mandatory parameters:
	 * userVerification, origin, extensions, timeout
	 */
	public AssertionOptionsResponse options(AssertionOptions assertionOptions) {
		log.debug("Assertion options {}", CommonUtilService.toJsonNode(assertionOptions));

		// Apply external custom scripts
		ExternalFido2Context externalFido2InterceptionContext = new ExternalFido2Context(
				CommonUtilService.toJsonNode(assertionOptions), httpRequest, httpResponse);
		boolean externalInterceptContext = externalFido2InterceptionService.authenticateAssertionStart(
				CommonUtilService.toJsonNode(assertionOptions), externalFido2InterceptionContext);

		// Verify request parameters
		String username = assertionOptions.getUsername();// commonVerifiers.verifyThatFieldString(params, "username");

		// Create result object
		// ObjectNode optionsResponseNode = dataMapperService.createObjectNode();
		AssertionOptionsResponse assertionOptionsResponse = new AssertionOptionsResponse();

		// Put userVerification
		UserVerification userVerification = commonVerifiers
				.prepareUserVerification(assertionOptions.getUserVerification());
		assertionOptionsResponse.setUserVerification(userVerification.name());

		// Generate and put challenge
		String challenge = challengeGenerator.getAssertionChallenge();
		assertionOptionsResponse.setChallenge(challenge);
		log.debug("Put challenge {}", challenge);

		// Put RP
		String origin = commonVerifiers.verifyRpDomain(assertionOptions.getRpId(), appConfiguration.getIssuer(),
				appConfiguration.getFido2Configuration().getRequestedParties());
		assertionOptionsResponse.setRpId(origin);
		log.debug("Put rpId {}", origin);

		// Put allowCredentials
		if (username != null && StringHelper.isNotEmpty(username)) {
			Pair<List<PublicKeyCredentialDescriptor>, String> allowedCredentialsPair = prepareAllowedCredentials(origin,
					username);
			List<PublicKeyCredentialDescriptor> allowedCredentials = allowedCredentialsPair.getLeft();
			if (allowedCredentials.isEmpty()) {
				throw errorResponseFactory.badRequestException(AssertionErrorResponseType.KEYS_NOT_FOUND,
						"Can't find associated key(s). Username: " + username);
			}
			assertionOptionsResponse.setAllowCredentials(allowedCredentials);
			allowedCredentials.stream().forEach(ele -> log.debug("Put allowedCredentials {}", ele.toString()));
			log.debug("Put allowedCredentials {}", allowedCredentials);

		} else
		// Conditional UI
		{
			assertionOptionsResponse.setAllowCredentials(assertionOptions.getAllowCredentials());
		}

		// in case of conditional UI, timeout has to be large.
		if (username != null && StringHelper.isNotEmpty(username)) {
			// Put timeout
			long timeout = commonVerifiers.verifyTimeout(assertionOptions.getTimeout());
			assertionOptionsResponse.setTimeout(timeout);
			log.debug("Put timeout {}", timeout);
		}
		/*
		 * else { assertionOptionsResponse.setTimeout(60000l); }
		 */
		// Copy extensions
		if (assertionOptions.getExtensions() != null) {
			assertionOptionsResponse.setExtensions(assertionOptions.getExtensions());
			log.debug("Put extensions {}", assertionOptions.getExtensions());
		}

		Fido2AuthenticationData entity = new Fido2AuthenticationData();
		entity.setUsername(username);
		entity.setChallenge(challenge);
		entity.setOrigin(origin);
		entity.setUserVerificationOption(userVerification);
		entity.setStatus(Fido2AuthenticationStatus.pending);
		entity.setRpId(origin);
		entity.setCredId(assertionOptions.getCredentialId());

		// Store original request
		entity.setAssertionRequest(CommonUtilService.toJsonNode(assertionOptions).toString());

		Fido2AuthenticationEntry authenticationEntity = authenticationPersistenceService
				.buildFido2AuthenticationEntry(entity);
		if (!Strings.isNullOrEmpty(assertionOptions.getSessionId())) {
			authenticationEntity.setSessionStateId(assertionOptions.getSessionId());
		}

		// Set expiration
		int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration();
		authenticationEntity.setExpiration(unfinishedRequestExpiration);

		authenticationPersistenceService.save(authenticationEntity);

		externalFido2InterceptionContext.addToContext(null, authenticationEntity);
		externalFido2InterceptionService.authenticateAssertionFinish(CommonUtilService.toJsonNode(assertionOptions),
				externalFido2InterceptionContext);

		log.debug("assertionOptionsResponse :" + assertionOptionsResponse);
		return assertionOptionsResponse;
	}

	public AsserOptGenerateResponse generateOptions(AssertionOptionsGenerate assertionOptionsGenerate)
			throws JsonProcessingException {
		log.debug("Generate assertion options: {}", CommonUtilService.toJsonNode(assertionOptionsGenerate));

		// Create result object
		AsserOptGenerateResponse asserOptGenerateResponse = new AsserOptGenerateResponse();

		// Put userVerification
		UserVerification userVerification = commonVerifiers
				.prepareUserVerification(assertionOptionsGenerate.getUserVerification());
		asserOptGenerateResponse.setUserVerification(userVerification.name());

		// Generate and put challenge
		String challenge = challengeGenerator.getAssertionChallenge();
		asserOptGenerateResponse.setChallenge(challenge);
		log.debug("Put challenge {}", challenge);

		// Put RP
		String origin = commonVerifiers.verifyRpDomain(assertionOptionsGenerate.getOrigin(),
				appConfiguration.getIssuer(), appConfiguration.getFido2Configuration().getRequestedParties());
		asserOptGenerateResponse.setRpId(origin);
		log.debug("Put rpId {}", origin);

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
		entity.setOrigin(origin);
		entity.setUserVerificationOption(userVerification);
		entity.setStatus(Fido2AuthenticationStatus.pending);
		entity.setRpId(origin);

		// Store original request
		entity.setAssertionRequest(CommonUtilService.toJsonNode(assertionOptionsGenerate).toString());

		Fido2AuthenticationEntry authenticationEntity = authenticationPersistenceService
				.buildFido2AuthenticationEntry(entity);
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
		log.debug("authenticateResponse verify {}", CommonUtilService.toJsonNode(assertionResult));

		// Apply external custom scripts
		ExternalFido2Context externalFido2InterceptionContext = new ExternalFido2Context(
				CommonUtilService.toJsonNode(assertionResult), httpRequest, httpResponse);
		boolean externalInterceptContext = externalFido2InterceptionService
				.verifyAssertionStart(CommonUtilService.toJsonNode(assertionResult), externalFido2InterceptionContext);

		// Verify if there are mandatory request parameters
		commonVerifiers.verifyBasicPayload(assertionResult);
		commonVerifiers.verifyAssertionType(assertionResult.getType());
		commonVerifiers.verifyNullOrEmptyString(assertionResult.getRawId());

		String keyId = commonVerifiers.verifyNullOrEmptyString(assertionResult.getId());

		// Get response
		Response response = assertionResult.getResponse();
		if (response == null) {
			throw errorResponseFactory.invalidRequest("The response parameter is null.");
		}
		// Verify client data
		JsonNode clientJsonNode = commonVerifiers.verifyClientJSON(response.getClientDataJSON());

		// Get challenge
		String challenge = commonVerifiers.getChallenge(clientJsonNode);

		// Find authentication entry
		Fido2AuthenticationEntry authenticationEntity = authenticationPersistenceService.findByChallenge(challenge)
				.parallelStream().findFirst().orElseThrow(() -> new Fido2RuntimeException(
						String.format("Can't find associated assertion request by challenge '%s'", challenge)));
		Fido2AuthenticationData authenticationData = authenticationEntity.getAuthenticationData();
		log.debug("Fido2AuthenticationData: " + authenticationData.toString());
		// Verify domain
		domainVerifier.verifyDomain(authenticationData.getOrigin(), clientJsonNode);

		// Find registered public key
		Fido2RegistrationEntry registrationEntry = registrationPersistenceService
				.findByPublicKeyId(keyId, authenticationEntity.getRpId()).orElseThrow(() -> new Fido2RuntimeException(
						String.format("Couldn't find the key by PublicKeyId '%s'", keyId)));
		Fido2RegistrationData registrationData = registrationEntry.getRegistrationData();
		log.debug("Fido2RegistrationEntry" + registrationEntry);
		log.debug("registrationData" + registrationData);

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

		// Set expiration
		int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getMetadataRefreshInterval();
		authenticationEntity.setExpiration(unfinishedRequestExpiration);

		authenticationPersistenceService.update(authenticationEntity);

		// Store actual counter value in separate attribute. Note: Fido2 not update
		// initial value in Fido2RegistrationData to minimize DB updates
		registrationEntry.setCounter(registrationData.getCounter());
		registrationPersistenceService.update(registrationEntry);

		log.debug("registrationEntry.getUserInum() : " + registrationEntry.getUserInum());
		// If SessionStateId is not empty update session
		String sessionStateId = authenticationEntity.getSessionStateId();
		if (StringHelper.isNotEmpty(sessionStateId)) {
			log.debug("There is session id. Setting session id attributes");

			userSessionIdService.updateUserSessionIdOnFinishRequest(sessionStateId, registrationEntry.getUserInum(),
					registrationEntry, authenticationEntity, false);
		}

		PublicKeyCredentialDescriptor credentialDescriptor = new PublicKeyCredentialDescriptor();
		credentialDescriptor.setTransports(registrationData.getTransports());
		credentialDescriptor.setId(registrationData.getPublicKeyId());
		credentialDescriptor.setType("public-key");

		AttestationOrAssertionResponse assertionResultResponse = new AttestationOrAssertionResponse(
				credentialDescriptor, "ok", "", registrationData.getUsername(),
				registrationData.getAuthentictatorAttachment(),
				String.valueOf(registrationData.isUserPresentFlag()), true, registrationData.getBackupStateFlag(),
				registrationData.getBackupEligibilityFlag(), registrationData.getType(), true, "level",
				"aaguid", "authenticatorName", registrationData.getOrigin(), "hint", registrationData.getChallenge(),
				registrationData.getRpId(), null, Long.valueOf(9000), null);

		externalFido2InterceptionContext.addToContext(registrationEntry, authenticationEntity);
		externalFido2InterceptionService.verifyAssertionFinish(CommonUtilService.toJsonNode(assertionResultResponse),
				externalFido2InterceptionContext);

		return assertionResultResponse;
	}

	private Pair<List<PublicKeyCredentialDescriptor>, String> prepareAllowedCredentials(String origin,
			String username) {

		List<Fido2RegistrationEntry> existingFido2Registrations;

		// TODO: incase of a bug, this the second argument should have been null, see
		// old code to understand
		existingFido2Registrations = registrationPersistenceService.findByRpRegisteredUserDevices(username, origin);

		// f.getRegistrationData().getAttenstationRequest() null check is added to
		// maintain backward compatiblity with U2F devices when U2F devices are migrated
		// to the FIDO2 server
		List<Fido2RegistrationEntry> allowedFido2Registrations = existingFido2Registrations.parallelStream()
				.filter(f -> StringHelper.isNotEmpty(f.getRegistrationData().getPublicKeyId()))
				.collect(Collectors.toList());

		List<PublicKeyCredentialDescriptor> allowedFido2Keys = new ArrayList<>(allowedFido2Registrations.size());
		allowedFido2Registrations.forEach((f) -> {
			log.debug("attestation request:" + f.getRegistrationData().getAttestationRequest());

			PublicKeyCredentialDescriptor descriptor = new PublicKeyCredentialDescriptor();
			descriptor.setTransports(f.getRegistrationData().getTransports());
			descriptor.setId(f.getRegistrationData().getPublicKeyId());
			descriptor.setType("public-key");

			allowedFido2Keys.add(descriptor);
		});

		Optional<Fido2RegistrationEntry> fidoRegistration = allowedFido2Registrations.parallelStream()
				.filter(f -> StringUtils.isNotEmpty(f.getRegistrationData().getRpId())).findAny();
		String applicationId = null;

		// applicationId should not be sent incase of pure fido2
		applicationId = fidoRegistration.get().getRegistrationData().getRpId();

		return Pair.of(allowedFido2Keys, applicationId);
	}

}
