/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.u2f;

import io.jans.as.common.service.common.UserService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.fido2.model.u2f.DeviceRegistration;
import io.jans.fido2.model.u2f.DeviceRegistrationResult;
import io.jans.fido2.model.u2f.DeviceRegistrationStatus;
import io.jans.fido2.model.u2f.RegisterRequestMessageLdap;
import io.jans.fido2.model.u2f.RequestMessageLdap;
import io.jans.fido2.model.u2f.exception.BadInputException;
import io.jans.fido2.model.u2f.exception.DeviceCompromisedException;
import io.jans.fido2.model.u2f.message.RawRegisterResponse;
import io.jans.fido2.model.u2f.protocol.AuthenticateRequest;
import io.jans.fido2.model.u2f.protocol.ClientData;
import io.jans.fido2.model.u2f.protocol.DeviceData;
import io.jans.fido2.model.u2f.protocol.RegisterRequest;
import io.jans.fido2.model.u2f.protocol.RegisterRequestMessage;
import io.jans.fido2.model.u2f.protocol.RegisterResponse;
import io.jans.fido2.model.u2f.util.ServerUtil;
import io.jans.fido2.service.ChallengeGenerator;
import io.jans.as.model.util.Base64Util;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import org.slf4j.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Provides operations with U2F registration requests
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@ApplicationScoped
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
        return startRegistration(appId, challengeGenerator.getChallenge().getBytes());
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
        DeviceRegistration deviceRegistration = rawRegistrationService.createDevice(userInum, rawRegisterResponse);
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
            List<DeviceRegistration> foundDeviceRegistrations = deviceRegistrationService.findDeviceRegistrationsByKeyHandle(appId, deviceRegistration.getKeyHandle(), "jansId");
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

    public RegisterRequestMessage getRegisterRequestMessage(String jsId) {
        String requestDn = getDnForRegisterRequestMessage(jsId);

        RegisterRequestMessageLdap registerRequestMessageLdap = ldapEntryManager.find(RegisterRequestMessageLdap.class, requestDn);
        if (registerRequestMessageLdap == null) {
            return null;
        }

        return registerRequestMessageLdap.getRegisterRequestMessage();
    }

    public RegisterRequestMessageLdap getRegisterRequestMessageByRequestId(String requestId) {
        String baseDn = getDnForRegisterRequestMessage(null);
        Filter requestIdFilter = Filter.createEqualityFilter("jansReqId", requestId);

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
    public String getDnForRegisterRequestMessage(String jsId) {
        final String u2fBaseDn = staticConfiguration.getBaseDn().getU2fBase(); // ou=registration_requests,ou=u2f,o=jans
        if (StringHelper.isEmpty(jsId)) {
            return String.format("ou=registration_requests,%s", u2fBaseDn);
        }

        return String.format("jansId=%s,ou=registration_requests,%s", jsId, u2fBaseDn);
    }

    public void merge(RequestMessageLdap request) {
        ldapEntryManager.merge(request);
    }
}
