/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.operation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.jans.fido2.service.external.ExternalFido2InterceptionService;
import io.jans.fido2.service.external.context.ExternalFido2InterceptionContext;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.orm.model.fido2.Fido2RegistrationStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import io.jans.entry.DeviceRegistration;
import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.ctap.AuthenticatorAttachment;
import io.jans.fido2.ctap.UserVerification;
import io.jans.fido2.exception.Fido2CompromisedDevice;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.PublicKeyCredentialDescriptor;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.entry.Fido2AuthenticationData;
import io.jans.fido2.model.entry.Fido2AuthenticationEntry;
import io.jans.fido2.model.entry.Fido2AuthenticationStatus;
import io.jans.fido2.service.ChallengeGenerator;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.persist.AuthenticationPersistenceService;
import io.jans.fido2.service.persist.RegistrationPersistenceService;
import io.jans.fido2.service.verifier.AssertionVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.DomainVerifier;
import io.jans.service.net.NetworkService;
import io.jans.u2f.service.persist.DeviceRegistrationService;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
	private ExternalFido2InterceptionService externalFido2InterceptionService;

	/*
	 * Requires mandatory parameters: username Support non mandatory parameters:
	 * userVerification, documentDomain, extensions, timeout
	 */
	public JsonNode options(JsonNode params, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		log.debug("Assertion options {}", params);

		// Apply external custom scripts
		ExternalFido2InterceptionContext externalFido2InterceptionContext = new ExternalFido2InterceptionContext(params, httpRequest, httpResponse);
		boolean externalInterceptContext = externalFido2InterceptionService.interceptAuthenticateAssertion(params, externalFido2InterceptionContext);

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

		// TODO: Verify documentDomain

		// Put allowCredentials
		Pair<ArrayNode, String> allowedCredentialsPair = prepareAllowedCredentials(documentDomain, username);
		ArrayNode allowedCredentials = allowedCredentialsPair.getLeft();
		if (allowedCredentials.isEmpty()) {
			throw new Fido2RuntimeException("Can't find associated key(s). Username: " + username);
		}
		optionsResponseNode.set("allowCredentials", allowedCredentials);
		log.debug("Put allowedCredentials {}", allowedCredentials);

		// Put timeout
		// int timeout = commonVerifiers.verifyTimeout(params);
		// log.debug("Put timeout {}", timeout);
		// optionsResponseNode.put("timeout", timeout);

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
			} else {
				ObjectNode extensions = dataMapperService.createObjectNode();
				extensions.put("appid", fidoApplicationId);
				optionsResponseNode.set("extensions", extensions);
			}
		}

		// optionsResponseNode.put("status", "ok");
		// optionsResponseNode.put("errorMessage", "");

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

	public JsonNode verify(JsonNode params, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		log.debug("authenticateResponse {}", params);

		// Apply external custom scripts
		ExternalFido2InterceptionContext externalFido2InterceptionContext = new ExternalFido2InterceptionContext(params, httpRequest, httpResponse);
		boolean externalInterceptContext = externalFido2InterceptionService.interceptVerifyAssertion(params, externalFido2InterceptionContext);

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
		Fido2AuthenticationEntry authenticationEntity = authenticationPersistenceService.findByChallenge(challenge).parallelStream()
				.findFirst().orElseThrow(() -> new Fido2RuntimeException(
						String.format("Can't find associated assertion request by challenge '%s'", challenge)));
		Fido2AuthenticationData authenticationData = authenticationEntity.getAuthenticationData();

		// Verify domain
		domainVerifier.verifyDomain(authenticationData.getDomain(), clientDataJSONNode);

		// Find registered public key
		Fido2RegistrationEntry registrationEntry = registrationPersistenceService.findByPublicKeyId(keyId)
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

		authenticationData.setStatus(Fido2AuthenticationStatus.authenticated);

		authenticationPersistenceService.update(authenticationEntity);

		// Store actual counter value in separate attribute. Note: Fido2 not update
		// initial value in Fido2RegistrationData to minimize DB updates
		registrationEntry.setCounter(registrationData.getCounter());
		registrationPersistenceService.update(registrationEntry);

		// Create result object
		ObjectNode finishResponseNode = dataMapperService.createObjectNode();

		PublicKeyCredentialDescriptor credentialDescriptor = new PublicKeyCredentialDescriptor(registrationData.getType(),
				registrationData.getPublicKeyId());
		finishResponseNode.set("authenticatedCredentials", dataMapperService.convertValue(credentialDescriptor, JsonNode.class));

		finishResponseNode.put("status", "ok");
		finishResponseNode.put("errorMessage", "");

		return finishResponseNode;
	}

	private Pair<ArrayNode, String> prepareAllowedCredentials(String documentDomain, String username) {
		// TODO: Add property to enable/disable U2F -> Fido2 migration
		List<DeviceRegistration> existingFidoRegistrations = deviceRegistrationService.findAllRegisteredByUsername(username,
				documentDomain);
		if (existingFidoRegistrations.size() > 0) {
			deviceRegistrationService.migrateToFido2(existingFidoRegistrations, documentDomain, username);
		}

		List<Fido2RegistrationEntry> existingFido2Registrations = registrationPersistenceService.findAllRegisteredByUsername(username);
		List<Fido2RegistrationEntry> allowedFido2Registrations = existingFido2Registrations.parallelStream()
				.filter(f -> StringHelper.equals(documentDomain, f.getRegistrationData().getDomain()))
				.filter(f -> StringHelper.isNotEmpty(f.getRegistrationData().getPublicKeyId())).collect(Collectors.toList());

		allowedFido2Registrations.forEach((value) -> {
			log.debug("attestation request:" + value.getRegistrationData().getAttenstationRequest());
		});
		
		//  f.getRegistrationData().getAttenstationRequest() null check is added to maintain backward compatiblity with U2F devices when U2F devices are migrated to the FIDO2 server
		List<JsonNode> allowedFido2Keys = allowedFido2Registrations.parallelStream()
				.map(f -> dataMapperService.convertValue(new PublicKeyCredentialDescriptor(f.getRegistrationData().getType(),
						((f.getRegistrationData().getAttestationType().equalsIgnoreCase(AttestationFormat.apple.getFmt())) || ( f.getRegistrationData().getAttenstationRequest() != null && 
								f.getRegistrationData().getAttenstationRequest().contains(AuthenticatorAttachment.PLATFORM.getAttachment())))

										? new String[] { "internal" }
										: new String[] { "usb", "ble", "nfc" },
						f.getRegistrationData().getPublicKeyId()), JsonNode.class))
				.collect(Collectors.toList());

		Optional<Fido2RegistrationEntry> fidoRegistration = allowedFido2Registrations.parallelStream()
				.filter(f -> StringUtils.isNotEmpty(f.getRegistrationData().getApplicationId())).findAny();
		String applicationId = null;
		if (fidoRegistration.isPresent()) {
			applicationId = fidoRegistration.get().getRegistrationData().getApplicationId();
		}

		ArrayNode allowedCredentials = dataMapperService.createArrayNode();
		allowedCredentials.addAll(allowedFido2Keys);

		return Pair.of(allowedCredentials, applicationId);
	}

}
