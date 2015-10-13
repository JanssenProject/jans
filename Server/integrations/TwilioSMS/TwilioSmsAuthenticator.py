from org.jboss.seam.security import Identity
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService
from org.xdi.util import StringHelper
from org.jboss.seam.contexts import Context, Contexts

import com.twilio.sdk.TwilioRestClient as TwilioRestClient
import com.twilio.sdk.TwilioRestException as TwilioRestException
import com.twilio.sdk.resource.factory.MessageFactory as MessageFactory
import com.twilio.sdk.resource.instance.Message as Message
import org.apache.http.NameValuePair as NameValuePair
import org.apache.http.message.BasicNameValuePair BasicNameValuePair
import java.util.ArrayList as ArrayList
 
import java
import random

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
        
    def init(self, configurationAttributes):
        printOut("Initialization")
        return True   

    def destroy(self, configurationAttributes):
        printOut("Destroy")
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
    	context = Contexts.getEventContext()
    	userService = UserService.instance()
    	
        if (step == 1):
            printOut("Step 1 Password Authentication")
            credentials = Identity.instance().getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = userService.authenticate(user_name, user_password)

            if (not logged_in):
                return False
            
            # Get Custom Properties
            ACCOUNT_SID = None
    		AUTH_TOKEN = None
    		FROM_NUMBER = None
            try:    
	            ACCOUNT_SID = configurationAttributes.get("twilio_sid").getValue2()
	    	except:
	    		printOut('Missing required configuration attribute "twilio_sid"')
	    	try:
	    		AUTH_TOKEN = configurationAttributes.get("twilio_token").getValue2()
	    	except:
	    		printOut('Missing required configuration attribute "twilio_token")
	    	try:
	    		FROM_NUMBER = configurationAttributes.get("from_number").getValue2()
	    	except:
	    		printOut('Missing required configuration attribute "from_number"')
	    	if None in (ACCOUNT_SID, AUTH_TOKEN, FROM_NUMBER):
	    		return False
    		
    		# Get the Person's number and generate a code
    		foundUser = None
    		try:
	    		foundUser = userService.getUserByAttribute("uid", user_name)
	    	except:
	    		printOut('Error retrieving user %s from LDAP' % user_name)
	    		return False
	    	try:
	    		mobile_number = foundUser.getAttribute("mobile")
	    	except:
	    		printOut("Error finding mobile number for 
	    		return False
	    		
	    	# Generate Random six digit code
    		code = random.randint(100000,999999)
    		context.set("code", code)
    		
    		client = TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN)
    		bodyParam = BasicNameValuePair("Body", code)
    		toParam = BasicNameValuePair("To", mobile_number)
    		fromParam = BasicNameValuePair("From", FROM_NUMBER)
    		params = ArrayList()
    		params.add(bodyParam)
    		params.add(toParam)
    		params.add(fromParam)
    		    		
			messageFactory = client.getAccount().getMessageFactory()
			message = messageFactory.create(params)
			printOut("Message Sid: %s" % message.getSid())
            return True

        elif (step == 2):
        	code = sessionAttributes.get("code")
        	if (code is None):
                printOut("Failed to find previously sent code")
                return False
            form_passcode = requestParameters.get("passcode")[0].strip()
            if len(form_passcode) != 6:
            	printOut("Invalid passcode length from form: %s" % form_passcode)
            if form_passcode == code:
            	return True
            else:
            	return False
            
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "TwilioSMS. Prepare for Step 1"
            return True
        elif (step == 2):
            print "TwilioSMS. Prepare for Step 2"
            return True
        else:
            return False

	def printOut(s):
		print "TwilioSmsAuthenticator: %s" % s

    def getExtraParametersForStep(self, configurationAttributes, step):
        if (step == 2):
            return Arrays.asList("code")
        return None
        
    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            return "/auth/twilio/twiliologin.xhtml"
        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True