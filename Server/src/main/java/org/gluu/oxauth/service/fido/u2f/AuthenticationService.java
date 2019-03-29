/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service.fido.u2f;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.oxauth.crypto.random.ChallengeGenerator;
import org.gluu.oxauth.exception.fido.u2f.DeviceCompromisedException;
import org.gluu.oxauth.exception.fido.u2f.InvalidKeyHandleDeviceException;
import org.gluu.oxauth.exception.fido.u2f.NoEligableDevicesException;
import org.gluu.oxauth.model.fido.u2f.AuthenticateRequestMessageLdap;
import org.gluu.oxauth.model.fido.u2f.DeviceRegistration;
import org.gluu.oxauth.model.fido.u2f.DeviceRegistrationResult;
import org.gluu.oxauth.model.fido.u2f.exception.BadInputException;
import org.gluu.oxauth.model.fido.u2f.message.RawAuthenticateResponse;
import org.gluu.oxauth.model.fido.u2f.protocol.AuthenticateRequest;
import org.gluu.oxauth.model.fido.u2f.protocol.AuthenticateRequestMessage;
import org.gluu.oxauth.model.fido.u2f.protocol.AuthenticateResponse;
import org.gluu.oxauth.model.fido.u2f.protocol.ClientData;
import org.gluu.oxauth.model.util.Base64Util;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.service.UserService;

/**
 * Provides operations with U2F authentication request
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@Stateless
@Named("u2fAuthenticationService")
public class AuthenticationService extends RequestService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private RawAuthenticationService rawAuthenticationService;

    @Inject
    private ClientDataValidationService clientDataValidationService;

    @Inject
    private DeviceRegistrationService deviceRegistrationService;

    @Inject
    private UserService userService;

    @Inject
    @Named("randomChallengeGenerator")
    private ChallengeGenerator challengeGenerator;

    @Inject
    private StaticConfiguration staticConfiguration;

    public AuthenticateRequestMessage buildAuthenticateRequestMessage(String appId, String userInum) throws BadInputException, NoEligableDevicesException {
        if (applicationService.isValidateApplication()) {
            applicationService.checkIsValid(appId);
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

        return new AuthenticateRequest(Base64Util.base64urlencode(challenge), appId, device.getKeyHandle());
    }

    public DeviceRegistrationResult finishAuthentication(AuthenticateRequestMessage requestMessage, AuthenticateResponse response, String userInum)
            throws BadInputException, DeviceCompromisedException {
        return finishAuthentication(requestMessage, response, userInum, null);
    }

    public DeviceRegistrationResult finishAuthentication(AuthenticateRequestMessage requestMessage, AuthenticateResponse response, String userInum, Set<String> facets)
            throws BadInputException, DeviceCompromisedException {
        List<DeviceRegistration> deviceRegistrations = deviceRegistrationService.findUserDeviceRegistrations(userInum, requestMessage.getAppId());

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
        clientDataValidationService.checkContent(clientData, RawAuthenticationService.SUPPORTED_AUTHENTICATE_TYPES, request.getChallenge(), facets);

        RawAuthenticateResponse rawAuthenticateResponse = rawAuthenticationService.parseRawAuthenticateResponse(response.getSignatureData());
        rawAuthenticationService.checkSignature(request.getAppId(), clientData, rawAuthenticateResponse,
                Base64Util.base64urldecode(usedDeviceRegistration.getDeviceRegistrationConfiguration().getPublicKey()));
        rawAuthenticateResponse.checkUserPresence();
        usedDeviceRegistration.checkAndUpdateCounter(rawAuthenticateResponse.getCounter());

        usedDeviceRegistration.setLastAccessTime(new Date());

        deviceRegistrationService.updateDeviceRegistration(userInum, usedDeviceRegistration);

        DeviceRegistrationResult.Status status = DeviceRegistrationResult.Status.APPROVED;

        boolean approved = StringHelper.equals(RawAuthenticationService.AUTHENTICATE_GET_TYPE, clientData.getTyp());
        if (!approved) {
            status = DeviceRegistrationResult.Status.CANCELED;
            log.debug("Authentication request with keyHandle '{}' was canceled", response.getKeyHandle());
        }

        return new DeviceRegistrationResult(usedDeviceRegistration, status);
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

    public void storeAuthenticationRequestMessage(AuthenticateRequestMessage requestMessage, String userInum, String sessionId) {
        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        final String authenticateRequestMessageId = UUID.randomUUID().toString();

        AuthenticateRequestMessageLdap authenticateRequestMessageLdap = new AuthenticateRequestMessageLdap(getDnForAuthenticateRequestMessage(authenticateRequestMessageId),
                authenticateRequestMessageId, now, sessionId, userInum, requestMessage);

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

    public String getUserInumByKeyHandle(String appId, String keyHandle) throws InvalidKeyHandleDeviceException {
        if (org.gluu.util.StringHelper.isEmpty(appId) || StringHelper.isEmpty(keyHandle)) {
            return null;
        }

        List<DeviceRegistration> deviceRegistrations = deviceRegistrationService.findDeviceRegistrationsByKeyHandle(appId, keyHandle, "oxId");
        if (deviceRegistrations.isEmpty()) {
            throw new InvalidKeyHandleDeviceException(String.format("Failed to find device by keyHandle '%s' in LDAP", keyHandle));
        }

        if (deviceRegistrations.size() != 1) {
            throw new BadInputException(String.format("There are '%d' devices with keyHandle '%s' in LDAP", deviceRegistrations.size(), keyHandle));
        }

        DeviceRegistration deviceRegistration = deviceRegistrations.get(0);

        return userService.getUserInumByDn(deviceRegistration.getDn());
    }

    /**
     * Build DN string for U2F authentication request
     */
    public String getDnForAuthenticateRequestMessage(String oxId) {
        final String u2fBaseDn = staticConfiguration.getBaseDn().getU2fBase(); // ou=authentication_requests,ou=u2f,o=gluu
        if (StringHelper.isEmpty(oxId)) {
            return String.format("ou=authentication_requests,%s", u2fBaseDn);
        }

        return String.format("oxid=%s,ou=authentication_requests,%s", oxId, u2fBaseDn);
    }

}
