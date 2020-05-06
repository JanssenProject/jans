package org.gluu.oxauth.fido2.ws.rs.service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.ctap.AttestationConveyancePreference;
import org.gluu.oxauth.fido2.ctap.AuthenticatorAttachment;
import org.gluu.oxauth.fido2.ctap.CoseEC2Algorithm;
import org.gluu.oxauth.fido2.ctap.CoseRSAAlgorithm;
import org.gluu.oxauth.fido2.ctap.UserVerification;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.model.auth.CredAndCounterData;
import org.gluu.oxauth.fido2.model.cert.PublicKeyCredentialDescriptor;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationData;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationEntry;
import org.gluu.oxauth.fido2.model.entry.Fido2RegistrationStatus;
import org.gluu.oxauth.fido2.persist.RegistrationPersistenceService;
import org.gluu.oxauth.fido2.service.Base64Service;
import org.gluu.oxauth.fido2.service.ChallengeGenerator;
import org.gluu.oxauth.fido2.service.DataMapperService;
import org.gluu.oxauth.fido2.service.verifier.AuthenticatorAttestationVerifier;
import org.gluu.oxauth.fido2.service.verifier.ChallengeVerifier;
import org.gluu.oxauth.fido2.service.verifier.CommonVerifiers;
import org.gluu.oxauth.fido2.service.verifier.DomainVerifier;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.service.net.NetworkService;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ApplicationScoped
public class AttestationService {

    @Inject
    private Logger log;

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
    private NetworkService networkService;

    @Inject
    private AppConfiguration appConfiguration;

    /*
     * Supports parameters: username, displayName, attestation, authenticatorSelection, extensions
     * documentDomain
     */
    public JsonNode options(JsonNode params) {
        log.debug("attestationOptions {}", params);

        // Verify request parameters
        commonVerifiers.verifyOptions(params);

        // Result object
        ObjectNode credentialCreationOptionsNode = dataMapperService.createObjectNode();

        // Generate challenge
        String challenge = challengeGenerator.getChallenge();

        // Put attestation
        AttestationConveyancePreference attestationConveyancePreference = commonVerifiers.verifyAttestationConveyanceType(params);
        credentialCreationOptionsNode.put("attestation", attestationConveyancePreference.toString());
        log.debug("Put attestation {}", attestationConveyancePreference);

        // Put authenticatorSelection
        ObjectNode authenticatorSelectionNode = prepareAuthenticatorSelection(params);
        credentialCreationOptionsNode.set("authenticatorSelection", authenticatorSelectionNode);
        log.debug("Put authenticatorSelection {}", authenticatorSelectionNode);

        // Put challenge
        credentialCreationOptionsNode.put("challenge", challenge);
        log.debug("Put challenge {}", challenge);

        // Put pubKeyCredParams
        ArrayNode credentialParametersNode = preparePublicKeyCredentialSelection();
        credentialCreationOptionsNode.set("pubKeyCredParams", credentialParametersNode);
        log.debug("Put pubKeyCredParams {}", credentialParametersNode);

        // Put rp
        String documentDomain = getRpDomain(params);

        ObjectNode credentialRpEntityNode = creareRpDomain(documentDomain);
        credentialCreationOptionsNode.set("rp", credentialRpEntityNode);
        log.debug("Put rp {}", credentialRpEntityNode);

        // Put user
        String userId = generateUserId();
        String username = params.get("username").asText();
        String displayName = params.get("displayName").asText();

        ObjectNode credentialUserEntityNode = createUserCredentials(userId, username, displayName);
        credentialCreationOptionsNode.set("user", credentialUserEntityNode);
        log.debug("Put user {}", credentialUserEntityNode);

        // Put excludeCredentials
        ArrayNode excludedCredentials = prepareExcludeCredentials(username);
        credentialCreationOptionsNode.set("excludeCredentials", excludedCredentials);
        log.debug("Put excludeCredentials {}", excludedCredentials);

        // Copy extensions
        if (params.hasNonNull("extensions")) {
        	JsonNode extensions = params.get("extensions");
            credentialCreationOptionsNode.set("extensions", extensions);
            log.debug("Put extensions {}", extensions);
        }

        credentialCreationOptionsNode.put("status", "ok");
        credentialCreationOptionsNode.put("errorMessage", "");

        // Store request in DB
        Fido2RegistrationData entity = new Fido2RegistrationData();
        entity.setUsername(username);
        entity.setUserId(userId);
        entity.setChallenge(challenge);
        entity.setDomain(documentDomain);
        entity.setCredentialCreationOptions(credentialCreationOptionsNode.toString());
        entity.setStatus(Fido2RegistrationStatus.pending);

        registrationsRepository.save(entity);

        return credentialCreationOptionsNode;
    }

