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
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistrationStatus;
import org.xdi.oxauth.model.fido.u2f.RegisterRequestMessageLdap;
import org.xdi.oxauth.model.fido.u2f.RequestMessageLdap;
import org.xdi.oxauth.model.fido.u2f.exception.BadInputException;
import org.xdi.oxauth.model.fido.u2f.message.RawRegisterResponse;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateRequest;
import org.xdi.oxauth.model.fido.u2f.protocol.ClientData;
import org.xdi.oxauth.model.fido.u2f.protocol.RegisterRequest;
import org.xdi.oxauth.model.fido.u2f.protocol.RegisterRequestMessage;
import org.xdi.oxauth.model.fido.u2f.protocol.RegisterResponse;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.service.UserService;
import org.xdi.util.StringHelper;

import com.unboundid.ldap.sdk.Filter;

/**
 * Provides operations with U2F registration requests
 *
 * @author Yuriy Movchan Date: 05/19/2015
 */
@Scope(ScopeType.STATELESS)
@Name("u2fRegistrationService")
@AutoCreate
public class RegistrationService extends RequestService {

	@Logger
	private Log log;

	@In
	private LdapEntryManager ldapEntryManager;

	@In
	private ApplicationService applicationService;

	@In
	private UserService userService;

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

	public RegisterRequestMessage builRegisterRequestMessage(String appId, String userName) {
		if (applicationService.isValidateApplication()) {
			applicationService.checkIsValid(appId);
		}

		String userInum = userService.getUserInum(userName);
		if (StringHelper.isEmpty(userInum)) {
			throw new BadInputException(String.format("Failed to find user '%s' in LDAP", userName));
		}

		List<AuthenticateRequest> authenticateRequests = new ArrayList<AuthenticateRequest>();
		List<RegisterRequest> registerRequests = new ArrayList<RegisterRequest>();

		List<DeviceRegistration> deviceRegistrations = deviceRegistrationService.findUserDeviceRegistrations(userInum, appId);
		for (DeviceRegistration deviceRegistration : deviceRegistrations) {
			if (!deviceRegistration.isCompromised()) {
				try {
					AuthenticateRequest authenticateRequest = u2fAuthenticationService.startAuthentication(appId, deviceRegistration);
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
		String userInum = userService.getUserInum(userName);
		if (StringHelper.isEmpty(userInum)) {
			throw new BadInputException(String.format("Failed to find user '%s' in LDAP", userName));
		}

		RegisterRequest request = requestMessage.getRegisterRequest();
		String appId = request.getAppId();

		ClientData clientData = response.getClientData();
		clientDataValidationService.checkContent(clientData, RawRegistrationService.REGISTER_TYPE, request.getChallenge(), facets);

		RawRegisterResponse rawRegisterResponse = rawRegistrationService.parseRawRegisterResponse(response.getRegistrationData());
		rawRegistrationService.checkSignature(appId, clientData, rawRegisterResponse);

		Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
		DeviceRegistration deviceRegistration = rawRegistrationService.createDevice(rawRegisterResponse);
		deviceRegistration.setStatus(DeviceRegistrationStatus.ACTIVE);
		deviceRegistration.setApplication(appId);
		deviceRegistration.setCreationDate(now);

		final String deviceRegistrationId = String.valueOf(System.currentTimeMillis());
		deviceRegistration.setId(deviceRegistrationId);
		deviceRegistration.setDn(deviceRegistrationService.getDnForU2fDevice(deviceRegistrationId, userInum));

		deviceRegistrationService.addUserDeviceRegistration(userInum, deviceRegistration);

		return deviceRegistration;
	}

	public void storeRegisterRequestMessage(RegisterRequestMessage requestMessage) {
		Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
		final String registerRequestMessageId = UUID.randomUUID().toString();

		RequestMessageLdap registerRequestMessageLdap = new RegisterRequestMessageLdap(requestMessage);
		registerRequestMessageLdap.setCreationDate(now);
		registerRequestMessageLdap.setId(registerRequestMessageId);
		registerRequestMessageLdap.setDn(getDnForRegisterRequestMessage(registerRequestMessageId));

		ldapEntryManager.persist(registerRequestMessageLdap);
	}

	public RegisterRequestMessage getRegisterRequestMessage(String oxId) {
		String requestDn = getDnForRegisterRequestMessage(oxId);

		RegisterRequestMessageLdap registerRequestMessageLdap = ldapEntryManager.find(RegisterRequestMessageLdap.class, requestDn);
		if (registerRequestMessageLdap == null) {
			return null;
		}

		return registerRequestMessageLdap.getRegisterRequestMessage();
	}

	public RegisterRequestMessageLdap getRegisterRequestMessageByRequestId(String requestId) {
		String baseDn = getDnForRegisterRequestMessage(null);
		Filter requestIdFilter = Filter.createEqualityFilter("oxRequestId", requestId);

		List<RegisterRequestMessageLdap> registerRequestMessagesLdap = ldapEntryManager.findEntries(baseDn, RegisterRequestMessageLdap.class,
				requestIdFilter);
		if ((registerRequestMessagesLdap == null) || registerRequestMessagesLdap.isEmpty()) {
			return null;
		}

		return registerRequestMessagesLdap.get(0);
	}

	public void removeRegisterRequestMessage(RequestMessageLdap registerRequestMessageLdap) {
		removeRequestMessage(registerRequestMessageLdap);
	}

	/**
	 * Build DN string for U2F register request
	 */
	public String getDnForRegisterRequestMessage(String oxId) {
		final String u2fBaseDn = ConfigurationFactory.instance().getBaseDn().getU2fBase(); // ou=registration_requests,ou=u2f,o=@!1111,o=gluu
		if (StringHelper.isEmpty(oxId)) {
			return String.format("ou=registration_requests,%s", u2fBaseDn);
		}

		return String.format("oxid=%s,ou=registration_requests,%s", oxId, u2fBaseDn);
	}

}
