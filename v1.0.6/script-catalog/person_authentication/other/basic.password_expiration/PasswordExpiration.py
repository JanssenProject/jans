import datetime

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import UserService, AuthenticationService
from io.jans.util import StringHelper, ArrayHelper
from com.unboundid.util import StaticUtils
from java.util import GregorianCalendar, TimeZone
from java.util import Arrays


# This script expect that user has attribute oxPasswordExpirationDate with valid expiration date
class PersonAuthentication(PersonAuthenticationType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Basic (with password update). Initialization"
        print "Basic (with password update). Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "Basic (with password update). Destroy"
        print "Basic (with password update). Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    def getAuthenticationMethodClaims(self, requestParameters):
        return None

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        authenticationService = CdiUtil.bean(AuthenticationService)
        userService = CdiUtil.bean(UserService)

        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()
        if step == 1:
            print "Basic (with password update). Authenticate for step 1"
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
                logged_in = authenticationService.authenticate(user_name, user_password)
                                                    
            if not logged_in:
                return False

            find_user_by_uid = authenticationService.getAuthenticatedUser()
            user_expDate = find_user_by_uid.getAttribute("oxPasswordExpirationDate", False)
           
            if user_expDate == None:
                print "Basic (with password update). Authenticate for step 1. User has no oxPasswordExpirationDate date"
                return False

            dt = StaticUtils.decodeGeneralizedTime(user_expDate)

            # Get Current Date
            calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"));
            now = calendar.getTime()
            if now.compareTo(dt) > 0:
                # Add 90 Days to current date
                calendar.setTime(now)
                calendar.add(calendar.DATE, 1)
                dt_plus_90 = calendar.getTime()
                expDate = StaticUtils.encodeGeneralizedTime(dt_plus_90)
                identity.setWorkingParameter("expDate", expDate)

            return True
        elif step == 2:
            print "Basic (with password update). Authenticate for step 2"
            user = authenticationService.getAuthenticatedUser()            
            if user == None:
                print "Basic (with password update). Authenticate for step 2. Failed to determine user name"
                return False

            user_name = user.getUserId()
            find_user_by_uid = userService.getUser(user_name)
            newExpDate = identity.getWorkingParameter("expDate")
            if find_user_by_uid == None:
                print "Basic (with password update). Authenticate for step 2. Failed to find user"
                return False
            print "Basic (with password update). Authenticate for step 2"
            update_button = requestParameters.get("loginForm:updateButton")

            if ArrayHelper.isEmpty(update_button):
                return True

            find_user_by_uid.setAttribute("oxPasswordExpirationDate", newExpDate)
            new_password_array = requestParameters.get("loginForm:password")
            if ArrayHelper.isEmpty(new_password_array) or StringHelper.isEmpty(new_password_array[0]):
                print "Basic (with password update). Authenticate for step 2. New password is empty"
                return False

            new_password = new_password_array[0]
            find_user_by_uid.setAttribute("userPassword", new_password)
            print "Basic (with password update). Authenticate for step 2. Attempting to set new user '%s' password" % user_name

            userService.updateUser(find_user_by_uid)
            print "Basic (with password update). Authenticate for step 2. Password updated successfully"

            return True
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if step == 1:
            print "Basic (with password update). Prepare for Step 1"
            return True
        elif step == 2:
            print "Basic (with password update). Prepare for Step 2"
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("expDate")

    def getCountAuthenticationSteps(self, configurationAttributes):
        identity = CdiUtil.bean(Identity)
        if identity.isSetWorkingParameter("expDate"):
            return 2
        else:
           return 1

    def getPageForStep(self, configurationAttributes, step):
        if step == 2:        
            return "/auth/pwd/newpassword.xhtml"
        return ""

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
