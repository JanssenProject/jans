/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.operation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import io.jans.fido2.ctap.AttestationConveyancePreference;
import io.jans.fido2.ctap.AuthenticatorAttachment;
import io.jans.fido2.ctap.CoseEC2Algorithm;
import io.jans.fido2.ctap.CoseRSAAlgorithm;
import io.jans.fido2.ctap.CoseEdDSAAlgorithm;
import io.jans.fido2.model.attestation.*;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.common.*;
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
import io.jans.fido2.service.util.CommonUtilService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    public PublicKeyCredentialCreationOptions options(AttestationOptions attestationOptions) {

        log.debug("Attestation options {}", CommonUtilService.toJsonNode(attestationOptions).toString());

        // Apply external custom scripts
        ExternalFido2Context externalFido2InterceptionContext = new ExternalFido2Context(CommonUtilService.toJsonNode(attestationOptions), httpRequest, httpResponse);
        boolean externalInterceptContext = externalFido2InterceptionService.registerAttestationStart(CommonUtilService.toJsonNode(attestationOptions), externalFido2InterceptionContext);

        // Verify request parameters
        commonVerifiers.verifyAttestationOptions(attestationOptions);

		boolean oneStep = commonVerifiers.isSuperGluuOneStepMode(CommonUtilService.toJsonNode(attestationOptions));

		// Create result object
		PublicKeyCredentialCreationOptions credentialCreationOptions = new PublicKeyCredentialCreationOptions();
		// Put attestation
		AttestationConveyancePreference attestationConveyancePreference = commonVerifiers.verifyAttestationConveyanceType(attestationOptions);
		credentialCreationOptions.setAttestation(attestationConveyancePreference);
		log.debug("Put attestation {}", attestationConveyancePreference);

		// Put authenticatorSelection
		prepareAuthenticatorSelection(credentialCreationOptions, attestationOptions);
		log.debug("Put authenticatorSelection {}", credentialCreationOptions.getAuthenticatorSelection());

		// Generate and put challenge
		String challenge = challengeGenerator.getAttestationChallenge();
		credentialCreationOptions.setChallenge(challenge);
		log.debug("Put challenge {}", challenge);

		// Put pubKeyCredParams
		Set<PublicKeyCredentialParameters> pubKeyCredParams = preparePublicKeyCredentialSelection();
		credentialCreationOptions.setPubKeyCredParams(pubKeyCredParams);
		pubKeyCredParams.stream().forEach(ele -> log.debug("Put pubKeyCredParam {}", ele.toString()));

		// Put RP
		String documentDomain = commonVerifiers.verifyRpDomain(attestationOptions.getDocumentDomain());
		RelyingParty relyingParty = createRpDomain(documentDomain);
		if (relyingParty != null) {
			credentialCreationOptions.setRp(relyingParty);
			log.debug("Put rp {}", relyingParty.toString());
		}

		// Put user
		String userId = generateUserId();
		User user = User.createUser(userId, attestationOptions.getUsername(), attestationOptions.getDisplayName());
		credentialCreationOptions.setUser(user);
		log.debug("Put user {}", user.toString());

		// Put excludeCredentials
		if (!oneStep) {
			Set<PublicKeyCredentialDescriptor> excludedCredentials = prepareExcludeCredentials(documentDomain, attestationOptions.getUsername());
			credentialCreationOptions.setExcludeCredentials(excludedCredentials);
			excludedCredentials.stream().forEach(ele -> log.debug("Put excludeCredentials {}", ele.toString()));
		}

		// Copy extensions
		if (attestationOptions.getExtensions() != null) {
			credentialCreationOptions.setExtensions(attestationOptions.getExtensions());

			log.debug("Put extensions {}", attestationOptions.getExtensions());
		}
		// incase of Apple's Touch ID and Window's Hello; timeout,status and error message cause a NotAllowedError on the browser, so skipping these attributes
		if (attestationOptions.getAuthenticatorAttachment() != null) {
			if (AuthenticatorAttachment.CROSS_PLATFORM.getAttachment().equals(attestationOptions.getAuthenticatorAttachment().getAttachment())) {
				// Put timeout
				long timeout = commonVerifiers.verifyTimeout(attestationOptions.getTimeout());
				credentialCreationOptions.setTimeout(timeout);
				log.debug("Put timeout {}", timeout);

				credentialCreationOptions.setStatus("ok");
				credentialCreationOptions.setErrorMessage("");
			}
		}
		
		// Store request in DB
		Fido2RegistrationData entity = new Fido2RegistrationData();
		entity.setUsername(attestationOptions.getUsername());
		entity.setUserId(userId);
		entity.setChallenge(challenge);
		entity.setDomain(documentDomain);
		entity.setStatus(Fido2RegistrationStatus.pending);
		//if (params.hasNonNull(CommonVerifiers.SUPER_GLUU_APP_ID)) {
		/*
		 * if (!Strings.isNullOrEmpty(attestationOptions.getSuperGluuAppId())) {
		 * entity.setApplicationId(attestationOptions.getSuperGluuAppId()); } else {
		 */
		// TODO: this can be removed out in the future
			entity.setApplicationId(documentDomain);
		//}

		// Store original requests
		entity.setAttenstationRequest(CommonUtilService.toJsonNode(attestationOptions).toString());

		Fido2RegistrationEntry registrationEntry = registrationPersistenceService.buildFido2RegistrationEntry(entity, oneStep);
		//if (params.hasNonNull("session_id")) {
		if (attestationOptions.getSessionId() != null) {
			registrationEntry.setSessionStateId(attestationOptions.getSessionId());
		}

		// Set expiration
		int unfinishedRequestExpiration = appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration();
        registrationEntry.setExpiration(unfinishedRequestExpiration);

		registrationPersistenceService.save(registrationEntry);

		log.debug("Saved in DB");

		externalFido2InterceptionContext.addToContext(registrationEntry, null);
		externalFido2InterceptionService.registerAttestationFinish(CommonUtilService.toJsonNode(attestationOptions), externalFido2InterceptionContext);

		return credentialCreationOptions;
	}

	public AttestationResultResponse verify(AttestationResult attestationResult) {
		log.debug("Attestation verify {}", CommonUtilService.toJsonNode(attestationResult));

        // Apply external custom scripts
        ExternalFido2Context externalFido2InterceptionContext = new ExternalFido2Context(CommonUtilService.toJsonNode(attestationResult), httpRequest, httpResponse);
        boolean externalInterceptContext = externalFido2InterceptionService.verifyAttestationStart(CommonUtilService.toJsonNode(attestationResult), externalFido2InterceptionContext);

        boolean superGluu = commonVerifiers.hasSuperGluu(CommonUtilService.toJsonNode(attestationResult));
        boolean oneStep = commonVerifiers.isSuperGluuOneStepMode(CommonUtilService.toJsonNode(attestationResult));
        boolean cancelRequest = commonVerifiers.isSuperGluuCancelRequest(CommonUtilService.toJsonNode(attestationResult));

		// Verify if there are mandatory request parameters
		commonVerifiers.verifyBasicAttestationResultRequest(attestationResult);
		commonVerifiers.verifyAssertionType(attestationResult.getType());

		// Get response
		//JsonNode responseNode = params.get("response");

		// Verify client data
		JsonNode clientDataJSONNode = commonVerifiers.verifyClientJSON(attestationResult.getResponse().getClientDataJSON());
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
		CredAndCounterData attestationData = attestationVerifier.verifyAuthenticatorAttestationResponse(attestationResult.getResponse(),
				registrationData);

		registrationData.setUncompressedECPoint(attestationData.getUncompressedEcPoint());
		registrationData.setSignatureAlgorithm(attestationData.getSignatureAlgorithm());
		registrationData.setCounter(attestationData.getCounters());

		String keyId = commonVerifiers.verifyCredentialId(attestationData, attestationResult);

		registrationData.setPublicKeyId(keyId);
		registrationData.setType(PublicKeyCredentialType.PUBLIC_KEY.getKeyName());
		registrationData.setAttestationType(attestationData.getAttestationType());

		registrationData.setBackupEligibilityFlag(attestationData.getBackupEligibilityFlag());
		registrationData.setBackupStateFlag(attestationData.getBackupStateFlag());

        // Support cancel request
        if (cancelRequest) {
        	registrationData.setStatus(Fido2RegistrationStatus.canceled);
        } else {
        	registrationData.setStatus(Fido2RegistrationStatus.registered);
        }

		// Store original response
		registrationData.setAttenstationResponse(CommonUtilService.toJsonNode(attestationResult).toString());

		// Set actual counter value. Note: Fido2 not update initial value in
		// Fido2RegistrationData to minimize DB updates
		registrationData.setCounter(registrationEntry.getCounter());

		String deviceDataFromReq = attestationResult.getResponse().getDeviceData();
		if (!Strings.isNullOrEmpty(deviceDataFromReq)) {
            try {
				Fido2DeviceData deviceData = dataMapperService.readValue(
						new String(base64Service.urlDecode(deviceDataFromReq), StandardCharsets.UTF_8),
						Fido2DeviceData.class);
                registrationEntry.setDeviceData(deviceData);
            } catch (Exception ex) {
                throw errorResponseFactory.invalidRequest(String.format("Device data is invalid: %s", deviceDataFromReq), ex);
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
		AttestationResultResponse attestationResultResponse = new AttestationResultResponse();

		PublicKeyCredentialDescriptor credentialDescriptor = new PublicKeyCredentialDescriptor(
				registrationData.getPublicKeyId());
		attestationResultResponse.setCreatedCredentials(credentialDescriptor);
		attestationResultResponse.setStatus("ok");
		attestationResultResponse.setErrorMessage("");

		externalFido2InterceptionContext.addToContext(registrationEntry, null);
		externalFido2InterceptionService.verifyAttestationFinish(CommonUtilService.toJsonNode(attestationResult), externalFido2InterceptionContext);

		return attestationResultResponse;
	}

	private void prepareAuthenticatorSelection(PublicKeyCredentialCreationOptions credentialCreationOptions, AttestationOptions attestationOptions) {

		// default is cross platform
		AuthenticatorAttachment authenticatorAttachment = AuthenticatorAttachment.CROSS_PLATFORM;
		UserVerification userVerification = UserVerification.preferred;
		UserVerification residentKey = UserVerification.preferred;

		Boolean requireResidentKey = false;

		if (attestationOptions.getAuthenticatorSelection() != null) {
			return;
		}
		log.debug("authenticatorSelection is not null");
		AuthenticatorSelection authenticatorSelection = attestationOptions.getAuthenticatorSelection();
		credentialCreationOptions.setAuthenticatorSelection(authenticatorSelection);
	}

	private Set<PublicKeyCredentialParameters> preparePublicKeyCredentialSelection() {
		List<String> enabledFidoAlgorithms = appConfiguration.getFido2Configuration().getEnabledFidoAlgorithms();

		Set<PublicKeyCredentialParameters> credentialParametersSets = new HashSet<>();
		if ((enabledFidoAlgorithms == null) || enabledFidoAlgorithms.isEmpty()) {
			// Add default requested credential types
			// FIDO2 RS256
			credentialParametersSets.add(PublicKeyCredentialParameters.createPublicKeyCredentialParameters(CoseRSAAlgorithm.RS256.getNumericValue()));
			// FIDO2 ES256
			credentialParametersSets.add(PublicKeyCredentialParameters.createPublicKeyCredentialParameters(CoseEC2Algorithm.ES256.getNumericValue()));
			// FIDO2 Ed25519
			credentialParametersSets.add(PublicKeyCredentialParameters.createPublicKeyCredentialParameters(CoseEdDSAAlgorithm.Ed25519.getNumericValue()));
		} else {
			for (String enabledFidoAlgorithm : enabledFidoAlgorithms) {
				CoseRSAAlgorithm coseRSAAlgorithm = null;
				try {
					coseRSAAlgorithm = CoseRSAAlgorithm.valueOf(enabledFidoAlgorithm);
				} catch (IllegalArgumentException ex) {
				}

				if (coseRSAAlgorithm != null) {
					credentialParametersSets.add(PublicKeyCredentialParameters.createPublicKeyCredentialParameters(coseRSAAlgorithm.getNumericValue()));
					break;
				}
			}

			for (String enabledFidoAlgorithm : enabledFidoAlgorithms) {
				CoseEC2Algorithm coseEC2Algorithm = null;
				try {
					coseEC2Algorithm = CoseEC2Algorithm.valueOf(enabledFidoAlgorithm);
				} catch (IllegalArgumentException ex) {
				}

				if (coseEC2Algorithm != null) {
					credentialParametersSets.add(PublicKeyCredentialParameters.createPublicKeyCredentialParameters(coseEC2Algorithm.getNumericValue()));
					break;
				}
			}

			for (String enabledFidoAlgorithm : enabledFidoAlgorithms) {
				CoseEdDSAAlgorithm coseEdDSAAlgorithm = null;
				try {
					coseEdDSAAlgorithm = CoseEdDSAAlgorithm.valueOf(enabledFidoAlgorithm);
				} catch (IllegalArgumentException ex) {
				}

				if (coseEdDSAAlgorithm != null) {
					credentialParametersSets.add(PublicKeyCredentialParameters.createPublicKeyCredentialParameters(coseEdDSAAlgorithm.getNumericValue()));
					break;
				}
			}
		}

		return credentialParametersSets;
	}

	private RelyingParty createRpDomain(String documentDomain) {
		List<RequestedParty> requestedParties = appConfiguration.getFido2Configuration().getRequestedParties();

		if ((requestedParties == null) || requestedParties.isEmpty()) {
			// Add entry for default RP
			return RelyingParty.createRelyingParty(documentDomain, appConfiguration.getIssuer());
		} else {
			for (RequestedParty requestedParty : requestedParties) {
				for (String domain : requestedParty.getDomains()) {
					if (StringHelper.equalsIgnoreCase(documentDomain, domain)) {
						// Add entry for supported RP
						return RelyingParty.createRelyingParty(documentDomain, requestedParty.getName());
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

	private Set<PublicKeyCredentialDescriptor> prepareExcludeCredentials(String documentDomain, String username) {
		List<Fido2RegistrationEntry> existingRegistrations = registrationPersistenceService
				.findByRpRegisteredUserDevices(username, documentDomain);
		Set<PublicKeyCredentialDescriptor> excludedKeys = existingRegistrations.parallelStream()
				.filter(f -> StringHelper.isNotEmpty(f.getRegistrationData().getPublicKeyId()))
				.map(f -> new PublicKeyCredentialDescriptor(
						new String[] { "usb", "ble", "nfc", "internal", "net", "qr" },
						f.getRegistrationData().getPublicKeyId()))
				.collect(Collectors.toSet());

		return excludedKeys;
	}

}