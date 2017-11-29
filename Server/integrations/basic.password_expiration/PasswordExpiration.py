import datetime

from org.xdi.service.cdi.util import CdiUtil
from org.xdi.oxauth.security import Identity
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService, AuthenticationService
from org.xdi.util import StringHelper, ArrayHelper
from com.unboundid.util import StaticUtils
from java.util import GregorianCalendar, TimeZone
from java.util import Arrays

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Basic (with password update). Initialization"
        print "Basic (with password update). Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "Basic (with password update). Destroy"
        print "Basic (with password update). Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        authenticationService = CdiUtil.bean(AuthenticationService)
        userService = CdiUtil.bean(UserService)

        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()
        if (step == 1):
            print "Basic (with password update). Authenticate for step 1"
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()
            find_user_by_uid = userService.getUser(user_name)
            user_expDate = find_user_by_uid.getAttribute("oxPasswordExpirationDate", False)
           
            if (user_expDate == None):
                    print "Failed to get Date"
                    return False

            print "Exp Date is : '" + user_expDate + "' ."
            dt = StaticUtils.decodeGeneralizedTime(user_expDate)

            # Get Current Date
            calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"));
            now = calendar.getTime()
            if now.compareTo(dt) > 0:
   	        # Add 90 Days to current date
	        calendar.setTime(now)
	        calendar.add(calendar.DATE, 90);
	        dt_plus_90 = calendar.getTime()
	        expDate = StaticUtils.encodeGeneralizedTime(dt_plus_90)
                identity.setWorkingParameter("expDate", expDate)
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = authenticationService.authenticate(user_name, user_password)
													
            if (not logged_in):
                return False
            return True
        elif (step == 2):
            print "Basic (with password update). Authenticate for step 2"
            user = authenticationService.getAuthenticatedUser()
            if (user == None):
                print "Basic (with password update). Authenticate for step 2. Failed to determine user name"
                return False
            user_name = user.getUserId()
            find_user_by_uid = userService.getUser(user_name)
            session_attributes = identity.getSessionId().getSessionAttributes()
            newExpDate = session_attributes.get("expDate")

            if (find_user_by_uid == None):
                print "Basic (with password update). Authenticate for step 2. Failed to find user"
                return False

            print "Basic (with password update). Authenticate for step 2"
            find_user_by_uid.setAttribute("oxPasswordExpirationDate", newExpDate)
            update_button = requestParameters.get("loginForm:updateButton")

            if ArrayHelper.isEmpty(update_button):
                return True

            new_password_array = requestParameters.get("new_password")
            if ArrayHelper.isEmpty(new_password_array) or StringHelper.isEmpty(new_password_array[0]):
                print "Basic (with password update). Authenticate for step 2. New password is empty"
                return False
            new_password = new_password_array[0]
            find_user_by_uid.setAttribute("userPassword", new_password)
            print "Basic (with password update). Authenticate for step 2. Attempting to set new user '" + user_name + "' password"
            userService.updateUser(find_user_by_uid)
            print "Basic (with password update). Authenticate for step 2. Password updated successfully"
            return True
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Basic (with password update). Prepare for Step 1"
            return True
        elif (step == 2):
            print "Basic (with password update). Prepare for Step 2"
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("expDate")

    def getCountAuthenticationSteps(self, configurationAttributes):
        identity = CdiUtil.bean(Identity)
        session_attributes = identity.getSessionId().getSessionAttributes()
        newExpDate = session_attributes.get("expDate")
        print newExpDate
        if(newExpDate !=None):
            return 2
        else:
           return 1

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):        
            return "/auth/pwd/newpassword.xhtml"
        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True
