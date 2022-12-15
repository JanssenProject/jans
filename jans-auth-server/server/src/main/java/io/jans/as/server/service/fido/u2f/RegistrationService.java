/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.fido.u2f;

import io.jans.as.common.service.common.UserService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.fido.u2f.DeviceRegistrationStatus;
import io.jans.as.model.fido.u2f.exception.BadInputException;
import io.jans.as.model.fido.u2f.message.RawRegisterResponse;
import io.jans.as.model.fido.u2f.protocol.ClientData;
import io.jans.as.model.util.Base64Util;
import io.jans.as.server.crypto.random.ChallengeGenerator;
import io.jans.as.server.exception.fido.u2f.DeviceCompromisedException;
import io.jans.as.server.model.fido.u2f.DeviceRegistration;
import io.jans.as.server.model.fido.u2f.DeviceRegistrationResult;
import io.jans.as.server.model.fido.u2f.RegisterRequestMessageLdap;
import io.jans.as.server.model.fido.u2f.RequestMessageLdap;
import io.jans.as.server.util.ServerUtil;
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
    @Named("randomChallengeGenerator")
    private ChallengeGenerator challengeGenerator;

    @Inject
    private StaticConfiguration staticConfiguration;

    public io.jans.as.model.fido.u2f.protocol.RegisterRequestMessage builRegisterRequestMessage(String appId, String userInum) {
        if (applicationService.isValidateApplication()) {
            applicationService.checkIsValid(appId);
        }

        List<io.jans.as.model.fido.u2f.protocol.AuthenticateRequest> authenticateRequests = new ArrayList<io.jans.as.model.fido.u2f.protocol.AuthenticateRequest>();
        List<io.jans.as.model.fido.u2f.protocol.RegisterRequest> registerRequests = new ArrayList<io.jans.as.model.fido.u2f.protocol.RegisterRequest>();

        boolean twoStep = StringHelper.isNotEmpty(userInum);
        if (twoStep) {
            // In two steps we expects not empty userInum
            List<DeviceRegistration> deviceRegistrations = deviceRegistrationService.findUserDeviceRegistrations(userInum, appId);
            for (DeviceRegistration deviceRegistration : deviceRegistrations) {
                if (!deviceRegistration.isCompromised()) {
                    try {
                        io.jans.as.model.fido.u2f.protocol.AuthenticateRequest authenticateRequest = u2fAuthenticationService.startAuthentication(appId, deviceRegistration);
                        authenticateRequests.add(authenticateRequest);
                    } catch (DeviceCompromisedException ex) {
                        log.error("Faield to authenticate device", ex);
                    }
                }
            }
        }

        io.jans.as.model.fido.u2f.protocol.RegisterRequest request = startRegistration(appId);
        registerRequests.add(request);

        return new io.jans.as.model.fido.u2f.protocol.RegisterRequestMessage(authenticateRequests, registerRequests);
    }

    public io.jans.as.model.fido.u2f.protocol.RegisterRequest startRegistration(String appId) {
        return startRegistration(appId, challengeGenerator.generateChallenge());
    }

    public io.jans.as.model.fido.u2f.protocol.RegisterRequest startRegistration(String appId, byte[] challenge) {
        return new io.jans.as.model.fido.u2f.protocol.RegisterRequest(Base64Util.base64urlencode(challenge), appId);
    }

    public DeviceRegistrationResult finishRegistration(io.jans.as.model.fido.u2f.protocol.RegisterRequestMessage requestMessage, io.jans.as.model.fido.u2f.protocol.RegisterResponse response, String userInum) throws BadInputException {
        return finishRegistration(requestMessage, response, userInum, null);
    }

    public DeviceRegistrationResult finishRegistration(io.jans.as.model.fido.u2f.protocol.RegisterRequestMessage requestMessage, io.jans.as.model.fido.u2f.protocol.RegisterResponse response, String userInum, Set<String> facets)
            throws BadInputException {
        io.jans.as.model.fido.u2f.protocol.RegisterRequest request = requestMessage.getRegisterRequest();
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
                io.jans.as.model.fido.u2f.protocol.DeviceData deviceData = ServerUtil.jsonMapperWithWrapRoot().readValue(responseDeviceDataDecoded, io.jans.as.model.fido.u2f.protocol.DeviceData.class);
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

    public RequestMessageLdap storeRegisterRequestMessage(io.jans.as.model.fido.u2f.protocol.RegisterRequestMessage requestMessage, String userInum, String sessionId) {
        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
        final String registerRequestMessageId = UUID.randomUUID().toString();

        RequestMessageLdap registerRequestMessageLdap = new RegisterRequestMessageLdap(getDnForRegisterRequestMessage(registerRequestMessageId),
                registerRequestMessageId, now, sessionId, userInum, requestMessage);

        ldapEntryManager.persist(registerRequestMessageLdap);
        return registerRequestMessageLdap;
    }

    public io.jans.as.model.fido.u2f.protocol.RegisterRequestMessage getRegisterRequestMessage(String jsId) {
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
