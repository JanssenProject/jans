from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from org.jboss.seam import Component
from javax.faces.context import FacesContext
from org.jboss.seam import Component
from org.xdi.oxauth.service.python.interfaces import ExternalAuthenticatorType
from org.xdi.oxauth.service import UserService
from org.xdi.util.security import StringEncrypter 
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from java.util import Arrays
from com.toopher import ToopherAPI
from com.toopher import RequestError

import java
import sys
try:
    import json
except ImportError:
    import simplejson as json

class ExternalAuthenticator(ExternalAuthenticatorType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Toopher initialization"
        toopher_creds_file = configurationAttributes.get("toopher_creds_file").getValue2()

        # Load credentials from file
        f = open(toopher_creds_file, 'r')
        try:
            creds = json.loads(f.read())
        except:
            return False
        finally:
            f.close()

        consumer_key = creds["CONSUMER_KEY"]
        consumer_secret = creds["CONSUMER_SECRET"]
        try:
            stringEncrypter = StringEncrypter.defaultInstance()
            consumer_secret = stringEncrypter.decrypt(consumer_secret)
        except:
            return False

        self.tapi = ToopherAPI(consumer_key, consumer_secret)
        print "Toopher initialized successfully"

        return True

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        context = Contexts.getEventContext()
        userService = UserService.instance()

        stringEncrypter = StringEncrypter.defaultInstance()

        toopher_user_timeout = int(configurationAttributes.get("toopher_user_timeout").getValue2())

        credentials = Identity.instance().getCredentials()
        user_name = credentials.getUsername()

        if (step == 1):
            print "Toopher authenticate for step 1"

            user_password = credentials.getPassword()
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = UserService.instance()
                logged_in = userService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            # Find user by uid
            userService = UserService.instance()
            find_user_by_uid = userService.getUser(user_name)
            if (find_user_by_uid == None):
                print "Toopher authenticate for step 1. Failed to find user"
                return False

            # Check if the user paired account to phone
            user_external_uid_attr = userService.getCustomAttribute(find_user_by_uid, "oxExternalUid")
            if ((user_external_uid_attr == None) or (user_external_uid_attr.getValues() == None)):
                print "Toopher authenticate for step 1. There is no external UIDs for user: ", user_name
            else:
                topher_user_uid = None
                for ext_uid in user_external_uid_attr.getValues():
                    if (ext_uid.startswith('toopher:')):
                        topher_user_uid = ext_uid[8:len(ext_uid)]
                        break
            
                if (topher_user_uid == None):
                    print "Toopher authenticate for step 1. There is no Topher UID for user: ", user_name
                else:
                    context.set("toopher_user_uid", stringEncrypter.encrypt(topher_user_uid))

            return True
        elif (step == 2):
            print "Toopher authenticate for step 2"

            passed_step1 = self.isPassedDefaultAuthentication
            if (not passed_step1):
                return False

            toopher_user_uid_array = requestParameters.get("toopher_user_uid")
            
            if (ArrayHelper.isEmpty(toopher_user_uid_array) or StringHelper.isEmptyString(toopher_user_uid_array[0])):
                print "Toopher authenticate for step 2. toopher_user_uid is empty"

                # Pair with phone
                pairing_phrase_array = requestParameters.get("pairing_phrase")
                if ArrayHelper.isEmpty(pairing_phrase_array):
                    print "Toopher authenticate for step 2. pairing_phrase is empty"
                    return False
                
                pairing_phrase = pairing_phrase_array[0]
                try:
                    pairing_status = self.tapi.pair(pairing_phrase, user_name);
                    toopher_user_uid = pairing_status.id;
                except RequestError, err:
                    print "Toopher authenticate for step 2. Failed pair with phone: ", err
                    return False
                
                pairing_result = self.checkPairingStatus(toopher_user_uid, toopher_user_timeout) 

                if (not pairing_result):
                    print "Toopher authenticate for step 2. The pairing has not been authorized by the phone yet"
                    return False
                    
                print "Toopher authenticate for step 2. Storing toopher_user_uid in user entry", toopher_user_uid

                # Store toopher_user_uid in user entry
                find_user_by_uid = userService.addUserAttribute(user_name, "oxExternalUid", "toopher:" + toopher_user_uid)
                if (find_user_by_uid == None):
                    print "Toopher authenticate for step 2. Failed to update current user"
                    return False

                context.set("toopher_user_uid", stringEncrypter.encrypt(toopher_user_uid))
            else:
                toopher_user_uid = stringEncrypter.decrypt(toopher_user_uid_array[0])

                # Check pairing stastus
                print "Toopher authenticate for step 2. toopher_user_uid: ", toopher_user_uid
                pairing_result = self.checkPairingStatus(toopher_user_uid, 0) 
                if (not pairing_result):
                    print "Toopher authenticate for step 2. The pairing has not been authorized by the phone yet"
                    return False

            return True
        elif (step == 3):
            print "Toopher authenticate for step 3"

            passed_step1 = self.isPassedDefaultAuthentication
            if (not passed_step1):
                return False

            toopher_user_uid_array = requestParameters.get("toopher_user_uid")
            if ArrayHelper.isEmpty(toopher_user_uid_array):
                print "Toopher authenticate for step 3. toopher_user_uid is empty"
                return False

            toopher_terminal_name = configurationAttributes.get("toopher_terminal_name").getValue2()

            toopher_user_uid = stringEncrypter.decrypt(toopher_user_uid_array[0])

            try:
                request_status = self.tapi.authenticate(toopher_user_uid, toopher_terminal_name);
                request_id = request_status.id;
            except RequestError, err:
                print "Toopher authenticate for step 3. Failed to send authentication request to phone: ", err
                return False

            print "Toopher authenticate for step 3. request_id: ", request_id
            request_result = self.checkRequestStatus(request_id, toopher_user_timeout) 

            if (not request_result):
                print "Toopher authenticate for step 3. The authentication request has not received a response from the phone yet"
                return False
                
            print "Toopher authenticate for step 3. The request was granted"

            return True
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        return True

    def getExtraParametersForStep(self, configurationAttributes, step):
        if (step in [2, 3]):
            return Arrays.asList("toopher_user_uid")
        
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 3

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            return "/login/toopher/tppair.xhtml"
        elif (step == 3):
            return "/login/toopher/tpauthenticate.xhtml"
        return ""

    def isPassedDefaultAuthentication():
        credentials = Identity.instance().getCredentials()
        user_name = credentials.getUsername()
        passed_step1 = StringHelper.isNotEmptyString(user_name)

        return passed_step1

    def checkPairingStatus(self, pairing_id, timeout):
        try:
            curTime = java.lang.System.currentTimeMillis()
            endTime = curTime + timeout * 1000
            while (endTime >= curTime):
                pairing_status = self.tapi.getPairingStatus(pairing_id)
                if (pairing_status.enabled):
                    print "Pairing complete"
                    return True

                java.lang.Thread.sleep(2000)
                curTime = java.lang.System.currentTimeMillis()
        except java.lang.Exception, err:
            print "Could not check pairing status: ", err
            return False

        print "The pairing has not been authorized by the phone yet"

        return False

    def checkRequestStatus(self, request_id, timeout):
        try:
            curTime = java.lang.System.currentTimeMillis()
            endTime = curTime + timeout * 1000
            while (endTime >= curTime):
                request_status = self.tapi.getAuthenticationStatus(request_id)
                if (request_status.cancelled):
                    print "The authentication request has been cancelled"
                    return False

                if (not request_status.pending):
                    if (request_status.granted):
                        print "The request was granted"
                        return True

                java.lang.Thread.sleep(2000)
                curTime = java.lang.System.currentTimeMillis()
        except java.lang.Exception, err:
            print "Could not check authentication status: ", err
            return False

        print "The authentication request has not received a response from the phone yet"

        return False

    def logout(self, configurationAttributes, requestParameters):
        return True

    def getApiVersion(self):
        return 3
