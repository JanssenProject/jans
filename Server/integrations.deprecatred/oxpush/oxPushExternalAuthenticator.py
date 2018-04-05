# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

import java
from java.util import Arrays
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.security import Identity
from org.xdi.oxauth.service import UserService, AuthenticationService
from org.xdi.oxpush import OxPushClient
from org.xdi.service.cdi.util import CdiUtil
from org.xdi.util import StringHelper


class PersonAuthentication(PersonAuthenticationType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "oxPush. Initialization"

        oxpush_server_base_uri = configurationAttributes.get("oxpush_server_base_uri").getValue2()
        self.oxPushClient = OxPushClient(oxpush_server_base_uri)
        print "oxPush. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "oxPush. Destroy"
        print "oxPush. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)

        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()

        oxpush_user_timeout = int(configurationAttributes.get("oxpush_user_timeout").getValue2())
        oxpush_application_name = configurationAttributes.get("oxpush_application_name").getValue2()

        user_name = credentials.getUsername()

        if (step == 1):
            print "oxPush. Authenticate for step 1"

            user_password = credentials.getPassword()
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = CdiUtil.bean(UserService)
                logged_in = authenticationService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            # Get user entry
            userService = CdiUtil.bean(UserService)
            find_user_by_uid = authenticationService.getAuthenticatedUser()
            if (find_user_by_uid == None):
                print "oxPush. Authenticate for step 1. Failed to find user"
                return False

            # Check if the user paired account to phone
            user_external_uid_attr = userService.getCustomAttribute(find_user_by_uid, "oxExternalUid")
            if ((user_external_uid_attr == None) or (user_external_uid_attr.getValues() == None)):
                print "oxPush. Authenticate for step 1. There is no external UIDs for user: ", user_name
            else:
                oxpush_user_uid = None
                for ext_uid in user_external_uid_attr.getValues():
                    if (ext_uid.startswith('oxpush:')):
                        oxpush_user_uid = ext_uid[7:len(ext_uid)]
                        break
            
                if (oxpush_user_uid == None):
                    print "oxPush. Authenticate for step 1. There is no oxPush UID for user: ", user_name
                else:
                    # Check deployment status
                    print "oxPush. Authenticate for step 1. oxpush_user_uid: ", oxpush_user_uid
                    deployment_status = self.oxPushClient.getDeploymentStatus(oxpush_user_uid) 
                    if (deployment_status.result):
                        print "oxPush. Authenticate for step 1. Deployment status is valid"
                        if ("enabled" == deployment_status.status):
                            print "oxPush. Authenticate for step 1. Deployment is enabled"
                            identity.setWorkingParameter("oxpush_user_uid", oxpush_user_uid)
                        else:
                            print "oxPush. Authenticate for step 1. Deployment is disabled"
                            return False
                    else:
                        print "oxPush. Authenticate for step 1. Deployment status is invalid. Force user to pair again"
                        # Remove oxpush_user_uid from user entry
                        find_user_by_uid = userService.removeUserAttribute(user_name, "oxExternalUid", "oxpush:" + oxpush_user_uid)
                        if (find_user_by_uid == None):
                            print "oxPush. Authenticate for step 1. Failed to update current user"
                            return False

            return True
        elif (step == 2):
            print "oxPush. Authenticate for step 2"

            passed_step1 = self.isPassedDefaultAuthentication
            if (not passed_step1):
                return False

            sessionAttributes = identity.getSessionId().getSessionAttributes()
            if (sessionAttributes == None) or not sessionAttributes.containsKey("oxpush_user_uid"):
                print "oxPush. Authenticate for step 2. oxpush_user_uid is empty"

                if (not sessionAttributes.containsKey("oxpush_pairing_uid")):
                    print "oxPush. Authenticate for step 2. oxpush_pairing_uid is empty"
                    return False

                oxpush_pairing_uid = sessionAttributes.get("oxpush_pairing_uid")

                # Check pairing status                
                pairing_status = self.checkStatus("pair", oxpush_pairing_uid, oxpush_user_timeout)
                if (pairing_status == None):
                    print "oxPush. Authenticate for step 2. The pairing has not been authorized by user"
                    return False

                oxpush_user_uid = pairing_status.deploymentId

                print "oxPush. Authenticate for step 2. Storing oxpush_user_uid in user entry", oxpush_user_uid

                # Store oxpush_user_uid in user entry
                find_user_by_uid = userService.addUserAttribute(user_name, "oxExternalUid", "oxpush:" + oxpush_user_uid)
                if (find_user_by_uid == None):
                    print "oxPush. Authenticate for step 2. Failed to update current user"
                    return False

                identity.setWorkingParameter("oxpush_count_login_steps", 2)
                identity.setWorkingParameter("oxpush_user_uid", oxpush_user_uid)
            else:
                print "oxPush. Authenticate for step 2. Deployment status is valid"

            return True
        elif (step == 3):
            print "oxPush. Authenticate for step 3"

            passed_step1 = self.isPassedDefaultAuthentication
            if (not passed_step1):
                return False

            sessionAttributes = identity.getWorkingParameter("oxpush_user_uid")
            if (sessionAttributes == None) or not sessionAttributes.containsKey("oxpush_user_uid"):
                print "oxPush. Authenticate for step 3. oxpush_user_uid is empty"
                return False

            oxpush_user_uid = sessionAttributes.get("oxpush_user_uid")
            passed_step1 = StringHelper.isNotEmptyString(oxpush_user_uid)
            if (not passed_step1):
                return False

            # Initialize authentication process
            authentication_request = None
            try:
                authentication_request = self.oxPushClient.authenticate(oxpush_user_uid, user_name)
            except java.lang.Exception, err:
                print "oxPush. Authenticate for step 3. Failed to initialize authentication process: ", err
                return False

            if (not authentication_request.result):
                print "oxPush. Authenticate for step 3. Failed to initialize authentication process"
                return False

            # Check authentication status                
            authentication_status = self.checkStatus("authenticate", authentication_request.authenticationId, oxpush_user_timeout)
            if (authentication_status == None):
                print "oxPush. Authenticate for step 3. The authentication has not been authorized by user"
                return False
                
            print "oxPush. Authenticate for step 3. The request was granted"

            return True
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)

        oxpush_application_name = configurationAttributes.get("oxpush_application_name").getValue2()

        if (step == 1):
            print "oxPush. Prepare for step 1"
            oxpush_android_download_url = configurationAttributes.get("oxpush_android_download_url").getValue2()
            identity.setWorkingParameter("oxpush_android_download_url", oxpush_android_download_url)
        elif (step == 2):
            print "oxPush. Prepare for step 2"

            passed_step1 = self.isPassedDefaultAuthentication
            if (not passed_step1):
                return False

            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            user_name = credentials.getUsername()

            sessionAttributes = identity.getSessionId().getSessionAttributes()
            if (sessionAttributes == None) or not sessionAttributes.containsKey("oxpush_user_uid"):
                print "oxPush. Prepare for step 2. oxpush_user_uid is empty"

                # Initialize pairing process
                pairing_process = None
                try:
                    pairing_process = self.oxPushClient.pair(oxpush_application_name, user_name)
                except java.lang.Exception, err:
                    print "oxPush. Prepare for step 2. Failed to initialize pairing process: ", err
                    return False

                if (not pairing_process.result):
                    print "oxPush. Prepare for step 2. Failed to initialize pairing process"
                    return False

                pairing_id = pairing_process.pairingId
                print "oxPush. Prepare for step 2. Pairing Id: ", pairing_id
    
                identity.setWorkingParameter("oxpush_pairing_uid", pairing_id)
                identity.setWorkingParameter("oxpush_pairing_code", pairing_process.pairingCode)
                identity.setWorkingParameter("oxpush_pairing_qr_image", pairing_process.pairingQrImage)

        return True

    def getExtraParametersForStep(self, configurationAttributes, step):
        if (step in [2, 3]):
            return Arrays.asList("oxpush_user_uid", "oxpush_pairing_uid")
        
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        identity = CdiUtil.bean(Identity)
        if (identity.isSetWorkingParameter("oxpush_count_login_steps")):
            return identity.getWorkingParameter("oxpush_count_login_steps")

        return 3

    def getPageForStep(self, configurationAttributes, step):
        if (step == 1):
            return "/auth/oxpush/oxlogin.xhtml"
        elif (step == 2):
            return "/auth/oxpush/oxpair.xhtml"
        elif (step == 3):
            return "/auth/oxpush/oxauthenticate.xhtml"
        return ""

    def isPassedDefaultAuthentication():
        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()

        user_name = credentials.getUsername()
        passed_step1 = StringHelper.isNotEmptyString(user_name)

        return passed_step1

    def checkStatus(self, mode, request_id, timeout):
        try:
            curTime = java.lang.System.currentTimeMillis()
            endTime = curTime + timeout * 1000
            while (endTime >= curTime):
                response_status = None
                if (StringHelper.equals("pair", mode)):
                    response_status = self.oxPushClient.getPairingStatus(request_id)
                else:
                    response_status = self.oxPushClient.getAuthenticationStatus(request_id)

                if (not response_status.result):
                    print "oxPush. CheckStatus. Get false result from oxPushServer"
                    return None

                status = response_status.status

                if ("declined" == status):
                    print "oxPush. CheckStatus. The process has been cancelled"
                    return None

                if ("expired" == status):
                    print "oxPush. CheckStatus. The process has been expired"
                    return None

                if ("approved" == status):
                    print "oxPush. CheckStatus. The process was approved"
                    return response_status

                java.lang.Thread.sleep(2000)
                curTime = java.lang.System.currentTimeMillis()
        except java.lang.Exception, err:
            print "oxPush. CheckStatus. Could not check process status: ", err
            return None

        print "oxPush. CheckStatus. The process has not received a response from the phone yet"

        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
