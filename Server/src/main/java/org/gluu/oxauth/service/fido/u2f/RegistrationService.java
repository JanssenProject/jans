/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service.fido.u2f;

import org.gluu.oxauth.crypto.random.ChallengeGenerator;
import org.gluu.oxauth.exception.fido.u2f.DeviceCompromisedException;
import org.gluu.oxauth.model.fido.u2f.*;
import org.gluu.oxauth.model.fido.u2f.exception.BadInputException;
import org.gluu.oxauth.model.fido.u2f.message.RawRegisterResponse;
import org.gluu.oxauth.model.fido.u2f.protocol.*;
import org.gluu.oxauth.model.util.Base64Util;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.service.UserService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

/**
 * Provides operations with U2F registration requests
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@Stateless
@Named("u2fRegistrationService")
public class RegistrationService extends RequestService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private UserService userService;

    @Inject
    private AuthenticationService u2fAuthenticationService;

    @Inject
    private RawRegistrationService rawRegistrationService;

    @Inject
    private ClientDataValidationService clientDataValidationService;

    @Inject
    private DeviceRegistrationService deviceRegistrationService;

    @Inject
    @Named("randomChallengeGenerator")
    private ChallengeGenerator challengeGenerator;

    @Inject
    private StaticConfiguration staticConfiguration;

    public RegisterRequestMessage builRegisterRequestMessage(String appId, String userInum) {
        if (applicationService.isValidateApplication()) {
            applicationService.checkIsValid(appId);
        }

        List<AuthenticateRequest> authenticateRequests = new ArrayList<AuthenticateRequest>();
        List<RegisterRequest> registerRequests = new ArrayList<RegisterRequest>();

        boolean twoStep = StringHelper.isNotEmpty(userInum);
        if (twoStep) {
            // In two steps we expects not empty userInum
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

    public DeviceRegistrationResult finishRegistration(RegisterRequestMessage requestMessage, RegisterResponse response, String userInum) throws BadInputException {
        return finishRegistration(requestMessage, response, userInum, null);
    }

    public DeviceRegistrationResult finishRegistration(RegisterRequestMessage requestMessage, RegisterResponse response, String userInum, Set<String> facets)
            throws BadInputException {
        RegisterRequest request = requestMessage.getRegisterRequest();
        String appId = request.getAppId();

        ClientData clientData = response.getClientData();
        clientDataValidationService.checkContent(clientData, RawRegistrationService.SUPPORTED_REGISTER_TYPES, request.getChallenge(), facets);

        RawRegisterResponse rawRegisterResponse = rawRegistrationService.parseRawRegisterResponse(response.getRegistrationData());
        rawRegistrationService.checkSignature(appId, clientData, rawRegisterResponse);

        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        DeviceRegistration deviceRegistration = rawRegistrationService.createDevice(rawRegisterResponse);
        deviceRegistration.setStatus(DeviceRegistrationStatus.ACTIVE);
        deviceRegistration.setApplication(appId);
        deviceRegistration.setCreationDate(now);

        int keyHandleHashCode = deviceRegistrationService.getKeyHandleHashCode(rawRegisterResponse.getKeyHandle());
        deviceRegistration.setKeyHandleHashCode(keyHandleHashCode);

        final String deviceRegistrationId = String.valueOf(System.currentTimeMillis());
        deviceRegistration.setId(deviceRegistrationId);

        String responseDeviceData = response.getDeviceData();
        if (StringHelper.isNotEmpty(responseDeviceData)) {
            try {
                String responseDeviceDataDecoded = new String(Base64Util.base64urldecode(responseDeviceData));
                DeviceData deviceData = ServerUtil.jsonMapperWithWrapRoot().readValue(responseDeviceDataDecoded, DeviceData.class);
                deviceRegistration.setDeviceData(deviceData);
            } catch (Exception ex) {
                throw new BadInputException(String.format("Device data is invalid: %s", responseDeviceData), ex);
            }
        }

        boolean approved = StringHelper.equals(RawRegistrationService.REGISTER_FINISH_TYPE, response.getClientData().getTyp());
        if (!approved) {
            log.debug("Registratio request with keyHandle '{}' was canceled", rawRegisterResponse.getKeyHandle());
            return new DeviceRegistrationResult(deviceRegistration, DeviceRegistrationResult.Status.CANCELED);
        }

        boolean twoStep = StringHelper.isNotEmpty(userInum);
        if (twoStep) {
            deviceRegistration.setDn(deviceRegistrationService.getDnForU2fDevice(userInum, deviceRegistrationId));

            // Check if there is device registration with keyHandle in LDAP already
            List<DeviceRegistration> foundDeviceRegistrations = deviceRegistrationService.findDeviceRegistrationsByKeyHandle(appId, deviceRegistration.getKeyHandle(), "oxId");
            if (foundDeviceRegistrations.size() != 0) {
                throw new BadInputException(String.format("KeyHandle %s was compromised", deviceRegistration.getKeyHandle()));
            }

            deviceRegistrationService.addUserDeviceRegistration(userInum, deviceRegistration);
        } else {
            deviceRegistration.setDn(deviceRegistrationService.getDnForOneStepU2fDevice(deviceRegistrationId));

            deviceRegistrationService.addOneStepDeviceRegistration(deviceRegistration);
        }

        return new DeviceRegistrationResult(deviceRegistration, DeviceRegistrationResult.Status.APPROVED);
    }

    public RequestMessageLdap storeRegisterRequestMessage(RegisterRequestMessage requestMessage, String userInum, String sessionId) {
        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        final String registerRequestMessageId = UUID.randomUUID().toString();

        RequestMessageLdap registerRequestMessageLdap = new RegisterRequestMessageLdap(getDnForRegisterRequestMessage(registerRequestMessageId),
                registerRequestMessageId, now, sessionId, userInum, requestMessage);

        ldapEntryManager.persist(registerRequestMessageLdap);
        return registerRequestMessageLdap;
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
        final String u2fBaseDn = staticConfiguration.getBaseDn().getU2fBase(); // ou=registration_requests,ou=u2f,o=gluu
        if (StringHelper.isEmpty(oxId)) {
            return String.format("ou=registration_requests,%s", u2fBaseDn);
        }

        return String.format("oxid=%s,ou=registration_requests,%s", oxId, u2fBaseDn);
    }

    public void merge(RequestMessageLdap request) {
        ldapEntryManager.merge(request);
    }
}
