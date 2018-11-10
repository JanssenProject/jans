package org.gluu.oxauth.fido2.ws.rs.controller;

/// *
// * Copyright (c) 2018 Mastercard
// * Copyright (c) 2018 Gluu
// *
// * Licensed under the Apache License, Version 2.0 (the "License"); you may not
/// use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
/// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
/// limitations under the License.
// */
//
// package org.gluu.oxauth.fido2.controller;
//
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.databind.node.ArrayNode;
// import com.fasterxml.jackson.databind.node.ObjectNode;
// import org.gluu.oxauth.fido2.database.FIDO2AuthenticationEntity;
// import org.gluu.oxauth.fido2.database.FIDO2AuthenticationRepository;
// import org.gluu.oxauth.fido2.database.FIDO2RegistrationEntity;
// import org.gluu.oxauth.fido2.database.FIDO2RegistrationRepository;
// import org.gluu.oxauth.fido2.database.RegistrationStatus;
// import org.gluu.oxauth.fido2.attestation.AttestationService;
// import org.gluu.oxauth.fido2.service.AuthenticatorAssertionVerifier;
// import org.gluu.oxauth.fido2.service.AuthenticatorAttestationVerifier;
// import org.gluu.oxauth.fido2.service.CredAndCounterData;
// import org.gluu.oxauth.fido2.service.Fido2RPRuntimeException;
// import java.io.IOException;
// import java.net.MalformedURLException;
// import java.net.URL;
// import java.nio.charset.Charset;
// import java.security.SecureRandom;
// import java.util.Base64;
// import java.util.List;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import javax.inject.Inject;
// import javax.inject.Named;
// import org.springframework.util.StringUtils;
// import org.springframework.web.bind.annotation.PatchMapping;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;
//
// @RestController
// @RequestMapping(value = "/webauthn")
// public class WebAuthnController {
// private static final Logger LOGGER =
/// LoggerFactory.getLogger(WebAuthnController.class);
//
// @Inject
// private ObjectMapper om;
//
// @Inject
// AttestationService attestationService;
//
// @Inject
// FIDO2RegistrationRepository registrationsRepository;
//
// @Inject
// FIDO2AuthenticationRepository authenticationsRepository;
//
// @Inject
// AuthenticatorAttestationVerifier authenticatorAttestationVerifier;
// @Inject
// AuthenticatorAssertionVerifier authenticatorAuthorizationVerifier;
//
// @Inject
// @Named("base64UrlEncoder")
// private Base64.Encoder base64UrlEncoder;
//
// @Inject
// @Named("base64UrlDecoder")
// private Base64.Decoder base64UrlDecoder;
//
//
// @PostMapping(value = {"/register","/attestation/options"}, produces =
/// {"application/json"}, consumes = {"application/json"})
// JsonNode register(@RequestBody JsonNode params) {
// return attestationService.register();
// }
//
// @PostMapping(value = {"/attestation/result"}, produces =
/// {"application/json"}, consumes = {"application/json"})
// JsonNode attestationResponse(@RequestBody JsonNode params) {
// log.info("registerResponse {}", params);
// JsonNode request = params.get("request");
// JsonNode response = params.get("response");
// String userId = request.get("user").get("id").asText();
// String challenge = request.get("challenge").asText();
// JsonNode clientDataJSONNode = null;
//
// try {
// clientDataJSONNode = om.readTree(new
/// String(base64Service.urlDecode(params.get("response").get("response").get("clientDataJSON").asText()),Charset.forName("UTF-8")));
// } catch (IOException e) {
// new Fido2RPRuntimeException("Can't parse message");
// }
// String keyId = response.get("id").asText();
//
// String clientDataChallenge = clientDataJSONNode.get("challenge").asText();
// String clientDataOrigin = clientDataJSONNode.get("origin").asText();
//
// log.info("userId {} challenge {} {} {}",
/// userId,challenge,clientDataChallenge,clientDataOrigin);
// if(!challenge.equals(clientDataChallenge)){
// throw new Fido2RPRuntimeException("Challenges don't match");
// }
//
// List<FIDO2RegistrationEntity> registrations =
/// registrationsRepository.findAllByUserId(userId);
// FIDO2RegistrationEntity credentialFound = registrations.parallelStream()
// .filter(f -> verifyChallenge(f.getChallenge(),challenge,clientDataChallenge))
// .filter(f-> verifyDomain(f.getDomain(),clientDataOrigin))
// .findAny()
// .orElseThrow(() -> new Fido2RPRuntimeException("Can't find request with
/// matching id and challenge"));
//
//
// CredAndCounterData attestationData =
/// authenticatorAttestationVerifier.verifyAuthenticatorAttestationResponse(response,credentialFound.getDomain());
// credentialFound.setUncompressedECPoint(attestationData.getUncompressedEcPoint());
// credentialFound.setAttestationType(attestationData.getAttestationType());
// credentialFound.setStatus(RegistrationStatus.REGISTERED);
// credentialFound.setW3cAuthenticatorAttenstationResponse(response.toString());
// credentialFound.setPublicKeyId(attestationData.getCredId());
// registrationsRepository.save(credentialFound);
// return params;
// }
//
//
//
//
//
// @PatchMapping(value = {"/register"}, produces = {"application/json"},
/// consumes = {"application/json"})
// JsonNode registerResponse(@RequestBody JsonNode params) {
//
// }
//
//
//
// @PostMapping(value = {"/authenticate"}, produces = {"application/json"},
/// consumes = {"application/json"})
// JsonNode authenticate(@RequestBody JsonNode params) {
//
// log.info("authenticate {}", params);
// String username = params.get("username").asText();
// String documentDomain = params.get("documentDomain").asText();
//
// ObjectNode credentialRequestOptionsNode = om.createObjectNode();
// List<FIDO2RegistrationEntity> registrations =
/// registrationsRepository.findAllByUsernameAndDomain(username,documentDomain);
//
// byte buffer[] = new byte[32];
// new SecureRandom().nextBytes(buffer);
//
// String challenge = base64Service.urlEncodeToString(buffer);
// credentialRequestOptionsNode.put("challenge", challenge);
//
// ObjectNode credentialUserEntityNode =
/// credentialRequestOptionsNode.putObject("user");
// credentialUserEntityNode.put("name", username);
//
// ObjectNode publicKeyCredentialRpEntityNode =
/// credentialRequestOptionsNode.putObject("rp");
// publicKeyCredentialRpEntityNode.put("name", "ACME Dawid");
// publicKeyCredentialRpEntityNode.put("id", documentDomain);
// ArrayNode publicKeyCredentialDescriptors =
/// credentialRequestOptionsNode.putArray("allowCredentials");
//
// for(FIDO2RegistrationEntity registration :registrations) {
// if(StringUtils.isEmpty(registration.getPublicKeyId())) {
// throw new Fido2RPRuntimeException("Can't find associated key. Have you
/// registered");
// }
// ObjectNode publicKeyCredentialDescriptorNode =
/// publicKeyCredentialDescriptors.addObject();
// publicKeyCredentialDescriptorNode.put("type","public-key");
// ArrayNode authenticatorTransportNode =
/// publicKeyCredentialDescriptorNode.putArray("transports");
// authenticatorTransportNode.add("usb").add("ble").add("nfc");
// publicKeyCredentialDescriptorNode.put("id",registration.getPublicKeyId());
// }
//
// credentialRequestOptionsNode.put("status", "ok");
//
//
// FIDO2AuthenticationEntity entity = new FIDO2AuthenticationEntity();
// entity.setUsername(username);
// entity.setChallenge(challenge);
// entity.setDomain(documentDomain);
// entity.setW3cCredentialRequestOptions(credentialRequestOptionsNode.toString());
// authenticationsRepository.save(entity);
//
// return credentialRequestOptionsNode;
// }
//
// @PatchMapping(value = {"/authenticate"}, produces = {"application/json"},
/// consumes = {"application/json"})
// JsonNode authenticateResponse(@RequestBody JsonNode params) {
// log.info("authenticateResponse {}", params);
// JsonNode request = params.get("request");
// JsonNode response = params.get("response");
// String challenge = request.get("challenge").asText();
// String username = request.get("user").get("name").asText();
// String domain = params.get("request").get("rp").get("id").asText();
//
// JsonNode clientDataJSONNode;
// try {
// clientDataJSONNode = om.readTree(new
/// String(base64Service.urlDecode(params.get("response").get("response").get("clientDataJSON").asText()),Charset.forName("UTF-8")));
// } catch (IOException e) {
// throw new Fido2RPRuntimeException("Can't parse message");
// }catch (Exception e) {
// throw new Fido2RPRuntimeException("Invalid assertion data");
// }
//
//
// FIDO2AuthenticationEntity authenticationEntity =
/// authenticationsRepository.findByChallenge(challenge).orElseThrow(() -> new
/// Fido2RPRuntimeException("Can't find matching request"));
//
// String clientDataChallenge = clientDataJSONNode.get("challenge").asText();
// String clientDataOrigin = clientDataJSONNode.get("origin").asText();
//
// verifyChallenge(authenticationEntity.getChallenge(),challenge,clientDataChallenge);
// verifyDomain(authenticationEntity.getDomain(),clientDataOrigin);
//
// String keyId = response.get("id").asText();
// FIDO2RegistrationEntity registration =
/// registrationsRepository.findByPublicKeyId(keyId).orElseThrow(()->new
/// Fido2RPRuntimeException("Couldn't find the key"));
// authenticatorAuthorizationVerifier.verifyAuthenticatorAssertionResponse(response,
/// registration);
//
// authenticationEntity.setW3cAuthenticatorAssertionResponse(response.toString());
// authenticationsRepository.save(authenticationEntity);
// return params;
// }
//
//
//
//
//
//
//
// }
//