	private ObjectNode prepareAuthenticatorSelection(JsonNode params) {
		AuthenticatorAttachment authenticatorAttachment = AuthenticatorAttachment.CROSS_PLATFORM;
        UserVerification userVerification = UserVerification.preferred;
        Boolean requireResidentKey = false;
        if (params.hasNonNull("authenticatorSelection")) {
        	JsonNode authenticatorSelectionNodeParameter = params.get("authenticatorSelection");
        	authenticatorAttachment = commonVerifiers.verifyAuthenticatorAttachment(authenticatorSelectionNodeParameter.get("authenticatorAttachment"));
        	userVerification = commonVerifiers.verifyUserVerification(authenticatorSelectionNodeParameter.get("userVerification"));
        	requireResidentKey = commonVerifiers.verifyRequireResidentKey(authenticatorSelectionNodeParameter.get("requireResidentKey"));
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
		// @TODO: Add parameters to allow specify supported keys
        ArrayNode credentialParametersNode = dataMapperService.createArrayNode();

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
        
        // FIDO
//        ObjectNode credentialParametersNode = credentialParametersArrayNode.addObject();
//        credentialParametersNode.put("type", "FIDO");
//        credentialParametersNode.put("alg", CoseEC2Algorithm.ES256.getNumericValue());
		return credentialParametersNode;
	}

	private String getRpDomain(JsonNode params) {
		// TODO: Where takes this information?
        // Allow to change default RP
        // @TODO: Use client to get information about RP 
        String documentDomain;
        if (params.hasNonNull("documentDomain")) {
            documentDomain = params.get("documentDomain").asText();
        } else {
            documentDomain = appConfiguration.getIssuer();
        }
        documentDomain = networkService.getHost(documentDomain);

        return documentDomain;
	}

	private ObjectNode creareRpDomain(String documentDomain) {
		ObjectNode credentialRpEntityNode = dataMapperService.createObjectNode();
        credentialRpEntityNode.put("name", "oxAuth RP");
        credentialRpEntityNode.put("id", documentDomain);

        return credentialRpEntityNode;
	}

	private String generateUserId() {
		byte[] buffer = new byte[32];
        new SecureRandom().nextBytes(buffer);

        return base64Service.urlEncodeToString(buffer);
	}

	private ObjectNode createUserCredentials(String userId, String username, String displayName) {
		ObjectNode credentialUserEntityNode = dataMapperService.createObjectNode();
        // Get username and displayName parameters
        // TODO: Is userId should be the same for all keys?
        credentialUserEntityNode.put("id", userId);
        credentialUserEntityNode.put("name", username);
        credentialUserEntityNode.put("displayName", displayName);

        return credentialUserEntityNode;
	}

	private ArrayNode prepareExcludeCredentials(String username) {
		// @TODO: Protect user enrolled keys and username 
        List<Fido2RegistrationEntry> existingRegistrations = registrationsRepository.findAllByUsername(username);
        List<JsonNode> excludedKeys = existingRegistrations.parallelStream()
                .filter(f -> (Fido2RegistrationStatus.registered.equals(f.getRegistrationData().getStatus())))
                .map(f -> dataMapperService.convertValue(
                        new PublicKeyCredentialDescriptor(f.getRegistrationData().getType(), f.getRegistrationData().getPublicKeyId()),
                        JsonNode.class))
                .collect(Collectors.toList());

        ArrayNode excludedCredentials = dataMapperService.createArrayNode();
        excludedCredentials.addAll(excludedKeys);

        return excludedCredentials;
	}

    public JsonNode verify(JsonNode params) {
        log.debug("registerResponse {}", params);

        // Verify if there are mandatory request parameters
        commonVerifiers.verifyBasicPayload(params);
        commonVerifiers.verifyBase64UrlString(params, "type");

        // Get response
        JsonNode responseNode = params.get("response");

        // Verify client data
        JsonNode clientDataJSONNode = commonVerifiers.verifyClientJSON(responseNode);

        // Get credential entry
        Fido2RegistrationEntry credentialEntryFound = findCredentialEntryByChallenge(clientDataJSONNode);
        Fido2RegistrationData credentialFound = credentialEntryFound.getRegistrationData();

        // Verify domain
        domainVerifier.verifyDomain(credentialFound.getDomain(), clientDataJSONNode);

        // Verify domain
        CredAndCounterData attestationData = authenticatorAttestationVerifier.verifyAuthenticatorAttestationResponse(responseNode, credentialFound);

        credentialFound.setUncompressedECPoint(attestationData.getUncompressedEcPoint());
        credentialFound.setSignatureAlgorithm(attestationData.getSignatureAlgorithm());
        credentialFound.setCounter(attestationData.getCounters());

        if (attestationData.getCredId() != null) {
            credentialFound.setPublicKeyId(attestationData.getCredId());
        } else {
            String keyId = commonVerifiers.verifyBase64UrlString(params, "id");
            credentialFound.setPublicKeyId(keyId);
        }
        credentialFound.setType("public-key");
        credentialFound.setStatus(Fido2RegistrationStatus.registered);

        // Store original response
        credentialFound.setAuthenticatorAttenstationResponse(responseNode.toString());

        registrationsRepository.update(credentialEntryFound);

        // Result object
        ObjectNode credentialFinishOptionsNode = dataMapperService.createObjectNode();

        PublicKeyCredentialDescriptor credentialDescriptor = new PublicKeyCredentialDescriptor(credentialFound.getType(), credentialFound.getPublicKeyId());
        credentialFinishOptionsNode.set("createdCredentials", dataMapperService.convertValue(credentialDescriptor, JsonNode.class));
        credentialFinishOptionsNode.put("status", "ok");
        credentialFinishOptionsNode.put("errorMessage", "");

        return credentialFinishOptionsNode;
    }

	private Fido2RegistrationEntry findCredentialEntryByChallenge(JsonNode clientDataJSONNode) {
		String clientDataChallenge = base64Service
                .urlEncodeToStringWithoutPadding(base64Service.urlDecode(clientDataJSONNode.get("challenge").asText()));
        log.debug("Challenge {}", clientDataChallenge);

        List<Fido2RegistrationEntry> registrationEntries = registrationsRepository.findAllByChallenge(clientDataChallenge);
        Fido2RegistrationEntry credentialEntryFound = registrationEntries.parallelStream().findAny()
                .orElseThrow(() -> new Fido2RPRuntimeException(String.format("Can't find request with matching challenge '%s' and domain", clientDataChallenge)));

        return credentialEntryFound;
	}

}
