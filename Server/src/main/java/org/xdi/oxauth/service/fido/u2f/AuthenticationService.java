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
import org.xdi.oxauth.exception.fido.u2f.NoEligableDevicesException;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;
import org.xdi.oxauth.model.fido.u2f.exception.BadInputException;
import org.xdi.oxauth.model.fido.u2f.message.RawAuthenticateResponse;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateRequest;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateRequestMessage;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateResponse;
import org.xdi.oxauth.model.fido.u2f.protocol.ClientData;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.util.StringHelper;

/**
 * Provides operations with U2F authentication request
 *
 * @author Yuriy Movchan Date: 05/19/2015
 */
@Scope(ScopeType.STATELESS)
@Name("authenticationService")
@AutoCreate
public class AuthenticationService {

	@Logger
	private Log log;

	@In
	private ApplicationService applicationService;

	@In
	private RawAuthenticationService rawAuthenticationService;

	@In
	private ClientDataValidationService clientDataValidationService;

	@In
	private DeviceRegistrationService deviceRegistrationService;

	@In(value = "randomChallengeGenerator")
	private ChallengeGenerator challengeGenerator;

	private final Map<String, AuthenticateRequestMessage> requestStorage = new HashMap<String, AuthenticateRequestMessage>();

	public AuthenticateRequestMessage buildAuthenticateRequestMessage(String appId, String userName) throws BadInputException, NoEligableDevicesException {
		if (applicationService.isValidateApplication()) {
			applicationService.checkIsValid(appId);
		}

		List<AuthenticateRequest> authenticateRequests = new ArrayList<AuthenticateRequest>();
		byte[] challenge = challengeGenerator.generateChallenge();

		List<DeviceRegistration> devices = deviceRegistrationService.findUserDeviceRegistrations(appId, userName);
		for (DeviceRegistration device : devices) {
			if (!device.isCompromised()) {
				AuthenticateRequest request;
				try {
					request = startAuthentication(appId, device, challenge);
					authenticateRequests.add(request);
				} catch (DeviceCompromisedException ex) {
					log.error("Faield to authenticate device", ex);
				}
			}
		}

		if (authenticateRequests.isEmpty()) {
			if (devices.isEmpty()) {
				throw new NoEligableDevicesException(devices, "No devices registrered");
			} else {
				throw new NoEligableDevicesException(devices, "All devices compromised");
			}
		}

		return new AuthenticateRequestMessage(authenticateRequests);
	}

	public AuthenticateRequest startAuthentication(String appId, DeviceRegistration device) throws DeviceCompromisedException {
		return startAuthentication(appId, device, challengeGenerator.generateChallenge());
	}

	public AuthenticateRequest startAuthentication(String appId, DeviceRegistration device, byte[] challenge) throws DeviceCompromisedException {
		if (!device.isCompromised()) {
			throw new DeviceCompromisedException(device, "Device has been marked as compromised, cannot authenticate");
		}

		return new AuthenticateRequest(Base64Util.base64urlencode(challenge), appId, device.getKeyHandle());
	}

	public DeviceRegistration finishAuthentication(AuthenticateRequestMessage requestMessage, AuthenticateResponse response, String userName)
			throws BadInputException, DeviceCompromisedException {
		List<DeviceRegistration> deviceRegistrations = deviceRegistrationService.findUserDeviceRegistrations(requestMessage.getAppId(), userName);

		return finishAuthentication(requestMessage, response, deviceRegistrations, userName, null);
	}

	public DeviceRegistration finishAuthentication(AuthenticateRequestMessage requestMessage, AuthenticateResponse response,
			List<DeviceRegistration> deviceRegistrations, String userName, Set<String> facets) throws BadInputException, DeviceCompromisedException {
		final AuthenticateRequest request = getAuthenticateRequest(requestMessage, response);

		DeviceRegistration usedDeviceRegistration = null;
		for (DeviceRegistration deviceRegistration : deviceRegistrations) {
			if (StringHelper.equals(request.getKeyHandle(), deviceRegistration.getKeyHandle())) {
				usedDeviceRegistration = deviceRegistration;
				break;
			}
		}

		if (usedDeviceRegistration == null) {
			throw new BadInputException("Failed to find DeviceRegistration for the given AuthenticateRequest");
		}

		if (usedDeviceRegistration.isCompromised()) {
			throw new DeviceCompromisedException(usedDeviceRegistration, "The device is marked as possibly compromised, and cannot be authenticated");
		}

		ClientData clientData = response.getClientData();
		clientDataValidationService.checkContent(clientData, RawAuthenticationService.AUTHENTICATE_TYPE, request.getChallenge(), facets);

		RawAuthenticateResponse rawAuthenticateResponse = rawAuthenticationService.parseRawAuthenticateResponse(response.getSignatureData());
		rawAuthenticationService.checkSignature(request.getAppId(), clientData, rawAuthenticateResponse,
				Base64Util.base64urldecode(usedDeviceRegistration.getPublicKey()));
		rawAuthenticateResponse.checkUserPresence();
		usedDeviceRegistration.checkAndUpdateCounter(rawAuthenticateResponse.getCounter());

		deviceRegistrationService.updateDeviceRegistration(userName, usedDeviceRegistration);

		return usedDeviceRegistration;
	}

	public AuthenticateRequest getAuthenticateRequest(AuthenticateRequestMessage requestMessage, AuthenticateResponse response) throws BadInputException {
		if (!StringHelper.equals(requestMessage.getRequestId(), response.getRequestId())) {
			throw new BadInputException("Wrong request for response data");
		}

		for (AuthenticateRequest request : requestMessage.getAuthenticateRequests()) {
			if (StringHelper.equals(request.getKeyHandle(), response.getKeyHandle())) {
				return request;
			}
		}

		throw new BadInputException("Responses keyHandle does not match any contained request");
	}

	public void storeAuthenticationRequestMessage(AuthenticateRequestMessage requestMessage) {
		requestStorage.put(requestMessage.getRequestId(), requestMessage);
	}

	public AuthenticateRequestMessage getAuthenticationRequestMessage(String requestId) {
		return requestStorage.get(requestId);
	}

	public void removeAuthenticationRequestMessage(String requestId) {
		requestStorage.remove(requestId);
	}

}
