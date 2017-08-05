import datetime

from org.xdi.service.cdi.util import CdiUtil
from org.xdi.oxauth.security import Identity
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService, AuthenticationService
from org.xdi.util import StringHelper, ArrayHelper


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

    def parseDate(self, LDAPDate):
        list = []
        list.append(LDAPDate[0:4])
        for index in range (4,( len(LDAPDate) )-1,2):
            list.append(LDAPDate[index:index+2])
        return list

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def newExpirationDate(self, date_time):
        year = date_time[0]
        date_time[0] = year[2:]
        
        startDate = ""
        index = 0
        for index in range(0,2):
            startDate += date_time[index]
            startDate += "-"
        index += 1
        startDate = startDate + date_time[index]
        date_1 = datetime.datetime.strptime(startDate, "%y-%m-%d")
        expDate = date_1 + datetime.timedelta(days=20)

        return expDate

    def previousExpDate(self, dateList):
        year = dateList[0]
        dateList[0] = year[2:]
        startDate = ""
        index = 0
        for index in range(0,2):
            startDate += dateList[index]
            startDate += "-"
        index += 1
        startDate = startDate + dateList[index]
        return datetime.datetime.strptime(startDate, "%y-%m-%d")

    def authenticate(self, configurationAttributes, requestParameters, step):
        authenticationService = CdiUtil.bean(AuthenticationService)
        userService = CdiUtil.bean(UserService)

        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()

        if (step == 1):
            print "Basic (with password update). Authenticate for step 1"

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

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

            if (find_user_by_uid == None):
                print "Basic (with password update). Authenticate for step 2. Failed to find user"
                return False

            user_expDate = find_user_by_uid.getAttribute("oxPasswordExpirationDate", False)

            if (user_expDate == None):
                    print "Failed to get Date"
                    return False

            print "Exp Date is : '" + user_expDate + "' ."

            now = datetime.datetime.now()

            myDate = self.parseDate(user_expDate)

            prevExpDate = self.previousExpDate(myDate)

            expDate = self.newExpirationDate(myDate)

            temp = expDate.strftime("%y%m%d")

            expDate = (expDate + temp + "195000Z")

            if prevExpDate < now:
                print "Basic (with password update). Authenticate for step 2"

                find_user_by_uid.setAttribute("oxPasswordExpirationDate", expDate)

                update_button = requestParameters.get("loginForm:updateButton")
                if ArrayHelper.isEmpty(update_button):
                    return True

                new_password_array = requestParameters.get("new_password")
                if ArrayHelper.isEmpty(new_password_array) or StringHelper.isEmpty(new_password_array[0]):
                    print "Basic (with password update). Authenticate for step 2. New password is empty"
                    return False

                new_password = new_password_array[0]

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
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            return "/auth/pwd/newpassword.xhtml"

        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True


