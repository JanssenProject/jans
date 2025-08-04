/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.operation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import io.jans.entry.PublicKeyCredentialHints;
import io.jans.entry.Transports;
import io.jans.fido2.ctap.AttestationConveyancePreference;
import io.jans.fido2.ctap.AuthenticatorAttachment;
import io.jans.fido2.ctap.CoseEC2Algorithm;
import io.jans.fido2.ctap.CoseRSAAlgorithm;
import io.jans.fido2.ctap.CoseEdDSAAlgorithm;
import io.jans.fido2.model.attestation.*;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.common.*;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.AttestationMode;
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
import io.jans.service.net.NetworkService;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
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

	@Inject
    private NetworkService networkService;

	@Context
	private HttpServletRequest httpRequest;
	@Context
	private HttpServletResponse httpResponse;

    /*
     * Requires mandatory parameters: username, displayName, attestation Support non
     * mandatory parameters: authenticatorSelection, origin, extensions,
     * timeout
     */
    public PublicKeyCredentialCreationOptions options(AttestationOptions attestationOptions) {

        log.debug("Attestation options {}", CommonUtilService.toJsonNode(attestationOptions).toString());

        // Apply external custom scripts
        ExternalFido2Context externalFido2InterceptionContext = new ExternalFido2Context(CommonUtilService.toJsonNode(attestationOptions), httpRequest, httpResponse);
        boolean externalInterceptContext = externalFido2InterceptionService.registerAttestationStart(CommonUtilService.toJsonNode(attestationOptions), externalFido2InterceptionContext);

        // Verify request parameters
        commonVerifiers.verifyAttestationOptions(attestationOptions);

		// Create result object
		PublicKeyCredentialCreationOptions credentialCreationOptions = new PublicKeyCredentialCreationOptions();
		

		// Generate and put challenge
		String challenge = challengeGenerator.getAttestationChallenge();
		credentialCreationOptions.setChallenge(challenge);
		log.debug("Put challenge {}", challenge);

		// Put pubKeyCredParams
		Set<PublicKeyCredentialParameters> pubKeyCredParams = preparePublicKeyCredentialSelection();
		credentialCreationOptions.setPubKeyCredParams(pubKeyCredParams);
		pubKeyCredParams.stream().forEach(ele -> log.debug("Put pubKeyCredParam {}", ele.toString()));

		// Put RP
		String origin = commonVerifiers.verifyRpDomain(attestationOptions.getOrigin(), appConfiguration.getIssuer(), appConfiguration.getFido2Configuration().getRequestedParties());
		RelyingParty relyingParty = createRpDomain(origin);
		log.debug("Relying Party: "+relyingParty);
		
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
		
		Set<PublicKeyCredentialDescriptor> excludedCredentials = prepareExcludeCredentials(origin, attestationOptions.getUsername());
		credentialCreationOptions.setExcludeCredentials(excludedCredentials);
		excludedCredentials.stream().forEach(ele -> log.debug("Put excludeCredentials {}", ele.toString()));
		
		
		//set hints - client-device, security key, hybrid
		List<String> hints = appConfiguration.getFido2Configuration().getHints();
		
		credentialCreationOptions.setHints(new HashSet<String>(hints));
		
		//TODO: check if authenticatorSelection can be set in attestation options as well specially incase of platform
		prepareAuthenticatorSelection( credentialCreationOptions,attestationOptions) ;
		
		prepareAttestation(credentialCreationOptions);
		
		
		// Copy extensions
		if (attestationOptions.getExtensions() != null) {
			credentialCreationOptions.setExtensions(attestationOptions.getExtensions());

			log.debug("Put extensions {}", attestationOptions.getExtensions());
		}
		
		// Store request in DB
		Fido2RegistrationData entity = new Fido2RegistrationData();
		entity.setUsername(attestationOptions.getUsername());
		entity.setUserId(userId);
		entity.setChallenge(challenge);
		entity.setOrigin(origin);
		entity.setStatus(Fido2RegistrationStatus.pending);
		entity.setRpId(origin);
		
		if(credentialCreationOptions.getAuthenticatorSelection()!=null && credentialCreationOptions.getAuthenticatorSelection().getAuthenticatorAttachment() != null)
		{
			entity.setAuthentictatorAttachment(credentialCreationOptions.getAuthenticatorSelection().getAuthenticatorAttachment().getAttachment());
		}

		// Store original requests
		entity.setAttestationRequest(CommonUtilService.toJsonNode(attestationOptions).toString());

		Fido2RegistrationEntry registrationEntry = registrationPersistenceService.buildFido2RegistrationEntry(entity);
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

		log.debug("Returning from options: "+credentialCreationOptions.toString());
		return credentialCreationOptions;
	}

	public AttestationOrAssertionResponse verify(AttestationResult attestationResult) {
		log.debug("Attestation verify {}", CommonUtilService.toJsonNode(attestationResult));

        // Apply external custom scripts
        ExternalFido2Context externalFido2InterceptionContext = new ExternalFido2Context(CommonUtilService.toJsonNode(attestationResult), httpRequest, httpResponse);
        boolean externalInterceptContext = externalFido2InterceptionService.verifyAttestationStart(CommonUtilService.toJsonNode(attestationResult), externalFido2InterceptionContext);

       
		// Verify if there are mandatory request parameters
		commonVerifiers.verifyBasicAttestationResultRequest(attestationResult);
		commonVerifiers.verifyAssertionType(attestationResult.getType());

		// Verify client data
		JsonNode clientDataJSONNode = commonVerifiers.verifyClientJSON(attestationResult.getResponse().getClientDataJSON());
		

		// Get challenge
		String challenge = commonVerifiers.getChallenge(clientDataJSONNode);

		// Find registration entry
		Fido2RegistrationEntry registrationEntry = registrationPersistenceService.findByChallenge(challenge)
				.parallelStream().findAny().orElseThrow(() ->
					errorResponseFactory.badRequestException(AttestationErrorResponseType.INVALID_CHALLENGE, String.format("Can't find associated attestation request by challenge '%s'", challenge)));
		Fido2RegistrationData registrationData = registrationEntry.getRegistrationData();

		// Verify domain
		domainVerifier.verifyDomain(registrationData.getOrigin(), clientDataJSONNode);

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
		
		
		// ----- testing
		HashSet<String> tempTransports = new HashSet<String>(
				Arrays.asList(attestationResult.getResponse().getTransports()));
		// in somecases only USB is shows up in transport
		if (tempTransports.contains(Transports.USB.getValue()) || tempTransports.contains(Transports.NFC.getValue())
				|| tempTransports.contains(Transports.BLE.getValue())) {
			tempTransports.add(Transports.USB.getValue());
			tempTransports.add(Transports.NFC.getValue());
			tempTransports.add(Transports.BLE.getValue());
		}

		String[] transports = (String[]) tempTransports.toArray(new String[tempTransports.size()]);

		registrationData.setTransports(transports);
		// --------- testing

		// all flags being set
		registrationData.setBackupEligibilityFlag(attestationData.getBackupEligibilityFlag());
		registrationData.setBackupStateFlag(attestationData.getBackupStateFlag());
		registrationData.setAttestedCredentialDataFlag(attestationData.isAttestedCredentialDataFlag());
		registrationData.setUserPresentFlag(attestationData.isUserPresentFlag());
		registrationData.setUserVerifiedFlag(attestationData.isUserVerifiedFlag());

		registrationData.setStatus(Fido2RegistrationStatus.registered);
		
		if(attestationResult.getAuthentictatorAttachment() == null)
		{
			
			log.debug("Transports : "+ attestationResult.getResponse().getTransports().toString());
			// look inside transports
			
			if(tempTransports.contains(Transports.INTERNAL.getValue()))
			{
				registrationData.setAuthentictatorAttachment( AuthenticatorAttachment.PLATFORM.getAttachment());
			}
			else
			{
				registrationData.setAuthentictatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM.getAttachment());
			}
			
		}
		else
		{
			registrationData.setAuthentictatorAttachment(attestationResult.getAuthentictatorAttachment());
		}

		// Store original response
		registrationData.setAttestationResponse(CommonUtilService.toJsonNode(attestationResult).toString());

		// Set actual counter value. Note: Fido2 not update initial value in
		// Fido2RegistrationData to minimize DB updates
		registrationData.setCounter(registrationEntry.getCounter());
        registrationEntry.setPublicKeyId(registrationData.getPublicKeyId());

        int publicKeyIdHash = registrationPersistenceService.getPublicKeyIdHash(registrationData.getPublicKeyId());
        registrationEntry.setPublicKeyIdHash(publicKeyIdHash);

        // Get sessionId before cleaning it from registration entry
        String sessionStateId = registrationEntry.getSessionStateId();
        registrationEntry.setSessionStateId(null);

        
        registrationEntry.clearExpiration();
        

		registrationPersistenceService.update(registrationEntry);

		// If sessionStateId is not empty update session
        if (StringHelper.isNotEmpty(sessionStateId)) {
            log.debug("There is session id. Setting session id attributes");

            userSessionIdService.updateUserSessionIdOnFinishRequest(sessionStateId, registrationEntry.getUserInum(), registrationEntry, true);
        }


		PublicKeyCredentialDescriptor credentialDescriptor = new PublicKeyCredentialDescriptor("public-key",registrationData.getTransports(),
				registrationData.getPublicKeyId());
		
		// Create result object
        AttestationOrAssertionResponse attestationResultResponse = new AttestationOrAssertionResponse(
				credentialDescriptor, "ok", "", registrationData.getUsername(),
				registrationData.getAuthentictatorAttachment().toString() , String.valueOf(registrationData.isUserPresentFlag()), true,
				registrationData.getBackupStateFlag(), registrationData.getBackupEligibilityFlag(),
				registrationData.getType().toString(), true, "level", "aaguid", "authenticatorName", registrationData.getOrigin(),
				"hint", registrationData.getChallenge(), registrationData.getRpId(), null, Long.valueOf(9000), null);
        

		externalFido2InterceptionContext.addToContext(registrationEntry, null);
		externalFido2InterceptionService.verifyAttestationFinish(CommonUtilService.toJsonNode(attestationResult), externalFido2InterceptionContext);

		return attestationResultResponse;
	}

	private void prepareAuthenticatorSelection(PublicKeyCredentialCreationOptions credentialCreationOptions,
			AttestationOptions attestationOptions) {

		// set hints - client-device, security key, hybrid
		List<String> hints = appConfiguration.getFido2Configuration().getHints();
		log.debug("hints"+hints+":"+hints.contains(PublicKeyCredentialHints.CLIENT_DEVICE.getValue()) );
		
		//credentialCreationOptions.setHints(new HashSet<String>(hints));
		
		if (attestationOptions.getAuthenticatorSelection() != null)
		{
			credentialCreationOptions.setAuthenticatorSelection(attestationOptions.getAuthenticatorSelection());
		}
		else
		{
			credentialCreationOptions.setAuthenticatorSelection(new AuthenticatorSelection());
			// only platform
			if (hints.contains(PublicKeyCredentialHints.CLIENT_DEVICE.getValue()) && hints.size() == 1) {
				

				log.debug("platform ");
				credentialCreationOptions.getAuthenticatorSelection()
						.setAuthenticatorAttachment(AuthenticatorAttachment.PLATFORM);
				credentialCreationOptions.getAuthenticatorSelection().setUserVerification(UserVerification.preferred);
				credentialCreationOptions.getAuthenticatorSelection().setRequireResidentKey(true);
				credentialCreationOptions.getAuthenticatorSelection().setResidentKey(UserVerification.preferred);

			} 
			// only cross platform
			else if (hints.size() > 0 && (hints.contains(PublicKeyCredentialHints.CLIENT_DEVICE.getValue()) == false))
			{
				log.debug("cross platform ");
				credentialCreationOptions.getAuthenticatorSelection()
						.setAuthenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM);
				credentialCreationOptions.getAuthenticatorSelection().setUserVerification(UserVerification.required);
				credentialCreationOptions.getAuthenticatorSelection().setRequireResidentKey(false);
			}
			else {
				// both platform and cross platform are a possiblity
				log.debug("both platform and cross platform are a possiblity");
				// Set defaults allowing either platform or cross-platform attachment
				// setting platform means, we show platform first
				credentialCreationOptions.getAuthenticatorSelection()
						.setAuthenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM);
				credentialCreationOptions.getAuthenticatorSelection().setUserVerification(UserVerification.preferred);
				credentialCreationOptions.getAuthenticatorSelection().setRequireResidentKey(false);

			}
			log.debug("Put authenticatorSelection {}", credentialCreationOptions.getAuthenticatorSelection());
		}
		
	}

	private void prepareAttestation(PublicKeyCredentialCreationOptions credentialCreationOptions) {
		
		List<String> hints = appConfiguration.getFido2Configuration().getHints();
		
		// set attestation - enterprise, none, direct
				boolean enterpriseAttestation = appConfiguration.getFido2Configuration().isEnterpriseAttestation();
				if (enterpriseAttestation)
				{
					credentialCreationOptions.setAttestation(AttestationConveyancePreference.enterprise);
				}
				// only platform authn, no other types of authenticators are allowed
				else if(hints.contains(PublicKeyCredentialHints.CLIENT_DEVICE.getValue()) && hints.size()== 1)
				{
					credentialCreationOptions.setAttestation(AttestationConveyancePreference.none);
				}
				else if(appConfiguration.getFido2Configuration().getAttestationMode().equals(AttestationMode.DISABLED.getValue()))
				{
					credentialCreationOptions.setAttestation(AttestationConveyancePreference.none);
				}
				
				// the priority of this check is last
				else if(hints.contains(PublicKeyCredentialHints.SECURITY_KEY.getValue()) || hints.contains(PublicKeyCredentialHints.HYBRID.getValue()))
				{
					credentialCreationOptions.setAttestation(AttestationConveyancePreference.direct);
				}
				//TODO: this else does not make sense
				else
				{
					credentialCreationOptions.setAttestation(AttestationConveyancePreference.direct);
				}
				
				log.debug("Put attestation {}", credentialCreationOptions.getAttestation());
		
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

	public RelyingParty createRpDomain(String origin) {
		List<RequestedParty> requestedParties = appConfiguration.getFido2Configuration().getRequestedParties();
		
		if ((requestedParties == null) || requestedParties.isEmpty()) {
			// Add entry for default RP
			return RelyingParty.createRelyingParty(origin, appConfiguration.getIssuer());
		} else {
			for (RequestedParty requestedParty : requestedParties) {

				for (String domain : requestedParty.getOrigins()) {

					if (StringHelper.equalsIgnoreCase(origin, domain)) {
						// Add entry for supported RP
						return RelyingParty.createRelyingParty(origin, requestedParty.getId());
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


	private Set<PublicKeyCredentialDescriptor> prepareExcludeCredentials(String origin, String username) {
		List<Fido2RegistrationEntry> existingRegistrations = registrationPersistenceService
				.findByRpRegisteredUserDevices(username, origin);
		Set<PublicKeyCredentialDescriptor> excludedKeys = existingRegistrations.parallelStream()
				.filter(f -> StringHelper.isNotEmpty(f.getRegistrationData().getPublicKeyId()))
				.map(f -> new PublicKeyCredentialDescriptor("public-key",
						new String[] { Transports.USB.getValue(),Transports.BLE.getValue() ,Transports.NFC.getValue() ,Transports.INTERNAL.getValue(), Transports.HYBRID.getValue() },
						f.getRegistrationData().getPublicKeyId()))
				.collect(Collectors.toSet());

		return excludedKeys;
	}
	
}