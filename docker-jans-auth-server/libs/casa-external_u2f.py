from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.client.fido.u2f import FidoU2fClientFactory
from io.jans.as.model.config import Constants
from io.jans.as.server.security import Identity
from io.jans.as.server.service import AuthenticationService, SessionIdService, UserService
from io.jans.as.server.service.fido.u2f import DeviceRegistrationService
from io.jans.as.server.util import ServerUtil
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper

from javax.ws.rs import ClientErrorException, WebApplicationException
from javax.ws.rs.core import Response

import java
import sys


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "U2F. Initialization"

        print "U2F. Initialization. Downloading U2F metadata"
        u2f_server_uri = configurationAttributes.get("u2f_server_uri").getValue2()
        u2f_server_metadata_uri = u2f_server_uri + "/.well-known/fido-configuration"
        #u2f_server_metadata_uri = u2f_server_uri + "/oxauth/restv1/fido-configuration"

        metaDataConfigurationService = FidoU2fClientFactory.instance().createMetaDataConfigurationService(u2f_server_metadata_uri)

        max_attempts = 20
        for attempt in range(1, max_attempts + 1):
            try:
                self.metaDataConfiguration = metaDataConfigurationService.getMetadataConfiguration()
                break
            except WebApplicationException, ex:
                # Detect if last try or we still get Service Unavailable HTTP error
                if (attempt == max_attempts) or (ex.getResponse().getStatus() != Response.Status.SERVICE_UNAVAILABLE.getStatusCode()):
                    raise ex

                java.lang.Thread.sleep(3000)
                print "Attempting to load metadata: %d" % attempt

        print "U2F. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "U2F. Destroy"
        print "U2F. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    def getAuthenticationMethodClaims(self, configurationAttributes):
        return None

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        authenticationService = CdiUtil.bean(AuthenticationService)

        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()

        user_name = credentials.getUsername()

        if (step == 1):
            print "U2F. Authenticate for step 1"

            if authenticationService.getAuthenticatedUser() != None:
                return True

            user_password = credentials.getPassword()
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = CdiUtil.bean(UserService)
                logged_in = authenticationService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            return True
        elif (step == 2):
            print "U2F. Authenticate for step 2"

            token_response = ServerUtil.getFirstValue(requestParameters, "tokenResponse")
            if token_response == None:
                print "U2F. Authenticate for step 2. tokenResponse is empty"
                return False

            auth_method = ServerUtil.getFirstValue(requestParameters, "authMethod")
            if auth_method == None:
                print "U2F. Authenticate for step 2. authMethod is empty"
                return False

            authenticationService = CdiUtil.bean(AuthenticationService)
            user = authenticationService.getAuthenticatedUser()
            if (user == None):
                print "U2F. Prepare for step 2. Failed to determine user name"
                return False

            if (auth_method == 'authenticate'):
                print "U2F. Prepare for step 2. Call FIDO U2F in order to finish authentication workflow"
                authenticationRequestService = FidoU2fClientFactory.instance().createAuthenticationRequestService(self.metaDataConfiguration)
                authenticationStatus = authenticationRequestService.finishAuthentication(user.getUserId(), token_response)

                if (authenticationStatus.getStatus() != Constants.RESULT_SUCCESS):
                    print "U2F. Authenticate for step 2. Get invalid authentication status from FIDO U2F server"
                    return False

                return True
            elif (auth_method == 'enroll'):
                print "U2F. Prepare for step 2. Call FIDO U2F in order to finish registration workflow"
                registrationRequestService = FidoU2fClientFactory.instance().createRegistrationRequestService(self.metaDataConfiguration)
                registrationStatus = registrationRequestService.finishRegistration(user.getUserId(), token_response)

                if (registrationStatus.getStatus() != Constants.RESULT_SUCCESS):
                    print "U2F. Authenticate for step 2. Get invalid registration status from FIDO U2F server"
                    return False

                return True
            else:
                print "U2F. Prepare for step 2. Authenticatiod method is invalid"
                return False

            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)

        if (step == 1):
            return True
        elif (step == 2):
            print "U2F. Prepare for step 2"

            session_id = CdiUtil.bean(SessionIdService).getSessionId()
            if session_id == None:
                print "U2F. Prepare for step 2. Failed to determine session_id"
                return False

            authenticationService = CdiUtil.bean(AuthenticationService)
            user = authenticationService.getAuthenticatedUser()
            if (user == None):
                print "U2F. Prepare for step 2. Failed to determine user name"
                return False

            u2f_application_id = configurationAttributes.get("u2f_application_id").getValue2()

            # Check if user have registered devices
            deviceRegistrationService = CdiUtil.bean(DeviceRegistrationService)

            userInum = user.getAttribute("inum")

            registrationRequest = None
            authenticationRequest = None

            deviceRegistrations = deviceRegistrationService.findUserDeviceRegistrations(userInum, u2f_application_id)
            if (deviceRegistrations.size() > 0):
                print "U2F. Prepare for step 2. Call FIDO U2F in order to start authentication workflow"

                try:
                    authenticationRequestService = FidoU2fClientFactory.instance().createAuthenticationRequestService(self.metaDataConfiguration)
                    authenticationRequest = authenticationRequestService.startAuthentication(user.getUserId(), None, u2f_application_id, session_id.getId())
                except ClientErrorException, ex:
                    if (ex.getResponse().getResponseStatus() != Response.Status.NOT_FOUND):
                        print "U2F. Prepare for step 2. Failed to start authentication workflow. Exception:", sys.exc_info()[1]
                        return False
            else:
                print "U2F. Prepare for step 2. Call FIDO U2F in order to start registration workflow"
                registrationRequestService = FidoU2fClientFactory.instance().createRegistrationRequestService(self.metaDataConfiguration)
                registrationRequest = registrationRequestService.startRegistration(user.getUserId(), u2f_application_id, session_id.getId())

            identity.setWorkingParameter("fido_u2f_authentication_request", ServerUtil.asJson(authenticationRequest))
            identity.setWorkingParameter("fido_u2f_registration_request", ServerUtil.asJson(registrationRequest))

            return True
        elif (step == 3):
            print "U2F. Prepare for step 3"

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            #Modified for Casa compliance
            return "/casa/u2f.xhtml"

        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    # Added for Casa compliance

    def hasEnrollments(self, configurationAttributes, user):

        inum = user.getAttribute("inum")
        devRegService = CdiUtil.bean(DeviceRegistrationService)
        app_id = configurationAttributes.get("u2f_application_id").getValue2()
        userDevices = devRegService.findUserDeviceRegistrations(inum, app_id, "jansStatus")

        hasDevices = False
        for device in userDevices:
            if device.getStatus() != None and device.getStatus().getValue() == "active":
                hasDevices=True
                break

        return hasDevices
