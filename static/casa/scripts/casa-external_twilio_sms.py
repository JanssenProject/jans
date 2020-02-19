# This is a modified version of original twilio_sms Gluu's script to work with Casa

from java.util import Arrays

from javax.faces.application import FacesMessage

from org.gluu.jsf2.message import FacesMessages
from org.gluu.oxauth.security import Identity
from org.gluu.oxauth.service import UserService, AuthenticationService
from org.gluu.oxauth.util import ServerUtil
from org.gluu.model.custom.script.type.auth import PersonAuthenticationType
from org.gluu.service.cdi.util import CdiUtil
from org.gluu.util import StringHelper, ArrayHelper

from com.google.common.base import Joiner

from com.twilio import Twilio
import com.twilio.rest.api.v2010.account.Message as TwMessage
from com.twilio.type import PhoneNumber

import random
import sys

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Twilio SMS. Initialized"
        return True

    def destroy(self, configurationAttributes):
        print "Twilio SMS. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):

        print "TwilioSMS. Authenticate for Step %s" % str(step)
        identity = CdiUtil.bean(Identity)
        authenticationService = CdiUtil.bean(AuthenticationService)
        user = authenticationService.getAuthenticatedUser()

        if step == 1:

            if user == None:
                credentials = identity.getCredentials()
                user_name = credentials.getUsername()
                user_password = credentials.getPassword()

                if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
                    authenticationService.authenticate(user_name, user_password)
                    user = authenticationService.getAuthenticatedUser()

            if user == None:
                return False

            #Attempt to send message now if user has only one mobile number
            mobiles = user.getAttributeValues("mobile")

            if mobiles == None:
                return False
            else:
                code = random.randint(100000, 999999)
                identity.setWorkingParameter("randCode", code)

                sid = configurationAttributes.get("twilio_sid").getValue2()
                token = configurationAttributes.get("twilio_token").getValue2()
                self.from_no = configurationAttributes.get("from_number").getValue2()
                Twilio.init(sid, token)

                if mobiles.size() == 1:
                    self.sendMessage(code, mobiles.get(0))
                else:
                    chopped = ""
                    for numb in mobiles:
                        l = len(numb)
                        chopped += "," + numb[max(0, l-4) : l]

                    #converting to comma-separated list (identity does not remember lists in 3.1.3)
                    identity.setWorkingParameter("numbers", Joiner.on(",").join(mobiles.toArray()))
                    identity.setWorkingParameter("choppedNos", chopped[1:])

                return True
        else:
            if user == None:
                return False

            session_attributes = identity.getSessionId().getSessionAttributes()
            code = session_attributes.get("randCode")
            numbers = session_attributes.get("numbers")

            if step == 2 and numbers != None:
                #Means the selection number page was used
                idx = ServerUtil.getFirstValue(requestParameters, "OtpSmsloginForm:indexOfNumber")
                if idx != None and code != None:
                    sendToNumber = numbers.split(",")[int(idx)]
                    self.sendMessage(code, sendToNumber)
                    return True
                else:
                    return False

            success = False
            form_passcode = ServerUtil.getFirstValue(requestParameters, "passcode")

            if form_passcode != None and code == form_passcode:
                print "TwilioSMS. authenticate. 6-digit code matches with code sent via SMS"
                success = True
            else:
                facesMessages = CdiUtil.bean(FacesMessages)
                facesMessages.setKeepMessages()
                facesMessages.clear()
                facesMessages.add(FacesMessage.SEVERITY_ERROR, "Wrong code entered")

            return success

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print "TwilioSMS. Prepare for Step %s" % str(step)
        return True

    def getExtraParametersForStep(self, configurationAttributes, step):
        if step > 1:
            return Arrays.asList("randCode", "numbers", "choppedNos")
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        print "TwilioSMS. getCountAuthenticationSteps called"

        if CdiUtil.bean(Identity).getWorkingParameter("numbers") == None:
            return 2
        else:
            return 3

    def getPageForStep(self, configurationAttributes, step):
        print "TwilioSMS. getPageForStep called %s" % step
        print "numbers are %s" % CdiUtil.bean(Identity).getWorkingParameter("numbers")

        defPage = "/casa/otp_sms.xhtml"
        if step == 2:
            if CdiUtil.bean(Identity).getWorkingParameter("numbers") == None:
                return defPage
            else:
                return "/casa/otp_sms_prompt.xhtml"
        elif step == 3:
            return defPage
        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    def hasEnrollments(self, configurationAttributes, user):
        return user.getAttribute("mobile") != None

    def sendMessage(self, code, numb):
        try:
            print "TwilioSMS. Sending SMS message (%s) to %s" % (code, numb)
            msg = "%s is your passcode to access your account" % code
            message = TwMessage.creator(PhoneNumber(numb), PhoneNumber(self.from_no), msg).create()
            print "TwilioSMS. Message Sid: %s" % message.getSid()
        except:
            print "TwilioSMS. Error sending message", sys.exc_info()[1]
