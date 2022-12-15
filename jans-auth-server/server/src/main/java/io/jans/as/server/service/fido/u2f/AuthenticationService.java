/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.fido.u2f;

import io.jans.as.common.service.common.UserService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.fido.u2f.exception.BadInputException;
import io.jans.as.model.fido.u2f.message.RawAuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.AuthenticateRequest;
import io.jans.as.model.fido.u2f.protocol.AuthenticateRequestMessage;
import io.jans.as.model.fido.u2f.protocol.AuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.ClientData;
import io.jans.as.model.util.Base64Util;
import io.jans.as.server.crypto.random.ChallengeGenerator;
import io.jans.as.server.crypto.signature.SHA256withECDSASignatureVerification;
import io.jans.as.server.exception.fido.u2f.DeviceCompromisedException;
import io.jans.as.server.exception.fido.u2f.InvalidKeyHandleDeviceException;
import io.jans.as.server.exception.fido.u2f.NoEligableDevicesException;
import io.jans.as.server.model.fido.u2f.AuthenticateRequestMessageLdap;
import io.jans.as.server.model.fido.u2f.DeviceRegistration;
import io.jans.as.server.model.fido.u2f.DeviceRegistrationResult;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import jakarta.ws.rs.Produces;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Provides operations with U2F authentication request
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@ApplicationScoped
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

    @Produces 
    @ApplicationScoped
    @Named("sha256withECDSASignatureVerification")
    public SHA256withECDSASignatureVerification getBouncyCastleSignatureVerification() {
        return new SHA256withECDSASignatureVerification();
    }

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
        log.debug("Client data HEX '{}'", Hex.encodeHexString(response.getClientDataRaw().getBytes()));
        log.debug("Signature data HEX '{}'", Hex.encodeHexString(response.getSignatureData().getBytes()));

        clientDataValidationService.checkContent(clientData, RawAuthenticationService.SUPPORTED_AUTHENTICATE_TYPES, request.getChallenge(), facets);

        RawAuthenticateResponse rawAuthenticateResponse = rawAuthenticationService.parseRawAuthenticateResponse(response.getSignatureData());
        rawAuthenticationService.checkSignature(request.getAppId(), clientData, rawAuthenticateResponse,
                Base64Util.base64urldecode(usedDeviceRegistration.getDeviceRegistrationConfiguration().getPublicKey()));
        rawAuthenticateResponse.checkUserPresence();

        log.debug("Counter in finish authentication request'{}', countr in database '{}'", rawAuthenticateResponse.getCounter(), usedDeviceRegistration.getCounter());
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

    public AuthenticateRequestMessage getAuthenticationRequestMessage(String jsId) {
        String requestDn = getDnForAuthenticateRequestMessage(jsId);

        AuthenticateRequestMessageLdap authenticateRequestMessageLdap = ldapEntryManager.find(AuthenticateRequestMessageLdap.class, requestDn);
        if (authenticateRequestMessageLdap == null) {
            return null;
        }

        return authenticateRequestMessageLdap.getAuthenticateRequestMessage();
    }

    public AuthenticateRequestMessageLdap getAuthenticationRequestMessageByRequestId(String requestId) {
        String baseDn = getDnForAuthenticateRequestMessage(null);
        Filter requestIdFilter = Filter.createEqualityFilter("jansReqId", requestId);

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
        if (io.jans.util.StringHelper.isEmpty(appId) || StringHelper.isEmpty(keyHandle)) {
            return null;
        }

        List<DeviceRegistration> deviceRegistrations = deviceRegistrationService.findDeviceRegistrationsByKeyHandle(appId, keyHandle, "jansId");
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
    public String getDnForAuthenticateRequestMessage(String jsId) {
        final String u2fBaseDn = staticConfiguration.getBaseDn().getU2fBase(); // ou=authentication_requests,ou=u2f,o=jans
        if (StringHelper.isEmpty(jsId)) {
            return String.format("ou=authentication_requests,%s", u2fBaseDn);
        }

        return String.format("jansId=%s,ou=authentication_requests,%s", jsId, u2fBaseDn);
    }

}
