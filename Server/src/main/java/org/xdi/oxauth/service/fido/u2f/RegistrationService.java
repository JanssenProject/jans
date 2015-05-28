/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.crypto.random.ChallengeGenerator;
import org.xdi.oxauth.exception.fido.u2f.DeviceCompromisedException;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;
import org.xdi.oxauth.model.fido.u2f.exception.BadInputException;
import org.xdi.oxauth.model.fido.u2f.message.RawRegisterResponse;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateRequest;
import org.xdi.oxauth.model.fido.u2f.protocol.ClientData;
import org.xdi.oxauth.model.fido.u2f.protocol.RegisterRequest;
import org.xdi.oxauth.model.fido.u2f.protocol.RegisterRequestMessage;
import org.xdi.oxauth.model.fido.u2f.protocol.RegisterResponse;
import org.xdi.oxauth.model.util.Base64Util;

/**
 * Provides operations with U2F registration requests
 *
 * @author Yuriy Movchan Date: 05/19/2015
 */
@Scope(ScopeType.APPLICATION)
//@Scope(ScopeType.STATELESS)
//TODO: Replace after finish
@Name("u2fRegistrationService")
@AutoCreate
public class RegistrationService {

	@Logger
	private Log log;

	@In
	private ApplicationService applicationService;

	@In
	private AuthenticationService u2fAuthenticationService;

	@In
	private RawRegistrationService rawRegistrationService;

	@In
	private ClientDataValidationService clientDataValidationService;

	@In
	private DeviceRegistrationService deviceRegistrationService;

	@In(value = "randomChallengeGenerator")
	private ChallengeGenerator challengeGenerator;

	private final Map<String, RegisterRequestMessage> requestStorage = new HashMap<String, RegisterRequestMessage>();

	public RegisterRequestMessage builRegisterRequestMessage(String appId, String userName) {
		if (applicationService.isValidateApplication()) {
			applicationService.checkIsValid(appId);
		}

		List<AuthenticateRequest> authenticateRequests = new ArrayList<AuthenticateRequest>();
		List<RegisterRequest> registerRequests = new ArrayList<RegisterRequest>();

		List<DeviceRegistration> devices = deviceRegistrationService.findUserDeviceRegistrations(userName, appId);
		for (DeviceRegistration device : devices) {
			if (!device.isCompromised()) {
				try {
					AuthenticateRequest authenticateRequest = u2fAuthenticationService.startAuthentication(appId, device);
					authenticateRequests.add(authenticateRequest);
				} catch (DeviceCompromisedException ex) {
					log.error("Faield to authenticate device", ex);
				}
			}
		}

		RegisterRequest request = startRegistration(appId);
		registerRequests.add(request);

		return new RegisterRequestMessage(authenticateRequests, registerRequests);

	}

	public RegisterRequest startRegistration(String appId) {
		return startRegistration(appId, challengeGenerator.generateChallenge());
	}

	public RegisterRequest startRegistration(String appId, byte[] challenge) {
		return new RegisterRequest(Base64Util.base64urlencode(challenge), appId);
	}

	public DeviceRegistration finishRegistration(RegisterRequestMessage requestMessage, RegisterResponse response, String userName) throws BadInputException {
		return finishRegistration(requestMessage, response, userName, null);
	}

	public DeviceRegistration finishRegistration(RegisterRequestMessage requestMessage, RegisterResponse response, String userName, Set<String> facets)
			throws BadInputException {
		RegisterRequest request = requestMessage.getRegisterRequest();
		String appId = request.getAppId();

		ClientData clientData = response.getClientData();
		clientDataValidationService.checkContent(clientData, RawRegistrationService.REGISTER_TYPE, request.getChallenge(), facets);

		RawRegisterResponse rawRegisterResponse = rawRegistrationService.parseRawRegisterResponse(response.getRegistrationData());
		rawRegistrationService.checkSignature(appId, clientData, rawRegisterResponse);
		DeviceRegistration deviceRegistration = rawRegistrationService.createDevice(rawRegisterResponse);

		deviceRegistrationService.addUserDeviceRegistration(userName, appId, deviceRegistration);

		return deviceRegistration;
	}

	public void storeRegistrationRequestMessage(RegisterRequestMessage requestMessage) {
		requestStorage.put(requestMessage.getRequestId(), requestMessage);
	}

	public RegisterRequestMessage getRegistrationRequestMessage(String requestId) {
		return requestStorage.get(requestId);
	}

	public void removeRegistrationRequestMessage(String requestId) {
		requestStorage.remove(requestId);
	}

}
