/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.gluu.site.ldap.persistence.LdapEntryManager;
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
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.fido.u2f.AuthenticateRequestMessageLdap;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;
import org.xdi.oxauth.model.fido.u2f.exception.BadInputException;
import org.xdi.oxauth.model.fido.u2f.message.RawAuthenticateResponse;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateRequest;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateRequestMessage;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateResponse;
import org.xdi.oxauth.model.fido.u2f.protocol.ClientData;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.service.UserService;
import org.xdi.util.StringHelper;

import com.unboundid.ldap.sdk.Filter;

/**
 * Provides operations with U2F authentication request
 *
 * @author Yuriy Movchan Date: 05/19/2015
 */
@Scope(ScopeType.STATELESS)
@Name("u2fAuthenticationService")
@AutoCreate
public class AuthenticationService extends RequestService {

	@Logger
	private Log log;

	@In
	private LdapEntryManager ldapEntryManager;

	@In
	private ApplicationService applicationService;

	@In
	private RawAuthenticationService rawAuthenticationService;

	@In
	private ClientDataValidationService clientDataValidationService;

	@In
	private DeviceRegistrationService deviceRegistrationService;

	@In
	private UserService userService;

	@In(value = "randomChallengeGenerator")
	private ChallengeGenerator challengeGenerator;

	public AuthenticateRequestMessage buildAuthenticateRequestMessage(String appId, String userName) throws BadInputException, NoEligableDevicesException {
		if (applicationService.isValidateApplication()) {
			applicationService.checkIsValid(appId);
		}

		String userInum = userService.getUserInum(userName);
		if (StringHelper.isEmpty(userInum)) {
			throw new BadInputException(String.format("Failed to find user '%s' in LDAP", userName));
		}

		List<AuthenticateRequest> authenticateRequests = new ArrayList<AuthenticateRequest>();
		byte[] challenge = challengeGenerator.generateChallenge();

		List<DeviceRegistration> deviceRegistrations = deviceRegistrationService.findUserDeviceRegistrations(userInum, appId);
		for (DeviceRegistration deviceRegistration : deviceRegistrations) {
			if (!deviceRegistration.isCompromised()) {
				AuthenticateRequest request;
				try {
					request = startAuthentication(appId, deviceRegistration, challenge);
					authenticateRequests.add(request);
				} catch (DeviceCompromisedException ex) {
					log.error("Faield to authenticate device", ex);
				}
			}
		}

		if (authenticateRequests.isEmpty()) {
			if (deviceRegistrations.isEmpty()) {
				throw new NoEligableDevicesException(deviceRegistrations, "No devices registrered");
			} else {
				throw new NoEligableDevicesException(deviceRegistrations, "All devices compromised");
			}
		}

		return new AuthenticateRequestMessage(authenticateRequests);
	}

	public AuthenticateRequest startAuthentication(String appId, DeviceRegistration device) throws DeviceCompromisedException {
		return startAuthentication(appId, device, challengeGenerator.generateChallenge());
	}

	public AuthenticateRequest startAuthentication(String appId, DeviceRegistration device, byte[] challenge) throws DeviceCompromisedException {
		if (device.isCompromised()) {
			throw new DeviceCompromisedException(device, "Device has been marked as compromised, cannot authenticate");
		}

		return new AuthenticateRequest(Base64Util.base64urlencode(challenge), appId, device.getDeviceRegistrationConfiguration().getKeyHandle());
	}

	public DeviceRegistration finishAuthentication(AuthenticateRequestMessage requestMessage, AuthenticateResponse response, String userName)
			throws BadInputException, DeviceCompromisedException {
		return finishAuthentication(requestMessage, response, userName, null);
	}

	public DeviceRegistration finishAuthentication(AuthenticateRequestMessage requestMessage, AuthenticateResponse response, String userName, Set<String> facets)
			throws BadInputException, DeviceCompromisedException {
		String userInum = userService.getUserInum(userName);
		if (StringHelper.isEmpty(userInum)) {
			throw new BadInputException(String.format("Failed to find user '%s' in LDAP", userName));
		}

		List<DeviceRegistration> deviceRegistrations = deviceRegistrationService.findUserDeviceRegistrations(userInum, requestMessage.getAppId());

		final AuthenticateRequest request = getAuthenticateRequest(requestMessage, response);

		DeviceRegistration usedDeviceRegistration = null;
		for (DeviceRegistration deviceRegistration : deviceRegistrations) {
			if (StringHelper.equals(request.getKeyHandle(), deviceRegistration.getDeviceRegistrationConfiguration().getKeyHandle())) {
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
				Base64Util.base64urldecode(usedDeviceRegistration.getDeviceRegistrationConfiguration().getPublicKey()));
		rawAuthenticateResponse.checkUserPresence();
		usedDeviceRegistration.checkAndUpdateCounter(rawAuthenticateResponse.getCounter());

		deviceRegistrationService.updateDeviceRegistration(userInum, usedDeviceRegistration);

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
		Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
		final String authenticateRequestMessageId = UUID.randomUUID().toString();

		AuthenticateRequestMessageLdap authenticateRequestMessageLdap = new AuthenticateRequestMessageLdap(requestMessage);
		authenticateRequestMessageLdap.setCreationDate(now);
		authenticateRequestMessageLdap.setId(authenticateRequestMessageId);
		authenticateRequestMessageLdap.setDn(getDnForAuthenticateRequestMessage(authenticateRequestMessageId));

		ldapEntryManager.persist(authenticateRequestMessageLdap);
	}

	public AuthenticateRequestMessage getAuthenticationRequestMessage(String oxId) {
		String requestDn = getDnForAuthenticateRequestMessage(oxId);

		AuthenticateRequestMessageLdap authenticateRequestMessageLdap = ldapEntryManager.find(AuthenticateRequestMessageLdap.class, requestDn);
		if (authenticateRequestMessageLdap == null) {
			return null;
		}

		return authenticateRequestMessageLdap.getAuthenticateRequestMessage();
	}

	public AuthenticateRequestMessageLdap getAuthenticationRequestMessageByRequestId(String requestId) {
		String baseDn = getDnForAuthenticateRequestMessage(null);
		Filter requestIdFilter = Filter.createEqualityFilter("oxRequestId", requestId);

		List<AuthenticateRequestMessageLdap> authenticateRequestMessagesLdap = ldapEntryManager.findEntries(baseDn, AuthenticateRequestMessageLdap.class,
				requestIdFilter);
		if ((authenticateRequestMessagesLdap == null) || authenticateRequestMessagesLdap.isEmpty()) {
			return null;
		}

		return authenticateRequestMessagesLdap.get(0);
	}

	public void removeAuthenticationRequestMessage(AuthenticateRequestMessageLdap authenticateRequestMessageLdap) {
		removeRequestMessage(authenticateRequestMessageLdap);
	}

	/**
	 * Build DN string for U2F authentication request
	 */
	public String getDnForAuthenticateRequestMessage(String oxId) {
		final String u2fBaseDn = ConfigurationFactory.instance().getBaseDn().getU2fBase(); // ou=authentication_requests,ou=u2f,o=@!1111,o=gluu
		if (StringHelper.isEmpty(oxId)) {
			return String.format("ou=authentication_requests,%s", u2fBaseDn);
		}

		return String.format("oxid=%s,ou=authentication_requests,%s", oxId, u2fBaseDn);
	}

}
